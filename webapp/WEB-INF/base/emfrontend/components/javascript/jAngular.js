// jAngular.js
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
							text = text.replace(m[0], eval(m[1]));
						}
						catch( err )
						{
							//Dont log?
							console.log(err);
						}	
					}
					return text;
				}	
				/*
				replace : function( inCode, scope)
				{
						if( inCode == null)
						{
								return inCode;
						}
						var start = 0;
						while( (start = inCode.indexOf("{{",start)) != -1)
						{
								var end = inCode.indexOf("}}",start);           
								if( end == -1)
								{
										break;
								}
								
								var key = inCode.substring(start+2,end);
								var value = null;
								try
								{
										value = scope.eval(key); //check for property
								} 
								catch ( err )
								{
										value = err.message;
								}   
								
								if( typeof value !== 'undefined')
								{
										//sub = replace(sub,inValues);  //recursive
										inCode = inCode.substring(0,start) + value + inCode.substring(end+2);
										start = start + key.length + 1;
								}               
								else
								{
										start = end; //could not find a hit, go to the next one
								}
						}
						return inCode;
				}   
				*/
		};
		return out;
};

var jAngular =  {};

var jAngularScopes = {};

jAngular.replacer = new Replacer();

jAngular.findScope = function(scopename)
{
		var foundscope = jAngularScopes[scopename];
		//todo: Default scope?
		return  foundscope;
}

jAngular.findScopeFor = function(inElement)
{
		var theel = $(inElement);
		var found = theel.closest("[ng-scope]");
		var scopename = found.attr("ng-scope");
		return jAngular.findScope(scopename);
}       

jAngular.addScope = function(scopename, inScope)
{
		jAngularScopes[scopename] = inScope; 
}

/*
jAngular.replace = function(selector, scope)
{
		var replacer = new Replacer();
		var div = $(selector);
		var element = div.get(0);
		var origContent = element.origContent;
		if( typeof( origContent ) === 'undefined' )
		{
				origContent = div.html();

				element.origContent = origContent; 
		}
		var evalcontent = replacer.replace(origContent,scope);
		if( evalcontent != origContent)
		{
			div.html(evalcontent);
		}	

}
*/

jAngular.processRepeat = function(li , scope)
{
	var li = $(this);
	var vars = li.attr("ng-repeat");
	var split = vars.indexOf(" in ");
	var lid = li.contents().attr("id");
	
	var rowname = vars.substring(0,split);
	var loopname = vars.substring(split + 4,vars.length );
	
	var rows = scope.eval(loopname);  //TODO: Find the name
	
	//set a local scope of asset = rows[i];
	var origContent = this.origContent;
	if( typeof( origContent ) === 'undefined' )
	{
			origContent = li.html();

			this.origContent =origContent; 
	}
	li.html("");
	if( rows )
	{
			$.each(rows, function(index, value) {
					//TODO: replace scope variables
					var localscope = scope.createScope();
					localscope.add("loopcountzero",index);
					localscope.add("loopcountone",index + 1);
					
					localscope.add(rowname,value);

					var child = li.append(origContent);
					jAngular.process( child, localscope);
//					var evalcontent = replacer.replace(origContent,localscope);
//					evalcontent = evalcontent.replace("ng-src","src");
//					li.append(evalcontent);
				});
		 }
}

jAngular.process = function(div, scope)
{
	var newscope  = div.attr("ng-scope");
	if( newscope )
	{
		scope = jAngular.findScope(newscope);
	}

	if( scope) 
	{
		$.each(div.attributes, function() 
		{
		   var attr = this;
		   var code = div.data("origattr" + attr.name);
		   if( !code )
		   {
			   if( attr.value.indexOf("{{") > -1)
			   {
				   code = attr.value;
				   div.data("origattr" + attr.name, code);   //backup original code for future renderings
			   }
		   }
		   if( code )
		   {
			   var val = jAngular.replacer.replace(code,scope);
			   attr.attr(attr.name,val);
		   }
		   var aname = attr.name;
		   if( aname.indexOf(jAngular.PREFIX) > -1 )
		   {
			   if( aname == jAngular.PREFIX + "click" )
			   {
				   $div.on('click', function(e)
					{
						e.preventDefault();
						var theel = jQuery(this);
						var code = theel.attr("ng-click");
						eval(code);
					});
			   }
			   if( aname == jAngular.PREFIX + "repeat" )
			   {
				   //We are going to loop the content of this div/li
				   jAngular.processRepeat(div,scope);
			   }
		   }
		   //TODO: Now check for loops	   
		}  
		//TODO: Deal with text variables
		var code = div.data("origtext");   
		if( !code ) 
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
	div.children().each(function()
	{
		jAngular.process(this,scope);
	});
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
		jAngular.process(div,scope);

		/*
		jAngular.replaceRows(div, scope);

		$(div + " .jangular-render" ).each(function()
		{
			// possible fix to having to add class:
			// check in each filter function for .text() OR defined origContent with brackets
			var element = $(this); // cast to jQuery object
			var origContent = this.origContent;
			if( typeof( origContent ) === 'undefined' )
			{
					origContent = element.text();
					this.origContent = origContent;
			}
			var text = origContent;
			var regex = /{{([^{]+)}}/g;
			var m;
			while ((m = regex.exec(text)) !== null)
			{
				try
				{
					text = text.replace(m[0], eval(m[1]));
				}
				catch( err )
				{
					//Dont log?
				}	
			}
			element.text(text);

		});
		*/
		
};


jQuery(document).ready(function() 
{       
		jAngular.livequery();
});
