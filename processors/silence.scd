//Silent Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Silence", \silence, {|swayInstance|
	{
		var out = Silent.ar(swayInstance.numChan);
		out;
	}
},fadeTime: sway.fade);
});

//Server.default.sync;

("Silent Processor Added").postln;
