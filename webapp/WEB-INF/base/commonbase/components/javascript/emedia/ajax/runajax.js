(function ($) {
	$.fn.runAjax = function (successCallback = null) {
		var anchor = $(this);
		var confirmation = $(anchor).data("confirm");
		if (confirmation && !confirm(confirmation)) {
			return this;
		}

		anchor.attr("disabled", "disabled");

		if (anchor.hasClass("activelistener")) {
			$(".activelistener").removeClass("active");
			anchor.addClass("active");
			$(".activelistener").removeClass("selected");
			anchor.addClass("selected");
		}
		//for listeners in a container
		if (anchor.hasClass("activelistenerparent")) {
			var listenerParent = anchor.closest(".activelistcontainer");
			if (listenerParent.length > 0) {
				listenerParent.siblings().removeClass("active");
				listenerParent.addClass("active");
			}
		}

		var nextpage = anchor.attr("href");
		if (!nextpage) {
			nextpage = anchor.data("nextpage");
		}
		if (!nextpage && anchor.data("url")) {
			nextpage = anchor.data("url");
		}

		var options = anchor.cleandata();
		if (!options) options = {};
		var editdiv = anchor.closest(".editdiv"); //This is used for lightbox tree opening
		if (
			anchor.data("includeeditcontext") === undefined ||
			anchor.data("includeeditcontext") == true
		) {
			if (editdiv.length > 0) {
				var otherdata = editdiv.cleandata();
				options = {
					...otherdata,
					...options,
				};
			} else {
				//console.warn("No editdiv found for includeeditcontext");
			}
		}

		if (
			anchor.data("includesearchcontext") === undefined ||
			anchor.data("includesearchcontext") == true
		) {
			var editresultsdiv = editdiv.find(".resultsdiv");

			if (editresultsdiv.length > 0) {
				var otherdata = editresultsdiv.cleandata();
				options = {
					...otherdata,
					...options,
				};
			} else {
				//console.warn("No resultsdiv found for icludesearchcontext");
			}
		}

		var activemenu;
		if (anchor.hasClass("auto-active-link")) {
			activemenu = anchor;
		} else if (anchor.data("autoactivecontainer")) {
			activemenu = $("." + anchor.data("autoactivecontainer"));
		}
		if (activemenu !== undefined && activemenu.length > 0) {
			var container = activemenu.closest(".auto-active-container");
			if (container.length == 0) {
				container = activemenu.parent().parent();
			}

			jQuery(".auto-active-row", container).removeClass("current");
			var row = activemenu.closest(".auto-active-row");
			row.addClass("current");

			jQuery("li", container).removeClass("current");
			var row = activemenu.closest("li");
			row.addClass("current");
		}

		var anchorModal = anchor.closest(".modal");

		var replaceHtml = true;

		var targetdiv = anchor.data("selectedtargetdiv");

		var targetDivInner = anchor.data("targetdivinner");
		if (targetDivInner) {
			targetdiv = $("#" + $.escapeSelector(targetDivInner));
			replaceHtml = false;
		}

		var targetdivS = anchor.data("targetdiv");

		if (targetdiv == undefined || targetdiv.length == 0) {
			var edithomeid = options["edithomeid"];
			if (edithomeid !== undefined) {
				var parent = $("#" + edithomeid);
				if (parent.hasClass(targetdivS)) {
					targetdiv = parent;
				} else {
					targetdiv = parent.find("." + targetdivS);
				}
			} else {
				targetdiv = anchor.closest("." + $.escapeSelector(targetdivS));
			}
		}

		if (!targetdiv.length) {
			targetdiv = $("#" + $.escapeSelector(targetdivS));
		}
		if (!targetdiv.length) {
			targetdiv = $("." + $.escapeSelector(targetdivS)); //legacy?
		}

		//if (targetdiv.length) {
		anchor.css("cursor", "wait");
		$("body").css("cursor", "wait");

		//before ajaxcall
		if (anchor.data("onbefore")) {
			var onbefore = anchor.data("onbefore");
			var fnc = window[onbefore];
			if (fnc && typeof fnc === "function") {
				//make sure it exists and it is a function
				fnc(anchor); //execute it
			}
		}

		if (anchor.data("noToast") !== true) {
			$(window).trigger("showToast", [anchor]);
		}
		var toastUid = $(anchor).data("uid");

		//console.log("Run Ajax",nextpage, options);

		var anchorData = anchor.cleandata(); //anchor.data looses dynamically set data after ajax call, so we need to use this instead of anchor.data()
		if (!anchorData) anchorData = {};
		jQuery
			.ajax({
				url: nextpage,
				data: options,
				success: function (data) {
					$(window).trigger("successToast", toastUid);
					/*
						var cell;
						if (useparent && useparent == "true") {
							cell = $("#" + targetDiv, window.parent.document);
						} else {
							cell = findClosest(anchor, targetDiv);
						}*/
					var onpage;
					var newcell;
					if (replaceHtml) {
						//Call replacer to pull $scope variables
						onpage = targetdiv.parent();
						targetdiv.replaceWith(data); //Cant get a valid dom element
						newcell = findClosest(onpage, targetdiv);
					} else {
						onpage = targetdiv;
						targetdiv.html(data);
						newcell = onpage.children(":first");
					}
					$(window).trigger("setPageTitle", [newcell]);

					//on success execute extra JS
					if (anchorData["onsuccess"]) {
						var onsuccess = anchorData["onsuccess"];
						var fnc = window[onsuccess];
						if (fnc && typeof fnc === "function") {
							//make sure it exists and it is a function
							fnc(anchor); //execute it
						}
					}

					$(window).trigger("checkautoreload", [anchor]);

					if (successCallback) {
						successCallback();
					}

					//actions after autoreload?
					var message = anchorData["alertmessage"];
					if (message) customToast(message);
				},
				error: function () {
					$(window).trigger("errorToast", toastUid);
				},
				type: "POST",
				dataType: "text",
				xhrFields: {
					withCredentials: true,
				},
				crossDomain: true,
			})
			.always(function () {
				var scrolltotop = anchorData["scrolltotop"];
				if (scrolltotop) {
					window.scrollTo(0, 0);
				}
				//anchor.css("enabled",true);
				anchor.removeAttr("disabled");

				//Close All Dialogs
				var closealldialogs = anchorData["closealldialogs"];
				if (closealldialogs) {
					closeallemdialogs();
				} else {
					//Close Dialog
					var closedialog = anchorData["closedialog"];
					if (closedialog && anchorModal != null) {
						closeemdialog(anchorModal);
					}
					//Close MediaViewer
					var closemediaviewer = anchorData["closemediaviewer"];
					if (closemediaviewer) {
						var overlay = $("#hiddenoverlay");
						if (overlay.length) {
							hideOverlayDiv(overlay);
						}
					}
				}
				//Close Navbar if exists
				var navbar = anchor.closest(".navbar-collapse");
				if (navbar.length) {
					navbar.collapse("hide");
				}

				$(window).trigger("resize");

				anchor.css("cursor", "");
				$("body").css("cursor", "");

				var updateurl = anchorData["updateurl"];
				if (
					typeof global_updateurl !== "undefined" &&
					global_updateurl == false
				) {
					console.warn("Global updateurl is disabled.");
				} else {
					if (updateurl) {
						var url = anchorData["urlbar"] || nextpage;
						history.pushState($("#application").html(), null, url);
					}
				}
			});
		//}
		return this;
	};
})(jQuery);

$(document).ajaxError(function (e, jqXhr, settings, exception) {
	console.log(e, jqXhr, exception);
	if (exception == "abort") {
		return;
	}
	var err = "Error processing the request!";
	if (jqXhr.readyState == 0) {
		err = "Network error!";
	} else if (exception == "timeout") {
		err = "Request timed out!";
	}

	var errors =
		"Error: " +
		exception +
		"\n" +
		jqXhr.responseText +
		"\n URL: " +
		settings.url;
	console.error(errors);
	return;
});

var runAjaxOn = {};
var ajaxRunning = false;

var statusCallCount = {};
runAjaxStatus = function () {
	//for each asset on the page reload it's status
	//console.log(uid);

	for (const [uid, enabled] of Object.entries(runAjaxOn)) {
		if (uid) {
			if (statusCallCount[uid] === undefined) {
				statusCallCount[uid] = 0;
			} else {
				statusCallCount[uid]++;
			}
		}
		if (!enabled || enabled === undefined) {
			if (statusCallCount[uid]) {
				delete statusCallCount[uid];
			}
			continue;
		}
		var cell = $("#" + uid);

		if (cell.length == 0 || !cell.hasClass("ajaxstatus")) {
			delete statusCallCount[uid];
			continue;
		}

		if (!isInViewport(cell[0])) {
			delete statusCallCount[uid];
			continue;
		}

		// Warn if ajax status is called more than 10 times
		var WARN_CAP = 20;
		if (
			statusCallCount[uid] >= WARN_CAP &&
			statusCallCount[uid] % WARN_CAP == 0
		) {
			console.warn(
				"Ajax Status for " + uid + " ran " + statusCallCount[uid] + " times"
			);
		}

		var path = cell.attr("ajaxpath");
		if (!path || path == "") {
			path = cell.data("ajaxpath");
		}
		//console.log("Loading " + path );
		if (path && path.length > 1) {
			var entermediakey = "";
			if (app && app.data("entermediakey") != null) {
				entermediakey = app.data("entermediakey");
			}
			var data = cell.cleandata();
			jQuery.ajax({
				url: path,
				async: false,
				data: data,
				success: function (data) {
					cell.replaceWith(data);
					//$(window).trigger("checkautoreload", [cell]);
					$(window).trigger("resize");
				},
				xhrFields: {
					withCredentials: true,
				},
				crossDomain: true,
			});
		}
	}
	setTimeout("runAjaxStatus();", 1000); //Start checking any and all fields on the screeen that are saved in runAjaxOn
};

lQuery(".ajaxstatus").livequery(function () {
	var uid = $(this).attr("id");

	var iscomplete = $(this).data("ajaxstatuscomplete");

	if (iscomplete) {
		runAjaxOn[uid] = false;
	} else {
		var inqueue = runAjaxOn[uid];
		if (inqueue == undefined) {
			runAjaxOn[uid] = true; //Only load once per id
		}
	}
	if (!ajaxRunning) {
		setTimeout("runAjaxStatus();", 500); //Start checking then runs every second on all status
		ajaxRunning = true;
	}
});
