// jAngular.js

var jAngular =  {};

var jAngularScopes = {};

jAngular.PREFIX = "ng-";

var Scope = function() {
	var parentScope;
	var out = {
		add: function(name, model) {
			this[name] = model;
		},
		eval: function(code) 
		{
			var scope = this;				
			
			var name = code;
			var cut = code.indexOf(".");
			if( cut > -1)
			{
				name = code.substring(0,cut);
			}
			else
			{
				name = code;
			}
			if(  typeof scope[name] != 'undefined' )
			{
				var found = eval("this." + code);
				return found;
			}
			else if(this.parentScope != null)
			{
				return this.parentScope.eval(code);
			}
			else
			{
				var found = eval(code);
				return found;
			}
		},
		get: function(name) 
		{
			var found = this[name];
			return found;
		},
		createScope: function()
		{
			var newscope = new Scope();
			newscope.parentScope  = this;
			return newscope
		}
	}
	return out;
}


var Replacer = function() {
	var out = {
		regex : /{{([^{]+)}}/g ,
		replace: function( inCode, scope)
		{
			var text = inCode;
			var m;
			while ((m = this.regex.exec(text)) !== null)
			{
				try
				{
					text = text.replace(m[0], scope.eval(m[1]));
				}
				catch( err )
				{
					//Dont log?
					//console.log(err);
				}	
			}
			return text;
		}	
	};
	return out;
};

jAngular.replacer = new Replacer();

jAngular.findScope = function(scopename)
{
	var foundscope = jAngularScopes[scopename];
	//todo: Default scope?
	return  foundscope;
}

jAngular.findScopeFor = function(indiv)
{
	var theel = $(indiv);
	var found = theel.closest("[ng-scope]");
	var scopename = found.attr("ng-scope");
	return jAngular.findScope(scopename);
}

jAngular.addScope = function(scopename, inScope)
{
	jAngularScopes[scopename] = inScope; 
}

jAngular.processRepeat = function(li , scope)
{
	var parent = li.parent();	
	var element = parent.get(0);
	var vars = li.attr(jAngular.PREFIX + "repeat");
	var split = vars.indexOf(" in ");
	
	var rowname = vars.substring(0,split);
	var loopname = vars.substring(split + 4,vars.length );
	
	var rows = scope.eval(loopname);  //TODO: Find the name
	
	//set a local scope of asset = rows[i];
	var origContent = element.origContent;
	if( typeof( origContent ) === 'undefined' )
	{
		origContent = parent.html();

		element.origContent = origContent;
	}
	li.removeAttr(jAngular.PREFIX + "repeat");
	if( rows )
	{
		$.each(rows, function(index, value) {
			//TODO: replace scope variables
			var localscope = scope.createScope();
			localscope.add("loopcountzero",index);
			localscope.add("loopcountone",index + 1);
			
			localscope.add(rowname,value);
			
			var template = li.clone();
			li.before(template);
			jAngular.process( template, localscope);
		});
	}
	li.remove();
}

jAngular.process = function(div, scope)
{
	//console.log("Processing: " + div.get(0).localName, div);
	var element = div.get(0);
	var origContent = element.origContent;
	if( origContent )
	{
		div.html(origContent);
	}

	var newscope  = div.attr(jAngular.PREFIX + "scope");
	if( newscope )
	{
		scope = jAngular.findScope(newscope);
	}
	
	var processchildren = true;
	if( scope) 
	{
		$.each(element.attributes, function() 
		{
			var attr = this;
			
			if( !attr.value )
			{
				return;
			}
			var aname = attr.name;
		
			var code = div.data("origattr" + aname);
			// should actually require the end user to explicitly use the provided loopcount variables instead
			// if (aname == "id" && scope.loopcountone)
			// {
			// 	div.attr('id', attr.value + scope.loopcountone);
			// }
			if(!code ) 
			{
				if( attr.value.indexOf("{{") > -1)
				{
					code = attr.value;
					if(  aname.indexOf(jAngular.PREFIX) != 0 ) //Dont backup ng- prefix items
					{
						div.data("origattr" + aname, code);   //backup original code for future renderings
					}
				}
			}

			if( code )
			{
				var val = jAngular.replacer.replace(code,scope);
				if( aname == jAngular.PREFIX + "src" )
				{
					//copy it
					div.attr("src",val);
				}
				else
				{
					div.attr(aname,val);
				}
			}

			if( aname.indexOf(jAngular.PREFIX) > -1 )
			{
				switch (aname)
				{
					case jAngular.PREFIX + "click":
						div.on('click', function(e)
						{
							e.preventDefault();
							var theel = jQuery(this);
							var code = theel.attr(jAngular.PREFIX + "click");
							scope.eval(code);
						});
						break;
						//behavior is same as before. we can't fall through here
				case jAngular.PREFIX + "repeat":
					//We are going to loop the content of this div/li
					jAngular.processRepeat(div,scope);
					processchildren = false;
				}
			}  
		}); 
		if( processchildren )
		{
			//TODO: Deal with text variables
			var code = div.data("origtext");   
			if( !code && div.children().length == 0) 
			{
				var orig = div.text();
				if( orig && orig.indexOf("{{") > -1)
				{
					div.data("origtext", orig);   //backup original code for future renderings
					code = orig;
				}
			}
			if( code )
			{
				 var val = jAngular.replacer.replace(code,scope);
				 div.text(val);
			}
		}	
	}	
	if( processchildren )
	{
		div.children().each(function()
		{
			jAngular.process($(this),scope);
		});
	}	
}

jAngular.render = function(div, scope)
{
		/*
		when rendering ng-repeat for the image carousel
		we will need to only loop maximum editor.imageCarouselPageAssetCount times

		begin asset rendering at Asset# editor.imageCarouselPageIndex*editor.imageCarouselPageAssetCount+1
		*/
		var replacer = new Replacer();
		
		if( div )
		{
				div = div + " ";
		} 
		else
		{
				div = "";
		}
		
		var toplevel = $(div);
		
		//look at all the attributes
		jAngular.process(toplevel,scope);
		
};