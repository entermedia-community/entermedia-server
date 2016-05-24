formatHitCountResult = function(inRow)
{
	return inRow[1];
}

uiload = function() {

	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var themeprefix = app.data("home") + app.data("themeprefix");
	
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
	jQuery("select.select2").livequery( function() 
	{
		var input = jQuery(this);
		input.select2();
	});
	
	jQuery("input.select2editable").livequery( function() 
	{
	 	var input = jQuery(this);
		var arr = new Array(); //[{id: 0, text: 'story'},{id: 1, text: 'bug'},{id: 2, text: 'task'}]
		
		var ulid = input.data("optionsul");
		
		var options = jQuery("#" + ulid + " li");
		
		if( !options.length )
		{
			return;
		}		
		
		options.each(function() 
		{
			var id = $(this).data('value');
			var text = $(this).text();
			//console.log(id + " " + text);
		 	arr.push({id: id, text: text}); 
		});

		
		//Be aware: calling select2 forces livequery to filter again
	 	input.select2({
				createSearchChoice: function(term, data) 
				{ 
					if ($(data).filter(function() { return this.text.localeCompare(term)===0; } ).length===0) 
					{
						console.log("picking" + term );
						return {id:term, text:term};
					}
				 }
				 , multiple: false
				 , data: arr
		});
	});	
	
	jQuery(".validate-inputs").livequery(
			function() 
			{
//				jQuery(".required",this).each(function()
//				{
//					//jQuery(this).attr("required","true");
//				});
				
				var theform = jQuery(this).closest("form");
				
				theform.on("click", function()
				{
					theform.valid();
				});
				
				theform.validate({
					  ignore: ".ignore"
				});
							
			}
		);
		jQuery("select.ajax").livequery('change',
			function(e) 
			{
				var inlink = jQuery(this);
				var nextpage= inlink.data('href');
				nextpage = nextpage + inlink.val();
				var targetDiv = inlink.data("targetdiv");
				targetDiv = targetDiv.replace(/\//g, "\\/");
				jQuery.get(nextpage, {}, function(data) 
				{
					var	cell = jQuery("#" + targetDiv);
					cell.replaceWith(data);
					$(window).trigger( "resize" );
				});	
			}
		);
	
	jQuery("a.toggle-visible").livequery('click',
			function(e) 
			{
				e.preventDefault();
				var div = jQuery(this).data("targetdiv");
				var target = jQuery("#" + div );
				if(target.is(":hidden"))
				{
					var hidelable = jQuery(this).data("hidelabel");
					jQuery(this).find("span").text(hidelable);
          			target.show();
          		} else {

					var showlabel = jQuery(this).data("showlabel");
					jQuery(this).find("span").text(showlabel);
          			target.hide();
          		}
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
	
	var browserlanguage =  app.data("browserlanguage");
	if( browserlanguage == undefined )
	{
		browserlanguage = "";
	}
	jQuery.datepicker.setDefaults(jQuery.extend({
		showOn: 'button',
		buttonImage: themeprefix + '/entermedia/images/cal.gif',
		buttonImageOnly: true,
		changeMonth: true,
		changeYear: true, 
		yearRange: '1900:2050'
	}, jQuery.datepicker.regional[browserlanguage]));  //Move this to the layout?
	
		jQuery("input.datepicker").livequery( function() 
		{
		var targetid = jQuery(this).data("targetid");
		jQuery(this).datepicker( {
			altField: "#"+ targetid,
			altFormat: "yy-mm-dd", 
			yearRange: '1900:2050'
		});
				
		var current = jQuery("#" + targetid).val();
		if(current != undefined)
		{
			//alert(current);
			var date;
			if( current.indexOf("-") > 0) //this is the standard
			{
				current = current.substring(2,10);
				//2012-09-17 09:32:28 -0400
				date = jQuery.datepicker.parseDate('yy-mm-dd', current);
			}
			else
			{
				date = jQuery.datepicker.parseDate('mm/dd/yy', current); //legacy support
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
		});
	});
	
	//deprecated, use data-confirm
	jQuery(".confirm").livequery('click',
			function(e)
			{
				var inText = jQuery(this).attr("confirm");
				if( !inText )
				{
					inText = jQuery(this).data("confirm");
				}
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

		jQuery("form.ajaxform").livequery('submit', //Make sure you use $(this).closest("form").trigger("submit")	
		function(e) 
		{
			e.preventDefault();
			var form = jQuery(this);
			form.validate({
			  ignore: ".ignore"
			});
			
			
    		var isvalidate = form.valid();
			if(!isvalidate)
        	{
            	e.preventDefault();
            	//show message
            	return;
        	}
			var targetdiv = form.attr("targetdiv");
			targetdiv = targetdiv.replace(/\//g, "\\/");
			// allows for posting to a div in the parent from a fancybox.
			if(targetdiv.indexOf("parent.") == 0)
			{
				targetdiv = targetdiv.substr(7);
				parent.jQuery(this).ajaxSubmit({
					target:"#" + targetdiv
				 });
				 
				// closes the fancybox after submitting
				parent.jQuery.fancybox.close();
			}
			else
			{
				jQuery(this).ajaxSubmit({
					target:"#" + targetdiv
				 });
			}
			

			var reset =form.data("reset") 
			if( reset == true){
				form.get(0).reset();
			}
			return false;
		}
	);
	
	jQuery("form.autosubmit").livequery( function() 
	{
		var form = $(this);
		var targetdiv = form.data('targetdiv');
		jQuery(this,"select").change(function() 
		{
			jQuery(form).ajaxSubmit( {target:"#" + targetdiv} );
		});
		jQuery(this,"input").on("keyup",function() 
		{
			jQuery(form).ajaxSubmit( {target:"#" + targetdiv} );
		});

	});
	
	
	
	
	jQuery("form.ajaxform input.cancel").livequery('click',function()
	{
		parent.jQuery.fancybox.close();
	});
	
	jQuery("form.ajaxautosubmit").livequery( function() 
			{
				var theform = jQuery(this); 
				theform.find("select").change( function()
						{
							theform.submit();
						});
	});
	
	
	jQuery("a.emdialog").livequery("click",
			function(event) 
			{
				event.stopPropagation();
				var dialog = jQuery(this);
				var hidescrolling = dialog.data("hidescrolling");
				
				var height = dialog.data("height");
				if( !height )
				{
					height = "500";
				}
	
				var width = dialog.data("width");
				if( !width )
				{
					width = "650";
				}
				var id = "modals";
				var modaldialog = $( "#" + id );
				if( modaldialog.length == 0 )
				{
					$(document.body).append('<div class="modal" id="' + id + '" style="display:none" ></div>');
					modaldialog = $("#" + id );
				}
				var link = dialog.attr("href");
				 
				// modaldialog.modal("show"); 
				 
				modaldialog.load(link, function() { 
        		 	modaldialog.modal("show"); 
    			});
				event.preventDefault();
				return false;
				/*
				dialog.fancybox(
				{ 
					'zoomSpeedIn': 0, 'zoomSpeedOut': 0, 'overlayShow': true,
					enableEscapeButton: true, 
					type: 'iframe',
			        height: height,
			        width: width,
					autoScale: false,
			        autoHeight: false,
			        fitToView: false,
			        iframe: { 
			        	preload   : false ,
			        	scrolling: hidescrolling ? "no" : "auto"
			        }
				});
				*/
	});
	
	
	jQuery("a.thickbox").livequery(
			function() 
			{
				jQuery(this).fancybox(
				{
			    	openEffect	: 'elastic',
			    	closeEffect	: 'elastic',
			    	helpers : {
			    		title : {
			    			type : 'inside'
			    		}
			    	}
				});
	});
	
	
	jQuery("#fancy_content .fancyclose").livequery( function() {
		//if( $(this).parent.jQuery.fancybox )
		{
		//	$(this).parent.jQuery.fancybox.close();
		}	
		
	});
	
	
	
	jQuery("#closemodal").livequery("click", function() {
		parent.jQuery.fancybox.close(); 
	});
	
	jQuery("a.slideshow").livequery(
		function() 
		{
			jQuery(this).fancybox(
			{ 
				'zoomSpeedIn': 300, 'zoomSpeedOut': 300, 'overlayShow': true , 'slideshowtime': 6000
			});
	});

	jQuery("img.framerotator").livequery(
		function()
		{
			jQuery(this).hover(
				function() {
					jQuery(this).data("frame", 0);
					var path = this.sr$('select#speedC').selectmenu({style:'dropdown'});c.split("?")[0];
					var intval = setInterval("nextFrame('" +  this.id + "', '" + path + "')", 1000);
					jQuery(this).data("intval", intval);
				},
				function() {
					var path = this.src.split("?")[0];
					this.src = path + '?frame=0';
					var intval = jQuery(this).data("intval");
					clearInterval(intval);
				}
			); 
	});
	

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
	        swfPath: apphome + '/components/javascript',
	        supplied: "mp3",
	        wmode: "window",
	        cssSelectorAncestor: "#" + containerid
	    });
		
		//player.jPlayer("play");

	});


	$('.select-dropdown-open').livequery("click",function(){
		
		if ($(this).hasClass('down')) {
			$(this).removeClass('down');
			$(this).addClass('up');
			$(this).siblings('.select-dropdown').show();
		} else {
			$(this).removeClass('up');
			$(this).addClass('down');
			$(this).siblings('.select-dropdown').hide();
		}
	});
	$('.select-dropdown li a').livequery("click",function(){
		$(this).closest('.select-dropdown').siblings('.select-dropdown-open').removeClass('up');
		$(this).closest('.select-dropdown').siblings('.select-dropdown-open').addClass('down');
		$(this).closest('.select-dropdown').hide();
	});
	
	function select2formatResult(emdata)
	{
	/*	var element = $(this.element);
		var showicon = element.data("showicon");
		if( showicon )
		{
			var type = element.data("searchtype");
	    	var html = "<img class='autocompleteicon' src='" + themeprefix + "/images/icons/" + type + ".png'/>" + emdata.name;
	    	return html;
		}
		else
		{
			return emdata.name;
		}
	*/
		return emdata.name;
	}
	function select2Selected(selectedoption) {

		//"#list-" + foreignkeyid
//		var id = container.closest(".select2-container").attr("id");
//		id = "list-" + id.substring(5); //remove sid2_
//		container.closest("form").find("#" + id ).val(emdata.id);
		return selectedoption.name || selectedoption.text;
	}
	jQuery("select.listtags").livequery( function() 
	{
		var theinput = jQuery(this);
		var searchtype = theinput.data('searchtype');
		var searchfield = theinput.data('searchfield');
		var catalogid = theinput.data('listcatalogid');
		var sortby = theinput.data('sortby');
		var defaulttext = theinput.data('showdefault');
		if( !defaulttext )
		{
			defaulttext = "Search";
		}
		var url = apphome + "/components/xml/types/autocomplete/tagsearch.txt?catalogid=" + catalogid + "&field=" + searchfield + "&operation=contains&searchtype=" + searchtype;
	
		theinput.select2({
		  	tags: true,
			placeholder : defaulttext,
			allowClear: true,
			delay: 250,
			minimumInputLength : 1,
			ajax : { // instead of writing the function to execute the request we use Select2's convenient helper
				url : url,
				dataType : 'json',
				data : function(params) 
				{
					var search = {
						page_limit : 15,
						page: params.page
					};
					search[searchfield+ ".value"] = params.term.toLowerCase(); //search term
					search["sortby"] = sortby; //search term
					return search;
				},
				processResults: function(data, params) { // parse the results into the format expected by Select2.
				 	 params.page = params.page || 1;
					 return {
				        results: data.rows,
				        pagination: {
				          more: false //(params.page * 30) < data.total_count
				        }
				      };
				}
			},
			escapeMarkup: function(m) { return m; },
			templateResult : select2formatResult, 
			templateSelection : select2Selected,
			tokenSeparators: [",","|"],
			separator: '|'
		  }).change(function() { $(this).valid(); });  //is this still needed?

	});

	jQuery("input.grabfocus").livequery( function() 
	{
		var theinput = jQuery(this);
		theinput.focus();
	});

	$(".emtabs").livequery( function()   
	{
		var tabs = $(this); 
		
		var tabcontent = $("#" + tabs.data("targetdiv"));
		
		//active the right tab
		var hash = window.location.hash;
		if( hash )
		{
			var activelink = $(hash,tabs);
			if( activelink.length == 0)
			{
				hash = false;
			}
		}
		
		if( !hash )
		{
			hash = "#" + tabs.data("defaulttab");
		}
		var activelink = $(hash,tabs);
		var loadedpanel = $(hash + "panel",tabcontent);
		if( loadedpanel.length == 0)
		{
			loadedpanel = $("#loadedpanel",tabcontent);
			loadedpanel.attr("id",activelink.attr("id") + "panel");
			activelink.data("tabloaded",true);
		}	
		activelink.parent("li").addClass("emtabselected");
		activelink.data("loadpageonce",false);
		
		$("a:first-child",tabs).livequery("click", function (e)   
		{
			e.preventDefault();
			
	    	var link = $(this); // activated tab
			$("li",tabs).removeClass("emtabselected");
	    	link.parent("li").addClass("emtabselected");
	    	
		    var id = link.attr("id");

		    var url = link.attr("href");
		    url = url + "#" + id;
			var panelid =  id + "panel";
			var tab = $( "#" + panelid);
			if( tab.length == 0)
			{
			  tab = tabcontent.append('<div class="tab-pane" id="' + panelid + '" ></div>');
			  tab = $( "#" + panelid);
			}	  
			
			var reloadpage = link.data("loadpageonce");
			var alwaysreloadpage = link.data("alwaysreloadpage");
			if( reloadpage || alwaysreloadpage )
			{
				if( window.location.href.endsWith( url ) )
				{
					window.location.reload();
				}
				else
				{
					window.location.href = url;
				}
			}
			else
			{
				var loaded = link.data("tabloaded");
				if( link.data("allwaysloadpage") )
				{	
					loaded = false;
				}
				if( !loaded )
				{
					var levels = link.data("layouts");
					if( !levels)
					{
						levels = "1";
					}
					jQuery.get(url , {oemaxlevel:levels}, function(data) 
					{
						tab.html(data);
						link.data("tabloaded",true);
						$(">.tab-pane",tabcontent).hide();
						tab.show();
						$(window).trigger( "resize" );
					});
				}
				else
				{
					$(">.tab-pane",tabcontent).hide();
					tab.show();
					$(window).trigger( "resize" );
				}
			}	
		});
	});
	
	jQuery(".closetab").livequery('click',
			function(e) 
			{
				e.preventDefault();
				var tab = $(this);
				var nextpage = tab.data("closetab");
				jQuery.get(nextpage, {oemaxlayout:1}, function(data) 
				{
					var prevtab = tab.closest('li').prev();
					prevtab.find('a').click();
					
					if (prevtab.hasClass('firstab')) {
						tab.closest('li').remove();
					}
					
					
				});
				return false;
			}
	);
	
	jQuery(".collectionclose").livequery('click',
			function(e) 
			{
				e.preventDefault();
				var collection = $(this);
				var nextpage = collection.data("closecollection");
				jQuery.get(nextpage, {oemaxlayout:1}, function(data) 
				{
					collection.closest('li').remove();
				});
				return false;
			}
	);

	jQuery("select.listautocomplete").livequery(function()   //select2
	//jQuery.fn.liveajax("select.listautocomplete", function()   //select2
	{
		var theinput = jQuery(this);
		var searchtype = theinput.data('searchtype');
		if(searchtype != undefined ) //called twice due to the way it reinserts components
		{
			var searchfield = theinput.data('searchfield');
			var catalogid = theinput.data('listcatalogid');

			var foreignkeyid = theinput.data('foreignkeyid');
			var sortby = theinput.data('sortby');

			var defaulttext = theinput.data('showdefault');
			if( !defaulttext )
			{
				defaulttext = "Search";
			}
			var defaultvalue = theinput.data('defaultvalue');
			var defaultvalueid = theinput.data('defaultvalueid');

			var url = apphome + "/components/xml/types/autocomplete/datasearch.txt?catalogid=" + catalogid + "&field=" + searchfield + "&operation=contains&searchtype=" + searchtype;
			if( defaultvalue != undefined )
			{
				url =  url + "&defaultvalue=" + defaultvalue + "&defaultvalueid=" + defaultvalueid;
			}
			//var value = theinput.val();
			theinput.select2({
				placeholder : defaulttext,
				allowClear: true,
				minimumInputLength : 0,
				ajax : { // instead of writing the function to execute the request we use Select2's convenient helper
					url : url,
					dataType : 'json',
					data : function(params) 
					{
						var fkv = theinput.closest("form").find("#list-" + foreignkeyid + "value").val();
						if( fkv == undefined )
						{
							fkv = theinput.closest("form").find("#list-" + foreignkeyid).val();
						}
						var search = {
							page_limit : 15,
							page: params.page
						};
						search[searchfield+ ".value"] = params.term; //search term
						if( fkv )
						{
							search["field"] = foreignkeyid; //search term
							search["operation"] = "matches"; //search term
							search[foreignkeyid+ ".value"] = fkv; //search term
						}
						if( sortby )
						{
							search["sortby"] = sortby; //search term
						}
						return search;
					},
					processResults: function(data, params) { // parse the results into the format expected by Select2.
					 	 params.page = params.page || 1;
						 return {
					        results: data.rows,
					        pagination: {
					          more: false //(params.page * 30) < data.total_count
					        }
					      };
					}
				},
				escapeMarkup: function(m) { return m; },
				templateResult : select2formatResult, 
				templateSelection : select2Selected
			});
			
			//TODO: Remove this?
			theinput.on("change", function(e) {
				if( e.val == "" ) //Work around for a bug with the select2 code
				{
					var id = "#list-" + theinput.attr("id");
					jQuery(id).val("");
				}
			});
		}
	});		

	if( jQuery.fn.minicolors )
	{
		$(".color-picker").minicolors({
						defaultValue: '',
						letterCase: 'uppercase'
					});
	}	
	
	jQuery(".sidebarsubmenu").livequery("click", function(e){
		e.stopPropagation();
	});
}


jQuery(document).ready(function() 
{ 
	uiload();
	$(window).on('resize',function()
	{
		w1 = ( $('#main').width() - $('#left-col').width() - 41 );
		$('#right-col .liquid-sizer').width(w1);
		w2 = ( $('#data').width() - 40 );
		$('#asset-data').width(w2);
		//w3 = ( w2 - 23);
		//$('#commenttext').width(w3);
	});

}); 


