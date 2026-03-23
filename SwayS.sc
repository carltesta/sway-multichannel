/*

Ideas about how to set up the quadrants so that changes can be made based on things that are not just random.

For example, each quadrant will have processor associations based on the current polarity. So the noisy dense quadrant would have granular synthesis allogrithms when polarity is off, and amplitude modulation when polarity is on.

processors can have associated spatializers so that when a certain processor is started it can activate a particular spatializer

I need to learn how to create spatializer patterns via code
*/

SwayS {

	var <>name, <>channel=0, <>numChan, <>input, <>dry, <>analysis_input, <>buffer, <>processing, <>recorder, <>delaybuffer, <>beamformer, <>stereomix, <>refresh_rate=1, <>fade=30, <>output, <polarity=false,
	<>density, <>clarity, <>amplitude, <>tempo, <>gridanalysis, <>xbus, <>ybus, <>ampThreshBus, <>ampThresh=0.01,
	<current_processing, <current_processingKey=\silence, <>processors, <>modulators, <current_spatializer, <current_spatializerKey=\static, <>spatializers, <>changeSpatializationView, <>analysisView, <>changeProcessingView, <>modulatorView, <>gridView, <>quadrantHistory, <>processorHistory, <>timeHistory, <quadrant=nil, <>loopQuadrantRead=true, <>quadrantReadTime=10,
	<>quadrantProcessingThresholds,
	<quadrantProcessors, <array, <xPosition, <yPosition, <angle=nil, <>numSpeakers=64, <>speakerSpacing=0.07, <>speedOfSound=343;

	new {
		this.init;
	}

	init {
		|givenName="sway",chan=0,numChannels=1|

		quadrantProcessingThresholds = Array.fill(4, 100/quadrantReadTime);
		quadrantProcessors = Array.newClear(4);

		//assign name of channel
		name = givenName;
		numChan = numChannels;
		channel = chan;

		//histories
		quadrantHistory = List.new;
		processorHistory = List.new;
		timeHistory = List.new;

		//audio input

		input = NodeProxy.audio(Server.default, numChan)
		.source = { |chan=0, gain=1| SoundIn.ar(chan)*gain };//removed 1.neg in the mul argument

		//analysis input

		analysis_input = NodeProxy.audio(Server.default, numChan)
		.source = { |chan=0, gain=1| SoundIn.ar(chan)*gain };

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

		//12 second delay buffer
		delaybuffer = Buffer.allocConsecutive(2, Server.default, (12*Server.default.sampleRate), 1);

		Server.default.sync;
		//processing

		processing = NodeProxy.audio(Server.default, numChan).fadeTime_(fade)
		.source = { Silent.ar(1) }; //start with silent processing

		//output

		output = NodeProxy.audio(Server.default, numChan).fadeTime_(fade)
		.source = { |volume=1| processing.ar(1)*volume };

		//busses for cartesian coordinate system
		xbus = Bus.control(Server.default, 1);
		ybus = Bus.control(Server.default, 1);
		ampThreshBus = Bus.control(Server.default, 1);

		//no spatializer to start;
		beamformer = NodeProxy.audio(Server.default, numSpeakers).fadeTime_(fade);
		//.source = { Silent.ar(numSpeakers) };

		//object to store the speaker array config in
		array = BeamFormer.arrayConf(numSpeakers, speakerSpacing, speedOfSound);

		xPosition = this.initialXPositions[chan];
		yPosition = 2; //2 meters from array

		//dry output (set to be asleep at first, won't start until needed)
		dry = NodeProxy.audio(Server.default, numSpeakers).source_({
			|drymix=0|
		BeamFormer.arFocal(input.ar(numChan), x: xPosition, y: yPosition, amp: drymix, numSpeakers: numSpeakers);
		});

		stereomix = NodeProxy.audio(Server.default, 2).fadeTime_(fade);
		//.source = { |in| Splay.ar(in, 1, 1/64) };

		//build analysis on start
		this.build_analysis;

		CmdPeriod.doOnce({
			this.loopQuadrantRead = false;
		});

		//build grid framework
		//this.build_grid;
	}

	/*////////////

	AUDIO CONTROLS

    */////////////

	audioGUI { |window|
		var flow, audioView, outVolView, outVolNum, stereoMix, dryVolView, dryVolNum, dryVolStart, ampThreshView, ampThreshSlider, ampThreshIndicator, layout,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		audioView = View.new(window, (labelWidth+valueWidth+20+(valueWidth/3)*2)@(lineHeight*4));
		flow = audioView.addFlowLayout();
		StaticText(audioView, Rect(10,10,labelWidth, lineHeight)).string_("Out Vol");
		outVolView = Slider(audioView, Rect(10,10,valueWidth, lineHeight)).action_({
			this.output.set(\volume, outVolView.value);
			outVolNum.value_(outVolView.value.ampdb)});
		outVolNum = NumberBox(audioView, Rect(10,10,valueWidth/3,lineHeight)).value_(outVolView.value.ampdb).action_({|box|outVolView.value_(box.value.dbamp)});
		stereoMix = Button(audioView, Rect(10,10,lineHeight,lineHeight));
			stereoMix.states_([["Off", Color.black, Color.red],["On", Color.black, Color.green]]);
			stereoMix.action_({|button| if(button.value==1, {beamformer.stop; stereomix.play; beamformer<>>stereomix;},{stereomix.stop;beamformer.play;})});
		flow.nextLine;
		StaticText(audioView, Rect(10,10,labelWidth, lineHeight)).string_("Dry Vol");
		dryVolView = Slider(audioView, Rect(10,10,valueWidth, lineHeight)).action_({
			if(dry.awake==false, {dry.awake_(true)});
			this.dry.set(\drymix, dryVolView.value);
			dryVolNum.value_(dryVolView.value.ampdb)});
		dryVolNum = NumberBox(audioView, Rect(10,10,valueWidth/3,lineHeight)).value_(dryVolView.value.ampdb).action_({|box|dryVolView.value_(box.value.dbamp)});
		dryVolStart = Button(audioView, Rect(10,10,lineHeight,lineHeight));
			dryVolStart.states_([["Play", Color.black, Color.red],["Stop", Color.black, Color.green]]);
			dryVolStart.action_({|button| if(button.value==1, {dry.play},{dry.stop})});
		flow.nextLine;
		StaticText(audioView, Rect(10,10,labelWidth,lineHeight)).string_("Amp Thresh");

		ampThreshView = NumberBox(audioView,Rect(10,10,valueWidth/3,lineHeight)).value_(this.ampThresh).action_({|box|
			this.ampThresh=box.value;
			ampThreshSlider.value(box.value)});

		ampThreshSlider = Slider(audioView, Rect(10,10,valueWidth,lineHeight)).value_(this.ampThresh).action_({|slider|
			ampThreshView.value_(slider.value);
			this.ampThresh = slider.value;
			this.amplitude.set(\ampThresh, (slider.value).ampdb);
		});

		ampThreshIndicator = StaticText(audioView,Rect(10,10,lineHeight,lineHeight)).string_("<").background_(Color.red).stringColor_(Color.black).align_(\center);

		OSCdef(("c"++channel.asString++"ampAboveThresh"++"OSC").asSymbol, {|msg|
			if(msg[3]==1,{	defer{ampThreshIndicator.string_(">").background_(Color.green)};
				//("channel: "++channel++"aboveThresh").postln;
				//msg.postln;
			},
			{
				defer{ampThreshIndicator.string_("<").background_(Color.red)};
				//defer{this.runProcessor(\silence)};
				//("channel: "++channel++"belowThresh").postln;
				//msg.postln;
			});
		},
		("/"++channel++"/aboveThresh/").asSymbol);

		audioView = Dictionary[
			\view -> audioView,
			\outVol -> outVolView,
			\outVolNum -> outVolNum,
			\dryVol -> dryVolView,
			\dryVolNum -> dryVolNum,
			\dryVolButton -> dryVolStart,
			\ampThreshView -> ampThreshView,
			\ampThreshSlider -> ampThreshSlider,
			\ampThreshIndicator -> ampThreshIndicator
		];
		^audioView;

	}

	/*//////////////////////////////////

	ANALYSIS SECTION

	*///////////////////////////////////


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
			SendReply.kr(Impulse.kr(1), ("/"++channel++"/density/").asSymbol, shortValue);
			shortValue;
		};

		amplitude = NodeProxy.control(Server.default, 1)
		.source = { |ampThresh=(36.neg)|
			//Amplitude Tracker
			//FluidAmpFeature.ar(analysis_input.ar(1))
			var chain = FFT(LocalBuf(1024), analysis_input.ar(1));
			var loudness = Loudness.kr(chain);

			var wamp = WAmp.kr(analysis_input.ar(1), 5);
			//var shortAverage = AverageOutput.kr(loudness, Impulse.kr(short_win.reciprocal));
			//var longAverage = AverageOutput.kr(loudness, Impulse.kr(long_win.reciprocal));
			//[shortAverage, longAverage];
			var aboveThresh = FluidAmpGate.ar(analysis_input.ar(1), 10, SampleRate.ir*3, ampThresh, -90, SampleRate.ir*10, SampleRate.ir*1, 1, 1);
			SendReply.kr(Impulse.kr(1), ("/"++channel++"/amplitude/").asSymbol, wamp);
			SendReply.kr(Impulse.kr(1), ("/"++channel++"/aboveThresh/").asSymbol, aboveThresh);
			loudness;
		};

		clarity = NodeProxy.control(Server.default, 1)
		.source = {
			var freq, hasFreq, shortAverage, longAverage;
			//Pitch hasfreq Tracker
			# freq, hasFreq = Tartini.kr(analysis_input.ar(1));
			//shortAverage = AverageOutput.kr(hasFreq,Impulse.kr(1));
			//longAverage = AverageOutput.kr(hasFreq,Impulse.kr(long_win.reciprocal));
			//[shortAverage, longAverage];
			//shortAverage;
			SendReply.kr(Impulse.kr(1), ("/"++channel++"/clarity/").asSymbol, hasFreq);
			hasFreq;
		};

		tempo = NodeProxy.control(Server.default, 1)
		.source = {
			//add some variation of BeatTrack here
		};

		gridanalysis = NodeProxy.control(Server.default, 3)
		.source = {
			|lag=30, densityWarp = 0, clarityWarp = 0, ampWarp = 0|
			var d = this.density.kr(1,0).lag(lag),
			c = this.clarity.kr(1,0).lag(lag);

			// scale these to 0 - 1 and curve
			c = c.clip.lincurve(0, 1, 0, 1, clarityWarp);
			//a = (a / 40).clip.lincurve(0, 1, 0, 1, ampWarp);
			d = (d / 6).clip.lincurve(0, 1, 0, 1, densityWarp);

			SendReply.kr(Impulse.kr(1), ("/"++channel++"/x/").asSymbol, c);
			SendReply.kr(Impulse.kr(1), ("/"++channel++"/y/").asSymbol, d);

			Out.kr(xbus, c);
			Out.kr(ybus, d);
			//Out.kr(this.xbus, c.linlin(0, 1, -0.5, 0.5) * 1 + 0.5);
			//Out.kr(this.ybus, d.linlin(0, 1, -0.5, 0.5) * 1 + 0.5);
			Out.kr(ampThreshBus, -999);
		};

	}

	analysisGUI { |window|
		var flow, processingView, densityView, clarityView, ampView, xView, yView, lagView, layout,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		analysisView = View.new(window, (labelWidth+valueWidth+20+(valueWidth/3))@(lineHeight*9.5));//If you need more vertical space adjust that here
		flow = analysisView.addFlowLayout();
		StaticText(analysisView, Rect(10,10,labelWidth,lineHeight))
		.string_(("Channel "++((this.name.asInteger)))).align_(\left);
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("processing: ").align_(\right);
		processingView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("density: ").align_(\right);
		densityView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("clarity: ").align_(\right);
		clarityView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("amp: ").align_(\right);
		ampView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		StaticText(analysisView, labelWidth@lineHeight).string_("X: ").align_(\right);
		xView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		StaticText(analysisView, labelWidth@lineHeight).string_("Y: ").align_(\right);
		yView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		StaticText(analysisView, labelWidth@lineHeight).string_("Grid Lag Set").align_(\right);
		lagView = NumberBox(analysisView, Rect(10,10,valueWidth,lineHeight)).value_(30)
		.action_({|val| gridanalysis.set(\lag, val)});
		OSCdef(("c"++channel.asString++"clarity"++"OSC").asSymbol, {|msg|
			defer({
				clarityView.value = msg[3].round(0.01);
			})
		}, ("/"++channel++"/clarity/").asSymbol);

		OSCdef(("c"++channel.asString++"density"++"OSC").asSymbol, {|msg|
			defer({
				densityView.value = msg[3];
			})
		}, ("/"++channel++"/density/").asSymbol);

		OSCdef(("c"++channel.asString++"amplitude"++"OSC").asSymbol, {|msg|
			defer({
				ampView.value = msg[3].round(0.01);
			})
		}, ("/"++channel++"/amplitude/").asSymbol);

		OSCdef(("c"++channel.asString++"xcoord"++"OSC").asSymbol, {|msg|
			defer({
				xView.value = msg[3].round(0.01);
				//msg.postln;
			})
		}, ("/"++channel++"/x/").asSymbol);

		OSCdef(("c"++channel.asString++"ycoord"++"OSC").asSymbol, {|msg|
			defer({
				yView.value = msg[3].round(0.01);
			})
		}, ("/"++channel++"/y/").asSymbol);
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
			\y -> yView,
			\lag -> lagView
		];
		^analysisView;
	}


	/*///////////////////////////////////////////////////////

	METHODS FOR ADDING AND RUNNING PROCESSORS

	*////////////////////////////////////////////////////////

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
		(changeProcessingView.isNil.not).if({
			this.refreshProcessorView;
		});
	}

	runProcessor {|key|
		var processor = this.processors[key];

		(processor.isNil.not).if({
			//processing.source.fadeTime = processor[\fadeTime];
			processing.source = processor[\func].value(this);
			(this.name++": " ++ "Processing is " ++ processor[\name]).postln;
			current_processing = processor[\name];
			current_processingKey = processor[\key];
		}, {
			"Processor does not exist".postln;
		});

		(this.modulatorView.isNil.not).if({
			this.modulatorGUI;
		});

		(this.analysisView.isNil.not).if({
			this.analysisView[\processing].string_(this.current_processing);
		});
	}

	assignProcessorToQuadrant {|key, quadrant|
		// key can be a symbol or an array of symbols;
		// i.e., [\reverb, \reverb, \reverb, \pitch]
		this.quadrantProcessors[quadrant - 1] = key;
	}

	processingGUI { |window|
		var flow, popUp, items, index,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		changeProcessingView = View.new(window, (labelWidth+valueWidth+20)@(lineHeight));
		flow = changeProcessingView.addFlowLayout();
		//StaticText(changeProcessingView)
		StaticText(changeProcessingView, Rect(10,10,labelWidth,lineHeight))
		.string_("Change Processing: ");
		popUp = PopUpMenu(changeProcessingView, Rect(10,10,valueWidth,lineHeight));
		popUp.items_(this.processors.keys.asArray.sort);
		popUp.action_({|menu,item|
			this.runProcessor(menu.item.asSymbol);
		});
		items = this.processors.values.collect({|val| val[\key] }).asArray.sort;
		index = items.indexOf(\silence);
		popUp.valueAction_(index);
		//.items(this.processors.keys.asArray);
		//popUp.action({|menu| [menu.value,menu.item].postln;});

		changeProcessingView = Dictionary[
			\view -> changeProcessingView,
			\menu -> popUp
		];
		^changeProcessingView;
	}

	refreshProcessorView {
		var items = this.processors.values.collect({|val| val[\key] }).asArray.sort,
		index = items.indexOf(this.current_processingKey) ? items.indexOf(\silence);

		this.changeProcessingView[\menu]
		.items_(items)
		.value_(index);
	}

	/*///////////////////////////////////////

	METHODS FOR ADDING AND RUNNING MODULATORS

	*////////////////////////////////////////

	getModulatorNode {|key|
		^this.modulators[key][\node];
	}

	addModulator {|name, key, assocProc, func, reversePolarityFunc, lagTime=0, fadeTime=1, spec|
		(this.modulators.isNil).if({
			modulators = Dictionary.new;
		});
		(this.modulators[key].isNil.not).if({
			this.modulators.removeAt(key);
			this.modulators[key][\tracker].clear;
			("removed "++key++" from modulators").postln;
		});
		this.modulators.put(key, Dictionary[
			\name -> name,
			\func -> func,
			\assocProc -> assocProc,
			\reversePolarityFunc -> reversePolarityFunc,
			\fadeTime -> fadeTime,
			\node -> NodeProxy(Server.default, 'control', 1),
			\tracker -> NodeProxy(Server.default, 'control', 1),
			\lagTime -> lagTime,
			\spec -> spec
		]);
		this.runModulator(key.asSymbol);
		this.addTracker(key.asSymbol);
		(changeProcessingView.isNil.not).if({
			this.refreshProcessorView;
		});
	}

	runModulator {|key|
		var modulator = this.modulators[key],
		node = modulator[\node],
		funcKey = (this.polarity).if({ \reversePolarityFunc }, { \func });

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

	addTracker {|key,rate=10|
		var modulator = this.modulators[key],
		node = modulator[\node],
		tracker = modulator[\tracker];
		tracker.source = {
			SendReply.kr(Impulse.kr(rate), ("/"++channel++"/"++key).asSymbol, node.kr(1,0));
		};

		("The Tracker key is: "++key).postln;
	}

	getAssocModulators { |assocKey|
		var mods;
		//return all of the keys for modulators associated with the given processing type

		mods = this.modulators.keys.select({|key|
			(this.modulators[key][\assocProc].isArray).if({
				this.modulators[key][\assocProc].includes(assocKey)},{
				this.modulators[key][\assocProc] == assocKey});
		});
		^mods;
	}

	modulatorGUI { |window|
		var flow, lineHeight = 25, labelWidth = 150, valueWidth = 100;

		modulatorView.isNil.if({
			modulatorView = View.new(window, (labelWidth+labelWidth+valueWidth+30)@(lineHeight*8));
			flow = modulatorView.addFlowLayout();
		});
		modulatorView.removeAll;
		modulatorView.decorator.reset;
		//The idea for this Gui is to be able to see the analysis inputs getting mapped to paramteters and perhaps be able to adjust the mapping? Via an iEnvGen?
		StaticText(modulatorView, Rect(0,0,labelWidth, lineHeight))
		.string_((this.current_processing++" Mods"));
		StaticText(modulatorView, Rect(0,0,valueWidth,lineHeight))
			.string_("");
	StaticText(modulatorView, Rect(0,0,valueWidth/3,lineHeight))
			.string_("");
		StaticText(modulatorView, Rect(0,0,valueWidth/3,lineHeight))
		.string_("Lag");
		modulatorView.decorator.nextLine;
		this.getAssocModulators(current_processingKey).do({|key|
			var slider, nbox, lagbox;
			StaticText(modulatorView, Rect(0,0,labelWidth, lineHeight))
			.string_(this.modulators[key][\name]);
			slider = Slider.new(modulatorView, Rect(0,0,valueWidth, lineHeight));
			nbox = NumberBox(modulatorView, Rect(0,0,valueWidth/3,lineHeight));
			OSCdef(("c"++channel.asString++key++"OSC").asSymbol, {|msg|
				defer({
					slider.value = this.modulators[key][\spec].unmap(msg[3]);
					nbox.value = msg[3];
				})
			}, ("/"++channel++"/"++key).asSymbol);
			lagbox = NumberBox(modulatorView, Rect(0,0,valueWidth/3,lineHeight)).clipLo_(0).value_(this.modulators[key][\lagTime]);
			lagbox.action = {
				|box|
				var node;
				//get modulator node
				node = this.getModulatorNode(key);
				//set lagtime
				this.modulators[key][\lagTime] = box.value;
				node.set(\lag, this.modulators[key][\lagTime]);
				(key++" node lagtime set to "++box.value).postln;
			};
		});
			modulatorView.decorator.nextLine;
		^modulatorView;

	}

	/*/////////////////////////////////////////

	METHODS FOR ADDING AND RUNNING SPATIALIZERS

	*//////////////////////////////////////////

	//The spatializers are now multichannel Audio Nodeproxies that take in the processed audio coming from the output NodeProxy and spatialize the sound with the custom BeamFormer class. The number of output speakers must be known to the instance of Sway. Depending on the type of spatializer the controls are generally either angle, focal point, or delay transformation function

	initialXPositions { |y=3|
		var len, xPos, num, out;
		len = array.arrayLength*0.95;
		num = SwayConductor.numPlayers;
		xPos = Array.fill(num, { |i|
		(i - ((num - 1) / 2)) * (len / (num - 1))
		}); //evenly spread the players across the array
		^xPos;
	}

	addSpatializer {|name, key, func, fadeTime = 1|
		(this.spatializers.isNil).if({
			spatializers = Dictionary.new;
		});
		this.spatializers.put(key, Dictionary[
			\name -> name,
			\key -> key,
			\func -> func,
			\fadeTime -> fadeTime
		]);

		(changeSpatializationView.isNil.not).if({
			this.refreshSpatializerView;
		});

	}

	runSpatializer {|key|
		var spatializer = this.spatializers[key];

		(spatializer.isNil.not).if({
			//processing.source.fadeTime = processor[\fadeTime];
			beamformer.source = spatializer[\func].value(this);
			(this.name++": " ++ "Spatializer is: " ++ spatializer[\name]).postln;
			current_spatializer = spatializer[\name];
			current_spatializerKey = spatializer[\key];
		}, {
			"Spatializer does not exist".postln;
		})
	}

	refreshSpatializerView {
		var items = this.spatializers.values.collect({|val| val[\key] }).asArray.sort,
		index = items.indexOf(this.current_spatializerKey) ? items.indexOf(\static);

		this.changeSpatializationView[\menu]
		.items_(items)
		.value_(index);
	}

	spatializationGUI { |window|
		var flow, popUp, items, index,
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
		items = this.spatializers.values.collect({|val| val[\key] }).asArray.sort;
		index = items.indexOf(\static);
		popUp.valueAction_(index);
		//.items(this.processors.keys.asArray);
		//popUp.action({|menu| [menu.value,menu.item].postln;});

		changeSpatializationView = Dictionary[
			\view -> changeSpatializationView,
			\menu -> popUp
		];
		^changeSpatializationView;
	}

	getQuadrant {|x,y|

		if((x>=0.5)&&(y>=0.5),{quadrant=1},
			if((x>=0.5)&&(y<0.5),{quadrant=2},
				if((x<0.5)&&(y<0.5),{quadrant=3},
					if((x<0.5)&&(y>=0.5),{quadrant=4}
		))));
		^quadrant;
	}

	addToHistory {
		timeHistory.addFirst(SystemClock.seconds);
		processorHistory.addFirst(this.current_processingKey);
		xbus.get({|x|
			ybus.get({|y|

				quadrantHistory.addFirst(
					this.getQuadrant(x, y);
				);
				(quadrantHistory.size>1).if({this.quadrantAction()});
			});
		});
	}

	scheduleGetQuadrant {
		{
			"waiting for read time".postln;
			quadrantReadTime.wait;
			"adding quadrant to History".postln;
			this.addToHistory();
			(this.loopQuadrantRead).if({
				this.scheduleGetQuadrant();
			});
		}.fork(AppClock)
	}

	assignQuadrant {|quadrant, processorKey|
		this.quadrantProcessors;
	}

	quadrantAction {
		var current = this.quadrantHistory[0],
		previous = this.quadrantHistory[1],
		instances = this.quadrantHistory
		.select({|v| v == current })
		.size,
		threshold = this.quadrantProcessingThresholds[current - 1];
		(quadrantHistory.size<3).if({	{this.runProcessor((this.quadrantProcessors[current-1]).asSymbol)}.defer;
		});
		(instances > threshold).if({
			// change processing
			//right now the new assignment is just random and it can re-assign the same processing randomly
			//* ADD Function to change the processing
			// increase threshold
			this.quadrantProcessingThresholds[current - 1] = threshold + (100 / this.quadrantReadTime);
		}, {
			(current != previous).if({
				// change to assigned processing
				{this.runProcessor((this.quadrantProcessors[current - 1]).asSymbol)}.defer;
			});
		});
	}

	saveHistory {
	var file, path, history;
	path = thisProcess.nowExecutingPath;
	history = [timeHistory,quadrantHistory,processorHistory];
	history.writeTextArchive((path++channel++"_history.txt").standardizePath);
	("History saved to "++"C:/Users/Carl/Desktop/"++channel++"_history.txt").postln
	}

	/*/////////////////

	NOT YET IMPLEMENTED

	*//////////////////

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


}