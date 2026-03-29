//CrossOver Distortion Processor for Sway
/*
~swayInstances.do({|sway, n|
sway.addProcessor("Crossover Distortion", \codistortion, {|swayInstance|
	{

		var amp = sway.getModulatorNode(\codistamp);
		var smooth = sway.getModulatorNode(\codistsmooth);
		var out = CrossoverDistortion.ar(sway.input.ar(1), amp, smooth);
		out;
	}
}, fadeTime: sway.fade);
});

//control mapping:
// density -> amp
// clarity - > smooth


~swayInstances.do({|sway, n|
sway.addModulator("Density -> Amp", \codistamp, \codistortion,
		{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,0,1.0)}
},
	{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,0.9,0.1) }
	}, lagTime: 10,
		spec: ControlSpec(0.1,0.9,\lin,0.01),

	);

sway.addModulator("Clarity -> Smooth", \codistsmooth, \codistortion,
		{|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1.0,0,0.95) }
},
{|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,0.95,0.0) }
}, lagTime: 30,
		spec: ControlSpec(0.0,0.95,\lin,0.01)
	);

});

//Server.default.sync;

("Crossover Distortion Processor Added").postln;
*/