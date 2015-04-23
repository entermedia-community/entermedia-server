function selectresultview(val){
	if (val != "none"){
		var href = $("#ajaxselectresultview").attr("href");
		href = href + "&resultview="+val;
		$("#ajaxselectresultview").attr("href",href);
		$("#ajaxselectresultview").click();
	}
}


jQuery(document).ready(function(url,params) 
{ 
	var home = $('#application').data('home') + $('#application').data('apphome');

	var refreshdiv = function(url,params)
	{
		jQuery.get(url, params, function(data) 
		{
			jQuery("#resultheader").replaceWith(data);
		});	
	}
	
	
jQuery("input.selectionbox").livequery( function() 
{
	jQuery(this).change(function() 
	{
		var hitssessionid = $('#resultsdiv').data('hitssessionid');
		var dataid = jQuery(this).data('dataid');
		refreshdiv( home + "/components/results/toggle.html", {dataid:dataid, searchtype: "asset", hitssessionid: hitssessionid });
	});
});

jQuery("a.selectpage").livequery( 'click', function() 
{
	jQuery('input[name=pagetoggle]').attr('checked','checked');
	jQuery('.selectionbox').attr('checked','checked');
//    jQuery("#select-dropdown-open").click();

});
	//Uses ajax
jQuery("a.deselectpage").livequery( 'click', function() 
{
	jQuery('input[name=pagetoggle]').removeAttr('checked');
	jQuery('.selectionbox').removeAttr('checked'); //Not firing the page
//	jQuery("#select-dropdown-open").click();

});

jQuery("input[name=pagetoggle]").livequery( 'click', function() 
{
	  var home = $('#application').data('home');
	  var apphome = $('#application').data('apphome');
	  var hitssessionid = $('#resultsdiv').data('hitssessionid');
	   
	   var status = jQuery('input[name=pagetoggle]').is(':checked');
	   if(status)
	   {
		   refreshdiv( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitssessionid, action:"page"});
		   jQuery('.selectionbox').attr('checked','checked');
       }
       else
       {
    	   refreshdiv( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitssessionid, action:"none"});         
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




jQuery(".moduleselectionbox").livequery("click", function(e) {
	
	
	e.stopPropagation();
	
	var searchhome = $('#resultsdiv').data('searchhome');
	  
	var dataid = jQuery(this).data("dataid");
	var sessionid = jQuery(this).data("hitssessionid");
	
	
	jQuery.get(searchhome + "/selections/toggle.html", {dataid:dataid, hitssessionid:sessionid});
	
		
	return;
	
});

});        //document ready
        

//TODO: remove this. using ajax Used for modules
togglehits =  function(action)
{
	var searchhome = $('#resultsdiv').data('searchhome');
	var sessionid = jQuery('#resultsdiv').data("hitssessionid");

	jQuery.get(searchhome + "/selections/togglepage.html", {oemaxlevel:1, hitssessionid:sessionid, action:action});         
       if(action == 'all' || action== 'page'){
    	   jQuery('.moduleselectionbox').attr('checked','checked');
        }else{
        	jQuery('.moduleselectionbox').removeAttr('checked');  
        }
       return false;       

}


