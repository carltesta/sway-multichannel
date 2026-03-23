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
	sway.addModulator("Density -> Mix", \rvmix, [\reverb, \waveloss],
		{|swayInstance|
			{
				|lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,6,0.5,1) }
}, {|swayInstance|
	{
				|lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,6,1.0,0.5) }
},
		spec: ControlSpec(0.5,1,\lin,0.01)
	);

sway.addModulator("Amp -> Size", \rvsize, [\reverb, \waveloss],
		{|swayInstance|
	{ |lag=30| swayInstance.amplitude.kr(1,0).linlin(0,30,0.3,1) }
}, {|swayInstance|
	{ |lag=30| swayInstance.amplitude.kr(1,0).linlin(0,30,0.9,0.7) }
},
		lagTime: 30,
		spec: ControlSpec(0,1,\lin,0.01)
	);

sway.addModulator("Clarity -> HF Damp", \rvdamp, [\reverb, \waveloss],
		{|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,1,0) }
}, {|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,0.2,1) }
},
		spec: ControlSpec(0,1,\lin,0.01)
	);
});

("Reverb Processor and Modulators Added").postln;
