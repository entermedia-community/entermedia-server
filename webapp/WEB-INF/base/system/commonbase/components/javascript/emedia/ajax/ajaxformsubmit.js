(function ($) {
	$.fn.ajaxFormSubmit = function (callbackFunc = null) {
		// $(window).trigger("ajaxsubmitting");
		var form = $(this);

		var app = jQuery("#application");

		if (form.data("submitting")) {
			console.log(form.data("submitting"));
			console.warn("Already submitting this form");
			// return this;
		}

		var warning = form.data("warning");
		if (warning && !confirm(warning)) {
			return this;
		}

		if ("updateAllCK5" in window) {
			updateAllCK5();
		}

		if (!form.hasClass("novalidate")) {
			//
			if (form.validate) {
				try {
					$.validator.addClassRules("entityRequired", {
						entityrequired: true,
						number: true,
						min: 1,
					});
					form.validate({
						ignore:
							".ignore,:hidden:not(.validatehidden),:hidden:not(.select2-hidden-accessible)",
					});
					var isvalidate = form.valid();
					if (!isvalidate) {
						//e.preventDefault();
						console.log("Form is not Valid");
						var validator = form.validate();
						var errors = validator.errorList;
						if (errors.length > 0) {
							console.log("Form is invalid. Errors found:", errors);
						} else {
							console.log("Form is truly valid now.");
						}
						var submitbtn = form.find(".submitform");
						if (submitbtn) {
							submitbtn.prop("disabled", false);
						}
						return this;
					}
				} catch (_e) {
					console.log(_e);
				}
			}
		}
		var targetdivS = form.data("targetdiv");
		var targetdiv;
		var edithomeid = form.data("edithomeid");

		//TODO: Move all this to a jQuery plugin form.findTargetDiv()
		if (edithomeid !== undefined && edithomeid != "") {
			targetdiv = $("#" + edithomeid + " ." + targetdivS);
		} else {
			targetdiv = form.closest("." + $.escapeSelector(targetdivS));
		}
		if (!targetdiv.length) {
			targetdiv = $("#" + $.escapeSelector(targetdivS));
		}
		if (!targetdiv.length) {
			targetdiv = $("." + $.escapeSelector(targetdivS));
		}
		if (targetdiv.length > 1) {
			console.error("Should not have more than one target ", targetdiv);
		}
		if (form.attr("action") == undefined) {
			var action = targetdiv.data("saveaction");
			if (action == undefined) {
				action = form.data("defaultaction");
			}
			form.attr("action", action);
		}

		if (form.hasClass("showwaiting") && app !== undefined) {
			var apphome = app.data("siteroot") + app.data("apphome");
			var showwaitingtarget = targetdiv;
			if (form.data("showwaitingtarget")) {
				showwaitingtarget = form.data("showwaitingtarget");
				showwaitingtarget = $("#" + $.escapeSelector(showwaitingtarget));
			}
			showwaitingtarget.html(
				'<img src="' + apphome + '/theme/images/ajax-loader.gif">',
			);
			showwaitingtarget.show();
		}

		var oemaxlevel = targetdiv.data("oemaxlevel");
		if (oemaxlevel == undefined) {
			oemaxlevel = form.data("oemaxlevel");
		}
		if (oemaxlevel == undefined) {
			oemaxlevel = form.find("input[name=oemaxlevel]").val();
		}
		if (oemaxlevel == undefined) {
			oemaxlevel = 1;
		}
		//targetdiv.data("oemaxlevel", oemaxlevel);
		var data = form.cleandata();
		if (form.data("includeeditcontext") == true) {
			if (edithomeid !== undefined) {
				var editdiv = $("#" + edithomeid);
				if (editdiv.length > 0) {
					var otherdata = editdiv.cleandata();
					data = {
						...otherdata,
						...data,
					};
				} else {
					console.warn("No editdiv found for includeeditcontext");
				}
			}
		}
		if (form.data("includesearchcontext") == true) {
			var editdiv = targetdiv.closest(".editdiv"); //This is used for lightbox tree opening
			var resultsdiv = editdiv.find(".resultsdiv");

			if (resultsdiv.length > 0) {
				var otherdata = resultsdiv.cleandata();
				data = {
					...otherdata,
					...data,
				};
			} else {
				console.warn("No resultsdiv found for icludesearchcontext");
			}
		}

		data.oemaxlevel = oemaxlevel;

		var formmodal = form.closest(".modal");
		var submitButton = form.find('button[type="submit"]');
		if (submitButton.length == 0) {
			submitButton = form.find('input[type="submit"]');
		}
		if (submitButton.length == 0) {
			var modalbutton = formmodal.find("#submitbutton");
			if (
				modalbutton.length > 0 &&
				modalbutton.attr("form") == form.attr("id")
			) {
				submitButton = modalbutton;
			}
		}
		if (submitButton.length > 0) {
			submitButton.attr("disabled", "disabled");
			var icon = submitButton.find("i");
			if (icon.length == 0) {
				submitButton.prepend("<i class='fas fa-spinner fa-spin me-1'></i>");
			} else {
				icon.replaceWith("<i class='fas fa-spinner fa-spin me-1'></i>");
			}
		}

		$(window).trigger("showToast", [form]);
		var toastUid = $(form).data("uid");

		form.data("submitting", true);

		form.ajaxSubmit({
			data: { ...data },
			xhrFields: {
				withCredentials: true,
			},
			crossDomain: true,
			success: function (result) {
				if (callbackFunc) {
					callbackFunc(result);
				}
				$(window).trigger("successToast", [toastUid]);
				$(window).trigger("checkautoreload", [form]);
				if (showwaitingtarget !== undefined) {
					showwaitingtarget.hide();
				}

				var pickertarget = form.data("pickertarget");
				var targettype = form.data("targettype");
				if (pickertarget !== undefined && targettype == "entitypickerfield") {
					var parsed = $(result);
					var dataid = parsed.data("dataid");
					var dataname = parsed.data("dataname");

					$(window).trigger("updatepickertarget", [
						//Is this still used? Delete?
						pickertarget,
						dataid,
						dataname,
					]);
				}

				var targetdivinner = form.data("targetdivinner");
				if (targetdivinner) {
					$("#" + $.escapeSelector(targetdivinner)).html(result);
				} else {
					if (targetdiv) {
						targetdiv.replaceWith(result);
					}
				}
				let closedialogid = form.data("closedialogid");
				if (closedialogid !== undefined) {
					let splitnames = closedialogid.split(",");
					$.each(splitnames, function (index, modalid) {
						modalid = modalid.trim();
						$("#" + modalid).each(function (index, div) {
							closeemdialog($(div).closest(".modal"));
						});
					});
				}
				if (formmodal.length > 0 && form.hasClass("autocloseform")) {
					if (formmodal.modal) {
						closeemdialog(formmodal);
					}
				}

				//OLD? $("#resultsdiv").data("reloadresults", true);

				//TODO: Move this to results.js
				if (form.hasClass("hideMediaViewer")) {
					$(window).trigger("hideMediaViewer");
				}

				if (form.hasClass("autoreloadsource")) {
					//TODO: Use ajaxreloadtargets
					var link = form.data("openedfrom");
					if (link) {
						window.location.replace(link);
					}
				}
				$(window).trigger("resize");

				//on success execute extra JS
				if (form.data("onsuccess")) {
					var onsuccess = form.data("onsuccess");
					var fnc = window[onsuccess];
					if (fnc && typeof fnc === "function") {
						//make sure it exists and it is a function
						fnc(form); //execute it
					}
				}

				//experimental
				if (form.data("onsuccessreload")) {
					document.location.reload(true);
				}
			},
			error: function (data) {
				$(window).trigger("errorToast", [toastUid]);
				if (targetdiv) {
					$("#" + $.escapeSelector(targetdiv)).html(data);
				}
				form.append(data);
			},
			complete: function () {
				if (submitButton.length > 0) {
					submitButton.removeAttr("disabled");
					submitButton.find(".fa-spinner").remove();
				}
				form.data("submitting", false);
			},
		});

		var reset = form.data("reset");
		if (reset == true) {
			form.get(0).reset();
		}

		if (typeof global_updateurl !== "undefined" && global_updateurl == false) {
			//globaly disabled updateurl
		} else {
			//Update Address Bar
			var updateurl = form.data("updateurl");
			if (updateurl) {
				//serialize and attach
				var params = form.serialize();
				var url = form.attr("action");
				url += (url.indexOf("?") >= 0 ? "&" : "?") + params;
				history.pushState($("#application").html(), null, url);
				window.scrollTo(0, 0);
			}
		}

		var scrolltotop = form.data("scrolltotop");
		if (scrolltotop) {
			window.scrollTo(0, 0);
		}
		return this;
	};
})(jQuery);

$(document).ready(function () {
	lQuery("form.ajaxform").livequery("submit", function (e) {
		e.preventDefault();
		e.stopImmediatePropagation();
		e.stopPropagation();
		$(this).ajaxFormSubmit();
	});
});
