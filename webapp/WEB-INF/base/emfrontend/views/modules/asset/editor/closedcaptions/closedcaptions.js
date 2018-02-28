$(document).ready(function() 
{
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var themeprefix = app.data("home")	+ app.data("themeprefix");

	var video = jQuery("#videoclip");
	video = video[0]; 
	
	var link = $("#playtab");
	
	var stopplayingat = 8;

	startchunk = function()
	{
		var inTime = video.currentTime;
			    
	    stopplayingat = inTime + 8; 
	    video.play();
	    link.addClass("playing");
	    link.text(link.data("stoptext"));
	    jQuery("#captioninput").focus();
	};
	
	stopchunk = function()
	{
		video.pause();
		link.removeClass("playing");
	    link.text(link.data("playtext"));
	    jQuery("#captioninput").focus();
	};			    

	//Closed caption stuff
	jQuery("#captioninput").livequery( "keydown",function(e) 
	{
			var theinput = jQuery(this);
			
			var keyCode = e.keyCode || e.which; 

			if (keyCode == 9)
			{ 
			 	e.preventDefault(); 
				if( video.paused )
				{
					startchunk();
				}
				else if(link.hasClass("playing") )
				{
				    stopchunk();
				}
				else
				{
					startchunk();
				}
			} 
	});
	jQuery("#addcaption").livequery('submit',function(e)
	{ 
		e.preventDefault();
		$(this).ajaxSubmit({
			target:"#captionview",
			beforeSubmit: function()
			{
				console.log("test",stopplayingat);
				$("#timecodestart").val( stopplayingat - 8);
				$("#timecodelength").val(8);	
			},
			error: function(data)
			{
				$("#captionview").html(data);
			},
			success: function() 
			{
				$("#captiontext").val("");
				stopchunk();
				startchunk();
				console.log("sent");
			}
	 	});
	 	return false;
	});
	
	jQuery("#playtab").livequery("click",function(e)
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
	
	jQuery("#videoclip").on("timeupdate",function(e)
	{
		var link = $("#playtab");
		if( video.paused )
		{
			link.removeClass("playing");
		}
		else if(link.hasClass("playing") )
		{
			 var inTime = video.currentTime;
			 if( inTime > stopplayingat )
			 {
			 	stopchunk();
			 }
		}
	});	
		
});