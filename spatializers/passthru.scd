//Passthru Spatializer for Sway

//this spatializer is to enable the output nodeproxy to expand to 64 channels and then then pass the 64 channels on to the array

~swayInstances.do({|sway, n|
	sway.addSpatializer("passthru", \passthru, {|swayInstance|
		{
			var out = sway.output.ar(sway.numSpeakers);
			out;

		}
	},fadeTime: sway.fade);
});

("Passthru Spatializer Added").postln;
