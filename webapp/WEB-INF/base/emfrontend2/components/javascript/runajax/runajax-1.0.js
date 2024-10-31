(function ( $ ) {
 
$.fn.runajax = function (options) {
 // $(".ajaxprogress").show();
 
 finddata = function (inlink, inname) {
  var item = $(inlink);
  var value = item.data(inname);
  if (!value) {
    value = inlink.attr(inname);
  }
  var parent = inlink;
  //debugger;
  while (!value) {
    parent = parent.parent().closest(".domdatacontext");
    if (parent.length == 0) {
      break;
    }
    value = parent.data(inname);
  }
  return value;
};

findclosest = function (link, inid) {
  var result = link.closest(inid);
  if (result.length == 0) {
    result = link.children(inid);
    if (result.length == 0) {
      result = $(inid);
    }
  }
  return result.first();
};

 
 var inlink = $(this);
  var inText = $(inlink).data("confirm");
  if (inText && !confirm(inText)) {
    return false;
  }
  inlink.attr("disabled", "disabled");

  if (inlink.hasClass("activelistener")) {
    $(".activelistener").removeClass("active");
    inlink.addClass("active");
  }
  //for listeners in a container
  if (inlink.hasClass("activelistenerparent")) {
    var listenerparent = inlink.closest(".activelistcontainer");
    if (listenerparent.length > 0) {
      listenerparent.siblings().removeClass("active");
      listenerparent.addClass("active");
    }
  }

  var nextpage = inlink.attr("href");
  if (!nextpage) {
    nextpage = inlink.data("nextpage");
  }

  var targetDiv = finddata(inlink, "targetdiv");
  var replaceHtml = true;

  var targetDivInner = finddata(inlink, "targetdivinner");
  if (targetDivInner) {
    targetDiv = targetDivInner;
    replaceHtml = false;
  }

  var useparent = inlink.data("useparent");

  if (inlink.hasClass("auto-active-link")) {
    var container = inlink.closest(".auto-active-container");

    jQuery(".auto-active-row", container).removeClass("current");
    var row = inlink.closest(".auto-active-row");
    row.addClass("current");

    jQuery("li", container).removeClass("current");
    var row = inlink.closest("li");
    row.addClass("current");
  }

  var options = $(inlink).data();
  if (options.isEmptyObject || $(inlink).data("findalldata")) {
    options = findalldata(inlink);
  }

  var inlinkmodal = inlink.closest(".modal");

  if (targetDiv) {
    inlink.css("cursor", "wait");
    $("body").css("cursor", "wait");

    targetDiv = targetDiv.replace(/\//g, "\\/");

    //before ajaxcall
    if (inlink.data("onbefore")) {
      var onbefore = inlink.data("onbefore");
      var fnc = window[onbefore];
      if (fnc && typeof fnc === "function") {
        //make sure it exists and it is a function
        fnc(inlink); //execute it
      }
    }

    jQuery
      .ajax({
        url: nextpage,
        data: options,
        success: function (data) {
          var cell;
          if (useparent && useparent == "true") {
            cell = $("#" + targetDiv, window.parent.document);
          } else {
            cell = findclosest(inlink, "#" + targetDiv);
          }
          var onpage;
          if (replaceHtml) {
            //Call replacer to pull $scope variables
            onpage = cell.parent();
            cell.replaceWith(data); //Cant get a valid dom element
          } else {
            onpage = cell;
            cell.html(data);
          }
          cell = findclosest(onpage, "#" + targetDiv);

          setPageTitle(cell);

          //on success execute extra JS
          if (inlink.data("onsuccess")) {
            var onsuccess = inlink.data("onsuccess");
            var fnc = window[onsuccess];
            if (fnc && typeof fnc === "function") {
              //make sure it exists and it is a function
              fnc(inlink); //execute it
            }
          }

		  $(window).trigger("checkautoreload", [inlink]);

          //actions after autoreload?
          var message = inlink.data("alertmessage");
          if (message) {
            $("#resultsmessages").append(
              '<div class="alert alert-success fader alert-save">' +
                message +
                "</div>"
            );
          }
        },
        type: "POST",
        dataType: "text",
        xhrFields: {
          withCredentials: true,
        },
        crossDomain: true,
      })
      .always(function () {
        var scrolltotop = inlink.data("scrolltotop");
        if (scrolltotop) {
          window.scrollTo(0, 0);
        }

        $(".ajaxprogress").hide();
        //inlink.css("enabled",true);
        inlink.removeAttr("disabled");

        //Close All Dialogs
        var closealldialogs = inlink.data("closealldialogs");
        if (closealldialogs) {
          closeallemdialogs();
        } else {
          //Close Dialog
          var closedialog = inlink.data("closedialog");
          if (closedialog && inlinkmodal != null) {
            closeemdialog(inlinkmodal);
          }
          //Close MediaViewer
          var closemediaviewer = inlink.data("closemediaviewer");
          if (closemediaviewer) {
            var overlay = $("#hiddenoverlay");
            if (overlay.length) {
              hideOverlayDiv(overlay);
            }
          }
        }
        //Close Navbar if exists
        var navbar = inlink.closest(".navbar-collapse");
        if (navbar) {
          navbar.collapse("hide");
        }

        $(window).trigger("resize");

      //  hideLoader();

        if (
          typeof global_updateurl !== "undefined" &&
          global_updateurl == false
        ) {
          //globaly disabled updateurl
        } else {
          var updateurl = inlink.data("updateurl");
          if (updateurl) {
            //console.log("Saving state ", updateurl);
            history.pushState($("#application").html(), null, nextpage);
          }
        }
      });
  }

  inlink.css("cursor", "");
  $("body").css("cursor", "");
}
}( jQuery ));

$(document).ready(function() 
{
	lQuery("a.ajax").livequery("click", function (e) {
  	e.stopPropagation();
  	e.preventDefault();
  	$(this).runajax();
  	return false;
  	});
});

