(function( $ ){
	var stopautoscroll = false;
	var gridcurrentpageviewport = 1;
	var gridlastscroll = 0;
	var gridscrolldirection = "down";


gridResize = function (grid) {
  //TODO: Put these on grid.data()
  stopautoscroll = false;
  gridcurrentpageviewport = 1;
  gridlastscroll = 0;
  gridscrolldirection = "down";
  
  if (!grid) {
    return;
  }

  if (!grid.is(":visible")) {
    return;
  }
  //console.log("gridResize resizing "	);
  var fixedheight = grid.data("maxheight");
  if (fixedheight == null || fixedheight.length == 0) {
    fixedheight = 200;
  }
  fixedheight = parseInt(fixedheight);

  var totalwidth = 0;
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
        var newheight = trimRowToFit(row);
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

  var makebox = grid.data("makebox");

  if (row.length > 0) {
    trimRowToFit(row);
    //if( makebox && makebox == true && rownum >= 3)
    {
      grid.css("height", totalheight + "px");
      //grid.css("overflow","hidden");
    }
  }

	$.each(rows, function () { 
		var row = $(this);
		trimRowToFit(row);
	});
   checkScroll();
};

trimRowToFit = function(grid, row ) {  
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
  
  // The overwidth may not be able to be divided out evenly depending on
  // number of
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
    // Deal
    // with
    // fraction
    // of a
    // pixel
    // We have a fraction of a pixel to add to last item
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



isInViewport = function( cell ) { 
  const rect = cell.getBoundingClientRect();
  var isin =
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <=
      (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth);
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


gridupdatepositions = function(grid) {
  //console.log("Checking Old Position: " + oldposition);
  var resultsdiv = grid.closest(".resultsdiv");
  if (!resultsdiv) {
    resultsdiv = grid.closest(".resultsdiv");
  }

  var positionsDiv = resultsdiv.find(".resultspositions");
  //console.log("positionsDiv:", positionsDiv);

  if (positionsDiv.length > 0) {
    var oldpage = grid.data("currentpagenum");

    $(".masonry-grid-cell", grid).each(function (index, cell) {
      var elementviewport = isInViewport(cell);
      if (elementviewport) {
        var pagenum = $(cell).data("pagenum");
        if (pagenum != oldpage) {
          grid.data("currentpagenum", pagenum);
          positionsDiv.data("currentpagenum", pagenum);
          //var currentscroll = $(window).scrollTop();

          //console.log("Firing dom event: ",oldpage, pagenum, $(window).scrollTop());
          var url = positionsDiv.data("url");
          //positionsDiv.data("currentpage",pagenum); //Where we are at
          var options = positionsDiv.data();
          replaceelement(url, positionsDiv, options);
        }
        return false;
      }
    });
  }
};

checkScroll = function () {
  var appdiv = $("#application");
  var siteroot = appdiv.data("siteroot") + appdiv.data("apphome");
  var componenthome = appdiv.data("siteroot") + appdiv.data("componenthome");

  var grid = getCurrentGrid();
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

  gridupdatepositions(grid);

  var page = parseInt(resultsdiv.data("pagenum"));
  if (isNaN(page)) {
    page = 1;
  }

  var total = parseInt(resultsdiv.data("totalpages"));
  if (isNaN(total)) {
    total = 1;
  }
  // console.log("checking scroll " + stopautoscroll + " page " + page + " of
  // " + total);
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
    //console.log("up top, dont load more yet")
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
      //$(".masonry-grid",resultsdiv).append(code);
      $(grid).append(code);
      // $(resultsdiv).append(code);
      //gridResize();
      $(window).trigger("resize");

      stopautoscroll = false;

      if (getOverlay().is(":hidden")) {
        checkScroll();
      }
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
