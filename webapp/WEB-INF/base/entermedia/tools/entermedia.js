outlineSelectionRow = function(event, ui)
{
	jQuery(this).addClass("rowdraggableenabled");
}
	
unoutlineSelectionRow = function(event, ui)
{
	jQuery(this).removeClass("rowdraggableenabled");
}


disapproveasset = function() {
	if(getConfirmation("This will delete the media.  Are you sure?"))
	{
		var input = jQuery("#approved");
		input.val("false");
		//load the approval form
		var form = jQuery("#approvalform");
		form.submit();
	}
}

approveasset = function() {
	var input = jQuery("#approved");
	input.val("true");
	//load the approval form
	var form = jQuery("#approvalform");
	form.submit();
}

getConfirmation = function(inText)
{
	if(!confirm(inText))
	{
		return false;
	}
	return true;
}

showPostingAttachments = function(inUsername)
{
	//check if need to show selection box
	var thediv = jQuery("#postingshowattachments");
	var selectedid = jQuery("[name=postingattachments]").val(); 
	if(selectedid != "none")
	{
		thediv.css("display", "");
		thediv.load('$home/$applicationid/users/' + inUsername + '/albums/view/index.html?albumid=' + selectedid, {oemaxlevel: 1});
	}
	else
	{
		thediv.css("display", "none");
	}
}

selectAllFriendsPosting = function()
{
	jQuery("input[id*='postingfriendchecked']:not(:checked)").each(
		function() {
			this.checked = !this.checked;
		}
	);
}

selectNoneFriendsPosting = function()
{
	jQuery("input[id*='postingfriendchecked']:checked").each(
		function() {
			this.checked = !this.checked;
		}
	);
}

selectPostingSendTo = function(inFriendCount)
{
	//check if need to show selection box
	var thediv = jQuery("#postingsendtodetail");
	var selection = jQuery("[name=postingsendto]").val(); 
	if(selection == "selectfriends")
	{
		thediv.css("display", "");
		thediv.load('$home/${applicationid}/albums/create/selectfriends.html', {oemaxlevel: 1});
	}
	else if(selection == "email")
	{
		thediv.load('$home/${applicationid}/albums/create/email.html', {oemaxlevel: 1});
		thediv.css("display", "");
	}
	else if(selection == "allfriends")
	{
		thediv.load('$home/${applicationid}/albums/create/allfriends.html', {oemaxlevel: 1, friendcount: inFriendCount});
		thediv.css("display", "");
	}
	else if(selection == "homepage")
	{
		thediv.css("display", "none");
	}
}

toggleCommentNotifications = function(dataid)
{
	var input = jQuery("#idtotoggle");
	input.val(dataid);
	jQuery("#friendform").submit();
}

toggleSection = function(divid, arrowid, theme)
{
	var div = document.getElementById(divid);
	var arrow = document.getElementById(arrowid);
	
	if (div.style.display == "none")
	{
		div.style.display = "block";
		arrow.src = theme + "/entermedia/images/down-off.png";
	}
	else
	{
		div.style.display = "none";
		arrow.src = theme + "/entermedia/images/next-off.png";
	}

	return false;
}	

nextFrame = function(inId, inPath)
{
	var img = jQuery("#" + inId);
	var frame = img.data("frame");
	frame++;
	if (frame > 10)
	{
		frame = 0;
	}
	img.data("frame", frame);
	img.attr("src", inPath+"?frame="+frame);
}


outlineSelection = function(event, ui)
{
	jQuery(this).css("border", "2px solid blue");
}
	
unoutlineSelection = function(event, ui)
{
	jQuery(this).css("border", "");
}

updateSelectionsHeader = function(inCatalogId, inId, inAlbum)
{
	jQuery('#emselectionshead').load('$home/${applicationid}/albums/selection/selections.html', { applicationid: '$applicationid', catalogid: inCatalogId, assetid: inId, albumid: inAlbum});
	return false;
}

showpath = function(inCss, inPath, inMaxLevel)
{
	var targetDiv = "#" + inCss;
	targetDiv = targetDiv.replace(/\//g, "\\/");
	jQuery(targetDiv).load(inPath, {oemaxlevel: inMaxLevel});
}

removeAlbumItem = function(inUserId, inAlbum, inCatalogId, inId)
{
	var targetDiv = "#cell"+inCatalogId+"_"+inId;
	targetDiv = targetDiv.replace(/\//g, "\\/");
		jQuery(targetDiv).load('$home/${applicationid}/users/'  + inUserId + '/albums/edit/actions/remove.html', { catalogid: inCatalogId, assetid: inId, albumid: inAlbum});
	return false;
}

toggleSideMenu = function(inId, toggle, inPath)
{
	jQuery('#' + inId).load('$home' + toggle, {id: inId, pluginpath: inPath, origURL: "$content.path" });
}	

clearApplets = function()
{
	//Try to remove all applets before submitting form with jQuery.
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

//Everyone put your onload stuff in here:
onloadselectors = function()
{
	jQuery("a.ajax").livequery('click', runajax);

	jQuery("form.ajaxform").livequery('submit',	
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
				'zoomSpeedIn': 300, 'zoomSpeedOut': 300, 'overlayShow': true , 'slideshowtime': 3000
			});
		}
	);
	
	
	
	jQuery("img.framerotator").livequery(
		function()
		{
			jQuery(this).hover(
				function() {
					jQuery(this).data("frame", 0);
					var path = this.src.split("?")[0];
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
	
	jQuery("input.datepicker").livequery(
		function() 
		{
			jQuery(this).datepicker(
			{
				dateFormat: 'mm/dd/yy', showOn: 'button',
				buttonImage: '$home$page.themeprefix/entermedia/images/cal.gif',
				buttonImageOnly: true
			});
		}
	);
	//Live query not needed since Ajax does not normally replease the header part of a page
	var theinput = jQuery("#assetsearchinput");
	if( theinput && theinput.autocomplete )
	{
		theinput.autocomplete('$home/$applicationid/tools/autocomplete/suggestions.txt', {
			selectFirst: false,
			formatItem: formatHitCount,
			formatResult:formatHitCountResult
		});
	}

	//Live query not needed since Ajax does not normally reload the header part of a page
	/*
	var theinput = jQuery("#usersearchinput");
	if( theinput && theinput.autocomplete )
	{
		theinput.autocomplete('$home/$applicationid/tools/autocomplete/ldapusersuggestions.txt', {
			selectFirst: false,
			formatResult:formatLdapOutput
		});
	}
	*/
	
	//For group manager area
	jQuery("#addUsernames").livequery( function() 
	{
		var theinput = jQuery(this);
		if( theinput && theinput.autocomplete )
		{
			theinput.autocomplete('$home/$applicationid/tools/autocomplete/usersuggestions.txt', {
				selectFirst: false,
				formatResult:formatHitCountResult
			});
		}
	});

	jQuery(".userautocomplete").livequery( function() 
	{
		var theinput = jQuery(this);
		if( theinput && theinput.autocomplete )
		{
			theinput.autocomplete('$home/$applicationid/tools/autocomplete/usersuggestions.txt', {
				selectFirst: false,
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
	
	if( jQuery.jcarousel )
	{
		jQuery('#autoscroll').jcarousel({
	        auto: 5,
	        wrap: 'last', 
	        visible: 7, 
	        scroll: 1
	    });
	}

	jQuery('textarea.resizer').livequery( function()
	{	
		var ta = jQuery(this);
		ta.click(function() 
		{
			if( ta.val() == "Write a comment" ) //write a comment
			{
				ta.val('');
				ta.unbind('click');
			}
		});
		ta.prettyComments();
		//ta.focus();
	});
	if( !window.name || window.name == "")
	{
		window.name = "uploader" + new Date().getTime();	
	}
	
	var appletholder = jQuery('#emsyncstatus');
	if(appletholder.size() > 0)
	{
		appletholder.load('$home/${page.applicationid}/upload/sync/index.html?appletname=' + window.name);
	}
	
	jQuery("div[id*='draggable']").livequery( 
		function()
		{	
			jQuery(this).draggable( 
				{ 
					helper: 'clone',
					revert: 'invalid',
					opacity: '0.2'
				}
			);
		}
	);
	
	jQuery("div[id*='droppable']").livequery(
		function()
		{
			jQuery(this).droppable(
				{
					drop: function(event, ui) {
						jQuery(this).css("border", "");
						var assetidandcatalogid = ui.draggable.attr("id");
						var ownerandalbumid = this.id;
						var assetid = assetidandcatalogid.split("|")[1];
						
						var catalogid = assetidandcatalogid.split("|")[2];
						var albumid = ownerandalbumid.split("_")[2];
						addItemToSelection(catalogid, assetid, albumid);
						ui.helper.effect("transfer", { to: jQuery(this).children("a") }, 200);
					},
					tolerance: 'pointer',
					over: outlineSelection,
					out: unoutlineSelection
				}
			);
		}
	);
	
	//Handles thumbnail hover popups
	jQuery("div[id*='emthumbholder']").livequery(
		function()
		{
			jQuery(this).mouseleave(
				function(){
					var el = document.getElementById("emhover");
					if( el )
					{
						jQuery(el).attr("status","hide"); //Beware this gets called when popup is shown
					}
				});
		
			jQuery(this).mouseenter(
			function()
			{
				var dohover = jQuery(this).parent().parent().parent().attr("hover"); //TODO: use parents("div)
				if(dohover == "true")
				{
					var el = document.getElementById("emhover");
					if(!el)
					{
						el = document.createElement("div");
						document.body.appendChild(el);
						el=jQuery(el);
						el.attr("id","emhover");
						el.attr("status","hide");
						el.bind("mouseleave",function()
						{
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
					
					jQuery(el).load('$home/' + catalogid + '/results/hover.html', {
						assetid: assetid,
						sourcepath: sourcepath,
						catalogid: catalogid,
						title: title,
						href: link
					});
					
					el.attr("status","show"); //Show it is ok. If someone leaves the show is turned off
					el.attr("assetid",assetid); //Show it is ok. If someone leaves the show is turned off
					
					setTimeout('showHoverImage(\"' + assetid +'\")',500)
				}
			});
			
		}
	);

	
	//Handles thumbnail iconbar menu popup
	jQuery("div[id='emthumboptionsheader']").livequery(
		function()
		{
			jQuery(this).mouseleave(
				function(){
					var el = document.getElementById("emoptionshovermenu");
					if( el )
					{
						jQuery(el).attr("status","hide"); //Beware this gets called when popup is shown
					}
				});
		
			jQuery(this).mouseenter(
			function()
			{
					var el = document.getElementById("emoptionshovermenu");
					if(!el)
					{
						el = document.createElement("div");
						document.body.appendChild(el);
						el=jQuery(el);
						el.attr("id","emoptionshovermenu");
						el.attr("status","hide");
						el.bind("mouseleave",function(){
							jQuery(this).attr("status","hide");
							jQuery(this).hide();
						});
					}
					el=jQuery(el);
					var offset = jQuery(this).offset();
					el.css("top", offset.top + "px");
					el.css("left", offset.left+ "px"); 
					
					var assetid = jQuery(this).attr("assetid");
					var catalogid = jQuery(this).attr("catalogid");
					var type = jQuery(this).attr("type");
					
					jQuery(el).load('$home/' + catalogid + '/results/thumbnails/optionsmenu/' + type + '.html?oemaxlevel=1&assetid=' + assetid +'&catalogid='+ catalogid); 
					
					el.attr("status","show"); //Show it is ok. If someone leaves the show is turned off
					el.attr("assetid",assetid); //Show it is ok. If someone leaves the show is turned off
					
					setTimeout('showHoverMenu(\"' + assetid +'\")',500)
				
			});
			
		}
	);
	if( jQuery.history )
	{
		jQuery.history.init(pageload);
	}
	
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
		return false;  //why is this here
	});

	//This clears out italics and grey coloring from the search box if it has a user-entered value
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
						
						jQuery("#emselectable #emselectedrow").val(id);
						jQuery("#emselectable .emneedselection").each( function()
							{
								jQuery(this).removeAttr('disabled');
							});	
						//jQuery('#emselectable form').trigger('submit');
						var form = jQuery('#emselectable').find("form");
						if( form.length > 0 )
						{
							if( !jQuery('#emselectable form').attr("action") )
							{
								var path = jQuery('#emselectable form #emselectedrow').attr("value");
								jQuery('#emselectable form').attr("action",path);
							}
							jQuery('#emselectable form').submit();
						}
						else if(jQuery('#emselectable #editlink'))
						{
							var link = jQuery('#emselectable #editlink').attr("link");
							var targetdiv = jQuery('#emselectable #editlink').attr("targetdiv");
							targetdiv = targetdiv.replace(/\//g, "\\/");
							var id = jQuery(row).attr("rowid");
							link = link + "&id=" + id;
							jQuery("#" + targetdiv).load(link);
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
	
	jQuery('div.embutton' ).livequery( function() 
	{ 
		//add a bunch of top things
		var existinghtml = jQuery(this).html();

		var finalhtml ='<b class="spiffytwo">\n<b class="spiffytwo1"><b></b></b>\n<b class="spiffytwo2"><b></b></b><b class="spiffytwo3"></b>' + 
		  '<b class="spiffytwo4"></b><b class="spiffytwo5"></b></b><div class="spiffyfgfour">' +
		  existinghtml +
		  '</div><b class="spiffytwo">\n<b class="spiffytwo5"></b><b class="spiffytwo4"></b><b class="spiffytwo3"></b><b class="spiffytwo2"><b></b></b><b class="spiffytwo1"><b></b></b></b>';
		
		finalhtml = '<div class="embuttoninside">' + finalhtml + '</div>';
		jQuery(this).html(finalhtml);
	});
	
	//makes it look right on ie7, because don't honor css min-width
	jQuery("div#catalogSettingsPanel").livequery( function ()
	{
		var width = jQuery(this).width();
		if(!(width > 780)) {
			jQuery(this).width(780);
		}
	});
	
	
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
							jQuery("#metadataeditarea").load("$home$cataloghome/settings/metadata/views/movefields.html",
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
	
}

formatLdapOutput = function(inRow)
{
	return inRow[1] + "\n";
}

formatHitCountResult = function(inRow)
{
	return inRow[1];
}

formatHitCount = function(inRow)
{
	return inRow[0];
}
runajax = function(e)
{
	var nextpage= jQuery(this).attr('href');
	var targetDiv = jQuery(this).attr("targetdiv");
	targetDiv = targetDiv.replace(/\//g, "\\/");
	jQuery("#"+targetDiv).load(nextpage, {}, function()
		{
			//onloadselectors("#"+ targetDiv);
		}
	);

	return false;
}
showHoverMenu = function(inAssetId)
{
	var el = document.getElementById("emoptionshovermenu");
	el = jQuery(el);
	if( el.attr("status") == "show")
	{
		if( inAssetId == el.attr("assetid") )
		{
			el.show();
		}
	}
}

showHoverImage = function(inAssetId)
{
	var el = document.getElementById("emhover");
	el = jQuery(el);
	if( el.attr("status") == "show")
	{
		if( inAssetId == el.attr("assetid") )
		{
			el.show();
		}
	}
}
jQuery(document).ready(function() 
{ 
	var el = document.getElementById("emerrorarea");
	if(!el)
	{
		el = document.createElement("div");
		document.body.appendChild(el);
	    el=jQuery(el);
	}
  	jQuery(el).ajaxError(
  		function(event, request, settings)
  		{
  			if( settings.url )
  			{
	   			jQuery(this).append("<li>Error on page " + settings.url + "</li>");
	   		}
	   		else
	   		{
	   			jQuery(this).append("<li>Server returned an error</li>");
	   		}
   		}
   	);
 
	onloadselectors();

}); 
