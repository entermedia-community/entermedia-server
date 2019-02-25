$(document).ready(function(url,params) 
{ 
	var appdiv = $('#application');
	var home = appdiv.data('home') + appdiv.data('apphome');
	var componenthome = appdiv.data('home') + appdiv.data('componenthome');
	

	var refreshdiv = function(url,params)
	{
		$.get(url, params, function(data) 
		{
			jQuery("#resultsheader").replaceWith(data);
		});	
	}
		
	lQuery("select#selectresultview").livequery( function()
	{
		var select = $(this);
		select.on("change",function() 
		{
			var originalhitsperpage = select.data("hitsperpage");
			if(originalhitsperpage){
				var href = componenthome  +  "/results/changeresultview.html?oemaxlevel=1&cache=false&hitsperpage=" + originalhitsperpage;
			}
			else{
				var href = componenthome  +  "/results/changeresultview.html?oemaxlevel=1";
			}
			var args = { hitssessionid: select.data("hitssessionid") ,
						 searchtype:  select.data("searchtype") ,
						 page:  select.data("page") ,
						 showremoveselections:  select.data("showremoveselections") ,
						  };
						 
			var category = $("#resultsdiv").data("category");
			if( category )
			{
				args.category = category;
			}
			var collectionid = $("#resultsdiv").data("collectionid");
			if( collectionid )
			{
				args.collectionid = collectionid;
			}
			args.resultview = select.val();
						 
			$.get(href, args, function(data) 
			{
				$("#emresultscontainer").html(data);
				$(window).trigger( "resize" );
			});
		});
		
	});
	
	lQuery(".filterschangesort").livequery("click", function(){
		var dropdown = $("#assetsortby");
		var up = dropdown.data("sortup");
		var selected = dropdown.find(":selected");
		var id = selected.data("detailid");
		if(up){
			selected.attr("value", id + "Down");
			$(this).removeClass("fa-sort-alpha-up");
			$(this).addClass("fa-sort-alpha-down");
			
			dropdown.data("sortup", false);
		} else{
			selected.attr("value", id + "Up");
			$(this).removeClass("fa-sort-alpha-down");
			$(this).addClass("fa-sort-alpha-up");
			dropdown.data("sortup", true);
		}
		
		selected.closest("form").submit();
		return false;
	});
	
	
	
		
	lQuery("input.selectionbox").livequery("change", function(e) 
	{
		var dataid = $(this).data('dataid');
		var data = $('#resultsdiv').data();
		
		data['dataid'] = dataid;
		
		refreshdiv( componenthome + "/results/toggle.html", data);
		if(typeof(refreshSelections) != 'undefined'){
			refreshSelections();
		}
		jQuery('.assetproperties').trigger('click');
	});
	
	lQuery("a.selectpage").livequery( 'click', function() 
	{
		jQuery('input[name=pagetoggle]').prop('checked',true);
		jQuery('.selectionbox').prop('checked',true);
		if(typeof(refreshSelections) != 'undefined'){
			refreshSelections();
		}
		
		
	//    $("#select-dropdown-open").click();
	
	});
		//Uses ajax
	lQuery("a.deselectpage").livequery( 'click', function() 
	{
		jQuery('input[name=pagetoggle]').prop('checked',false);
		jQuery('.selectionbox').prop('checked',false); //Not firing the page
	//	$("#select-dropdown-open").click();
		if(typeof(refreshSelections) != 'undefined'){
			refreshSelections();
		}
	
	});
	
	lQuery("input[name=pagetoggle]").livequery( 'click', function() 
	{
		  var home = $('#application').data('home');
		  var apphome = $('#application').data('apphome');
		  var hitssessionid = $('#resultsdiv').data('hitssessionid');
		   var options = $('#resultsdiv').data();
		   options.oemaxlevel = 1;
		   
		   var status = $('input[name=pagetoggle]').is(':checked');
		   if(status)
		   {
			   options.action = "page";
			   refreshdiv( componenthome + "/results/togglepage.html", options);
			   $('.selectionbox').prop('checked', true);
	       }
	       else
	       {
	       	   options.action = "pagenone";
	    	   refreshdiv( componenthome + "/results/togglepage.html", options);  
	   	       $('.selectionbox').prop('checked', false);  
	   	   }
	});
	
	
	lQuery(".gallery-checkbox input").livequery( 'click', function() 
	{
		if ( $(this).is(':checked') ) {
			$(this).closest(".emthumbbox").addClass("selected");
		} else {
			$(this).closest(".emthumbbox").removeClass("selected");
		}
	});
	
	
	lQuery(".moduleselectionbox").livequery("click", function(e) {
		
		
		e.stopPropagation();
		
		  
		var dataid = $(this).data("dataid");
		var sessionid = $(this).data("hitssessionid");
		
		
		$.get(componenthome + "/moduleresults/selections/toggle.html", {dataid:dataid, hitssessionid:sessionid});
		
			
		return;
		
	});

	autosubmitformtriggers = function(form) {
		if ($(form).hasClass("autosubmitform")) {
			$('select',form).on('select2:select', function() 
			{
			    form.trigger("submit");
			});
			
			$('input[type=checkbox]',form).change( function() 
			{
			    form.trigger("submit");
			});
			$('input[type=radio]',form).change( function() 
					{
					    form.trigger("submit");
					});
	
			$('input[type=text]',form).change( function() 
			{
			    form.trigger("submit");
			});
		}
	}
	
	lQuery(".autosubmitform").livequery(function() 
	{
		autosubmitformtriggers($(this));
				
	});
	
	overlayResize = function()
	{
		var img = $("#hiddenoverlay #main-media");
		var avwidth = $(window).width();
		var wheight = $(window).height();
		var overlay = $("#hiddenoverlay");
		overlay.height(wheight);
		overlay.width(avwidth);
		
		$("#hiddenoverlay .playerarea").width(avwidth);
		var w = img.data("width");
		var avheight = $(window).height() - 40;
		if(!isNaN(w) && w != "")
		{
			w = parseInt(w);
			var h = parseInt(img.data("height"));	
			var newh = Math.floor( avwidth * h / w );
			var neww = Math.max(avwidth, Math.floor( avwidth * w / h ));
			img.width(avwidth);
			img.css("height", "auto");
			//Only if limited by height
			

			if( newh > avheight )
			{ 
				img.height(avheight);
				img.css("margin-top","0px");
				//var neww2 = Math.floor( avheight * w / h );
				//img.width(neww2);
				img.css("width", "auto");
				img.css("height", avheight);
			}
			else
			{
				var remaining = avheight - newh;
				
				if ( remaining > 0 )
				{
					remaining = remaining/2;
					img.css("margin-top",remaining + "px");
				}	
				else
				{
					img.css("margin-top","0px");
				}	
			}
			//img.css("height", avheight);
			
		}
		else
		{
			img.width(avwidth);
			//img.css("height", "auto");
			img.css("height", avheight);
			img.css("margin-top","0px");
		}
	}
	$(window).resize(function(){
				overlayResize(); //TODO: Add this to the shared
	});
	
	$.fn.exists = function () {
   		 return this.length !== 0;
	}
	
	getCurrentAssetId = function()
	{
		var mainmedia = $("#main-media-viewer");
		return mainmedia.data("assetid");
	}
	
	function enable(inData,inSpan)
	{
			if( inData == "")
			{
				$(inSpan).addClass("arrowdisabled");
				$(inSpan).data("enabled","false");
				$(inSpan).attr("data-enabled","false");
				
			}
			else
			{
				$(inSpan).addClass("arrowenabled");
				$(inSpan).data("enabled","true");
				$(inSpan).attr("data-enabled","true");
				
			}
	}
	disposevideos = function(){
		//Stop/Dispose Videos
		$('.video-js, .video-player').each(function () {
			if (this.id) {
				videojs(this.id).dispose();
			}
		});
	}
	hideOverlayDiv = function(inOverlay)
	{
		disposevideos();
		stopautoscroll = false;
		$("body").css({ overflow: 'auto' })
		inOverlay.hide();
		var reloadonclose =  $(inOverlay).data('reloadonclose');
		if (reloadonclose == undefined) {
			reloadonclose = true;
		}
		if (reloadonclose) {
			var reloadparent =  window.location.href.split('#')[0];
			if (typeof reloadparent != 'undefined') {
				 window.location = reloadparent;  //TODO: Use Ajax?
			}
		}
		var lastscroll = getOverlay().data("lastscroll");
		$(window).scrollTop( lastscroll );
	}
	
	showOverlayDiv = function(inOverlay)
	{
		stopautoscroll = true;
		 $("body").css({ overflow: 'hidden' })
		inOverlay.show();
		var lastscroll = $(window).scrollTop();
		getOverlay().data("lastscroll",lastscroll);
	}
	
	
	showAsset = function(assetid,pagenum)
	{

		var mainmedia = $("#main-media-viewer");
		var resultsdiv = $("#resultsdiv");
		if( !pagenum )
		{
			pagenum = mainmedia.data("pagenum"); 
			if( !pagenum )
			{
				pagenum = resultsdiv.data("pagenum");
			}
		}
		var hidden = getOverlay();

		//Not needed?
		var link = resultsdiv.data("assettemplate");
		if( link == null )
		{
			 link = componenthome + "/mediaviewer/fullscreen/currentasset.html";	
		}
		var	hitssessionid = resultsdiv.data("hitssessionid");
		var params = {embed:true,assetid:assetid,hitssessionid:hitssessionid,oemaxlevel:1};
		if( pagenum != null )
		{
			params.pagenum = pagenum; //Do we use this for anything?
		}
		params.pageheight =  $(window).height() - 100;

		var collectionid = $("#collectiontoplevel").data("collectionid");
		if(!collectionid )
		{
			collectionid = resultsdiv.data("collectionid");
			if (collectionid) {
				params.collectionid = collectionid;
			}
		}
		
		window.location.hash = 'asset-'+assetid;
		
		disposevideos();
		

		$.get(link, params, function(data) 
		{
			
			showOverlayDiv(hidden);
			
			var container = $("#main-media-container");
			container.replaceWith(data);
			var div = $("#main-media-viewer");
			var id = div.data("previous");
			if (typeof id != 'undefined') {
				enable(id,".goleftclick");
				enable(id,"#leftpage");
			}
			id = div.data("next");
			if (typeof id != 'undefined') {
				enable(id,".gorightclick");
				enable(id,"#rightpage");
			}
		    $(document).trigger("domchanged");
			$(window).trigger( "resize" );
			$(".gallery-thumb").removeClass("active-asset");
			
			if( assetid.indexOf("multiedit:") > -1 )
			{
				var link = $("#main-media-viewer").data("multieeditlink");
				var mainmedia2 = $("#main-media-viewer");
			
				var options = mainmedia2.data();
				mainmedia2.load(link, options, function()
				{
					$(window).trigger("tabready");
				});
			}
			else
			{
				$("#gallery-" + assetid).addClass("active-asset");
				$(window).trigger("tabready");
			}
			
		});
		$(document).trigger("domchanged");
	}
	initKeyBindings = function(hidden)
	{
		//Do we need keyup at all?
		/*
		$(document).keyup(function(e) 
		{
			if( !hidden.is(":visible") )
			{
				return;
			}
			switch(e.which) {
		        case 27: // esc
		        	if ($('#modals').hasClass('show')) {
		        		//Close modal only
		        		$('#modals').modal('hide');
		        		e.stopPropagation();
		        	}
		        	else{
		        		hideOverlayDiv(getOverlay());
		        	}
		        break;
			    
			    default: return; 
		     }
		});*/	
		$(document).keydown(function(e) {
			if( !hidden.is(":visible") )
			{
				return;
			}
			var target  = e.target;
			if ($(target).is('input') || $(target).is('.form-control') ) {
				return;
			}
		    switch(e.which) {
		        case 37: // left
		        	
					var div = $("#main-media-viewer");
		        	var id = div.data("previous");
		        	if( id )
		        	{
			        	showAsset(id);
			        }		        	
		        break;
		
				case 39: // right
					
					var div = $("#main-media-viewer" );
		        	var id = div.data("next");
		        	if( id )
		        	{
			        	showAsset(id);
			        }	
		        break;
		        
		        // TODO: background window.scrollTo the .masonry-grid-cell we view, so we can reload hits
		        
		        case 27: // esc
		        	if ($('#modals').hasClass('show')) {
		        		//Close modal only
		        		$('#modals').modal('hide');
		        		e.stopPropagation();
		        	}
		        	else{
		        		hideOverlayDiv(getOverlay());
		        	}
		        break;
		
		
		        default: return; // exit this handler for other keys
		    }
		    e.preventDefault(); // prevent the default action (scroll / move caret)
		});
	}
	getOverlay = function()
	{
		var hidden = $("#hiddenoverlay");
		if( hidden.length == 0 )
		{
			var grid = $(".masonry-grid");
			var href = grid.data("viewertemplate");
			if( href == null )
			{
				 href = componenthome + "/mediaviewer/fullscreen/index.html";	
			}
			
			$.ajax({ url:href,async: false, data: {oemaxlevel:1}, success: function(data) {
				$('body').prepend(data);
				hidden = $("#hiddenoverlay");
				initKeyBindings(hidden);
			}
			});
		}
		hidden = $("#hiddenoverlay");
		return hidden;
		
	}
	
	lQuery('#jumptoform .jumpto-left').livequery('click',function(e)
	{
		e.preventDefault();
		var input = $("#jumptoform #pagejumper" );
		var current = input.val();
		current = parseInt(current);
		current--;
		if( current > 0 )
		{
			input.val(current);
			$("#jumptoform").submit();
		}
		else
		{
			$('#jumptoform .jumpto-left').addClass("invisible");
		}	

		$('#jumptoform .jumpto-right').removeClass("invisible");
	});


	lQuery('#jumptoform .jumpto-right').livequery('click',function(e)
	{
		e.preventDefault();
		var input = $("#jumptoform #pagejumper" );
		var current = input.val();
		current = parseInt(current);
		current++;
		var totalpages = $("#jumptoform").data("totalpages");
		totalpages = parseInt(totalpages);
		if( current <= totalpages )
		{
			input.val(current);
			$("#jumptoform").submit();
		}	
		if( current >= totalpages )
		{
			$('#jumptoform .jumpto-right').addClass("invisible");
		}	
		$('#jumptoform .jumpto-left').removeClass("invisible");
		
	});
	
	lQuery('div.goleftclick .gocaret').livequery('click',function(e)
	{
		e.preventDefault();
		var div = $("#main-media-viewer" );
		var id = div.data("previous");
		var enabled = $(this).parent().data("enabled");
		if (id && enabled) {
			showAsset(id);
		}

	});
	
	lQuery('div.gorightclick .gocaret').livequery('click',function(e)
	{
		e.preventDefault();
		var div = $("#main-media-viewer" );
		var id = div.data("next");
		var enabled = $(this).parent().data("enabled");
		if (id && enabled) {
			showAsset(id);
		}
	});

	lQuery('.carousel-indicators li#leftpage').livequery('click',function(e)
	{
		e.preventDefault();
		var div = $("#main-media-viewer" );
		var id = div.data("previouspage");
		if( id )
		{
			showAsset(id);
		}
	});
	lQuery('.carousel-indicators li#rightpage').livequery('click',function(e)
	{
		e.preventDefault();
		var div = $("#main-media-viewer" );
		var id = div.data("nextpage");
		if( id )
		{
			showAsset(id);
		}
	});	
	
	
	lQuery("#main-media").livequery("swipeleft",function(){
		
		var div = $("#main-media-viewer" );
		var id = div.data("next");
		if( id ) 
		{
			showAsset(id);
		}	
		});
	lQuery("#main-media").livequery("swiperight",function(){
	
		var div = $("#main-media-viewer" );
		var id = div.data("previous");
		if( id ) 
		{
			showAsset(id);
		}	
		});

	lQuery('a.stackedplayer').livequery('click',function(e)
	{
		e.preventDefault();
		var link = $(this);
		var assetid = link.data("assetid");
		var pagenum = link.data("pagenum"); 
		showAsset(assetid,pagenum);
		return false;
	});
	
	lQuery('.stackedplayertable tr td' ).livequery(
	function()
	{
		$(this).hover(
			function () 
			{
			  	var row = $($(this).closest("tr"));
				var id = $(row).data("rowid");
			    if( id != null )
			    {
				    row.addClass("emborderhover");
				}
		 	}, 
			function () {
			  	var row = $($(this).closest("tr"));
			    row.removeClass("emborderhover");
			}
		);
	});
		
	
	lQuery('table.stackedplayertable td').livequery('click',function(e)
	{
		var clicked = $(this);
		if(clicked.attr("noclick") =="true") {
			return true;
		}
		if( $(e.target).is("input") || $(e.target).is("a"))
		{
			return true;
		}
		
		e.preventDefault();
		e.stopPropagation()
		
		var row = $(clicked.closest("tr"));
		var assetid = row.data("rowid");
		
		showAsset(assetid);
	});
	
	lQuery('.showasset').livequery('click',function(e)
	{
		var clicked = $(this);
		if(clicked.attr("noclick") =="true") {
			return true;
		}
		
		e.preventDefault();
		e.stopPropagation()
		
		var assetid = clicked.data("assetid");
		
		showAsset(assetid);
	});

	lQuery('a#multiedit-menu').livequery('click',function(e)
	{
		e.preventDefault();
		showAsset("multiedit:dialoghitsassetassets/catalog",1);
		return false;
	});
	
	
	lQuery("#hiddenoverlay .overlay-close").livequery('click',function(e)
	{	
		e.preventDefault();
		hideOverlayDiv(getOverlay());
	});
	
	lQuery("#hiddenoverlay .overlay-popup span").livequery('click',function(e)
			{	
				e.preventDefault();
				// editor/viewer/index.html?hitssessionid=${hits.getSessionId()}&assetid=${hit.id}
				var hitssessionid = $('#resultsdiv').data("hitssessionid");
				var href = home + "/views/modules/asset/editor/viewer/index.html?hitssessionid=" + hitssessionid + "&assetid=" + getCurrentAssetId();
				window.location = href;
				
			});
	
	document.addEventListener('touchmove', function(e) 
	{
		//console.log("touchmove event");
		checkScroll();
	});
	
	$(window).on('scroll',function(e) 
	{
		//console.log("scroll event *");
		checkScroll();
	});
	$(document).on('domchanged',function() 
	{
		checkScroll();
	});
	//END Gallery stuff
	
	lQuery('select.addremovecolumns').livequery("change",function()
	{
		var selectedval = $(this).val();
        var resultsdiv = $(this).closest("#resultsdiv");
		var options = resultsdiv.data();
		var searchhome = resultsdiv.data('searchhome');
		$.get(searchhome + "/addremovecolumns.html?oemaxlevel=1&editheader=true&addcolumn=" + selectedval,options, function(data) 
		{	
			resultsdiv.html(data);
		});
	});

	
	lQuery('th.sortable').livequery('click', function(){
            var id = $(this).attr('sortby');
            var resultsarea = "#resultsdiv";
            
            var resultsdiv = $(this).closest("#resultsdiv");
			var searchhome = resultsdiv.data('searchhome');
			var sessionid = resultsdiv.data("hitssessionid");
			var searchtype = resultsdiv.data("searchtype");
            
            if ( $(this).hasClass('currentsort') ) {
                if ( $(this).hasClass('up') ) {
                    $(resultsdiv).load( searchhome + '/columnsort.html?oemaxlevel=1&searchtype=' + searchtype + '&hitssessionid=' + sessionid + '&sortby=' + id + 'Down');
                } else {
                    $(resultsdiv).load( searchhome + '/columnsort.html?oemaxlevel=1&searchtype=' + searchtype + '&hitssessionid=' + sessionid + '&sortby=' + id + 'Up');
                }
            } else {
                $('th.sortable').removeClass('currentsort');
                $(this).addClass('currentsort');
                $(resultsdiv).load( searchhome + '/columnsort.html?oemaxlevel=1&searchtype=' + searchtype + '&hitssessionid=' + sessionid + '&sortby=' + id + 'Down');
            }
        }
    );
	
	$(window).on('resize',function(){
		gridResize();
	});
	
	gridResize();
	window.addEventListener('load', 
	  function() { 
	    	gridResize();
	  }, false);
	
	
	
	var hash = window.location.hash;
	
	if (hash && hash.startsWith('#asset-')){
		var assetid = hash.substring(7,hash.length);
		if (assetid) {
			showAsset(assetid);
		}
	}
	
});        //document ready
        

//TODO: remove this. using ajax Used for modules
togglehits =  function(action)
{
	var data = $('#resultsdiv').data();
	data.oemaxlevel = 1;
	data.action = action;

	$.get(componenthome + "/moduleresults/selections/togglepage.html", data);         
       if(action == 'all' || action== 'page'){
    	   $('.moduleselectionbox').attr('checked','checked');
        }else{
        	$('.moduleselectionbox').removeAttr('checked');  
        }
       return false;       

}
var stopautoscroll = false;

checkScroll = function()
{
		if( stopautoscroll )
		{
			//ignore scrolls
			if( getOverlay().is(":visible") )
			{
				var lastscroll = getOverlay().data("lastscroll");
				var currentscroll = $(window).scrollTop();
				if( Math.abs(lastscroll -  currentscroll) > 50 )
				{
					$(window).scrollTop( lastscroll );
				}
			}
			return;
		}
		
		//No results?
		var resultsdiv= $("#resultsdiv");
		var lastcell = $(".masonry-grid-cell",resultsdiv).last();
		 if( lastcell.length == 0 )
		 {
		 	return;
		 }
		
		
		//are we near the end? Are there more pages?
  		var visibleHeight = $(window).height();
  		var totalHeight = $(document).height();


	    var page = parseInt(resultsdiv.data("pagenum")); 
	    var total = parseInt(resultsdiv.data("totalpages"));
		//console.log("checking scroll " + stopautoscroll + " page " + page + " of " + total);
	    if( page == total)
	    {
			return;
		}

  		//console.log($(window).scrollTop() + " + " +   (visibleHeight*2 + 500) + ">=" + totalHeight); 
		var atbottom = ($(window).scrollTop() + (visibleHeight*2 + 500)) >= totalHeight ; //is the scrolltop plus the visible equal to the total height?
		if(	!atbottom )
	    {
	    	//console.log("Not yet within 500px");
		  return;
		}
		 
		   stopautoscroll = true; 
		   var session = resultsdiv.data("hitssessionid");
		   page = page + 1;
		   resultsdiv.data("pagenum",page);
		   var appdiv = $('#application');
		   var home = $('#application').data('home') + $('#application').data('apphome');
		   var componenthome = appdiv.data('componenthome');

		   console.log("Loading page: #" + page +" - " + home);
		   
		   var link = componenthome + "/results/stackedgallery.html";
	//			   	async: false,

		   $.ajax({
			   	url: link,
			   	xhrFields: {
			      withCredentials: true
			   	},
			   	cache: false,
			   	data: {hitssessionid:session,page:page,oemaxlevel:"1"},
				success: function(data) 
			   	{
				   var jdata = $(data);
				   var code = $(".masonry-grid",jdata).html();
				   $(".masonry-grid",resultsdiv).append(code);
				   //$(resultsdiv).append(code);
				   gridResize();
				   $(document).trigger("domchanged");
				   stopautoscroll = false; 
				   //Once that is all done loading we can see if we need a second page?
			   	   //console.log( page + " Loaded get some more?" + getOverlay().is(':hidden') );
				   if( getOverlay().is(':hidden') )
				   {
				   		checkScroll(); //Might need to load up two pages worth
				   }
				}
			});
}


gridResize = function() 
{
	
	var grid = $(".masonry-grid");
	if( grid.length == 0 )
	{
		//console.log("No grid");
		return;
	}
	
	var fixedheight = grid.data("maxheight");
	if( fixedheight == null || fixedheight.length == 0)
	{
		fixedheight = 200;
	}
	fixedheight = parseInt(fixedheight);
	var cellpadding = grid.data("cellpadding");
	if( cellpadding == null)
	{
		cellpadding = 8;  //this has to be twice what is in results.css
	}
	cellpadding = parseInt(cellpadding);
	
	var totalwidth = 0;
	var rownum = 0;

	var totalavailablew = grid.width();
	
	//Two loops, one to make rows and one to render cells
	var sofarusedw = 0;
	var sofarusedh = 0;
	
	var row = [];
	$(".masonry-grid .masonry-grid-cell").each(function()
	{		
		var cell = $(this);
		cell.css("margin",cellpadding/2 + "px");
		//cell.css("padding",cellpadding);
		var w = cell.data("width");
		var	h = cell.data("height");
		w = parseInt(w);
		h = parseInt(h);
		if( w == 0 )
		{
			w = fixedheight;
			h = fixedheight;
		}
		var a = w / h;  
		cell.data( "aspect",a);
		//console.log("Aspect" + cell.data("aspect"));
		var neww = Math.floor( fixedheight * a );
		var isover = sofarusedw + neww;
		if( isover > totalavailablew )
		{
			//Process previously added cell
			computeRow(row,fixedheight,totalavailablew,sofarusedw,cellpadding);
			row = [];
			sofarusedw = 0;
			rownum = rownum + 1;
		}
		sofarusedw = sofarusedw + neww;// + cellpadding;
		row.push( cell );		
		cell.data( "rownum",rownum);
	});
	$.each( row, function()
			{
				var div = this;
				var a = div.data("aspect");
				div.css("line-height",fixedheight + "px"); 
				div.height(fixedheight);
				$("img.imagethumb",div).height(fixedheight);
				var neww = fixedheight * a - cellpadding;
				div.width(Math.floor(neww - 1));
	});
	
	//console.log("Resized grid");
	checkScroll();
	
}

/**
A = W / H
H = W / A
W = A * H
*/
computeRow = function(row,fixedheight,totalavailablew,sofarusedw,cellpadding)
{
			var growthratiow = (totalavailablew - sofarusedw) / sofarusedw;
			$.each( row, function()
			{
				var div = this;
				var a = div.data("aspect");
				var oldw = a * fixedheight;
				var ratiow = oldw + (oldw * growthratiow); //Here is the magic
				var newheight = Math.floor(ratiow / a); //There cells should all be the same height
				div.css("line-height",newheight + "px"); 
				div.height(newheight);
				$("img.imagethumb",div).height(newheight);
				var neww = newheight * a - cellpadding;
				div.width(Math.floor(neww - 1));  //The 1 is for rounding errors
			});
}
	

	lQuery('div.assetpreview').livequery('click',function(e)
	{
		e.preventDefault();
		$(".bottomtab").removeClass("tabselected");
		$(this).closest(".bottomtab").addClass("tabselected");
		var div = $("#main-media-viewer" );
		var id = div.data("assetid");
		showAsset(id);
		saveProfileProperty("assetopentab","viewpreview",function(){});
	});
	
	lQuery('a.assettab').livequery('click',function(e) {
		e.preventDefault();
		$(".bottomtab").removeClass("tabselected");
		$(".bottomtabactions a").removeClass("dropdown-current");
		$(this).closest(".bottomtab").addClass("tabselected");
		var div = $("#main-media-viewer");
		var options = div.data();
		var assettab = $(this).data("assettab");
		
		if (assettab=='viewpreview') {
			var id = div.data("assetid");
			saveProfileProperty("assetopentab",assettab,function(){});
			showAsset(id);
		}
		else if (assettab=='multiedit') {
			var link = $(this).data("link");
			div.load(link, options, function()
			{
				$(window).trigger("tabready");
			});
		}
		else {
			disposevideos();
			var link = $(this).data("link");
			div.load(link, options, function()
			{
				$(window).trigger("tabready");
			});
			saveProfileProperty("assetopentab",assettab,function(){});
			var assettabactions = $(this).data("assettabactions");
			if (assettabactions) {
				$(this).addClass("dropdown-current");
				var label = $(this).data("assettabname");
				if (label) {
					$('.bottomtabactionstext').text(label);
				}
				saveProfileProperty("assetopentabactions",assettabactions,function(){});
			}
			var assettabtable = $(this).data("assettabtable");
			if (assettabtable) {
				$(this).addClass("dropdown-current");
				var label = $(this).data("assettabname");
				if (label) {
					$('.bottomtabactionstext').text(label);
				}
				saveProfileProperty("assetopentabassettable",assettabtable,function(){});
			}
		}
		
	});
	
	
	
	
	