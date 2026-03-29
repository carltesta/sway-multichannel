//Line Movement Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Line", \line, {|swayInstance|
		{
			|x=0, y=3, rate=0.5|
			var xP = swayInstance.initialXPositions;
			var newX = xP[n] + x;
			var halflength = swayInstance.array.speakerLength/2;
			var line = LFTri.kr(rate).range(halflength.neg,halflength);
			var out = BeamFormer.arFocal(swayInstance.output.ar(1,0),(x+newX+line),y, numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	},fadeTime: sway.fade);
});

("Line Spatializer Added").postln;