//Filter Processor for Sway

~swayInstances.do({|sway, n|
	sway.addProcessor("Filter", \filter, {|swayInstance|
	{
		var filtfreq = swayInstance.getModulatorNode(\filtfreq);
		var filtrq = swayInstance.getModulatorNode(\filtrq);
		var out = RLPF.ar(swayInstance.input.ar(1), filtfreq.kr(1), filtrq.kr(1)).tanh;
		out;
	}
},fadeTime: sway.fade);
});

//control mapping:
//density -> filtfreq
//amplitude -> filtrq
~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Filter Freq", \filtfreq, \filter,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linexp(0,20,500,5000) }
}, {|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linexp(0,20,5000,500) }
},
		spec: ControlSpec(500,5000,\exp,0.01)
	);

sway.addModulator("Amp -> Filter RQ", \filtrq, \filter,
		{|swayInstance|
			{ |lag=15|swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,10,0.3,4.0) }
}, {|swayInstance|
			{ |lag=15|swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,10,4.0,0.3) }
}, lagTime: 15,
		spec: ControlSpec(0.3,4.0,\lin,0.01)
	);
});

("Filter Processor and Modulators Added").postln;
