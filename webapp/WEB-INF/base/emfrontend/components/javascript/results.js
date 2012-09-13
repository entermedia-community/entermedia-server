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
	  var hitsessionid = $('#resultsdiv').data('hitsessionid');
	   
	   var status = jQuery('input[name=pagetoggle]').is(':checked');
	   if(status)
	   {
		   jQuery(this).load( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitsessionid, action:"page"});
		   jQuery('.selectionbox').attr('checked','checked');
       }
       else
       {
   	       jQuery(this).load( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitsessionid, action:"none"});         
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