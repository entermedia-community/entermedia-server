
(function($){
 	$.fn.extend({ 
		//pass the options variable to the function
 		entermedia: function(options) {

			var emdefaults = {
				action : "listcatalogs",
				appid: null,
				catalogid: "/media/catalogs/public",
				categoryid : null,
				searchtype: "asset",
				key : '#000000',
				thecanvas  : this,
				datahandler: null,
				homeid: ""
			};

			//Set the default values, use comma to separate the settings, example:
			var options =  $.extend(emdefaults, options);

			//TODO: Should we search for more than one?
    		return this.each(function() {
				var o = options;
				var path = appHome(o) + "/services/rest/" + o.action +".xml";
				getData(path, o);
    		});
    	}
	});
})(jQuery);

getData = function( inUrl, o )
{
	return jQuery.ajax(
	{
		  type: "GET",
		  url: inUrl,
		  async: false,
		  dataType: "xml",
		  data: createParamsObject(o),
		  success: function(data)
		  {
				var parsed = jQuery(data);
				if( o.datahandler )
				{
					o.datahandler(parsed, o.thecanvas);
				}
				else
				{
					listifyResult(parsed, o);
				}
	   	  },
	   	  error: function (XMLHttpRequest, textStatus, errorThrown) 
	   	  {
			  // typically only one of textStatus or errorThrown 
			  // will have info
			  alert("Error: " + textStatus + " " + this.url );
		  }	  
	});
}

appHome = function(inOptions) {
	if(inOptions.homeid !== undefined && inOptions.homeid.length > 0) {
		return "/" + inOptions.homeid + "/" + inOptions.appid;
	}
	return "/" + inOptions.appid;
}

catalogHome = function(inOptions) {
	if(inOptions.homeid !== undefined && inOptions.homeid.length > 0) {
		return "/" + inOptions.homeid + inOptions.catalogid;
	}
	return inOptions.catalogid;
}

listifyResult = function(inData, inOptions)
{
	var canvas = inOptions.thecanvas;
	var list = $('<ul style="list-style-type: none;">').appendTo(canvas);
	if(inOptions.action == "search")
	{
		return listifySearch(inData, list, inOptions);
	}
	else if(inOptions.action == "listcatalogs")
	{
		return listifyListCatalogs(inData, list, inOptions);
	}
	list.append("</ul>");
}

listifySearch = function(inData, inList, inOptions)
{
	inData.find("hit").each(function() 
	{
		var id = this.getAttribute("id");
		var text = this.firstChild.nodeValue;
						
		var imageUrl = catalogHome(inOptions) + '/downloads/preview/thumb/' + this.getAttribute("sourcepath") + '/thumb.jpg';
		var largeUrl = catalogHome(inOptions) + '/downloads/preview/large/' + this.getAttribute("sourcepath") + '/thumb.jpg';
		inList.append('<li><a href="' + largeUrl + '">' + '<img src="' + imageUrl + '"/></a> ' + text + '</li>');
	});
}

listifyListCatalogs = function(inData, inList, inOptions)
{
	inData.find("catalog").each(
		function() {
			inList.append('<li id="' + this.getAttribute("id") + '">' + this.firstChild.nodeValue + '</li>');
		}
	);
}

createParamsObject = function(inOptions)
{
	//decide what to send as data
	var params = {};
	if(inOptions.action == "search")
	{
		params.searchtype = inOptions.searchtype;
		params.catalogid = inOptions.catalogid;
	}
	return params; 
}
