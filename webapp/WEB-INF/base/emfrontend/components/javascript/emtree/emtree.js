jQuery(document).ready(function() 
{ 


	$('.emtree ul li div').livequery('click', function(){
		
			//$(this).parent().find('> ul').toggle('fast');
			var node = $(this).closest('.noderow');
			var nodeid = node.data('nodeid');
			var depth = node.data('depth');
			
			var home = $(this).closest(".emtree").data("home");
			
			if ( $(this).find('.arrow').hasClass('down') )
			{
				$(this).find('.arrow').removeClass('down');				
			}
			else 
			{ 
				//Open it. add a UL
				$(this).find('.arrow').addClass('down');				
			}
			node.load(home + "/views/settings/modules/asset/categories/tree.html?toggle=true&nodeID=" + nodeid + "&depth=" + depth);
	});
	
	$('.emtree .field').on( {
		click: function () {
			event.stopPropagation();
		},
		focusin: function(){
			if ($(this).val() == this.defaultValue ) { 
				$(this).val(''); 
			}
		}, 
		focusout: function(){
			if ($(this).val() == "") { 
				$(this).val(this.defaultValue); 
			}
		}
			
	});
	
	
	$(".emtree .add").livequery('click', function(event) {
				event.stopPropagation();
				var tree = $(this).closest(".emtree");
				tree = $(tree);
				var home = tree.data("home");
				
				var id = $(this).attr("id");
				padding = tree.find( "#" + id + "_row div").css('padding-left');
				padding = padding.replace('px', '');
				padding = parseInt(padding);
				padding += 20;
				var node = $(this).closest('.noderow').data('nodeid');
				
				if ( tree.find( "#" + id + "_row > div .arrow").length > 0 ) {
					if ( tree.find("#" + id + "_row > div .arrow").hasClass('down') ) {	
						
					} else {
						tree.find("#" + id + "_row").find('> ul').toggle('fast');
						tree.find("#" + id + "_row > div .arrow").addClass('down');
					   jQuery.get(home + "/views/settings/modules/asset/categories/expandnode.html?nodeID=" + node);
					}
				} else {
					tree.find("#" + id + "_row > div").prepend('<span class="arrow down" id="newarrow"></span>');
					tree.find("#" + id + "_row").find('> ul').toggle('fast');
					tree.find("#" + id + "_row > div .arrow").addClass('down');
				}
				
				tree.find("#" + id + "_add").show("fast");
				tree.find("#" + id + "_add div").css('padding-left', padding )
				tree.find("#" + id + "_row > div").addClass('selected');
				tree.find("#" + id + "_add input").focus();
	} );
	
	
	$(".emtree .cancel").livequery('click', function(event) { 
			
			event.stopPropagation();

			$("#newarrow").remove();
	
			var id = $(this).attr("id");	
			$("#" + id + "_add").hide("fast");
			$("#" + id + "_row > div").removeClass('selected');
	} );
	
	$(".emtree .delete").livequery('click', function(event) {
			event.stopPropagation();

			var id = $(this).data('parent');
			
			var agree=confirm("Are you sure you want to delete?");
			if (agree)
			{
				var tree = $(this).closest(".emtree");
				var home = tree.data("home");

				jQuery.get(home + "/views/settings/modules/asset/categories/deletecategory.html", {
					categoryid: id,
					} ,function () {
						tree.find("#" + id + "_row").hide( 'fast', function(){
							repaintEmTree(tree); 
						} );
						
					});
			} else {
				return false;
			}
	} );
			
	$(".emtree .save").livequery('click', function(event) {
					event.stopPropagation();
					var id = $(this).data("parent");
					//alert("parent category was: " + id);
					var newname = $("#" + id + "_new").attr("value");
					var tree = $(this).closest(".emtree");

					//alert("New name: " + newname);
					var home = tree.data("home");

					jQuery.get(home + "/views/settings/modules/asset/categories/addcategory.html", {
						categoryid: id,
						'newname': newname
					
						} , function () {
							tree.find("#" + id + "_add").hide("fast", function(){
								repaintEmTree(tree); 
							});
						}
					
					);
	} );
	
	
	$(".emtree .edit").livequery('click', function(event) {
				event.stopPropagation();
				var id = $(this).parents('.noderow:first').data('nodeid');

				var tree = $(this).closest(".emtree");
				tree.find("#" + id +"_display").hide("fast");
				tree.find("#" + id +"_edit").show("fast");		
				
				return false;
	} );
	

	$(".emtree .editcancel").livequery('click', function(event) {
				event.stopPropagation();
				var id = $(this).parents('.noderow:first').data('nodeid');

				var tree = $(this).closest(".emtree");
				
				tree.find("#" + id +"_display").show("fast");
				tree.find("#" + id +"_edit").hide("fast");		
				return false;
	} );

	$(".emtree .editsave").livequery('click', function(event) {
				event.stopPropagation();
				var id = $(this).parents('.noderow:first').data('nodeid');


				var tree = $(this).closest(".emtree");
				var home = tree.data("home");
				
				var newname = tree.find("#" + id + "_edit_field").attr("value");
				jQuery.get( home + "/views/settings/modules/asset/categories/savecategory.html", {
					'id': id,
					categoryid: id,
					'name': newname
				
					} , function () {						
						tree.find("#" + id +"_display").show("fast");
						tree.find("#" + id +"_edit").hide("fast");		
						tree.find("#" + id + "_display").html(newname);
					}
				
				);
				repaintEmTree(tree);
				return false;
	} );
	
	
});

repaintEmTree = function (tree) {
	tree.closest("#treeholder").load("$home$apphome/views/settings/modules/asset/categories/tree.html");
}
