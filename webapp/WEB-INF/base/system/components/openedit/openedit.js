jQuery(document).ready(function() 
{ 
	jQuery("a.openeditdialog").livequery(
		function() 
		{
			var height  = jQuery(window).height();
			var width  = jQuery(window).width();
			height = height * 0.9;
			width = width * 0.9;
			if(width < 900){
				width = 1050;
			}
			
			var newfancy = jQuery(this).fancybox(
			{ 
				'zoomSpeedIn': 300, 'zoomSpeedOut': 300, 'overlayShow': true,
				enableEscapeButton: true, type: 'iframe', 
				frameHeight: height, frameWidth: width
			});
		}
	); 



jQuery(".oethumbholder").livequery(
		function()
		{
			jQuery(this).mouseleave(
				function(){
					var el = document.getElementById("oehover");
					if( el )
					{
						jQuery(el).attr("status","hide"); //Beware this gets called when popup is shown
					}
				});
		
			jQuery(this).mouseenter(
			function()
			{
				var dohover = jQuery(this).parent().parent().parent().attr("hover"); //TODO: use parents("div)
				dohover = "true";
				if(dohover == "true")
				{
					var el = document.getElementById("oehover");
					if(!el)
					{
						el = document.createElement("div");
						document.body.appendChild(el);
						el=jQuery(el);
						el.attr("id","oehover");
						el.attr("status","hide");
						el.bind("mouseleave",function(){
							jQuery(this).attr("status","hide");
							jQuery(this).hide();
						});
					}
					el=jQuery(el);
					
					var img = jQuery(this).find("img.emthumbnail");
					var width = img.width();
					if(width < 100)
					{
						width=100;
					}
					var height = img.height();
					var offset = img.offset();
					height = offset.top -( height/2); //center it
					el.css("top", height + "px");
					width = offset.left - (width / 2);
					el.css("left", width + "px"); 

					var sourcepath = img.attr("sourcepath");
					var assetid = img.attr("assetid");
					var catalogid = img.attr("catalogid");
					var title = img.attr("title");
					var link = jQuery(this).parent().attr("href");
					
					var imgloc = "$home/"+catalogid+"/downloads/preview/medium/" + sourcepath + "/medium.jpg";
					new Image().src = escape(imgloc);
				
					jQuery(el).load('$home/${openeditid}/html/browse/hover.html', {
						assetid: assetid,
						sourcepath: sourcepath,
						catalogid: catalogid,
						title: title,
						href: link
					});
					
					el.attr("status","show"); //Show it is ok. If someone leaves the show is turned off
					el.attr("assetid",assetid); //Show it is ok. If someone leaves the show is turned off
					
					setTimeout('showHover(\"' + assetid +'\")',500)
				}
			});
			
		}
	);


jQuery("form.oeajaxform").livequery('submit',	
		function() 
		{
			var targetdiv = jQuery(this).attr("targetdiv");
			targetdiv = targetdiv.replace(/\//g, "\\/");
			//allows for posting to a div in the parent from a fancybox.
			if(targetdiv.indexOf("parent.") == 0)
			{
				targetdiv = targetdiv.substr(7);
				parent.jQuery(this).ajaxSubmit({target: "#" + targetdiv});
				//closes the fancybox after submitting
				parent.jQuery.fn.fancybox.close();
			}
			else
			{
				jQuery(this).ajaxSubmit( {target:"#" + targetdiv} );
			}
			return false;
		}
	);




showHover = function(inAssetId)
{
	var el = document.getElementById("oehover");
	el = jQuery(el);
	if( el.attr("status") == "show")
	{
		if( inAssetId == el.attr("assetid") )
		{
			el.show();
		}
	}
}

});