//Cascade Processor for Sway

~swayInstances.do({|sway, n|
	sway.addProcessor("Cascade", \cascade, {|swayInstance|
		{
		//TO DO: analysis control not implemented
			//pitch ->
			//amp -> number of layers??
			//onsets -> how far back in file does it read? Output current frame maybe?
			var sound = PlayBuf.ar(1, swayInstance.buffer.bufnum, 1, 0, {swayInstance.buffer.numFrames.rand}, 1)*1/16;
			var env = SinOsc.kr(1/16, (0..15).linlin(0,15,8pi.neg,8pi), 1/16);
			var mix = Limiter.ar(Mix.new(sound*env), -6.dbamp);
			mix;
		}
	}, fadeTime: sway.fade);
});

("Cascade Processor Added").postln;
