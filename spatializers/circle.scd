//Circle Movement Spatializer for Sway

~swayInstances.do({|sway, n|
sway.addSpatializer("Circle", \circle, {|swayInstance|
	{
		//var rate = swayInstance.density.kr(1,0).lag(15).linlin(0,6,0.1,8);
		var rate = 0.1;
		var out = SinOsc.kr(rate,[pi/2,0]).range(-1,1);
		out;
	}
});
});

//x = {SinOsc.kr(0.1, [0,0.5]).unipolar.poll }.play;

("Circle Spatializer Added").postln;