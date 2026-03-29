//Single Grain Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Single Grain", \grain, {|swayInstance|
			Tdef(("grain"++n).asSymbol, {
	var windowSize = 0.2,
	overlaps = 4,
	freqScale = 1,
	playRate = 1,
	buffer = sway.buffer,
	pos = 0,
	freqDeviation = 0.02;

	loop {
		var waitTime = windowSize / overlaps;
		Synth.grain(\grain, [
			\bufnum, sway.buffer,
			\pointer, pos / buffer.duration, // scale position
			\freqScale, freqScale.ring1(rrand(freqDeviation.neg, freqDeviation)),
			\windowSize, windowSize,
			\amp, overlaps.reciprocal,
			\x, sway.initialXPositions[n],
			\y, Pwhite(-0.5,4,inf).asStream.next
		]);
		waitTime.wait;
		pos = pos + (playRate * waitTime); // advance the position based on waitTime and playRate
	}
});
});
});

//Server.default.sync;

("Single Grain Processor Added").postln;
