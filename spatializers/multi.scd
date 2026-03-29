//Multi Spatializer for Sway

~swayInstances.do({|sway, n|
	sway.addSpatializer("Multi", \multi, {|swayInstance|
		{
			|x=0, y=3|
			var xP, newX, out;
			xP = swayInstance.initialXPositions;
			newX = xP[n] + x;
			out = BeamFormer.arFocal(swayInstance.output.ar(1,0),[newX-0.25,newX,newX+0.25],[y,y+0.25,y+0.25],[1/3,1/3,1/3], numSpeakers: swayInstance.array.numSpeakers);
			out;
		}
	},fadeTime: sway.fade);
});

("Multi Spatializer Added").postln;