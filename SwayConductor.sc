SwayConductor {

	classvar <>numPlayers=6;

	var swayDictionary, globalVolume, <>swayMaps, <>quadrantSpecs;

	*start {
		|swayDictionary,globalVolume=1|

		^super.newCopyArgs(swayDictionary,globalVolume);
	}

	addMap { |name, key, quadrantSpec1, quadrantSpec2, quadrantSpec3, quadrantSpec4|

		//A map contains a Dictionary of quadrantSpecs each mapped to a specific quadrant

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

	runMap {

	}

	addQuadrantSpec {|name, key, assocProcessors, assocSpatializers, polarityProcessors, polaritySpatializers, fadeTime=1|
		(this.quadrantSpecs.isNil).if({
			quadrantSpecs = Dictionary.new;
		});
		this.quadrantSpecs.put(key, Dictionary[
			\name -> name,
			\key -> key,
			\processors -> assocProcessors,
			\spatializers -> assocSpatializers,
			\polarityProcessors -> polarityProcessors,
			\polaritySpatialziers -> polaritySpatializers,
			\fadeTime -> fadeTime
		]);
	}

	generateQuadrantSpec { |swayInstance|
		var name, key, processors, spatializers, polarityProcessors, polaritySpatializers, fadeTime;
		(this.quadrantSpecs.isNil).if({
			quadrantSpecs = Dictionary.new;
		});

		name = String.rand;
		key = name;
		processors = swayInstance.processors.keys.choose();

	}

}
/*
1.0.rand

y = ~swayInstances[0].processors.keys.collect({|key| key.postln;if((1.0.rand)>0.5, {key}, {\nil})});

	y = ~swayInstances[0].processors.keys

	x = [0,1,2,3,4,5]
	y = x.collect({|x|