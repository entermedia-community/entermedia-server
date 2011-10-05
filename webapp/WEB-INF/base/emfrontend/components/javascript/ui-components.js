formatHitCountResult = function(inRow)
{
	return inRow[1];
}

uiload = function() {
	if( jQuery.fn.selectmenu )
	{
		jQuery('.uidropdown select').livequery(
				function()
				{
					jQuery(this).selectmenu({style:'dropdown'});
				}
		);
	}
	
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
	
	jQuery(".uibutton").livequery(
			function()
			{
				jQuery(this).button();
			}
	);
	
	jQuery(".uipanel").livequery(
			function()
			{
				jQuery(this).addClass("ui-widget");
				var header = jQuery(this).attr("header");
				if(header != undefined)
				{
					//http://dev.jquery.it/ticket/9134
					jQuery(this).wrapInner('<div class="ui-widget-content"/>');
					jQuery(this).prepend('<div class="ui-widget-header">' + header + '</div>');					
				}
			}
		);
	
	if( jQuery.fn.tablesorter )
	{
		jQuery("#tablesorter").tablesorter();
	}

}

jQuery(document).ready(function() 
{ 
	uiload();
}); 