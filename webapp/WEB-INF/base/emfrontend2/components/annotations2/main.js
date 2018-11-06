//jAnqular controller

lQuery("#annotation-holder").livequery(function() 
{ 
	var scope = new Scope();
	scope.add("app", jQuery("#application") );
	scope.add("home" ,scope.app.data("home") );
	scope.add("apphome" , scope.app.data("apphome") );
	scope.add("dbhome" , "/" + scope.app.data("mediadbappid") );

	var area = $("#annotationarea");
	scope.add("componentroot" ,scope.app.data("home") );
	scope.add("catalogid" ,area.data("catalogid"));	
	scope.add("assetid" ,area.data("assetid"));	
	scope.add("userid" ,area.data("userid"));	
	scope.add("userColor" ,area.data("usercolor"));
	
	var editor = new AnnotationEditor(scope);
	scope.add("annotationEditor",editor);
	jAngular.addScope("annoscope",scope);
	
	editor.loadModels();
	
	$("#annotation-toolbar li").livequery

});
