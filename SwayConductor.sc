SwayConductor {

	classvar <>numPlayers=6;

	var <>swayInstances, globalVolume, <>currentMap, <>swayMaps, <>quadrantSpecs;

	*start {
		|swayInstances,globalVolume=1|

		^super.newCopyArgs(swayInstances,globalVolume);
	}

	addMap { |name, key, quadrantSpec1, quadrantSpec2, quadrantSpec3, quadrantSpec4|

		//A map contains a Dictionary of quadrantSpecs or processing types each mapped to a specific quadrant. quadrantSpecs are basically a way to combine a specific set of processing and spatializations together
		//A single map could have enough possibilities to suffice for a single performance

		(this.swayMaps.isNil).if({
			swayMaps = Dictionary.new;
		});
		this.swayMaps.put(key, Dictionary[
			\name -> name,
			\key -> key,
			\q1 -> quadrantSpec1,
			\q2 -> quadrantSpec2,
			\q3 -> quadrantSpec3,
			\q4 -> quadrantSpec4
		]);
	}

	runMap {|map, swayInstance|
		//this method needs to first extract the information from the specs in the maps
		//If the map has a QuadrantSpec then extract the information, otherwise just pass the processor types to each quadrant

		if(swayInstance.notNil, {
			this.processMap(swayInstance,map);
		}, {
			swayInstances.do({|sway,n|
				this.processMap(sway, map);
			});
		});
	}

			//You don't to use the following method it's just here for the runMap method to use internally, this is to process a Map for a SINGLE instance of Sway
	processMap { |sway,map|
		var key, polarity, processor, spatializer;
			4.do({|n|
			key = this.swayMaps[map][("q"++(n+1)).asSymbol];
			key.postln;
			polarity = sway.polarity;
			if(this.quadrantSpecs.notNil,{
			if(this.quadrantSpecs.keys.asArray.includes(key), {
				if(polarity&&this.quadrantSpecs[key][\polarityProcessors].notNil, {
					processor = this.quadrantSpecs[key][\polarityProcessors].choose;
					spatializer = this.quadrantSpecs[key][\polaritySpatializers].choose;
					"this is a QuadrantSpec with Polarity".postln;
					sway.assignProcessorToQuadrant(processor.asSymbol, (n+1));
					sway.assignSpatializerToQuadrant(spatializer, (n+1));
				},{
					processor = this.quadrantSpecs[key][\processors].choose;
					spatializer = this.quadrantSpecs[key][\spatializers].choose;
					"this is a QuadrantSpec".postln;
					sway.assignProcessorToQuadrant(processor.asSymbol, (n+1));
				sway.assignSpatializerToQuadrant(spatializer, (n+1));
			})},{
				processor = key;
				spatializer = \static;
				"this is not a QuadrantSpec".postln;
				sway.assignProcessorToQuadrant(processor.asSymbol, (n+1));
				sway.assignSpatializerToQuadrant(spatializer, (n+1));
			});},{
				processor = key;
				spatializer = \static;
				"this is not a QuadrantSpec".postln;
				sway.assignProcessorToQuadrant(processor.asSymbol, (n+1));
				sway.assignSpatializerToQuadrant(spatializer, (n+1));
			});
		});
		}

	addQuadrantSpec {|name, key, processors, spatializers, polarityProcessors, polaritySpatializers, fadeTime=1|
		(this.quadrantSpecs.isNil).if({
			quadrantSpecs = Dictionary.new;
		});
		this.quadrantSpecs.put(key, Dictionary[
			\name -> name,
			\key -> key,
			\processors -> processors,
			\spatializers -> spatializers,
			\polarityProcessors -> polarityProcessors,
			\polaritySpatialziers -> polaritySpatializers,
			\fadeTime -> fadeTime
		]);
	}

	generateQuadrantSpec { |swayInstance|
		var name, key, processors, spatializers, fadeTime=30;
		(this.quadrantSpecs.isNil).if({
			quadrantSpecs = Dictionary.new;
		});

		name = String.rand;
		key = name;
		processors = this.selectNum(swayInstance.processors.keys, swayInstance.processors.keys.size.rand);
		spatializers = this.selectNum(swayInstance.spatializers.keys,swayInstance.spatializers.keys.size.rand);
		fadeTime = fadeTime.rand;

		this.addQuadrantSpec(name, key, processors, spatializers, fadeTime: fadeTime);
		(name++" Quadrant Spec added with "++processors++" and "++spatializers++" !").postln;

	}

	selectNum { |array=#[1,2,3,4], num=3|
	var result = array.asArray.scramble.copyRange(0, num-1);
	^result;
}

	solo { |chan=0|

			swayInstances.do({|sway,n|
			sway.input.set(\chan, chan);
			sway.analysis_input.set(\chan, chan);
			("solo started with channel "++chan).postln;
			});
	}

	resetInputs {
		swayInstances.do({|sway, n|
			sway.input.set(\chan, n);
			sway.analysis_input.set(\chan, n);
			"inputs reset to default".postln;
		});
	}

	randomPolarity {
		swayInstances.do({|sway, n|
			sway.polarity = [true, false].choose;
		});
	}

}