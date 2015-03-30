//jAnqular controller

jQuery(document).ready(function() 
{ 
	var scope = new Scope();
	scope.add("app", jQuery("#application") );
	scope.add("home" ,scope.app.data("home") );
	scope.add("apphome" , scope.app.data("apphome") );
	scope.add("dbhome" , "/" + scope.app.data("mediadbappid") );

	scope.add("componentroot" ,scope.app.data("home") );
	scope.add("collectionid", $("#collectiontoplevel").data("collectionid").toString() );
	scope.add("catalogid" ,$("#collectiontoplevel").data("catalogid"));	
	var editor = new AnnotationEditor(scope);
	scope.add("annotationEditor",editor);
	jAngular.addScope("annoscope",scope);
	
	editor.loadModels();
	editor.loadSelectors();

});
