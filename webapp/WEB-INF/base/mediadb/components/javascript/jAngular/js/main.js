$(document).ready(function()
{
	/* test model for jAngular */

	var firstScope = new Scope();
	var worldtext = "world";
	var items =
		[{
			name: "name 1",
			info: "test info 1",
			nested: {info:"nested info 1"},
			clear: function(input) {alert(input)},
			tasks: ["do the thing", "fix broken stuff", "walk the dog", "eat food"],
			type: "testy"
		}
		,
		{
			name: "name 2",
			info: "test info 2",
			nested: {info:"nested info 2"},
			clear: function(input) {alert(input)},
			tasks: ["do the thing", "fix broken stuff", "walk the dog", "eat food"],			
			type: "annoying"
		}
		,
		{
			name: "name 3",
			info: "test info 3",
			nested: {info:"nested info 3"},
			clear: function(input) {alert(input)},
			tasks: ["do the thing", "fix broken stuff", "walk the dog", "eat food"],			
			type: "fruity"
		}];
	firstScope.add("items", items);
	firstScope.add("worldtext", worldtext);
	firstScope.add("scopename", "firstScope");

	jAngular.addScope("firstScope", firstScope);

	var secondScope = new Scope();
	var babyMoose = "BABY MOOSE";
	var secondItems =
		[{
			name: "name 21",
			info: "test info 21",
			nested: {info:"nested info 21"},
			clear: function(input) {alert(input)},
			tasks: ["do the thing", "fix broken stuff", "walk the dog", "eat food"],			
			type: "fruity"
		}
		,
		{
			name: "name 22",
			info: "test info 22",
			nested: {info:"nested info 22"},
			clear: function(input) {alert(input)},
			tasks: ["do the thing", "fix broken stuff", "walk the dog", "eat food"],			
			type: "testy"
		}
		,
		{
			name: "name 23",
			info: "test info 23",
			nested: {info:"nested info 23"},
			clear: function(input) {alert(input)},
			tasks: ["do the thing", "fix broken stuff", "walk the dog", "eat food"],			
			type: "annoying"
		}];
		
	secondScope.add("secondItems", secondItems);
	secondScope.add("moose", babyMoose);
	secondScope.add("scopename", "secondScope");

	jAngular.addScope("secondScope", secondScope);

	//jAngular.render("#wrapper", firstScope);
	//jAngular.render("#secondWrapper", secondScope);

	jAngular.render("body");

})
