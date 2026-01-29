//Random Movement Spatializer for Sway

~swayInstances.do({|sway, n|
sway.addSpatializer("Random", \random, {|swayInstance|
	{
		//var rate = swayInstance.density.kr(1,0).lag(15).linlin(0,6,0.1,8);
		var out = LFNoise1.kr(0.1!2).range(0,1);
		out;
	}
});
});

("Random Spatializer Added").postln;