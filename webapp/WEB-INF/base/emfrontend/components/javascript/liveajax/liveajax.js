(function( $ ) { 
 	var regelements = new Array();
 	$(document).on( "domchanged", function(event,args)
 	{
		var inHtml;
 		if( typeof args == Array )
 		{
 			inHtml = $(args[0],args[1]);
 		}
 		else
 		{
 			inHtml = $(args);
 		}
 		$.each(regelements,function()
 		{
 			var item = this;
 			var funct = item.function;
 			$(item.selector,inHtml).each(function()
 			{
 				funct.call(this);	
 			});
 		});
 	
 	});

    $.fn.livequery = function() 
	{
		if( arguments.length == 1 )
		{
			var item = {"selector":this.selector,"function":arguments[0]};    
	    	regelements.push(item);
	        return this;
	    } 

		$(document).on(arguments[0], this.selector, arguments[1]);
	    return this;
	}
 	
 
}( jQuery ));