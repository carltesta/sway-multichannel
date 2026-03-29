//Pitch Bend Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Pitch Bend", \pitchbend, {|swayInstance|
	{
		var pbbend = swayInstance.getModulatorNode(\pbbend);
		var pbtime = swayInstance.getModulatorNode(\pbtime);
		//var pbdelay = swayInstance.getModulatorNode(\pbdelay);
		var pitch =	PitchShift.ar(swayInstance.input.ar(1), 1, pbbend.kr(1), 0.2, pbtime.kr(1));
		//var pitch = XFadeDelay.ar(PitchShift.ar(swayInstance.input.ar(1), 1, pbbend.kr(1), 0.2, pbtime.kr(1)), 2, pbdelay);
		pitch;
	}
},fadeTime: sway.fade);
});

//Server.default.sync;

~swayInstances.do({|sway, n|
sway.addModulator("Density -> Time", \pbtime, \pitchbend,
		{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,0.5,1.5)}
},
	{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,22,1.5,0.1) }
},
		spec: ControlSpec(0,1.5,\lin,0.01),

	);

sway.addModulator("Amp -> Bend", \pbbend, \pitchbend,
		{|swayInstance|
			{ |lag=0| swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,10,0.75,1.5) }
},
{|swayInstance|
			{ |lag=0| swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,10,1.5,0.75) }
},
		spec: ControlSpec(0.75,1.5,\lin,0.01)
	);


/*
sway.addModulator("Density -> Delay Time", \pbdelay, \pitchbend,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,9,1.5,0.25) }
},
{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,9,0.25,1.5) }
}, 3,
		spec: ControlSpec(0.25,1.5,\lin,0.01)
	);

*/
});

("Pitch Bend Processor and Modulators Added").postln;
