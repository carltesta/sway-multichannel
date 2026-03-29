//Pools Processor for Sway
//Pools reverb by @justmat for monome norns

~swayInstances.do({|sway, n|
	sway.addProcessor("Pools", \pools, {|swayInstance|
	{
		var plshimmer = swayInstance.getModulatorNode(\plshimmer);
		var plverb = swayInstance.getModulatorNode(\plverb);
		var plamp = swayInstance.getModulatorNode(\plamp);
		var dry, wet, shifted, local, mix;

		    dry = swayInstance.input.ar(1);
            dry = DFM1.ar(dry, 20000.0, 0.1, 1.0, 0.0, 0.0003).softclip;

            local = LocalIn.ar(1);

            wet = JPverb.ar(dry + local, 5.0, 0.13, 1.5, 0.7, 1.0, 0.1, 1.0, 1.0, 1.0, 6000, 1000);
            shifted = PitchShift.ar(wet, 0.5, 2.0, 0.13, 0.12);

			LocalOut.ar(shifted * plshimmer.kr(1));
            wet = LeakDC.ar(wet);
			mix = Mix.ar([dry * plamp.kr(1), wet * plverb.kr(1)]);
			Mix.ar(mix);
	}
},fadeTime: sway.fade);
});

//control mapping:
//clarity -> amount of shimmer
//onsets -> reverb level
//amplitude -> dry level (never full dry)
~swayInstances.do({|sway, n|
sway.addModulator("Clarity -> Shimmer Amount", \plshimmer, \pools,
		{|swayInstance|
			{ |lag=30| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,0,1) }
}, {|swayInstance|
			{ |lag=30| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1,1,0) }
},
		lagTime: 30,
		spec: ControlSpec(0,1,\lin,0.01)
	);

sway.addModulator("Density -> Reverb Level", \plverb, \pools,
		{|swayInstance|
			{ |lag=30| swayInstance.density.kr(1,0).lag(lag).linlin(0,15,0,1) }
}, {|swayInstance|
			{ |lag=30| swayInstance.density.kr(1,0).lag(lag).linlin(0,15,1,0) }
},
		lagTime: 30,
		spec: ControlSpec(0,1,\lin,0.01)
	);

	sway.addModulator("Amp -> Dry Level", \plamp, \pools,
		{|swayInstance|
			{ |lag=0| swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,10,0.1,0.5) }
}, {|swayInstance|
			{ |lag=0| swayInstance.amplitude.kr(1,2).lag(lag).linlin(0,10,0.5,0.1) }
},
		spec: ControlSpec(0,1,\lin,0.01)
	);
});

("Pools Reverb Processor and Modulators Added").postln;
