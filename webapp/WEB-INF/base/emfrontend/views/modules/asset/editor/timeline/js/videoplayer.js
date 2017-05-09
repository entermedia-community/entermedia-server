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

	selectTime = function()
	{
			var inTime = video.currentTime;
			var justseconds = Math.floor(inTime);
			var justremainder = inTime - justseconds;			
			var millis = Math.floor(justremainder * 1000);
			
			var minutes = Math.floor(justseconds / 60);
			var m = zeroPad(minutes,2);

			var secondsleft = justseconds - (minutes*60);
			var s = zeroPad(secondsleft,2);
			$(".selectedtime").val(m + ":" + s + "." + millis);  //00:00.000
	}


	videoclip.on("timeupdate",function(e)
	{
			selectTime();			
	});
	$("#timecodestart-value").livequery("click",function(e)
	{
		var input = $(this);
		$("input").removeClass("selectedtime");
		input.addClass("selectedtime");
		if( !input.val() )
		{
			selectTime();
		}		
	});
	$("#timecodeend-value").livequery("click",function(e)
	{
		var input = $(this);
		$("input").removeClass("selectedtime");
		input.addClass("selectedtime");
		if( !input.val() )
		{
			selectTime();
		}		
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
	jQuery(".timecell").livequery(function()
	{
		var mainimage = $(this);
		var clickspot;
		var imageposition;
		 
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
				var top = imageposition.top;// - changetop;
				
				$(this).css({"left" : left + "px", "top" : top + "px"});
			}	
		});	
	});
});	





