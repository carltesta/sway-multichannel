
(
~textFields = #[amplitude,clarity,onsets];

~win = Window.new("Sway", Rect(100,100,600,800)).front;
~win.view.decorator = FlowLayout(~win.view.bounds, 10@10,20@5);

Sway.all.keysValuesDo({|name, instance, i|

StaticText(~win, Rect(10,10,200,20))
.string_(("Channel "++((name.asInteger))));
~win.view.decorator.nextLine;
	instance.tfviews[\processing] = TextField(~win, Rect(10,10,200,20)).string_("processing");
	~win.view.decorator.nextLine;
~textFields.do({|k|
instance.tfviews[k.asSymbol] = TextField(~win, Rect(10,10,200,20)).string_(k);
EZSlider(~win, Rect(10,10,350,20), label: (k++" thresh"), labelWidth: 100);
~win.view.decorator.nextLine;
	});
});

Tdef(\val_display, {
	loop {
		Sway.refresh_rate.wait;
		Sway.all.keysValuesDo({|name, instance, i|

			{instance.tfviews[\processing].string = (name++" processing: "++instance.current_processing)}.defer;
		instance.onsets.bus.get({|val|
			{instance.tfviews[\onsets].string = (name++" onsets: "++val[1])}.defer;
			});
		instance.clarity.bus.get({|val|
				{instance.tfviews[\clarity].string = (name++" clarity: "++val[1])}.defer;
			});
		instance.amplitude.bus.get({|val|
				{instance.tfviews[\amplitude].string = (name++" amplitude: "++val[1])}.defer;
			});
		});
}});

Tdef(\val_display).play;
)
