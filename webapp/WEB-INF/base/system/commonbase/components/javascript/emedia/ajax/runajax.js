findClosest = function (link, inid) {
	var result = link.closest(inid);
	if (result.length == 0) {
		result = link.find(inid);
		if (result.length == 0) {
			result = link.children(inid);
			if (result.length == 0) {
				result = $(inid);
			}
		}
	}
	return result.first();
};

(function ($) {
	$.fn.runAjax = function (successCallback = null) {
		var anchor = $(this);
		if (!anchor || anchor.length == 0) {
			console.warn("runAjax called on an empty element");
			return;
		}
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

		var removeOnSuccess = anchor.data("removeonsuccess");
		var removeTarget = anchor.data("removetarget");
		if (removeTarget) {
			if (!removeTarget.startsWith("#") && !removeTarget.startsWith(".")) {
				removeTarget = "#" + removeTarget;
			}
		}

		//for listeners in a container
		if (anchor.hasClass("activelistenerparent")) {
			var listenerParent = anchor.closest(".activelistcontainer");
			if (listenerParent.length > 0) {
				listenerParent.siblings().removeClass("active");
				listenerParent.addClass("active");
			}
		}

		var href = anchor.attr("href");
		if (!href) {
			href = anchor.data("nextpage");
		}
		if (!href) {
			href = anchor.data("url");
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
			if (row.length == 0) {
				container.find(".auto-active-link").removeClass("current");
				activemenu.addClass("current");
			} else {
				container.find(".auto-active-row").removeClass("current");
				row.addClass("current");
			}

			jQuery("li", container).removeClass("current");
			var row = activemenu.closest("li");
			row.addClass("current");
		}

		var anchorModal = anchor.closest(".modal");

		var targetappendtype = anchor.data("targetappendtype");
		if (!targetappendtype) {
			targetappendtype = "replace";
		}

		var targetdiv = anchor.data("selectedtargetdiv");

		var targetDivInner = anchor.data("targetdivinner");
		if (targetDivInner) {
			targetdiv = $("#" + $.escapeSelector(targetDivInner));
			targetappendtype = "children";
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

		if (targetdivS !== undefined && targetdiv.length == 0) {
			console.warn("Specified targetdiv doesn't exitst in the DOM tree");
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

		if (anchor.data("no-toast") !== true) {
			$(window).trigger("showToast", [anchor]);
		}
		var toastUid = $(anchor).data("uid");

		// console.log("Run Ajax", href, options);

		var anchorData = anchor.cleandata(); //anchor.data looses dynamically set data after ajax call, so we need to use this instead of anchor.data()
		if (!anchorData) anchorData = {};

		if (options.targetdiv || options.targetdivinner) {
			if (!options.oemaxlevel && !options.oemaxlayout) {
				var searchStart = href.indexOf("?");
				if (searchStart > -1) {
					var search = href.substr(searchStart + 1);
					var searchParams = new URLSearchParams(search);
					options.oemaxlevel = searchParams.get("oemaxlevel");
					options.oemaxlayout = searchParams.get("oemaxlayout");
				}
			}
			if (!options.oemaxlevel && !options.oemaxlayout) {
				options.oemaxlevel = 1;
				delete options.oemaxlayout;
			}
		}

		jQuery
			.ajax({
				url: href,
				data: {
					...options,
				},
				success: function (data) {
					$(window).trigger("successToast", [toastUid]);
					/*
						var cell;
						if (useparent && useparent == "true") {
							cell = $("#" + targetDiv, window.parent.document);
						} else {
							cell = findClosest(anchor, targetDiv);
						}*/
					var onpage;
					var newcell;
					if (targetappendtype == "replace") {
						//Call replacer to pull $scope variables
						onpage = targetdiv.parent();
						var targetdivid = "";
						if (targetdiv.attr("id") !== undefined) {
							targetdivid = "#" + targetdiv.attr("id");
						}
						targetdiv.replaceWith(data); //Cant get a valid dom element
						newcell = findClosest(onpage, targetdivid);
					} else if (targetappendtype == "children") {
						onpage = targetdiv;
						targetdiv.html(data);
						newcell = onpage.children(":first");
					} else if (targetappendtype == "insertbefore") {
						onpage = targetdiv;
						$(data).insertBefore(targetdiv);
						newcell = onpage.children(":first");
					} else if (targetappendtype == "append") {
						onpage = targetdiv;
						targetdiv.append(data);
						newcell = onpage.children(":last");
					}
					if (newcell.length > 0) {
						$(window).trigger("setPageTitle", [newcell]);
					}

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
					if (message && window.customToast) {
						window.customToast(message);
					}

					if (removeOnSuccess !== undefined && removeOnSuccess == "true") {
						if (removeTarget) {
							$(removeTarget).remove();
						} else {
							anchor.remove();
						}
					}
				},
				error: function () {
					$(window).trigger("errorToast", [toastUid]);
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
						var url = anchorData["urlbar"] || href;
						history.pushState($("#application").html(), null, url);
					}
				}
			});
		//}
		return this;
	};
})(jQuery);

$(document).ready(function () {
	$(document).ajaxError(function (e, jqXhr, settings, exception) {
		console.log(e, jqXhr, exception);
		var err = "An error occurred while processing the request!";
		if (exception == "timeout") {
			err = "Request timed out!";
		}
		if (exception == "abort" || jqXhr.readyState == 0) {
			if (jqXhr.readyState == 0) {
				err = "Request aborted by user or another script.";
			}
			if (!navigator.onLine) {
				err = "Network error! Please check your network connection.";
			} else if (exception == "abort") {
				err = "Request aborted by user or another script.";
			}
			console.log(err);
			return;
		}

		if (window.customToast) window.customToast(err, { positive: false });
		var errors = "Error details: ";
		if (exception) {
			errors += "\n\tException: " + exception;
		}
		if (jqXhr.responseText) {
			errors += "\n\tResponse: " + jqXhr.responseText;
		}
		if (settings.url) {
			errors += "\n\tURL: " + settings.url;
		}
		console.error(errors);
	});

	var WARN_CAP = 20;
	var runAjaxOn = {}; // queue of ajax status to run
	var ajaxRunning = false;

	runAjaxStatus = function () {
		//for each asset on the page reload it's status
		//console.log(uid);

		for (const [uid, config] of Object.entries(runAjaxOn)) {
			var enabled = config.enabled;
			var interval = config.interval || 1000;
			var lastRun = config.lastRun || 0;
			var totalRun = config.totalRun || 0;
			if (!enabled) {
				continue;
			}

			var now = new Date().getTime();
			if (lastRun > 0 && now - lastRun < interval) {
				continue;
			}

			config.lastRun = now;

			var cell = $("#" + uid);

			if (cell.length == 0 || !cell.hasClass("ajaxstatus")) {
				continue;
			}

			var alwaysrun = cell.data("runwhenhidden");
			if (alwaysrun == undefined) {
				if (!isInViewport(cell[0])) {
					continue;
				}
			}

			// Warn if ajax status is called more than 10 times
			if (totalRun >= WARN_CAP && totalRun % WARN_CAP == 0) {
				console.warn("Ajax Status for " + uid + " ran " + totalRun + " times");
			}

			var path = cell.attr("ajaxpath");
			if (!path || path == "") {
				path = cell.data("ajaxpath");
			}
			//console.log("Loading " + path );
			if (path && path.length > 1) {
				var data = cell.cleandata();
				if (data.targetdiv || data.targetdivinner) {
					if (!data.oemaxlevel && !data.oemaxlayout) {
						data.oemaxlevel = 1;
					}
				}
				config.totalRun = totalRun + 1;
				jQuery.ajax({
					url: path,
					async: false,
					data: {
						...data,
					},
					success: function (data) {
						cell.replaceWith(data);
						$(window).trigger("resize");
					},
					xhrFields: {
						withCredentials: true,
					},
					crossDomain: true,
				});
			}
		}
		setTimeout(runAjaxStatus, 1000); //Start checking any and all fields on the screeen that are saved in runAjaxOn
	};

	lQuery(".ajaxstatus").livequery(function () {
		var uid = $(this).attr("id");
		var interval = $(this).data("interval");
		if (!interval) {
			interval = 1000;
		}

		var iscomplete = $(this).data("ajaxstatuscomplete");

		if (iscomplete) {
			runAjaxOn[uid] = { enabled: false };
		} else {
			var inqueue = runAjaxOn[uid];
			if (inqueue == undefined) {
				runAjaxOn[uid] = {
					enabled: true,
					interval: interval,
					lastRun: 0,
					totalRun: 0,
				};
			}
		}
		if (!ajaxRunning) {
			setTimeout(runAjaxStatus, 500); //Start checking then runs every second on all status
			ajaxRunning = true;
		}
	});

	var singleAjaxReqs = {};
	lQuery(".ajaxonce").livequery(function () {
		/**
		 * Runs an ajax call on elements with class .ajaxonce
		 * It will abort any previous ajax request that has the same id as the element
		 */
		var uid = $(this).attr("id");
		if (!uid) {
			console.warn(".ajaxonce must have an id.");
			return;
		}
		if (singleAjaxReqs[uid] !== undefined) {
			singleAjaxReqs[uid].abort();
			console.log("Aborting previous ajax request for " + uid);
		}
		var cell = $(this);
		var showloader = cell.data("showloader");
		if (showloader) {
			cell.append(`<div class="ajax-loader">
				<i class="fa fa-spinner fa-spin"></i>
			</div>`);
		}

		var data = cell.cleandata();

		if (!data.oemaxlevel && !data.oemaxlayout) {
			data.oemaxlevel = 1;
		}

		var path = cell.data("ajaxpath");
		singleAjaxReqs[uid] = jQuery.ajax({
			url: path,
			data: {
				...data,
			},
			success: function (data) {
				cell.replaceWith(data);
				$(window).trigger("resize");
			},
			complete: function () {
				delete singleAjaxReqs[uid];
			},
			xhrFields: {
				withCredentials: true,
			},
			crossDomain: true,
		});
	});

	lQuery("a.ajax").livequery("click", function (e) {
		e.stopPropagation();
		e.preventDefault();
		$(this).runAjax();
	});

	lQuery("a.toggleAjax").livequery("click", function (e) {
		/**
		 * Runs an ajax call and removes the element from the DOM on ajax success
		 * Optionally checks for a focus parent
		 **/
		e.stopPropagation();
		e.preventDefault();
		var $this = $(this);
		$this.data("no-toast", true);
		$this.runAjax(function () {
			var focusParent = $this.closest(`.${$this.data("focusparent")}`);
			if (focusParent.length) {
				focusParent.find("input:visible:first").focus();
			}
			$this.remove();
		});
	});

	autoreload = function (div, callback, classname = null) {
		var url = div.data("autoreloadurl");
		if (url !== undefined) {
			div.data("url", url);
		}
		url = div.data("url");
		if (url != undefined) {
			var targetdiv = div.data("targetdiv");
			if (targetdiv == undefined) {
				div.data("targetdiv", classname); //Save to ourself
				div.data("oemaxlevel", 1);
			}
			div.data("no-toast", true);

			div.runAjax(function () {
				if (callback !== undefined && callback != null) {
					callback();
				}
				jQuery(window).trigger("resize");
			});
		}
	};

	// Call this way	$(window).trigger("autoreload", [indiv,callback,targetdiv]);
	$(window).on("autoreload", function (event, indiv, callback, targetdiv) {
		autoreload($(indiv), callback, targetdiv);
	});

	lQuery(".refreshautoreload").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		$(window).trigger("checkautoreload", [$(this)]);
	});

	// Call this way	$(window).trigger("checkautoreload", [form]);
	$(window).on("checkautoreload", function (event, indiv) {
		var classes = indiv.data("ajaxreloadtargets"); //assetresults, projectpage, sidebaralbums
		if (classes) {
			var splitnames = classes.split(",");
			$.each(splitnames, function (index, classname) {
				classname = $.trim(classname);
				$("." + classname).each(function (index, div) {
					autoreload($(div), null, classname);
				});
			});
		} else {
			var element = indiv.data("ajaxreloadtargetid"); //For single reload
			if (element) {
				autoreload($("#" + element), null, null);
			}
		}
	});

	//Sets Page title on ajax calls, needs a setpagetitle data set in the targetdiv
	$(window).on("setPageTitle", function (event, inElement) {
		var element = inElement;
		//entitytabs:
		//search parent #entitypreviewdialog-body then search .entitydialog
		var isentitydialog = $(element).closest("#entitypreviewdialog-body");
		if (isentitydialog.length > 0) {
			element = isentitydialog.find(".entitydialog");
		}

		if (element === undefined || $(element).data("setpagetitle") == null) {
			element = $("#applicationcontent");
		}
		if (element === undefined || $(element).data("setpagetitle") == null) {
			element = $("#application");
		}
		var setpagetitle = $(element).data("setpagetitle");

		if (setpagetitle != null && inElement.data("addtopagetitle") != null) {
			setpagetitle = setpagetitle + " - " + inElement.data("addtopagetitle");
		}

		var titlepostfix = $("#application").data("titlepostfix");
		var title = "";
		if (setpagetitle) {
			title = setpagetitle;
		}
		if (titlepostfix) {
			title = title ? title + " - " + titlepostfix : titlepostfix;
		}
		document.title = title;
	});
}); //document ready
