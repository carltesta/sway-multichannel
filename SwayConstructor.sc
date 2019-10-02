SwayConstructor : Singleton {

	var <>gui, <>counter=0, <>wait_time=1.0, <>quadrant, <>global, <>amp, <>channel, <>global_loop, <>mixer_bus, <>monitor, <>solo_done=false, <>video_loop, <>hydra, <>blends, <>aggregatexy, <>averagexy, <>aggregateamp, <>aggregateclarity, <>aggregatedensity, <>video_verbose=false;

	init {
		//build structures for global changes
		global = Dictionary.new;
		quadrant = Dictionary.new;
		amp = Dictionary.new;
		channel = Dictionary.new;
		//start global loop here
		global_loop = TaskProxy.new({ loop {
			if(Sway.all.size>1, {
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
				//var mix = Mix.ar(in*level);
				//mix!2;
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
		{quadrant.every({|item|item==2})}{this.waveloss_all}
		{quadrant.every({|item|item==1})}{this.delay_all}
		{quadrant.every({|item|item==3})}{this.texture_all}
		{quadrant.every({|item|item==4})}{this.cascade_all}
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
		var old_fade = Dictionary.new;
		var old_input = Dictionary.new;
		var old_processing = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.quadrant_change=false;//turn off quadrant change to prevent processing changes
			//val.analysis_on=false;//turn off analysis to prevent movement around grid
			old_fade.put(key, val.fade);//capture current fadetime
			old_input.put(key, val.input.get(\chan));//capture original input assignment
			old_processing.put(key, val.quadrant_names);//capture current processing type
			val.input.source = { |chan| SoundIn.ar(newchan) };//change all instances to same processing input
			val.fade_time(5);//change fadetime to sometime quick
			val.all_processing.choose.value;//choose new type of processing for each channel
			val.analysis_input.source { |chan| SoundIn.ar(newchan) };//change all instances to same analysis input
			(val.name++": Global Event Solo Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;//wait for the timelimit
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Event Solo Ending").postln;
			val.input.source = { |chan| SoundIn.ar(old_input.at(key)) };//reapply the current inputs
			val.analysis_input.source = { |chan| SoundIn.ar(old_input.at(key)) };//reapply the current inputs for analysis
			val.fade_time(old_fade.at(key));//reapply old fadetime
			val.map_quadrants(old_processing.at(key));//map based on the old processing
			val.global_change=false;
			val.quadrant_flag=true;
			//val.analysis_on=true;
			val.quadrant_change=true;//turn quadrant changes back on so that processing grids can change
		});
		solo_done=true;
	}

	decouple_all {
		var scramble = Array.fill(Sway.all.size, {|n| n}).scramble;
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val,n|
			//val.analysis_on=false;
			val.quadrant_change=false;//turn off quadrant change to prevent processing changes
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
			val.quadrant_change=true;//turn quadrant changes back on so that processing grids can change
			//val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	delay_all {
		var old_fade = Dictionary.new;
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.quadrant_change=false;//turn off quadrant change to prevent processing changes
			//val.analysis_on=false;
			old_fade.put(key, val.fade);//capture old fadetime
			val.fade_time(5);//change fadetime
			old.put(key, val.quadrant_names);
			val.delay;
			(val.name++": Global Delay Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Delay Complete").postln;
			val.fade_time(old_fade.at(key));//reapply old fade
			val.map_quadrants(old.at(key));
			val.quadrant_change=true;//turn quadrant changes back on so that processing grids can change
			//val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	texture_all {
		var old_fade = Dictionary.new;
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.quadrant_change=false;//turn off quadrant change to prevent processing changes
			//val.analysis_on=false;
			old_fade.put(key, val.fade);//capture old fadetime
			val.fade_time(5);//change fadetime
			old.put(key, val.quadrant_names);
			val.textural;
			(val.name++": Global Texture Landscape Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Texture Landscape Complete").postln;
			val.fade_time(old_fade.at(key));//reapply old fade
			val.map_quadrants(old.at(key));
			val.quadrant_change=true;//turn quadrant changes back on so that processing grids can change
			//val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	cascade_all {
		var old_fade = Dictionary.new;
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.quadrant_change=false;//turn off quadrant change to prevent processing changes
			//val.analysis_on=false;
			old_fade.put(key, val.fade);//capture old fadetime
			val.fade_time(5);//change fadetime
			old.put(key, val.quadrant_names);
			val.cascade;
			(val.name++": Global Cascade Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Cascade Complete").postln;
			val.fade_time(old_fade.at(key));//reapply old fade
			val.map_quadrants(old.at(key));
			val.quadrant_change=true;//turn quadrant changes back on so that processing grids can change
			//val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	waveloss_all {
		var old_fade = Dictionary.new;
		var old = Dictionary.new;
		var limit;
		Sway.all.keysValuesDo({|key,val|
			val.quadrant_change=false;//turn off quadrant change to prevent processing changes
			//val.analysis_on=false;
			old_fade.put(key, val.fade);//capture old fadetime
			val.fade_time(5);//change fadetime
			old.put(key, val.quadrant_names);
			val.waveloss;
			(val.name++": Global Waveloss Beginning").postln;
			limit = val.timelimit;
		});
		limit.wait;
		Sway.all.keysValuesDo({|key,val|
			(val.name++": Global Waveloss Complete").postln;
			val.fade_time(old_fade.at(key));//reapply old fade
			val.map_quadrants(old.at(key));
			val.quadrant_change=true;//turn quadrant changes back on so that processing grids can change
			//val.analysis_on=true;
			val.global_change=false;
			val.quadrant_flag=true;
		});
	}

	reset_all {
		Sway.all.keysValuesDo({|key, value|
			(value.name++": resetting").postln;
			value.reset;
		});
		solo_done=false;
	}

	end_all {
		Sway.all.keysValuesDo({|key, value|
			(value.name++": ending").postln;
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

	video_start { |hydra_host="192.168.159.245"|
		var amp, clarity, density, calcBlend;
		calcBlend = {|x2,y2,x1,y1|
			//calculate distance of coordinate which is the blend for the video module
			var value = (0.73-((((x2-x1)**2) + ((y2-y1)**2)).sqrt)).clip(0.0,1.0);
			value;
		};
		//video module control using Hydra https://github.com/ojack/atom-hydra
		hydra = NetAddr.new(hydra_host, 57101);
			aggregatexy = Dictionary.new;
		    aggregateamp = Dictionary.new;
		    aggregateclarity = Dictionary.new;
		    aggregatedensity = Dictionary.new;
			averagexy = [0.5,0.5];
			blends = Array.newClear(4);
		    //initialize data structures
		    Sway.all.keysValuesDo({|key,val|
			aggregatexy.put(key,val.xy);
			aggregateamp.put(key, val.amplitude.bus.subBus(1).getSynchronous.linlin(0,30,0,1));
			aggregateclarity.put(key, val.clarity.bus.subBus(1).getSynchronous);
			aggregatedensity.put(key, val.onsets.bus.subBus(1).getSynchronous.linlin(0,6,0,1));
		});
			video_loop = TaskProxy.new({ loop {
			//get all xy values
				Sway.all.keysValuesDo({|key,val|
					//check if above amp threshold
				if(val.quadrant!=0, {
						//get the xy coordinates and add to dictionary
						aggregatexy.put(key, val.xy);
					//get the analysis values (30 second averaged values)
					aggregateamp.put(key, val.amplitude.bus.subBus(1).getSynchronous.linlin(0,30,0,1));
					aggregateclarity.put(key, val.clarity.bus.subBus(1).getSynchronous);
				    aggregatedensity.put(key, val.onsets.bus.subBus(1).getSynchronous.linlin(0,6,0,1));
				},{
					    //if input is in quadrant 0 then remove their influence from parameters
					    aggregatexy.removeAt(key);
					    aggregateamp.removeAt(key);
					    aggregateclarity.removeAt(key);
					    aggregatedensity.removeAt(key);
				});
				});
				//process them
				//find the averaged point
				//average xy
				averagexy = aggregatexy.sum/aggregatexy.size;
			    //[aggregatexy.sum,aggregatexy.size,averagexy].postln;
				//find the distance from each corner, 1-that is the blend
				//1-((((X2-X1)**2) + ((Y2-Y1)**2)).sqrt) this works
				//blend levels
				//1[1,1], 2[0,1], 3[0,0], 4[1,0]
				blends[0] = calcBlend.(averagexy[0],averagexy[1],1,1);
				blends[1] = calcBlend.(averagexy[0],averagexy[1],0,1);
				blends[2] = calcBlend.(averagexy[0],averagexy[1],0,0);
				blends[3] = calcBlend.(averagexy[0],averagexy[1],1,0);
			if(aggregateamp.size>0,{
				amp = aggregateamp.sum/aggregateamp.size;
				clarity = aggregateclarity.sum/aggregateclarity.size;
				density = aggregatedensity.sum/aggregatedensity.size;
			},{});
				//send everything via OSC
			if(video_verbose==true,{("blends: "++[blends[0],blends[1],blends[2],blends[3], amp, clarity, density]).postln;},{});
				hydra.sendMsg('/control', blends[0],blends[1],blends[2],blends[3],amp,clarity,density);
				//hydra.sendMsg('/fadeout');
				//wait so you don't crash
				0.04.wait;
			}}).play;
	}

	video_stop {
		video_loop.stop;
	}

}