//Waveloss Processor for Sway

~swayInstances.do({|sway, n|
	sway.addProcessor("Waveloss", \waveloss, {|swayInstance|
	{

		var preverb, waveloss, reverb;
			var rvmix = swayInstance.getModulatorNode(\rvmix);
			var rvsize = swayInstance.getModulatorNode(\rvsize);
			var rvdamp = swayInstance.getModulatorNode(\rvdamp);
			var wldrop = swayInstance.getModulatorNode(\wldrop);
			var wloutof = swayInstance.getModulatorNode(\wloutof);
			var wlmode = swayInstance.getModulatorNode(\wlmode);
			preverb = FreeVerb.ar(in: swayInstance.input.ar(1), mix: rvmix.kr(1), room: rvsize.kr(1), damp: rvdamp.kr(1));
			waveloss = WaveLoss.ar(preverb, wldrop.kr(1), wloutof.kr(1), wlmode.kr(1));
			reverb = FreeVerb.ar(in: waveloss, mix: rvmix.kr(1), room: rvsize.kr(1), damp: rvdamp.kr(1));
			reverb*1.5;
	}
},fadeTime: sway.fade);
});

//control mapping:
//density -> # of and Size of Waves to Drop
//polarity -> deterministic loss/random loss
~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Waves to Drop", \wldrop, \waveloss,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linexp(0,15,1,35) }
}, {|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linexp(0,15,35,1) }
},
		spec: ControlSpec(1,35,\lin,0.01)
	);

sway.addModulator("Density -> Drop Size", \wloutof, \waveloss,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,15,10,40) }
}, {|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,15,40,10) }
},
		spec: ControlSpec(10,40,\lin,0.01)
	);

	sway.addModulator("Polarity -> Mode (Period/Random)", \wlmode, \waveloss,
		{|swayInstance|
			{ DC.kr(1) }
}, {|swayInstance|
			{ DC.kr(2) }
},
		spec: ControlSpec(1,2,\lin,1)
	);
});

("Waveloss Processor and Modulators Added").postln;
