/*

Ideas about how to set up the quadrants so that changes can be made based on things that are not just random.

For example, each quadrant will have processor associations based on the current polarity. So the noisy dense quadrant would have granular synthesis allogrithms when polarity is off, and amplitude modulation when polarity is on.

processors can have associated spatializers so that when a certain processor is started it can activate a particular spatializer

I need to learn how to create spatializer patterns via code
*/

SwayS {

	var <>name, <>channel=0, <>numChan, <>input, <>dry, <>analysis_input, <>buffer, <>processing, <>recorder, <>delaybuffer, <>beamformer, <>stereomix, <>refresh_rate=1, <>fade=30, <>output, <>polarity=false,
	<>density, <>clarity, <>amplitude, <>tempo, <>gridanalysis, <>xbus, <>ybus, <>ampThreshBus, <>ampThresh=0.01, <>clarityThresh=0.5, <>densityThresh=0.5,
	<current_processing, <current_processingKey=\silence, <>processors, <>modulators, <current_spatializer, <current_spatializerKey=\static, <>spatializers, <>audioView, <>changeSpatializationView, <>analysisView, <>changeProcessingView, <>modulatorView, <>gridView, <>quadrantHistory, <>processorHistory, <>timeHistory, spatializerHistory, <quadrant=nil, <>loopQuadrantRead=true, <>quadrantReadTime=10, <>amplitudeControl=false,
	<>quadrantProcessingThresholds,
	<>quadrantProcessors, <>quadrantSpatializers, <array, <xPosition, <yPosition, <angle=nil, <>numSpeakers=64, <>speakerSpacing=0.07, <>speedOfSound=343, <>settings;

	new {
		this.init;
	}

	init {
		|givenName="sway",chan=0,numChannels=1|

		quadrantProcessingThresholds = Array.fill(4, 100/quadrantReadTime);
		quadrantProcessors = Array.fill(4, \silence);
		quadrantSpatializers = Array.fill(4, \static);

		//assign name of channel
		name = givenName;
		numChan = numChannels;
		channel = chan;

		//histories
		quadrantHistory = List.new;
		processorHistory = List.new;
		spatializerHistory = List.new;
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

		processing = NodeProxy.audio(Server.default, numChan).reshaping_(\elastic).fadeTime_(fade)
		.source = { Silent.ar(1) }; //start with silent processing

		//output

		output = NodeProxy.audio(Server.default, numChan).reshaping_(\elastic).fadeTime_(fade)
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

		//stereomix = NodeProxy.audio(Server.default, 2).fadeTime_(fade);
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
		var flow, outVolView, outVolNum, stereoMix, dryVolView, dryVolNum, dryVolStart, ampThreshView, ampThreshSlider, ampThreshIndicator, ampControlButton, layout,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		audioView = View.new(window, (labelWidth+valueWidth+20+(valueWidth/3)*2)@(lineHeight*5));
		flow = audioView.addFlowLayout();
		StaticText(audioView, Rect(10,10,labelWidth, lineHeight)).string_("Out Vol");
		outVolView = Slider(audioView, Rect(10,10,valueWidth, lineHeight)).action_({
			this.output.set(\volume, outVolView.value);
			outVolNum.value_(outVolView.value.ampdb)});
		outVolNum = NumberBox(audioView, Rect(10,10,valueWidth/3,lineHeight)).value_(outVolView.value.ampdb).action_({|box|outVolView.value_(box.value.dbamp)});

		/*
		stereoMix = Button(audioView, Rect(10,10,lineHeight,lineHeight));
			stereoMix.states_([["Off", Color.black, Color.red],["On", Color.black, Color.green]]);
		stereoMix.action_({|button| if(button.value==1, {stereomix.source_({|in| Splay.ar(In.ar(beamformer.ar(64,0),numSpeakers),level: 1/64)});beamformer.stop; stereomix.play;},{stereomix.stop;beamformer.play;})});
		*/

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

		ampThreshView = NumberBox(audioView,Rect(10,10,valueWidth/3,lineHeight)).decimals_(4).step_(0.0001).value_(this.ampThresh).action_({|box|
			this.ampThresh=box.value;
			ampThreshSlider.valueAction = box.value});

		ampThreshSlider = Slider(audioView, Rect(10,10,valueWidth,lineHeight)).step_(0.0001).value_(this.ampThresh).action_({|slider|
			ampThreshView.value_(slider.value);
			this.ampThresh = slider.value;
			this.amplitude.set(\ampThresh, (slider.value).ampdb);
		});

		ampThreshIndicator = StaticText(audioView,Rect(10,10,lineHeight,lineHeight)).string_("<").background_(Color.red).stringColor_(Color.black).align_(\center);

		flow.nextLine;
		StaticText(audioView, Rect(10,10,labelWidth, lineHeight)).string_("Amp Control");
		ampControlButton = Button(audioView, Rect(10,10,lineHeight, lineHeight));
		ampControlButton.states_([["Off", Color.black, Color.red],["On", Color.black,Color.green]]);
		ampControlButton.action_({|button| if(button.value==1, {amplitudeControl=true}, {amplitudeControl=false})});
		ampControlButton.valueAction_(1);

		OSCdef(("c"++channel.asString++"ampAboveThresh"++"OSC").asSymbol, {|msg|
			if(msg[3]>0,{	defer{ampThreshIndicator.string_(">").background_(Color.green)};

				amplitudeControl.if({
				defer{this.runProcessor(this.quadrantProcessors[this.quadrantHistory[0]+1])};
				//("channel: "++channel++"aboveThresh").postln;
				//msg.postln;
				});
			},
			{
				defer{ampThreshIndicator.string_("<").background_(Color.red)};
				amplitudeControl.if({
				defer{this.runProcessor(\silence)};
				//("channel: "++channel++"belowThresh").postln;
				//msg.postln;
				});
			});
		},
		("/"++channel++"/aboveThresh/").asSymbol);

		OSCdef(("c"++channel.asString++"isAtZero"++"OSC").asSymbol, {|msg|
			if(this.current_processingKey!==\silence,{
				//defer({this.runProcessor(\silence)});
				//"Processing Silenced because at Zero".postln;
			});
		}, ("/"++channel++"/isAtZero/").asSymbol);

		audioView = Dictionary[
			\view -> audioView,
			\outVol -> outVolView,
			\outVolNum -> outVolNum,
			\dryVol -> dryVolView,
			\dryVolNum -> dryVolNum,
			\dryVolButton -> dryVolStart,
			\ampThreshView -> ampThreshView,
			\ampThreshSlider -> ampThreshSlider,
			\ampThreshIndicator -> ampThreshIndicator,
			\ampControlButton -> ampControlButton
		];
		^audioView;

	}

	/*//////////////////////////////////

	ANALYSIS SECTION

	*///////////////////////////////////


	build_analysis {

		density = NodeProxy.control(Server.default, 1)
		.source = {|window=5|
			var chain = FFT(LocalBuf(512), analysis_input.ar(1));
			var onsetTrigger = Onsets.kr(chain);
			var density = OnsetStatistics.kr(onsetTrigger, window);
			SendReply.kr(onsetTrigger + Impulse.kr(1/window), ("/"++channel++"/density/").asSymbol, density);
			density;
		};

		amplitude = NodeProxy.control(Server.default, 3)
		.source = { |ampThresh = -40|
			//Amplitude Tracker
			var amplitude = Lag.kr(Amplitude.kr(analysis_input.ar(1)), 5);
			var isAboveThresh = amplitude > (ampThresh.dbamp);
			var isAtZero = amplitude < (0.001.dbamp);
			var smoothedThresh = EnvGen.kr(Env([0, 1, 1, 0], [0.01, 0, 30], [4, -4], releaseNode: 2), isAboveThresh);
			var chain = FFT(LocalBuf(1024), analysis_input.ar(1));
			var loudness = Loudness.kr(chain);
			var shortAverage = AverageOutput.kr(loudness, Impulse.kr(1/5));
			SendReply.kr(Impulse.kr(1/5), ("/"++channel++"/isAtZero/").asSymbol, isAtZero);
			SendReply.kr(Impulse.kr(5), ("/"++channel++"/amplitude/").asSymbol, amplitude);
			SendReply.kr(Impulse.kr(1/5), ("/"++channel++"/aboveThresh/").asSymbol, smoothedThresh);
			[amplitude, isAboveThresh, shortAverage];
		};

		clarity = NodeProxy.control(Server.default, 1)
		.source = {
			var freq, hasFreq, latch;
			# freq, hasFreq = Tartini.kr(analysis_input.ar(1));
			latch = Latch.kr(hasFreq, Impulse.kr(1/5));//latch every 5 seconds
			SendReply.kr(Impulse.kr(1), ("/"++channel++"/clarity/").asSymbol, latch);
			[hasFreq, latch];
		};

		/*
		tempo = NodeProxy.control(Server.default, 4)
		.source = { |lock=0|
			//add some variation of BeatTrack here
			var buf = LocalBuf.new(1024,1);
			var fft = FFT(buf, analysis_input.ar(1));
			BeatTrack.kr(fft, lock);//
		};
		*/

		gridanalysis = NodeProxy.control(Server.default, 3)
		.source = {
			|lag=30, densityWarp = 0, clarityWarp = 0, ampWarp = 0|
			var d = this.density.kr(1,0).lag(lag),
			c = this.clarity.kr(1,1).lag(lag), //take latch output for grid
			a = this.amplitude.kr(1,0).lag(lag);

			// scale these to 0 - 1 and curve
			c = c.lincurve(0, 1, 0, 1, clarityWarp);
			//a = (a / 40).clip.lincurve(0, 1, 0, 1, ampWarp);
			d = d.lincurve(0, 22, 0, 1, densityWarp);

			SendReply.kr(Impulse.kr(5), ("/"++channel++"/x/").asSymbol, c);
			SendReply.kr(Impulse.kr(5), ("/"++channel++"/y/").asSymbol, d);

			Out.kr(xbus, c);
			Out.kr(ybus, d);
			//Out.kr(this.xbus, c.linlin(0, 1, -0.5, 0.5) * 1 + 0.5);
			//Out.kr(this.ybus, d.linlin(0, 1, -0.5, 0.5) * 1 + 0.5);
			Out.kr(ampThreshBus, -999);
		};

	}

	analysisGUI { |window|
		var flow, processingView, spatializationView, densityView, clarityView, ampView, xView, yView, lagView, xThreshView, yThreshView, layout,
		lineHeight = 25, labelWidth = 150, valueWidth = 100;

		analysisView = View.new(window, (labelWidth+valueWidth+20+(valueWidth/3))@(lineHeight*10.5));//If you need more vertical space adjust that here
		flow = analysisView.addFlowLayout();
		StaticText(analysisView, Rect(10,10,labelWidth,lineHeight))
		.string_(("Channel "++((this.name.asInteger)))).align_(\left);
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("processing: ").align_(\right);
		processingView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("spatialization: ").align_(\right);
		spatializationView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("density: ").align_(\right);
		densityView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("clarity: ").align_(\right);
		clarityView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");
		flow.nextLine;
		StaticText(analysisView, labelWidth@lineHeight).string_("amp: ").align_(\right);
		ampView = TextField(analysisView, Rect(10,10,valueWidth,lineHeight)).string_("");

		StaticText(analysisView, (labelWidth/2)@lineHeight).string_("Clarity Threshold: ").align_(\right);
		xThreshView = NumberBox(analysisView, Rect(10,10,(valueWidth/2),lineHeight)).value_(0.5).clipLo_(0).clipHi_(1.0);
		xThreshView.action_({|numb| clarityThresh=numb.value; gridanalysis.set(\clarityWarp, this.calcMidCurve(numb.value))});

		StaticText(analysisView, (labelWidth/2)@lineHeight).string_("X: ").align_(\right);
		xView = TextField(analysisView, Rect(10,10,(valueWidth/2),lineHeight)).string_("");

		StaticText(analysisView, (labelWidth/2)@lineHeight).string_("Density Threshold: ").align_(\right);
		yThreshView = NumberBox(analysisView, Rect(10,10,(valueWidth/2),lineHeight)).value_(11).clipLo_(0).clipHi_(22);
		yThreshView.action_({|numb| densityThresh=numb.value; gridanalysis.set(\densityWarp, this.calcMidCurve((numb.value).linlin(0,22,0,1)))});
		StaticText(analysisView, (labelWidth/2)@lineHeight).string_("Y: ").align_(\right);
		yView = TextField(analysisView, Rect(10,10,(valueWidth/2),lineHeight)).string_("");

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
				ampView.value = msg[3].round(0.0001);
			})
		}, ("/"++channel++"/amplitude/").asSymbol);

		OSCdef(("c"++channel.asString++"xcoord"++"OSC").asSymbol, {|msg|
			defer({
				xView.value = msg[3].round(0.001);
				//msg.postln;
			})
		}, ("/"++channel++"/x/").asSymbol);

		OSCdef(("c"++channel.asString++"ycoord"++"OSC").asSymbol, {|msg|
			defer({
				yView.value = msg[3].round(0.001);
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
			\spatialization -> spatializationView,
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

			processing.fadeTime = processor[\fadeTime];
			processing.source = processor[\func].value(this);
			(this.name++": " ++ "Processing is " ++ processor[\name]).postln;
			current_processing = processor[\name];
			current_processingKey = processor[\key];

			//update the modulators (perhaps polarity has changed)
			this.getAssocModulators(key).do({|key|
				this.runModulator(key)
			});
		}, {
			//"Processor does not exist".postln;
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
			this.quadrantProcessors=Array.fill(4, menu.item.asSymbol);
			this.audioView[\ampControlButton].valueAction_(0);//set the amp control to false because it is assumed you want direct control if you are changing the processing
		});
		items = this.processors.values.collect({|val| val[\key] }).asArray.sort;
		index = items.indexOf(\silence);
		popUp.value_(index);
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

	addModulator {|name, key, assocProc, func, reversePolarityFunc, lagTime=0, fadeTime=15, spec|
		(this.modulators.isNil).if({
			modulators = Dictionary.new;
		});
		(this.modulators[key].isNil.not).if({
			this.modulators[key][\tracker].clear;
			this.modulators.removeAt(key);
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
			node.fadeTime = modulator[\fadeTime];
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

	setPolarity { |pol=true|
		polarity=pol;
		this.getAssocModulators(current_processingKey).do({|key|
			this.runModulator(key);
		});
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
			beamformer.fadeTime = spatializer[\fadeTime];
			beamformer.source = spatializer[\func].value(this);
			(this.name++": " ++ "Spatializer is: " ++ spatializer[\name]).postln;
			current_spatializer = spatializer[\name];
			current_spatializerKey = spatializer[\key];

			//update the modulators (perhaps polarity has changed)
			//this.getAssocModulators(key).do({|key|
			//	this.runModulator(key)
		}, {
			"Spatializer does not exist".postln;
		});

		(this.modulatorView.isNil.not).if({
			this.modulatorGUI;
		});

		(this.analysisView.isNil.not).if({
			this.analysisView[\spatialization].string_(this.current_spatializer);
		});
	}

	refreshSpatializerView {
		var items = this.spatializers.values.collect({|val| val[\key] }).asArray.sort,
		index = items.indexOf(this.current_spatializerKey) ? items.indexOf(\static);

		this.changeSpatializationView[\menu]
		.items_(items)
		.value_(index);
	}

	assignSpatializerToQuadrant {|key, quadrant|
		// key can be a symbol or an array of symbols;
		// i.e., [\static, \multi, \random, \vibrato]
		this.quadrantSpatializers[quadrant - 1] = key;
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
			this.quadrantSpatializers=Array.fill(4, menu.item.asSymbol);
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

	/*////////////////////////////////

	METHODS FOR MANAGING THE QUADRANTS

	*////////////////////////////////


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

	/*
	assignQuadrant {|quadrant, processorKey|
		this.quadrantProcessors;
	}*/

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
		("History saved to "++"C:/Users/Carl/Desktop/"++channel++"_history.txt").postln;

	}

	//Maybe this is a better way to save the History?
	/*
	~swayInstances.do({|sway,n|

	sway.processorHistory.writeArchive(("~/"++n++"_sway_processor_history.txt").standardizePath);

	sway.timeHistory.writeArchive(("~/"++n++"_sway_time_history.txt").standardizePath);

	sway.quadrantHistory.writeArchive(("~/"++n++"_sway_quadrant_history.txt").standardizePath);

});
	*/

	captureSettings {
		settings = ();
		settings[\dryVol] = audioView[\dryVol].value;
		settings[\outVol] = audioView[\outVol].value;
		settings[\ampThresh] = audioView[\ampThreshView].value;
		settings[\clarityThresh] = analysisView[\clarity].value;
		settings[\densityThresh] = analysisView[\density].value;
		settings[\gridLag] = analysisView[\lag].value;
		Archive.global.put(("swaySettings"++channel).asSymbol, settings);
	}

	loadSettings {
		settings = Archive.global.at(("swaySettings"++channel).asSymbol);
		defer{audioView[\dryVol].valueAction_(settings[\dryVol])};
		defer{audioView[\outVol].valueAction_(settings[\outVol])};
		defer{audioView[\ampThreshView].valueAction_(settings[\ampThresh])};
		defer{analysisView[\clarity].valueAction_(settings[\clarityThresh])};
	    defer{analysisView[\density].valueAction_(settings[\densityThresh])};
		defer{analysisView[\lag].valueAction_(settings[\gridLag])};
	}

	/*//////////////

	HELPER FUNCTIONS

	*///////////////

	calcMidCurve {
		|inX|
		var lo = -20, hi = 20, mid, yVal, target=0.5;
		20.do {
			mid = (lo + hi) / 2;
			yVal = inX.lincurve(0, 1, 0, 1, mid);

			if (yVal > target) {
				lo = mid;
			} {
				hi = mid;
			};
		};
		^mid;
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