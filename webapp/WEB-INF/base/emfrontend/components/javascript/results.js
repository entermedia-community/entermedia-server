function selectresultview(val){
	if (val != "none"){
		var href = $("#ajaxselectresultview").attr("href");
		
		href = href + "&resultview="+val;
		var category = $("#resultsdiv").data("category");
		if( category )
		{
			href = href + "&category="+category;
		}
		$("#ajaxselectresultview").attr("href",href);
		$("#ajaxselectresultview").click();
	}
}


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

	$(window).load(function() {
		gridResize();
	});	
	$(window).resize(function(){
		gridResize();
	});

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





gridResize = function() 
{
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
		//var w = cell.data("width");
		var useimage = false;
		var w = jQuery("#emthumbholder img",cell).width();
		if(w == 0 || w == null) //not loaded yet
		{
			useimage = true;
			w = cell.data("width");
			if( isNaN(w) || w == "" )
			{
				w = 80;
			}
		}
		
		if( useimage )
		{
			h= cell.data("height");
			if(isNaN(h)  || h == "")
			{
				h = 80;
			}			
		}
		else
		{
			h = jQuery("#emthumbholder img",cell).height();
		}
		if( w == 0 )
		{
			w = 100;
		}
		w = parseInt(w);
		h = parseInt(h);
		var a = w / h;  
	
		var neww = Math.floor( fixedheight * a );
		
		var over = sofarused + neww;
		if( over > totalavailable )
		{
			var overage = (totalavailable - row.length * cellpadding)/ sofarused;
			var newheight = fixedheight * overage;

			//Need to figure aspect of entire row
			var roundedheight = Math.floor( newheight ); //make smaller
			$.each( row, function()
				{
					var newcell = this;
					var newwidth = Math.floor(newheight * newcell.aspect); 
					
					var img = jQuery("#emthumbholder img",newcell.cell);
					img.width(newwidth);
					jQuery(".imagearea",newcell.cell).height(roundedheight); //TODO: Fix aspect
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
			var newwidth = Math.floor(newheight * newcell.aspect); 
			jQuery("#emthumbholder img",newcell.cell).width(newwidth);
			jQuery(".imagearea",newcell.cell).height(roundedheight); //TODO: Fix aspect
			jQuery(".imagearea",newcell.cell).width(newwidth);
		}	
	);
}
	