SwayGUI : Singleton {
	//Carl Testa 2018
	//Processing Graph GUI for Sway
	//Used to be part of Sway class, but separated it out for rebuild of prototype 3

	var <>win, color_scheme, main, view, text, amp_sliders, step_slider, grav_slider, refresh_slider, mixer_slider, updater, formatted_x, formatted_y;

   init {
		win = Window("Sway - Prototype 3", Rect(0, 300, 1200, 600));
		win.onClose_({this.clear});

		//Use a differing color scheme to different what GUI goes with which channel
		color_scheme = [Color.white, Color.yellow, Color.green, Color.blue, Color.red, Color.gray];

		main = View.new(win,Rect(0,0,1200,600))
		.layout_(HLayout());

		//Format the x/y coordinates to be input into the GUI
		formatted_x = Array.newClear(Sway.all.size);
		formatted_y = Array.newClear(Sway.all.size);

		Sway.all.keysValuesDo({|name, instance, n|
			formatted_x.put(n,instance.xy[0])
		});
		Sway.all.keysValuesDo({|name, instance, n|
			formatted_y.put(n,instance.xy[1])
		});

		//Begin building the view, I'm using an EnvelopeView here as the grid
		view = EnvelopeView(main)
		.thumbWidth_(60.0)
		.thumbHeight_(15.0)
		.drawLines_(false)
		.drawRects_(true)
		.step_(0.01)
		.selectionColor_(Color.red)
		.grid_(Point(0.5, 0.5))
		.gridOn_(true)
		.mouseDownAction_({|view|
			updater.pause;})
			//Tdef(("analysisLoop_"++view.index).asSymbol).pause;})
		.mouseUpAction_({|view|
			updater.resume(AppClock,1);})
			//Tdef(("analysisLoop_"++view.index).asSymbol).resume(AppClock,1);})
		.action_({|view|
			//model[view.index] = [view.x, view.y];
			//[view.index,view.x,view.y].postln;
		})

		.value_([formatted_x, formatted_y]);

		//TO DO: Put some kind of label on each quadrant so that you can see how the system changes over time
		//text = StaticText(view, Rect(40,40,100,100))
		//.string = Sway(\1).quadrant_map[2].source.asString;

		Sway.all.keysValuesDo({|name, instance, i|
			view.setString(i, instance.name);
			view.setFillColor(i,color_scheme[i%6]);
		});

		//Amp sliders
		Sway.all.keysValuesDo({|name, instance, i|
			Slider(main, Rect(0,0,25,100))
			.step_(0.01)
		    .value_(instance.amp_thresh/40)
			.background_(color_scheme[i%6])
			.knobColor_(color_scheme[i%6])
			.action_({|val|
				instance.amp_thresh=val.value*40;
				(name++": amp threshold: "++(val.value*40)).postln;
			});
		});

		//density
		Sway.all.keysValuesDo({|name, instance, i|
			Slider(main, Rect(0,0,25,100))
			.step_(0.01)
		    .value_(instance.density_thresh/5)
			.background_(color_scheme[i%6])
			.knobColor_(color_scheme[i%6])
			.action_({|val|
				instance.density_thresh=val.value*5;
				(name++": density threshold: "++(val.value*5)).postln;
			});
		});

		//clarity
		Sway.all.keysValuesDo({|name, instance, i|
			Slider(main, Rect(0,0,25,100))
			.step_(0.01)
		    .value_(instance.clarity_thresh)
			.background_(color_scheme[i%6])
			.knobColor_(color_scheme[i%6])
			.action_({|val|
				instance.clarity_thresh=val.value;
				(name++": clarity threshold: "++(val.value)).postln;
			});
		});

		//step
		step_slider = Slider(main, Rect(0,0,25,100))//step
			.step_(0.001)
		    .value_(Sway.step)
			.action_({|val|
				Sway.step=val.value;
			("step: "++(val.value)).postln;
			});

		//gravity
		grav_slider = Slider(main, Rect(0,0,25,100))//gravity
			.step_(0.001)
		    .value_(Sway.gravity)
			.action_({|val|
				Sway.gravity=val.value;
			("gravity: "++(val.value)).postln;
			});

		//refresh
		refresh_slider = Slider(main, Rect(0,0,25,100))//refreshRate
			.step_(0.01)
		    .value_(Sway.refresh_rate/2)
			.action_({|val|
				Sway.refresh_rate=val.value*2+0.1;
			("refresh rate: "++(val.value*2+0.1)).postln;
			});

		//mixer level slider (MAIN VOLUME)
		mixer_slider = Slider(main, Rect(0,0,25,100))//mixerLevel
			.step_(0.01)
		    .value_(1.0)
		    .background_(Color.gray)
			.action_({|val|
			SwayConstructor(\sway).monitor.set(\level, val.value);
			("mixer level: "++(val.value)).postln;
			});

		win.front;

		//gui updater task
		updater = TaskProxy.new({ loop {
				Sway.all.keysValuesDo({|name, instance, i|
					view.selectIndex(i);
				    view.x_(instance.xy[0]);
					view.y_(instance.xy[1]);
				});
				Sway.refresh_rate.wait;
			}
		}).play(AppClock);

	}

}