//MiClouds Processor for Sway


~swayInstances.do({|sway, n|
	sway.addProcessor("MiClouds", \miclouds, {|swayInstance|
	{
		var out = MiClouds.ar(swayInstance.input.ar(1), 12, rvb: 0.7, fb: 0.4, drywet: 1);
		Mix.new(out*0.5);
	}
});
});


("MiClouds Processor Added").postln;
