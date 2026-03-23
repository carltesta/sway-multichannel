//Static Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Static", \static, {|swayInstance|
		{
			|x=0, y=3|
			var out, xP;
			xP = swayInstance.xPosition;
			out = BeamFormer.arFocal(swayInstance.output.ar(1,0),x+xP,y, numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	});
});

("Static Spatializer Added").postln;
