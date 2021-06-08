
var inittimeline = function() 
{
	var app = $("#application");
	var siteroot =  app.data("siteroot");
	var apphome = siteroot + app.data("apphome");

	var readyforedit = false;
	//$("#clipdetails :input").prop('disabled', true);

	var videoclip = $("#videoclip");
	var video = document.getElementById("videoclip");//videoclip[0];

	var timelinecursor = document.getElementById("timelinecursor");

	
	
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

	videoclip.on("timeupdate",function(e)
	{
		
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
	
	lQuery("#timecodestart\\.value").livequery("click",function(e)
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
	lQuery("#timecodelength\\.value").livequery("click",function(e)
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
	
	lQuery("#cliplabel\\.value").livequery("keyup", function()
	{
		updateSelectedClip();		
	});


	lQuery("#timecodestart\\.value").livequery("blur", function()
	{
		updateSelectedClip();
	});

	lQuery("#timecodelength\\.value").livequery("blur", function()
	{
		updateSelectedClip();		
	});


	lQuery("#nestedfields select").livequery("change", function()
	{
		if( readyforedit )
		{
		updateSelectedClip();
		}
	});
			
	
	lQuery(".removetime").livequery("click",function(e)
	{
		e.preventDefault();
		video.currentTime = video.currentTime - 1;
		
	});
	lQuery(".addtime").livequery("click",function(e)
	{
		e.preventDefault();
		video.currentTime = video.currentTime + 1;
	});
	lQuery("#removeclip").livequery("click",function(e)
	{
		e.preventDefault();
		$(".selectedclip").remove();
	});
	lQuery("#playclip").livequery("click",function(e)
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
	
	lQuery("#addnewcopy").livequery("click",function(e)
	{
		e.preventDefault();
		
		addNewClip();
				
	});
	
	
	addNewClip = function() {
		//console.log("Make copy");
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

		var lengthtext = $("#timecodelength\\.value").val();
		if( !lengthtext ) {
			$("#timecodelength\\.value").val("5");
		}		

		updateSelectedClip();	
		
		updateDetails();
		
		$("#cliplabel\\.value").focus();
	}
	
	
	
	lQuery("#savetimeline").livequery("click",function(e)
	{
		e.preventDefault();
		//Grab all the dom... Submit it to a method, render
		
		if (!$(this).hasClass("btn-disabled")) {
			var clips = [];
	    	$("#timelinemetadata  .data-selection").each(function() 
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
	
	lQuery("#removetime").livequery("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		video.currentTime = video.currentTime - .5;
		return false;
	});
	
	lQuery("#addtime").livequery("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		video.currentTime = video.currentTime + .5;
		return false;
	});
	
	
	
	lQuery(".data-selection").livequery("click",function(e)
	{
		e.preventDefault();
		
		selectClip(this);
		updateDetails();
	});	
	
	selectClip = function(div)
	{
		var div = $(div).closest(".data-selection");
		$(".data-selection").removeClass("selectedclip");
		div.addClass("selectedclip");
		updateDetails();
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

	updateDetails = function(jumptoend)
	{
		readyforedit = false;
		
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
		
		if( jumptoend )
		{
			video.currentTime = (decstart + len)/1000;
		}	
		else if( decstart )
		{
			video.currentTime = decstart/1000;
		}
		$("a.btn-disabled").removeClass("btn-disabled");
		$("#savetimeline").removeAttr("disabled");
		
		
		var assetName =  $("#timelineviewer").data("assetname");
		var clipname = selected.data("cliplabel");	
		if( clipname )
		{
			if( clipname.length > 10)			
			{
				clipname=clipname.substring(0,10);
			}
		}
		else 
		{
			clipname="clip";
		}
		clipname = assetName + "-" + clipname + "-"+ start +".mp4";
		
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
				select2.val(values.split("|")); //Split? |				
			}
			else
			{
				select2.val([]);
				//select2.select2("val", "");
			}
			select2.trigger('change'); 
		}
		else
		{
			//var input = jQuery("list_" + fieldid);
		}
		//get this from the 
	});
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

	lQuery(".grabresize").livequery(function()
	{
		var mainimage = $(this).closest(".timecell");
		var slider = $(this).closest(".time-slider");
		
		var clickspot;
		var startwidth;
		mainimage.on("mousedown", function(event)
		{
			if( $(event.target).hasClass("grabresize") )
			{
				clearSelection();
				clickspot = event;
				startwidth = mainimage.width();
				selectClip(this);
			}	
			
		});
		mainimage.on("mouseup", function(event)
		{
			clickspot = false;
			return false;
		});
		slider.on("mouseup", function(event)
		{
			clickspot = false;
			return false;
		});
		slider.on("mouseleave", function(event)
		{
			clickspot = false;
			return false;
		});
		slider.on("mousemove", function(event)
		{
			if( clickspot )
			{
				clearSelection();
				var changeleft = event.pageX - clickspot.pageX;
				var width = startwidth + changeleft;
				if( width < 10 )
				{
					width = 10;
				}
				mainimage.width(width);
				
				var ratio = $("#timelinemetadata").data("ratio");
				ratio = parseFloat(ratio);
				
				var miliseconds = (width / ratio );
				var selected = $(".selectedclip");
				
				
				selected.data("timecodelength",miliseconds);
				updateDetails(true);
				event.preventDefault();
				return false;
			}	
		});	
	});

	lQuery(".timecell").livequery(function()
	{
		var mainimage = $(this);
		var clickspot = false;
		var imageposition;
		mainimage.on("mousedown", function(event)
		{
			if( !$(event.target).hasClass("grabresize") )
			{
				clearSelection();
				clickspot = event;
				imageposition = mainimage.position();
				var left = imageposition.left;
				$("#timelinecursor").css({"left" : left+60 + "px"});
				return false;
			}	
		});
	
		mainimage.on("mouseup", function(event)
		{
			clickspot = false;
			return false;
		});
		mainimage.on("mouseleave", function(event)
		{
			clickspot = false;
			return false;
		});
		
		mainimage.on("mousemove", function(event)
		{
			if( clickspot )
			{
				clearSelection();
				
				var changeleft = clickspot.pageX - event.pageX;
				var changetop = clickspot.pageY - event.pageY;
				
				var left = imageposition.left - changeleft;
				if( left < 0 ) {
					left = 0;
				}
				$(this).css({"left" : left + "px"});
				
				var top = imageposition.top - changetop;// - changetop;
				if (top<0) {
					top = 0;
				}
				$(this).css({"top" : top + "px"});
			
				
				var ratio = $("#timelinemetadata").data("ratio");
				ratio = parseFloat(ratio);
				var seconds = left / ratio;
				
				//console.log("r:"+ratio+" l:"+left+" s:"+seconds);
				var selected = $(".selectedclip");
				selected.data("timecodestart",seconds);
				updateDetails();
				
				$("#timelinecursor").css({"left" : left+60 + "px"});
				
				//set video as well
				
			}	
		});	
	});
	var jump = $("#timelineviewer").data("timecodejump");
	//console.log(jump);
	if( jump )
	{
		//$('[data-test="the_exact_value"]')
		var selectedc = $(".data-selection" + '[data-timecodestart="' + jump + '"]');
		//console.log(selectedc);
		selectClip(selectedc);
	}

	lQuery(".clickjump").livequery("click",function(e)
		{
			e.preventDefault();
			var jump = $(this).data("starttime");
			jump = parseFloat(jump);
			video.currentTime = jump;
			
		}
	);
	
	lQuery("#timelineviewerbackground").livequery("click", function(event)
		{
			event.preventDefault();
			
			var left = event.pageX - $(this).offset().left;
			var clicked = left - 60;
			//console.log(left + " c:"+clicked);
			var ratio = $("#timelinemetadata").data("ratio");
			var start = (clicked / ratio) / 1000;
			video.currentTime = start;
			
			addNewClip();
			
		}
	);
	
	
	jQuery(document).on("mousemove", function(event)
			{
				//console.log(event);
				return false;
			});
	
	
};




jQuery(document).ready(function()
{
	inittimeline();
	
	$(window).on('tabready',function()
	{
		inittimeline();
	});
});






