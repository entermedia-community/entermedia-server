(function ($) {
  $.fn.emDialog = function (onsuccess = null, always = null) {
    var initiator = $(this);

    var width = initiator.data("width");
    var maxwidth = initiator.data("maxwidth");
    var id = initiator.data("dialogid");
    if (!id) {
      id = "modals";
    }

    var options = initiator.cleandata();

    if (initiator.data("includeeditcontext") == true) {
      var editdiv = initiator.closest(".editdiv"); //This is used for lightbox tree opening
      if (editdiv.length > 0) {
        var otherdata = editdiv.cleandata();
        options = {
          ...otherdata,
          ...options,
        };
      } else {
        console.warn("No editdiv found for includeeditcontext");
      }
    }

    if (initiator.data("includesearchcontext") == true) {
      var editdiv = initiator.closest(".editdiv"); //This is used for lightbox tree opening
      var resultsdiv = editdiv.find(".resultsdiv");

      if (resultsdiv.length > 0) {
        if (resultsdiv.length > 1) {
          console.error(
            "Should not be finding more than one resultdiv ",
            resultsdiv
          );
        }
        var otherdata = resultsdiv.cleandata();
        options = {
          ...otherdata,
          ...options,
        };
      } else {
        console.warn("No resultsdiv found for includesearchcontext");
      }
    }

    var modaldialog = $("#" + id);
    if (modaldialog.length == 0) {
      jQuery("#application").append(
        '<div class="modal" tabindex="-1" id="' +
          id +
          '" style="display:none" ></div>'
      );
      modaldialog = jQuery("#" + id);
    }

    var link = initiator.data("url");

    if (link === undefined) {
      link = initiator.attr("href");
    }
    if (link === undefined) {
      link = initiator.data("targetlink");
    }
    var olddialog = initiator.closest(".modal");
    if (olddialog.length !== 0 && olddialog.attr("id") == id) {
      //Try and reuse the dialog
      var hasbackbutton = modaldialog.find(".enablebackbtn"); //Remove existing layouts if using backbutton
      if (hasbackbutton.length !== 0) {
        options.oemaxlevel = 1;
      }
    }

    //NOT USED. Delete
    var param = initiator.data("parameterdata");
    if (param) {
      var element = jQuery("#" + param);
      var name = element.prop("name");
      options[name] = element.val();
    }

    var searchpagetitle = "";

    $(window).trigger("showToast", [initiator]);
    var toastUid = initiator.data("uid");
    var initiatorData = initiator.data();
    var onsuccessFunc = onsuccess;
    var alwaysFunc = always;
    jQuery.ajax({
      xhrFields: {
        withCredentials: true,
      },
      crossDomain: true,
      url: link,
      data: { ...options },
      success: function (data) {
        $(window).trigger("successToast", toastUid);
        var targetdiv = modaldialog.find(".enablebackbtn");
        if (targetdiv.length == 0) {
          modaldialog.html(data);
        } else {
          //--Entities
          targetdiv.html(data);
        }

        if (onsuccessFunc) {
          onsuccessFunc();
        }

        if (width) {
          width = Math.min(width, $(window).width() - 16);
          $(".modal-dialog", modaldialog).css("min-width", width + "px");
        }
        if (maxwidth) {
          $(".modal-dialog", modaldialog).css("max-width", maxwidth + "px");
        }

        var modalbackdrop = true;
        if ($(".modal-backdrop").length) {
          modalbackdrop = false;
        }

        var modalinstance;
        var modalOptions = {
          closeExisting: false,
          show: true,
          backdrop: modalbackdrop,
          keyboard: false,
        };

        if (!window.bsVersion) {
          window.bsVersion = parseInt(bootstrap.Modal.VERSION);
        }

        if (
          modaldialog.find(".htmleditor").length > 0 ||
          modaldialog.find(".htmleditor-advanced").length > 0
        ) {
          modalOptions.focus = false;
        }

        if (window.bsVersion == 5) {
          var modIns = new bootstrap.Modal(modaldialog[0], modalOptions);
          modIns.show();
          modalinstance = $(modIns);
        } else {
          modalinstance = modaldialog.modal(modalOptions);
        }

        $(document.body).addClass("modal-open");
        /*  Use editdivid
				var autosetformtargetdiv = initiatorData["autosetformtargetdiv"];
				if (autosetformtargetdiv !== undefined) {
					var tdiv = initiator.closest("." + autosetformtargetdiv);
					if (tdiv.length == 1) {
						firstform.data("targetdiv", tdiv.attr("id"));
					}
				}
				*/
        // fix submit button
        var justok = initiatorData["cancelsubmit"];
        if (justok != null) {
          $(".modal-footer #submitbutton", modaldialog).hide();
        } else {
          var id = $("form", modaldialog).attr("id");
          $("#submitbutton", modaldialog).attr("form", id);
        }
        var hidetitle = initiatorData["hideheader"];
        if (hidetitle == null) {
          var title = initiator.attr("title");
          if (title == null) {
            title = initiator.text();
          }
          $(".modal-title", modaldialog).text(title);
        }
        var hidefooter = initiatorData["hidefooter"];
        if (hidefooter != null) {
          $(".modal-footer", modaldialog).hide();
        }

        //backup url
        var currenturl = window.location.href;
        modalinstance.data("oldurlbar", currenturl);

        searchpagetitle = modaldialog.find("[data-setpagetitle]");

        modalinstance.on("hidden.bs.modal", function () {
          //on close execute extra JS -- Todo: Move it to closedialog()
          if (initiatorData["onclose"]) {
            var onclose = initiatorData["onclose"];
            var fnc = window[onclose];
            if (fnc && typeof fnc === "function") {
              //make sure it exists and it is a function
              fnc(initiator); //execute it
            }
          }

          closeemdialog($(this)); //Without this the asset Browse feature does not close all the way
          $(window).trigger("resize");
        });

        adjustZIndex(modalinstance);

        if (
          typeof global_updateurl !== "undefined" &&
          global_updateurl == false
        ) {
          //globaly disabled updateurl
        } else {
          //Update Address Bar
          var updateurl = initiatorData["updateurl"];
          if (updateurl) {
            var urlbar = initiatorData["urlbar"];
            if (!urlbar) {
              urlbar = link;
            }

            history.pushState($("#application").html(), null, urlbar);
            window.scrollTo(0, 0);
          }
        }

        if (searchpagetitle) {
          $(window).trigger("setPageTitle", [searchpagetitle]);
        }

        //on success execute extra JS
        if (initiatorData["onsuccess"]) {
          var onsuccess = initiatorData["onsuccess"];
          var fnc = window[onsuccess];
          if (fnc && typeof fnc === "function") {
            //make sure it exists and it is a function
            fnc(initiator); //execute it
          }
        }

        $(window).trigger("resize");
      },
      error: function () {
        $(window).trigger("errorToast", toastUid);
      },
      complete: function () {
        if (alwaysFunc) {
          alwaysFunc();
        }
      },
    });

    $(modaldialog).on("shown.bs.modal", function () {
      trackKeydown = true;
      var focuselement = modaldialog.data("focuson");
      if (focuselement) {
        //console.log(focuselement);
        var elmnt = document.getElementById(focuselement);
        elmnt.scrollIntoView();
      }
    });

    // $(modaldialog).on("hidePrevented.bs.modal", function (e) {
    // 	var backUrl = $(modaldialog).find(".entityNavBack");
    // 	if (backUrl.length > 0) {
    // 		e.stopImmediatePropagation();
    // 		e.preventDefault();
    // 		backUrl.trigger("click");
    // 	}
    // });
    $(modaldialog).on("hide.bs.modal", function (e) {
      trackKeydown = false;
      if (!$(this).hasClass("onfront")) {
        e.stopPropagation();
        e.stopImmediatePropagation();
        return this;
      } else {
        if ($(".modal:visible").length > 0) {
          // restore the modal-open class to the body element, so that scrolling works
          // properly after de-stacking a modal.
          setTimeout(function () {
            $(document.body).removeClass("modal-open");
          }, 0);
        }
      }
    });

    //Close drodpown if exists
    if (initiator.closest(".dropdown-menu").length !== 0) {
      initiator.closest(".dropdown-menu").removeClass("show");
    }
    return this;
  };
})(jQuery);

closeemdialog = function (modaldialog) {
  var oldurlbar = modaldialog.data("oldurlbar");
  var dialogid = modaldialog.attr("id");

  if (window.bsVersion == 5) {
    var modIns = bootstrap.Modal.getInstance(modaldialog[0]);
    modIns.hide();
  } else {
    modaldialog.modal("hide");
  }
  modaldialog.remove();
  //other modals?
  var othermodal = $(".modal");
  if (othermodal.length && !othermodal.is(":hidden")) {
    adjustZIndex(othermodal);
  }

  modaldialog.find(".video-js, .video-player").each(function () {
    if (this.id) {
      videojs(this.id).dispose();
    }
  });

  $(window).trigger("setPageTitle", [othermodal]);

  if (oldurlbar !== undefined) {
    if (dialogid == "mediaviewer") {
      //history.replaceState(null, null, " ");
      oldurlbar = RemoveParameterFromUrl(oldurlbar, "assetid");
    }
    if (dialogid == "entitydialog") {
      //history.replaceState(null, null, " ");
      oldurlbar = RemoveParameterFromUrl(oldurlbar, "entityid");
    }

    history.pushState($("#application").html(), null, oldurlbar);
  }
  if ($(".modal:visible").length === 0) {
    $(document.body).removeClass("modal-open");
  }
};

closeallemdialogs = function () {
  $(".modal").each(function () {
    var modaldialog = $(this);
    if (window.bsVersion == 5) {
      var modIns = bootstrap.Modal.getInstance(this);
      modIns.hide();
    } else {
      modaldialog.modal("hide");
    }
    modaldialog.remove();
  });
  var overlay = $("#hiddenoverlay");
  if (overlay.length) {
    hideOverlayDiv(overlay);
  }
  $(document.body).removeClass("modal-open");
};

function RemoveParameterFromUrl(url, parameter) {
  return url
    .replace(new RegExp("[?&]" + parameter + "=[^&#]*(#.*)?$"), "$1")
    .replace(new RegExp("([?&])" + parameter + "=[^&]*&"), "$1");
}

function adjustZIndex(element) {
  var zIndex = 100000;
  setTimeout(function () {
    var adjust = 0;
    if (element.hasClass("modalmediaviewer")) {
      $(".modal:visible").css("z-index", zIndex);
      $(".modal:visible").off();
      $(".modal:visible").addClass("behind");
      $(".modal:visible").hide();
    } else {
      $(".modalmediaviewer").css("z-index", zIndex);
      $(".modal:visible").css("z-index", zIndex - 1); //reset others?
      $(".modal-backdrop")
        .not(".modal-stack")
        .css("z-index", zIndex - 2)
        .addClass("modal-stack");
    }
    adjust = 1 + 1 * $(".modal:visible").length;
    element.css("z-index", zIndex + adjust);
    $(".onfront").removeClass("onfront");
    element.show();
    element.addClass("onfront");

    //$(window).trigger("resize");
  });
}

$(document).ready(function () {
  lQuery("a.openemdialog").livequery(function (e) {
    e.preventDefault();
    e.stopPropagation();
    $(this).emDialog();
  });

  lQuery("a.entityupdateurl").livequery("click", function (e) {
    e.preventDefault();
    var dialogid = $(this).data("dialogid");
    if (dialogid == null) {
      $(this).data("dialogid", "entitydialog");
    }
    var entityid = $(this).data("entityid");
    var entitymoduleid = $(this).data("moduleid");

    var urlbar = `${apphome}/views/modules/${entitymoduleid}/index.html?entityid=${entityid}`;
    $(this).data("urlbar", urlbar);
    $(this).data("updateurl", true);
  });

  lQuery("a.emdialog").livequery("click", function (e) {
    e.preventDefault();
    e.stopPropagation();
    $(this).emDialog();
  });

  lQuery(".closemodal").livequery("click", function (e) {
    if (e.target != this) return;
    closeemdialog($(this).closest(".modal"));
  });

  lQuery("#closebutton").livequery("click", function () {
    closeemdialog($(this).closest(".modal"));
  });

  var urlHash = window.location.hash;
  if (urlHash.startsWith("#open")) {
    var id = urlHash.substring(6);
    var dialog = $(`#${id}`);
    if (dialog.length && dialog.hasClass("emdialog")) {
      dialog.emDialog();
    }
  }
});
