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
	
	jQuery("input.uicalendar").livequery(
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
			jQuery(this).wrapInner('<div class="ui-widget-content"/>');
			if(header !== undefined)
			{
				jQuery(this).prepend('<div class="ui-widget-header">' + header + '</div>');
				
			}
			
		}
	);
}

jQuery(document).ready(function() 
{ 
	uiload();
}); 