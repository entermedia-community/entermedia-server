function zeroPad(num, numZeros) {
    var n = Math.abs(num);
    var zeros = Math.max(0, numZeros - Math.floor(n).toString().length );
    var zeroString = Math.pow(10,zeros).toString().substr(1);
    if( num < 0 ) {
        zeroString = '-' + zeroString;
    }

    return zeroString+n;
}


$(document).ready(function() 
{
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var themeprefix = app.data("home")	+ app.data("themeprefix");

	var videoclip = jQuery("#videoclip");
	var video = videoclip[0]; 


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
			var done = parseTimeToText(inTime - start);
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
		var parts = inText.split(":");
		if( parts.length == 1)
		{
			return parseFloat(parts[0]);
		}
		if( parts.length == 2)
		{
			var totals = 60 * parseFloat(parts[0]);
			totals = totals +  parseFloat(parts[1]);
			return totals;
		}	
		if( parts.length == 3)
		{
			var totals =  60 * 60 * parseFloat(parts[0]);				
			totals = totals +  60 * parseFloat(parts[1]);
			totals = totals +  parseFloat(parts[2]);
			return totals;
		}	
		
	}

	videoclip.on("timeupdate",function(e)
	{
		if(	$(".selectedtime").length > 0 || $(".selectedlength").length > 0 )
		{ 
			copyStartTime();	
			copyLength();
			updateClip();
		}			
	});
	$("#timecodestart-value").livequery("click",function(e)
	{
		var input = $(this);
		$("input").removeClass("selectedtime");
		$("input").removeClass("selectedlength");
		
		if( !input.val() )
		{
			copyStartTime();
			updateClip();
		}	
		var selected = $(".selectedclip");
		var start = selected.data("timecodestart");
		video.currentTime = start;
		input.addClass("selectedtime");
			
	});
	$("#timecodelength-value").livequery("click",function(e)
	{
		var input = $(this);
		$("input").removeClass("selectedtime");
		$("input").removeClass("selectedlength");
		if( !input.val() )
		{
			copyLength();
			updateClip();
		}		
		var selected = $(".selectedclip");
		var start = selected.data("timecodestart");
		var length = selected.data("timecodelength");
		video.currentTime = start + length;
		
		input.addClass("selectedlength");
		
	});
	
	$("#cliplabel\\.value").livequery("keyup", function()
	{
		updateClip();		
	});


	$("#timecodestart-value").livequery("blur", function()
	{
		updateClip();
	});

	$("#timecodelength-value").livequery("blur", function()
	{
		updateClip();		
	});
	
	jQuery(".removetime").livequery("click",function(e)
	{
		e.preventDefault();
		video.currentTime = video.currentTime - 1;
		
	});
	jQuery(".addtime").livequery("click",function(e)
	{
		e.preventDefault();
		video.currentTime = video.currentTime + 1;
	});
	jQuery("#removeclip").livequery("click",function(e)
	{
		e.preventDefault();
		$(".selectedclip").remove();
	});
	jQuery("#playclip").livequery("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		video.play();		
	});
	
	jQuery("#addnewcopy").livequery("click",function(e)
	{
		e.preventDefault();
		console.log("Make copy");
		var template = $("#templateclip").clone();
		template.attr("id","randomone");
		$(".selectedclip").removeClass("selectedclip");
		template.addClass("selectedclip");
		$("#timelinemetadata").prepend(template);
		template.show();
		updateDetails();
		updateClip();	
		$("#cliplabel\\.value").focus();
				
	});
	
	jQuery("#savetimeline").livequery("click",function(e)
	{
		e.preventDefault();
		//Grab all the dom... Submit it to a method, render
		
		var clips = [];

    	$("#timelinemetadata  .data-selection").each(function() 
    	{
			var clip = $(this);
			//cant use data() because it does not save doubles correctly timecodelength=10.0, timecodestart=0.0, cliplabel=sfsf, index=0}

			var timecodestart = parseFloat( clip.data("timecodestart") );
			var timecodelength = parseFloat( clip.data("timecodelength") );
			var data = {"timecodestart": timecodestart,"timecodelength":timecodelength,"cliplabel":clip.data("cliplabel")};
			
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
            	//change button color 
            	$("#savetimeline").addClass("btn-disabled");
            	$("#savetimeline").css("color","white");
       		}
    	}); 
	});
	
	jQuery(".addtime").livequery("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		video.currentTime = video.currentTime + 1;
		return false;
	});
	
	
	
	jQuery(".data-selection").livequery("click",function(e)
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

	updateClip = function()
	{
		var text = $("#cliplabel\\.value").val();

		var selected = $(".selectedclip");
		var cell = $(".selectedclip .timecell");
		selected.data("cliplabel", text);
		$(".cliptext",selected).html(text);

		var starttext = $("#timecodestart-value").val();
		var start = parseTimeFromText(starttext);
		selected.data("timecodestart", start);
		//calculate the px left
		var ratio = $("#timelinemetadata").data("ratio");
		var left = start * ratio;
		cell.css({"left" : left + "px"});

		var lengthtext = $("#timecodelength-value").val();
		var length = parseTimeFromText(lengthtext);
		selected.data("timecodelength", length);
		//console.log("Saved",length,selected);
		var width = length * ratio;	
		if( width < 1 )
		{
			width = 5;
		}	
		cell.css({"width" : width + "px"});
		$("#savetimeline").show();
	}	

	updateDetails = function(jumptoend)
	{
		var selected = $(".selectedclip");
		$("#cliplabel\\.value").val( selected.data("cliplabel") );
		var dec = selected.data("timecodestart");
		dec = parseFloat(dec);
		var start = parseTimeToText( dec );
		$("#timecodestart-value").val( start );
		//video.currentTime = dec; 
	
		var len = parseFloat(selected.data("timecodelength"));
		var textlength = parseTimeToText( len);
		$("#timecodelength-value").val( textlength );
		
		if( jumptoend )
		{
			video.currentTime = dec + len;
		}	
		else if( dec )
		{
			video.currentTime = dec;
		}
		$("a.btn-disabled").removeClass("btn-disabled");
		$("a.btn-yellow").removeClass("btn-yellow");
	}

/*
#set($time = $context.getRequestParameter("jumpto"))
#if($time)

	 var video = jQuery("#video");
	jQuery("#video").bind("loadeddata", function() {
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

	jQuery(".grabresize").livequery(function()
	{
		var mainimage = $(this).closest(".timecell");
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
				var changeleft = event.pageX - clickspot.pageX;
				var width = startwidth + changeleft;
				if( width < 10 )
				{
					width = 10;
				}
				mainimage.width(width);
				
				var ratio = $("#timelinemetadata").data("ratio");
				ratio = parseFloat(ratio);
				
				var seconds = width / ratio;
				var selected = $(".selectedclip");
				
				
				selected.data("timecodelength",seconds);
				updateDetails(true);
				event.preventDefault();
				return false;
			}	
		});	
	});

	jQuery(".timecell").livequery(function()
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
				
				var left = imageposition.left - changeleft;
				if( left < 0 )
				{
					left = 0;
				}
				//var top = imageposition.top;// - changetop;
				
				$(this).css({"left" : left + "px"});
				
				var ratio = $("#timelinemetadata").data("ratio");
				ratio = parseFloat(ratio);
				
				var seconds = left / ratio;
				var selected = $(".selectedclip");
				selected.data("timecodestart",seconds);
				updateDetails();
				
				//set video as well
				
			}	
		});	
	});
});	





