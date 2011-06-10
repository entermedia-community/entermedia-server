/**
 * Navigate to a page, perform a target action with the given form.
 */
function gotoPage( url, target, inForm )
{
	
	if ( inForm == null )
	{
		inForm = document.wizard;
	}
	
	// Should perform any validation that we need to do here
	/* Determine the file type of the file to create based upon the extension. 
	   Even if we're dealing with an XML file, we want to point the browser to
	   the base filename with an HTML extension.
	*/
	/*  Don't remember what this was used for, but it causes several bugs in the 
	    filemanager, specifically the "move" and "copy" commands were broken.
	    
	if ( inForm.destinationPath )
	{
		if ( isXMLFile(inForm.destinationPath.value) )
	  	{
	  		var extensionIndex = inForm.destinationPath.value.indexOf(".xml");
	  		inForm.sourcePath.value = "/openedit/styles/subpages/blankxml.xml";
	  		var baseFileName = inForm.destinationPath.value.substring( 0, extensionIndex );
		  	inForm.destinationPath.value = baseFileName + ".html";
	  	}
	  	else if ( isHTMLFile(inForm.destinationPath.value) )
	  	{
	  		inForm.sourcePath.value = "/openedit/styles/subpages/blankhtml.html";
	  	}
  	}
  	*/
  	inForm.action = url;
  	if ( target )
  	{
    	inForm.target = target;
  	}
  	else
  	{
  		inForm.target = "_self";
  	}
  	inForm.submit();
}

function openHelp( url )
{
	window.open(url, 'helpwindow','scrollbars=1,alwaysRaised=yes,menubar=no,resizable=yes,x=100,y=100,width=950,height=650' );
}

function isXMLFile( filename )
{
	return ( filename.indexOf(".xml") > -1 );
}

function isHTMLFile( filename )
{
	return ( filename.indexOf(".html") > -1 );
}

function getElement( inId )
{
	return document.getElementById( inId );
}

function skipAction( inId )
{
	getElement( inId ).name="no-op";
}