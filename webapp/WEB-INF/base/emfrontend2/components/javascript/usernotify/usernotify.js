/*
 * 
 * (function($){
	
     $.fn.socketMessage = function(data) 
     {
        return this.each(function()
		{
            var div = $(this);
 			div.html("we got this: " + data);
        });
     };
})(jQuery);
*/

usernotify = function () 
{	
	var usernotifyconnection;
	var usernotifyopen = false;
	var timerID = 0; 
	var wasconnected = false;
    var timeout = 1000;  

	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	if(usernotifyopen)
	{
		return;
	}	
	getCookieValue = function(apphome) 
	{
		var second = apphome.indexOf("/",2);	
		var root = apphome.substring(1,second);
		var key = "entermedia.key" + root;
	    var b = document.cookie.match('(^|;)\\s*' + key + '\\s*=\\s*([^;]+)');
	    return b ? b.pop() : '';
		
	}
	connect = function()
	{
    
	    var tabID = sessionStorage.tabID && sessionStorage.closedLastTab !== '2' ? sessionStorage.tabID : sessionStorage.tabID = Math.random();
	    sessionStorage.closedLastTab = '2';
	    $(window).on('unload beforeunload', function() {
	          sessionStorage.closedLastTab = '1';
	    });
	
		var app = jQuery("#application");
		var userid = app.data("user");
	    var protocol = location.protocol;
	
	    var url = "/entermedia/services/websocket/org/entermediadb/websocket/usernotify/UserNotifyConnection?sessionid=" + tabID + "&userid=" + userid;
	    if (protocol === "https:") {
	    	usernotifyconnection = new WebSocket("wss://" +location.host + url );	
	    } else{
	    	usernotifyconnection = new WebSocket("ws://" +location.host  + url );
	    }
	    
	    usernotifyconnection.onmessage = function(event) 
	    {
	    	var apphome = app.data("home") + app.data("apphome");
	    	
	        var message = JSON.parse(event.data);
	        console.log("receipbed = " , message);
	        if( message.command === "uireload")
	        {
		        var targetdiv = message.targetdiv;
		        console.log("got a message" + targetdiv);
		        alert(message);
		    	jQuery("."  +targetdiv ).each(function () 
		 		{
		    		var div = $(this);
		    		//div.socketMessage(message);
		    		div.html("got"  + message.html); //Or url load
		 		});
	        }	
	        else if( message.command === "authenticated")
	        {
	        	//make sure UI is uptodate?
	        	/*
	        	jQuery(".usernotify" ).each(function () 
	    		{
		    		var div = $(this);
		    		var url = div.data("reloadurl");
		    		div.load(url); 
		 		});
		 		*/
	        }
	    }; 

	    keepAlive(); 
       
	}
	keepAlive = function()
	{ 
	    if (usernotifyconnection.readyState == usernotifyconnection.OPEN) {  
	    	var command = new Object();
	    	if( wasconnected )
	    	{
		    	command.command = "keepalive";	    		
	    	}
	    	else
	    	{
		    	command.command = "login";
		    	timeout = 20000;
	    	}
	    	
	    	wasconnected = true;
	    	//var userid = jQuery(".chatterbox").data("user"); //TODO: Use app?
			var userid = app.data("user");
	    	command.userid =  userid;
	    	var appid = app.data("apphome");
	    	command.entermediakey = getCookieValue(appid);
	    	var json = JSON.stringify(command);
	    	usernotifyconnection.send(json);  
	    }
	    
	    if (usernotifyconnection.readyState === usernotifyconnection.CLOSED) {  
	    	connect();
	    	reloadAll();
	    }
	  
	    timerId = setTimeout(keepAlive, timeout);  
	}  

	cancelKeepAlive = function()
	{  
	    if (timerId) {  
	        clearTimeout(timerId);  
	    }  
	}
	
	connect();
	usernotifyopen=true;
}

$(document).ready(function() 
{ 
		usernotify();
});