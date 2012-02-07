
	jQuery(document).ready(function() 
	{ 
		jQuery(".metadatadroppable").livequery(
				function()
				{
					jQuery(this).droppable(
						{
							drop: function(event, ui) {
								var source = ui.draggable.attr("id");
								var viewpath = ui.draggable.attr("viewpath");
								var seachtype = ui.draggable.attr("searchtype");
								var assettype = ui.draggable.attr("assettype");
								var viewid = ui.draggable.attr("viewid");
								var destination = this.id;
								jQuery("#metadataeditarea").load("$home$apphome/views/settings/metadata/views/movefields.html",
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
		
	}); 
