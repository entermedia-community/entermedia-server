var ajaxtimerrunning = false;
var app,home,apphome,themeprefix;
var collectionId = '';
var categoryDragged;
var newCollection;
var isCategoryDragged = false;
var isCollectionCreatedByDragging = false;


repaint = function(divid) {
	var div = jQuery("#" + divid);
	var href = div.data('href');
	var args = div.data('args');
	jQuery.get(href + "?" + args, {}, function(data) 
			{
				//var toreplace = jQuery("#" + targetDiv);
				div.replaceWith(data);
			}
	);
}
toggleUserProperty = function(property, onsuccess) {
	app = jQuery("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	
	jQuery.ajax(
			{
				url:  apphome + "/components/userprofile/toggleprofileproperty.html?field=" + property,
				success: onsuccess
			}
		);
	
}

saveProfileProperty = function(property, value,onsuccess) {
	app = jQuery("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	
	jQuery.ajax(
			{
				url:  apphome + "/components/userprofile/saveprofileproperty.html?field=" + property + "&" + property + ".value=" + value,
				success: onsuccess
			}
		);
	
}



setSessionValue = function(key, value) {
	app = jQuery("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	
	jQuery.ajax(
			{
				url: apphome + "/components/session/setvalue.html?key=" + key + "&value=" + value 
			}
		);
	
}

getSessionValue = function(key) {
	var returnval = null;
	app = jQuery("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	
	
	jQuery.ajax(
			{
				url: apphome + "/components/session/getvalue.html?key=" + key,
				async: false,
				success: function(data){
					
					returnval = data;
					
				}
			
				
			}
		);
	
	return returnval;
}



outlineSelectionCol = function(event, ui)
{
	jQuery(this).addClass("selected");
	jQuery(this).addClass("dragoverselected");
}
	
unoutlineSelectionCol = function(event, ui)
{
	jQuery(this).removeClass("selected");
	jQuery(this).removeClass("dragoverselected");
}

outlineSelectionRow = function(event, ui)
{
	jQuery(this).addClass("rowdraggableenabled");  
	$(this).before('<li class="placeholder"></li>');
}
	
unoutlineSelectionRow = function(event, ui)
{
	jQuery(this).removeClass("rowdraggableenabled");
	$('.placeholder').remove();
}

toggleajax = function(e) 
{
	e.preventDefault();
	var nextpage= jQuery(this).attr('href');
	var loaddiv = jQuery(this).attr("targetdivinner");
	var maxlevel = jQuery(this).data("oemaxlevel");
	if( maxlevel )
	{
		if( !nextpage.contains("?") )
		{
			nextpage = nextpage + "?"; 
		}
		nextpage = nextpage + "oemaxlevel=" + maxlevel; 
	}
	
	loaddiv = loaddiv.replace(/\//g, "\\/");
	var cell = jQuery("#" + loaddiv);

	if ( cell.hasClass("toggle_on") || cell.hasClass("toggle_off") ) 
	{
		var off = cell.hasClass("toggle_off");
		if (off) 
		{
			cell.removeClass("toggle_off");
			cell.addClass("toggle_on");
			cell.show('fast');
		}
		else
		{	
			cell.removeClass("toggle_on");
			cell.addClass("toggle_off");
			cell.hide('fast');
		}
	}
	else
	{
		jQuery.get(nextpage, {}, function(data) 
			{
				cell.addClass("toggle_on");
				cell.show('fast');
			}
		);
	}
}
runajaxonthis = function(inlink,e)
{
	
	var inText = jQuery(inlink).data("confirm");
	if(e && inText && !confirm(inText) )
	{
		e.stopPropagation();
		e.preventDefault();
		return false;
	}
	inlink.attr('disabled','disabled');
	
	if( inlink.hasClass("activelistener") )
	{
		jQuery(".activelistener").removeClass("active");
		inlink.addClass("active");
	}
	var nextpage= inlink.attr('href');
	var targetDiv = inlink.attr("targetdiv");
	
	var useparent = inlink.data("useparent");
	
	if( targetDiv)
	{
		targetDiv = targetDiv.replace(/\//g, "\\/");
		
		jQuery.get(nextpage, {}, function(data) 
			{
				//console.log("Called REAL get on " ,arguments );
				
				var cell;
				if(useparent && useparent == "true")
				{
					cell = jQuery("#" + targetDiv, window.parent.document);
				}
				else
				{
					cell = jQuery("#" + targetDiv);
				}
				
				//Call replacer to pull $scope variables
				cell.replaceWith(data); //Cant get a valid dom element
				$(window).trigger( "resize" );
			}
		).always(function()
		{
			//inlink.css("enabled",true);
			inlink.removeAttr('disabled');
		});
	}	
	else
	{
		var loaddiv = inlink.attr("targetdivinner");
		loaddiv = loaddiv.replace(/\//g, "\\/");
		//jQuery("#"+loaddiv).load(nextpage);
		jQuery.get(nextpage, {}, function(data) 
				{
					var cell;
					
					if(useparent && useparent == "true")
					{
						cell = jQuery("#" + loaddiv, window.parent.document);
					}
					else
					{
						cell = jQuery("#" + loaddiv);
					}
					cell.html(data);
					$(window).trigger( "resize" );
				}).always(function()
						{
							//inlink.css("enabled",true);
							inlink.removeAttr('disabled');
						});		
	}
}
runajax = function(e)
{
	runajaxonthis($(this),e);
	 e.stopPropagation();
     e.preventDefault();
	//return false;
}

showHoverMenu = function(inDivId)
{
	el = jQuery("#" + inDivId);
	if( el.attr("status") == "show")
	{	
		el.show();
	}
}


updatebasket = function(e)
{
		var nextpage= jQuery(this).attr('href');
		var targetDiv = jQuery(this).attr("targetdiv");
		targetDiv = targetDiv.replace(/\//g, "\\/");
		var action= jQuery(this).data('action');
		jQuery("#"+targetDiv).load(nextpage, function()
			{
			    jQuery("#basket-paint").load(apphome + "/components/basket/menuitem.html");
				if(action == 'remove'){
					jQuery(".selectionbox:checked").closest("tr").hide("slow");
					jQuery(".selectionbox:checked").closest(".emthumbbox").hide("slow");
				}
			}
		);
		 e.preventDefault();
		return false;
}


//Is this being used?
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
	
	jQuery("a.toggleajax").livequery('click', toggleajax);
	
	jQuery("a.updatebasket").livequery('click', updatebasket);
//	jQuery("a.updatebasketonasset").livequery('click', updatebasketonasset);
	
	jQuery("a.propertyset").livequery('click', 
			function(e)
			{
				var propertyname = jQuery(this).attr("propertyname");
				var propertyvalue = jQuery(this).attr("propertyvalue");
				var thelink = $(this);
				app = jQuery("#application");
				home =  app.data("home");
				apphome = home + app.data("apphome");
				
				jQuery.ajax(
					{
						url: apphome + "/components/userprofile/saveprofileproperty.html?field=" + propertyname + "&" + propertyname + ".value="  + propertyvalue,
						success: function()
						{
							runajaxonthis(thelink,e);
						}
					}
				);
			     e.preventDefault();
			});
	
	
	//move this to the settings.js or someplace similar 
	jQuery(".addmygroupusers").livequery( function() 
			{
				var theinput = jQuery(this);
				if( theinput && theinput.autocomplete )
				{
					var assetid = theinput.attr("assetid");
					/*theinput.autocomplete({
					    source: ["c++", "java", "php", "coldfusion", "javascript", "asp", "ruby"]
					});*/
					theinput.autocomplete({
						source: apphome + '/components/autocomplete/addmygroupusers.txt?assetid=' + assetid,
						select: function(event, ui) {
							//set input that's just for display purposes
							jQuery(".addmygroupusers").val(ui.item.display);
							//set a hidden input that's actually used when the form is submitted
							jQuery("#hiddenaddmygroupusers").val(ui.item.value);
							var targetdiv = jQuery("#hiddenaddmygroupusers").attr("targetdiv");
							var targeturl = jQuery("#hiddenaddmygroupusers").attr("postpath");
							jQuery.get(targeturl + ui.item.value, 
									function(result) {
										jQuery("#" + targetdiv).html(result);
									}
							);
							return false;
						}
					});
				}
			});
	jQuery(".userautocomplete").livequery( function() 
			{
				var theinput = jQuery(this);
				if( theinput && theinput.autocomplete )
				{
					var theinputhidden = theinput.attr("id") + "hidden";
					theinput.autocomplete({
						source: apphome + '/components/autocomplete/usersuggestions.txt',
						select: function(event, ui) {
							//set input that's just for display purposes
							theinput.val(ui.item.display);
							//set a hidden input that's actually used when the form is submitted
							jQuery("#" + theinputhidden).val(ui.item.value);
							return false;
						}
					});
				}
			});

	
	
	
	jQuery(".addmygroups").livequery( function() 
	{
		var theinput = jQuery(this);
		if( theinput && theinput.autocomplete )
		{
			var assetid = theinput.attr("assetid");
			theinput.autocomplete({
					source:  apphome + '/components/autocomplete/addmygroups.txt?assetid=' + assetid,
					select: function(event, ui) {
						//set input that's just for display purposes
						jQuery(".addmygroups").val(ui.item.label);
						//set a hidden input that's actually used when the form is submitted
						jQuery("#hiddenaddmygroups").val(ui.item.value);
						var targetdiv = jQuery("#hiddenaddmygroups").attr("targetdiv");
						var targeturl = jQuery("#hiddenaddmygroups").attr("postpath");
						jQuery.get(targeturl + ui.item.value, 
								function(result) {
									jQuery("#" + targetdiv).html(result);
						});
						return false;
					}
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
	
	jQuery("#tree div:even").livequery( function(){
		jQuery(this).addClass("odd");
	});
	jQuery('.commentresizer').livequery( function()
	{	
		var ta = jQuery(this).find("#commenttext");
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
		//ta.prettyComments();
		 ta.focus();
	});
	

	jQuery(".initialtext").livequery('click', function() 
	{
		var ta = $(this);
		 
		var initial = ta.data("initialtext");
		if( !initial )
		{
			initial = ta.attr("initialtext");
		}
		if( ta.val() == "Write a comment" ||  ta.val() == initial) 
		{
			ta.val('');
			ta.removeClass("initialtext");
			ta.unbind('click');
		}
	});

	
	if( !window.name || window.name == "")
	{
		window.name = "uploader" + new Date().getTime();	
	}
	
/*	var appletholder = jQuery('#emsyncstatus');
//	if(appletholder.size() > 0)
//	{
//		appletholder.load('$home/${page.applicationid}/components/uploadqueue/index.html?appletname=' + window.name);
//	}
*/	
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
						el.load( home + path);
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
	
	jQuery(".headerdraggable").livequery( 
			function()
			{	
				jQuery(this).draggable( 
					{ 
						helper: 'clone',
						revert: 'invalid'
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
						revert: 'invalid'
					}
				);
			}
		);
	jQuery(".assetdraggable").livequery( 
			function()
			{	
				jQuery(this).draggable( 
					{ 
						helper: function()
						{
							var cloned = $(this).clone();
							
							//var status = jQuery('input[name=pagetoggle]').is(':checked');
							 var n = $("input.selectionbox:checked").length;
							 if( n > 1 )
							 {
									cloned.append('<div class="dragcount">+' + n + '</div>');
								 
							 }
							
							return cloned;
						}
						,
						revert: 'invalid'
					}
				);
				/*
				jQuery(this).bind("drag", function(event, ui) {
				    ui.helper.css("background-color", "red");
				    ui.helper.css("border", "2px solid red");
				    ui.helper.append("3");
				});
				*/
			}
		);
	
	if( jQuery.fn.draggable )
	{
		jQuery(".categorydraggable").livequery( 
			function()
			{	
				jQuery(this).draggable( 
					{
						delay: 300,
						helper: function()
						{
							var cloned = $(this).clone();
							
							
							$(cloned).css({"border":"1px solid blue",
										   "background":"#c9e8f2"});

							//var status = jQuery('input[name=pagetoggle]').is(':checked');
							 var n = $("input.selectionbox:checked").length;
							 if( n > 1 )
							 {
									cloned.append('<div class="dragcount">+' + n + '</div>');
								 
							 }
							
							return cloned;
						}
						,
						revert: 'invalid'
					}
				);
				/*
				jQuery(this).bind("drag", function(event, ui) {
				    ui.helper.css("background-color", "red");
				    ui.helper.css("border", "2px solid red");
				    ui.helper.append("3");
				});
				*/
			}
		);
	}
	jQuery(".headerdroppable").livequery(
			function()
			{
				jQuery(this).droppable(
					{
						drop: function(event, ui) {
							var source = ui.draggable.attr("id");
							var node = $(this);
							var destination = this.id;
							
							var rdiv = $("#resultsdiv");
							var searchtype = rdiv.data("searchtype");
							var sessionid = rdiv.data("hitssessionid");
							
							var editing = ui.draggable.attr("editing")
							if( !editing )
							{
								editing = false;
							}
							jQuery("#resultsdiv").load(apphome + "/components/results/savecolumns.html",
								{
								"source":source,
								"destination":destination,
								editheader:editing,
								searchtype:searchtype,
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

	if( jQuery.fn.droppable )
	{
    	jQuery(".assetdropcategory .categorydroparea").livequery(
			function()
			{
				jQuery(this).droppable(
					{
						drop: function(event, ui) {
							var assetid = ui.draggable.data("assetid");
							var node = $(this);
							var categoryid = node.parent().data("nodeid");
							
							var hitssessionid = $("#resultsdiv").data("hitssessionid");
							if( !hitssessionid )
							{
								hitssessionid = $("#main-results-table").data("hitssessionid");
							}
							//this is a category
							var moveit = false;
							if( node.closest(".assetdropcategorymove").length > 0 )
							{
								moveit = true;
							}
							var rootcategory = node.closest(".emtree").data("rootnodeid");
								
							jQuery.get(apphome + "/components/categorize/addassetcategory.html", 
									{
										assetid:assetid,
										categoryid:categoryid,
										hitssessionid:hitssessionid,
										moveasset: moveit,
										rootcategoryid: rootcategory
									},
									function(data) 
									{
										node.append("<span class='fader'>&nbsp;+" + data + "</span>");
										node.find(".fader").fadeOut(3000);
										node.removeClass("selected");
									}
							);

						},
						tolerance: 'pointer',
						over: outlineSelectionCol,
						out: unoutlineSelectionCol
					}
				);
			}
		);
		} //droppable
		
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
		jQuery(".ajaxstatus").livequery(
			function()
			{
				var uid = $(this).attr("id");
				var isrunning = $(this).data("ajaxrunning");
				var timeout = 3000;
				if( isrunning == undefined)
				{
					timeout = 500; //First one is always faster
				}
				setTimeout('showajaxstatus("' + uid +'");',timeout); //First one is always faster			
			}
		);
		
} //End of selections

showajaxstatus = function(uid)
{
	//for each asset on the page reload it's status
	var cell = jQuery("#" + uid);
	if( cell )
	{
		var path = cell.attr("ajaxpath");
		//console.log("Loading " + path );
		if( path && path.length > 1)
		{
			jQuery.get(path, {}, function(data) 
			{
				cell.replaceWith(data); //jQuery will reinit this class
			});
		}	
	}
}



jQuery(document).ready(function() 
{ 

	jQuery.ajaxSetup({
	    cache: false
	});
	app = jQuery("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	themeprefix = app.data("home") + app.data("themeprefix");	

	$(document).ajaxError(function(e, jqxhr, settings, exception) 
	{
			console.log(e,jqxhr,exception);
			if (exception == 'abort') {
				return;
			}		
				
				var errordiv = jQuery("#errordiv")
				if( errordiv.length > 0)
				{
					
					function fade(elem){
						$(elem).delay(6000).fadeOut(5000, "linear");
					}
					
					$('#errordiv').stop(true, true).show().css('opacity', 1);
					
					errors = '<p class="error"><strong>Error: </strong>' + settings.url  + '<br/><strong> Returned: </strong>' + '' + exception + '</p>'
					
					$('#errordiv').html(errors);
					
					fade($('#errordiv'));
					
					$('#errordiv').mouseover(function(){
						$(this).stop(true, true).show().css('opacity', 1);
					});
					
					$('#errordiv p').mouseout(function(){
						fade($('#errordiv'));
					});
				}
				else
				{
					//  alert("Error \n" + settings.url + " \nreturned " + exception);

				}
			});

	onloadselectors();
	emcomponents();
	
	
}); 

emcomponents = function() {
	$("#savedquerylist a").click(function(e)
			{
				e.preventDefault();
				var a = jQuery(this);
				var link = a.attr("href");
				
				jQuery.get(link, {}, function(data) 
						{
							var toreplace = jQuery("#searcheditor");
							toreplace.html(data);
							
							var tmp = jQuery("#savedquerylist #newterm");
							tmp.remove();
							var top = a.position().top;
							top = top + a.height() + 40;
							jQuery("#eml-green-dialog").css("top",top);
							
							jQuery("#arrow").show();
							var padleft = a.position().left;
							padleft = padleft + a.width() / 2;
							padleft = padleft  - 42; //arrow width
							jQuery("#arrow").css("left",padleft);

						}
				);
				return false;
			}
	);
	
	$("#addterm").click(function(e)
		{
			e.preventDefault();
			var a = jQuery(this);
			var link = a.attr("href");
			
			jQuery.get(link, {}, function(data) 
					{
						var toreplace = jQuery("#searcheditor");
						toreplace.html(data);
						
						jQuery("#savedquerylist span").append('<span id="newterm">new term</span>');
						var a = jQuery("#savedquerylist #newterm");
						var top = a.position().top;
						top = top + a.height() + 40;
						jQuery("#eml-green-dialog").css("top",top);
						
						jQuery("#arrow").show();
						var padleft = a.position().left;
						padleft = padleft + a.width() / 2;
						padleft = padleft  - 42; //arrow width
						jQuery("#arrow").css("left",padleft);
					}
			);
			return false;
		}
	);

	if( jQuery.fn.draggable )
	{
	   jQuery(".librarydroparea").livequery(
			function()
			{
				jQuery(this).droppable(
					{
						drop: function(event, ui) {
							
							var assetid = ui.draggable.data("assetid");
							var node = $(this);
							
							node.removeClass("selected");
							node.removeClass("dragoverselected");
							
							node.css('background-image','url("' + themeprefix + '/images/icons/loader.gif")');
							var targetDiv = node.data("targetdiv");
							var libraryid = node.data("libraryid");
							var hitssessionid = $("#resultsdiv").data("hitssessionid");
							if( !hitssessionid )
							{
								hitssessionid = $("#main-results-table").data("hitssessionid");
							}
							
							jQuery.get(apphome + "/components/libraries/addasset.html", 
									{
										assetid:assetid,
										libraryid:libraryid,
										hitssessionid: hitssessionid
									},
									function(data) 
									{
										var	cell = jQuery("#" + targetDiv);
										cell.replaceWith(data);	
									}
							);

						},
						tolerance: 'pointer',
						over: outlineSelectionCol,
						out: unoutlineSelectionCol
					}
				);
			}
		);
	} //droppable
	
	jQuery("img.assetdragdrop").livequery( function()
	{
		var img = $(this);
			
		var httplink = location.protocol + '//' + location.host;			
		var filename = img.data('name');
        var urls =  httplink + apphome + "/views/modules/asset/downloads/originals/" + img.data('sourcepath') + "/" + filename;
        
        var handler = function(event) 
	    {
           if( event.dataTransfer.getData("application/x-moz-file-promise-url") && navigator.appVersion.indexOf("Win") != -1 )
           {
     	 		event.dataTransfer.setData('application/x-moz-file-promise-url',urls );
             	event.dataTransfer.setData('application/x-moz-file-promise-dest-filename',filename);
             	event.dataTransfer.effectAllowed = 'all';       
           }
           else
           {
           		event.dataTransfer.clearData();
	            var download = "application/force-download:" + filename + ":" + urls;
	            event.dataTransfer.setData("DownloadURL", download);   
	        	event.dataTransfer.effectAllowed = 'copy';
	        }
	        
            event.dataTransfer.setData('text/uri-list',urls);
            event.dataTransfer.setData('text/plain',urls);

	        return true;
	    };
        
        //THIS IS NOT QUITE WORKING
       // this.addEventListener('dragstart', handler, false ); 
       // this.parentNode.addEventListener('dragstart', handler, false ); //Deal with A tags?
        
	});

	jQuery(".librarycollectiondroparea").livequery(
			function()
			{
				jQuery(this).droppable(
				{
					drop: function(event, ui) {
						
						/*
						 * Current droppable element 
						 */
						var anode = $(this);
						var collectionid = anode.data("collectionid");
						var targetDiv = anode.data("targetdiv");
						var dropsave = anode.data("dropsaveurl");
						var hitssessionid = $("#resultsdiv").data("hitssessionid");
						var collectionName = anode.find("a.librarylabel").data("collectionname");
						
						var params = {collectionid:collectionid};
						
						//console.log("Drop" + ui.draggable);
						/*
						 * Current draggable element
						 */
						var assetid = ui.draggable.data("assetid");
						var categoryid = ui.draggable.data("nodeid");
						var categoryName = ui.draggable.data("categoryname");
						params.categoryid = categoryid;
						params.categoryName = categoryName;

						var response;
						/*
						if(!categoryName){
							response = true;//confirm("Move asset to "+collectionName+" collection?");
						}else{
							response = confirm("Copy "+categoryName+" category to "+collectionName+" collection?");
						}
						if(response == false){
							return false;
						}
						*/
						
						if( !hitssessionid )
						{
							hitssessionid = $("#main-results-table").data("hitssessionid");
						}
						
						var nextpage= dropsave;
						if( assetid )
						{
							 params.assetid= assetid;
						}
						params.hitssessionid=hitssessionid;
						jQuery.get(nextpage, params, function(data) 
						{
							var	cell = jQuery("#" + targetDiv);
							cell.replaceWith(data);
						});
					},
					tolerance: 'pointer',
					over: outlineSelectionCol,
					out: unoutlineSelectionCol
				});
			}
		);

	jQuery(".sidetoggle").livequery("click",
			function()
			{
				var div = $(this);
				var target = jQuery(this).data("target");
				toggleUserProperty("minimize" + target,
					function() {
						jQuery("#" + target).slideToggle("fast");
						div.toggleClass("expanded");
					}
				);
			}
	);
	
	jQuery(".newcollectiondroparea").livequery(
	function()
	{
		jQuery(this).droppable(
		{
			drop: function(event, ui) 
			{
				var categortyid = ui.draggable.data("nodeid");
				var dropsaveurl;
				var params = {};
				if(typeof categortyid != "undefined" )
				{ 
					 var categoryName = ui.draggable.data("categoryname");
					 dropsaveurl = apphome + "/components/opencollections/dropcategory.html";
					 params.categoryid = categortyid;
					 params.categoryname = categoryName;
				}
				else
				{
					var assetid = ui.draggable.data("assetid");
					dropsaveurl = apphome + "/components/opencollections/addnewchild.html?assetid=" + assetid;
					params.assetid = assetid;
					var hitssessionid = $("#resultsdiv").data("hitssessionid");
					params.hitssessionid = hitssessionid;
					
				}
				jQuery.get(dropsaveurl, params, function(data) 
				{
					var cell = jQuery("#opencollectioncreatenewarea");
					cell.html(data);
				});
			},
			tolerance: 'pointer',
			over: outlineSelectionCol,
			out: unoutlineSelectionCol
		});
	});	
}
