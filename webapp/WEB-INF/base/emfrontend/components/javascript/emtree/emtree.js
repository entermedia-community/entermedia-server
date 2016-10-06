jQuery(document).ready(function() 
{ 
	//Open and close the tree
	$('.emtree-widget ul li div span.arrow').livequery('click', function(event){
			event.stopPropagation();
			var tree = $(this).closest(".emtree");
			var node = $(this).closest('.noderow');
			var nodeid = node.data('nodeid');
			var depth = node.data('depth');
			$('.emtree ul li div').removeClass('selected');
			
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
			
			tree.find(nodeid + "_add").remove();
			node.load(home + "/components/emtree/tree.html?toggle=true&tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth, 
				function()
				{
					$(window).trigger( "resize" ) 
				});
	});

	//Select a node
	$('.emtree-widget ul li div').livequery('click', function(event) {
		event.stopPropagation();
		$('.emtree ul li div').removeClass('selected');
		//$(this).addClass("selected");
		var tree = $(this).closest(".emtree");
		var node = $(this).closest('.noderow');
		var nodeid = node.data('nodeid');
		var depth = node.data('depth');
		var home = tree.data("home");
		var prefix = $(this).closest(".emtree").data("url-prefix");
		if( prefix)
		{
			var treeholder = $("div#categoriescontent");
			var toplocation =  parseInt( treeholder.scrollTop() );
			var leftlocation =  parseInt( treeholder.scrollLeft() );
		
			//$("#right-col").load(); 'clearfilters':true,
				jQuery.get(prefix + nodeid + ".html",
						{
							'oemaxlevel':3,
							'tree-name':tree.data("treename"),
							'nodeID':nodeid,							
							'treetoplocation':toplocation,
							'treeleftlocation':leftlocation,
							'depth': depth
						},	
						function(data) 
						{
							var cell = jQuery("#searchlayout"); //view-picker-content
							cell.html(data);
							//window.location.hash="TOP";
						}
				);
		}
		else
		{
			tree.find(nodeid + "_add").remove();
			node.load(home + "/components/emtree/tree.html?toggle=true&tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth);
			
		}
});
	
//	var field = $('.emtree .field'); 
//	
//	field.livequery('click', function(event) 	{
//			event.stopPropagation();
//	});
//	
//	$('.emtree-widget .field.text').livequery('focusin', function(event) 	{
//		if ($(this).val() == this.defaultValue ) { 
//			$(this).val(''); 
//		}
//	});
//	
//	$('.emtree-widget .field.text').livequery('focusout', function(event)	{
//		if ($(this).val() == "") { 
//			$(this).val(this.defaultValue); 
//		}
//	});
	
	
	$(".emtree-widget .delete").livequery('click', function(event) {
			event.stopPropagation();

			var id = $(this).data('parent');
			
			var agree=confirm("Are you sure you want to delete?");
			if (agree)
			{
				var tree = $(this).closest(".emtree");
				var home = tree.data("home");

				jQuery.get(home + "/components/emtree/deletecategory.html", {
					categoryid: id,
					'tree-name': tree.data("treename"),
					} ,function () {
						tree.find("#" + id + "_row").hide( 'fast', function(){
							repaintEmTree(tree); 
						} );
						
					});
			} else {
				return false;
			}
	} );
			
	//need to init this with the tree
	$("div#treeholder").livequery( function()
	{	
		var treeholder = $(this);
		var top = treeholder.data("treetoplocation");
		if( top )
		{
			var left = treeholder.data("treeleftlocation");
			var catcontent = $("div#categoriescontent");
			catcontent.scrollTop(parseInt(top));
			catcontent.scrollLeft(parseInt(left));
		}
	});

	$("#treeholder input").livequery('keyup', function(event) 
	{
       	var input = $(this);
       	var node = input.closest(".noderow");
       	var tree = input.closest(".emtree");
       	var value = input.val();
       	var nodeid = node.data('nodeid');
		var depth = node.data('depth');
		var home = tree.data("home");

		if( event.keyCode == 13 ) 
	  	{
	       	//13 represents Enter key
			var link = home + "/components/emtree/savecategory.html?tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth;
			link = link + "&edittext=" + value;
			link = link + "&adding=true";
			node.load(link, function() 
			{
				//Reload tree in case it moved order
				tree.closest("#treeholder").load(home +  "/components/emtree/tree.html?tree-name=" + tree.data("treename") );		
			});
	  	}
		else if( event.keyCode === 27 ) //esc 
	  	{
			var link = home + "/components/emtree/tree.html?tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth;
			link = link + "&adding=true";
			node.load(link); 	  		
	  	}
	});

	$(".treecontext #renamenode").livequery('click', function(event) {
				event.stopPropagation();
				var clickedon = $(this);
				var contextmenu = $(clickedon.closest(".treecontext"));
				var node = contextmenu.data("selectednoderow");
				var tree = node.closest(".emtree");
				var home = tree.data("home");
				contextmenu.hide();
				//var node = $(this).closest('.noderow');
				var nodeid = node.data('nodeid');
				var depth = node.data('depth');
				var link = home + "/components/emtree/rename.html?tree-name=" + tree.data("treename") + "&nodeID=" + nodeid + "&depth=" + depth; 
				node.find("> .categorydroparea").load(link , function()
				{
					node.find("input").select().focus();
				});
				//node.html('<input name="test" />');
				return false;
	} );
	
  $("body").on("contextmenu", ".noderow", function(e) 
  {
  	var noderow = $(this); // LI is the think that has context .find("> .noderow");
	var xPos = e.pageX;
	var yPos = e.pageY;
	
	noderow.find("> .categorydroparea").addClass('selected'); //Keep it highlighted
	
	var emtreediv = noderow.closest(".emtree");
	
	var treename = emtreediv.data("treename"); 
	
	var $contextMenu = $( "#" + treename + "contextMenu");
	$contextMenu.data("selectednoderow",noderow);
	$contextMenu.css({
	  display: "block",
	      left: xPos,
	      top: yPos
	    });
	return false;
  });
		  
   $('body').click(function () {
     	var $contextMenu = $(".treecontext");
     	$contextMenu.hide();
     	$(".categorydroparea").removeClass('selected');     	
    });

	//end document ready
	
});

repaintEmTree = function (tree) {
	var home = tree.data("home");

	tree.closest("#treeholder").load(home +  "/components/emtree/tree.html?tree-name=" + tree.data("treename") );
	
}
