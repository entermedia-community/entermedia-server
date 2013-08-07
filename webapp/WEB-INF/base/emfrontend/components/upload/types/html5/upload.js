    
var uploadid = new Date().getTime(); 
var home = null;
function handleFiles(files) 
{
	
  for (var i = 0; i < files.length; i++) 
  {
    var file = files[i];
    
	try 
	{
		uploadid++;
		
		//replace with jQuery based uploading. Disable the buttons
		jQuery("#uploadinstructions").hide();
		
		var link = home + "/components/upload/types/html5/uploadrow.html?uploadid=" + uploadid + "&name=" + file.name + "&size=" + file.size + "&fileindex=" + i;
		jQuery.ajax(
		{
		   type: "POST",
		   url: link,
		   async: false,
		   success: function(html)
		   {
				jQuery("#up-files-list").append(html);
				
				//start up the XHD data transfer and progress bar, thumbnail?
		   }
		 });
		
//		var action = jQuery("#uploadform").attr("action");
//		jQuery("#uploadform").attr("action", action + " );

		
	}
	catch (ex) 
	{
		alert( ex);
	}
	jQuery("#uploadinstructionsafter").show();
	
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
/*	
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
*/	
	function showResponse(responseText, statusText, xhr, $form)  
	{ 
		//jQuery.fn.livequery.stopped = true;
		//jQuery("#embody").html(responseText);	
		//jQuery.fn.livequery.stopped = false;
		
		
		/*
		    var home = jQuery("#application").data("home") + jQuery("#application").data("apphome"); 
			jQuery("#uploadarea").html(responseText);
			jQuery("#uploadarea").attr('id', 'view-picker-content');
	        jQuery("#view-picker-content").addClass('liquid-sizer');
	        doResize();
	    */    

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
	
	var currentupload = 0;
	var haderror = false;
// wait for the DOM to be loaded 
$(document).ready(function() 
{	
	home = jQuery("#application").data("home") + jQuery("#application").data("apphome"); 
    $('#filePicker').click(function(e){
        $('#upload_field').trigger('click');
        e.preventDefault(); 
     });

    jQuery("#startbutton").livequery('click',function() 
    {
    	var valid = $("#uploaddata").validate().form();
    	if(!valid){
    		return;
    	}
    	$(this).text("Uploading");
    	$(this).attr('disabled', 'disabled');
    	//jQuery("#upload_field").upload();
    	jQuery("#upload_field").triggerHandler("html5_upload.start");
    	
    });
    
	jQuery("#upload_field").livequery( function() 
	{
		var inputfield = $(this);
	
		 inputfield.html5_upload(
		   {
	         filesPicked: function(event, files) 
	         {
	        	 jQuery("#uploadinstructionsafter").show();
	        	 
	        	 var regex =new RegExp("currentupload", 'g');  
	        	 
        	    $("#up-files-list").empty();
	             //return confirm("You are trying to upload " + total + " files. Are you sure?");
	        	 for (var i = 0; i < files.length; i++) 
	        	 {
	        	    var file = files[i];
	        	    
	        	    var html = $("#progress_report_template").html();
	        	    
	        	    html = html.replace(regex,i);
	        	    $("#up-files-list").append(html);
	        	    
	        	    //TODO: set the name and size of each row
	        	    $("#progress_report_name" + i).text(file.name);
	        	    var size = bytesToSize(file.size,2);
	        	    $("#progress_report_size" + i).text(size);
	        	 }
	         },
	         url: function(number) {
	       		var url =  $("#uploaddata").attr("action");
	       		
	       		var str = $("#uploaddata").serialize();
	       		return url + "?" + str;
	            // return prompt(number + " url", "/");
	         },
	         autostart:false,
//	         extraFields: function() {
//	        	 return [];
//	         },         
	         sendBoundary: window.FormData || $.browser.mozilla,
	         onStart: function(event, total, files) 
	         {
	        	 jQuery("#uploadinstructions").hide();
	             return true;
	        	 //Loop over all the files. add rows
	        	 //alert("start");
	         },
	         onStartOne: function(event, name, number, total) {
	        	 //Set the currrent upload number?
	        	 currentupload = number;
	        	 return true;
	         },
	         onProgress: function(event, progress, name, number, total) {
	            // console.log(progress, number);
	         },
	//         genName: function(file, number, total) {
	//             return file;
	//         },
	//         setName: function(text) {
	//             $("#progress_report_name" + currentupload).text(text);
	//         },
	         setStatus: function(text) {
	        	 if( text == "Progress")
	        	 {
	        		 text = "Uploading";
	        	 }
	             $("#progress_report_status" + currentupload).text(text);
	         },
	         setProgress: function(val) {
	             $("#progress_report_bar" + currentupload).css('width', Math.ceil(val*100)+"%");
	         },
	         onFinishOne: function(event, response, name, number, total) {
	             //alert(response);
	             $("#progress_report_bar" + currentupload).css('width', "100%");
	             //$("#progress_report_bar" + currentupload).css('background-color', "green");
	         },
	         onError: function(event, name, error) {
	             alert('error while uploading file ' + name);
	             haderror =true;
	         },
	         onFinish: function(event, total) {
	             //do a search
	        	 if( !haderror)
	        	{
//	        		 document.location.href = home + "/views/search/reports/runsavedsearch.html?queryid=01newlyuploaded&searchtype=asset&reporttype=01newlyuploaded";
//	        		 document.location.href = home + "/views/myaccount/myassets/index.html";
	        		 
	        		 var nexturl = "/views/myaccount/myassets/index.html";
	        		 var uploadarea = $("#uploadarea");
	        		 if (uploadarea!=null && uploadarea!=undefined){
	        			 if (uploadarea.data("nextpage")!=null && uploadarea.data("nextpage")!=undefined && uploadarea.data("nextpage")!="default"){
	        				 nexturl = uploadarea.data("nextpage");
	        			 }
	        		 }
	        		 document.location.href = home + nexturl;
	        	}
	
	         }
	     });
	});
	
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
	

function bytesToSize(bytes, precision)
{  
    var kilobyte = 1024;
    var megabyte = kilobyte * 1024;
    var gigabyte = megabyte * 1024;
    var terabyte = gigabyte * 1024;
   
    if ((bytes >= 0) && (bytes < kilobyte)) {
        return bytes + ' B';
 
    } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
        return (bytes / kilobyte).toFixed(precision) + ' KB';
 
    } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
        return (bytes / megabyte).toFixed(precision) + ' MB';
 
    } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
        return (bytes / gigabyte).toFixed(precision) + ' GB';
 
    } else if (bytes >= terabyte) {
        return (bytes / terabyte).toFixed(precision) + ' TB';
 
    } else {
        return bytes + ' B';
    }
}
