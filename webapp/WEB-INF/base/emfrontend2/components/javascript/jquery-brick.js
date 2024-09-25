(function( $ ){
	var stopautoscroll = false;
	var gridcurrentpageviewport = 1;
	var gridlastscroll = 0;


	function gridResize (grid) {
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
	  var fixedheight = grid.data("maxheight");
	  if (fixedheight == null || fixedheight.length == 0) {
	    fixedheight = 200;
	  }
	  fixedheight = parseInt(fixedheight);
	
	  var totalheight = fixedheight;
	  var rownum = 0;
	  var totalavailablew = grid.width();
	
	  // Two loops, one to make rows and one to render cells
	  var sofarusedw = 0;
	  var sofarusedh = 0;
	
	  var row = new Array();
	  var rows = new Array();
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
	        w = fixedheight;
	        h = fixedheight;
	      }
	      var a = 1;
	      if (w >= h) {
	        a = w / h;
	      } else {
	        a = h / w;
	      }
	      cell.data("aspect", a);
	      var neww = Math.floor(a * fixedheight);
	      cell.data("targetw", Math.ceil(neww));
	      var isover = sofarusedw + neww;
	      if (isover > totalavailablew) {
	        // Just to make a row
	        // Process previously added cell
	        var newheight = trimRowToFit(grid,row);
	        totalheight = totalheight + newheight + 8;
	        row = new Array();
	        rows.push(row);
	        sofarusedw = 0;
	        rownum = rownum + 1;
	      }
	      sofarusedw = sofarusedw + neww;
	      row.push(cell);
	      cell.data("rownum", rownum);
	    });
	
	  if (row.length > 0) {
	    trimRowToFit(grid,row);
	    //if( makebox && makebox == true && rownum >= 3)
	    {
	      grid.css("height", totalheight + "px");
	      //grid.css("overflow","hidden");
	    }
	  }
	
		$.each(rows, function () { 
			var row = $(this);
			trimRowToFit(grid,row);
		});
	   checkScroll(grid);
	};
	
	
	function trimRowToFit(grid, row ) {  
	  var totalwidthused = 0;
	  var targetheight = grid.data("maxheight");
	  $.each(row, function () {
	    var div = this;
	    var usedw = div.data("targetw");
	    totalwidthused = totalwidthused + usedw;
	  });
	  
	  var totalavailablew = grid.width();
	  var existingaspect = targetheight / totalwidthused; // Existing aspec ratio
	  var overwidth = Math.abs(totalwidthused - totalavailablew);
	  var changeheight = existingaspect * overwidth;
	  var fixedheight = Math.floor(targetheight + changeheight);
	  
	  if (fixedheight > targetheight * 1.7) {
	    fixedheight = targetheight;
	  }
	  
	  var totalwused = 0;
	  $.each(row, function () {
	    var div = this;
	    var image = $("img.imagethumb", div);
	    // div.css("line-height",fixedheight + "px");
	    div.css("height", fixedheight + "px");
	    image.height(fixedheight);
	    image.data("fixedheight", fixedheight);
	
	    var a = div.data("aspect");
	    var neww = fixedheight * a;
	
	    neww = Math.floor(neww); // make sure we dont round too high across lots
	    // of widths
	    div.css("width", neww + "px");
	    image.width(neww);
	    totalwused = totalwused + neww;
	  });
	
	  totalavailablew = grid.width();
	  if (totalwused != totalavailablew && fixedheight != targetheight) {
	    var toadd = totalavailablew - totalwused;
	    var div = row[row.length - 1];
	    if (div) {
	      var w = div.width();
	      w = w + toadd;
	      div.css("width", w + "px");
	      var image = $("img.imagethumb", div);
	      image.width(w);
	    }
	  }
	  return fixedheight;
	};
	
	
	
	function isInViewport( cell ) { 
	  const rect = cell.getBoundingClientRect();
	  var isin =
	    rect.top >= 0 &&
	    rect.left >= 0 &&
	    rect.bottom <=
	      (window.innerHeight || document.documentElement.clientHeight) &&
	    rect.right <= (window.innerWidth || document.documentElement.clientWidth);
	  return isin;
	};
	
	
	function replaceelement(url, div, options, callback) {
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
	
	
	function gridupdatepositions(grid) {
	  var resultsdiv = grid.closest(".resultsdiv");
	  if (!resultsdiv) {
	    resultsdiv = grid.closest(".resultsdiv");
	  }
	
	  var positionsDiv = resultsdiv.find(".resultspositions");
	
	  if (positionsDiv.length > 0) {
	    var oldpage = grid.data("currentpagenum");
	
	    $(".masonry-grid-cell", grid).each(function (index, cell) {
	      var elementviewport = isInViewport(cell);
	      if (elementviewport) {
	        var pagenum = $(cell).data("pagenum");
	        if (pagenum != oldpage) {
	          grid.data("currentpagenum", pagenum);
	          positionsDiv.data("currentpagenum", pagenum);
	          var url = positionsDiv.data("url");
	          var options = positionsDiv.data();
	          replaceelement(url, positionsDiv, options);
	        }
	        return false;
	      }
	    });
	  }
	};
	
	function checkScroll(grid) {
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
	
	
	  var gridcells = $(".masonry-grid-cell", resultsdiv);
	  if (gridcells.length == 0) {
	    return; //No results?
	  }
	
	  gridupdatepositions(grid);
	
	  var page = parseInt(resultsdiv.data("pagenum"));
	  if (isNaN(page)) {
	    page = 1;
	  }
	
	  var total = parseInt(resultsdiv.data("totalpages"));
	  if (isNaN(total)) {
	    total = 1;
	  }
	  if (page == total) {
	    return;
	  }
	
	  var lastcell = gridcells.last().get(0);
	  if (!isInViewport(lastcell)) {
	    return; //not yet at bottom
	  }
	
	  stopautoscroll = true;
	  var session = resultsdiv.data("hitssessionid");
	  page = page + 1;
	  resultsdiv.data("pagenum", page);
	
	  var stackedviewpath = resultsdiv.data("stackedviewpath");
	  if (!stackedviewpath) {
	    stackedviewpath = "stackedgallery.html";
	  }
	  var link = componenthome + "/results/" + stackedviewpath;
	  var collectionid = $(resultsdiv).data("collectionid");
	  var params = {
	    hitssessionid: session,
	    page: page,
	    oemaxlevel: "1",
	  };
	  if (collectionid) {
	    params.collectionid = collectionid;
	  }
	
	  console.log("Loading page: #" + page + " - " + link);
	
	  $.ajax({
	    url: link,
	    xhrFields: {
	      withCredentials: true,
	    },
	    cache: false,
	    data: params,
	    success: function (data) {
	      var jdata = $(data);
	      var code = $(".masonry-grid", jdata).html();
	      $(grid).append(code);
	      $(window).trigger("resize");
	      stopautoscroll = false;
	      if (getOverlay().is(":hidden")) {
	        checkScroll(grid);
	      }
	    },
	  });
	};
	
	var methods = {
	    init : function(options) {
			//Any details?
			var grid = $(this);
			document.addEventListener("touchmove", function (e) {
			  checkScroll(grid);
			});
			
			gridResize(grid);
			
			jQuery(window).on("resize", function () {
				gridResize(grid);
			});
			
			lQuery(".scrollview").livequery("scroll", function () {
			    checkScroll(grid);
			});
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
