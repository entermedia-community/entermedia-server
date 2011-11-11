var runtimer = true;
filefinished = function(noarg)
{
	//tell all img links to reload
	jQuery('img').each(
		function(index)
		{
			var src = jQuery(this).attr("src");
			jQuery(this).attr("src",src);
		}
	);
}
uploadsComplete = function(noarg)
{
	//set the link to be a check box
	//jQuery('#syncstatus').html("<a class='thickbox' href='$home$apphome/uploadqueue/statusresults.html'><img src='$home${themeprefix}/entermedia/images/new_16x16.png' title='Uploads are completed' /></a>");
	runtimer = false;
	jQuery("#emsyncstatus").html("");
	//tell all img links to reload?
	jQuery('img').each(
		function(index)
		{
			var src = jQuery(this).attr("src");
			jQuery(this).attr("src",src);
		}
	);
	//Does not seem to be working
	//jQuery("#maincontent").load('$home/$usersettings.getLastCatalog().getId()/categories/index.html', {oemaxlevel: 2, cache: false }); //ID wont show if not in that area
}

showstatus = function()
{
	//for each asset on the page reload it's status
	jQuery(".miniassetstatus").each(
			function()
			{
				var cell = jQuery(this);
				var assetid = cell.attr("assetid");
				cell.load("$home$apphome/components/uploadqueue/assetstatus.html?assetid=" + assetid);
			}
	);
	if( runtimer )
	{
		setTimeout("showstatus();",1000);
	}
}

jQuery(document).ready(function() 
{ 
	showstatus();
}); 