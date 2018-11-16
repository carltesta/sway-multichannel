SwayConstructor : Singleton {

	var <>gui, <>counter=0, <>wait_time=1.0, <>quadrant, <>global, <>amp, <>channel, <>global_loop, <>mixer_bus, <>monitor, <>solo_done=false;

	init {
		//build structures for global changes
		global = Dictionary.new;
		quadrant = Dictionary.new;
		amp = Dictionary.new;
		channel = Dictionary.new;
		//start global loop here
		global_loop = TaskProxy.new({ loop {
			Sway.all.keysValuesDo({|key,val|
				global.put(key, val.global_change);
			});
			if( global.values.every({|item|item==true}), {
			this.check_all_amplitude;
			wait_time.wait;
			this.check_all_clarity;
			wait_time.wait;
			this.check_all_onsets;
			wait_time.wait;
			this.check_all_quadrant;
			wait_time.wait;
			},{});
			wait_time.wait;
			counter = counter + wait_time;
		}}).play;

	}

	set {
		|num|
		var rout = Routine({
		//if no names supplied then use index
		//if(names.isNil,{names=Array.fill(num, {|n| n})});
		//create mixer bus
		mixer_bus = Bus.audio(Server.default, num);
		//create the number of instances of Sway you need
		("starting sway").postln;
		num.do({|n|
			Sway(n.asSymbol);
		});
		1.0.wait;
		//setup the input and output connections
		num.do({|n|
			Sway(n.asSymbol).input.set(\chan, n);
			Sway(n.asSymbol).analysis_input.set(\chan, n);
			Sway(n.asSymbol).output.playN([(n+2),mixer_bus.subBus(n)]);

		});
		1.0.wait;
		gui=SwayGUI.new;
		1.0.wait;
		//monitor mix
		monitor = NodeProxy.audio(Server.default, 2).play(0, 2)
		.source = { |level=1|
				var in = In.ar(mixer_bus.index, num);
				var splay = Splay.ar(in, 1, level);
				splay;
			};
		}).play(AppClock);
	}

	add_channel {|name|
		Sway(name.asSymbol);
		this.update_gui;
	}

	update_gui {
		gui.win.close;
		gui=SwayGUI.new;
	}

	check_all_amplitude {
		//if all above threshold
		//if all below threshold
		//if one above threshold
		if(solo_done==false, {
		Sway.all.keysValuesDo({|key,val|
			channel.put(key, val.input.get(\chan));
			amp.put(key, val.above_amp_thresh);
			amp = amp.select({|item|item==true});
			});
		case
		{amp.size==1}{this.solo(amp.findKeyForValue(true).asInteger)};
		},{});
		//if one below threshold
	}

	check_all_clarity {
		//if all above threshold
		//if all below threshold
		//if one above threshold
		//if one below threshold
	}

	check_all_onsets {
		//if all above threshold
		//if all below threshold
		//if one above threshold
		//if one below threshold
	}

	check_all_quadrant {
		//if all in one quadrant
			Sway.all.keysValuesDo({|key,val|
			quadrant.put(key, val.quadrant[0]);
			});
		case
		{quadrant.every({|item|item==1})}{this.decouple_all}
		{quadrant.every({|item|item==2})}{this.delay_all}
		{quadrant.every({|item|item==3})}{this.cascade_all}
		{quadrant.every({|item|item==0})}{this.silence_all};
	}

	silence_all {
		Sway.all.keysValuesDo({|key,val|
			val.quadrant_map.put(0,val.all_processing.at(\silence));
			val.available_processing.removeAt(\silence);
			val.quadrant_names.put(0,"silence".asSymbol);
			val.quadrant_flag=true;
			val.global_change=false;
		});
	}

	solo { |newchan|
		var old_input = Dictionary.new;
		var old_processing = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.analysis_on=false;//turn off analysis to prevent movement around grid
			old_input.put(key, val.input.get(\chan));//capture original input assignment
			old_processing.put(key, val.quadrant_names);//capture current processing type
			val.input.source = { |chan| SoundIn.ar(newchan) };//change all instances to same processing input
			val.all_processing.choose.value;//choose new type of processing for each channel
			(val.name++": Global Event Solo Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;//wait for the timelimit
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Event Solo Ending").postln;
			val.input.source = { |chan| SoundIn.ar(old_input.at(key)) };//reapply the current inputs
			val.map_quadrants(old_processing.at(key));//map based on the old processing
			val.global_change=false;
			val.quadrant_flag=true;
			val.analysis_on=true;
		});
		solo_done=true;
	}

	decouple_all {
		var scramble = Array.fill(Sway.all.size, {|n| n}).scramble;
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val,n|
			//val.analysis_on=false;
			old.put(key, val.analysis_input.get(\chan));
			val.analysis_input.set(\chan, scramble[n]);
			(val.name++": Global Analysis Decoupling Beginning").postln;
			limit = val.timelimit;
			val.global_change=false;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val,n|
			(val.name++": Global Analysis Decoupling Ending").postln;
			val.analysis_input.set(\chan, old.at(key));
			//val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	delay_all {
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.analysis_on=false;
			old.put(key, val.quadrant_names);
			val.delay;
			(val.name++": Global Delay Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Delay Complete").postln;
			val.map_quadrants(old.at(key));
			val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	cascade_all {
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.analysis_on=false;
			old.put(key, val.quadrant_names);
			val.cascade;
			(val.name++": Global Cascade Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Cascade Complete").postln;
			val.map_quadrants(old.at(key));
			val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	reset_all {
		Sway.all.keysValuesDo({|key, value|
			value.name++": resetting".postln;
			value.reset;
		});
		solo_done=false;
	}

	end_all {
		Sway.all.keysValuesDo({|key, value|
			value.end;
		});
	}

	unique {|func, array|
		var match=[];
		array.do({|val, idx|
			if(func.(val)){match = match.add(idx)}
		});

		if(match.size == 1){
			Maybe(match[0]);
		}{
			Maybe(nil);
		};
	}

}