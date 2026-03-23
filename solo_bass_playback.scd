~buf = Buffer.read(s, "C:/Users/Carl/Documents/sc_documents/wfs/sway_wfs/bass_solo_test.wav", 0);

~playback = NodeProxy.audio(s, 1);
~playback.source = { |startPos=88200| PlayBuf.ar(1, ~buf, 1.0, \start.tr(0),startPos) };

( // make a simple SoundFileView
y = Window.screenBounds.height - 120;
w = Window.new("soundfile test", Rect(200, y, 740, 100)).alwaysOnTop_(true);
w.front;
a = SoundFileView.new(w, Rect(20, 20, 700, 60));

f = SoundFile.new;
f.openRead("C:/Users/Carl/Documents/sc_documents/wfs/sway_wfs/bass_solo_test.wav");
// f.inspect;

a.soundfile = f;            // set soundfile
a.read(0, f.numFrames);     // read in the entire file.
a.refresh;                  // refresh to display the file.
      // mouseUpAction
a.mouseUpAction = {
	var startPos = a.selections[a.currentSelection][0];
	~playback.set(\startPos, startPos, \start, 1);
    ("mouseUp, current selection is now:"
		+ a.selections[a.currentSelection][0]).postln;
};
)

~playback.index

~swayInstances[0].output.index

~swayInstances[0].loopQuadrantRead = false;

//Alternative Analysis Options
~swayInstances.do({|sway, n|
	sway.density.source = {
		var density = FluidOnsetFeature.kr(sway.input.ar(1), 0, 10, 0);
		SendReply.kr(Impulse.kr(1), ("/"++sway.channel++"/density/").asSymbol, density);
		density;
	};
	sway.amplitude.source = {
		var amplitude = FluidAmpFeature.ar(sway.input.ar(1));
		SendReply.kr(Impulse.kr(1), ("/"++sway.channel++"/amplitude/").asSymbol, amplitude);
		amplitude;
	};
	sway.clarity.source = {};
});