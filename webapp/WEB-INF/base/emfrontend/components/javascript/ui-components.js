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
	
	if( jQuery.fn.button )
	{	
		jQuery(".uibutton").livequery(
				function()
				{
					jQuery(this).button();
				}
		);
	}
	
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
	
	jQuery(".addmygroupusers").livequery( function() 
			{
				var theinput = jQuery(this);
				if( theinput && theinput.autocomplete )
				{
					theinput.autocomplete({
						source: '$apphome/components/autocomplete/addmygroupusers.txt',
						select: function(event, ui) {
							//set input that's just for display purposes
							jQuery(".addmygroupusers").val(ui.item.display);
							//set a hidden input that's actually used when the form is submitted
							jQuery("#hiddenaddmygroupusers").val(ui.item.value);
							return false;
						}
					});
				}
			});

	jQuery(".addmygroups").livequery( function() 
	{
		var theinput = jQuery(this);
		if( theinput && theinput.autocomplete )
		{
			theinput.autocomplete({
					source:  '$apphome/components/autocomplete/addmygroups.txt',
					select: function(event, ui) {
						//set input that's just for display purposes
						jQuery(".addmygroups").val(ui.item.label);
						//set a hidden input that's actually used when the form is submitted
						jQuery("#hiddenaddmygroups").val(ui.item.value);
						return false;
					}
			});
		}
	});
}

jQuery(document).ready(function() 
{ 
	uiload();
}); 