//Sway Startup File

WFSLib.startup;
(
s.waitForBoot({
    var w, f, numChannels=2,
    dir = PathName.new(thisProcess.nowExecutingPath),
    processingDirectory = PathName.new(dir.pathOnly++"processors/"),
	spatializersDirectory = PathName.new(dir.pathOnly++"spatializers/");
	~swayInstances = Array.newClear(numChannels);
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
	sway.output.play();
	});

    s.sync;

	w = Window.new("Sway", Rect(50,75,1200,700), scroll:true);
    f = w.addFlowLayout();
    ~swayInstances.do({|sway, n|
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

    CmdPeriod.doOnce({
        w.close;
        ~swayInstances.do({|sway, n|
		//sway.stopSyncGUI();
		});
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