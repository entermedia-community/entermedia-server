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
		
	jQuery("select#selectresultview").livequery( function()
	{
		var select = $(this);
		select.on("change",function() 
		{
			var originalhitsperpage = select.data("hitsperpage");
			if(originalhitsperpage){
				var href = home  +  "/components/results/changeresultview.html?oemaxlevel=1&cache=false&hitsperpage=" + originalhitsperpage;
			}
			else{
				var href = home  +  "/components/results/changeresultview.html?oemaxlevel=1";
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
			args.resultview = select.val();
						 
			jQuery.get(href, args, function(data) 
			{
				$("#resultsdiv").html(data);
				$(window).trigger( "resize" );
			});
		});
		
	});
		
	jQuery("input.selectionbox").livequery("change", function(e) 
	{
		var hitssessionid = $('#resultsdiv').data('hitssessionid');
		var dataid = jQuery(this).data('dataid');
		refreshdiv( home + "/components/results/toggle.html", {dataid:dataid, searchtype: "asset", hitssessionid: hitssessionid });
		if(typeof(refreshSelections) != 'undefined'){
			refreshSelections();
		}
	});
	
	jQuery("a.selectpage").livequery( 'click', function() 
	{
		jQuery('input[name=pagetoggle]').prop('checked',true);
		jQuery('.selectionbox').prop('checked',true);
		if(typeof(refreshSelections) != 'undefined'){
			refreshSelections();
		}
		
		
	//    jQuery("#select-dropdown-open").click();
	
	});
		//Uses ajax
	jQuery("a.deselectpage").livequery( 'click', function() 
	{
		jQuery('input[name=pagetoggle]').removeProp('checked');
		jQuery('.selectionbox').removeProp('checked'); //Not firing the page
	//	jQuery("#select-dropdown-open").click();
		if(typeof(refreshSelections) != 'undefined'){
			refreshSelections();
		}
	
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
			   jQuery('.selectionbox').prop('checked',true);
	       }
	       else
	       {
	    	   refreshdiv( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitssessionid, action:"pagenone"});         
	   	       jQuery('.selectionbox').removeProp('checked');  
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
		if(!isNaN(w) && w != "")
		{
			w = parseInt(w);
			var h = parseInt(img.data("height"));	
			var newh = Math.floor( avwidth * h / w );
			var neww = Math.max(avwidth, Math.floor( avwidth * w / h ));
			img.width(avwidth);
			img.css("height", "auto");
			//Only if limited by height
			var avheight = $(window).height();

			if( newh > avheight )
			{ 
				img.height(avheight);
				img.css("margin-top","0px");
				//var neww2 = Math.floor( avheight * w / h );
				//img.width(neww2);
				img.css("width", "auto");
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
			
		}
		else
		{
			img.width(avwidth);
			img.css("height", "auto");
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
				$(inSpan).css("color","#333");
				$(inSpan).css("visibility","hidden");
			}
			else
			{
				$(inSpan).css("color","rgb(200,200,200)");
				$(inSpan).css("visibility","visible");
			}
	}
	
	hideOverlayDiv = function(inOverlay)
	{
		//$("html, body").css({"overflow":"auto","height":"inherit"});
		$("#application").show();
		inOverlay.hide();
	}
	
	showOverlayDiv = function(inOverlay)
	{
		//$("html").css({"overflow":"hidden","height":"100%"});
		//$("body").css({"overflow":"hidden","height":"100%"});
		$("#application").hide();
		inOverlay.show();
	}
	
	showAsset = function(assetid,pagenum)
	{
		var hidden = getOverlay();
		var grid = $(".masonry-grid");
		var link = grid.data("assettemplate");
		if( link == null )
		{
			 link = home + "/components/mediaviewer/fullscreen/currentasset.html";	
		}
		
		var hitssessionid = jQuery('#resultsdiv').data("hitssessionid");
		var params = {playerareawidth: $(window).width() - 200, assetid:assetid,hitssessionid:hitssessionid,oemaxlevel:1};
		if( pagenum != null )
		{
			params.pagenum = pagenum;
		}
		jQuery.get(link, params, function(data) 
		{
			
			showOverlayDiv(hidden);
			
			var container = $("#main-media-container");
			container.replaceWith(data);
			overlayResize();
			var div = $("#main-media-viewer" );
			var id = div.data("previous");
			enable(id,"span.glyphicon-triangle-left");
			id = div.data("next");
			enable(id,"span.glyphicon-triangle-right");
		});
	}
	
	initKeyBindings = function(hidden)
	{
		$(document).keyup(function(e) 
		{
			if( !hidden.is(":visible") )
			{
				return;
			}
			switch(e.which) {
		        case 27: // esc
		       	 hideOverlayDiv(getOverlay());
		        break;
			    
			    default: return; 
		     }
		});	
		$(document).keydown(function(e) {
			if( !hidden.is(":visible") )
			{
				return;
			}
		    switch(e.which) {
		        case 37: // left
					var div = $("#main-media-viewer" );
		        	var id = div.data("previous");
		        	if( id )
		        	{
			        	showOverlayDiv(id);
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
		         	 hideOverlayDiv(getOverlay());
		        break;
		
		       
		        //case : // space //toggle slideshow
		        //break;
		
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
				 href = home + "/components/mediaviewer/fullscreen/index.html";	
			}
			
			jQuery.ajax({ url:href,async: false, data: {oemaxlevel:1}, success: function(data) {
				$('body').prepend(data);
				hidden = $("#hiddenoverlay");
				initKeyBindings(hidden);
			}
			});
		}
		hidden = $("#hiddenoverlay");
		return hidden;
		
	}
	
	jQuery('#jumptoform .jumpto-left').livequery('click',function(e)
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
		jQuery('#jumptoform .jumpto-right').removeClass("disable");
	});


	jQuery('#jumptoform .jumpto-right').livequery('click',function(e)
	{
		e.preventDefault();
		var input = $("#jumptoform #pagejumper" );
		var current = input.val();
		current = parseInt(current);
		current++;
		var totalpages = $("#jumptoform").data("totalpages");
		totalpages = parseInt(totalpages);
		if( current > totalpages )
		{
			$(this).addClass("disable");
		}
		else
		{
			input.val(current);
			$("#jumptoform").submit();
		}	
		
	});
	
	jQuery('div.goleftclick .glyphicon-triangle-left').livequery('click',function(e)
	{
		e.preventDefault();
		var div = $("#main-media-viewer" );
		var id = div.data("previous");
		showAsset(id);

	});
	
	jQuery('div.gorightclick .glyphicon-triangle-right').livequery('click',function(e)
	{
		e.preventDefault();
		var div = $("#main-media-viewer" );
		var id = div.data("next");
		showAsset(id);
	});
	
	$("#main-media").livequery("swipeleft",function(){
		
		var div = $("#main-media-viewer" );
		var id = div.data("previous");
		if( id ) 
		{
			showAsset(id);
		}	
		});
	$("#main-media").livequery("swiperight",function(){
	
		var div = $("#main-media-viewer" );
		var id = div.data("next");
		if( id ) 
		{
			showAsset(id);
		}	
		});
	jQuery('div.goleftclick .glyphicon-triangle-left').livequery('click',function(e)
			{
				e.preventDefault();
				var div = $("#main-media-viewer" );
				var id = div.data("previous");
				showAsset(id);

			});
			
			jQuery('div.gorightclick .glyphicon-triangle-right').livequery('click',function(e)
			{
				e.preventDefault();
				var div = $("#main-media-viewer" );
				var id = div.data("next");
				showAsset(id);
			});
	
	

	jQuery('a.stackedplayer').livequery('click',function(e)
	{
		e.preventDefault();
		var link = $(this);
		var assetid = link.data("assetid");
		var pagenum = link.data("pagenum"); 
		showAsset(assetid,pagenum);
		return false;
	});
	
	
	
	$("#hiddenoverlay .overlay-close").livequery('click',function(e)
	{	
		e.preventDefault();
		hideOverlayDiv(getOverlay());
	});
	
	$("#hiddenoverlay .overlay-popup span").livequery('click',function(e)
			{	
				e.preventDefault();
				// editor/viewer/index.html?hitssessionid=${hits.getSessionId()}&assetid=${hit.id}
				var hitssessionid = jQuery('#resultsdiv').data("hitssessionid");
				var href = home + "/views/modules/asset/editor/viewer/index.html?hitssessionid=" + hitssessionid + "&assetid=" + getCurrentAssetId();
				window.location = href;
				
			});
	
	$(window).on('scroll',function() 
	{
		checkScroll();
	});
	$(document).on('domchanged',function() 
	{
		checkScroll();
	});
	//END Gallery stuff
	
	$(window).on('resize',function(){
		gridResize();
	});
	gridResize();
	setTimeout(gridResize,50);
	
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
var loadingscroll = false;

checkScroll = function()
{
		if( loadingscroll )
		{
			console.log("loading scroll");
			return;
		}
		//are we near the end? Are there more pages?
  		var totalHeight = document.body.offsetHeight;
  		var visibleHeight = document.documentElement.clientHeight;
		//var attop = $(window).scrollTop() < $(window).height(); //past one entire window
		var atbottom = ($(window).scrollTop() + (visibleHeight + 200)) >= totalHeight ; //is the scrolltop plus the visible equal to the total height?
		if(	!atbottom )
	    {
		  return;
		}
		var resultsdiv= $("#resultsdiv");
		var lastcell = $(".masonry-grid-cell",resultsdiv).last();
		 if( lastcell.length == 0 )
		 {
		 	return;
		 }
		 
	    var page = parseInt(resultsdiv.data("pagenum"));   
	    var total = parseInt(resultsdiv.data("totalpages"));
		 console.log("checking scroll" + loadingscroll + " page " + page + " of " + total);
	    if( total > page)
	    {
		   loadingscroll = true; 
		   var session = resultsdiv.data("hitssessionid");
		   page = page + 1;
		   resultsdiv.data("pagenum",page);
		   var home = $('#application').data('home') + $('#application').data('apphome');
		   console.log("loading page: " + home +" " + page);
		   jQuery.get(home + "/components/results/stackedgallery.html", {hitssessionid:session,page:page,oemaxlevel:"1"}, function(data) 
		   {
			   var jdata = $(data);
			   var code = $(".masonry-grid",jdata).html();
			   $(".masonry-grid",resultsdiv).append(code);
			   gridResize();
			   loadingscroll = false; 
			   $(document).trigger("domchanged");
			});
	     }   
}


gridResize = function() 
{
	
	var grid = $(".masonry-grid");
	if( grid.length == 0 )
	{
		return;
	}
	console.log("resized grid");
	checkScroll();
	
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
	