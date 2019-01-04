var chatconnection;
var open = false;

function chatterbox() {	
	
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	reloadAll();
	
	if(open){
		return;
	}	
	
	connect();
	
	
	
	lQuery(".chatter-send").livequery("click", function(){
		var button = jQuery(this);
		var chatter = button.closest(".chatterbox");
		var data = chatter.data();
	    var content = document.getElementById("chatter-msg").value;
	    data.content = content;
	    data.command= button.data("command");
	    var json = JSON.stringify(data);
	    content.value="";
	    
	    if(chatconnection.readyState === chatconnection.CLOSED  ){
	    	connect();
	    	//IF we do a reconnect render the whole page
	    
	    
	    }
	    var toggle = button.data("toggle");
	    if(toggle == true){
	    	jQuery(".chatter-toggle").toggle();
	    }
	    chatconnection.send(json);
		
	}
	);

	lQuery('.chatter-text').livequery("keyup", function(e){
	    if(e.keyCode == 13)
	    {
	    	
			var button = jQuery('button[data-command="messagereceived"]');		    	
	    	button.trigger("click");
	    	clear();
	    }
	});
	
	open=true;
	
	
}



function clear(){
	jQuery("#chatter-msg").val("");
}


function scrollToChat()
{
	var inside = $('.chatterbox-body-inside');
	if( inside.length > 0 )
	{
		inside.animate({ scrollTop: inside.get(0).scrollHeight}, 500); 
	}
	
}

function connect() {
    var username = "$context.getUserName()";
    
    var tabID = sessionStorage.tabID && sessionStorage.closedLastTab !== '2' ? sessionStorage.tabID : sessionStorage.tabID = Math.random();
    sessionStorage.closedLastTab = '2';
    $(window).on('unload beforeunload', function() {
          sessionStorage.closedLastTab = '1';
    });
    
    var protocol = location.protocol;

    if (protocol === "https:") {
    	chatconnection = new WebSocket("wss://" +location.host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=" + tabID);	
    } else{
    	chatconnection = new WebSocket("ws://" +location.host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=" + tabID );
    }
    
    keepAlive(); 
       
    chatconnection.onmessage = function(event) {
    	
    	var app = jQuery("#application");
    	var apphome = app.data("home") + app.data("apphome");
        var message = JSON.parse(event.data);
        var channel = message.channel;
        var id = message.messageid;
        message.id = id;
        var existing = jQuery("#chatter-message-" + id);
        if(existing.length){
        	return;
        }
		var chatter = jQuery('div[data-channel="' + channel + '"]');		
		var listarea = chatter.find(".chatterbox-message-list")
		var urls =  apphome + "/components/chatterbox/message.html";

		jQuery.get( urls, message, function( data ) {
			listarea.append( data );
			$(document).trigger("domchanged");
		});
	
        scrollToChat();
        play();
    }; 

}


function reloadAll(){
	
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	
	jQuery(".chatterbox").each(function () {
			var urls =  apphome + "/components/chatterbox/index.html";
			var chatterdiv = $(this);
			var mydata = $( this ).data();
			jQuery.get( urls, mydata, function( data ) {
					chatterdiv.html( data );
					scrollToChat();

			});
					
					
	});
	
	
	
	
	
	
	
}



var timerID = 0; 
var wasconnected;

function keepAlive() { 
    var timeout = 20000;  
    if (chatconnection.readyState == chatconnection.OPEN) {  
    	wasconencted = true;
    	chatconnection.send('');  
    }
    
    if (chatconnection.readyState === chatconnection.CLOSED) {  
    	connect();
    	reloadAll();
    }
    
    
    timerId = setTimeout(keepAlive, timeout);  
}  

function cancelKeepAlive() {  
    if (timerId) {  
        clearTimeout(timerId);  
    }  
}

function play(){
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var urls =  apphome + "/components/chatterbox/stairs.wav";

	var snd = new Audio(urls); // buffers automatically when created
	snd.play();
	
	
}








