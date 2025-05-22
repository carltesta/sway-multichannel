//Analysis Select

SwayConstructor(\sway, 6);

(
~items = Array.fill(Sway.all.size, {|i| ("analyis"++i).asString});
~effects = #[silence,reverb,ampmod,delay,freeze,pitchbend,filter,granular,textural,cascade,waveloss,distort,smear,microtonalcloud,pools,amcascade,varspeed];

~anal = Window.new(bounds: Rect(0,0,400,600),scroll: true).front;

~anal.layout = VLayout();

~anal.layout.add(

~list = ListView(~anal,Rect(10,10,120,200))
.selectionMode_(\multi)
    .items_(~items)
    .background_(Color.clear)
    .hiliteColor_(Color.green(alpha:0.6))
.selection_((0..~items.size-1))
    .selectionAction_({ arg sbs;
	var instance = sbs.value;
	if(Sway(instance.asSymbol).analysis_on == true,
	{Sway(instance.asSymbol).analysis_on=false},
	{Sway(instance.asSymbol).analysis_on=true});
	//if(but.value==0, {instance.analysis_on=true},{instance.analysis_on=false});
//[sbs.value, v.items[sbs.value]].postln; // .value returns the integer
    });
);



Sway.all.keysValuesDo({|name, instance, i|

	~anal.layout.add(
	StaticText(~anal,Rect(0,0,50,30)).string_(instance.asString);
	);
	~anal.layout.add(
	PopUpMenu(~anal, Rect(10,10,120,30)).items_(~effects).font_(Font("Osaka",14))
	.action_({|menu|
	[menu.value,menu.item].postln;
	instance.perform(menu.item);
});
	);
});

~anal.layout.add(
StaticText(~anal,Rect(0,0,50,30)).string_("Set All");
);
~anal.layout.add(
~setAllmenu = PopUpMenu(~anal, Rect(0,0,120,30)).items_(~effects).font_(Font("Osaka", 14));
		~setAllmenu.action_({|menu|
			[menu.value,menu.item].postln;
			Sway.all.keysValuesDo({|name,instance,i|
				instance.perform(menu.item);
			});
		});
);
)




//Sway(\0).analysis_on;