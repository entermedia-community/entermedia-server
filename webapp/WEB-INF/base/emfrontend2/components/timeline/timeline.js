if( !jQuery.fn.videoTimeline )
{
(function ( $ ) {

	//console.log("Timeline init1", $.fn.videoTimeline);
//	if( $.fn.videoTimeline != null )
//	{
//		console.log("Timeline init skip, already defined");
//		
//		return this;
//	}
//debugger;
	
    $.fn.videoTimeline = function( options ) {
 
        // This is the easiest way to have default options.
        var settings = $.extend({
            // These are the defaults.
            color: "#556b2f",
            backgroundColor: "white"
        }, options );
 
	console.log("Timeline init");
	var app = $("#application");
	var siteroot =  app.data("siteroot");
	var apphome = siteroot + app.data("apphome");

	var readyforedit = false;
	//console.log("NOt ready for edit")
	//$("#clipdetails :input").prop('disabled', true);

	//var videoclip = $("#videoclip");

	var timelinecursor = document.getElementById("timelinecursor");
	var timelineeditor = $(this);

	var video = timelineeditor.find("video")[0];//videoclip[0];
	
	$(".vjs-poster").hide();
	
	//fix container width
	var tlbg = $("#timelinebg");
	var tlv = $("#timelineviewer");
	//tlv.width(tlbg.width()+100);
	tlv.css("min-width", tlbg.width()+40 + "px");
	
	var parentoffset = 0;
	
	function zeroPad(num, numZeros) {
	    var n = Math.abs(num);
	    var zeros = Math.max(0, numZeros - Math.floor(n).toString().length );
	    var zeroString = Math.pow(10,zeros).toString().substr(1);
	    if( num < 0 ) {
	        zeroString = '-' + zeroString;
	    }

	    return zeroString+n;
	}

	copyStartTime = function()
	{
			var inTime = video.currentTime;
			var done = parseTimeToText(inTime);
			$(".selectedtime").val(done);  //00:00.000
			
	}
	
	copyLength = function()
	{
			var inTime = video.currentTime;
			var selected = $(".selectedclip");
			var start = selected.data("timecodestart");
			//take off the start
			var done = parseTimeToText(inTime - (start/1000));
			$(".selectedlength").val(done);  //00:00.000
			
	}

	parseTimeToText = function(inTime)
	{
			var justseconds = Math.floor(inTime);
			var justremainder = inTime - justseconds;			
			var millis = Math.floor(justremainder * 1000);
			
			var minutes = Math.floor(justseconds / 60);
			var m = zeroPad(minutes,2);

			var secondsleft = justseconds - (minutes*60);
			var s = zeroPad(secondsleft,2);
			var done = m + ":" + s + "." + millis;
			return done;
	}
	parseTimeFromText = function(inText)
	{
		if (typeof inText !== 'undefined' && inText != '') {
			var seconds = 0;
			var parts = inText.split(":");
			if( parts.length == 1)
			{
				seconds =  parseFloat(parts[0]);
			}
			if( parts.length == 2)
			{
				var totals = 60 * parseFloat(parts[0]);
				totals = totals +  parseFloat(parts[1]);
				seconds = totals;
			}	
			if( parts.length == 3)
			{
				var totals =  60 * 60 * parseFloat(parts[0]);				
				totals = totals +  60 * parseFloat(parts[1]);
				totals = totals +  parseFloat(parts[2]);
				seconds = totals;
			}	
			return seconds * 1000;
		}
	}

	
//	jQuery(".ts-data-selection").on("click",function(e)
//	{
//		e.preventDefault();
//		selectClip(this);
//	});	
	
	selectClip = function(clickdiv)
	{
		var div = $(clickdiv).closest(".ts-data-selection");
		$(".ts-data-selection").removeClass("selectedclip");
		div.addClass("selectedclip");
		updateDetails(true);
	}

	//Saved the selected data
	updateSelectedClip = function()
	{
		var text = $("#cliplabel\\.value").val();

		var selected = $(".selectedclip");
		var cell = $(".selectedclip .timecell");
		selected.data("cliplabel", text);
		$(".cliptext",selected).html(text);

		var starttext = $("#timecodestart\\.value").val();
		var start = parseTimeFromText(starttext);
		selected.data("timecodestart", start);
		//calculate the px left
		var ratio = $("#timelinemetadata").data("ratio");
		var left = start * ratio;
		cell.css({"left" : left + "px"});
		$("#timelinecursor").css({"left" : left+60 + "px"});
		
		var lengthtext = $("#timecodelength\\.value").val();
		var length = parseTimeFromText(lengthtext);
		selected.data("timecodelength", length);
		//console.log("Saved",length,selected);
		var width = length * ratio;	
		if( width < 1 )
		{
			width = 5;
		}	
		cell.css({"width" : width + "px"});
		
		$('#nestedfields input[name="field"]').each(function() {
			
			var fieldid = $(this).val();
			var select2 = jQuery("#list-" + fieldid);
			if( select2.length > 0)
			{
				//select2.val(null).trigger('change');
				var textvalues = select2.val().toString();
				textvalues = textvalues.replace(",","|");
				selected.data(fieldid,textvalues);  //Convert to |
			}
		});	
		
		$("#warningarea").css('visibility','visible');
	}	
	
	updateVideo = function(start) {
		video.currentTime = start;
	}

	updateTime = function(updatevideo)
	{
		var selected = $(".selectedclip");
		$("#clipdetails").css('display','block');	
		//$("#clipdetails :input").prop('disabled', false);
		
		$("#cliplabel\\.value").val( selected.data("cliplabel") );
		var decstart = selected.data("timecodestart");
		decstart = parseFloat(decstart);
		var start = parseTimeToText( decstart / 1000 );
		$("#timecodestart\\.value").val( start );
	
		var len = parseFloat(selected.data("timecodelength"));
		var textlength = parseTimeToText( len / 1000);
		$("#timecodelength\\.value").val( textlength );
		
		if( updatevideo)
		{
//			if( jumptoend )
//			{
//				video.currentTime = (decstart + len)/1000;
//			}	
			if( decstart )
			{
				updateVideo(decstart/1000);
			}
		}

	}
	

	updateDetails = function(updatevideo)
	{
		readyforedit = false;
		updateTime(updatevideo);
		
		$("a.btn-disabled").removeClass("btn-disabled");
		$("#savetimeline").removeAttr("disabled");
		
		var selected = $(".selectedclip");

//		var assetName =  $("#timelineviewer").data("assetname");
//		var clipname = selected.data("cliplabel");	
//		if( clipname )
//		{
//			if( clipname.length > 10)			
//			{
//				clipname=clipname.substring(0,10);
//			}
//		}
//		else 
//		{
//			clipname="clip";
//		}
		//clipname = assetName + "-" + clipname + "-"+ start +".mp4";
		
/*
 * 
 		var link = $("#downloadclip");
		
		var mediadbappid =  $("#timelineviewer").data("mediadb");
		var sourcepath =  $("#timelineviewer").data("sourcepath");
		var source = "/" + mediadbappid + "/services/module/asset/downloads/converted/cache/" + sourcepath + "/video.mp4";
		source = source + "?start=" + decstart;
		source = source + "&endtime=" + (decstart + len);
		source = source + "&downloadname=" + clipname;
		source = source + "&forcedownload=true"; 
		
		link.attr("href",source);
*/
		
	
	$('#nestedfields input[name="field"]').each(function() {
		
		var fieldid = $(this).val();
		var values = selected.data(fieldid);
		
		var select2 = jQuery("#list-" + fieldid);
		if( select2.length > 0)
		{
			if( values )
			{
				select2.val(); //Split? |
				var options = values.split("|"); //Need \\| ?
				for(var i=0; i< options.length; i++){
					if (select2.find("option[value='" + options[i] + "']").length) {
						
					} else { 
					    var newOption = new Option(options[i], options[i], true, true);
					    select2.append(newOption).trigger('change');
					} 
				}
				select2.val(options).trigger('change');
			}
			else
			{
				select2.val([]);
				select2.trigger('change');
				//select2.select2("val", "");
			}
			 
		}
		else
		{
			//var input = jQuery("list_" + fieldid);
		}
		//get this from the 
	});
	//console.log("ready for edit")
	readyforedit = true;
		//Load up HTML for details
//		jQuery.ajax(
//				{
//					url:  apphome + "/components/timeline/details/fields.html?id=" + property + "&" + property + ".value=" + value,
//					success: onsuccess,
//					xhrFields: {
//		                withCredentials: true
//		            },
//					crossDomain: true
//				}
//			);
		
	}
	//updateDetails finish
	
	
	updateCursor = function(start) {
		var ratio = $("#timelinemetadata").data("ratio");
		var left = start * ratio * 1000;
		$("#timelinecursor").css({"left" : left+60 + "px"});
	}

/*
#set($time = $context.getRequestParameter("jumpto"))
#if($time)

	 var video = $("#video");
	$("#video").bind("loadeddata", function() {
		jump('$time');
		pause();
	});
#end
*/	

	clearSelection = function()
	{
		$("input").removeClass("selectedtime");
		$("input").removeClass("selectedlength");
	}

//	jQuery(".grabresize").on(function()
//	{
//		var selectedbox = $(this).closest(".timecell");
//		var slider = $(this).closest(".time-slider");
//		
//		var clickspot;
//		var startwidth;
//		selectedbox.on("mousedown", function(event)
//		{
//			if( $(event.target).hasClass("grabresize") )
//			{
//				clearSelection();
//				clickspot = event;
//				startwidth = selectedbox.width();
//				selectClip(this);
//			}	
//			
//		});
//		selectedbox.on("mouseup", function(event)
//		{
//			clickspot = false;
//			return false;
//		});
//		slider.on("mouseup", function(event)
//		{
//			clickspot = false;
//			return false;
//		});
//		slider.on("mouseleave", function(event)
//		{
//			clickspot = false;
//			return false;
//		});
//		slider.on("mousemove", function(event)
//		{
//			if( clickspot )
//			{
//				clearSelection();
//				var changeleft = event.pageX - clickspot.pageX;
//				var width = startwidth + changeleft;
//				if( width < 10 )
//				{
//					width = 10;
//				}
//				selectedbox.width(width);
//				
//				var ratio = $("#timelinemetadata").data("ratio");
//				ratio = parseFloat(ratio);
//				
//				var miliseconds = (width / ratio );
//				var selected = $(".selectedclip");
//				
//				
//				selected.data("timecodelength",miliseconds);
//				updateDetails(true);
//				event.preventDefault();
//				return false;
//			}	
//		});	
//	});

	var clickspot = {};
	var selectedbox = null;
	var resizingbox = false;
	var startwidth = 10;

	var moved = false;
	
	$(video).on("timeupdate", function(e) {
		updateCursor(video.currentTime);
		
		if(	$(".selectedtime").length > 0 || $(".selectedlength").length > 0 )
		{ 
			copyStartTime();	
			copyLength();
			updateSelectedClip();
		}	
		var link = $("#playclip");
				
		if( video.paused )
		{
			link.text(link.data("playtext"));
			link.removeClass("playing");
		}
		else if(link.hasClass("playing") )
		{
			//If they pressed the playbutton on the details then stop
			var selected = $(".selectedclip");
			var start = selected.data("timecodestart");
			var length = selected.data("timecodelength");
			//console.log(video.currentTime , start , length );
			if( video.currentTime > parseFloat(start) + parseFloat(length) )
			{
				video.pause();
				link.text(link.data("playtext"));
				link.removeClass("playing");
			}
			else
			{
				link.text(link.data("stoptext"));
				link.addClass("playing");				
			}
		}
		
	});
	
	jQuery("#timecodestartX\\.value").on("click",function(e)
	{
		//debugger;
		var input = $(this);
		$("input").removeClass("selectedtime");
		$("input").removeClass("selectedlength");
		
		if( !input.val() )
		{
			copyStartTime();
			updateSelectedClip();
		}	
		var selected = $(".selectedclip");
		var start = selected.data("timecodestart");
		video.currentTime = parseFloat(start);
		input.addClass("selectedtime");
			
	});
	
	jQuery("#timecodelengthX\\.value").on("click",function(e)
	{
		//debugger;
		var input = $(this);
		$(input).removeClass("selectedtime");
		$(input).removeClass("selectedlength");
		if( !input.val() )
		{
			copyLength();
			updateSelectedClip();
		}		
		var selected = $(".selectedclip");
		var start = selected.data("timecodestart");
		var length = selected.data("timecodelength");
		var millis = parseFloat(start) + parseFloat(length);
		video.currentTime = millis / 1000;
		
		input.addClass("selectedlength");
		
	});
	
	jQuery("#cliplabel\\.value").on("keyup", function()
	{
		updateSelectedClip();		
	});


	jQuery("#timecodestart\\.value").on("keyup", function()
	{
		updateSelectedClip();
		var selected = $(".selectedclip");
		var decstart = selected.data("timecodestart");
		decstart = parseFloat(decstart);
		if (decstart) {
			updateVideo(decstart/1000);
		}
	});

	jQuery("#timecodelength\\.value").on("keyup", function()
	{
		updateSelectedClip();		
		
	});


	//jQuery("#nestedfields select").off("change" )
	jQuery("#nestedfields select").on("change", function()
	{
		if( readyforedit )
		{
			updateSelectedClip();
		}
	});
			
	
	jQuery("#removetime").on("click",function(e)
	{
		e.preventDefault();
		var selected = $(".selectedclip");
		if (selected.length){
			var current = selected.data("timecodestart");
			selected.data("timecodestart", current-1000);
			updateTime(true);
			updateSelectedClip();
		}
		else {
			video.currentTime = video.currentTime - 1;
		}
		
	});
	jQuery("#addtime").on("click",function(e)
	{
		e.preventDefault();
		var selected = $(".selectedclip");
		if (selected.length){
			var current = selected.data("timecodestart");
			selected.data("timecodestart", current+1000);
			updateTime(true);
			updateSelectedClip();
		}
		else {
			video.currentTime = video.currentTime + 1;
		}
	});
	jQuery("#removeclip").on("click",function(e)
	{
		e.preventDefault();
		$(".selectedclip").remove();
	});
	jQuery("#playclip").on("click",function(e)
	{
		e.preventDefault();
		
		var link = $(this);
		if( link.hasClass("playing") )
		{
			video.pause();
			link.text(link.data("playtext"));
			link.removeClass("playing");
		}
		else
		{
			var selected = $(".selectedclip");
			var start = selected.data("timecodestart");
			if (typeof start!== 'undefined') {
				video.currentTime = parseFloat(start);
				video.play();
				link.text(link.data("stoptext"));
				link.addClass("playing");
			}
		}
	});
	
	jQuery("#addnewcopy").on("click",function(e)
	{
		e.preventDefault();
		addNewClip();
	});
	
	
	addNewClip = function() {
		var template = $("#templateclip").clone();
		var timestamp = new Date().getUTCMilliseconds();
		template.attr("id",timestamp);
		$(".selectedclip").removeClass("selectedclip");
		template.addClass("selectedclip");
		$("#timelinemetadata").append(template);
		template.show();
		template.css("z-index","6")
		//This copies the UI into the current selection
		$("#clipdetails").css('display','block');
		$("#cliplabel\\.value").val("");
		var done = parseTimeToText(video.currentTime);
		$("#timecodestart\\.value").val(done);

		$("#timecodelength\\.value").val("10");
		
		//clean fields
		$('#nestedfields input[name="field"]').each(function() {
			var fieldid = $(this).val();
			var select2 = jQuery("#list-" + fieldid);
			if( select2.length > 0)
			{
				select2.val(null).trigger('change');
			}
		});
		
		updateSelectedClip();	
		
		updateDetails(false);
		
		$("#cliplabel\\.value").focus();
	}
	
	
	
	jQuery("#savetimeline").on("click",function(e)
	{
		e.preventDefault();
		//Grab all the dom... Submit it to a method, render
		
		if (!$(this).hasClass("btn-disabled")) {
			var clips = [];
	    	$("#timelinemetadata  .ts-data-selection").each(function() 
	    	{
				var clip = $(this);
				//cant use data() because it does not save doubles correctly timecodelength=10.0, timecodestart=0.0, cliplabel=sfsf, index=0}
	
				var timecodestart = parseFloat( clip.data("timecodestart") );
				var timecodelength = parseFloat( clip.data("timecodelength") );
				var timecell = clip.find(".timecell");
				var toppx = parseInt( timecell.position().top );
				var data = {"timecodestart": timecodestart,"timecodelength":timecodelength,
						"cliplabel":clip.data("cliplabel"),
						"verticaloffset" : toppx		
				};
				
				$('#nestedfields input[name="field"]').each(function() {
					var fieldid = $(this).val();
					var v = clip.data(fieldid);
					if( v )
					{
						//v = v.replace(",","|");
						data[fieldid] = v;
					}
				});
				
	    		clips.push(data);
	    	});
	    	
			var assetid = $("#timelinemetadata").data("assetid");
			var link = $("#timelinemetadata").data("savelink");
			
			var data = {"assetid": assetid,"clips":clips};
			
			var json = JSON.stringify(data);
			$.ajax({        
	       		type: "POST",
	       		url: link,
	       		data: json,
	       		contentType: "application/json; charset=utf-8",
	    		dataType: "json",
	       		success: function() 
	       		{
	            	//reload page?
	            	//$("#savetimeline").addClass("btn-disabled");
					//$("#savetimeline").attr("disabled", true);
					$("#clipdetails").css('display','none');
					$("#warningarea").css('visibility','hidden');
	       		}
	    	}); 
		}
	});
	
	jQuery("#removetime").off("click");
	jQuery("#removetime").on("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		video.currentTime = video.currentTime - .5;
		return false;
	});
	
	jQuery("#addtime").off("click");
	jQuery("#addtime").on("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		video.currentTime = video.currentTime + .5;
		return false;
	});
	
	//We need to reinit but make sure we dont double register
	jQuery(".timecell").off("mousedown");
	lQuery(".timecell").livequery("mousedown", function(event)
	{
		//$(this).on("mousedown", function(event)
		//jQuery(this).on("mousedown", function(event)
		//{
			//console.log("clicked!",this,timelineeditor);
			
			selectedbox = $(this);
			//event.preventDefault();
			//event.stopPropagation();
			selectClip(selectedbox);
			
			if( !$(event.target).hasClass("grabresize") && !$(event.target).parent().hasClass("grabmove")  && !$(event.target).hasClass("grabmove")) 
			{
				
				return;
			}
			
			moved = false;
			
			startwidth = selectedbox.width();
			var xPos = event.pageX;
			var yPos = event.pageY;
			clickspot.xPos = xPos;
			clickspot.yPos = yPos;
			
			parentoffset = jQuery("#timelinemetadata").offset();
			parentoffset.width = jQuery("#timelinemetadata").width();
			parentoffset.height = jQuery("#timelinemetadata").height();
			//console.log("Parent position ",parentoffset);
			
			clickspot.relativeXPos = event.pageX - parentoffset.left;
			clickspot.relativeYPos = event.pageY - parentoffset.top;
			//console.log("Start Moving stuff ",clickspot);
			
			if( !$(event.target).hasClass("grabresize") )
			{
				clearSelection();
				//clickspot = event;
				resizingbox = false;
				var imageposition = selectedbox.position();
				var left = imageposition.left;
				$("#timelinecursor").css({"left" : left+60 + "px"});
				return false;
			}	
			else
			{
				resizingbox = true;
			}
			
		//});
	
//		selectedbox.on("mouseleave", function(event)
//		{
//			clickspot = {};
//			moving = false;
//			return false;
//		});
	});
	
		timelineeditor.on("mouseup", function(event)
		{
			if( selectedbox == null )
			{
				return;
			}
			clickspot = {};
			selectedbox = null;
			resizingbox = false;
			//moving = false;
			//console.log("Release");
			if (moved)
			{
				updateSelectedClip();
			}
			moved = false;
			//updateDetails(true);
			return false;
		});
		
		timelineeditor.on("mousemove", function(event)
		{
			if( selectedbox == null )
			{
				return;
			}
			
			moved = true;
			
			//Slow?
			//clearSelection();
		
			var changeleft =  event.pageX - clickspot.xPos;
			var changetop =  event.pageY - clickspot.yPos;
			//console.log("csX: " +clickspot.xPos + " eX: " +xPos);

			var ratio = $("#timelinemetadata").data("ratio");
			ratio = parseFloat(ratio);

			if( resizingbox )
			{
				var width = startwidth + changeleft;
				if( width < 10 )
				{
					width = 10;
				}
				selectedbox.css("width", width);
				
				var miliseconds = (width / ratio );
				//Slow?
				var selected = $(".selectedclip");
				selected.data("timecodelength",miliseconds);
				updateTime(false);
				//event.preventDefault();
				return false;
			}
			else
			{
				var left = clickspot.relativeXPos + changeleft;
				if( left < 0 ) {
					left = 0;
				}
				if (left > parentoffset.width) {
					left = parentoffset.width;
				}
				selectedbox.css({"left" : left + "px"});
				
				var top = clickspot.relativeYPos + changetop;
				if (top<0) {
					top = 0;
				}
				if (top > parentoffset.height) {
					top = parentoffset.height;
				}
				selectedbox.css({"top" : top + "px"});
				
				var seconds = left / ratio;
				$("#timelinecursor").css({"left" : left+60 + "px"});
				var selected = $(".selectedclip");
				selected.data("timecodestart",seconds);
				updateTime(true);
			}
		});

				/*
	var jump = $("#timelineviewer").data("timecodejump");
	//console.log(jump);
	if( jump )
	{
		//$('[data-test="the_exact_value"]')
		var selectedc = $(".ts-data-selection" + '[data-timecodestart="' + jump + '"]');
		//console.log(selectedc);
		selectClip(selectedc);
	}
	*/

	
	
	jQuery(".grabmove").on("click",function(e)
			{
				e.preventDefault();
				
			}
		);
	
	jQuery(".clickjump").on("click",function(e)
		{
			e.preventDefault();
			var jump = $(this).data("starttime");
			jump = parseFloat(jump);
			video.currentTime = jump;
			
		}
	);
	
	jQuery("#timelineviewerbackground").on("click", function(event)
		{
			event.preventDefault();
			$(".selectedclip").removeClass("selectedclip");
			//clean fields
			$("#clipdetails").hide();
			
			//console.log("bkg click");
			var left = event.pageX - $(this).offset().left;
			var clicked = left - 60;
			//console.log(left + " c:"+clicked);
			var ratio = $("#timelinemetadata").data("ratio");
			var start = (clicked / ratio) / 1000;
			video.currentTime = start;
			
			//addNewClip();
			
		}
	);
	

	return this;

};

}( jQuery ));

} //Dont reload

jQuery(document).ready(function()
{
	$("#timelineeditor").videoTimeline();
});






