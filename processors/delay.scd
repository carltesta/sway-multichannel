//Delay Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Delay", \delay, {|swayInstance|
	{
		var delayfeedback = swayInstance.getModulatorNode(\delayfeedback);
		var delaytime = swayInstance.getModulatorNode(\delaytime);
			var delaysourcevol = swayInstance.getModulatorNode(\delaysourcevol);
			var coin = CoinGate.kr(0.5, Impulse.kr(1/5));
		    var time = Latch.kr(delaytime.kr(1), coin);
			var feedback = delayfeedback.kr(1);
			var sourcevol = delaysourcevol.kr(1);
			var local = LocalIn.ar(1) + (swayInstance.input.ar(1)*sourcevol);
			var select = ToggleFF.kr(coin);
			var delay1 = BufDelayL.ar(swayInstance.delaybuffer[0], local, Latch.kr(time, 1- select));
			var delay2 = BufDelayL.ar(swayInstance.delaybuffer[1], local, Latch.kr(time, select));
			var fade = MulAdd.new(Lag2.kr(select, 4), 2, 1.neg);
			var delay = XFade2.ar(delay1, delay2, fade);
			LocalOut.ar(delay * feedback);
			delay;
	}
});
});

//control mapping:
//density -> delaytime
//density -> feedback
//density -> source Volume

~swayInstances.do({|sway, n|
sway.addModulator("Density -> Delay Time", \delaytime, \delay,
		{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linexp(0,3,0.5,9)}
},
	{|swayInstance|
		{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linexp(0,3,9,0.5) }
},
		spec: ControlSpec(0.5,9,\lin,0.01),

	);

sway.addModulator("Density -> Feedback", \delayfeedback, \delay,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,4,0.5,0.05) }
},
{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,4,0.05,0.5) }
},
		spec: ControlSpec(0.05,0.5,\lin,0.01)
	);

sway.addModulator("Density -> Source Vol", \delaysourcevol, \delay,
		{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,3,0.7,1) }
},
{|swayInstance|
			{ |lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,3,1,0.7) }
},
		spec: ControlSpec(0.1,0.7,\lin,0.01)
	);
});

("Delay Processor and Modulators Added").postln;
