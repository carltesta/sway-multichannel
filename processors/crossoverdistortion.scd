//CrossOver Distortion Processor for Sway


//CrossoverDistortion
/*
~swayInstances.do({|sway, n|
sway.addProcessor("Silence", \silence, {|swayInstance|
	{
		var out = Silent.ar(swayInstance.numChan);
		out;
	}
});
});

//Server.default.sync;

//("Silent Processor Added").postln;
