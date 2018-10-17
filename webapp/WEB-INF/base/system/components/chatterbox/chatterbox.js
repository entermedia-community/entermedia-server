var connection;


function chatterbox() {	
			
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	reloadAll();
	
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
	    
	    if(connection.readyState === connection.CLOSED  ){
	    	connect();
	    	//IF we do a reconnect render the whole page
	    
	    
	    }

	    connection.send(json);
		
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
	

	
	
}



function clear(){
	jQuery("#chatter-msg").val("");
}


function scrollToChat(){
	$('.chatterbox-body-inside').animate({
		scrollTop: $('.chatterbox-body-inside').get(0).scrollHeight}, 2000); 
	
	
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
    	connection = new WebSocket("wss://" +location.host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=" + tabID);	
    } else{
    	connection = new WebSocket("ws://" +location.host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=" + tabID );
    }
    
    keepAlive(); 
       
    connection.onmessage = function(event) {
    	
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
		});
	
        scrollToChat();
        
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
    if (connection.readyState == connection.OPEN) {  
    	wasconencted = true;
    	connection.send('');  
    }
    
    if (connection.readyState === connection.CLOSED) {  
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







