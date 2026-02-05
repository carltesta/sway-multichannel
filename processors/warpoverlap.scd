//WarpOverlap

~swayInstances.do({|sway, n|
	sway.addProcessor("Warp Overlap", \warpoverlap, {|swayInstance|
		{
			var warp = WarpOverlap.ar(1, swayInstance.buffer.bufnum, 4, 0, 0.5, [3,7,5,-12].midiratio.asRef, 6, 1, 0.11,8, 0.1);
			warp;
	}});
});