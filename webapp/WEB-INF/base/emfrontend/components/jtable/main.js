//jAnqular controller
jQuery(document).ready(function() 
{ 
	var component = $("#tablecontroller");

	var scope = new Scope();
	scope.add("app", jQuery("#application") );
	scope.add("home" ,scope.app.data("home") );
	scope.add("apphome" , scope.app.data("apphome") );
	scope.add("component" , component);
	scope.add("searchtype", component.data("searchtype") );
	scope.add("catalogid", component.data("catalogid") );
	jAngular.addScope("jtablescope",scope);
	
	var tablecontroller = new TableController(scope);
	tablecontroller.loadSelectors();
	tablecontroller.loadData();

});
