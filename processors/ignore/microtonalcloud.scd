//Microtonal Cloud Processor for Sway

~swayInstances.do({|sway, n|
	sway.addProcessor("Microtonal Cloud", \microtonalcloud, {|swayInstance|
		{
		var timearray, pitcharray, signal, delay, pitchshift;

			var timespread = swayInstance.getModulatorNode(\timespread);
			var pitchspread = swayInstance.getModulatorNode(\pitchspread);
			timearray = [0.1,0.5,1,2]*timespread.kr(1);
			pitcharray = [0.98,0.99,1.01,1.02]*pitchspread.kr(1);
			signal = swayInstance.input.ar(1);
			delay = CombN.ar(signal, 10, timearray, 1.5);
			pitchshift = PitchShift.ar(delay, 0.2, pitcharray);
			Mix.ar(pitchshift);
		}
	});
});

//control mapping:
		//onsets -> timearray
		//clarity -> pitcharray

~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Time Spread", \timespread, \microtonalcloud,
		{|swayInstance|
			{ |lag=0|
				swayInstance.density.kr(1,0).lag(lag).linlin(0,6,0.5,2.0) }
}, {|swayInstance|
	{ |lag=30|
				swayInstance.density.kr(1,0).lag(lag).linlin(0,6,2.0,0.5) }
},
		spec: ControlSpec(0.5,2,\lin,0.01),
	);

sway.addModulator("Clarity -> Pitch Spread", \pitchspread, \microtonalcloud,
		{|swayInstance|
	{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,1,4) }
}, {|swayInstance|
	{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,4,1) }
},
		spec: ControlSpec(1,4,\lin,0.01)
	);
});

("Microtonal Cloud Processor Added").postln;
