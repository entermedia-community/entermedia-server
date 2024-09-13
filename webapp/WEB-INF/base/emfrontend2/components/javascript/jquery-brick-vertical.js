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
  //console.log("gridResize resizing "	);
  var minwidth = grid.data("minwidth");
  if (minwidth == null || minwidth.length == 0) {
    minwidth = 350;
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

eachwidth = eachwidth -8;
//totalavailablew = totalavailablew - (maxcols*8);

  // Two loops, one to make rows and one to render cells
  var colwidthpx = totalavailablew/maxcols;
  var sofarusedw = 0;

 var colheight = {};

  var rows = new Array();
  var row = new Array();
  
  var colnum = 0;
  rows.push(row);
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
      
      cell.data("colnum", colnum);
      row.push(cell);
      
      var runningtotal = colheight[colnum]??0;
      runningtotal = runningtotal + 8;
      colheight[colnum] = runningtotal + newheight;
       
      cell.css("top",runningtotal + "px");
      cell.width(eachwidth);
      cell.height(newheight);
      
      var colx = colwidthpx * colnum;
      cell.css("left",colx + "px");
      
      if( (colnum + 1) == maxcols)
      {
		colnum = 0;
		row = new Array();
		rows.push(row);
	  }
	  else
	  {
		colnum++;
	  }
	  
	  grid.css("height", runningtotal + "px");
      
    });

   checkScroll();
};



isInViewport = function( cell ) { 
  const rect = cell.getBoundingClientRect();
  var isin =
    rect.bottom <=
      (window.innerHeight || document.documentElement.clientHeight);
  return isin;
};


replaceelement = function (url, div, options, callback) {
  jQuery.ajax({
    url: url,
    async: false,
    data: options,
    success: function (data) {
      div.replaceWith(data);

      if (callback && typeof callback === "function") {
        //make sure it exists and it is a function
        callback(); //execute it
      }
    },
    xhrFields: {
      withCredentials: true,
    },
    crossDomain: true,
  });
};


checkScroll = function (grid) {
	
  var appdiv = $("#application");
  var siteroot = appdiv.data("siteroot") + appdiv.data("apphome");
  var componenthome = appdiv.data("siteroot") + appdiv.data("componenthome");

  if (!grid) {
    return;
  }
  if (grid.data("singlepage") == true) {
    return;
  }

  var resultsdiv = $(grid).closest("#resultsdiv");
  var lastcheck = $(resultsdiv).data("lastscrollcheck");
  var currentscroll = 0;

  //currentscroll = $(window).scrollTop();
  currentscroll = $(".scrollview").scrollTop();

  if (lastcheck == currentscroll) {
    //Dom events cause it to fire recursively
    return false;
  }
  $(resultsdiv).data("lastscrollcheck", currentscroll);
  if (stopautoscroll) {
    // ignore scrolls
    if (typeof getOverlay === "function" && getOverlay().is(":visible")) {
      var lastscroll = getOverlay().data("lastscroll");

      if (Math.abs(lastscroll - currentscroll) > 50) {
        $(window).scrollTop(lastscroll);
      }
    }
    return;
  }

  // No results?

  var gridcells = $(".masonry-grid-cell", resultsdiv);
  if (gridcells.length == 0) {
    //console.log("No grid found")

    return; //No results?
  }

  //gridupdatepositions(grid);

  var page = parseInt(resultsdiv.data("pagenum"));
  if (isNaN(page)) {
    page = 1;
  }

  var total = parseInt(resultsdiv.data("totalpages"));
  if (isNaN(total)) {
    total = 1;
  }
   console.log("checking scroll " + stopautoscroll + " page " + page + " of " + total);
  if (page == total) {
    //Last page. dont load more
    //console.log("Last page, dont load more")
    return;
  }

  // console.log($(window).scrollTop() + " + " + (visibleHeight*2 + 500) +
  // ">=" + totalHeight);
  //	var visibleHeight = $(window).height();
  //	var totalHeight = $(document).height();
  //	var atbottom = ($(window).scrollTop() + (visibleHeight*2 + 500)) >= totalHeight ; // is the scrolltop plus the visible
  // equal to the total height?

  var lastcell = gridcells.last().get(0);
  if (!isInViewport(lastcell)) {
    return; //not yet at bottom (-500px)
  }

  stopautoscroll = true;
  var session = resultsdiv.data("hitssessionid");
  page = page + 1;
  resultsdiv.data("pagenum", page);

  var stackedviewpath = resultsdiv.data("stackedviewpath");
  if (!stackedviewpath) {
    stackedviewpath = "stackedgallery.html";
  }
  
  var collectionid = $(resultsdiv).data("collectionid");
  var publishingid = $(resultsdiv).data("publishingid");
  
  var params = {
    hitssessionid: session,
    pagenum: page,
    oemaxlevel: "1",
  };
  if (collectionid) {
    params.collectionid = collectionid;
  }
  if (publishingid) {
    params.publishingid = publishingid;
  }

  console.log("Loading page: #" + page + " - " + stackedviewpath);

  $.ajax({
    url: stackedviewpath,
    xhrFields: {
      withCredentials: true,
    },
    cache: false,
    data: params,
    success: function (data) {
      var jdata = $(data);
      var code = $(".masonry-grid2", jdata).html();
      //$(".masonry-grid",resultsdiv).append(code);
      $(grid).append(code);
      // $(resultsdiv).append(code);
      gridResize(grid);
      //$(window).trigger("resize");
      stopautoscroll = false;
      checkScroll(grid);
    },
  });
};

	
var methods = {
    init : function(options) {
		//Any details?
		gridResize($(this));
    },
    render: function()
    {
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
