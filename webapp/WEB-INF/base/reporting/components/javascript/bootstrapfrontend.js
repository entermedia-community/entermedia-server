$(document).ready(function() {
					
	
	
	
					var app = jQuery("#application");
					var apphome = app.data("home") + app.data("apphome");
					var themeprefix = app.data("home")
							+ app.data("themeprefix");
							
						jQuery.validator.addMethod("passwordsmatch", function (value, element) {
							return $("#password").val() == $("#password2").val();
						}, "Passwords do not match");
						
						
						jQuery.validator.addMethod("cdnPostal", function(postal, element) {
						    return this.optional(element) || 
						    postal.match(/[a-zA-Z][0-9][a-zA-Z](-| |)[0-9][a-zA-Z][0-9]/);
						}, "Please specify a valid postal code.");
					
						
						$('#editmodal').on('hidden.bs.modal', function (e) {
							//alert("doing something..");
							$('#edit-modal-body').html('loading..');
							});			



						$('.ajaxDialog').on("click", function(){
							
							var target = $(this).data('target');
							var title= $(this).data('title');
							var backdrop = $(this).data('backdrop');
							var width = $(this).data('width');
							var showheader = $(this).data("showheader");
							var showfooter = $(this).data("showfooter");
							if (showheader == undefined) showheader = false;
							if (showfooter == undefined) showfooter = true;//default is to show the footer
							
							if ( $("#edit-modal-header").length != 0 ){
								if (!showheader || showheader == "false"){
									$('#edit-modal-header').hide();
								} else{
									$('#edit-modal-header').show();
								}
							}
							if ( $('#edit-modal-footer').length != 0 ){
								if (!showfooter || showfooter == "false"){
									$('#edit-modal-footer').css("visibility", "hidden");
								} else {
									$('#edit-modal-footer').css("visibility", "visible");
								}
							}
							
							if(title != null && $('#modal-title')!=0 ){
								$('#modal-title').text(title);
							}
							if(width != null){
								$('.modal-dialog').css("width", width);
							} else{
								$('.modal-dialog').css("width", null);
							}
							if (backdrop==null || backdrop=="true"){
								backdrop = true;
							} else {
								backdrop = false;
							}
							$('#edit-modal-body').load(target,function(result){
								var textareas = jQuery(".htmleditor");
								if(textareas.size() > 0){
								
									loadEditors();
								}
								$('#editmodal').modal({show:true,backdrop:backdrop});
							
							});
								
						    return false;
						});		


						$('.iframeDialog').on("click", function(){
							var target = $(this).data('target');
							var title= $(this).data('title');
							var backdrop = $(this).data('backdrop');
							var width = $(this).data('width');
							if(title != null){
								$('#modal-title').text(title);
							}
							if(width != null){
								$('.modal-dialog').css("width", width);
							}
							else{
								$('.modal-dialog').css("width", null);
							}
							if (backdrop==null || backdrop=="true"){
								backdrop = true;
							} else {
								backdrop = false;
							}
								
						        $("#edit-modal-body").html('<iframe width="100%" height="100%" frameborder="0" scrolling="yes" allowtransparency="true" src="'+target+'"></iframe>');

								
								$('#editmodal').modal({show:true,backdrop:backdrop});
							
							
						    return false;
						});		


});










function reloadStylesheets() {
    var queryString = '?reload=' + new Date().getTime();
    $('link[rel="stylesheet"]').each(function () {
        this.href = this.href.replace(/\?.*|$/, queryString);
    });
}
