//Pitch Bend Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Pitch Bend", \pitchbend, {|swayInstance|
	{
		var pbbend = swayInstance.getModulatorNode(\pbbend);
		var pbtime = swayInstance.getModulatorNode(\pbtime);
		var pitch = PitchShift.ar(swayInstance.input.ar(1), 1, pbbend.kr(1), 0.2, pbtime.kr(1));
		pitch;
	}
});
});

//Server.default.sync;

~swayInstances.do({|sway, n|
sway.addModulator("Density -> Time", \pbtime, \pitchbend,
		{|swayInstance|
	{ swayInstance.density.kr(1,0).linlin(0,6,0.5,1)}
},
	{|swayInstance|
		{ swayInstance.density.kr(1,0).linlin(0,6,1,0.1) }
});

sway.addModulator("Amp -> Bend", \pbbend, \pitchbend,
		{|swayInstance|
	{ swayInstance.amplitude.kr(1,0).linlin(0,10,0.75,1.5) }
},
{|swayInstance|
	{ swayInstance.amplitude.kr(1,0).linlin(0,10,1.5,0.75) }
});
});

("Pitch Bend Processor and Modulators Added").postln;
