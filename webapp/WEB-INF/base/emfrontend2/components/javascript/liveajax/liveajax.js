/*
Known bugs:
If double clicks: Make sure livequeryrunning is correct
If fancybox errors: Make sure get is correct
If list2 not init: Make sure .html is correct and livequeryrunning
*/


(function ($) {
    var oldLoad = $.fn.load;
    $.fn.load = function(inArg,maybeData,inComplete) 
    {
    	var oldscope = this;
    	if (typeof(maybeData) != "object") {
    		inComplete = maybeData;
    		maybeData = {};
    	}
		var returned = oldLoad.call(oldscope,inArg, maybeData, function()
		{
			if( typeof(inComplete) == "function" )
			{
				// They passed in parameters
				inComplete.call(this);
			}
			//console.log("html complete");
			$(document).trigger("domchanged");
		});
		return returned;
    };

   var oldhtml = $.fn.html;
    $.fn.html = function(arg) 
    {
    	if( arguments.length == 0 )
    	{
			var returned = oldhtml.call($(this));
			return returned;
    	}
    	
		var returned = oldhtml.call($(this),arg);
		$(document).trigger("domchanged"); //a component may be adding html that will call this
		return returned;
    };
   
    var oldreplaceWith = $.fn.replaceWith;
    $.fn.replaceWith = function(arg) 
    {
		var returned = oldreplaceWith.call($(this),arg);
		//console.log("Called replacewith on " +	$(this).selector, arg.length );	
		$(document).trigger("domchanged");
		
		return returned;
    };
    var oldajaxSubmit = $.fn.ajaxSubmit;
    $.fn.ajaxSubmit = function() 
    {
    	var form = $(this);
    	var params = arguments[0];
    	var oldsucess = params.success;
		params.success = function(arg1,arg2,arg3,arg4)
		{
			if( oldsucess != null )
			{
				oldsucess.call(form,arg1,arg2,arg3,arg4);
			}	
			$(document).trigger("domchanged"); //TODO: Put this in the success section	
		};
		var returned  = oldajaxSubmit.call(form,params);
		params.success = oldsucess;
		return returned;
		
    };
    
    /*
    //This is problematic because we call get for various reasons
    var oldget = $.fn.get;
    $.fn.get = function() 
    {			
    	if( arguments.length == 3 )
		{
			//console.log("Called FAKE get on " ,arguments );
			var oldsucess = arguments[2];
			return oldget.call($(this),arguments[0],arguments[1],function()
			{
				oldsucess.call(this);	
				//console.log("Called get on " +	$(this).selector );	
				$(document).trigger("domchanged"); 
			});
		}
		else
		{
			//console.error("Wrong number of params for get ", arguments);
			return oldget.call($(this));
		}
    };
    */
    
    
})(jQuery);



(function( $ ) { 
 	var regelements = new Array();
 	var eventregistry = new Array();
 	var livequeryrunning = false;
 	//Listener
 	
 	$(document).on( "domchanged", function(event,args)
 	{
 		if( livequeryrunning )
 		{
 			//console.log("Skipping reload");
 			return;
 		}
 		//console.log("domchanged reload");
		var chunck;
 		if( typeof args == Array )
 		{
 			chunck = $(args[0],args[1]);
 		}
 		else
 		{
 			chunck = document;
 		}
 		$.each(regelements,function()
 		{
 			var item = this;
 			var funct = item.function;
 			$(item.selector,chunck).each(function()
 			{
 				try
 				{
	 				var node = $(this);
 					if( node.data("livequeryinit" + item.selector) == null )
 					{
 				 		//console.log("Not enabled: " + item.selector );
 					 	node.data("livequeryinit" + item.selector, true);
 					 	funct.call(node);
	 				}
	 			}	 
	 			catch ( error )
	 			{
	 				 console.log("Could not process: " + item.selector , error); 
	 			}		
 			});
		});
 		//TODO: Loop over events ones and register them
 		$.each(eventregistry,function()
 		{
 			var listener = this;
 			$(listener.selector,document).each(function()
 			{
 				var node = $(this);
 				if( node.data("livequery") == null )
 				{
 				 	//console.log("Registering " + listener.selector );
 					node.on(listener.event,listener.function);	
 				}	
 				else
 				{
 					//console.log("already Registered " + listener.selector );
 				}
 			});
 		});
		//We need to do this as the end in case there are more than one click handlers on the same node
		$.each(eventregistry,function()
 		{
 			var listener = this;
 			$(listener.selector,document).each(function()
 			{
 				var node = $(this);
 				if( node.data("livequery") == null )
 				{
 				 	node.data("livequery", true);
 				}
 			});
 		});
 	});  //document.on
	
	lQuery = function(selector) 
	{
		var runner = {};
		runner.livequery = function()
		{
			livequeryrunning  = true;
			var nodes = jQuery(selector);
			if( arguments.length == 1 )
			{
				var func = arguments[1];
				var item = {"selector":selector,"function":func};    
		    	regelements.push(item);
		    	try
	 			{
	 				nodes.each(function() //We need to make sure each row is initially handled
	 				{ 
		 				var onerow = $(this); 
		    			onerow.data("livequeryinit" + selector, true);
		 				func.call(onerow);
		 			});	
		 		} catch ( error ) 
		 		{
		 			console.log("Could not process: " + selector , error); 
		 		}	
		    } 
		    else //Note: on does not support scope of selectors 
		    {
				var eventtype = arguments[0];  //click
				var eventlistener = {"selector":selector,"event":eventtype};
	
				if( arguments.length == 2 )
				{ 
					eventlistener["function"] = arguments[1];
					eventlistener["scope"] = document;
				}
				else
				{
					eventlistener["scope"] = arguments[1];
					eventlistener["function"] = arguments[2];
				}
		    	eventregistry.push(eventlistener);
		    	console.log("Initial Registering  event" + eventlistener.selector );
		    	
 				nodes.each(function() //We need to make sure each row is initially handled
 				{ 
	 				var node = $(this); 
			    	node.data("livequery", true);
	    			node.on(eventlistener.event,eventlistener.function);
			    	//$(document).on(eventlistener.event,eventlistener.selector,eventlistener.function);
	 			});	
		    	
			}
			livequeryrunning = false;
		    return this;
		}
		return runner;
	}
	
}( jQuery ));