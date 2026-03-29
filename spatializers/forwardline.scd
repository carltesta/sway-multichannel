//Forward Line Movement Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Forward Line", \forwardline, {|swayInstance|
		{
			|x=0, y=0.1, rate=0.5|
			var amp = sway.amplitude.kr(1,2).linlin(0,30,0, -4);
			var xP = swayInstance.initialXPositions;
			var newY = y+amp;
			var out = BeamFormer.arFocal(swayInstance.output.ar(1,0),x+xP[n],newY, numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	},fadeTime: sway.fade);
});

("Forward Line Spatializer Added").postln;