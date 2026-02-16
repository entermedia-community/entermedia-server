var trackKeydown = false;
var exitWarning = false;

function isInViewport(cell) {
	const rect = cell.getBoundingClientRect();
	var isin =
		rect.top >= 0 &&
		rect.top <= (window.innerHeight || document.documentElement.clientHeight);
	return isin;
}
function isValidTarget(clickEvent) {
	var target = $(clickEvent.target);
	if (
		target.attr("noclick") == "true" ||
		target.is("input") ||
		target.is("a") ||
		target.closest(".jp-audio").length
	) {
		return false;
	}

	clickEvent.preventDefault();
	clickEvent.stopImmediatePropagation();

	return true;
}

jQuery(document).ready(function () {
	$(window).on("keydown", function (e) {
		if (trackKeydown) {
			exitWarning = true;
		} else {
			exitWarning = false;
		}
	});

	lQuery(".uipanel").livequery(function () {
		$(this).addClass("ui-widget");
		var header = $(this).attr("header");
		if (header != undefined) {
			// http://dev.$.it/ticket/9134
			$(this).wrapInner('<div class="ui-widget-content"/>');
			$(this).prepend('<div class="ui-widget-header">' + header + "</div>");
		}
	});

	function confirmModalClose(modal) {
		var checkForm = modal.find("form.checkCloseDialog");
		if (!checkForm) {
			closeemdialog(modal);
			trackKeydown = false;
			return true;
		} else {
			var prevent = false;
			$(checkForm)
				.find("input, textarea, select")
				.each(function () {
					if ($(this).attr("type") == "hidden") {
						return true;
					}
					var value = $(this).val();
					if (value) {
						prevent = value.length > 0;
						return false;
					}
				});

			if (prevent && exitWarning) {
				$("#exitConfirmationModal").css("display", "flex");
				return false;
			} else {
				closeemdialog(modal);
				trackKeydown = false;
				return true;
			}
		}
	}
	var defaultPopperConfig = {
		strategy: "fixed",
		modifiers: [
			{
				name: "computeStyles",
				options: {
					gpuAcceleration: true,
					adaptive: false,
				},
			},
			{
				name: "preventOverflow",
				options: {
					boundary: "viewport",
					altAxis: true,
				},
			},
			{
				name: "flip",
				options: {
					fallbackPlacements: ["bottom", "top", "right", "left"],
					flipVariations: true,
				},
			},
		],
	};

	var drops = [
		".dropdown",
		".dropup",
		".dropstart",
		".dropend",
		".dropupstart",
		".dropupend",
		".dropdown-center",
		".dropdown-submenu",
	];

	drops.forEach(function (drop) {
		lQuery(drop).livequery("mouseenter", function () {
			var dropdownToggleEl = $(this).find(
				'[data-bs-toggle="dropdown"], .dropdown-toggle',
			)[0];
			var dropdown = new bootstrap.Dropdown(dropdownToggleEl, {
				popperConfig: defaultPopperConfig,
			});
			dropdown.show();
		});

		lQuery(drop).livequery("mouseleave", function () {
			var dropdownToggleEl = $(this).find(
				'[data-bs-toggle="dropdown"], .dropdown-toggle',
			)[0];
			var dropdown = bootstrap.Dropdown.getOrCreateInstance(dropdownToggleEl);
			dropdown.hide();
		});
	});

	lQuery("form.checkCloseDialog").livequery(function () {
		trackKeydown = true;
		var modal = $(this).closest(".modal");
		if (modal.length) {
			if (typeof modal.modal == "function") {
				modal.modal({
					backdrop: "static",
					keyboard: false,
				});
			}
			modal.on("mousedown", function (e) {
				e.stopPropagation();
				e.stopImmediatePropagation();
				if (e.currentTarget === e.target) {
					confirmModalClose(modal);
				}
			});
		}
	});

	lQuery("#closeExit").livequery("click", function () {
		$("#exitConfirmationModal").hide();
	});
	lQuery("#confirmExit").livequery("click", function () {
		$("#exitConfirmationModal").hide();
		closeallemdialogs();
		trackKeydown = false;
	});

	//Remove this? Not useing ajax
	$(document).on("mousedown", ".modal", function (e) {
		if (e.target.classList.contains("modal")) {
			e.stopPropagation();
			e.stopImmediatePropagation();
			confirmModalClose($(this));
		}
	});

	lQuery(".entityclose").livequery("mousedown", function (event) {
		event.preventDefault();
		var targetModal = $(this).closest(".modal");
		confirmModalClose(targetModal);
	});

	lQuery("#checkallcustomizations").livequery("change", function (event) {
		event.preventDefault();
		var checked = $(this).is(":checked");
		if (checked) {
			$(".customizationcheckbox").prop("checked", true);
		} else {
			$(".customizationcheckbox").prop("checked", false);
		}
	});

	$(document).keydown(function (e) {
		switch (e.which) {
			case 27: //esckey
				var modal = $(".modal.onfront");
				if (modal.length) {
					e.stopPropagation();
					e.preventDefault();
					var backBtn = modal.find(".entityNavBack");
					var checkForm = modal.find("form.checkCloseDialog");
					if (backBtn.length && backBtn.is(":visible")) {
						backBtn.trigger("click");
					} else if (checkForm.length) {
						confirmModalClose(modal);
					} else {
						closeemdialog(modal);
						trackKeydown = false;
					}
				}
				return;
			default:
				return; // exit this handler for other keys
		}
	});

	lQuery(".entityNavHistory").livequery(function () {
		var history = [];
		$(".entityNavHistory").each(function () {
			history.push($(this).data());
		});
		var link = $(".entityNavBack");
		var currentLinkIdx = history.findIndex(function (d) {
			return d.entityid == link.data("entityid");
		});
		if (currentLinkIdx > 0) {
			var backLink = history[currentLinkIdx - 1];
			if (backLink.entityid !== undefined && backLink.entityid != "") {
				link.data("entityid", backLink.entityid);
				link.data("entitymoduleid", backLink.entitymoduleid);
				//link.data("entitymoduleviewid", backLink.entitymoduleviewid);
				link.data("url", backLink.url);
				link.attr("href", backLink.url);
				link.show();

				var parententityheader = link
					.closest(".entity-header")
					.find(".parententityheader");
				if (parententityheader) {
					parententityheader.css("display", "flex");
				}

				var entitymodulelabel = link
					.closest(".entity-header")
					.find(".entitymodulelabel");
				entitymodulelabel.hide();
			}
		}
	});

	lQuery(".entity-tab-content.overflow-x").livequery("scroll", function (e) {
		if (e.shiftKey) {
			if (e.originalEvent.deltaY > 0) {
				e.preventDefault();
				$(this).scrollLeft($(this).scrollLeft() - 100);
			} else {
				e.preventDefault();
				$(this).scrollLeft($(this).scrollLeft() + 100);
			}
		}
	});

	lQuery(".indexDocPages").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var btn = $(this);
		btn.addClass("loading");
		btn.runAjax(function () {
			btn.parent().remove();
			customToast("Document split and pages queued for indexing");
		});
	});

	function formsavebackbutton(form) {
		var savedcontainer = $(".enablebackbtn");
		if (savedcontainer.length) {
			var parent = savedcontainer.parent().closest(".entitydialog");
			tabbackbutton(parent);
		}
	}

	function tabbackbutton(parent) {
		var parentid = "";
		if (parent.length) {
			parentid = parent.data("entityid");
		}

		if (parentid != "") {
			//var container = parent.find('.entitydialog');
			var backbtn = $(".entitydialogback");
			//$(backbtn).show();
			$(backbtn).data("parentid", parentid);
			$(backbtn).data("parentcontainerid", parent.attr("id"));
			$(backbtn).data("urlbar", parent.data("urlbar"));
		}
	}

	lQuery(".autoopen").livequery(function () {
		var link = $(this);
		link.emDialog();
	});

	window.onhashchange = function () {
		$("body").css({ overflow: "visible" }); //Enable scroll
		$(window).trigger("resize");
	};

	function copyTextToClipboard(elem, text, cb) {
		try {
			if ("clipboard" in navigator) {
				navigator.clipboard.writeText(text);
			} else {
				var rm = false;
				if (!elem) {
					elem = document.createElement("textarea");
					elem.value = text;
					document.body.appendChild(elem);
					rm = true;
				}
				elem.focus();
				elem.select();
				document.execCommand("copy");
				if (rm) {
					document.body.removeChild(elem);
				}
			}
			if (cb) {
				cb();
			}
		} catch (err) {
			console.log(err);
		}
	}

	lQuery(".copytoclipboard").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var btn = $(this);
		var textElement = null;
		var textToCopy = btn.data("text");

		if (!textToCopy) {
			var selectid = btn.data("textsource");
			textToCopy = $("#" + selectid).val();
			textElement = $("#" + selectid);
		}
		if (!textToCopy) {
			var copyTextTarget = btn.data("copytarget");
			if (copyTextTarget) {
				textToCopy = $("#" + copyTextTarget).val();
				if (!textToCopy) {
					textToCopy = $("#" + copyTextTarget).text();
				} else {
					textElement = $("#" + copyTextTarget);
				}
			}
		}

		if (!textToCopy) {
			return;
		}

		copyTextToClipboard(textElement, textToCopy, function () {
			customToast("Copied to clipboard!");
			var btnHtm = btn.html();
			var _btnHtm = btnHtm;
			btnHtm = btnHtm.replace("bi-copy", "bi-check-lg");
			btnHtm = btnHtm.replace("fa-copy", "fas fa-check");
			btnHtm = btnHtm.replace("Copy", "Copied");
			btn.html(btnHtm);
			setTimeout(() => {
				btn.html(_btnHtm);
			}, 2500);
		});
	});

	lQuery(".downloadtext").livequery("click", function (e) {
		var selectid = $(this).data("textsource");
		var textToDownload = $("#" + selectid).val();
		if (!textToDownload) {
			return;
		}
		var mime = $(this).data("mime") || "text/plain";
		var ext = $(this).data("ext") || "txt";
		const blob = new Blob([textToDownload], { type: mime });
		const url = URL.createObjectURL(blob);
		const a = document.createElement("a");
		a.href = url;
		a.download = selectid + "." + ext;
		document.body.appendChild(a);
		a.click();
		document.body.removeChild(a);
		URL.revokeObjectURL(url);
	});

	lQuery(".copyFromTarget").livequery("click", function (e) {
		e.preventDefault();
		if ("clipboard" in navigator) {
			var $this = $(this);
			var type = $this.data("type") || "text";
			var btnText = $this.text();
			var target = $this.data("target");
			if (!target) return;
			var targetEl = $("#" + target);
			if (type == "text") {
				var data = targetEl.text();
				if (targetEl.is("input") || targetEl.is("textarea")) {
					data = targetEl.val();
				}
				navigator.clipboard.writeText(data);
				$this.html('<i class="bi bi-check-lg me-1"></i> Copied!');
				setTimeout(() => {
					$this.html('<i class="bi bi-clipboard me-1"></i> ' + btnText);
				}, 2000);
			} else {
				if (!targetEl.is("img")) {
					targetEl = targetEl.find("img");
				}
				var dataURL = targetEl.attr("src");
				if (!dataURL) {
					return;
				}
				$this.html('<i class="fas fa-spinner fa-spin"></i>');
				fetch(dataURL)
					.then((res) => res.blob())
					.then((blob) => {
						navigator.clipboard.write([
							new ClipboardItem({
								"image/png": blob,
							}),
						]);
					})
					.then(() => {
						$this.html('<i class="bi bi-check-lg"></i> Copied!');
						setTimeout(() => {
							$this.html('<i class="bi bi-clipboard"></i> ' + btnText);
						}, 2000);
					})
					.catch(() => {
						$this.html('<i class="bi bi-clipboard"></i> ' + btnText);
					});
			}
		} else {
			alert("Clipboard API not supported, please use a modern browser.");
			return;
		}
	});

	lQuery(".addnewwithai").livequery("click", function (e) {
		e.preventDefault();
		$(".createnewtoggle").toggle();
		//$(this).hide(); //One time only?
	});

	lQuery(".trim-text").livequery(function (e) {
		var text = $(this).text();
		var check = $(this).closest(".entitymetadatamodal");
		if (check.length > 0) {
			text = text.replace(/</g, "&lt;");
			text = text.replace(/>/g, "&gt;");
			text = text.replace(/(\r\n|\n|\r)/gm, "<br>");
			$(this).html(text);
			return;
		}
		$(this).click(function (e) {
			if (
				e.target.classList.contains("see-more") ||
				e.target.classList.contains("see-less")
			) {
				e.stopPropagation();
			}
		});
		var maxLength = $(this).data("max");
		if (text.length <= maxLength) return;
		var minimizedText = text.substring(0, maxLength).trim();
		minimizedText = minimizedText.replace(/</gm, "&lt;");
		minimizedText = minimizedText.replace(/>/gm, "&gt;");
		minimizedText = minimizedText.replace(/(\r\n|\n|\r)/gm, "<br>");
		$(this).html(minimizedText);
		$(this).data("text", text);
		var btn = $(this).parent().find(".see-more");
		btn.html(btn.data("seemore"));
	});

	lQuery(".see-more-btn").livequery("click", function (e) {
		e.preventDefault();
		e.stopImmediatePropagation();
		var textParent = $(this).prev(".trim-text");
		var text = textParent.data("text");
		if (!text) {
			$(this).remove();
			return;
		}
		if ($(this).hasClass("see-more")) {
			text = text.replace(/</gm, "&lt;");
			text = text.replace(/>/gm, "&gt;");
			textParent.html(text.replace(/(\r\n|\n|\r)/gm, "<br>"));
			//textParent.append('<button class="see-less">(...see less)</button>');
			$(this).removeClass("see-more").addClass("see-less");
			$(this).html($(this).data("seeless"));
		} else {
			var maxLength = textParent.data("max");
			if (!maxLength || !text) {
				$(this).remove();
				return;
			}
			var minimizedText = text.substring(0, maxLength).trim();
			minimizedText = minimizedText.replace(/<br>/gm, "\n");
			minimizedText = minimizedText.replace(/</gm, "&lt;");
			minimizedText = minimizedText.replace(/>/gm, "&gt;");
			minimizedText = minimizedText.replace(/(\r\n|\n|\r)/gm, "<br>");
			textParent.html(minimizedText);
			$(this).removeClass("see-less").addClass("see-more");
			$(this).html($(this).data("seemore"));
		}
	});

	lQuery(".see-more-tags-btn").livequery("click", function (e) {
		e.preventDefault();
		e.stopImmediatePropagation();
		var tagsParent = $(this).prev(".tageditor-viewer");
		if ($(this).hasClass("see-more")) {
			$(".seelesstags", tagsParent).css("display", "inline-block");
			$(this).removeClass("see-more").addClass("see-less");
			$(this).html($(this).data("seeless"));
		} else {
			$(".seelesstags", tagsParent).hide();
			$(this).removeClass("see-less").addClass("see-more");
			$(this).html($(this).data("seemore"));
		}
	});

	lQuery(".filtersshowmoretags").livequery("click", function (e) {
		e.preventDefault();
		e.stopImmediatePropagation();
		var moretags = $(this).next(".moreoptions");
		moretags.show();
		$(this).hide();
	});

	lQuery("#filterautorefresh").livequery("change", function () {
		toggleUserProperty("filtershowall");
	});

	lQuery(".sidetoggle").livequery("click", function () {
		var div = $(this);
		var target = $(this).data("target");
		var toggle = $(this).data("toggle");
		if (!toggle) {
			toggle = target;
		}
		$("#" + target).slideToggle("fast", function () {
			div.find(".caret").toggleClass("exp");
			div.toggleClass("expanded");
			div.toggleClass("minimized");
			div.find(".component-actions").toggle();
			$(window).trigger("resize");
		});
		toggleUserProperty("minimize" + toggle, null, function () {
			// revert if failed
			$("#" + target).slideToggle("fast", function () {
				div.find(".caret").addClass("exp");
				div.removeClass("expanded");
				div.addClass("minimized");
				div.find(".component-actions").toggle();
				$(window).trigger("resize");
			});
		});
	});

	lQuery(".summary-toggler").livequery("click", function (e) {
		var toggler = $(this);

		var resultsdiv = toggler.closest(".resultsdiv");

		var container = $(".summary-container", resultsdiv);
		var isminimized = true;

		//Refresh the UI quickly
		if (container.length == 0 || container.hasClass("closed")) {
			isminimized = true;
			container.removeClass("closed");
			$(".summary-opener", resultsdiv).addClass("closed"); //hide the button
			container.removeClass("closed");
		} else {
			isminimized = false;
			container.addClass("closed");
			$(".summary-opener", resultsdiv).removeClass("closed");
		}
		setTimeout(() => {
			$(window).trigger("resize");
		}, 210); //match the transition speed of summary sidebar 200ms

		var preferencename = toggler.data("preferencename");
		var url = resultsdiv.data("searchhome");
		resultsdiv.data("url", url + "/changeminimizefilter.html");

		var toggle = !isminimized;
		resultsdiv.data("profilepreference.value", toggle);
		resultsdiv.data("profilepreference", preferencename);
		if (isminimized) {
			resultsdiv.data("targetdiv", resultsdiv.attr("id"));
			resultsdiv.data("oemaxlevel", 1);
		} else {
			resultsdiv.data("targetdiv", "null");
			resultsdiv.data("oemaxlevel", 0);
		}
		resultsdiv.runAjax();
	});

	lQuery(".sidebar-toggler").livequery("click", function (e) {
		e.preventDefault();
		e.stopImmediatePropagation();
		var toggler = $(this);
		var options = toggler.data();
		var targetdiv = toggler.data("targetdiv");
		var sidebar = toggler.data("sidebar");
		options["propertyfield"] = "sidebarcomponent";
		var url = toggler.attr("href");

		if (toggler.data("action") == "home") {
			options["sidebarcomponent.value"] = "";
			options["sidebarcomponent"] = "home";

			jQuery.ajax({
				url: url,
				async: false,
				data: {
					...options,
				},
				success: function (data) {
					//data = $(data);
					var cell = findClosest(toggler, "#" + targetdiv);
					cell.replaceWith(data); //Cant get a valid dom element
					$(".pushcontent").removeClass("pushcontent-" + sidebar);
					$(".pushcontent").removeClass("pushcontent-open");
					$(".pushcontent").addClass("pushcontent-fullwidth");

					$(window).trigger("setPageTitle", [cell]);

					$(window).trigger("resize");

					history.pushState($("#application").html(), null, url);
				},
				xhrFields: {
					withCredentials: true,
				},
				crossDomain: true,
			});
		} else if (toggler.data("action") == "hide") {
			$(".pushcontent").removeClass("pushcontent-" + sidebar);
			$(".pushcontent").removeClass("pushcontent-open");
			$(".pushcontent").addClass("pushcontent-fullwidth");
			$(".col-mainsidebar").remove();
			$(window).trigger("resize");

			var drawer = $(".drawer.open");
			if (drawer.length) {
				closeDrawer(drawer);
			}

			options["module"] = $("#applicationcontent").data("moduleid");
			options["sidebarcomponent.value"] = "";

			var url = apphome + "/components/sidebars/index.html";
			setTimeout(() => {
				jQuery.ajax({
					url: url,
					async: false,
					data: {
						...options,
					},
				});
			});
		} else {
			//showsidebar
			showsidebar(toggler);
		}
	});

	showsidebar = function (toggler) {
		var options = toggler.data();
		var targetdiv = toggler.data("targetdiv");
		var sidebar = toggler.data("sidebar");
		options["propertyfield"] = "sidebarcomponent";
		var moduleid = $("#applicationcontent").data("moduleid");
		options["module"] = moduleid;
		options["sidebarcomponent.value"] = sidebar;
		var url;
		if (options["contenturl"] != undefined) {
			url = options["contenturl"];
			targetdiv = $("#" + targetdiv);
		} else {
			if (moduleid !== undefined) {
				url = `${apphome}/views/modules/${moduleid}/components/sidebars/index.html`;
			} else {
				url = apphome + "/components/sidebars/index.html";
			}
			targetdiv = findClosest(toggler, "#" + targetdiv);
		}

		jQuery.ajax({
			url: url,
			async: false,
			data: {
				...options,
			},
			success: function (data) {
				targetdiv.replaceWith(data); //Cant get a valid dom element
				$(".pushcontent").removeClass("pushcontent-fullwidth");
				$(".pushcontent").addClass("pushcontent-open");
				$(".pushcontent").addClass("pushcontent-" + sidebar);
				var mainsidebar = $(".col-mainsidebar");
				if (mainsidebar.data("sidebarwidth")) {
					var width = mainsidebar.data("sidebarwidth");
					if (typeof width == "number") {
						$(".pushcontent").css("margin-left", width + "px");
					}
				}
				$(window).trigger("setPageTitle", [targetdiv]);
				$(window).trigger("resize");
			},
		});
	};

	showsidebaruploads = function () {
		$("#sidebarUserUploads").trigger("click");
	};

	autosubmitformtriggers = function (form) {
		if ($(form).hasClass("autosubmitform")) {
			$("select", form).on("select2:select", function () {
				if (!$(this).hasClass("cancelautosubmit")) {
					form.trigger("submit");
				}
			});
			$("select", form).on("change", function () {
				if (!$(this).hasClass("select2")) {
					if (!$(this).hasClass("cancelautosubmit")) {
						form.trigger("submit");
					}
				}
			});
			$("select", form).on("select2:unselect", function () {
				if (!$(this).hasClass("cancelautosubmit")) {
					$("#filtersremoveterm", form).val($(this).data("searchfield"));
					form.trigger("submit");
				}
			});
			$("input[type=checkbox]", form).change(function () {
				if ($(this).hasClass("filtercheck")) {
					var fieldname = $(this).data("fieldname");
					var fieldtype = $(this).data("fieldtype");
					if (fieldtype == "boolean") {
						if ($("#filtersremoveterm", form).length) {
							$("#filtersremoveterm", form).val(fieldname);
						}
					}
					var boxes = $(".filtercheck" + fieldname + ":checkbox:checked", form);
					if (boxes.length == 0) {
						if ($("#filtersremoveterm", form).length) {
							$("#filtersremoveterm", form).val(fieldname);
						}
					}
				} else {
					var parent = $(this).closest(".boolean-switches");
					if ($(this).hasClass("true-switch")) {
						parent.find(".false-switch").prop("checked", false);
					} else {
						parent.find(".true-switch").prop("checked", false);
					}
				}
				form.trigger("submit");
			});
			$("input[type=radio], .selectbox", form).change(function () {
				form.trigger("submit");
			});

			$("input[type=text]", form)
				.not(".datepicker")
				.not(".typeahead")
				.change(function () {
					form.trigger("submit");
				});

			$("input[type=text].typeahead", form).on("keyup change", function (e) {
				if (e.keyCode == 13) {
					form.trigger("submit");
				}
			});
		}
	};

	lQuery(".autosubmitform").livequery(function () {
		autosubmitformtriggers($(this));
	});

	$(".autosubmitform").on("submit", function () {
		var form = $(this);
		// Remove required from Filters Form
		if (form.hasClass("filterform")) {
			$(".required", form).each(function () {
				$(this).removeClass("required");
			});
		}
		if (form.valid()) {
			return true;
		}
		return false;
	});

	//Select Generic Table - Entity Data picuers review dataentitypicker.js
	lQuery(".emselectable table td").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}

		var clicked = $(this);
		clicked.css("pointer-events", "none");

		var emselectable = clicked.closest("#emselectable");
		if (emselectable.length < 1) {
			emselectable = clicked.closest(".emselectable");
		}
		var row = clicked.closest("tr");
		var rowid = row.attr("rowid");
		if (rowid == null) {
			rowid = row.data("dataid");
		}

		emselectable.find("table tr").each(function (index) {
			clicked.removeClass("emhighlight");
		});
		row.addClass("emhighlight");
		row.removeClass("emborderhover");

		var url = emselectable.data("url");

		var form = emselectable.find("form");
		if (!form.length) {
			form = emselectable.data("emselectableform");
			if (form) {
				form = $("#" + form);
			}
		}
		var data = row.data();

		if (form && form.length > 0) {
			data.id = rowid;
			data.oemaxlevel = form.data("oemaxlevel");
			form.find("#emselectedrow").val(rowid);
			form.find(".emneedselection").each(function () {
				clicked.removeAttr("disabled");
			});
			//form.submit();
			// var targetdiv = form.data("targetdiv");
			/*if ((typeof targetdiv) != "undefined") {
						$(form).ajaxSubmit({
							target : "#" + $.escapeSelector(targetdiv), 
							data:data
							
						});
					} else {
						*/
			$(form).trigger("submit");
			//}
			if (form.hasClass("autoclose")) {
				closeemdialog(form.closest(".modal"));
			}
			clicked.css("pointer-events", "auto");
		} else if (url != undefined && url != "") {
			if (emselectable.hasClass("showmodal")) {
				emselectable.data("id", rowid);
				emselectable.emDialog(null, function () {
					clicked.css("pointer-events", "auto");
				});
				//showmodal(emselectable, url);;
			} else {
				parent.document.location.href = url + rowid; //?
			}
		}
	});

	lQuery(".color-picker").livequery(function () {
		$(this).minicolors({
			defaultValue: "",
			letterCase: "uppercase",
		});
	});

	var browserlanguage = app.data("browserlanguage");
	if (browserlanguage == undefined || browserlanguage == "") {
		browserlanguage = "en";
	}

	lQuery("input.datepicker").livequery(function () {
		if ($.datepicker) {
			var dpicker = $(this);
			var icontext = dpicker.data("alt");
			if (!icontext) {
				icontext = "Select Date";
			}
			$.datepicker.setDefaults($.datepicker.regional[browserlanguage]);
			$.datepicker.setDefaults(
				$.extend({
					showOn: "button",
					buttonImage:
						"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='20' height='20' fill='%23444444' class='bi bi-calendar-plus' viewBox='0 0 16 16'%3E%3Cpath d='M8 7a.5.5 0 0 1 .5.5V9H10a.5.5 0 0 1 0 1H8.5v1.5a.5.5 0 0 1-1 0V10H6a.5.5 0 0 1 0-1h1.5V7.5A.5.5 0 0 1 8 7'/%3E%3Cpath d='M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5M1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4z'/%3E%3C/svg%3E",
					buttonText: icontext,
					buttonImageOnly: true,
					changeMonth: true,
					changeYear: true,
					yearRange: "1900:2050",
				}),
			); // Move this to the Layouts?

			var targetid = dpicker.data("targetid");
			dpicker.datepicker({
				altField: "#" + targetid,
				altFormat: "yy-mm-dd",
				beforeShow: function (input, inst) {
					setTimeout(function () {
						$("#ui-datepicker-div").css("z-index", 100100);
						$("#application").append($("#ui-datepicker-div"));
						// var quickSelect = $("#operationentitydatefindercatalog");
						// quickSelect.css("display", "block");
						// $("#ui-datepicker-div").append(quickSelect);
						//Fix Position if in bootstrap modal
						var modal = $("#modals");
						if (modal.length) {
							var modaltop = $("#modals").offset().top;
							if (modaltop) {
								var dpickertop = dpicker.offset().top;
								dpickertop = dpickertop - modaltop;
								var dpHeight = inst.dpDiv.outerHeight();
								var inputHeight = inst.input ? inst.input.outerHeight() : 0;
								var viewHeight = document.documentElement.clientHeight;
								if (dpickertop + dpHeight + inputHeight > viewHeight) {
									dpickertop = dpickertop - dpHeight;
								}
								inst.dpDiv.css({
									top: dpickertop + inputHeight,
								});
							}
						}
					}, 0);
				},
			});

			var current = $("#" + targetid).val();
			if (current != undefined) {
				// alert(current);
				var date;
				if (current.indexOf("-") > 0) {
					// this is the standard
					current = current.substring(0, 10);
					// 2012-09-17 09:32:28 -0400
					date = $.datepicker.parseDate("yy-mm-dd", current);
				} else {
					date = $.datepicker.parseDate("mm/dd/yy", current); // legacy
				}
				$(this).datepicker("setDate", date);
			}

			$(this).blur(function () {
				var val = $(this).val();
				if (val == "") {
					$("#" + targetid).val("");
				}
			});
			$(this).on("mousedown focus", function () {
				if ($("#ui-datepicker-div").is(":visible")) {
					return;
				}
				let picker = $(this).parent().find(".ui-datepicker-trigger");
				picker.trigger("click");
			});
			/*
			$(this).clickOutside({
				event: "click",
				handler: function () {
					if (!$("#ui-datepicker-div").is(":visible")) {
						return;
					}
					let picker = $(this).parent().find(".ui-datepicker-trigger");
					picker.trigger("click");
				},
				exclude: [".ui-datepicker", ".ui-icon"],
			});
			*/
		} //datepicker
	});

	function toggleAiSuggestions() {
		var container = $(".ai-suggestions-container");
		if (container.hasClass("expanded")) {
			container.removeClass("expanded");
			$(".ai-func-toggle").text("Show Examples");
		} else {
			container.addClass("expanded");
			$(".ai-func-toggle").text("Hide Examples");
		}
	}

	lQuery(".autoprefixchatmsg").livequery("click", function () {
		var prefix = $(this).data("prefix");
		var editorid = $(this).data("editorid");
		if (!editorid) {
			editorid = "chatter-msg";
		}
		var editor = $("#" + editorid);
		setTimeout(() => {
			toggleAiSuggestions();
			editor.focus();
			editor.val(prefix);
		}, 100);
	});

	lQuery(".ai-func-toggle").livequery("click", toggleAiSuggestions);

	lQuery(".ai-suggestion").livequery("click", function () {
		var parent = $(this).closest(".ai-suggestions");
		parent.find(".ai-suggestion").each(function () {
			$(this).removeClass("selected");
		});
		$(this).addClass("selected");
	});
}); //on ready
