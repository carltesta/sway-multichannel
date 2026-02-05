//Filter Processor for Sway

~swayInstances.do({|sway, n|
	sway.addProcessor("Filter", \filter, {|swayInstance|
	{
		var filtfreq = swayInstance.getModulatorNode(\filtfreq);
		var filtrq = swayInstance.getModulatorNode(\filtrq);
		var out = RLPF.ar(swayInstance.input.ar(1), filtfreq.kr(1), filtrq.kr(1)).tanh;
		out;
	}
});
});

//control mapping:
//density -> filtfreq
//amplitude -> filtrq
~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Filter Freq", \filtfreq, \filter,
		{|swayInstance|
	{ swayInstance.density.kr(1,0).linexp(0,6,500,5000) }
}, {|swayInstance|
	{ swayInstance.density.kr(1,0).linexp(0,6,5000,5) }
},
		spec: ControlSpec(500,5000,\exp,0.01)
	);

sway.addModulator("Amp -> Filter RQ", \filtrq, \filter,
		{|swayInstance|
			{ swayInstance.amplitude.kr(1,0).lag(30).linlin(0,10,0.3,0.8) }
}, {|swayInstance|
			{ swayInstance.amplitude.kr(1,0).lag(30).linlin(0,10,0.9,0.3) }
},
		spec: ControlSpec(0.3,0.8,\lin,0.01)
	);
});

("Filter Processor and Modulators Added").postln;
