$(document).ready(function() {
					
	
	
	
					var app = jQuery("#application");
					var apphome = app.data("home") + app.data("apphome");
					var themeprefix = app.data("home")
							+ app.data("themeprefix");
					var clientroot = jQuery("#showcaseclient").data("clientroot");
					
					
				

					// Grab the data?
					/* This forces our modal dialogs to reload */
					/*
					$('body').on('hidden', '.modal', function () {
						  $(this).removeData('modal');
						});
					/*
					 * */
					 /*
					jQuery("#editmodal").on("shown", function () {
						var textareas = jQuery(".htmleditor");
						if(textareas.size() > 0){
						
							loadEditors();
						}
					} );
					*/
					
					
					
					
					
					jQuery("#share").on("shown", function () {
						addthis.toolbox('.addthis_toolbox');
					} );
					
					
					//This is the open and close code
					$("#closeTrigger").click(function() {

						$('.resizer').removeClass('openLeft', 1000).addClass('closeLeft', 1000);
						$('#openTrigger').show();
						$('#closeTrigger').hide();
						jump();
						resize();
					});

					$("#openTrigger").click(function() {
						$('.resizer').removeClass('closeLeft',1000).addClass('openLeft',1000);
						$('#openTrigger').hide();
						$('#closeTrigger').show();		
						mark();
						resize();
					});
					
					
					$('.carousel').carousel({
						interval : 2000														
					});

					$('.carousel').carousel('pause');

					$('.productclosebutton').on("click", function() {
						$('#product-dialog').fadeTo('slow', 0);
						$('.close2').fadeTo('slow', 0);
						play();
					});

					
					
					
					
					
					$('.product .thumb .productlink')
							.on("click", 
									function() {

								var parentnode = $(this).parent();									
								var productid = parentnode.data(
												"productid");
												
										var cpid = parentnode.data(	"cpid");
										$('#product-dialog').load(clientroot+ "/details/productdetails.html?productid="+ productid + "&cp="+cpid + "&oemaxlevel=1", function() {});
										
										$('#product-dialog').fadeTo('slow', 1);
										pause();
									
										$('.close2').fadeTo('slow', 1);
									});

					

//					jQuery(".addtocart").livequery('click', function() {
//										var clicked = jQuery(this);
//										var productid = jQuery(this).data("productid");
//										jQuery
//												.ajax({
//													url : apphome+ "/views/cart/toggle.html?productid=" + productid,
//															
//													async : false,
//													success : function(data) {
//
//														if( clicked.hasClass("active") )
//														{
//															clicked.removeClass("active");															
//														}
//														else
//														{
//															clicked.addClass("active");
//														}
//													}
//												});
//											return false;
//					});
					

//					jQuery(".addtolove").livequery('click', function(e) {
//										var clicked = jQuery(this);
//										var cueproductid = jQuery(this).data("cueproduct");
//										jQuery
//												.ajax({
//													url : apphome+ "/components/lovedproducts/toggle.html?cueproductid=" + cueproductid,
//															
//													async : false,
//													success : function(data) {
//														if( clicked.hasClass("active") )
//														{
//															clicked.removeClass("active");															
//														}
//														else
//														{
//															clicked.addClass("active");
//														}
//													}
//												});
//											return false;
//					});
					
					
					jQuery("body")
							.on("click","#addcue",
									
									function() {
										var paused = cuepoint.video.paused;
										var departmentasset = jQuery(this)
												.data("dataid");
										var current = cuepoint.currentTime();
										var time = Math.round(current);
										jQuery
												.ajax({
													url : clientroot
															+ "/addcue.html?save=true&field=timecode&timecode.value="
															+ time														
															+ "&field=assetid&assetid.value="
															+ departmentasset ,
															
													async : false,
													success : function(data) {
													
														reloadCues(paused, data);
														//$('#editmodal').modal('hide');
														

														if (paused) {
															pause();
														}
													}
												});
										
									});

					
					
					
					
					
//					
//					jQuery(".addexistingproduct")
//					.livequery(
//							'click',
//							function() {
//
//								var productid = jQuery(this).data("productid");
//								var cueid = jQuery(this).data("cueid");
//								jQuery
//										.ajax({
//											url : apphome
//													+ "/views/modules/product/addexistingfinish.html?productid=" + productid +"&cueid=" + cueid,
//											async : false,
//											success : function(data) {
//												reloadProducts();
//												//$('#editmodal').modal('hide')
//											}
//										});
//
//							});
//					
					
					jQuery("body").on('click',".shiftLeft", function() {
						var amount = jQuery(this).data("amount");
						if (amount == null) {
							amount = 1;
						}
						shiftLeft(amount);
						return false;
					});

					jQuery("body").on('click',".shiftRight", function() {
						var amount = jQuery(this).data("amount");
						if (amount == null) {
							amount = 1;
						}
						shiftRight(amount);
						return false;
					});
					
					
					
					
					jQuery(".play").on('click', function() {
						play();
						
						var hidepanel = jQuery(this).data("hidepanel");
						if(hidepanel == true){
							
							$("#responseform :input").prop("disabled", true);
							jQuery(".autohide").fadeOut("slow");
						}
						return false;
					});

					
					
					

						
					

					$(window).on("resize", function() {
						resize();
						
					}).resize();

			//		$('#rs-carousel').carousel();

					// TODO: Move this code to a function
					jQuery(".ImageLink").on('mousedown', function(e) {
						$(this).data('p0', {
							x : e.pageX,
							y : e.pageY
						});
					}).on(
							'mouseup',
							function(e) {
								var p0 = $(this).data('p0'), p1 = {
									x : e.pageX,
									y : e.pageY
								}, d = Math.sqrt(Math.pow(p1.x - p0.x, 2)
										+ Math.pow(p1.y - p0.y, 2));

								if (d < 4) {
									e.stopPropagation();
									e.preventDefault();
									// reload the page into application and full
									// screen it
									var link = $(this).attr("href");
									window.location.assign(link);
									/*
									 * jQuery.get(link, {}, function(data) {
									 * $('#application').replaceWith(data);
									 * //now somehow let it load? } );
									 * $('#application').fullScreen(function(e) {
									 * $(window).resize(); });
									 */
								}
							});

				});





updateVideoDisplay = function () {
	var urls = apphome + "/test/active/actions/teststatus.json";
	console.log("Updating..");
	$.getJSON(urls,  function(data) {
		
		
		
		
		 var percent = data.testdata.percentcomplete;
		 $("#completion-bar").css("width", percent + "%");
		jQuery.each(data.testdata.nodes, function(index, more) {
			console.log(more.id + ":" + more.show);
			var id = more.id;
			var show = more.show;
			if(show){
				console.log("showing" + id);
				jQuery("#node-" + id).show();
			} else{
				console.log("hiding" + id);

				jQuery("#node-" + id).hide();

			}
		});

		
	});
	
	
}








resize = function()
{
	
	var video = document.getElementById("video"); // assuming "video" is your videos' id
	//video.setAttribute("controls","controls");
	//video.removeAttribute("controls");
// $("#video").css('width', $(window).width()*.75 );
						
						//$("#video").css('height', "65%");
						
						//$("#video").css('width', "90%");

						var footer = 100; //header

						if( $("#btmhead").is(":visible") )
						{
							footer = footer + 30;
						}
						
						if( $("#btmcontent").is(":visible") )
						{
							footer = footer + 200;
						}
						/*console.log(footer);*/
						$("#video").css('height', $(window).height() -  footer );
						
						var height = $("#video-holder").height();
						$("#left-slide").css('height',height );
}

getPadding = function() {
	if ($("#cuepointcontainer").is(":visible")) {
		return 200;
	}
	return 80;
}

jump = function(timecode) {
	if(timecode == null){
	 timecode = getSessionValue("timecode");
	}
	console.log("time " + timecode);
	cuepoint.setTime(timecode);
}

mark = function() {
	var current = cuepoint.currentTime();
	setSessionValue("timecode", current);
}

pause = function() {
	cuepoint.pause();

}

play = function() {
	cuepoint.play();

}

play = function() {
	cuepoint.play();

}


$('#productmodal').on('show', function () {
pause();
	});



slideToCue = function() {
	
	var page = jQuery( ".videolinks li a.current").parents(".item").data("page");
	var cueid = jQuery( ".videolinks li a.current").data("cueid");
	
	loadCueMenu(cueid);
	jQuery("#myCarousel").carousel(page);
	jQuery("#myCarousel").carousel('pause');
	$(window).resize();
}


shiftLeft = function(shiftamount) {
	var current = cuepoint.currentTime();
	current = current - shiftamount;
	var paused = cuepoint.video.paused;

	cuepoint.setTime(current);
	if (paused) {
		pause();
	}
	// cuepoint.play();

}
shiftRight = function(shiftamount) {
	var current = cuepoint.currentTime();
	current = current + shiftamount;testplan
	var paused = cuepoint.video.paused;
	cuepoint.setTime(current);
	if (paused) {
		pause();
	}
}

loadCueMenu= function(cueid){
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	var dataid = jQuery("#showcaseclient").data("dataid");

	var clientroot = jQuery("#showcaseclient").data("clientroot");
	jQuery("#cuepointactions").load(clientroot + "/details/cuepointactions.html?oemaxlevel=1&cueid=" + cueid + "&dataid=" + dataid);
}



reloadCues = function(paused, startingcue){
		 
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var videoid = jQuery("#video-cuepoints").data("dataid");
	if(paused){
		var pstatus = "true"
	} else{
		pstatus = "false";
	}
	var clientroot = jQuery("#showcaseclient").data("clientroot");
	
	
	jQuery("#video-cuepoints").load(clientroot + "/details/cuepoints.html?oemaxlevel=1&id=" + videoid + "&paused=" + pstatus, function() 
			{
		if(startingcue != null){
			var slide =jQuery("#aslide-" + startingcue); 	
			loadCueMenu(startingcue);

			slide.addClass("current");
			slideToCue();
			if(paused){
				pause();
			}
		}
			}
			
	);
	
}



reloadProducts = function(){
		 
	
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var videoid = jQuery("#cuepoint-products").data("cueid");
		var clientroot = app.data("home") + jQuery("#showcaseclient").data("clientroot");
	jQuery("#cuepoint-products").load(clientroot + "/details/cuepointproducts.html?oemaxlevel=1&cueid=" + videoid);
	
}




setSessionValue = function(key, value) {
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	jQuery.ajax({
		url : apphome + "/components/session/setvalue.html?key=" + key
				+ "&value=" + value
	});

}

getSessionValue = function(key) {
	var returnval = "";
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	jQuery.ajax({
		url : apphome + "/components/session/getvalue.html?key=" + key,
		async : false,
		success : function(data) {
			returnval = data;
		}
	});

	return returnval;
}


showModal = function(inUrl){
	
	$('#edit-modal-body').load(inUrl,function(result){
		var textareas = jQuery(".htmleditor");
		if(textareas.size() > 0){
		
			loadEditors();
		}
		$('#editmodal').modal({show:true});
	
	});
	
	
}

