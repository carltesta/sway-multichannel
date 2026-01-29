
~input = NodeProxy.audio(s, 1);

~input.source = {SoundIn.ar(0)};

~spectralshape = NodeProxy.control(s, 7);

~spectralshape.source = {
	FluidSpectralShape.kr(~input.ar(1));
};

w = Window("SpectralShape", Rect(200, 450, 700, 100));
v = MultiSliderView(w, Rect(0,0,700,100))
.size = 7;
v.readOnly=true;
w.front;

Tdef(\update, { loop {
	var val, array;
	array = Array.newClear(7);
	0.1.wait;
	7.do({|n|
		val = ~spectralshape.bus.subBus(n,1).get();
		array[n] = val;
	});
	{v.valueAction_(array)}.defer;
}});

Tdef(\update).play(AppClock);
Tdef(\update).stop;



MultiSliderView


FluidSpectralShape

~spectralshape.bus.subBus(0).get();