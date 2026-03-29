//Random Movement Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Random", \random, {|swayInstance|
		{

			|x=0, y=3, rate=0.5|
			var xP = swayInstance.initialXPositions;
			var halflength = swayInstance.array.speakerLength/2;
			var randomX = LFNoise1.kr(rate).range(halflength.neg,halflength);
			var randomY = LFNoise1.kr(rate).range(halflength.neg,halflength);
			var out = BeamFormer.arFocal(swayInstance.output.ar(1,0),(x+xP[n]+randomX),(y+randomY), numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	},fadeTime: sway.fade);
});

("Random Spatializer Added").postln;