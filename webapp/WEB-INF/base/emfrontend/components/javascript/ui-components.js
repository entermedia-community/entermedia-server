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
			doResize();
		} else {
			
			$('#embody').removeClass('max');
			$('#maximize').html('Maximize');
			$('#maximize').attr('title', 'Maximize the application.')
			var w1 = 574;
			$('#asset-data').width(w1);
			var w2 = ( $('#main').width() - 261 );
			$('#right-col .liquid-sizer').width(w2);
			var w3 = ( 551 );
			$('#commenttext').width(w3);
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
	
	
	
	
	jQuery.datepicker.setDefaults(jQuery.extend({
		
		
		showOn: 'button',
		
		buttonImage: '$home$page.themeprefix/entermedia/images/cal.gif',
		buttonImageOnly: true,
		changeMonth: true,
		changeYear: true	

		
	
	}, jQuery.datepicker.regional['$!browserlanguage']));
	
	jQuery("input.datepicker").livequery(
			function() 
			{
				var targetid = jQuery(this).data("targetid");
							
				
				jQuery(this).datepicker( {
					altField: "#"+ targetid,
					altFormat: "mm/dd/yy"
				
				}
				);
				
				var current = jQuery("#" + targetid).val();
				if(current != undefined)
				{
					//alert(current);
					var date;
					if( current.indexOf("-") > 0)
					{
						current = current.substring(2,10);
						//2012-09-17 09:32:28 -0400
						date = jQuery.datepicker.parseDate('yy-mm-dd', current);
					}
					else
					{
						date = jQuery.datepicker.parseDate('mm/dd/yy', current);
					}
					jQuery(this).datepicker("setDate", date );
							
				}
				jQuery(this).blur(function()
						{
							var val = jQuery(this).val();
							if( val == "")
							{
								jQuery("#" + targetid).val("");
							}
						}
				);
			}
		);
	
	//deprecated, use data-confirm
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

	
	

	jQuery(".jp-play").livequery("click", function(){
		
	
	//	alert("Found a player, setting it up");
		var player = jQuery(this).closest(".jp-audio").find(".jp-jplayer");
		var url = player.data("url");
		var containerid = player.data("container");
		var container = jQuery("#" + containerid);
		
		player.jPlayer({
	        ready: function (event) {
	        	player.jPlayer("setMedia", {
	                mp3:url
	            }).jPlayer("play");
	        },
	        play: function() { // To avoid both jPlayers playing together.
	        	player.jPlayer("pauseOthers");
			},
	        swfPath: '$home/emshare/components/javascript',
	        supplied: "mp3",
	        wmode: "window",
	        cssSelectorAncestor: "#" + containerid
	    });
		
		player.jPlayer("play");

	});


	$('#select-dropdown-open').livequery("click",function(){
		
		if ($(this).hasClass('down')) {
			$(this).removeClass('down');
			$(this).addClass('up');
			$('#select-dropdown').show();
		} else {
			$(this).removeClass('up');
			$(this).addClass('down');
			$('#select-dropdown').hide();
		}
	});
	$('#select-dropdown li a').livequery("click",function(){
		$('#select-dropdown-open').removeClass('up');
		$('#select-dropdown-open').addClass('down');
		$('#select-dropdown').hide();
	});

/*
		jQuery('select.choose-select').livequery(
				function()
				{
					if( !jQuery(this).closest(".chzn-filter").length )
					{
						//jQuery(this).chosen({ search_contains: true } );
					}
				}
		);
*/	
	$("select.choose-select-ajax").livequery(
			function()
			{
				var selector = jQuery(this);
				//alert(selector.html());
				var sid = selector.attr("id");

				var listid = selector.data("listid");
				
				var app = jQuery("#application").data("apphome");
						
				selector.ajaxChosen({
					method: 'GET',
					url: app + '/components/xml/types/modulepicker/datasearch.txt?searchtype=' + listid + '&field=name&operation=contains&hitsperpage=400',
					dataType: 'json',
					jsonTermKey: 'name.value'
				}, function(options)
				{
					//extends options with data
					//data: { name: "John", location: "Boston" }
					var args = jQuery.extend({}, options);
					var inputs = jQuery("#" + sid + "_filter .autosubmited");
					inputs.each(function() {
						var name = this.name;
						name = name.substring(12,name.length - 6); //remove the prefix and .value
						args.data[ name + ".value"] = $(this).val();
						args.data["operation" ] = "matches";
						args.data["field" ] = name; //does this work with duplicates?
					 });
					return jQuery.ajax(args);
				}, function (data) {
					var terms = {};
					jQuery.each(data, function () {
						terms[this.id] = this.name;
					});
					return terms;
				});
			}
		);	
	
	
	jQuery('.module-picker-starter .hideshow').livequery("click",function(e)
	{
		  var theinput = $(this).closest(".module-picker-starter");
	      var dropdown = theinput.find('div.chzn-drop').first();
	      var dd_top = theinput.height();
	      var dd_width = theinput.width() - 2;

	      if (theinput.hasClass("chzn-container-active")) {

	    	  theinput.removeClass("chzn-container-active");
		      dropdown.css({
			        "width": dd_width + "px",
			        "top": dd_top + "px",
		            "left": "-9000px"
			      });	  
		      //Do search here and on each keyup and each change of the filter?
		      
	      }
	      else
	      {
	    	  theinput.addClass("chzn-container-active");
		      var a = theinput.find('a').first();
		      a.addClass("chzn-single-with-drop");
		      dropdown.css({
		        "width": dd_width + "px",
		        "top": dd_top + "px",
	            "left": 0
		      });
	      }
	});
	

	
}

function doResize() {
		w1 = ( $('#main').width() - 262 );
		$('#right-col .liquid-sizer').width(w1);
		w2 = ( $('#data').width() - 40 );
		$('#asset-data').width(w2);
		w3 = ( w2 - 23);
		$('#commenttext').width(w3);
	}


jQuery(document).ready(function() 
{ 
	uiload();
	doResize();
}); 

$(window).resize(function(){
	doResize();
});


