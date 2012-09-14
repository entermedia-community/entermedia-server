    
$(document).ready(function(){
    $('#filePicker').click(function(e){
       $('#fileElem').trigger('click');
       e.preventDefault(); 
    });
    /*
    var dropbox = document.getElementById("emdropbox"); 
    dropbox.addEventListener("dragenter", function(e) {
    	  e.stopPropagation();
    	  e.preventDefault();
    	} , false);
    dropbox.addEventListener("dragover",  function(e) {
  	  	e.stopPropagation();
  	  	e.preventDefault();
	} , false);

    dropbox.addEventListener("drop", function(e) {
        e.stopPropagation();
        e.preventDefault();
       
        var dt = e.dataTransfer;
        var files = dt.files;
       
       // jQuery("#fileElem").files = files;
       // document.getElementById("fileElem").files = files;
        handleFiles(files);
     }, false);
     */
})

var uploadid = new Date().getTime(); 

function handleFiles(files) 
{
  var home = jQuery("#application").data("home") + jQuery("#application").data("apphome"); 
	
  for (var i = 0; i < files.length; i++) 
  {
    var file = files[i];
    
	try 
	{
		uploadid++;
		var link = home + "/components/upload/types/html5/uploadrow.html?uploadid=" + uploadid + "&name=" + file.name + "&size=" + file.size + "&fileindex=" + i;
		jQuery.ajax(
		{
		   type: "POST",
		   url: link,
		   async: false,
		   success: function(html)
		   {
				jQuery("#up-files-list").append(html);
		   }
		 });
		
//		var action = jQuery("#uploadform").attr("action");
//		jQuery("#uploadform").attr("action", action + " );

		
	}
	catch (ex) 
	{
		alert( ex);
	}
	if( jQuery("#bulkuploader").length > 0)
	{
	}
	else
	{
		jQuery("#metadataarea").load(home + '/components/upload/types/html5/bulkloader.html');
	}
  }
}

var uploadid;

	doUpload =  function()
	{
		if(jQuery("#uploadform").valid()) {
	 		jQuery("#finishbutton").attr('disabled','disabled');
	 		jQuery("#uploadform").submit();
		}
	}
	
	getUploadId = function()
	{
		return jQuery("#uploadform").data("uploadid");
	}
	
	checkProgress = function()
	{
		var home = jQuery("#application").data("home") + jQuery("#application").data("apphome"); 

		var next = jQuery("#up-files-list").find("[data-complete='false']").first()
		if( next.length > 0 )
		{
			var complete = next.data("complete");
			var name = next.data("name");
			var size = next.data("size");
			var fileindex = next.data("fileindex");
			if( complete != "true")
			{
				var link = home + "/components/upload/types/html5/uploadprogress.html?uploadid="+ getUploadId() + "&name=" + name +"&size=" + size + "&fileindex=" + fileindex;
				jQuery.get(link, {}, function(data) 
					{
						next.replaceWith(data);
					}
				);
			}
			setTimeout("checkProgress()",500);
		}
	}
	
	function showResponse(responseText, statusText, xhr, $form)  
	{ 
		//jQuery.fn.livequery.stopped = true;
		//jQuery("#embody").html(responseText);	
		//jQuery.fn.livequery.stopped = false;
		var home = jQuery("#application").data("home") + jQuery("#application").data("apphome"); 
		jQuery("#uploadarea").html(responseText);	
			 
		//document.location.href = home + "/views/search/reports/runsavedsearch.html?queryid=01newlyuploaded&searchtype=asset&reporttype=01newlyuploaded";
		//  alert("upload done");
	}
	
	//special validator for file name
	jQuery.validator.addMethod("filename",
			function(value) {
				var characterReg = /^\s*[a-zA-Z0-9\._,\s]+\s*$/;
	   			return characterReg.test(value);
			}, 
			'Invalid file name.'
	);
	
// wait for the DOM to be loaded 
$(document).ready(function() 
{	
    // bind 'myForm' and provide a simple callback function 
    $('#uploadform').ajaxForm({ 
    	        // target identifies the element(s) to update with the server response 
    	       // target: '#emcontainer', 
    	        // success identifies the function to invoke when the server response 
    	        // has been received; here we apply a fade-in effect to the new content
    	        beforeSubmit:function() 
    	        { 
    	        	
    	            checkProgress(); 
    	           // document.onclick = disable;
    	            jQuery("#finishbutton").attr("value","Sending...");
    	           
    	        }, 
    	        success: showResponse
    	        
    	    }); 
}); 
	

