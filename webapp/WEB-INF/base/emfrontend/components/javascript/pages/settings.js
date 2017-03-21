
	jQuery(document).ready(function() 
	{ 
	
	      $('#datamanager-workarea th.sortable').livequery("click",function()
	      {
	      		var table = $("#main-results-table");
                var args = {oemaxlevel:1,hitssessionid:table.data("hitssessionid"),origURL:table.data("origURL"),catalogid:table.data("catalogid"),searchtype:table.data("searchtype")};
                var column = $(this);
                var fieldid = column.data("fieldid");
                if ( column.hasClass('currentsort') ) 
                {
                    if ( column.hasClass('up') ) {
						args.sortby=fieldid + 'Down';
                    } else {
                    	args.sortby=fieldid + 'Up';
                    }	         
                } else {
                    $('#datamanager-workarea th.sortable').removeClass('currentsort');
                   column.addClass('currentsort');
                   column.addClass("up");
                   args.sortby=fieldid + 'Up';
                }
                jQuery('#datamanager-workarea').load( '$home$apphome/views/settings/lists/datamanager/list/columnsort.html',args);
        });
	
	
/*		jQuery(".metadatadroppable").livequery(
				function()
				{
				
				
					jQuery(this).draggable({
						start: function(){
							var width = jQuery(this).width();
							var id = jQuery(this).attr('id');
							
					        jQuery(this).hide();
					        jQuery(this).attr('id', 'old-' + id);
					        jQuery('#' + id).width(width);
					        jQuery(this).attr('id', id);
					        
						}
							
					});
					jQuery(this).droppable(
						{
							drop: function(event, ui) 
							{
								var source = ui.draggable.attr("id");
								var destination = this.id;

								var ul = ui.draggable.closest("ul");
								var viewpath = ul.attr("viewpath");
								var seachtype = ul.attr("searchtype");
								var assettype = ul.attr("assettype");
								var viewid = ul.attr("viewid");
								var path = ul.attr("path");
								
								jQuery("#workarea").load(path,
									{
									"source":source,
									"destination":destination,
									"viewpath": viewpath,
									"searchtype": seachtype,
									"assettype": assettype,
									"viewid" : viewid
									});
							},
							tolerance: 'pointer',
							over: outlineSelectionRow,
							out: unoutlineSelectionRow
						}
					);
				}
			);
	*/	

function replaceAll(str, find, replace) {
	find = escapeRegExp(find);
    return str.replace(new RegExp(find, 'g'), replace);
}

function escapeRegExp(str) {
    return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
}
	jQuery('.sortviews').livequery(function()
	{
		var sortable = jQuery(this);
		var path = sortable.data("path");
		
		sortable.sortable({
			axis: 'y',
		    update: function (event, ui) 
		    {
		        var data = sortable.sortable('serialize');
		        data = replaceAll(data,"viewid[]=","|");
		        data = replaceAll(data,"&","");
		        data = data.replace("|","");
		        var args = {};
		        args.items = data;
		        args.viewpath = sortable.data("viewpath");
		        args.searchtype = sortable.data("searchtype");
		        args.assettype = sortable.data("assettype");
		        args.viewid = sortable.data("viewid");
		        jQuery.ajax({
		            data: args,
		            type: 'POST',
		            url: path 		            
		        });
		    },
	        stop: function (event, ui) 
	        {
	            //db id of the item sorted
	            //alert(ui.item.attr('plid'));
	            //db id of the item next to which the dragged item was dropped
	            //alert(ui.item.prev().attr('plid'));
	        }
	     });   
	});
	
	jQuery('.listsort').sortable({
			  
			axis: 'y',
		    stop: function (event, ui) {
		  
				var path = jQuery(this).data("path");
				
		    	
		        var data = jQuery(this).sortable('serialize');
		        
		        // POST to server using $.post or $.ajax
		        jQuery.ajax({
		            data: data,
		            type: 'POST',
		            url: path 		            
		        });
		    }
		});
		
		
		
		
	}); 
