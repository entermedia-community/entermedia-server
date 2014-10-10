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
		eval: function(name) 
		{
			var scope = this;				
			
			var command = name;
			if( name.indexOf("if") == 0 || name.indexOf("scope") == 0 ) //see if it starts with scope.
			{
				//do nothing
			}
			else
			{
				command = "this." + name;
			}
			
			var found = eval(command);
			if( parentScope != null && found == null )
			{
				return parentScope.eval(name);
			}
			return found;				
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
	parent.html("");
	var child = $(origContent);
	child.removeAttr(jAngular.PREFIX + "repeat");
	if( rows )
	{
		$.each(rows, function(index, value) {
			//TODO: replace scope variables
			var localscope = scope.createScope();
			localscope.add("loopcountzero",index);
			localscope.add("loopcountone",index + 1);
			
			localscope.add(rowname,value);
			var copy = child.clone().appendTo(parent);
			jAngular.process( copy, localscope);
		});
	}
}

jAngular.process = function(div, scope)
{
	//console.log("Processing: " + div.get(0).localName, div);
	var element = div.get(0);
	var newscope  = div.attr(jAngular.PREFIX + "scope");
	if( newscope )
	{
		scope = jAngular.findScope(newscope);
	}
	var hascontent = true;
	if( scope) 
	{
		$.each(element.attributes, function() 
		{
		   var attr = this;
		   var aname = attr.name;
		   
		   var code = div.data("origattr" + aname);
		   if (aname == "id" && scope.loopcountone)
		   {
				div.attr('id', attr.value + scope.loopcountone);
		   }
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
			   if( aname == jAngular.PREFIX + "click" )
			   {
				   div.on('click', function(e)
					{
						e.preventDefault();
						var theel = jQuery(this);
						var code = theel.attr(jAngular.PREFIX + "click");
						scope.eval(code);
					});
			   }
			   else if( aname == jAngular.PREFIX + "repeat" )
			   {
				   //We are going to loop the content of this div/li
				   jAngular.processRepeat(div,scope);
				   hascontent = false;
			   }
		   }
		   //TODO: Now check for loops	   
		}); 
		if( hascontent )
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
	if( hascontent )
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