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
	//jAngular.addScope("annoscope",scope);
	
	editor.loadModels();
	
	lQuery("#annotation-toolbar li").livequery('click', function()
	{
		var id = $(this).attr("id");
		if( id == "movetool")
		{
			editor.fabricModel.selectTool('move');
		}
		else if( id == "drawtool")
		{
			editor.fabricModel.selectTool('draw');
		}
		else if( id == "rectangletool")
		{
			editor.fabricModel.setShapeTypeFromUi('rectangle');
		}
		else if( id == "circletool")
		{
			editor.fabricModel.setShapeTypeFromUi('circle');
		}
		else if( id == "colortoolbararea")
		{
			editor.colorPicker.colorpicker('open');
		}
		else if( id == "deletetool")
		{
			editor.deleteAnnotations();
		}
		
	});

});
