formatHitCountResult = function(inRow)
{
	return inRow[1];
}


function getRandomColor() {
    var letters = '0123456789ABCDEF'.split('');
    var color = '#';
    for (var i = 0; i < 6; i++ ) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
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
				
				$.validator.setDefaults({ignore: ".ignore"});

				
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
				if(!targetDiv){
					targetDiv = inlink.attr("targetdiv");
				}
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
	if( jQuery.datepicker )
	{
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
					current = current.substring(0,10);
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
	}
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
			
			if( form.validate )
			{
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
	        }	
			var targetdiv = form.data("targetdiv");
			if(!targetdiv){
				targetdiv = form.attr("targetdiv");
			}
			targetdiv = targetdiv.replace(/\//g, "\\/");
			// allows for posting to a div in the parent from a fancybox.
			
			// closes the fancybox after submitting
				

			form.ajaxSubmit({
				target:"#" + targetdiv,
				error: function(data ) {
					alert("error");
					$("#" + targetdiv).html(data);
				}
			 });
			
				
			var findmodal = form.closest(".modal");
			if( findmodal && findmodal.modal )
			{
				findmodal.modal("hide");
			}	
			

			var reset = form.data("reset") 
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
		jQuery("form.autosubmit select").change(function() 
		{
			jQuery(form).ajaxSubmit( {target:"#" + targetdiv} );
		});
		jQuery("form.autosubmit input").on("keyup",function() 
		{
			jQuery(form).ajaxSubmit( {target:"#" + targetdiv} );
		});

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
				
	
				var width = dialog.data("width");
				if( !width )
				{
					width = "800";
				}
				
				var id = "modals";
				var modaldialog = $( "#" + id );
				if( modaldialog.length == 0 )
				{
					$("#emcontainer").append('<div class="modal " tabindex="-1" id="' + id + '" style="display:none" ></div>');
					modaldialog = $("#" + id );
				}
				var link = dialog.attr("href");
				
				var param = dialog.data("parameterdata");
				var options = {};
				if( param )
				{	
					var element = $("#" + param );
					var name = element.prop("name");
					options[name] = element.val(); 
				}
				
				modaldialog.load(link, options, function() { 
					$(".modal-lg").css("min-width",width + "px" );
					//$(".modal-lg").css("min-height",height + "px" );
				
        		 	modaldialog.modal({keyboard: true,backdrop:true, "show":true});
        		 	//fix submit button
        		 	var justok = dialog.data("cancelsubmit");
        		 	if( justok != null)
        		 	{
        		 		$(".modal-footer #submitbutton",modaldialog).hide();
        		 	}
        		 	else
        		 	{
	        		 	var id = $("form",modaldialog).attr("id");
	        		 	$("#submitbutton",modaldialog).attr("form",id);
	        		 }	
        		 	var title = dialog.attr("title");
        		 	if( title == null)
        		 	{
        		 		title = dialog.text();
        		 	}
        		 	$(".modal-title",modaldialog).text(title);
        		 	var hidefooter = dialog.data("hidefooter");
        		 	if( hidefooter != null)
        		 	{
        		 		$(".modal-footer",modaldialog).hide();
        		 	}
        		 	$('form', modaldialog).find('*').filter(':input:visible:first').focus();
    			});
    			
				event.preventDefault();
				return false;
	});
	
	jQuery('.emrowpicker table td' ).livequery("click", function(event)
	{
		event.preventDefault();

		var clicked = jQuery(this);
		var row = clicked.closest("tr");
		var existing = row.hasClass("emrowselected");
		row.toggleClass("emrowselected");
		var id = row.data("id");
		
		var form = $(clicked.closest("form"));		
		$('.emselectedrow',form ).each(function()
		{
			if( form.hasClass("emmultivalue" ) )
			{
				var old = $(this).val();
				if( old )
				{
					if( existing )  //removing the value
					{
						old = old.replace(id, "");
						old = old.replace("||", "|");
					}
					else
					{
						old = old +"|"+id;
					}		
				}
				else
				{
					old = id;
				}
				$(this).val(old);
			}
			else
			{
				$(this).val(id);
			}
		});

		var targetdiv = form.data("targetdiv");
		if( (typeof targetdiv) != "undefined" )
		{
			jQuery(form).ajaxSubmit( {target:"#" + targetdiv} );	
		}	
		else
		{
			jQuery(form).trigger("submit");
		}
		if( form.hasClass("autoclose" ) )
		{
			form.closest(".modal").modal("hide");
		}
			
	});
		
	
	jQuery('#emselectable table td' ).livequery("click", function(event)
	{
		var clicked = jQuery(this);
		if(clicked.attr("noclick") =="true") {
			return true;
		}
		if( $(event.target).is("input") )
		{
			return true;
		}
		var emselectable = clicked.closest("#emselectable");
		var row = $(clicked.closest("tr"));
		if ( row.hasClass("thickbox") ) 
		{
			var href = row.data("href");
			openFancybox(href);
		}
		else 
		{
			emselectable.find('table tr' ).each(function(index) 
			{ 
				clicked.removeClass("emhighlight");
			});
			row.addClass('emhighlight');
			row.removeClass("emborderhover");
			var table = row.closest("table");
			var id = row.attr("rowid");
			//var url = emselectable.data("clickpath");
			var url = table.data("clickpath");
			var form = emselectable.find("form");
				
			if( form.length > 0 )
			{
				emselectable.find( '#emselectedrow' ).val(id);
				emselectable.find( '.emneedselection').each( function()
				{
					clicked.removeAttr('disabled');
				});	
				form.submit();
			}
			else if( url != undefined )
			{
				if (url=="") {
					return true;
				}
				var post = table.data("viewpostfix");
				if( post != undefined )
				{
					parent.document.location.href = url + id + post;
				}
				else
				{
					parent.document.location.href = url + id;
				}
			}
			else
			{
				parent.document.location.href = id;
			}
		}	
	}
	);

	jQuery('#emselectable table tr' ).livequery(
	function()
	{
		jQuery(this).hover(
			function () 
			{
			  	var row = jQuery(this).closest("tr");
				var id = jQuery(row).attr("rowid");
			    if( id != null )
			    {
				    jQuery(this).addClass("emborderhover");
				}
		 	}, 
			function () {
			    jQuery(this).removeClass("emborderhover");
			}
		);
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
		console.log("Clicked");
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
	
	jQuery(".suggestsearchinput").livequery( function() 
		{
			var theinput = jQuery(this);
			if( theinput && theinput.autocomplete )
			{
				theinput.autocomplete({
					source: apphome + '/components/autocomplete/assetsuggestions.txt',
					select: function(event, ui) {
						//set input that's just for display purposes
						theinput.val(ui.item.value);
						//theinput.submit();
						return false;
					}
				});
			}
		});

	jQuery("input.defaulttext").livequery("click", function() 
	{
		var theinput = $(this);
		var startingtext = theinput.data('startingtext');
		if( theinput.val() == startingtext )
		{
			theinput.val("");
		}
	});


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
		var url = apphome + "/components/xml/types/autocomplete/tagsearch.txt?catalogid=" + catalogid + "&field=" + searchfield + "&operation=startswith&searchtype=" + searchtype;
	
		theinput.select2({
		  	tags: true,
			placeholder : defaulttext,
			allowClear: true,
			selectOnBlur: true,
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
					search[searchfield+ ".value"] = params.term; //search term
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
			tokenSeparators: ["|"],
			separator: '|'
		  }).change(function() { 
		  	if( $(this).parents(".ignore").length == 0 )
		  	{
			  $(this).valid(); 
			}
		  });  //is this still needed?
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
		
		$("a:first-child",tabs).on("click", function (e)   
		{
			e.preventDefault();
			
	    	var link = $(this); // activated tab
			$("li",tabs).removeClass("emtabselected");
	    	link.parent("li").addClass("emtabselected");
	    	
		    var id = link.attr("id");

		    var url = link.attr("href");
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
		    	url = url + "#" + id;
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
						 var rows = data.rows;
						 if( theinput.hasClass("selectaddnew") )
						 {
						 	if( params.page == 1 || !params.page)
						 	{	
						 		var addnewlabel = theinput.data('addnewlabel');
							 	var addnewdata = { name: addnewlabel, id: "_addnew_" };
							 	rows.unshift(addnewdata);
							}	
						 }	 
						 //addnew
					 	 params.page = params.page || 1;
						 return {
					        results: rows,
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
				else
				{	
					//Check for "_addnew_" show ajax form
					var selectedid = theinput.val();
					
					if(  selectedid == "_addnew_" )
					{
						var clicklink = $("#" + theinput.attr("id") + "add");
						clicklink.trigger("click");
						
						e.preventDefault();
						theinput.select2("val", "");
						return false;
					}
					//Check for "_addnew_" show ajax form
					if( theinput.hasClass("selectautosubmit") )
					{
						if( selectedid )
						{
							var theform = jQuery(this).closest("form");
							theform.closest("form").trigger("submit");
						}	
					}
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

	jQuery("#mainimageholder").livequery(function(e)
	{
		// Zooming code, only makes sense to run this when we actually have the DOM
		if ($(this).position() == undefined){ // check if the element isn't there (best practice is...?)
			return;
		}
		var clickspot;
		var imageposition;
		var zoom = 40;
		var mainholder = $(this); 	
		mainholder.height($(window).height());
		mainholder.width($(window).width());
		var mainimage = $("#mainimage",mainholder);
/*		var centerimage = function()
		{
			var left = ( $(window).width() - mainimage.width() - $(".overlay-margin").width() ) / 2;
			mainimage.css({"left" : left + "px"});
		};
	 	mainimage.on("load",centerimage);
*/	 	
	
		$(window).bind('mousewheel DOMMouseScroll', function(event)
		{
			var mainimage = $("#mainimage");
			if ($("#hiddenoverlay").css("display") == "none") {
				return true;
			}

			if (event.originalEvent.wheelDelta > 0 || event.originalEvent.detail < 0) {
		        // scroll up
		        var w = mainimage.width(); 
		    	mainimage.width(w+zoom);
		    	var left = 	mainimage.position().left - zoom/2;
		    	mainimage.css({"left" : left + "px"});
		    	return false;
		    }
		    else {
		        // scroll down
		        var w = mainimage.width(); 
		    	mainimage.width(w-zoom);
		    	var left = 	mainimage.position().left + zoom/2;
		    	mainimage.css({"left" : left + "px"});
		    	return false;
		    }
		});
		
		mainimage.on("mousedown", function(event)
		{
			clickspot = event;
			imageposition = mainimage.position();
			console.log(event);
			return false;
		});
	
		mainimage.on("mouseup", function(event)
		{
			clickspot = false;
			return false;
		});
		
		mainimage.on("mousemove", function(event)
		{
			//if( isMouseDown() )
			if( clickspot )
			{
				var changetop = clickspot.pageY - event.pageY;
				var changeleft = clickspot.pageX - event.pageX;
				
				var left = imageposition.left - changeleft;
				var top = imageposition.top - changetop;
				
				$(this).css({"left" : left + "px", "top" : top + "px"});
			}	
		});		
	
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


