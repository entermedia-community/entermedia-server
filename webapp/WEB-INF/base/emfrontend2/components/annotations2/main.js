//jAnqular controller

lQuery("#annotation-holder").livequery(function() 
{ 
	var scope = new Scope();
	scope.add("app", jQuery("#application") );
	scope.add("home" ,scope.app.data("home") );
	scope.add("apphome" , scope.app.data("apphome") );
	scope.add("dbhome" , "/" + scope.app.data("mediadbappid") );

	scope.add("componentroot" ,scope.app.data("home") );
//	scope.add("collectionid", $("#collectiontoplevel").data("collectionid").toString() );
	scope.add("catalogid" ,$("#annotationarea").data("catalogid"));	
	scope.add("assetid" ,$("#annotationarea").data("assetid"));	
	
	var editor = new AnnotationEditor(scope);
	scope.add("annotationEditor",editor);
	jAngular.addScope("annoscope",scope);
	
	editor.loadModels();

});
