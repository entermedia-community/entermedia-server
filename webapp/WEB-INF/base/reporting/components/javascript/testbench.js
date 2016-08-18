$(document).ready(
		function() {

			var app = jQuery("#application");
			var apphome = app.data("home") + app.data("apphome");
			var themeprefix = app.data("home") + app.data("themeprefix");
		
			
			var options = { 
				    success:    function() { 
						updateTestDisplay();				    },
					error:    function() { 
						alert("Oh no!  Your response could not be saved.  This normally means your session has timed out or something went wrong.  Your previous responses will be saved.  You can restart this survey from your profile.");
						document.location.href = apphome + "/index.html";
					}
				}; 
			
			
			
			jQuery(".render-choose-file").on("change", function() {
				console.log("file value = "+ $(this).val());
				console.log("found a file form item");
				var target = $(this).closest("form").find("#resultarea");
				var options = { 
				       target:        '#'+target.attr("id"),
				       success: function(data, textStatus, jqXHR)
				       {
				    	   console.log("saved response OK");
				    	   updateTestDisplay();
				       }, 
				       data: { oemaxlevel: '1', key2: 'value2' }
				    };
				$(this).closest("form").ajaxSubmit(options);
			});
			
			jQuery(".fullpage-response").find('input').each(function() {
				var item = $(this);
				if (item.attr("type")!=undefined && item.attr("type")!="hidden"){
					var type = item.attr("type");
					/* handle bindings for each type */
					if (type == "radio" || type=="checkbox"){
						$(this).on("click", function() {
							console.log("found a radio form item");
							$(this).closest("form").ajaxSubmit(options, function() {
								console.log("saved response OK");
								updateTestDisplay();
							});
						});
					} else if (type == "text"){

						$(this).on("change", function() {
							console.log("found a text form item");
							$(this).closest("form").ajaxSubmit(options,function() {
								console.log("saved response OK");
								updateTestDisplay();
							});
						});
					} 
					
					/*else if (type == "file"){
						$(this).on("change", function() {
							console.log("file value = "+ $(this).val());
							console.log("found a file form item");
							var target = $(this).closest("form").find("#resultarea");
							var options = { 
							       target:        '#'+target.attr("id"),
							       success: function(data, textStatus, jqXHR)
							       {
							    	   console.log("saved response OK");
							    	   updateTestDisplay();
							       }
							    };
							$(this).closest("form").ajaxSubmit(options);
						});
					}*/
					
				}
				
				
				
			});
			
			jQuery(".detail-select").select2({

				placeholder : "Search..",
			});
			
			
			jQuery(".fullpage-response").find('textarea').each(function() {
				$(this).on("change", function() {
					console.log("found a text form item");
					$(this).closest("form").ajaxSubmit(options, function() {
						console.log("saved response OK");
						updateTestDisplay();
					});
				});
			});
			
			
			
			jQuery(".fullpage-response").find('select').each(function() {
				$(this).on("change", function() {
					console.log("found a text form item");
					
					$(this).closest("form").ajaxSubmit(options,function() {
						console.log("saved response OK");
						updateTestDisplay();
					});
				});
			});
			
			jQuery("input.tbdatepicker").livequery( function() 
			{
				var targetid = jQuery(this).data("targetid");
				var time = $(this).data("time");
				jQuery(this).datepicker({
					altField: "#"+ targetid,
					altFormat: "mm/dd/yy", 
					yearRange: '1900:2050',
					onSelect: function(selected,evnt) {
				         var value = $("#"+targetid).val();
				         if (value!="" && time!=undefined){
				        	 value = value + " "+time;
				        	 $("#"+targetid).val(value);
				         }
				    }
				});
						
				var current = jQuery("#" + targetid).val();
				if(current != undefined)
				{
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
				});
				
				
				
			});

			quickQuestion = function(inId) {

				var url = apphome
						+ "/test/quickquestion/index.html?questionid=" + inId;
				showModal(url);

			}
		});

loadTestStatus = function(myvar) {
	var urls = apphome + "/test/active/actions/teststatus.json";
	$.getJSON(urls, function(data) {

		// console.log(data.testdata.nodes);
		jQuery.each(data.testdata.nodes, function(index, more) {
			console.log(more.id + ":" + more.show);
		});

		// console.log(data.header1[0].title);
		// console.log(data.header2[0].title);
	});

}



updateTestDisplay = function () {
	var urls = apphome + "/test/active/actions/teststatus.json";
	console.log("Updating..");
	$.getJSON(urls,  function(data) {
		
		
		
		
		 var percent = data.testdata.percentcomplete;
		 $("#completion-bar").css("width", percent + "%");
		jQuery.each(data.testdata.nodes, function(index, more) {
			console.log(more.id + ":" + more.show);
			var id = more.id;
			var show = more.show;
			if(show){
				console.log("showing" + id);
				jQuery("#node-" + id).show();
			} else{
				console.log("hiding" + id);

				jQuery("#node-" + id).hide();

			}
		});

		
	});
	
	
	calculatetotals();
}


calculatetotals = function(){

	jQuery(".grid").each(function () {
			var overalltotal = 0;
		
			jQuery(this).find(".gridrow").each(function() {
				
				var rowid = $(this).data("rowid");
				var max = $(this).data("max");
				var questionid = $(this).data("questionid");
				var units = $(this).data("units");
		
		
				var total = 0;
				jQuery("." + rowid + "-field").each( function () {
					var include = jQuery(this).data("include");
					if(include != null && include){
						var val = parseFloat($(this).val());
						console.log(include);
						if($.isNumeric(val)){
							total = total + val;
							overalltotal = overalltotal + val;
						}
					}
					if(max != null && max != ""){
						
						if(total > max){
							console.log("total" + total);
							console.log("qid" + questionid);
							jQuery("#checktotal-" + questionid).html("Please ensure your total is less than " + max);
							jQuery("#node-" + questionid).addClass("node-required");
		
						} else{
							jQuery("#checktotal-" + questionid).html("");
							jQuery("#node-" + questionid).removeClass("node-required");
		
						}
					}
					
				});
				console.log(total);
				console.log(overalltotal);
				if(units == "percentage"){
						jQuery("#" + rowid + "-summary").html(total.toFixed(2) + "%");
						jQuery("#" + questionid + "-overallsummary").html(overalltotal.toFixed(2) + "%");
		
		
					
				} else{
					jQuery("#" + rowid + "-summary").html("$" + total.toFixed(2));
					jQuery("#" + questionid + "-overallsummary").html("$" + overalltotal.toFixed(2));
		
					
				}
		
				jQuery("#" + rowid + "-summary-field").val(total);
				
				jQuery("#" + questionid + "-overallsummary-field").val(overalltotal);
				
			}); 
	
	
	
	});
	
	
	
	
	
}







highlightRequired = function () {
	var urls = apphome + "/test/active/actions/teststatus.json";
	console.log("Updating..");
	$.getJSON(urls, function(data) {
		var first = null;
		jQuery.each(data.testdata.nodes, function(index, more) {
			var id = more.id;
			var show = more.show;
			var completed = more.completed;
			var required = more.required;
			if(show == true && required == true  && completed==false){
				if(first == null){
					first = jQuery("#node-" + id);
				}
				jQuery("#node-" + id).addClass("node-required");
			} else{
				jQuery("#node-" + id).removeClass("node-required");

			}
		});
		$('html,body').animate({
	        scrollTop: first.offset().top-20},
	        'slow');
		
	});
}



$( document ).ajaxComplete(function() {
	jQuery(".detail-select").select2({

		placeholder : "Search..",
});	});






