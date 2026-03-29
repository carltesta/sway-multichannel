//Disintegrator Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Disintegrator", \disintegrator, {|swayInstance|
	{

		var probability = sway.getModulatorNode(\disdensprob)+sway.getModulatorNode(\disclarprob);
		var multiplier = sway.getModulatorNode(\disdensmul)+sway.getModulatorNode(\disclarmul);
		var out = Disintegrator.ar(sway.input.ar(1), probability, multiplier);
		out;
	}
}, fadeTime: sway.fade);
});

//control mapping:
// density -> probability
// density -> multiplier
// clarity -> probability
// clarity -> multiplier


~swayInstances.do({|sway, n|
sway.addModulator("Density -> Probability", \disdensprob, \disintegrator,
		{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,0.1,1.0)}
},
	{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,1.0,0.1) }
	}, lagTime: 6,
		spec: ControlSpec(0.1,1.0,\lin,0.01),

	);

sway.addModulator("Density -> Multiplier", \disdensmul, \disintegrator,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,1.neg,0.25) }
},
{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,0.25,1.neg) }
}, lagTime: 8,
		spec: ControlSpec( -1,0.25,\lin,0.01)
	);

sway.addModulator("Clarity -> Probability", \disclarprob, \disintegrator,
		{|swayInstance|
		{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0.0,1.0,0,0.5)}
},
	{|swayInstance|
		{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0.0,1.0,0.5,0) }
	}, lagTime: 4,
		spec: ControlSpec(0.0,0.5,\lin,0.01),

	);

sway.addModulator("Clarity -> Multiplier", \disclarmul, \disintegrator,
		{|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0.0,1.0,1.neg,0.3) }
},
{|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0.0,1.0,0.3,1.neg) }
}, lagTime: 10,
		spec: ControlSpec( -1.0,0.3,\lin,0.01)
	);

});

//Server.default.sync;

("Disintegrator Processor Added").postln;
