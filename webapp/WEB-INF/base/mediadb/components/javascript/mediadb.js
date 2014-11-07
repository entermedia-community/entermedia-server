//Deal with networking code

var testhome = "http://demo.entermediasoftware.com";

// Query class from em-ratchet, should live here
var Query = function() {
    var out = { 
        field : "description",
        operator: "contains",
        values: null 
    };
    return out;
}


/*
testing using old xml-based API calls, new calls will use mediaDbUrl/services/target
*/

var MediaDb = function()
{
	var out = {
		mediaDbUrl: '/mediadb',
		entermediakey: "webusermd5420a1e7019490f00b90913b280ce20500ef0e23",
		templates: ['document', 'image', 'video', 'embedded', 'audio']
	};
	
	out.setMediaDbUrl = function (inUrl)
	{
		this.mediaDbUrl = inUrl;
	};

	out.getMediaDbUrl = function ()
	{
		return this.mediaDbUrl;
	};

	out.getObject = function (url)
	{
		var self = this;
		var outData = null;
		$.ajax({
			type: "GET",
			url: url,
			async: false,
			error: function(data, status, err)
				{
					console.log(this.url);
					console.log('from error:', data);
				},
			success: function(data)
				{
					//data is library stuff?
					// need to do some parsing?
					console.log('from success:', data);
					outData = data;
				},
			failure: function(errMsg)
				{
					console.log('from failure:', errMsg);
				}
			});
		return outData;
	};

	out.getContent = function (url)
	{
		// not quite sure about this one yet
		// wouldn't this be more like a template name or a datatype than a url? i.e. below:
	};

	out.getTemplate = function (template)
	{
		// assuming template is 'document', 'audio', 'video', 'embedded', or 'image', if not use 'default'
		if (this.templates.indexOf(template) == -1)
		{
			template = 'default';
		}
		// does this need user authentication and catalogid?
		// will this even return the HTML content or just a messed up request?

		return this.getObject(testhome + "/entermedia/components/mediaviewer/player/" + template);
	};

	out.getLibraries = function ()
	{
		// need special parsing?
		return this.getObject(testhome + "/" + this.mediaDbUrl + "/services/rest/search.xml?entermedia.key="+ self.getuserKey() +"&catalogid=media/catalogs/public&searchtype=library");
	};
	
	out.getCollections = function (libraryid)
	{
		return this.getObject(testhome + "/" + this.mediaDbUrl + "/services/rest/search.xml?entermedia.key="+ self.getUserKey() +"&catalogid=media/catalogs/public&searchtype=librarycollection&field=library&operation=matches&library.value=" + libraryid);
	};
	
	out.getCollectionAssets = function (collectionid)
	{
		return this.getObject(testhome + "/" + this.mediaDbUrl + "/services/rest/search.xml?entermedia.key="+ self.getuserKey() +"&catalogid=media/catalogs/public&searchtype=librarycollectionasset&field=librarycollection&operation=exact&librarycollection.value=" + collectionid);
	};

	out.searchAssets = function (query)
	{
		var outResult = null;
		var self = this;
		// need to adjust synchrony?
        $.ajax({
              contentType: 'text/plain',
              type: 'POST',
              //processData: false,
              url: testhome + "/" + self.mediaDbUrl + "/services/search/data/asset?&catalogid=media/catalogs/public",
              data: '{ "entermediakey: "' + self.getUserKey() + '" ,"query" : [' + JSON.stringify(query) + '] }',
              success: 
                    function(outData)
                    {
                        outResult = JSON.parse(outData).results;
                    },
              failure:
              		function(outData)
              		{
              			console.log("FAILURE ON AJAX REQUEST: ", outData);
              		}
        });
        return outResult;
	};
	
	out.searchTables = function (tablename, inQuery)
	{
		var outResult = null;
		var inData = {
			entermediakey: this.getUserKey(),
			searchtype: tablename,
			query: [inQuery]
		};
		$.ajax({
            contentType: 'text/plain',
            type: 'POST',
            //processData: false,
            url: testhome + "/" + this.mediaDbUrl + "/services/search/data/asset?catalogid=media/catalogs/public",
            data: inQuery,
            success: 
                function(outData)
                {
                    outResult = JSON.parse(outData).results;
                },
            failure:
            	function(outData)
            	{
              		console.log("FAILURE ON AJAX REQUEST: ", outData);
              	}
        });
        return outResult;
	};
	
	out.saveData = function (tablename, inData)
	{
		var self = this;
		$.ajax({
            contentType: 'text/plain',
            type: 'POST',
            //processData: false,
            url: testhome + '/entermedia/catalog/data/fields/' + tablename + '?entermedia.key=' + self.getUserKey() + '&catalogid=media/catalogs/public',
            data: inData,
            success: 
                function()
                {
                    console.log("data saved to " + tablename)
                },
            failure:
            	function(outErr)
            	{
              		console.log("FAILURE ON SAVE: ", outErr);
              	}
        });
        return outResult;	
	};
	out.setUserKey = function(inKey)
	{
		this.entermediakey = inKey;
	};
	out.getUserKey = function()
	{
		return this.entermediakey;
	};
	return out;

}

//Asset


//Query

