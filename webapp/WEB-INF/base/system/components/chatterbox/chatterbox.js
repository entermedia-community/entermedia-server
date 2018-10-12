var connection;


function chatterbox() {	
			
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	jQuery(".chatterbox").each(function () {
						var urls =  apphome + "/components/chatterbox/index.html";
						var chatterdiv = $(this);
						var mydata = $( this ).data();
						jQuery.get( urls, mydata, function( data ) {
							chatterdiv.html( data );
						});
					
					
					});
	connect();
	
	lQuery(".chatter-send").livequery("click", function(){
		var button = jQuery(this);
		var chatter = button.closest(".chatterbox");
		var data = chatter.data();
	    var content = document.getElementById("msg").value;
	    data.content = content;
	    data.command= button.data("command");
	    var json = JSON.stringify(data);
	    content.value="";
	    
	    connection.send(json);
		
	}
	);

	$('.chatter-text').keyup(function(e){
	    if(e.keyCode == 13)
	    {
	    	
			var button = jQuery('div[data-command="messagereceived"]');		    	
	    	button.trigger("click");
	    }
	});
}



function connect() {
    var username = "$context.getUserName()";
    
    var protocol = location.protocol;

    if (protocol === "https:") {
    	connection = new WebSocket("wss://" +location.host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection");	
    } else{
    	connection = new WebSocket("ws://" +location.host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection");
    }
    
    
    connection.onmessage = function(event) {
    	
    	var app = jQuery("#application");
    	var apphome = app.data("home") + app.data("apphome");
        
        var message = JSON.parse(event.data);
        var channel = message.channel;
        var id = message.messageid;
        message.id = id;
		var chatter = jQuery('div[data-channel="' + channel + '"]');		
		var listarea = chatter.find(".chatterbox-message-list")
		var urls =  apphome + "/components/chatterbox/message.html";

		jQuery.get( urls, message, function( data ) {
			listarea.append( data );
		});
	
        
        
    };
    
    
}


