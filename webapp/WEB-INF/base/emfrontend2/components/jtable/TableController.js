//Controller
var TableController = function(scope) {
	
	var out = {
		scope : scope,
		userData: null,
		loadSelectors : function()
		{
			var controller = this;
			$("#annotation-list .comment").livequery(function()
			{
				var div = $(this);
				if( div.data("author") != editor.userData.id)
				{
					//hide edit buttons
					div.find("button").hide();
				}
			});
		}
		,
		loadData : function()
		{
			var controller = this;
			
			// get user data, should this be in connect?
			$.getJSON('/entermedia/services/json/users/status.json', function(data) {
				controller.userData = data;
			});
		}
	};
	scope.add("tablecontroller",out);

	return out; 
}
