//Freeze Processor for Sway
/*

~swayInstances.do({|sway, n|
	sway.addProcessor("Freeze", \freeze, {|swayInstance|
	{
			//time can vary with density
			//maybe there should be some changing probability that the freeze will happen
			//maybe there should be a freeze when a definite pitch is detected?
			//maybe whether the freeze happens with pitch or without could be determined by the current polarity

//probability can be determined by amplitude+ clarity?

//length of freeze?


			//MiVerb.ar(swayInstance.input.ar(1,0), time, drywet, damp, hp, freeze);
	}
});
});

//control mapping:
//density ->
//amplitude ->
//clarity ->
/*
~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Mix", \rvmix, [\reverb, \waveloss],
		{|swayInstance|
			{
				swayInstance.density.kr(1,0).linlin(0,6,0.5,1) }
}, {|swayInstance|
	{
				swayInstance.density.kr(1,0).linlin(0,6,1.0,0.5) }
},
		spec: ControlSpec(0.5,1,\lin,0.01)
	);

sway.addModulator("Amp -> Size", \rvsize, [\reverb, \waveloss],
		{|swayInstance|
	{ swayInstance.amplitude.kr(1,1).linlin(0,30,0.3,1) }
}, {|swayInstance|
	{ swayInstance.amplitude.kr(1,1).linlin(0,30,0.9,0.7) }
},
		spec: ControlSpec(0,1,\lin,0.01)
	);

sway.addModulator("Clarity -> HF Damp", \rvdamp, [\reverb, \waveloss],
		{|swayInstance|
	{ swayInstance.clarity.kr(1,0).linlin(0,1,1,0) }
}, {|swayInstance|
	{ swayInstance.clarity.kr(1,0).linlin(0,1,0.2,1) }
},
		spec: ControlSpec(0,1,\lin,0.01)
	);
});

("Reverb Processor and Modulators Added").postln;
*/