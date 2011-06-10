outlineSelectionCol = function(event, ui)
{
	jQuery(this).addClass("headerdraggableenabled");
}
	
unoutlineSelectionCol = function(event, ui)
{
	jQuery(this).removeClass("headerdraggableenabled");
}

outlineSelectionRow = function(event, ui)
{
	jQuery(this).addClass("rowdraggableenabled");
}
	
unoutlineSelectionRow = function(event, ui)
{
	jQuery(this).removeClass("rowdraggableenabled");
}


formatHitCountResult = function(inRow)
{
	return inRow[1];
}

formatHitCount = function(inRow)
{jQuery('select#speedC').selectmenu({style:'dropdown'});
	return inRow[0];
}
runajax = function(e)
{
	var nextpage= jQuery(this).attr('href');
	var targetDiv = jQuery(this).attr("targetdiv");
	targetDiv = targetDiv.replace(/\//g, "\\/");
	jQuery("#"+targetDiv).load(nextpage, {}, function()
		{
			// onloadselectors("#"+ targetDiv);
		}
	);

	return false;
}

showHoverMenu = function(inDivId)
{
	el = jQuery("#" + inDivId);
	if( el.attr("status") == "show")
	{
		el.show();
	}
}



toggleitem = function(e)
{
		
		var nextpage= jQuery(this).attr('href');
		var targetDiv = jQuery(this).attr("targetdiv");
		targetDiv = targetDiv.replace(/\//g, "\\/");
		
		jQuery("."+targetDiv).load(nextpage, {}, function()
			{
				var html = jQuery(this).html();
				
				jQuery("."+targetDiv, window.parent.document).html(html);
			    jQuery("#basketmenu").load("$home/$applicationid/components/albums/basket/menuitem.html");
				jQuery("#basketmenu", window.parent.document).load("$home/$applicationid/components/albums/basket/menuitem.html");
				jQuery("#collectionbasket", window.parent.document).load("$home/$applicationid/views/albums/basket/index.html?oemaxlevel=1");
						
				

			}
		);

		return false;

}

toggleorderitem = function(e)
{
		
		var nextpage= jQuery(this).attr('href');
		var targetDiv = jQuery(this).attr("targetdiv");
		targetDiv = targetDiv.replace(/\//g, "\\/");
		
		jQuery("."+targetDiv).load(nextpage, {}, function()
			{
				var html = jQuery(this).html();
				
				jQuery("."+targetDiv, window.parent.document).html(html);
			    jQuery("#basketmenu").load("$home/$applicationid/views/activity/menuitem.html");
				jQuery("#basketmenu", window.parent.document).load("$home/$applicationid/views/activity/menuitem.html");
				// jQuery("#collectionbasket",
				// window.parent.document).load("$home/$applicationid/views/albums/basket/index.html?oemaxlevel=1");
						
				

			}
		);

		return false;

}

getConfirmation = function(inText)
{
	if(!confirm(inText))
	{
		return false;
	}
	return true;
}

clearApplets = function()
{
	// Try to remove all applets before submitting form with jQuery.
	var coll = document.getElementsByTagName("APPLET");
	for (var i = 0; i < coll.length; i++) 
	{
	    var el = coll[i];
	    el.parentNode.removeChild(el);
	} 
}

pageload = function(hash) 
{
	// hash doesn't contain the first # character.
	if(hash) 
	{
		var hasharray = hash.split("|");
		var targetdiv = hasharray[1];
		var location = hasharray[0];
		if(targetdiv != null && location!= null)
		{
			targetdiv = targetdiv.replace(/\//g, "\\/");
			jQuery("#"+targetdiv).load(location);
		}
	} 
}

// Everyone put your onload stuff in here:
onloadselectors = function()
{
	jQuery("a.ajax").livequery('click', runajax);
	
	
	jQuery("form.ajaxform").livequery('submit',	
		function() 
		{
			var targetdiv = jQuery(this).attr("targetdiv");
			targetdiv = targetdiv.replace(/\//g, "\\/");
			// allows for posting to a div in the parent from a fancybox.
			if(targetdiv.indexOf("parent.") == 0)
			{
				targetdiv = targetdiv.substr(7);
				parent.jQuery(this).ajaxSubmit({target: "#" + targetdiv});
				// closes the fancybox after submitting
				parent.jQuery.fn.fancybox.close();
			}
			else
			{
				jQuery(this).ajaxSubmit( {target:"#" + targetdiv} );
			}
			return false;
		}
	);
	
	//used? jQuery(".validateform").validate();
	
	jQuery("a.thickbox, a.emdialog").livequery(
		function() 
		{
			jQuery(this).fancybox(
			{ 
				'zoomSpeedIn': 300, 'zoomSpeedOut': 300, 'overlayShow': true,
				enableEscapeButton: true, type: 'iframe'
			});
		}
	); 
	
	jQuery("a.slideshow").livequery(
		function() 
		{
			jQuery(this).fancybox(
			{ 
				'zoomSpeedIn': 300, 'zoomSpeedOut': 300, 'overlayShow': true , 'slideshowtime': 6000
			});
		}
	);

	jQuery("img.framerotator").livequery(
		function()
		{
			jQuery(this).hover(
				function() {
					jQuery(this).data("frame", 0);
					var path = this.sr$('select#speedC').selectmenu({style:'dropdown'});c.split("?")[0];
					var intval = setInterval("nextFrame('" +  this.id + "', '" + path + "')", 1000);
					jQuery(this).data("intval", intval);
				},
				function() {
					var path = this.src.split("?")[0];
					this.src = path + '?frame=0';
					var intval = jQuery(this).data("intval");
					clearInterval(intval);
				}
			); 
		});
	
	// Live query not needed since Ajax does not normally replease the header
	// part of a page
	var theinput = jQuery("#assetsearchinput");
	if( theinput && theinput.autocomplete )
	{
		theinput.autocomplete('$home$apphome/components/autocomplete/suggestions.txt', {
			selectFirst: false,
			formatItem: formatHitCount,
			formatResult:formatHitCountResult
		});
	}

	// For group manager area
	jQuery("#useremailinput").livequery( function() 
	{
		var theinput = jQuery(this);
		if( theinput && theinput.autocomplete )
		{
			theinput.autocomplete('$home/$applicationid/components/autocomplete/emailsuggestions.txt', {
				selectFirst: false,
				matchCase: true,
				formatResult:formatHitCountResult
			});
		}
	});
	jQuery("#friendemailinput").livequery( function() 
	{
		var theinput = jQuery(this);
		if( theinput && theinput.autocomplete )
		{
			theinput.autocomplete('$home/$applicationid/components/autocomplete/friendemailsuggestions.txt', {
				selectFirst: false,
				matchCase: true,
				formatResult:formatHitCountResult
			});
		}
	});

	jQuery("table.striped tr:nth-child(even)").livequery( function()
		{
			jQuery(this).addClass("odd");
		});
		
	jQuery("div.emtable.striped div.row:nth-child(even)").livequery( function()
		{
			jQuery(this).addClass("odd");
		});

	jQuery('.commentresizer').livequery( function()
	{	
		var ta = jQuery(this).find(".commentinput");
		ta.click(function() 
		{
			var initial = ta.attr("initialtext");
			if( ta.val() == "Write a comment" ||  ta.val() == initial) 
			{
				ta.val('');
				ta.unbind('click');
				var button = jQuery('.commentresizer #commentsubmit');
				button.show();	
			}
		});
		ta.prettyComments();
		// ta.focus();
	});
	if( !window.name || window.name == "")
	{
		window.name = "uploader" + new Date().getTime();	
	}
	
	var appletholder = jQuery('#emsyncstatus');
	if(appletholder.size() > 0)
	{
		appletholder.load('$home/${page.applicationid}/components/uploadqueue/index.html?appletname=' + window.name);
	}
	
	jQuery('.baseemshowonhover' ).livequery( function() 
	{ 
		var image = jQuery(this);
		
		jQuery(this).parent().hover(
				function () 
				{
					image.addClass("baseemshowonhovershow");
			 	}, 
				function () {
			 		image.removeClass("baseemshowonhovershow");
				}
			);	
	});
	
	// Handles emdropdowns
	jQuery("div[id='emdropdown']").livequery(
		function()
		{
			jQuery(this).mouseleave(
				function(){
					var el = document.getElementById("emdropdowndiv");
					if( el )
					{
						jQuery(el).attr("status","hide"); // Beware this gets
															// called when popup
															// is shown
					}
				});
		
			jQuery(this).click(
				function()
				{
					var el = jQuery(this).find(".emdropdowncontent");
					el.bind("mouseleave",function()
					{
						jQuery(this).attr("status","hide");
						jQuery(this).hide();
					});
					//var offset = jQuery(this).offset();
					//var top = offset.top + 20;
					//el.css("top",  top + "px");
					//el.css("left", offset.left+ "px"); 
					
					var path = el.attr("contentpath");
					if( path )
					{
						el.load('$home' + path);
					}
					el.attr("status","show"); //The mouse may jump over a gap so we need delay the show
					el.show();
					var id = el.attr('id');
					setTimeout('showHoverMenu(\"' +  id + '")',300)
			});
			
		}
	);
	if( jQuery.history )
	{
		jQuery.history.init(pageload);
		// set onlick event for buttons
		jQuery("a[class='ajax']").click(function()
		{
			var hash = this.href;
			var targetdiv = this.targetdiv;
			
			hash = hash.replace(/^.*#/, '');
			// moves to a new page.
			// page load is called at once.
			hash=hash+"|"+targetdiv;
			jQuery.history.load(hash);
			return false;  // why is this here
		});
	}	

	// This clears out italics and grey coloring from the search box if it has a
	// user-entered value
	if(jQuery("#assetsearchinput").val() != "Search")
	{
		jQuery("#assetsearchinput").removeClass("defaulttext");
	}

		jQuery('#emselectable table td' ).livequery(	
			function()
			{
				if(jQuery(this).attr("noclick") =="true"){
					return true;
				}
				
				jQuery(this).click(
					function() 
					{
						jQuery('#emselectable table tr' ).each(function(index) 
						{ 
							jQuery(this).removeClass("emhighlight");
						});
						var row = jQuery(this).closest("tr");
						jQuery(row).addClass('emhighlight');
						jQuery(row).removeClass("emborderhover");
						
						var id = jQuery(row).attr("rowid");
						var form = jQuery('#emselectable').find("form");
						if( form.length > 0 )
						{
							jQuery('#emselectable #emselectedrow').val(id);
							jQuery("#emselectable .emneedselection").each( function()
								{
									jQuery(this).removeAttr('disabled');
								});	
							form.submit();
						}
						else
						{
							window.location = id;
						}
					}
				);		
			}
		);

	jQuery('#emselectable table tr' ).livequery(
	function()
	{
		jQuery(this).hover(
			function () 
			{
			  	var row = jQuery(this).closest("tr");
				var id = jQuery(row).attr("rowid");
			    if( id != null )
			    {
				    jQuery(this).addClass("emborderhover");
				}
		 	}, 
			function () {
			    jQuery(this).removeClass("emborderhover");
			}
		);
	});
		
	
	jQuery(".toggleitem").livequery('click', toggleitem);
	
	jQuery(".toggleorderitem").livequery('click', toggleorderitem);

	jQuery(".headerdraggable").livequery( 
			function()
			{	
				jQuery(this).draggable( 
					{ 
						helper: 'clone',
						revert: 'invalid',
						opacity: '.9'
					}
				);
			}
		);
	jQuery(".rowdraggable").livequery( 
			function()
			{	
				jQuery(this).draggable( 
					{ 
						helper: 'clone',
						revert: 'invalid',
						opacity: '.9'
					}
				);
			}
		);
	
	jQuery(".headerdroppable").livequery(
			function()
			{
				jQuery(this).droppable(
					{
						drop: function(event, ui) {
							var source = ui.draggable.attr("id");
							var destination = this.id;
							
							//searchtype=asset&hitssessionid=$hits.getSessionId()&editheader=true
							
							var sessionid = ui.draggable.attr("hitssessionid");
							
							var editing = ui.draggable.attr("editing")
							if( !editing )
							{
								editing = false;
							}
							jQuery("#emresultscontent").load("$home$apphome/components/results/savecolumns.html",
								{
								"source":source,
								"destination":destination,
								editheader:editing,
								searchtype:"asset",
								"hitssessionid":sessionid
								});
							//ui.helper.effect("transfer", { to: jQuery(this).children("a") }, 200);
						},
						tolerance: 'pointer',
						over: outlineSelectionCol,
						out: unoutlineSelectionCol
					}
				);
			}
		);

	jQuery(".metadatadroppable").livequery(
			function()
			{
				jQuery(this).droppable(
					{
						drop: function(event, ui) {
							var source = ui.draggable.attr("id");
							var view = ui.draggable.attr("view");
							var seachtype = ui.draggable.attr("searchtype");
							var assettype = ui.draggable.attr("assettype");
							var destination = this.id;
							jQuery("#metadataeditarea").load("$home$apphome/views/settings/metadata/views/movefields.html",
								{
								"source":source,
								"destination":destination,
								"view": view,
								"searchtype": seachtype,
								"assettype": assettype
								});
						},
						tolerance: 'pointer',
						over: outlineSelectionRow,
						out: unoutlineSelectionRow
					}
				);
			}
		);
	
		jQuery(".autosubmitdetails").livequery(
			function()
			{
				jQuery(this).find(".autosubmited").change(
				  function() 
				  {
					  jQuery(this).parents("form").submit();
				  }
				);
				
			}
		);		
		jQuery(".emfadeout").livequery(
			function()
			{
				jQuery(this).fadeOut(3000, function() 
				 {
					jQuery(this).html("");
				 });
			}
		);	
			
}


jQuery(document).ready(function() 
{ 
	onloadselectors();
}); 
