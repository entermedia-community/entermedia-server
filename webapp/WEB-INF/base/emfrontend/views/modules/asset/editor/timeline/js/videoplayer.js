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

/*
	jQuery("body")
			.on("click","#addcue",
					
					function() {
						var paused = cuepoint.video.paused;
						var departmentasset = jQuery(this)
								.data("dataid");
						var current = cuepoint.currentTime();
						var targetfield = jQuery(this).data("targetfield");

						var time = Math.round(current);
						jQuery
								.ajax({
									url : clientroot
											+ "/addcue.html?save=true&field=timecode&timecode.value="
											+ time														
											+ "&field=assetid&assetid.value="
											+ departmentasset 

											+ "&targetfield=" 

											+ targetfield,
											
									async : false,
									success : function(data) {
									
										//reloadCues(paused, data);
										//$('#editmodal').modal('hide');
										
										if (paused) {
											pause();
										}
									}
								});
						
					});

		*/			
					


	var videoclip = jQuery("#videoclip");
	var video = videoclip[0]; 

	selectTime = function()
	{
			var inTime = video.currentTime;
			var done = parseTime(inTime);
			$(".selectedtime").val(done);  //00:00.000
			$(".selectedtime").data("time",inTime);
	}
	
	selectLength = function()
	{
			var inTime = video.currentTime;
			
			var start = $(".selectedtime").data("time");
			var length = "10";
			if( start )
			{	
				length = inTime - start;
			}	
				var done = parseTime(length);
				$(".selectedlength").val(done);  //00:00.000
	}

	parseTime = function(inTime)
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
	parseTimeInText = function(inTime)
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

	videoclip.on("timeupdate",function(e)
	{
			selectTime();	
			selectLength();		
	});
	$("#timecodestart-value").livequery("click",function(e)
	{
		var input = $(this);
		$("input").removeClass("selectedtime");
		$("input").removeClass("selectedlength");
		
		input.addClass("selectedtime");
		if( !input.val() )
		{
			selectTime();
		}		
	});
	$("#timecodelength-value").livequery("click",function(e)
	{
		var input = $(this);
		$("input").removeClass("selectedtime");
		$("input").removeClass("selectedlength");
		input.addClass("selectedlength");
		if( !input.val() )
		{
			selectLength();
		}		
	});
	
	$("#cliplabel\\.value").livequery("keyup", function()
	{
		var text = $(this).val();
		var selected = $(".selectedclip");
		selected.data("cliplabel", text);
		$(".cliptext",selected).html(text);
		
	});

	$("#timecodelength-value").livequery("change", function()
	{
		var val = $(this).val();
		
		//Convert to seconds
		var selected = $(".selectedclip");
		selected.data("cliplabel", text);
		$(".cliptext",selected).html(text);
		
	});
	
	jQuery(".removetime").livequery("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		
	//	val formated = $("#timecodestart\\.value").val();
	//	formated.split(":");
		
		video.currentTime = video.currentTime - 1;
		
		return false;
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
	

	updateDetails = function()
	{
		var selected = $(".selectedclip");
		$("#cliplabel\\.value").val( selected.data("cliplabel") );
		var dec = selected.data("timecodestart");
		if( dec )
		{
			dec = parseFloat(dec);
			var start = parseTime( dec );
			$("#timecodestart-value").val( start );
			console.log(dec);
			video.currentTime = dec; 
		
			var length = parseTime( selected.data("timecodelength") );
			$("#timecodelength-value").val( length );
		}	
		
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

	jQuery(".grabresize").livequery(function()
	{
		var mainimage = $(this).closest(".timecell");
		var clickspot;
		var startwidth;
		mainimage.on("mousedown", function(event)
		{
			if( $(event.target).hasClass("grabresize") )
			{
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
				var changeleft = event.pageX - clickspot.pageX;
				
				console.log("Moved: ",clickspot.pageX + " - " + event.pageX + " + " + startwidth);
				var width = startwidth + changeleft;
				mainimage.width(width);
				
				var ratio = $("#timelinemetadata").data("ratio");
				ratio = parseFloat(ratio);
				
				var seconds = width / ratio;
				var selected = $(".selectedclip");
				selected.data("timecodelength",seconds);
				updateDetails();
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
				var changeleft = clickspot.pageX - event.pageX;
				
				var left = imageposition.left - changeleft;
				//var top = imageposition.top;// - changetop;
				
				$(this).css({"left" : left + "px"});
				
				var ratio = $("#timelinemetadata").data("ratio");
				ratio = parseFloat(ratio);
				
				var seconds = left / ratio;
				var selected = $(".selectedclip");
				selected.data("timecodestart",seconds);
				updateDetails();
			}	
		});	
	});
});	





