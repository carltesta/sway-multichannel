//Amplitude Modulation Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Amplitude Modulation", \ampmod, {|swayInstance|
	{
		var off = Lag2.kr(A2K.kr(DetectSilence.ar(swayInstance.input.ar(1),0.02),0.3));
		var on = 1-off;
		var amfreq = swayInstance.getModulatorNode(\amfreq);
		var fade = MulAdd.new(on, 2, 1.neg);
		var out = XFade2.ar(Silent.ar(), swayInstance.input.ar(1), fade);
		var sine = LFTri.ar(amfreq.kr(1), 0).unipolar;
		var am = out*sine;
		am;
	}
});
});

//Server.default.sync;

~swayInstances.do({|sway, n|
sway.addModulator("Amp -> Rate", \amrate, \ampmod, {|swayInstance|
	{ swayInstance.amplitude.kr(1,0).linlin(0,30,14,1) }
}, {|swayInstance|
	{ swayInstance.amplitude.kr(1,0).linlin(0,30,1,14) }
});
});

("Amplitude Modulation Processor and Modulator Added").postln;