//Static Spatializer for Sway

~swayInstances.do({|sway, n|
sway.addSpatializer("Static", \static, {|swayInstance|
	{
		//var rate = swayInstance.density.kr(1,0).lag(15).linlin(0,6,0.1,8);
		var out = DC.kr(0!2);
		out;
	}
});
});

("Static Spatializer Added").postln;