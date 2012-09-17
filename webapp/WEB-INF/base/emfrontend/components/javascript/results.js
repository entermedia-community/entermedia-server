jQuery("input.selectionbox").livequery( function() 
{
	jQuery(this).change(function() 
	{
		var home = $('#application').data('home') + $('#application').data('apphome');
		var hitssessionid = $('#resultsdiv').data('hitssessionid');
		var count = jQuery(this).data('count');
		
		jQuery(this).load( home + "/components/results/toggle.html", {count:count, searchtype: "asset", hitssessionid: hitssessionid });
			//jQuery(this).load( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitsessionid });
	});
});

jQuery("a.selectpage").livequery( 'click', function() 
{
	jQuery('input[name=pagetoggle]').attr('checked','checked');
	jQuery('.selectionbox').attr('checked','checked');
   // jQuery("#select-dropdown-open").click();

});
	
jQuery("a.deselectpage").livequery( 'click', function() 
{
	jQuery('input[name=pagetoggle]').removeAttr('checked');
	jQuery('.selectionbox').removeAttr('checked');
	//jQuery("#select-dropdown-open").click();

});

jQuery("input[name=pagetoggle]").livequery( 'click', function() 
{
	  var home = $('#application').data('home');
	  var apphome = $('#application').data('apphome');
	  var hitssessionid = $('#resultsdiv').data('hitssessionid');
	   
	   var status = jQuery('input[name=pagetoggle]').is(':checked');
	   if(status)
	   {
		   jQuery(this).load( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitssessionid, action:"page"});
		   jQuery('.selectionbox').attr('checked','checked');
       }
       else
       {
   	       jQuery(this).load( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitssessionid, action:"none"});         
   	       jQuery('.selectionbox').removeAttr('checked');  
   	   }
	   //jQuery("#select-dropdown-open").click();
});


jQuery(".gallery-checkbox input").livequery( 'click', function() 
{
	if ( jQuery(this).is(':checked') ) {
		jQuery(this).closest(".emthumbbox").addClass("selected");
	} else {
		jQuery(this).closest(".emthumbbox").removeClass("selected");
	}
});



/**
jQuery(".selectionbox").livequery("click", function(e) {
	
	

	e.stopPropagation();
	
	
	
	var count = jQuery(this).data("count");
	var sessionid = jQuery(this).data("sessionid");
	
	

	jQuery.get("$home${content.searchhome}/selections/toggle.html", {count:count, sessionid:"$hits.getSessionId()"});
	
	
		
	return;
	
});




togglehits =  function(action)
{
    
	jQuery.get("$home${content.searchhome}/selections/togglepage.html", {oemaxlevel:1, hitssessionid:"$hits.getSessionId()", action:action});         
       if(action == 'all' || action== 'page'){
    	   jQuery('.selectionbox').attr('checked','checked');
        }else{
        	jQuery('.selectionbox').removeAttr('checked');  
        }
       return false;       

}



        
        hide_spinner = function(){$j('#spinner').css('visibility', 'hidden');};
        show_spinner = function(){$j('#spinner').css('visibility','visible');};

        jQuery('.navlink').livequery('click', function(e){  
            show_spinner(); 
            
           nextpage = $j(this).attr('href');
         
           jQuery('#resultsarea').load(nextpage, {oemaxlevel:1}, hide_spinner);       
            return false;      
        });
        
        
*/