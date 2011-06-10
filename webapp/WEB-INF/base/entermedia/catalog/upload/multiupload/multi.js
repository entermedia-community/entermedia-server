setUploadPath = function(inFilePath)
{
	//using the number of 'localfilepath's to count the current upload number
	var files = document.getElementsByName("localFilePath").length;
	displayNewFilePath(inFilePath, files);
	var field = document.getElementById("localFilePath" + files);
	field.value = inFilePath;
	return false;
}

displayNewFilePath = function(inFilePath, inCount)
{
	var newField = "<input type='hidden' name='localFilePath' id='localFilePath" + inCount + "' />";
	jQuery("#uploadforminsertionpoint").before(newField);
}

//Applet
uploadstatus = function( inId, inPath, inSentSoFar, inTotal, inSpeed, inCount)
{
   	var content = createUpdate(inId,inPath, inSentSoFar, inTotal, inSpeed, inCount);
	jQuery("#uploadprogressbar"+inCount).html(content);
}

uploadfinished = function(inId, inPath, inCount)
{
	jQuery(document).ready( function()
	{
		//remove the applet
		var applet = document.getElementById("uploadapplet" + inCount);
		applet.parentNode.removeChild(applet);
		
		var pathfield = document.getElementById('uploadpath'+ inCount );
		pathfield.value = inPath;
		var filename = inPath;
		if(filename.indexOf('/') > 0)
		{
			filename = filename.substring(filename.lastIndexOf('/')+1);
		}
		var filenamefield = document.getElementById('filename' + inCount);
		filenamefield.value = filename;
		filenamefield = document.getElementById('imagefilename' + inCount);
		filenamefield.value = filename;
		jQuery('#imagefinishform' + inCount).submit();
		checkAllUploads();
	});
}


createUpdate = function(inId,inPath, inSentSoFar, inTotal, inSpeed, inCount)
{
	var row = "";
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
	row += "Upload Status: " + text + "% <div style='border: 1px solid black; width:100%'><div style='background-color: #5c94e0;width:" + percent +"%; height:25px;' ></div></div>";

	return row;
}
