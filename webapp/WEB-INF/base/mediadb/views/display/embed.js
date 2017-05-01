//Depends on jQuery being available
if (window.jQuery) 
{  
       // jQuery is loaded  
} else {
      alert("jQuery is required");
}

var script = document.currentScript;

var server =  script.getAttribute("src"); 
server = server.substring(0,server.length - 22);
console.log(server);
var collectionid = script.getAttribute('collectionid'); 
if( collectionid )
{
  $(window).load(function() 
  {
	var targetdiv = script.getAttribute('targetdiv');
	if(!targetdiv)
	{
		targetdiv = "body";
	}
	else
	{
		targetdiv = "#"  +targetdiv;
	}
	var width = script.getAttribute('width');
	if(!width)
	{
		width = "100%";
	}
	var height = script.getAttribute('height');
	if(!height)
	{
		height = "100vh";
	}


    var f = document.createElement('iframe');
    f.src = server + "/services/module/asset/players/collections/display/" + collectionid + ".html"; 
    f = $(f);
    f.css("width", width); 
    f.css("height", height); 
    $(targetdiv).append(f);
  });
}