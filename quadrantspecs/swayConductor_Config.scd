//Sway Conductor Quadrant Spec Configuration File
//You use this file to create different quadrant combinations and assemble them into maps you can use in Sway

//I'm assuming that ~sway is the name of the instance of SwayConductor currently running

//|name, key, assocProcessors, assocSpatializers, polarityProcessors, polaritySpatializers, fadeTime=1|

~sway = SwayConductor.start(~swayInstances);

~sway.addQuadrantSpec("Delay", \delayland, [\delay], [\static,\multi], [\delay], [\line,\random]);

~sway.addQuadrantSpec("Amp Mod and Delays", \ampModDelay, [\delay,\ampMod], [\static, \multi], [\delay, \ampMod], [\random, \line]);

~sway.addQuadrantSpec("Pitch Bend", \pitchbendland, [\pitchbend], [\static], [\ampMod], [\line]);

~sway.addQuadrantSpec("Pools", \poolland, [\reverb, \ampmod, \filter], [\static, \multi, \vibrato, \line],fadeTime: 30);

~sway.addQuadrantSpec("Concat", \concatland, [\concat, \reverb], [\static, \multi], [\ampmod, \concat], [\random, \vibrato, \line], fadeTime: 10);


~sway.addQuadrantSpec("Everything", \everything, [\concat, \reverb, \filter, \ampmod, \freeze, \pitchbend], [\vibrato, \multi, \line, \static, \random, \forwardline], fadeTime: 30);

~sway.addMap("section1", \section1, \pitchbend, \ampmod, \ampmod, \pitchbend);

~sway.addMap("section2", \section2, \delay, \ampmod, \pitchbend, \reverb);

~sway.addMap("section3", \section3, \concatland, \concatland, \concatland, \concatland);

~sway.addMap("section4", \section4, \everything, \everything, \everything, \everything);

~sway.addMap("section5", \section5, \poolland, \poolland, \poolland, \poolland);

~sway.addMap("section6", \section6, \everything, \everything, \everything, \everything);

~sway.addMap("silence", \silence, \silence, \silence, \silence, \silence);

//MIDI Control
MIDIdef.cc(\section1, {|val,num,chan,src|
	if(val==127,{~sway.runMap(\section1)});
	"Starting Map for Section 1".postln;
},21);

MIDIdef.cc(\section2, {|val,num,chan,src|
	if(val==127,{
		//~sway.randomPolarity;
		~sway.runMap(\section2)});
	"Starting Map for Section 2".postln;
},22);

MIDIdef.cc(\section3, {|val,num,chan,src|
	if(val==127,{
		//~sway.randomPolarity;
		~sway.runMap(\section3)});
	"Starting Map for Section 3".postln;
},23);

MIDIdef.cc(\section4, {|val,num,chan,src|
	if(val==127,{
		//~sway.randomPolarity;
		~sway.runMap(\section4)});
	"Starting Map for Section 4".postln;
},24);

MIDIdef.cc(\section5, {|val,num,chan,src|
	if(val==127,{
		//~sway.randomPolarity;
		~sway.runMap(\section5)});
	"Starting Map for Section 5".postln;
},25);

MIDIdef.cc(\section6, {|val,num,chan,src|
	if(val==127,{~sway.runMap(\section6)});
	"Starting Map for Section 6".postln;
},26);

MIDIdef.cc(\silence, {|val,num,chan,src|
	if(val==127,{~sway.runMap(\silence)});
	"Starting Map for Silence".postln;
},20);