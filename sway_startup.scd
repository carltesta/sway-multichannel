//Sway Startup File

(
s.waitForBoot({
	var w, f, numChannels=SwayConductor.numPlayers,
	dir = PathName.new(thisProcess.nowExecutingPath),
	processingDirectory = PathName.new(dir.pathOnly++"processors/"),
	spatializersDirectory = PathName.new(dir.pathOnly++"spatializers/");

	MIDIClient.init;

	s.sync;
	1.0.wait;

	MIDIIn.connectAll;

	s.sync;

	1.0.wait;

	SynthDef(\grain, {|bufnum = 0, pointer = 0, freqScale = 1, windowSize = 0.2, amp = 1, out = 0, x=0, y=2|
		var sig, env, beam;
		env = EnvGen.ar(Env.sine(windowSize), gate: 1, doneAction: 2);
		sig = BufRd.ar(1, bufnum, LFSaw.ar(BufDur.ir(bufnum).reciprocal * freqScale, 0, BufFrames.ir(bufnum), pointer * BufFrames.ir(bufnum)), 1.0); // range of this is pointer * bufframes + bufframes
		beam = BeamFormer.arFocal(sig, x, y, numSpeakers: 64);
		Out.ar(out, beam*amp);
	}).add;

	s.sync;
	1.0.wait;

	~swayInstances = Array.newClear(numChannels);
	~swayInstances = ~swayInstances.collect({SwayS.new});
	~swayInstances.do({|sway, n| sway.init(n.asSymbol,n,1)});

	s.sync;

	1.0.wait;

	("Loading Sway Processors and Modulators in Directory: /"++processingDirectory.folderName).postln;
	processingDirectory.filesDo({|file|
		if(file.fullPath.asString.includes("ignore"),
			{("ignored"++file).postln;},
			{file.fullPath.asString.load;})
	});
	("Loading Sway Spatializers in Directory: /"++spatializersDirectory.folderName).postln;
	spatializersDirectory.filesDo({|file| file.fullPath.asString.load;});

	s.sync;

	1.0.wait;

	//set up channels and analysis inputs
	~swayInstances.do({|sway, n|
		sway.input.set(\chan, n);
		sway.analysis_input.set(\chan, n);
	});



	~swayInstances.do({|sway, n|
		//sway.runProcessor(\silence);
		//sway.analysis_input.source = {~playback[n].ar(1)};
		//sway.input.source =  {~playback[n].ar(1)};
		//sway.output.play();
		//for spatialization you have to play the beamformer nodeproxy
		sway.beamformer.play();

		//add initial processors to Quadrants
		//sway.quadrantProcessors.collectInPlace({sway.processors.keys.choose});
		sway.scheduleGetQuadrant;
	});

	s.sync;

	1.0.wait;

	w = Window.new("Sway", Rect(50,75,800,500), scroll:true);
	f = w.addFlowLayout();
	~swayInstances.do({|sway, n|
		sway.audioGUI(w);
		f.nextLine;
		sway.analysisGUI(w);
		sway.modulatorGUI(w);
		f.nextLine;
		sway.processingGUI(w);
		f.nextLine;
		sway.spatializationGUI(w);
		f.nextLine;
		//~sway.gridGUI(w);
		//~sway.analysisGUI(w);
		//sway.syncGUI();
		sway.refreshProcessorView();
	});
	w.front;

	s.sync;
	1.0.wait;

	//Route outputs to Subs
	~swayInstances.do({|sway, n|
		sway.output.playN([64,65])
	});

	//Route Processing to AUX Outputs for Recording
	~swayInstances.do({|sway, n|
		sway.output.playN(66+n);
	});

	//Load SwayConductor Configuration
	//(dir.pathOnly.asString++"swayConductor_Config.scd").load;

	s.volume_(-6);

	1.0.wait;

	~swayInstances.do({|sway, n|
		sway.loadSettings;
	});

	CmdPeriod.doOnce({
		~swayInstances.do({|sway, n|
			sway.captureSettings;
			OSCdef.clear;
			OSCdef.freeAll;
			//sway.stopSyncGUI();
			sway.saveHistory;
		});
		w.close;

	});
});
)

/*
~swayInstances.do({|sway, n|
sway.runSpatializer(\circle);
});

~swayInstances.do({|sway, n|
sway.runProcessor(\pitchbend);
});

*/

~swayInstances[1].output.index;

~swayInstances[0].position.source.asCompileString
~swayInstances[0].position.numChannels