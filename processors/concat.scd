//Concat2 Processor for Sway

~swayInstances.do({|sway, n|
sway.addProcessor("Concatenative", \concat, {|swayInstance|
	{
			var cnctmatchlength = sway.getModulatorNode(\cnctmatchlength);
			var cnctfreeze = sway.getModulatorNode(\cnctfreeze);
			var cnctfeatweight = sway.getModulatorNode(\cnctfeatweight);
			//var out = Concat2.ar(sway.analysis_input.ar(1), PlayBuf.ar(1, sway.buffer, loop: 1),matchlength: cnctmatchlength.kr(1), freezestore: cnctfreeze.kr(1),sc: cnctfeatweight.kr(1));
			var out = Concat2.ar(sway.analysis_input.ar(1), PlayBuf.ar(1, sway.buffer, loop: 1));
		out;
	}
},fadeTime: sway.fade);
});

//control mapping:
//density -> match length
//amplitude -> freeze
//clarity -> feature weightings

~swayInstances.do({|sway, n|
	sway.addModulator("Density -> Match Length", \cnctmatchlength, \concat,
		{|swayInstance|
			{
				|lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,30,0.05,0.5) }
}, {|swayInstance|
	{
				|lag=0| swayInstance.density.kr(1,0).lag(lag).linlin(0,30,0.5,0.01) }
},
		spec: ControlSpec(0.01,0.5,\lin,0.01)
	);


sway.addModulator("Amp -> Freeze", \cnctfreeze, \concat,
		{|swayInstance|
	{ |lag=0| swayInstance.amplitude.kr(1,2).linlin(29,30,0,1) }
}, {|swayInstance|
	{ |lag=0| swayInstance.amplitude.kr(1,2).linlin(29,30,1,0) }
},
		lagTime: 0,
		spec: ControlSpec(0,1,\lin,0.01)
	);

sway.addModulator("Clarity -> Centroid Feature Weight", \cnctfeatweight, \concat,
		{|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1.0,1.0,4.0) }
}, {|swayInstance|
			{ |lag=0| swayInstance.clarity.kr(1,0).lag(lag).linlin(0,1.0,4.0,1.0) }
},
		spec: ControlSpec(0,4.0,\lin,0.01)
	);
});

//Server.default.sync;

("Concatenative Synth Processor Added").postln;