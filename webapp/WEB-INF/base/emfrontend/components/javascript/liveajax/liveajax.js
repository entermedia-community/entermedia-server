/*
Known bugs:
If double clicks: Make sure livequeryrunning is correct
If fancybox errors: Make sure get is correct
If list2 not init: Make sure .html is correct and livequeryrunning
*/


(function ($) {
    var oldLoad = $.fn.load;
    $.fn.load = function(inArg,inComplete) 
    {
    	var oldscope = this;
		var returned = oldLoad.call($(this),inArg, function()
		{
			if( inComplete )
			{
				inComplete.call($(this));
			}
			console.log("html complete");
			$(document).trigger("domchanged");
		});
		return returned;
    };

   var oldhtml = $.fn.html;
    $.fn.html = function(arg) 
    {
		var returned = oldhtml.call($(this),arg);
		$(document).trigger("domchanged"); //a component may be adding html that will call this
		return returned;
    };
   
    var oldreplaceWith = $.fn.replaceWith;
    $.fn.replaceWith = function(arg) 
    {
		var returned = oldreplaceWith.call($(this),arg);
		$(document).trigger("domchanged");
		return returned;
    };
    var oldajaxSubmit = $.fn.ajaxSubmit;
    $.fn.ajaxSubmit = function() 
    {
    	var form = $(this);
    	var params = arguments[0];
    	var oldsucess = params.success;
		params.success = function()
		{
			if( oldsucess != null )
			{
				oldsucess.call(form);
			}	
			$(document).trigger("domchanged"); //TODO: Put this in the success section	
		};
		var returned  = oldajaxSubmit.call(form,params);
		params.success = oldsucess;
		return returned;
		
    };
    
    //This is problematic because we call get for various reasons
    var oldget = $.fn.get;
    $.fn.get = function() 
    {
    	if( arguments.length == 3 )
		{
			var oldsucess = arguments[2];
			return oldget.call($(this),arguments[0],arguments[1],function()
			{
				oldsucess.call(this);			
				$(document).trigger("domchanged"); 
			});
		}
		else
		{
			//console.error("Wrong number of params for get ", arguments);
			return oldget.call($(this));
		}
    };
    
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
 			console.log("Skipping reload");
 			return;
 		}
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
 					if( node.data("livequeryinit") == null )
 					{
 				 		node.data("livequeryinit", true);
 				 		//console.log("Not enabled: " + item.selector );
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
 				if( node.data("livequery" + listener.event) == null )
 				{
 				 	node.data("livequery" + listener.event, true);
 				 	//console.log("reRegistering " + listener.selector );
 					node.on(listener.event,listener.function);	
 				}	
 				else
 				{
 					//console.log("already Registered " + listener.selector );
 				}
 			});
 		});
 		
 	});

    $.fn.livequery = function() 
	{
		this.self = this;
    	var node = $(this);
		livequeryrunning  = true;
		if( arguments.length == 1 )
		{
			var func = arguments[0];
			var item = {"selector":this.selector,"function":func};    
	    	regelements.push(item);
	    	try
 			{
	 			func.call($(this));
	    		node.data("livequeryinit", true);
	 		} catch ( error ) 
	 		{
	 			console.log("Could not process: " + item.selector , error); 
	 		}	
	    } 
	    else //Note: on does not support scope of selectors 
	    {
			var eventtype = arguments[0];
			var eventlistener = {"selector":this.selector,"event":eventtype};

			if( arguments.length == 3 )
			{ 
				eventlistener["scope"] = arguments[1];
				eventlistener["function"] = arguments[2];
			}
			else
			{
				eventlistener["function"] = arguments[1];
				eventlistener["scope"] = document;
			}
	    	eventregistry.push(eventlistener);
	    	//console.log("Initial Registering  event" + eventlistener.selector );
	    	node.data("livequery" + eventlistener.event, true);
	    	node.on(eventlistener.event,eventlistener.function);
	    	//$(document).on(eventlistener.event,eventlistener.selector,eventlistener.function);
		}
		livequeryrunning = false;
	    return this;
	}
 	
 
}( jQuery ));