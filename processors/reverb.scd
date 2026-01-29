//Reverb Processor for Sway


~swayInstances.do({|sway, n|
	sway.addProcessor("Reverb", \reverb, {|swayInstance|
	{
		var rvmix = swayInstance.getModulatorNode(\rvmix);
		var rvsize = swayInstance.getModulatorNode(\rvsize);
		var rvdamp = swayInstance.getModulatorNode(\rvdamp);
		var out = FreeVerb.ar(in: swayInstance.input.ar(1), mix: rvmix.kr(1), room: rvsize.kr(1), damp: rvdamp.kr(1));
		out;
	}
});
});

//Server.default.sync;

//control mapping:
//density -> mix
//amplitude (30 sec average) -> roomsize
//clarity -> high freq damping

~swayInstances.do({|sway, n|
sway.addModulator("Density -> Mix", \rvmix, \reverb,
		{|swayInstance|
	{ swayInstance.density.kr(1,0).linlin(0,6,0.5,1) }
}, {|swayInstance|
	{ swayInstance.density.kr(1,0).linlin(0,6,1.0,0.5) }
});

sway.addModulator("Amp -> Size", \rvsize, \reverb,
		{|swayInstance|
	{ swayInstance.amplitude.kr(1,1).linlin(0,30,0.3,1) }
}, {|swayInstance|
	{ swayInstance.amplitude.kr(1,1).linlin(0,30,0.9,0.7) }
});

sway.addModulator("Clarity -> HF Damp", \rvdamp, \reverb,
		{|swayInstance|
	{ swayInstance.clarity.kr(1,0).linlin(0,1,1,0) }
}, {|swayInstance|
	{ swayInstance.clarity.kr(1,0).linlin(0,1,0.2,1) }
});
});

("Reverb Processor and Modulators Added").postln;
