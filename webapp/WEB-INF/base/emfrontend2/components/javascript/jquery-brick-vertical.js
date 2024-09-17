(function( $ ){
	var stopautoscroll = false;
	var gridcurrentpageviewport = 1;
	var gridlastscroll = 0;


gridResize = function (grid) {
  //TODO: Put these on grid.data()
  stopautoscroll = false;
  gridcurrentpageviewport = 1;
  gridlastscroll = 0;
  
  if (!grid) {
    return;
  }

  if (!grid.is(":visible")) {
    return;
  }

  var minwidth = grid.data("minwidth");
  if (minwidth == null || minwidth.length == 0) {
    minwidth = 250;
  }
  var totalavailablew = grid.width();
  var maxcols = 5;
  var eachwidth = 0;
  
  while( eachwidth < minwidth)
  {
	  eachwidth = totalavailablew / maxcols;
	  maxcols--;
  }
  maxcols++;
  
  if( maxcols == 0)
  {
	maxcols = 1;
  }
   var colheight = {};
  for (let col=0; col<maxcols; col++) {
		colheight[col] = 0;
  }
  var innerheight = (window.innerHeight || document.documentElement.clientHeight);

  eachwidth = eachwidth -8;
  // Two loops, one to make rows and one to render cells
  var colwidthpx = totalavailablew/maxcols;
  var sofarusedw = 0;



  var colnum = 0;
  $(grid)
    .find(".masonry-grid-cell")
    .each(function () {
      var cell = $(this);
      var w = cell.data("width");
      var h = cell.data("height");
       w = parseInt(w);
       h = parseInt(h);
      if (w == 0) {
        w = eachwidth;
        h = eachwidth;
      }
      var a = 1;
      a = w / h;
      
      cell.data("aspect", a);
      var newheight = Math.floor(eachwidth / a);
      colnum = shortestColumn(colheight);
      cell.data("colnum", colnum);
      
      var runningtotal = colheight[colnum]??0;
      runningtotal = runningtotal + 8;
      colheight[colnum] = runningtotal + newheight;
       
      cell.css("top",runningtotal + "px");
      cell.width(eachwidth);
      cell.height(newheight);
      
      var colx = colwidthpx * colnum;
      cell.css("left",colx + "px");
      grid.css("height", colheight[colnum] + "px");
      
      
      
	  
      
    });

	//Gray boxes on bottom    
   var maxheight = 0;
   for (let column in Object.keys(colheight)) {
		if( colheight[column] > maxheight)
		{
			maxheight = colheight[column]; 
		}
    }
 	for (let column in Object.keys(colheight)) 
 	{
		var onecolheight = colheight[column] + 8;
		if( onecolheight < maxheight)
		{
			var cell = $('<div></div>');
			cell.addClass("masonry-grid-cell grid-filler");
			cell.css("top",onecolheight + "px");
	        var colx = colwidthpx * column;
			cell.css("left",colx + "px");
      		cell.width(eachwidth);
      		var h = maxheight - onecolheight;
      		cell.height(h);
   	 	    grid.append(cell);
		}
    }
    

   checkScroll();
};


shortestColumn = function (colheight) {
	var shortColumn = 0;
	var shortColumnHeight = -1;
	for (let column in Object.keys(colheight)) 
 	{
		var onecolheight = colheight[column];
		if(shortColumnHeight == -1 || onecolheight < shortColumnHeight) {
			shortColumnHeight = onecolheight;
			shortColumn = column;
		}
    }
    return shortColumn;
}


isInViewport = function( cell ) { 
  const rect = cell.getBoundingClientRect();
  var isin =
    rect.top <=
      (window.innerHeight || document.documentElement.clientHeight);
  return isin;
};


checkScroll = function (grid) {

 var currentscroll = $(".scrollview").scrollTop();

 //From the top to this height. Set the src
 $(grid)
    .find(".masonry-grid-cell")
    .each(function () {
			var cell = $(this);
			//var bottom = cell.top() + cell.height();
			//if( bottom < currentscroll)
			if (isInViewport(cell.get(0)))
			{
				var image = cell.find("img");
				if( image.prop("src") == undefined ||  image.prop("src") == "")
				{
  				  image.prop("src", image.data("imagesrc")); 
  				  image.show();			
				}
			}	
	});
}
	
var methods = {
    init : function(options) {
		gridResize($(this));
    },
    resize: function()    {
		gridResize($(this));
	}
}; //Methods end


$.fn.brick = function(methodOrOptions) { //Generic brick caller
        if ( methods[methodOrOptions] ) {
            return methods[ methodOrOptions ].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof methodOrOptions === 'object' || ! methodOrOptions ) {
            // Default to "init"
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  methodOrOptions + ' does not exist on jQuery.tooltip' );
        }    
};

})( jQuery );
