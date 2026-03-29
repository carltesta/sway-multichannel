//Circle Focus Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Circle Focus", \circle, {|swayInstance|
		{
			|x=0, y= -2.1, rate=1|
			var speed = sway.density.kr(1,0).linlin(0,22,0.1,2);
			var xP = sway.initialXPositions[n]+x;
			var xCoord = SinOsc.kr(speed+rate).range(-2,2);
			var yCoord = SinOsc.kr(speed+rate, 0.5pi).range(-2,2);
			var out = BeamFormer.arFocal(swayInstance.output.ar(1,0),(x+xCoord),(y+yCoord), numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	});
});

("Circle Spatializer Added").postln;