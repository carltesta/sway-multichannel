SwaySimple : Singleton {
	//Carl Testa
	//2017-2025

	classvar <>short_win=1, <>long_win=30, <>refresh_rate=1.0, <>gravity=0.01, <>step=0.05;

	var <>xy, <>quadrant, <>quadrant_names, <>quadrant_map, <>input, <>output, <>analysis_input, <>buffer, <>fftbuffer, <>delaybuffer, <>recorder, <>processing, <>fade=45, <>onsets, <>amplitude, <>clarity, <>flatness, <>amfreq, <>rvmix, <>rvsize, <>rvdamp, <>delaytime, <>delayfeedback, <>delaysourcevol, <>delaylatch, <>pbtime, <>pbbend, <>graintrig, <>grainfreq, <>grainpos, <>grainsize, <>granpos, <>granenvspeed, <>granrate, <>filtfreq, <>filtrq, <>freezedurmin, <>freezedurmax, <>freezeleg, <>texturalmin, <>texturalmax, <>texturalsusmin, <>texturalsusmax, <>texturalposrate, <>texturalpostype, <>texturalrate,
<>wldrop, <>wloutof, <>wlmode, <>dslevel, <>smlevel, <>timespread, <>pitchspread, <>plshimmer, <>plamp, <>plverb, <>vsspeed, <>analysis_loop, <>above_amp_thresh=false, <>above_clarity_thresh=false, <>above_density_thresh=false, <>thresholds, <>tracker, <>count=0, <>analysis_on=true, <>tracker_on=true, <>audio_processing=true, <>verbose=false, <>polarity=false, <>quadrant_flag=false, <>timelimit=180,<>tfviews,//timelimit*16 for video
<>available_processing, <>all_processing, <>current_processing, <>global_change=false, <>quadrant_change=true;

	init {
		//Setup initial parameters
		this.reset;
		//audio input with chan argument
		input = NodeProxy.audio(Server.default, 1).fadeTime_(fade)
		.source = { |chan=0| SoundIn.ar(chan,1.neg) };

		//fft
		fftbuffer = Buffer.alloc(Server.default, 1024);

		//delaybuffer
		delaybuffer = Buffer.allocConsecutive(2, Server.default, 12*44100, 1);

		//audio recorder
		buffer = Buffer.alloc(Server.default, long_win*44100, 1);
		recorder = NodeProxy.audio(Server.default, 1)
		.source = {
			var off = Lag2.kr(A2K.kr(DetectSilence.ar(input.ar(1), 0.1), 0.3));
            var on = 1-off;
			var fade = MulAdd.new(on, 2, 1.neg);
			var out = XFade2.ar(Silent.ar(), input.ar(1), fade);
			RecordBuf.ar(out, buffer, loop: 1, run: on);
		};

		//this is the placeholder for audio procesing
		processing = NodeProxy.audio(Server.default, 1).fadeTime_(fade)
		.source = { Silent.ar(1) };

		//audio output to listen and change channel
		output = NodeProxy.audio(Server.default, 1)
		.source = { processing.ar(1) };

		//analysis input so there is option to decouple processed audio from analysed audio
		analysis_input = NodeProxy.audio(Server.default, 1)
		.source = { |chan=0| SoundIn.ar(chan) };

		//Build the analysis modules
		this.build_analysis;
		//Begin with an initial mapping of parameters
		//this.nonpolarity_map;
		//this.polarity_map;
		//TO DO: How does the system switch between the polarity and nonpolarity map?

		//Longer term analysis controlling placement on processing grid
		analysis_loop = TaskProxy.new({ loop {
			//if analysis on flag is set to true then do analysis
			if(analysis_on==true, {
				//(this.name++": analysis on").postln;
			//if verbose is on report values
			if(verbose==true,{
			flatness.bus.get({|val|
				(this.name++" flatness: "++val[1]).postln;
					});
			onsets.bus.get({|val|
				(this.name++" onsets: "++val[1]).postln;
					});
			clarity.bus.get({|val|
				(this.name++" clarity: "++val[1]).postln;
					});
				});
			//if signal is above amplitude threshold do analysis
			amplitude.bus.get({|val|
				if(verbose==true,{(this.name++" amp: "++val[1]).postln});
				if( val[0] > thresholds.at(\amp), {//change it so that the amplitude thresh boolean is more accurate
					above_amp_thresh=true;
				}, {above_amp_thresh=false;});

				if( val[1] > thresholds.at(\amp), { //conduct analysis if longer averaged amp is above threshold
					if(verbose==true,{(this.name++" amp threshold reached").postln});
					clarity.bus.get({|val|
						//if(verbose==true,{(this.name++" clarity: "++val[1]).postln});
						if( val[1] > thresholds.at(\clarity),
							{above_clarity_thresh=true;
							xy[0]=(xy[0]+step).clip(0,1)},
							{above_clarity_thresh=false;
							xy[0]=(xy[0]-step).clip(0,1)});
					});
					onsets.bus.get({|val|
						//if(verbose==true,{(this.name++" onsets: "++val[1]).postln});
						if( val[1] > thresholds.at(\density),
							{above_density_thresh=true;
							xy[1]=(xy[1]+step).clip(0,1)},
							{above_density_thresh=false;
							xy[1]=(xy[1]-step).clip(0,1)});
					});
					//("analysis movement: "++xy).postln;
				}, {
			//else if below threshold drift to center
					if(verbose==true,{(this.name++" drift to center").postln});
					if(xy[0] > 0.5, {
						(xy[0]=xy[0]-gravity).clip(0,0.5)},{
						(xy[0]=xy[0]+gravity).clip(0,0.5)});
					if(xy[1] > 0.5, {
						(xy[1]=xy[1]-gravity).clip(0,0.5)},{
						(xy[1]=xy[1]+gravity).clip(0,0.5)});
					//("drift to center: "++xy).postln;
				});
			});
		if (quadrant_change==true, {
		this.assign_quadrant(xy[0], xy[1]);
		//Checks to see if quadrant has changed, if so, it changes type of processing
		if (quadrant[0] == quadrant[1], {
					},{this.change_processing});
		if (quadrant_flag==true, {
				this.change_processing;
				quadrant_flag=false;
				},{});
		//Tracker processing grid changer is implemented here
			if (tracker_on==true, {
					if( tracker.any({|i,n|i>(timelimit)}), {//if any item in tracker is above timelimit
					//then choose new fadetime
					if(fade>30, {fade=2+(38.0.rand)},{fade=25+(35.0.rand)});
					this.fade_time(fade);
					(this.name++": fade time is now = "++fade).postln;
					//then choose new processing for that quadrant
					this.choose_new_processing(tracker.detectIndex({|i|i>timelimit}));
					(this.name++": processing grid changing").postln;
					if(verbose==false,{global_change=true;(this.name++": global change enabled").postln});
					quadrant_flag=true;
					tracker[tracker.detectIndex({|i|i>timelimit})]=0;
					//Change polarity for the hell of it
					if(polarity==false, {
						this.polarity_map;polarity=true;
						(this.name++": polarity mapping set").postln;
					},{
						this.nonpolarity_map;polarity=false;
						(this.name++": non-polarity mapping set").postln;

					});
				},{});
				});
			});
			});
		refresh_rate.wait;
		count=count+1;
		}}).play;
	}

	build_analysis {
		onsets = NodeProxy.control(Server.default, 2)
		.source = {
			//Density Tracker
			var buf = LocalBuf.new(512,1);
			var onsets = Onsets.kr(FFT(buf, analysis_input.ar(1)));
			var shortStats = OnsetStatistics.kr(onsets, short_win);
			var longStats = OnsetStatistics.kr(onsets, long_win);
			var shortValue = (shortStats[0]/short_win);
			var longValue = (longStats[0]/long_win);
			[shortValue, longValue];
		};

		amplitude = NodeProxy.control(Server.default, 2)
		.source = {
			//Amplitude Tracker
			var chain = FFT(LocalBuf(1024), analysis_input.ar(1));
            var loudness = Loudness.kr(chain);
			var shortAverage = AverageOutput.kr(loudness, Impulse.kr(short_win.reciprocal));
			var longAverage = AverageOutput.kr(loudness, Impulse.kr(long_win.reciprocal));
			[shortAverage, longAverage];
		};

		clarity = NodeProxy.control(Server.default, 2)
		.source = {
			var freq, hasFreq, shortAverage, longAverage;
			//Pitch hasfreq Tracker
			# freq, hasFreq = Pitch.kr(analysis_input.ar(1));
            shortAverage = AverageOutput.kr(hasFreq,Impulse.kr(short_win.reciprocal));
			longAverage = AverageOutput.kr(hasFreq,Impulse.kr(long_win.reciprocal));
			[shortAverage, longAverage];
		};

		flatness = NodeProxy.control(Server.default, 2)
		.source = {
			//Spectral Flatness Tracker
			var chain = FFT(LocalBuf(1024), analysis_input.ar(1));
            var flat = SpecFlatness.kr(chain);
			var shortAverage = AverageOutput.kr(flat, Impulse.kr(short_win.reciprocal));
			var longAverage = AverageOutput.kr(flat, Impulse.kr(long_win.reciprocal));
			[shortAverage, longAverage];
		};
	}

	change_processing {
		(quadrant_map[quadrant[0]]).value;
	}

	//assign which quadrant source is in based on x/y coordinates
	assign_quadrant { |x, y|
		quadrant = quadrant.shift(1);
		case
		    {(x<=0.45) && (y<=0.45)} {quadrant.put(0,3);tracker[3]=tracker[3]+1}//quadrant 3
		    {(x>=0.55) && (y<=0.45)} {quadrant.put(0,4);tracker[4]=tracker[4]+1}//quadrant 4
		    {(x<=0.45) && (y>=0.55)} {quadrant.put(0,2);tracker[2]=tracker[2]+1}//quadrant 2
		    {(x>=0.55) && (y>=0.55)} {quadrant.put(0,1);tracker[1]=tracker[1]+1}//quadrant 1
		    {(x<=0.55) && (x>=0.45) && (y<=0.55) && (y>=0.45)} {quadrant.put(0,0);tracker[0]=tracker[0]+1};//quadrant 0
		//correct order to prevent silences when changing quadrants??? not working?
	}

	//map different types of processing to the quadrants using the quadrant names
	map_quadrants {|names|
		names.do({|item,i|
			quadrant_map.put(i,all_processing.at(item));
			available_processing.removeAt(item);
			quadrant_names.put(i,item);
		});
	}

	//map single quadrant
	map_quadrant {|num, name|
		quadrant_map.put(num,all_processing.at(name));
		quadrant_names.put(num,name);
    }

	//change polarity
	change_polarity {
		if(polarity==false, {
			this.polarity_map;polarity=true;
			(this.name++": polarity mapping set").postln;
			},{
			this.nonpolarity_map;polarity=false;
			(this.name++": non-polarity mapping set").postln;
		});
	}

		choose_new_processing {|qrant|
		//choose_new_processing function receives a quadrant as an argument and assigns an available processing to that quadrant
		var old, new;
		//don't remap the center, keep it silent
		if(qrant!=0, {
		//get current processing type which is now "old"
		old = quadrant_names[qrant];
		//change processing to one that is available and capture its symbol
		new = available_processing.keys.choose;
		quadrant_map.put(qrant, available_processing.at(new));
		//update quadrant_names
		quadrant_names.put(qrant, new);
		//remove new processing from available
		available_processing.removeAt(new);
		//place old processing in available
		available_processing.put(old, all_processing.at(old));
		},{});
	}

	reset {
		//reset to initial parameters
		//intial placement on processing grid
		xy = [0.5,0.5];//start in center
		tracker = [0,0,0,0,0];//number of times in each quadrant area
		//TO DO: the number of data structures I have to keep track of the quadrants and the names of the processing and all the available processing etc feels very clunky. There must be a better way to manage all this information.
		//Experimenting with Dictionary for Threshold Data structure
		tfviews = Dictionary.new;
		thresholds = Dictionary.new;
		thresholds.putPairs([\amp, 4, \clarity, 0.6, \density, 1.5]);
		//If an old archive of thresholds doesn't exist, create it with the default values
		if(Archive.global.at(("sway"++this.name++"thresholds").asSymbol).isNil, {
			Archive.global.put(("sway"++this.name++"thresholds").asSymbol, thresholds);},{});
		quadrant = Array.newClear(2);
		quadrant_map = Array.newClear(5);
		//change the initial mapping setup here:
		quadrant_names = Array.newClear(5);
		quadrant_names.put(0,\silence);
		quadrant_names.put(1,\delay);
		quadrant_names.put(2,\textural);
		quadrant_names.put(3,\waveloss);
		quadrant_names.put(4,\ampmod);
		all_processing = Dictionary.new;
		all_processing.put(\silence, {this.silence});
		all_processing.put(\delay, {this.delay});
		all_processing.put(\reverb, {this.reverb});
		all_processing.put(\ampmod, {this.ampmod});
		all_processing.put(\granular, {this.granular});
		all_processing.put(\textural, {this.textural});
		all_processing.put(\pitchbend, {this.pitchbend});
		all_processing.put(\cascade, {this.cascade});
		all_processing.put(\filter, {this.filter});
		all_processing.put(\freeze, {this.freeze});
		all_processing.put(\waveloss, {this.waveloss});
		all_processing.put(\distortion, {this.distort});
		all_processing.put(\smear, {this.smear});
		all_processing.put(\microtonalcloud, {this.microtonalcloud});
		all_processing.put(\pools, {this.pools});
		all_processing.put(\amcascade, {this.amcascade});
		all_processing.put(\varspeed, {this.varspeed});

		//make all processing currently available
		available_processing = Dictionary.new;
		available_processing.put(\silence, {this.silence});
		available_processing.put(\delay, {this.delay});
		available_processing.put(\reverb, {this.reverb});
		available_processing.put(\ampmod, {this.ampmod});
		available_processing.put(\granular, {this.granular});
		available_processing.put(\textural, {this.textural});
		available_processing.put(\pitchbend, {this.pitchbend});
		available_processing.put(\cascade, {this.cascade});
		available_processing.put(\filter, {this.filter});
		available_processing.put(\freeze, {this.freeze});
		available_processing.put(\waveloss, {this.waveloss});
		available_processing.put(\distortion, {this.distort});
		available_processing.put(\smear, {this.smear});
		available_processing.put(\microtonalcloud, {this.microtonalcloud});
		available_processing.put(\pools, {this.pools});
		available_processing.put(\amcascade, {this.amcascade});
		available_processing.put(\varspeed, {this.varspeed});
		this.assign_quadrant(xy[0], xy[1]);
		this.map_quadrants(quadrant_names);
		//this next line randomizies the quadrant space on startup
		5.do({|n|this.choose_new_processing(n)});
		polarity=false;
		global_change=false;
		//quadrant_flag=true;
	}

	end {
		output.free(1);
		input.free(1);
		analysis_input.free(1);
		buffer.free;
		fftbuffer.free;
		delaybuffer.do(_.free);
		recorder.free(1);
		processing.free(1);
		onsets.free(1);
		amplitude.free(1);
		clarity.free(1);
		flatness.free(1);
		amfreq.free(1);
		rvmix.free(1);
		rvsize.free(1);
		rvdamp.free(1);
		delaytime.free(1);
		delayfeedback.free(1);
		delaylatch.stop;
		pbtime.free(1);
		pbbend.free(1);
		graintrig.free(1);
		grainfreq.free(1);
		grainpos.free(1);
		grainsize.free(1);
		granpos.free(1);
		granenvspeed.free(1);
		granrate.free(1);
		filtfreq.free(1);
		filtrq.free(1);
		freezedurmin.free(1);
		freezedurmax.free(1);
		freezeleg.free(1);
		texturalmin.free(1);
		texturalmax.free(1);
		texturalsusmin.free(1);
		texturalsusmax.free(1);
		texturalposrate.free(1);
		texturalpostype.free(1);
		texturalrate.free(1);
		wldrop.free(1);
		wloutof.free(1);
		wlmode.free(1);
		dslevel.free(1);
		smlevel.free(1);
		timespread.free(1);
		pitchspread.free(1);
		plshimmer.free(1);
		plamp.free(1);
		plverb.free(1);
		vsspeed.free(1);


		analysis_loop.stop;
		this.clear;
		//Server.freeAll;
	}

}