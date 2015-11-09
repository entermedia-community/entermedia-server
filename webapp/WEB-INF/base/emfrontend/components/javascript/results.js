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
			var href = home  +  "/components/results/changeresultview.html?oemaxlevel=1";

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
	    	   refreshdiv( home + apphome + "/components/results/togglepage.html", {oemaxlevel:1, hitssessionid: hitssessionid, action:"pagenone"});         
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

	overlayResize = function()
	{
		var img = $("#main-media");
		var w = img.data("width");
		if(!isNaN(w) && w != "")
		{
			w = parseInt(w);
			var h = parseInt(img.data("height"));	
			var newh = Math.floor( $(window).width() * h / w );
			var neww = Math.floor( $(window).width() * w / h );
			var setwidth = true;
			if( newh > $(window).height() ) //limit by height
			{	
				setwidth = false;
			}
			if( setwidth )
			{
				//For 
				img.width($(window).width());
				
				//Only if limited by height
				var remaining = $(window).height() - newh;
				if ( remaining > 0 )
				{
					remaining = remaining/2;
					img.css("margin-top",remaining + "px");
				}	
			}
			else
			{
				img.height($(window).height());
				img.css("margin-top","0px");
			}
		}
		else
		{
			img.height($(window).height());
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
		var mainmedia = $("#main-media");
		return mainmedia.data("assetid");
		
		
	}
	showOverlay = function(assetid)
	{
		var hidden = getOverlay();
		var href = home + "/components/mediaviewer/fullscreen/index.html";
		var hitssessionid = jQuery('#resultsdiv').data("hitssessionid");
		jQuery.get(href, {assetid:assetid,hitssessionid,oemaxlevel:1}, function(data) 
		{
			hidden.html(data);
			overlayResize();
			hidden.show();
		});
	}
	getOverlay = function()
	{
		var hidden = $("#hiddenoverlay");
		if( !hidden.exists() )
		{
			$('body').append('<div id="hiddenoverlay"></div>');
			hidden = $("#hiddenoverlay");
			$(document).keyup(function(e) 
			{
				if( !hidden.is(":visible") )
				{
					return;
				}
				switch(e.which) {
			        case 27: // esc
			       	 getOverlay().hide();
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
			        	//need to find out what asset is previous.
			        	var assetid = getCurrentAssetId();
			        	var div = $("#clickid"  + assetid );
			        	var link = div.parent(".masonry-grid-cell").prev(".masonry-grid-cell");
			        	var id = link.data("assetid");
			        	showOverlay(id);
			        break;
			
					case 39: // right
						var assetid = getCurrentAssetId();
			        	var div = $("#clickid"  + assetid );
			        	var link = div.parent(".masonry-grid-cell").next(".masonry-grid-cell");
			        	var id = link.data("assetid");
			        	showOverlay(id);
			        break;
			        
			        // TODO: background window.scrollTo the .masonry-grid-cell we view, so we can reload hits
			        
			        case 27: // esc
			         getOverlay().hide();
			        break;
			
			       
			        //case : // space //toggle slideshow
			        //break;
			
			        default: return; // exit this handler for other keys
			    }
			    e.preventDefault(); // prevent the default action (scroll / move caret)
			});
		}
		return hidden;
		
	}
	
	jQuery('div.masonry-grid a.playerclink').livequery('click',function(e)
	{
		e.preventDefault();
		var link = $(this);
		/*
		var image = $('img', link);
		if (image.length) {
			console.log(image);
			var percentleft = Math.floor(((e.pageX - link.offset().left) / image.width()) * 100);
			var percenttop = Math.floor(((e.pageY - link.offset().top) / image.height()) * 100);
		
			if (percenttop >= 70) {
				console.log('Click in bottom 30%');
				return;
			}
		}
		*/
		showOverlay(link.parent(".masonry-grid-cell").data("assetid"));
	});
	
	
	
	$("#hiddenoverlay .overlay-close").livequery('click',function(e)
	{	
		e.preventDefault();
		var hidden = $("#hiddenoverlay");
		hidden.hide();
	});
	
	$("#hiddenoverlay .overlay-play").livequery('click',function(e)
	{	
		e.preventDefault();
		var div = $('span', this);
		if (div.hasClass("glyphicon-play")) {
			div.removeClass("glyphicon-play");
			div.addClass("glyphicon-pause");
			console.log("Now Play slideshow");
		} else {
			div.removeClass("glyphicon-pause");
			div.addClass("glyphicon-play");
			console.log("Now Pause slideshow");
		}
	});
	jQuery('div.playerarea').livequery('click',function(e)
	{
		e.preventDefault(); 
		var link = $(this);
		var image = $('img', link);
		var percentleft = Math.floor(((e.pageX - link.offset().left) / image.width()) * 100);
		var percenttop = Math.floor(((e.pageY - link.offset().top) / image.height()) * 100);
	
		if (percentleft >= 70) {
			console.log('Click on right 30%');
		} else if (percentleft <= 30) {
			console.log('Click on left 30%');
		}
	});
	
	$(window).scroll(function() 
	{
		checkScroll();
	});
	//END Gallery stuff
	
//	$(window).load(function() {
//		gridResize();
//	});	
	$(window).on('resize',function(){
		gridResize();
	});
	gridResize();
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
		//console.log("checking scroll" + loadingscroll);
		if( loadingscroll )
		{
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
		var gallery= $("#resultsdiv");
		var lastcell = $(".masonry-grid-cell",gallery).last();
		 
	    var page = parseInt(gallery.data("pagenum"));   
	    var total = parseInt(gallery.data("totalpages"));
	    if( total > page)
	    {
		   loadingscroll = true; 
		   var session = gallery.data("hitssessionid");
		   page = page + 1;
		   gallery.data("pagenum",page);
		   console.log("loading page: " + page);
		   var home = $('#application').data('home') + $('#application').data('apphome');
		   jQuery.get(home + "/components/results/stackedgallery.html", {hitssessionid:session,page:page,oemaxlevel:"1"}, function(data) 
		   {
			   var jdata = $(data);
			   var code = $(".masonry-grid",jdata).html();
			   $(".masonry-grid",gallery).append(code);
			   gridResize();
			   loadingscroll = false; 
			});
	     }   
}


gridResize = function() 
{
	console.log("resized grid");
	checkScroll();
	var fixedheight = 250;
	var cellpadding = 12;
	var sofarused = 0;
	var totalwidth = 0;
	var rownum = 0;

	var totalavailable = $(".masonry-grid").width() - 5;
	
	var row = [];
	$(".masonry-grid .masonry-grid-cell").each(function()
	{		
		var cell = $(this);
		var useimage = false;
		var w = cell.data("width");
		var	h= cell.data("height");
		if( w == 0 )
		{
			w = 100;
			h = 100;
		}
		w = parseInt(w);
		h = parseInt(h);
		var a = w / h;  
	
		var neww = Math.floor( fixedheight * a );
		
		//TODO: Default the height to comething smart
		cell.width(neww); 
		
		var over = sofarused + neww;
		if( over > totalavailable )
		{
			var overage = (totalavailable - row.length * cellpadding)/ sofarused;
			var newheight = fixedheight * overage;

			//Need to figure aspect of entire row
			var roundedheight = Math.floor( newheight ); //make smaller
			if( roundedheight > fixedheight + 50 )
			{
				roundedheight = fixedheight;
			} 
			$.each( row, function()
				{
					var newcell = this;
					var div = newcell.cell;
					var newwidth = Math.floor(newheight * newcell.aspect); 
					//var area = jQuery(".imagearea img",div);
					//area = $(area);
					//area.height(roundedheight); 
					div.height(roundedheight); 
					div.width(newwidth); 
					jQuery.data( div, "rowcount",rownum);
				}	
			);
			row = [];
			sofarused = 0;
			rownum = rownum + 1;
		}
		
		sofarused = sofarused + neww;
		row.push( {cell:$(cell), aspect:a, width:w, height:h} );		
		
	});
	
	//TODO: Move to method call
	var overage = (totalavailable - row.length * cellpadding)/ sofarused;
	var newheight = fixedheight * overage;
	if( newheight > fixedheight + 100) //100 is how wide the next image is going to be
	{
		newheight = fixedheight + 100
	}
	var roundedheight = Math.floor(newheight);
	$.each( row, function()
		{
			var newcell = this;
			var div = newcell.cell;
			var newwidth = Math.floor(newheight * newcell.aspect); 
			//var area = jQuery(".imagearea",div);
			//area = $(area);
			div.height(roundedheight); 
			div.width(newwidth); 
			jQuery.data( div, "rowcount",rownum);
			//jQuery("#emthumbholder img",newcell.cell).width(newwidth);
			//jQuery(".imagearea",newcell.cell).height(roundedheight); //TODO: Fix aspect
			//jQuery(".imagearea",newcell.cell).width(newwidth);
		}	
	);
}
	