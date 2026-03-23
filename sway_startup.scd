//Sway Startup File

(
s.waitForBoot({
	var w, f, numChannels=SwayConductor.numPlayers,
	dir = PathName.new(thisProcess.nowExecutingPath),
	processingDirectory = PathName.new(dir.pathOnly++"processors/"),
	spatializersDirectory = PathName.new(dir.pathOnly++"spatializers/");
	~swayInstances = Array.newClear(1);
	~swayInstances = ~swayInstances.collect({SwayS.new});
	~swayInstances.do({|sway, n| sway.init(n.asSymbol,n,1)});

	s.sync;

	("Loading Sway Processors and Modulators in Directory: /"++processingDirectory.folderName).postln;
	processingDirectory.filesDo({|file| file.fullPath.asString.load;});
	("Loading Sway Spatializers in Directory: /"++spatializersDirectory.folderName).postln;
	spatializersDirectory.filesDo({|file| file.fullPath.asString.load;});

	s.sync;

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
		sway.quadrantProcessors.collectInPlace({sway.processors.keys.choose});
		sway.scheduleGetQuadrant;
	});

	s.sync;

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
		//~sway.analysisGUI(w);*/
		//sway.syncGUI();
		sway.refreshProcessorView();
	});
	w.front;

	s.sync;

	//Route outputs to Subs
	~swayInstances.do({|sway, n|
		sway.output.playN([64,65])
	});

	//Route Processing to AUX Outputs for Recording
	~swayInstances.do({|sway, n|
		sway.output.playN(66+n);
	});

	s.volume_(-6);

	CmdPeriod.doOnce({
		w.close;
		~swayInstances.do({|sway, n|
			OSCdef.freeAll;
			//sway.stopSyncGUI();
			//sway.saveHistory;
		});
	});
});
)

~swayInstances.do({|sway, n|
	var file;
	file = File("~/"++n++"_sway_history.txt".standardizePath, "w");
	file.write(([sway.quadrantHistory,sway.processorHistory]).asString);
	file.close;
});

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