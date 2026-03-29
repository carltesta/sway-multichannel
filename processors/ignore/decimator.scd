//Smooth Decimator Processor for Sway
/*

//SmoothDecimator

~swayInstances.do({|sway, n|
sway.addProcessor("Decimator", \decimator, {|swayInstance|
	{

		var decsmooth = swayInstance.getModulatorNode(\decsmooth);
		var decsamplerate = swayInstance.getModulatorNode(\decsamplerate);
		var out = SmoothDecimator.ar(swayInstance.input.ar(1,0),decsamplerate.kr(1), decsmooth.kr(1,0));
		out;
	}
});
});

//control mapping:
//clarity -> smoothing
//density -> samplerate

~swayInstances.do({|sway, n|
sway.addModulator("Clarity -> Smoothing", \decsmooth, \decimator,
		{|swayInstance|
		{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,6,0.01,1)}
},
	{|swayInstance|
		{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,6,1,0.01) }
},
		spec: ControlSpec(0,1,\lin,0.01),

	);

sway.addModulator("Density -> Sample Rate", \decsamplerate, \decimator,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,3,44100,(44100/64))}
},
	{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,3,(44100/64),44100) }
},
		spec: ControlSpec((44100/64),44100,\exp,0.01,44100),

	);

});

("Decimator Processor Added").postln;
*/