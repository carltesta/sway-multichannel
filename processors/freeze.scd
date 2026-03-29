//Freeze Processor for Sway
~swayInstances.do({|sway, n|
	sway.addProcessor("Freeze", \freeze, {|swayInstance|
	{

			var trig, time, on, off, out, verb;
			trig = sway.getModulatorNode(\freezetrig);
			time = sway.getModulatorNode(\freezeverbtime);
			off = ToggleFF.kr(trig);
			on = Lag3UD.kr(SetResetFF.kr(trig,off),0.5);
			out = Mix.ar(MiVerb.ar(swayInstance.input.ar(1,0)*off, 0.7, 1.0, 0.2, 0.05, on, mul: on));
			verb = Mix.ar(MiVerb.ar(out, time, 1, 0.2, 0.05, 0));
			verb;
		}
}, fadeTime: 30);
});

//control mapping:
//density ->
//amplitude -> trigger freeze when above amplitude
//clarity ->

~swayInstances.do({|sway, n|
sway.addModulator("Amp -> Freeze", \freezetrig, \freeze,
		{|swayInstance|
	{ swayInstance.amplitude.kr(1,2).linlin(19,20,0,1) }
}, {|swayInstance|
	{ swayInstance.amplitude.kr(1,2).linlin(19,20,1,0) }
},
		spec: ControlSpec(0,1,\lin,1)
	);

sway.addModulator("Density -> Reverb Time", \freezeverbtime, \freeze,
		{|swayInstance|
	{ swayInstance.density.kr(1,0).linlin(0,20,0.5,0.9) }
}, {|swayInstance|
	{ swayInstance.density.kr(1,0).linlin(0,20,0.9,0.3) }
},
		spec: ControlSpec(0.3,0.9,\lin,0.1)
	);
});

("Freeze Processor and Modulators Added").postln;


