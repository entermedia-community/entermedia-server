
	jQuery(document).ready(function() 
	{ 
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
		
	}); 
