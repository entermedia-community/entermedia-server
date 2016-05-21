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
 			chunck = $(args);
 		}
 		//console.log("Init on", chunck);
 		$.each(regelements,function()
 		{
 			var item = this;
 			var funct = item.function;
 			$(item.selector,chunck).each(function()
 			{
 				try
 				{
	 				funct.call($(this));
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
 			$(listener.selector,chunck).each(function()
 			{
 				$(this).on(listener.event,listener.function);	
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
	    	$(this).on(eventlistener.event,eventlistener.function);	
		}
	    return this;
	}
 	
 
}( jQuery ));