//Analysis Select
(
~anal = Window.new(bounds: Rect(0,0,400,400),scroll: true).front;
~anal.view.decorator = FlowLayout(~anal.view.bounds, 10@10,20@5);

~items = Array.fill(Sway.all.size, {|i| ("analyis"++i).asString});


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

~effects = #[silence,reverb,ampmod,delay,freeze,pitchbend,filter,granular,textural,cascade,waveloss,distort,smear,microtonalcloud,pools,amcascade,varspeed];

Sway.all.keysValuesDo({|name, instance, i|

	StaticText(~anal,Rect(0,0,50,30)).string_(instance.asString);
	PopUpMenu(~anal, Rect(10,10,120,30)).items_(~effects).font_(Font("Osaka",14))
.action_({|menu|
	[menu.value,menu.item].postln;
	instance.perform(menu.item);
});
});

~setAllmenu = PopUpMenu(~anal, Rect(0,0,120,30)).items_(~effects).font_(Font("Osaka", 14));
		~setAllmenu.action_({|menu|
			[menu.value,menu.item].postln;
			Sway.all.keysValuesDo({|name,instance,i|
				instance.perform(menu.item);
			});
		});

)


//Sway(\0).analysis_on;