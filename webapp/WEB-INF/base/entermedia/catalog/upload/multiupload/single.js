setUploadPath = function(inFilePath)
{
	var field = document.getElementById("localFilePath");
	field.value = inFilePath;
	return false;
}

//Applet
uploadstatus = function( inId, inPath, inSentSoFar, inTotal, inSpeed)
{
   	var content = createUpdate(inId,inPath, inSentSoFar, inTotal, inSpeed);
	jQuery("#uploadprogressbar").html(content);
}

uploadfinished = function(inId, inPath)
{
	//remove the applet
	var applet = document.getElementById("uploadapplet");
	applet.parentNode.removeChild(applet);
	
	var pathfield = document.getElementById('uploadpath');
	pathfield.value = inPath;
	var filename = inPath;
	if(filename.indexOf('/') > 0)
	{
		filename = filename.substring(filename.lastIndexOf('/')+1);
	}
	var filenamefield = document.getElementById('filename');
	filenamefield.value = filename;
	filenamefield = document.getElementById('imagefilename');
	filenamefield.value = filename;
	jQuery('#finishform').submit();
}


createUpdate = function(inId,inPath, inSentSoFar, inTotal, inSpeed)
{
	var row = "<div style='font-weight: normal' id='uploadprogressbar'>";
	var lastSlash = inPath.lastIndexOf('/');
	var name;
	if (lastSlash > 0)
		name = inPath.substring(lastSlash + 1);
	else
		name = inPath;
	
	var sent = parseInt( inSentSoFar );
	var total = parseInt( inTotal );
	var percent = (100 * sent) / total;
	var text = '' + percent;
	if( text.indexOf('.') > 0 )
	{
		text = text.substring(0, text.indexOf('.'));
	}
	row += "Upload Status: " + text + "% <div style='border: 1px solid black; width:100%'><div style='background-color: #5c94e0;width:" + percent +"%; height:25px;' ></div></div></td>";

	return row;
}

