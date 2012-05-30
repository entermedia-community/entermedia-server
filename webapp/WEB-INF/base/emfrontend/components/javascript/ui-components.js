formatHitCountResult = function(inRow)
{
	return inRow[1];
}

uiload = function() {
	
	$('#module-dropdown').click(function(e){
		e.stopPropagation();
		if ( $(this).hasClass('active') ) {
			$(this).removeClass('active');
			$('#module-list').hide();
		} else {
			$(this).addClass('active');
			$('#module-list').show();
		}
	});
	
	$('#maximize').click( function(){
		
		html = $('#maximize').html()
		if ( (html == ' Maximize ') || (html == 'Maximize') ) {
			$('#embody').addClass('max');
			$('#maximize').html('Minimize');
			$('#maximize').attr('title', 'Minimize the application.');
		var w = $('#data').width() - 35;
		$('#asset-data').width(w);
		var w = ( $('#main').width() - 261 );
		$('#right-col .liquid-sizer').width(w)
		
		} else {
			
			$('#embody').removeClass('max');
			$('#maximize').html('Maximize');
			$('#maximize').attr('title', 'Maximize the application.')
			var w = 594;
			$('#asset-data').width('594');
			var w = ( $('#main').width() - 261 );
			$('#right-col .liquid-sizer').width(w)
		}
		
		toggleUserProperty("maximize_screen");
		
	});		
	
	jQuery(".validate-inputs").livequery(
			function() 
			{
					jQuery(this).closest("form").validate();
			}
		);
	
	
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
	
	jQuery(".confirm").livequery('click',
			function(e)
			{
				var inText = jQuery(this).attr("confirm");
				if(confirm(inText) )
				{
					return;
				}
				else
				{	
					e.preventDefault();
				}
			}
		);
	
	jQuery(".uibutton").livequery(
			function()
			{
				jQuery(this).button();
			}
	);
	jQuery(".fader").livequery(
			function()
			{
				jQuery(this).fadeOut(1600, "linear");
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

	jQuery(".ajaxchange select").livequery(
			function()
			{	
				var select = jQuery(this);
				var div = select.parent(".ajaxchange")
				var url = div.attr("targetpath");
				var divid = div.attr("targetdiv");
				
				select.change( function()
					{
					   var url2 = url + $(this).val();						
					   $("#" + divid).load(url2);
					}
				);	
			}
		);

}

resize = function() {
	var w = $('#data').width() - 35;
	$('#asset-data').width(w);
	var w = ( $('#main').width() - 261 );
	$('#right-col .liquid-sizer').width(w);
}

jQuery(document).ready(function() 
{ 
	uiload();
	resize();
	
}); 

$(window).resize(function(){
	resize();
});
