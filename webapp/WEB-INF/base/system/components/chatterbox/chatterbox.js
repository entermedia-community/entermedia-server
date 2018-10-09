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
}


var chatter;

function connect() {
    var username = "$context.getUserName()";
    
    var host = "localhost:8080";
    
    chatter = new WebSocket("ws://" +host  +  "/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection");
    chatter.onmessage = function(event) {
    var log = document.getElementById("log");
        console.log(event.data);
        var message = JSON.parse(event.data);
        log.innerHTML += message.from + " : " + message.content + "\n";
    };
    
    
}

function send() {
    var content = document.getElementById("msg").value;
    var json = JSON.stringify({
        "content":content
    });

    chatter.send(json);
}





$(document).ready(function() {
					

	
	
					var app = jQuery("#application");
					var apphome = app.data("home") + app.data("apphome");

					

				
					
					
					
});