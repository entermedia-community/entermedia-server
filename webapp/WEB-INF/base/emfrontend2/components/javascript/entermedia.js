var ajaxtimerrunning = false;
var app,home,apphome,themeprefix;
var collectionId = '';
var categoryDragged;
var newCollection;
var isCategoryDragged = false;
var isCollectionCreatedByDragging = false;


repaint = function(divid) {
	var div = $("#" + divid);
	var href = div.data('href');
	var args = div.data('args');
	jQuery.get(href + "?" + args, {}, function(data) 
			{
				//var toreplace = $("#" + targetDiv);
				div.replaceWith(data);
			}
	);
}
toggleUserProperty = function(property, onsuccess) {
	app = $("#application");
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
	app = $("#application");
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
	app = $("#application");
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
	app = $("#application");
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
	//$(this).addClass("selected");
	$(this).addClass("dragoverselected");
}
	
unoutlineSelectionCol = function(event, ui)
{
	//$(this).removeClass("selected");
	$(this).removeClass("dragoverselected");
}

outlineSelectionRow = function(event, ui)
{
	$(this).addClass("rowdraggableenabled");  
	$(this).before('<li class="placeholder"></li>');
}
	
unoutlineSelectionRow = function(event, ui)
{
	$(this).removeClass("rowdraggableenabled");
	$('.placeholder').remove();
}

toggleajax = function(e) 
{
	e.preventDefault();
	var nextpage= $(this).attr('href');
	var loaddiv = $(this).attr("targetdivinner");
	var maxlevel = $(this).data("oemaxlevel");
	if( maxlevel )
	{
		if( !nextpage.contains("?") )
		{
			nextpage = nextpage + "?"; 
		}
		nextpage = nextpage + "oemaxlevel=" + maxlevel; 
	}
	
	loaddiv = loaddiv.replace(/\//g, "\\/");
	var cell = $("#" + loaddiv);

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

findclosest = function(link,inid)
{
	var result = link.closest(inid);
	if( result.length == 0 )
	{
		result = link.children(inid);
		if( result.length == 0 )
		{
			result = $(inid);
		}
	}
	return result.first();
}
runajaxonthis = function(inlink,e)
{
	
	$(".ajaxprogress").show();
	var inText = $(inlink).data("confirm");
	if(e && inText && !confirm(inText) )
	{
		e.stopPropagation();
		e.preventDefault();
		return false;
	}
	inlink.attr('disabled','disabled');
	
	if( inlink.hasClass("activelistener") )
	{
		$(".activelistener").removeClass("active");
		inlink.addClass("active");
	}
	var nextpage= inlink.attr('href');
	var targetDiv = inlink.data("targetdiv");
	if( !targetDiv )
	{
		targetDiv = inlink.attr("targetdiv");
	}
	var useparent = inlink.data("useparent");

	var options = inlink.data();
	
	if( targetDiv)
	{
		targetDiv = targetDiv.replace(/\//g, "\\/");
		
		$.get(nextpage, options, function(data) 
			{
				//console.log("Called REAL get on " ,arguments );
				
				var cell;
				if(useparent && useparent == "true")
				{
					cell = $("#" + targetDiv, window.parent.document);
				}
				else
				{
					cell = findclosest(inlink,"#" + targetDiv); 
					
				}
				
				//Call replacer to pull $scope variables
				cell.replaceWith(data); //Cant get a valid dom element
				$(window).trigger( "resize" );
			}
		).always(function()
		{
			$(".ajaxprogress").hide();

			//inlink.css("enabled",true);
			inlink.removeAttr('disabled');
		});
	}	
	else
	{
		//add oemaxlevel as data
		var loaddiv = inlink.data("targetdivinner");
		if( !loaddiv )
		{
			loaddiv = inlink.attr("targetdivinner");
		}
		loaddiv = loaddiv.replace(/\//g, "\\/");
		//$("#"+loaddiv).load(nextpage);
		jQuery.get(nextpage, options, function(data) 
				{
					var cell;
					
					if(useparent && useparent == "true")
					{
						cell = $("#" + loaddiv, window.parent.document);
					}
					else
					{
						cell = findclosest(inlink,"#" + loaddiv);
					}
					cell.html(data);
					$(window).trigger( "resize" );
				}).always(function()
						{
					$(".ajaxprogress").hide();

							//inlink.css("enabled",true);
							inlink.removeAttr('disabled');
						});		
	}
}
runajax = function(e)
{
	 e.stopPropagation();
     e.preventDefault();
	runajaxonthis($(this),e);
	return false;
}

showHoverMenu = function(inDivId)
{
	el = $("#" + inDivId);
	if( el.attr("status") == "show")
	{	
		el.show();
	}
}


updatebasket = function(e)
{
		var nextpage= $(this).attr('href');
		var targetDiv = $(this).attr("targetdiv");
		targetDiv = targetDiv.replace(/\//g, "\\/");
		var action= $(this).data('action');
		$("#"+targetDiv).load(nextpage, function()
			{
			    $("#basket-paint").load(apphome + "/components/basket/menuitem.html");
				if(action == 'remove'){
					$(".selectionbox:checked").closest("tr").hide("slow");
					$(".selectionbox:checked").closest(".emthumbbox").hide("slow");
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
			$("#"+targetdiv).load(location);
		}
	} 
}

// Everyone put your onload stuff in here:
onloadselectors = function()
{
	//autoheight("#emcontent"); 
	
	lQuery("a.ajax").livequery('click', runajax);
	
	lQuery("a.toggleajax").livequery('click', toggleajax);
	
	lQuery("a.updatebasket").livequery('click', updatebasket);
//	$("a.updatebasketonasset").livequery('click', updatebasketonasset);
	
	lQuery("a.propertyset").livequery('click', 
			function(e)
			{
				var propertyname = $(this).attr("propertyname");
				var propertyvalue = $(this).attr("propertyvalue");
				var thelink = $(this);
				app = $("#application");
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
	lQuery(".addmygroupusers").livequery( function() 
			{
				var theinput = $(this);
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
							$(".addmygroupusers").val(ui.item.display);
							//set a hidden input that's actually used when the form is submitted
							$("#hiddenaddmygroupusers").val(ui.item.value);
							var targetdiv = $("#hiddenaddmygroupusers").attr("targetdiv");
							var targeturl = $("#hiddenaddmygroupusers").attr("postpath");
							jQuery.get(targeturl + ui.item.value, 
									function(result) {
										$("#" + targetdiv).html(result);
									}
							);
							return false;
						}
					});
				}
			});
	lQuery(".userautocomplete").livequery( function() 
			{
				var theinput = $(this);
				if( theinput && theinput.autocomplete )
				{
					var theinputhidden = theinput.attr("id") + "hidden";
					theinput.autocomplete({
						source: apphome + '/components/autocomplete/usersuggestions.txt',
						select: function(event, ui) {
							//set input that's just for display purposes
							theinput.val(ui.item.display);
							//set a hidden input that's actually used when the form is submitted
							$("#" + theinputhidden).val(ui.item.value);
							return false;
						}
					});
				}
			});

	lQuery(".googlecontactlist").livequery( function() 
			{
				var theinput = $(this);
				if( theinput  )
				{
					var theinputhidden = theinput.attr("id") + "hidden";
					theinput.autocomplete({
						source: apphome + '/views/settings/google/contactsearch.txt',
						select: function(event, ui) {
							//set input that's just for display purposes
							theinput.val(ui.item.display);
							//set a hidden input that's actually used when the form is submitted
							$("#" + theinputhidden).val(ui.item.value);
							return false;
						}
					});
				}
			});

	
	
	
	
	
	lQuery(".addmygroups").livequery( function() 
	{
		var theinput = $(this);
		if( theinput && theinput.autocomplete )
		{
			var assetid = theinput.attr("assetid");
			theinput.autocomplete({
					source:  apphome + '/components/autocomplete/addmygroups.txt?assetid=' + assetid,
					select: function(event, ui) {
						//set input that's just for display purposes
						$(".addmygroups").val(ui.item.label);
						//set a hidden input that's actually used when the form is submitted
						$("#hiddenaddmygroups").val(ui.item.value);
						var targetdiv = $("#hiddenaddmygroups").attr("targetdiv");
						var targeturl = $("#hiddenaddmygroups").attr("postpath");
						jQuery.get(targeturl + ui.item.value, 
								function(result) {
									$("#" + targetdiv).html(result);
						});
						return false;
					}
			});
		}
	});
	
	
	lQuery("table.striped tr:nth-child(even)").livequery( function()
		{
			$(this).addClass("odd");
		});
		
	lQuery("div.emtable.striped div.row:nth-child(even)").livequery( function()
			{
				$(this).addClass("odd");
			});
	
	lQuery("#tree div:even").livequery( function(){
		$(this).addClass("odd");
	});
	lQuery('.commentresizer').livequery( function()
	{	
		var ta = $(this).find("#commenttext");
		ta.click(function() 
		{
			var initial = ta.attr("initialtext");
			if( ta.val() == "Write a comment" ||  ta.val() == initial) 
			{
				ta.val('');
				ta.unbind('click');
				var button = $('.commentresizer #commentsubmit');
				button.show();	
			}
		});
		//ta.prettyComments();
		 ta.focus();
	});
	

	lQuery(".initialtext").livequery('click', function() 
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
	
/*	var appletholder = $('#emsyncstatus');
//	if(appletholder.size() > 0)
//	{
//		appletholder.load('$home/${page.applicationid}/components/uploadqueue/index.html?appletname=' + window.name);
//	}
*/	
	lQuery('.baseemshowonhover' ).livequery( function() 
	{ 
		var image = $(this);
		
		$(this).parent().hover(
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
	lQuery("div[id='emdropdown']").livequery(
		function()
		{
			$(this).mouseleave(
				function(){
					var el = document.getElementById("emdropdowndiv");
					if( el )
					{
						$(el).attr("status","hide"); // Beware this gets
															// called when popup
															// is shown
					}
				});
		
			$(this).click(
				function()
				{
					var el = $(this).find(".emdropdowncontent");
					el.bind("mouseleave",function()
					{
						$(this).attr("status","hide");
						$(this).hide();
					});
					//var offset = $(this).offset();
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
		$("a[class='ajax']").click(function()
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
	if($("#assetsearchinput").val() != "Search")
	{
		$("#assetsearchinput").removeClass("defaulttext");
	}
	
	lQuery(".headerdraggable").livequery( 
			function()
			{	
				$(this).draggable( 
					{ 
						helper: 'clone',
						revert: 'invalid'
					}
				);
			}
		);
	lQuery(".rowdraggable").livequery( 
			function()
			{	
				$(this).draggable( 
					{ 
						helper: 'clone',
						revert: 'invalid'
					}
				);
			}
		);
	if( jQuery.fn.draggable )
	{
		lQuery(".assetdraggable").livequery( 
			function()
			{	
				$(this).draggable( 
					{ 
						helper: function()
						{
							var cloned = $(this).clone();
							
							//var status = $('input[name=pagetoggle]').is(':checked');
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
				$(this).bind("drag", function(event, ui) {
				    ui.helper.css("background-color", "red");
				    ui.helper.css("border", "2px solid red");
				    ui.helper.append("3");
				});
				*/
			}
		);
		lQuery(".categorydraggable").livequery( 
			function()
			{	
				$(this).draggable( 
					{
						delay: 300,
						helper: function()
						{
							var cloned = $(this).clone();
							
							
							$(cloned).css({"border":"1px solid blue",
										   "background":"#c9e8f2"});

							//var status = $('input[name=pagetoggle]').is(':checked');
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
				$(this).bind("drag", function(event, ui) {
				    ui.helper.css("background-color", "red");
				    ui.helper.css("border", "2px solid red");
				    ui.helper.append("3");
				});
				*/
			}
		);
	}
	lQuery(".headerdroppable").livequery(
			function()
			{
			
				if( !$(this).droppable )
				{
					return;
				}
			
				$(this).droppable(
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
							$("#resultsdiv").load(apphome + "/components/results/savecolumns.html",
								{
								"source":source,
								"destination":destination,
								editheader:editing,
								searchtype:searchtype,
								"hitssessionid":sessionid
								});
							//ui.helper.effect("transfer", { to: $(this).children("a") }, 200);
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
    	lQuery(".assetdropcategory .categorydroparea").livequery(
			function()
			{
				$(this).droppable(
					{
						drop: function(event, ui) {
							var node = $(this);
							var categoryid = node.parent().data("nodeid");
							var targetcategoryid = ui.draggable.data("nodeid")
							
							if( targetcategoryid )
							{
								var tree = node.closest(".emtree");
								var params = tree.data();
								params['categoryid'] = targetcategoryid;//Remove from self
								params['categoryid2'] = categoryid;
								params['oemaxlevel'] = "1";
								params['tree-name'] = tree.data("treename"); 
								
								jQuery.get(apphome + "/components/emtree/movecategory.html", 
										params,
										function(data) 
										{
											tree.closest("#treeholder").replaceWith(data);
										}
								);
							}
							else
							{
								var assetid = ui.draggable.data("assetid");
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
											node.removeClass("dragoverselected");
										}
								);		
							}
						},
						tolerance: 'pointer',
						over: outlineSelectionCol,
						out: unoutlineSelectionCol
					}
				);
			}
		);
		} //droppable
		
		lQuery(".autosubmitdetails").livequery(
			function()
			{
				$(this).find(".autosubmited").change(
				  function() 
				  {
					  $(this).parents("form").submit();
				  }
				);
				
			}
		);		
		lQuery(".emfadeout").livequery(
			function()
			{
				$(this).fadeOut(3000, function() 
				 {
					$(this).html("");
				 });
			}
		);	
		var ranajaxon = new Array();
		
		lQuery(".ajaxstatus").livequery(
			function()
			{
				var uid = $(this).attr("id");
				var isrunning = $(this).data("ajaxrunning");
				var timeout = $(this).data("reloadspeed");
				if( timeout == undefined)
				{
					timeout = 3000;
				}
				var ranonce = ranajaxon[uid];
				if( ranonce == undefined)
				{
					timeout = 500; //Make the first run a quick one
					ranajaxon[uid] = true;
				}
				
				setTimeout('showajaxstatus("' + uid +'");',timeout); //First one is always faster			
			}
		);
		

		
		
		
} //End of selections

autoheight = function(container)
{
    var maxHeight = 0;

    // This will check first level children ONLY as intended.
    $(container+" > *").each(function(){

        height = $(this).outerHeight(true); // outerHeight will add padding and margin to height total
        if (height > maxHeight ) {
            maxHeight = height;
        }
    });

    $(container).height(maxHeight);
}

showajaxstatus = function(uid)
{
	//for each asset on the page reload it's status
	var cell = $("#" + uid);
	if( cell )
	{
		var path = cell.attr("ajaxpath");
		if(!path || path =="")
		{
			path = cell.data("ajaxpath");
		}
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


$(document).ready(function() 
{ 

	jQuery.ajaxSetup({
	    cache: false
	});
	app = $("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	themeprefix = app.data("home") + app.data("themeprefix");	

	$(document).ajaxError(function(e, jqxhr, settings, exception) 
	{
			console.log(e,jqxhr,exception);
			if (exception == 'abort') {
				return;
			}		
				
				var errordiv = $("#errordiv")
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

	if( jQuery.fn.draggable )
	{
	   lQuery(".librarydroparea").livequery(
			function()
			{
				$(this).droppable(
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
										var	cell = $("#" + targetDiv);
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
	
	lQuery("img.assetdragdrop").livequery( function()
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

	lQuery(".librarycollectiondroparea").livequery(
			function()
			{
				$(this).droppable(
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
							var	cell = $("#" + targetDiv);
							cell.replaceWith(data);
						});
					},
					tolerance: 'pointer',
					over: outlineSelectionCol,
					out: unoutlineSelectionCol
				});
			}
		);

	lQuery(".sidetoggle").livequery("click",
			function()
			{
				var div = $(this);
				var target = $(this).data("target");
				toggleUserProperty("minimize" + target,
					function() {
						$("#" + target).slideToggle("fast");
						div.toggleClass("expanded");
						div.toggleClass("minimized");
						div.find(".ui-widget-toggle").toggleClass("fa-angle-left");
					}
				);
			}
	);
	
	lQuery(".newcollectiondroparea").livequery(
	function()
	{
		$(this).droppable(
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
					var cell = $("#opencollectioncreatenewarea");
					cell.html(data);
				});
			},
			tolerance: 'pointer',
			over: outlineSelectionCol,
			out: unoutlineSelectionCol
		});
	});	
}
