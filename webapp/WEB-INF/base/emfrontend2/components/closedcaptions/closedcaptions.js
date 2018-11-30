var initclosedcaptions = function() 
{
	
	
	var app = $("#application");
	var apphome = app.data("home") + app.data("apphome");
	var themeprefix = app.data("home")	+ app.data("themeprefix");

	var video = $("#videoclip");
	video = video[0]; 
	
	var inTime = video.currentTime;
	$("#captionend").text(parseTimeToText(inTime));
	
	var link = $("#playtab");
	
	var starttime = 0;
	
	
	zeroPad = function(num, numZeros) 
	{
	    var n = Math.abs(num);
	    var zeros = Math.max(0, numZeros - Math.floor(n).toString().length );
	    var zeroString = Math.pow(10,zeros).toString().substr(1);
	    if( num < 0 ) {
	        zeroString = '-' + zeroString;
	    }

	    return zeroString+n;
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

	startchunk = function()
	{
	    video.play();
	    link.addClass("playing");
	    link.text(link.data("stoptext"));
	    $("#captioninput").focus();
	};
	
	stopchunk = function()
	{
		video.pause();
		link.removeClass("playing");
	    link.text(link.data("playtext"));
	    $("#captioninput").focus();
	};			    

	saveCaptionToServer = function()
	{
		//update the data
//		var starttime = video.currentTime;
//		$("#captionstart").text(parseTimeToText(starttime));
//		$("#timecodestart").val( Math.round(starttime * 1000 ) );
	
		$("#addcaption").ajaxSubmit({
			target:"#captionview",
			error: function(data)
			{
				$("#captionview").html(data);
			},
			success: function() 
			{
				$("#captioninput").val("");
				$("#scrollarea").scrollTop($("#scrollarea")[0].scrollHeight);
				var endingtime = video.currentTime + 8;
				$("#timecodelength").val(8000);
				$("#captionend").text(parseTimeToText(endingtime));
			}
	 	});
	}	

	//Closed caption stuff
	lQuery("#captioninput").livequery( "keydown",function(e) 
	{
			var theinput = $(this);
			
			var keyCode = e.keyCode || e.which; 

			if (keyCode == 9) //tab
			{ 
			 	e.preventDefault(); 
				if( video.paused )
				{
					startchunk();
				}
				else
				{
				    stopchunk();
				}
			} 
			else if (keyCode == 13) //enter
			{ 
				saveCaptionToServer();
			}
	});
	lQuery("#addcaption").livequery('submit',function(e)
	{ 
		e.preventDefault();
	 	return false;
	});
	
	lQuery("#playtab").livequery("click",function(e)
	{
		e.preventDefault();
		if(link.hasClass("playing") )
		{
		    stopchunk();
		}
		else
		{
			startchunk();
		}
		
	});
	
	lQuery(".lenguagepicker").livequery("change",function(e)
	{
		var selected = $(this);
		$("#langform").submit();
	});
	
	lQuery("#videoclip").livequery("timeupdate",function(e)
	{
		var link = $("#playtab");
		if( video.paused )
		{
			link.removeClass("playing");
			starttime = video.currentTime;
			$("#captionstart").text(parseTimeToText(starttime));
			$("#timecodestart").val( Math.round(starttime * 1000 ));
		}
		else if(link.hasClass("playing") )
		{
			 var inTime = video.currentTime;
			 if( inTime > starttime  + 8 )
			 {
			 	stopchunk();
			 }
		 	var inTime = video.currentTime + 8;
			$("#captionend").text(parseTimeToText(inTime));
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
	lQuery("#removecaption").livequery("click",function(e)
	{
		e.preventDefault();
		var link = $(this);
		
		$("#captioninput").val("");
		
		$("#addcaption").ajaxSubmit({
			target:"#captionview",
			error: function(data)
			{
				$("#captionview").html(data);
			},
			success: function() 
			{
			}
	 	});
		
		return false;
	});

	selectClip = function(div)
	{
		var div = $(div).closest(".data-selection");
		$(".data-selection").removeClass("selectedclip");
		div.addClass("selectedclip");
		updateDetails();
	}
	
	updateDetails = function(jumptoend)
	{
		var selected = $(".selectedclip");
	
		$("#captioninput").val( selected.data("cliplabel") );
		var decstart = selected.data("timecodestart");
		decstart = parseFloat(decstart);
		if( decstart )
		{
			video.currentTime = decstart / 1000;
		}
		var declength = selected.data("timecodelength");
		declength = parseFloat(declength);
		$("#timecodelength").val(declength);
		var ending = (decstart + declength);
		$("#captionend").text(parseTimeToText(ending/1000));
		
	}
	
	lQuery(".data-selection").livequery("click",function(e)
	{
		e.preventDefault();
		selectClip(this);
		updateDetails();
	});	
};