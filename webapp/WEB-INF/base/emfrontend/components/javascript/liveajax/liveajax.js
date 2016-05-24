
(function ($) {
    var oldLoad = $.fn.load;
    $.fn.load = function(inArg,inComplete) 
    {
    	var oldscope = this;
		oldLoad.call($(this),inArg, function()
		{
			if( inComplete )
			{
				inComplete.call($(this));
			}
			$(document).trigger("domchanged");
		});
    };
/*
   var oldhtml = $.fn.html;
    $.fn.html = function() 
    {
		oldhtml.call($(this));
		$(document).trigger("domchanged");
    };
    
    var oldreplaceWith = $.fn.replaceWith;
    $.fn.replaceWith = function() 
    {
		oldreplaceWith.call($(this));
		$(document).trigger("domchanged");
    };
    
    var oldajaxSubmit = $.fn.ajaxSubmit;
    $.fn.ajaxSubmit = function() 
    {
		oldajaxSubmit.call($(this));
		$(document).trigger("domchanged"); //TODO: Put this in the success section
    };
    
    var oldget = $.fn.get;
    $.fn.get = function() 
    {
    	if( arguments.length == 3 )
		{
			var oldsucess = arguments[2];
			oldget.call($(this),arguments[0],arguments[1],function()
			{
				oldsucess.call(this);			
				$(document).trigger("domchanged"); 
			});
		}
		else
		{
			oldget.call($(this)); //not sure what this does
		}
    };
 */   
    
})(jQuery);



(function( $ ) { 
 	var regelements = new Array();
 	var eventregistry = new Array();
 	
 	//Listener
 	
 	$(document).on( "domchanged", function(event,args)
 	{
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
 					if( node.data("lqenabled") == null )
 					{
 				 		node.data("lqenabled", true);
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
 				if( node.data("lqenabled") == null )
 				{
 				 	node.data("lqenabled", true);
 				 	console.log("reRegistering " + listener.selector );
 					node.on(listener.event,listener.function);	
 				}	
 			});
 		});
 		
 	});

    $.fn.livequery = function() 
	{
		if( arguments.length == 1 )
		{
			var func = arguments[0];
			var item = {"selector":this.selector,"function":func};    
	    	regelements.push(item);
	    	try
 			{
	 			func.call($(this));
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
	    	console.log("Registering " + eventlistener.selector );
	    	$(this).data("lqenabled", true);
	    	$(this).on(eventlistener.event,eventlistener.function);	
		}
	    return this;
	}
 	
 
}( jQuery ));