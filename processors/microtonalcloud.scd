//Microtonal Cloud Processor for Sway
/*
//Change processing to microtonal cloud
	microtonalcloud {
		//control mapping:
		//onsets -> timearray
		//clarity -> pitcharray
		processing.source = {
			var timearray, pitcharray, signal, delay, pitchshift;
			timearray = [0.1,0.5,1,2]*timespread.kr(1);
			pitcharray = [0.98,0.99,1.01,1.02]*pitchspread.kr(1);
			signal = input.ar(1);
			delay = CombN.ar(signal, 10, timearray, 1.5);
			pitchshift = PitchShift.ar(delay, 0.2, pitcharray);
			Mix.ar(pitchshift);
		};
		(this.name++": Microtonal Cloud").postln;
		current_processing = "Microtonal Cloud";
}

~swayInstances.do({|sway, n|
	sway.addProcessor("Microtonal Cloud", \microtonalcloud, {|swayInstance|
		{
		var timearray, pitcharray, signal, delay, pitchshift;
			timearray = [0.1,0.5,1,2]*timespread.kr(1);
			pitcharray = [0.98,0.99,1.01,1.02]*pitchspread.kr(1);
			signal = input.ar(1);
			delay = CombN.ar(signal, 10, timearray, 1.5);
			pitchshift = PitchShift.ar(delay, 0.2, pitcharray);
			Mix.ar(pitchshift);
		}
	});
});

//control mapping:
		//onsets -> timearray
		//clarity -> pitcharray
//microtonal cloud
timespread.source = {onsets.kr(1,0).lag(lagTime).linlin(0,6,0.5,2) };
		pitchspread.source = {clarity.kr(1,0).linlin(0,1,1,4) };

//polarity
//microtonal cloud
		timespread.source = {onsets.kr(1,1).linlin(0,6,2,0.5) };
		pitchspread.source = {clarity.kr(1,1).linlin(0,1,4,1) };

~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Time Spread", \timespread, \microtonalcloud,
		{|swayInstance|
			{
				swayInstance.density.kr(1,0).linlin(0,6,0.5,2.0) }
}, {|swayInstance|
	{
				swayInstance.density.kr(1,0).linlin(0,6,2.0,0.5) }
},
		spec: ControlSpec(0.5,2,\lin,0.01),
	);

sway.addModulator("Clarity -> Pitch Spread", \pitchspread, \microtonalcloud,
		{|swayInstance|
	{ swayInstance.clarity.kr(1,0).linlin(0,1,1,4) }
}, {|swayInstance|
	{ swayInstance.clarity.kr(1,0).linlin(0,1,4,1) }
},
		spec: ControlSpec(1,4,\lin,0.01)
	);
});

("Microtonal Cloud Processor Added").postln;
