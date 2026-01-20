(function ($) {
	$.fn.emDialog = function (onsuccess = null, always = null) {
		var initiator = $(this);

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
						resultsdiv,
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
			var modalClass = initiator.data("modalclass") || "";
			jQuery("#application").append(
				`<div class="modal ${modalClass}" tabindex="-1" id="${id}" style="display:none"></div>`,
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

				var width = initiator.data("dialogwidth");
				if (!width) {
					width = initiator.data("width");
				}

				if (width) {
					if (typeof width === "string") {
						if (width.endsWith("%")) {
							width = $(window).width() * (parseInt(width) / 100);
						} else if (width.endsWith("px")) {
							width = parseInt(width);
						} else if (["sm", "md", "lg", "xl"].includes(width)) {
							var sizes = {
								sm: 300,
								md: 500,
								lg: 800,
								xl: 1200,
							};
							width = sizes[width];
						}
					}
					if (typeof width === "number") {
						width = Math.min(width, $(window).width() - 16);
						$(".modal-dialog", modaldialog).css("min-width", width + "px");
					}
				}

				var maxwidth = initiator.data("maxwidth");
				if (maxwidth) {
					$(".modal-dialog", modaldialog).css("max-width", maxwidth + "px");
				}
				var minwidth = initiator.data("minwidth");
				if (minwidth) {
					$(".modal-dialog", modaldialog).css("min-width", minwidth + "px");
				}

				var modalbackdrop = true;
				if ($(".modal-backdrop").length) {
					modalbackdrop = false;
				}

				var modalOptions = {
					closeExisting: false,
					show: true,
					backdrop: modalbackdrop,
					keyboard: false,
				};

				if (
					modaldialog.find(".htmleditor").length > 0 ||
					modaldialog.find(".htmleditor-advanced").length > 0
				) {
					modalOptions.focus = false;
				}

				var modalInstance = new bootstrap.Modal(modaldialog[0], modalOptions);
				modalInstance.show();

				$(document.body).addClass("modal-open");

				var justok = initiatorData["cancelsubmit"];
				if (justok != null) {
					$(".modal-footer #submitbutton", modaldialog).hide();
				} else {
					var id = $("form", modaldialog).attr("id");
					$("#submitbutton", modaldialog).attr("form", id);
				}

				var hidetitle = initiatorData["hideheader"];
				if (!hidetitle) {
					var title = initiator.data("dialogtitle");
					if (!title) {
						title = initiator.attr("title");
					}
					if (!title) {
						title = initiator.text();
					}
					if (title) {
						$(".modal-title", modaldialog).text(title);
					}
				}

				var hidefooter = initiatorData["hidefooter"];
				if (hidefooter) {
					$(".modal-footer", modaldialog).remove();
					$(".modal-body").css("padding-bottom", "6px");
				}

				//backup url
				var currenturl = window.location.href;
				modaldialog.data("oldurlbar", currenturl);

				searchpagetitle = modaldialog.find("[data-setpagetitle]");

				modaldialog.on("hidden.bs.modal", function () {
					//on close execute extra JS -- Todo: Move it to closedialog()
					if (initiatorData["onclose"]) {
						var onclose = initiatorData["onclose"];
						var fnc = window[onclose];
						if (fnc && typeof fnc === "function") {
							//make sure it exists and it is a function
							fnc(initiator); //execute it
						}
					}
					if (!$(this).hasClass("persistentmodal")) {
						closeemdialog($(this)); //Without this the asset Browse feature does not close all the way
					}

					$(window).trigger("resize");
				});

				// adjustZIndex(modaldialog);

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

		modaldialog.on("shown.bs.modal", function () {
			trackKeydown = true;
			var focuselement = modaldialog.data("focuson");
			if (focuselement) {
				//console.log(focuselement);
				var elmnt = document.getElementById(focuselement);
				elmnt.scrollIntoView();
			}
		});

		modaldialog.on("hide.bs.modal", () => {
			this.inert = true; // Makes everything in the modal non-interactive immediately
		});

		document.addEventListener("keydown", function (event) {
			if (event.key === "Escape") {
				// Find all currently open modals (those with the 'show' class)
				const openModals = document.querySelectorAll(".modal.show");
				//console.log(openModals);
				if (openModals.length > 0) {
					// Get the top-most modal (the last one in the list based on display order)
					const topModalEl = openModals[openModals.length - 1];

					// Get the Bootstrap modal instance for that element
					const modalInstance = bootstrap.Modal.getInstance(topModalEl);

					document.querySelectorAll(".modal").forEach((modalElement) => {
						modalElement.addEventListener("hide.bs.modal", () => {
							if (document.activeElement instanceof HTMLElement) {
								document.activeElement.blur(); //
							}
						});
					});

					// Hide only the top-most modal
					if (modalInstance) {
						modalInstance.hide();
						// Stop the event from propagating further to other modals
						event.stopPropagation();
						event.stopImmediatePropagation();
					}
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
	var dialogid = modaldialog.attr("id");
	var oldurlbar = modaldialog.data("oldurlbar");

	var modalInstance = bootstrap.Modal.getInstance(modaldialog[0]);
	modalInstance.hide();

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
	if (!modaldialog.hasClass("persistentmodal")) {
		setTimeout(function () {
			if (modaldialog) modaldialog.remove();
			onModalClosed(dialogid);
		}, 200);
	} else {
		onModalClosed(dialogid);
	}
};

function onModalClosed(dialogid) {
	if ($(".modal:visible").length === 0) {
		$(document.body).removeClass("modal-open");
		$(".modal-backdrop").remove();
	}
	$(window).trigger("modalclosed", [dialogid]);
}

lQuery(".modal-backdrop").livequery("click", function (e) {
	if ($(".modal:visible").length === 0) {
		$(this).remove();
		$(document.body).removeClass("modal-open");
	}
});

closeallemdialogs = function () {
	$(".modal").each(function () {
		var modaldialog = $(this);
		var modalInstance = bootstrap.Modal.getInstance(modaldialog[0]);
		modalInstance.hide();
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
	/*
	var zIndex = 100000;
	setTimeout(function () {
		var adjust = 0;
		if (element.find("modalmediaviewer")) {
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
	*/
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
		var clicked = $(this);
		if (clicked.hasClass("disableinpicker")) {
			var pickerresults = clicked.closest(".clickableresultlist");
			if (pickerresults.length > 0) {
				return;
			}
		}

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
