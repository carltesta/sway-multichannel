# Sway

Interactive Live Processing Environment for any number of musicians. Run in SuperCollider. Requires Scott Carver's Singleton Quark.

---

## To Run

```
//SwayConstructor(symbol of instance, number of channels);
//i.e.
SwayConstructor(\sway, 6)
//This will generate the GUI and you can proceed to calibrate the system
```

## Audio Input Settings
```
*/ By default, Sway will route a stereo mix of all of the processed audio out channels 1 and 2. It will then send indiviudally processed tracks out the subsequent channels. So if you are processing 6 instruments/channels, it will send the stereo mix out 1 + 2 and then send the processed instruments out channels 3-8 for multitrack audio capture (if desired). Audio coming in on channel 1 is set out channel 3, channel 2 is sent out 4, etc. If you want to change which input is being sent to which channel in Sway you can change that by executing the following code once Sway is running:*/

//Set 1st channel's (in Sway) mic input to 4
Sway(\0).input.set(\chan, 3);
Sway(\0).analysis_input.set(\chan, 3);
//Set 2nd channel's (in Sway) mic input to 1
Sway(\1).input.set(\chan, 0);
Sway(\1).analysis_input.set(\chan, 0);

/* Now Sway's first channel will process the audio coming in on channel 4 and Sway's second channel will process the audio coming in on channel 1. The analysis input (the channel that each Sway instance is listening to in order to control the processing parameters is decoupled in the system, so you have to manually assign the analysis input to the corressponding channel.*/
