SwayS {

	var <>name, <>channel=0, <>numChan, <>input, <>analysis_input, <>buffer, <>processing, <>recorder, <>position, <>wfsNetAddr, <>refresh_rate=1, <>fade=30, <>output,
	<>density, <>clarity, <>amplitude, <>tempo, <>gridanalysis, <>xbus, <>ybus, <>aThreshBus, <>aThresh=2,
	<current_processing, <current_processingKey, <>processors, <>modulators, <current_spatializer, <>spatializers, <>spatializerBusToOSC,
	<>task, <>analysisView, <>gridView;

	new {
		this.init;
	}

	init {
		|givenName="sway",chan=0,numChannels=1|

		//assign name of channel
		name = givenName;
		numChan = numChannels;
		channel = chan;

		//assign wfsNetAddr
		wfsNetAddr = NetAddr("127.0.0.1", 57120);

		//audio input

		input = NodeProxy.audio(Server.default, numChan)
		.source = { |chan=0| SoundIn.ar(chan,1.neg) };

		//analysis input

		analysis_input = NodeProxy.audio(Server.default, numChan)
		.source = { |chan=0| SoundIn.ar(chan) };

		//audio recorder
		buffer = Buffer.alloc(Server.default, (Server.default.sampleRate * fade), 1);
		recorder = NodeProxy.audio(Server.default, 1)
		.source = {
			var off = Lag2.kr(A2K.kr(DetectSilence.ar(input.ar(1), 0.1), 0.3));
            var on = 1-off;
			var fade = MulAdd.new(on, 2, 1.neg);
			var out = XFade2.ar(Silent.ar(), input.ar(1), fade);
			RecordBuf.ar(out, buffer, loop: 1, run: on);
		};

		//processing

		processing = NodeProxy.audio(Server.default, numChan).fadeTime_(fade)
		.source = { Silent.ar(1) }; //start with silent processing

		//output

		output = NodeProxy.audio(Server.default, numChan)
		.source = { processing.ar(1) };

		//busses for cartesian coordinate system
		xbus = Bus.control(Server.default, 1);
		ybus = Bus.control(Server.default, 1);
		aThreshBus = Bus.control(Server.default, 1);

		//static spatializer to start;
		position = NodeProxy.control(Server.default, 2)
		.source = { DC.kr(0!2); };

		spatializerBusToOSC = [BusToOSC.new(wfsNetAddr, position.bus.subBus(0,1), "/"++channel++"/x/", refresh_rate, 1),
			BusToOSC.new(wfsNetAddr, position.bus.subBus(1,1), "/"++channel++"/y/", refresh_rate, 1)];

		//build analysis on start
		this.build_analysis;

		//build grid framework
		//this.build_grid;

	}

	build_analysis {

		density = NodeProxy.control(Server.default, 1)
		.source = {
			//Density Tracker
			var buf = LocalBuf.new(512,1);
			var onsets = Onsets.kr(FFT(buf, analysis_input.ar(1)));
			var shortStats = OnsetStatistics.kr(onsets, 1);
			//var longStats = OnsetStatistics.kr(onsets, long_win);
			var shortValue = (shortStats[0]/1);
			//var longValue = (longStats[0]/long_win);
			//[shortValue, longValue];
			shortValue;
		};

		amplitude = NodeProxy.control(Server.default, 2)
		.source = {
			//Amplitude Tracker
			var chain = FFT(LocalBuf(1024), analysis_input.ar(1));
			var loudness = Loudness.kr(chain);
			//var shortAverage = AverageOutput.kr(loudness, Impulse.kr(short_win.reciprocal));
			//var longAverage = AverageOutput.kr(loudness, Impulse.kr(long_win.reciprocal));
			//[shortAverage, longAverage];
			loudness;
		};

		clarity = NodeProxy.control(Server.default, 2)
		.source = {
			var freq, hasFreq, shortAverage, longAverage;
			//Pitch hasfreq Tracker
			# freq, hasFreq = Pitch.kr(analysis_input.ar(1));
			shortAverage = AverageOutput.kr(hasFreq,Impulse.kr(1));
			//longAverage = AverageOutput.kr(hasFreq,Impulse.kr(long_win.reciprocal));
			//[shortAverage, longAverage];
			shortAverage;
		};

		tempo = NodeProxy.control(Server.default, 1)
		.source = {
			//add some variation of BeatTrack here
		};

		gridanalysis = NodeProxy.control(Server.default, 3)
		.source = {
			|lag=30, densityWarp = 0, clarityWarp = 0, ampWarp = 0|
			var d = this.density.kr(1,0).lag(lag),
			c = this.clarity.kr(1,0).lag(lag),
			a = this.amplitude.kr(1,0).lag(lag);

			// scale these to 0 - 1 and curve
			c = c.clip.lincurve(0, 1, 0, 1, clarityWarp);
			a = (a / 40).clip.lincurve(0, 1, 0, 1, ampWarp);
			d = (d / 10).clip.lincurve(0, 1, 0, 1, densityWarp);

			Out.kr(xbus, c);
			Out.kr(ybus, d);
			//Out.kr(this.xbus, c.linlin(0, 1, -0.5, 0.5) * 1 + 0.5);
			//Out.kr(this.ybus, d.linlin(0, 1, -0.5, 0.5) * 1 + 0.5);
			Out.kr(aThreshBus, a > this.aThresh);
		};

	}

	addProcessor {|name, key, func, fadeTime = 1|
		(this.processors.isNil).if({
			processors = Dictionary.new;
		});
		this.processors.put(key, Dictionary[
			\name -> name,
			\key -> key,
			\func -> func,
			\fadeTime -> fadeTime
		]);
	}

	runProcessor {|key|
		var processor = this.processors[key];

		(processor.isNil.not).if({
			//processing.source.fadeTime = processor[\fadeTime];
			processing.source = processor[\func].value(this);
			(this.name++": " ++ "Processing is: " ++ processor[\name]).postln;
			current_processing = processor[\name];
			current_processingKey = processor[\key];
		}, {
			"Processor does not exist".postln;
		})
	}

	getModulatorNode {|key|
		^this.modulators[key][\node];
	}

	addModulator {|name, key, assocProc, func, reversePolarityFunc, fadeTime = 1|
		(this.modulators.isNil).if({
			modulators = Dictionary.new;
		});
		this.modulators.put(key, Dictionary[
			\name -> name,
			\func -> func,
			\assocProc -> assocProc,
			\reversePolarityFunc -> reversePolarityFunc,
			\fadeTime -> fadeTime,
			\node -> NodeProxy(Server.default, 'control', 1)
		]);
	}

	runModulator {|key, reversePolarity = false|
		var modulator = this.modulators[key],
		node = modulator[\node],
		funcKey = (reversePolarity).if({ \reversePolarityFunc }, { \func });

		(modulator.isNil.not).if({
			//node.source.fadeTime = modulator[\fadeTime];
			node.source = modulator[funcKey].value(this);
			this.modulators[key] = modulator;
			// (this.name++": " ++ modulator[\name]).postln;
			// current_processing = modulator[\name];
		}, {
			"Modulator does not exist".postln;
		})
	}

	processingGUI { |window|
		var flow, changeProcessingView, popUp,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		changeProcessingView = View.new(window, (labelWidth+valueWidth+20)@(lineHeight*2));
		flow = changeProcessingView.addFlowLayout();
		//StaticText(changeProcessingView)
		StaticText(changeProcessingView, Rect(10,10,labelWidth,lineHeight))
		.string_("Change Processing: ");
		popUp = PopUpMenu(changeProcessingView, Rect(10,10,valueWidth,lineHeight));
		popUp.items_(this.processors.keys.asArray.sort);
		popUp.action_({|menu,item|
			this.runProcessor(menu.item.asSymbol);
		});
		popUp.valueAction_(3);
		//.items(this.processors.keys.asArray);
		//popUp.action({|menu| [menu.value,menu.item].postln;});

		changeProcessingView = Dictionary[
			\view -> changeProcessingView,
			\menu -> popUp
		];
		^changeProcessingView;
	}

	spatializationGUI { |window|
		var flow, changeSpatializationView, popUp,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		changeSpatializationView = View.new(window, (labelWidth+valueWidth+20)@(lineHeight*2));
		flow = changeSpatializationView.addFlowLayout();
		//StaticText(changeProcessingView)
		StaticText(changeSpatializationView, Rect(10,10,labelWidth,lineHeight))
		.string_("Change Spatialization: ");
		popUp = PopUpMenu(changeSpatializationView, Rect(10,10,valueWidth,lineHeight));
		popUp.items_(this.spatializers.keys.asArray.sort);
		popUp.action_({|menu,item|
			this.runSpatializer(menu.item.asSymbol);
		});
		popUp.valueAction_(2);
		//.items(this.processors.keys.asArray);
		//popUp.action({|menu| [menu.value,menu.item].postln;});

		changeSpatializationView = Dictionary[
			\view -> changeSpatializationView,
			\menu -> popUp
		];
		^changeSpatializationView;
	}

	analysisGUI { |window|
		var flow, processingView, densityView, clarityView, ampView, xView, yView, layout,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		analysisView = View.new(window, (labelWidth+valueWidth+20)@(lineHeight*8));
		flow = analysisView.addFlowLayout();
		StaticText(analysisView, Rect(10,10,labelWidth,lineHeight))
		.string_(("Channel "++((this.name.asInteger))));
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("processing: ");
		processingView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("density: ");
		densityView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("clarity: ");
		clarityView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("amp: ");
		ampView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		StaticText(analysisView, labelWidth@lineHeight).string_("X: ");
		xView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		StaticText(analysisView, labelWidth@lineHeight).string_("Y: ");
		yView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		/*layout = VLayout([
			HLayout([
				StaticText().string_("density: "),
				densityView
			]),
			HLayout([
				StaticText().string_("clarity: "),
				clarityView
			]),
			HLayout([
				StaticText().string_("amp: "),
				ampView
			])
		]);

		analysisView.layout_(layout);*/
		analysisView = Dictionary[
			\view -> analysisView,
			\processing -> processingView,
			\density -> densityView,
			\clarity -> clarityView,
			\amp -> ampView,
			\x -> xView,
			\y -> yView
		];
		^analysisView;
	}

	syncGUI {
		task = TaskProxy({
			loop {
				this.refresh_rate.wait;
				{this.analysisView[\processing].string_(this.current_processing.asString);}.defer;
				this.density.bus.get({|v|
					{ this.analysisView[\density].string_(v.round(0.001).asString); }.defer;
				});
				this.clarity.bus.get({|v|
					{ this.analysisView[\clarity].string_(v.round(0.001).asString); }.defer;
				});
				this.amplitude.bus.get({|v|
					{ this.analysisView[\amp].string_(v.round(0.001).asString); }.defer;
				});
				this.xbus.get({|v|
					{ this.analysisView[\x].string_(v.round(0.001).asString); }.defer;
				});
				this.ybus.get({|v|
					{ this.analysisView[\y].string_(v.round(0.001).asString); }.defer;
				});
			}
		});
		task.play;
	}

	stopSyncGUI {
		task.stop;
	}

	gridGUI { |window|
		var flow, gridWidth=400, gridHeight=400, layout;

		gridView = EnvelopeView(window, gridWidth@gridHeight)
		.thumbWidth_(60.0)
		.thumbHeight_(15.0)
		.drawLines_(false)
		.drawRects_(true)
		.step_(0.01)
		.selectionColor_(Color.red)
		.grid_(Point(0.5, 0.5))
		.gridOn_(true)
		.mouseDownAction_({|view|
			//updater.pause;

		})
		//Tdef(("analysisLoop_"++view.index).asSymbol).pause;})
		.mouseUpAction_({|view|
			//updater.resume(AppClock,1);

		})
		//Tdef(("analysisLoop_"++view.index).asSymbol).resume(AppClock,1);})
		.action_({|view|
			//model[view.index] = [view.x, view.y];
			//[view.index,view.x,view.y].postln;
		});

		//.value_([[0.5,0.5],[0.5,0.5]]);

		gridView.setString((this.name.asInteger)-1, this.name.asString);
		gridView.setFillColor((this.name.asInteger)-1, Color.yellow);
		^gridView;

	}


	verboseTask {
		task = TaskProxy({
			loop {
				1.0.wait;

				this.density.bus.get({|val| ("density: "++val).postln});
				this.clarity.bus.get({|val| ("clarity: "++val).postln});
				this.amplitude.bus.get({|val| ("amp: "++val).postln});
		}});

	}

	verbose { |boolean=false|
		boolean.if({
			task.play},
		{task.stop});
	}

	//The spatializers are Control NodeProxies that generate values between 0 and 1 for use within WFSCollider's spatial point source controls. Each spatializer should be a 2 channel NodeProxy with values coming out of the first channel corresponding to a sound source's x position and values on the second channel corresponding to a sound source's y position.
	addSpatializer {|name, key, func, fadeTime = 1|
		(this.spatializers.isNil).if({
			spatializers = Dictionary.new;
		});
		this.spatializers.put(key, Dictionary[
			\name -> name,
			\func -> func,
			\fadeTime -> fadeTime
		]);

	}

	runSpatializer {|key|
		var spatializer = this.spatializers[key];

		(spatializer.isNil.not).if({
			//processing.source.fadeTime = processor[\fadeTime];
			position.source = spatializer[\func].value(this);
			(this.name++": " ++ "Spatializer is: " ++ spatializer[\name]).postln;
			current_spatializer = spatializer[\name];
		}, {
			"Spatializer does not exist".postln;
		})
	}

}