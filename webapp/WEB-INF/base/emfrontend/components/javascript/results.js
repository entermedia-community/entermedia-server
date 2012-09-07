toggleHit =  function(div, count, sessionid)
{
	home = $('#application').data('home');
	apphome = $('#application').data('apphome');
	hitsessionid = $('#resultsdiv').data('hitsessionid');
	
	jQuery(this).load( home + apphome + "/components/results/toggle.html", {count:count, sessionid: hitsessionid });
	jQuery(this).load( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitsessionid });
    return false;
}

jQuery("a.selectpage").click(function () 
{
	jQuery('input[name=pagetoggle]').attr('checked','checked');
	 jQuery('.selectionbox').attr('checked','checked');
});
jQuery("a.deselectpage").click(function () 
{
	jQuery('input[name=pagetoggle]').removeAttr('checked');
	jQuery('.selectionbox').removeAttr('checked');  
});

jQuery("input[name=pagetoggle]").click(function () 
		{
		   home = $('#application').data('home');
		   apphome = $('#application').data('apphome');
		   hitsessionid = $('#resultsdiv').data('hitsessionid');
		   
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
       });
jQuery('.gallery-checkbox input').click(function(){
	this = jQuery(this);
	if ( this.is('checked') ) {
		this.closest('.emthumbbox').addClass('selected');
	} else {
		this.closest('.emthumbbox').removeClass('selected');
	}
})