~files = SoundFile.collect("C:/Users/Carl/Documents/Samples/sway/sway_stems_2018_03_23/44100/*");

~buffers = ~files.collect({|sndfile| Buffer.cueSoundFile(s, sndfile.path, numChannels: 1)});

~playback = ~buffers.collect({|buf| NodeProxy.audio(s, 1)});

~playback.do({|node,n| node.source_({ |amp=1| DiskIn.ar(1, ~buffers[n], 1)*amp})});

~swayInstances[0].output.gui

~playback[0].source;
~buffers[5]

~wfsControl.set(\maxUpperGain, -12.dbamp)

-3.dbamp was not enough limiting

	~swayInstances[0].loopQuadrantRead = false;

~lfo = NodeProxy.control(s, 1).source_({LFTri.kr(0.1).range(-4,-0.5)});

~lfo2 = NodeProxy.control(s, 1).source_({LFTri.kr(0.1).range(-3,3)});


~swayInstances[1].beamformer.set(\x, ~lfo2.asMap, \y, -3)
~swayInstances[5].beamformer.set(\x, 4, \y, ~lfo.asMap)


//Alternative Analysis Options
~swayInstances.do({|sway, n|
	sway.density.source = {
		var density = FluidOnsetFeature.kr(sway.input.ar(1), 0, 10, 0);
		SendReply.kr(Impulse.kr(1), ("/"++sway.channel++"/density/").asSymbol, density);
		density;
	};

	sway.amplitude.source = {
		var amplitude = FluidAmpFeature.kr(sway.input.ar(1));
		SendReply.kr(Impulse.kr(1), ("/"++sway.channel++"/amplitude/").asSymbol, amplitude);
		amplitude;
	};
	sway.clarity.source = {
		var clarity = FluidPitch.kr(sway.input.ar(1),[\confidence]);
		SendReply.kr(Impulse.kr(1), ("/"++sway.channel++"/clarity/").asSymbol, clarity);
		clarity;
	};
});