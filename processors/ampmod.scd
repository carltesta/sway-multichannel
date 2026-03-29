//Amplitude Modulation Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Amplitude Modulation", \ampmod, {|swayInstance|
	{
		var off = Lag2.kr(A2K.kr(DetectSilence.ar(swayInstance.input.ar(1),0.02),0.3));
		var on = 1-off;
		var amfreq = swayInstance.getModulatorNode(\amrate);
		var fade = MulAdd.new(on, 2, 1.neg);
		var out = XFade2.ar(Silent.ar(), swayInstance.input.ar(1), fade);
		var sine = LFTri.ar(amfreq.kr(1), 0).unipolar;
		var am = out*sine;
		am;
	}
},fadeTime: sway.fade);
});

//Server.default.sync;

~swayInstances.do({|sway, n|
sway.addModulator("Amp -> Rate", \amrate, \ampmod,
		{|swayInstance|
			{ |lag=0| swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,30,1,14) }
},
		{|swayInstance|
			{ |lag=0| swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,30,14,1) }
},
		spec: ControlSpec(1,14,\lin,0.01)
	);
});

("Amplitude Modulation Processor and Modulator Added").postln;