//for testing

SwayConstructor(\sway, 6);

//run gui
(

~sliders = Dictionary.new;
Spec.add(\amp, ControlSpec(0,40,default: 0));
Spec.add(\density, ControlSpec(0,5,default: 0.2));
Spec.add(\clarity, ControlSpec(0,1, default: 0.4));

~textFields = #[amp,clarity,density];

~win = Window.new("Sway", Rect(100,100,600,800), scroll: true).front;
~win.view.decorator = FlowLayout(~win.view.bounds, 10@10,20@5);

Sway.all.keysValuesDo({|name, instance, i|

	var archive = ("sway"++name++"thresholds").asSymbol;
	//var archive = (Archive.global.at(("sway"++name++"thresholds").asSymbol));

	StaticText(~win, Rect(10,10,200,20))
	.string_(("Channel "++((name.asInteger))));
	~win.view.decorator.nextLine;
	instance.tfviews[\processing] = TextField(~win, Rect(10,10,200,20)).string_("processing");
	~win.view.decorator.nextLine;
	~textFields.do({|k|

		var symbol = k.asSymbol;
		var spec = (k.asSymbol).asSpec;

		instance.tfviews[k.asSymbol] = TextField(~win, Rect(10,10,200,20)).string_(k);
		EZSlider(~win, Rect(10,10,350,20), label: (k++" thresh"), labelWidth: 100, controlSpec: spec, initVal: Archive.global.at(archive).at(symbol))
		.action_({|val|
			instance.thresholds.put(symbol, val.value);
			Archive.global.put(archive,instance.thresholds);
			//val.value.postln;
		});

		~win.view.decorator.nextLine;
	});
});

~win.onClose_({Tdef(\val_display).stop});

Tdef(\val_display, {
	loop {
		Sway.refresh_rate.wait;
		Sway.all.keysValuesDo({|name, instance, i|

			{instance.tfviews[\processing].string = (name++" processing: "++instance.current_processing)}.defer;
			instance.onsets.bus.get({|val|
				{instance.tfviews[\density].string = (name++" onsets: "++val[1])}.defer;
			});
			instance.clarity.bus.get({|val|
				{instance.tfviews[\clarity].string = (name++" clarity: "++val[1])}.defer;
			});
			instance.amplitude.bus.get({|val|
				{instance.tfviews[\amp].string = (name++" amplitude: "++val[1])}.defer;
			});
		});
}});

Tdef(\val_display).play;
)
