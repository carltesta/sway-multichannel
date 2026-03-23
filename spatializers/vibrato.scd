//Spatial Vibrato Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Spatial Vibrato", \vibrato, {|swayInstance|
		{
			|x=0, y=3, rate=8|
			var halflength = swayInstance.array.speakerLength/2;
			var line = LFTri.kr(rate).range(-0.5,0.5);
			var xP = swayInstance.initialXPositions[n];
			var out = BeamFormer.arFocal(swayInstance.output.ar(1,0),(x+xP+line),y, numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	});
});

("Vibrato Spatializer Added").postln;