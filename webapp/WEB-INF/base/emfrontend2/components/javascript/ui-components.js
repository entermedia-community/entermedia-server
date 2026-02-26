//EMfrontend2

formatHitCountResult = function (inRow) {
	return inRow[1];
};

function isInViewport(cell) {
	const rect = cell.getBoundingClientRect();
	var isin =
		rect.top >= 0 &&
		rect.top <= (window.innerHeight || document.documentElement.clientHeight);
	return isin;
}

function getRandomColor() {
	var letters = "0123456789ABCDEF".split("");
	var color = "#";
	for (var i = 0; i < 6; i++) {
		color += letters[Math.floor(Math.random() * 16)];
	}
	return color;
}
lQuery(".reloadpage").livequery(function () {
	window.location.reload();
});
lQuery(".redirecttopage").livequery(function () {
	var url = $(this).data("redirectok");
	window.location.href = url;
});
uiload = function () {
	var app = jQuery("#application");
	var siteroot = app.data("siteroot");
	var apphome = app.data("apphome");
	var themeprefix = app.data("themeprefix");
	if (siteroot !== undefined) {
		//legacy siteroot
		apphome = siteroot + apphome;
		themeprefix = siteroot + themeprefix;
	}
	var mediadb = $("#application").data("mediadbappid");

	$.fn.cleandata = function () {
		var element = $(this);
		var params = element.data();

		var cleaned = [];
		Object.keys(params).forEach(function (key) {
			var param = params[key];
			var thetype = typeof param;
			if (
				thetype === "string" ||
				thetype === "number" ||
				thetype === "boolean"
			) {
				cleaned[key] = param;
			}
		});
		return cleaned;
	};

	$.fn.setformdata = function (cleandata) {
		var form = $(this);
		Object.keys(cleandata).forEach(function (key) {
			var param = cleandata[key];
			//TODO make sure its not already on there
			//form.append(key,param);
			var found = $('input[name="' + key + '"]', form);
			if (found.length == 0) {
				form.append(
					'<input type="hidden" name="' + key + '" value="' + param + '" />',
				);
			} else {
				found.attr("value", param);
			}
		});
	};

	// https://github.com/select2/select2/issues/600
	//$.fn.select2.defaults.set("theme", "bootstrap4");
	$.fn.modal.Constructor.prototype._enforceFocus = function () {}; // Select2 on Modals

	resizecolumns();

	if ($.fn.tablesorter) {
		$("#tablesorter").tablesorter();
	}
	if ($.fn.selectmenu) {
		lQuery(".uidropdown select").livequery(function () {
			$(this).selectmenu({
				style: "dropdown",
			});
		});
	}

	var browserlanguage = app.data("browserlanguage");
	if (browserlanguage == undefined || browserlanguage == "") {
		browserlanguage = "en";
	}

	if ($.datepicker) {
		lQuery("input.datepicker").livequery(function () {
			var dpicker = $(this);
			$.datepicker.setDefaults($.datepicker.regional[browserlanguage]);
			$.datepicker.setDefaults(
				$.extend({
					showOn: "button",
					buttonImage: themeprefix + "/entermedia/images/cal.gif",
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
				yearRange: "1900:2050",
				beforeShow: function (input, inst) {
					setTimeout(function () {
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
		});
	} //datepicker

	if ($.fn.minicolors) {
		$(".color-picker").minicolors({
			defaultValue: "",
			letterCase: "uppercase",
		});
	}

	lQuery(".focusme").livequery(function () {
		$(this).focus();
	});

	lQuery("#module-dropdown").livequery("click", function (e) {
		e.stopPropagation();
		if ($(this).hasClass("active")) {
			$(this).removeClass("active");
			$("#module-list").hide();
		} else {
			$(this).addClass("active");
			$("#module-list").show();
		}
	});

	lQuery("select.select2").livequery(function () {
		var theinput = $(this);

		var allowClear = $(this).data("allowclear");
		if (allowClear == undefined) {
			allowClear = true;
		}

		var placeholder = $(this).data("placeholder");
		if (placeholder == undefined) {
			placeholder = "";
		}

		theinput.select2({
			allowClear: allowClear,
			placeholder: placeholder,
			dropdownParent: getDropdownParent(theinput),
		});
		theinput.on("select2:open", function (e) {
			var selectId = $(this).attr("id");
			if (selectId) {
				$(
					".select2-search__field[aria-controls='select2-" +
						selectId +
						"-results']",
				).each(function (key, value) {
					value.focus();
				});
			} else {
				document
					.querySelector(".select2-container--open .select2-search__field")
					.focus();
			}
		});
	});

	lQuery("select.listdropdown").livequery(function () {
		var theinput = $(this);

		//console.log(theinput.attr("id")+"using: "+dropdownParent.attr("id"));
		var placeholder = theinput.data("placeholder");
		if (!placeholder) {
			placeholder = " ";
		}
		var allowClear = theinput.data("allowclear");

		if (allowClear == undefined) {
			allowClear = true;
		}

		theinput.select2({
			placeholder: placeholder,
			allowClear: allowClear,
			minimumInputLength: 0,
			dropdownParent: getDropdownParent(theinput),
		});
		theinput.on("select2:open", function (e) {
			var selectId = $(this).attr("id");
			if (selectId) {
				$(
					".select2-search__field[aria-controls='select2-" +
						selectId +
						"-results']",
				).each(function (key, value) {
					value.focus();
				});
			} else {
				document
					.querySelector(".select2-container--open .select2-search__field")
					.focus();
			}
		});
	});

	lQuery(".select2editable").livequery(function () {
		var input = $(this);
		var arr = new Array(); // [{id: 0, text: 'story'},{id: 1, text:
		// 'bug'},{id: 2, text: 'task'}]

		var options = $(this).find("option");

		if (!options.length) {
			//			return;
		}

		options.each(function () {
			var id = $(this).data("value");
			var text = $(this).text();
			//console.log(id + " " + text);
			arr.push({
				id: id,
				text: text,
			});
		});

		// Be aware: calling select2 forces livequery to filter again
		input
			.select2({
				createSearchChoice: function (term, data) {
					if (
						$(data).filter(function () {
							return this.text.localeCompare(term) === 0;
						}).length === 0
					) {
						//console.log("picking" + term);
						return {
							id: term,
							text: term,
						};
					}
				},
				multiple: false,
				tags: true,
			})
			.on("select2:select", function (e) {
				var thevalue = $(this).val();
				if (thevalue != "" && $(this).hasClass("autosubmited")) {
					var theform = $(this).parent("form");
					if (theform.hasClass("autosubmitform")) {
						theform.trigger("submit");
					}
				}
			});

		input.on("select2:open", function (e) {
			var selectId = $(this).attr("id");
			if (selectId) {
				$(
					".select2-search__field[aria-controls='select2-" +
						selectId +
						"-results']",
				).each(function (key, value) {
					value.focus();
				});
			} else {
				document
					.querySelector(".select2-container--open .select2-search__field")
					.focus();
			}
		});
	});

	lQuery("select.ajax").livequery("change", function (e) {
		var inlink = $(this);
		var nextpage = inlink.data("href");
		nextpage = nextpage + inlink.val();
		var targetDiv = inlink.data("targetdiv");
		if (!targetDiv) {
			targetDiv = inlink.attr("targetdiv");
		}
		targetDiv = targetDiv.replace(/\//g, "\\/");
		$.get(nextpage, {}, function (data) {
			var cell = $("#" + targetDiv);
			cell.replaceWith(data);
			$(window).trigger("resize");
		});
	});

	lQuery("a.toggle-visible").livequery("click", function (e) {
		e.preventDefault();
		var div = $(this).data("targetdiv");
		var target = $("#" + div);
		if (target.is(":hidden")) {
			var hidelable = $(this).data("hidelabel");
			$(this).find("span").text(hidelable);
			target.show();
		} else {
			var showlabel = $(this).data("showlabel");
			$(this).find("span").text(showlabel);
			target.hide();
		}
	});

	// deprecated, use data-confirm
	lQuery(".confirm").livequery("click", function (e) {
		var inText = $(this).attr("confirm");
		if (!inText) {
			inText = $(this).data("confirm");
		}
		if (confirm(inText)) {
			return;
		} else {
			e.preventDefault();
		}
	});

	lQuery(".uibutton").livequery(function () {
		$(this).button();
	});
	lQuery(".fader").livequery(function () {
		$(this).fadeOut(2000, "linear");
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

	lQuery(".ajaxchange select").livequery(function () {
		var select = $(this);
		var div = select.parent(".ajaxchange");
		var url = div.attr("targetpath");
		var divid = div.attr("targetdiv");

		select.change(function () {
			var url2 = url + $(this).val();
			$("#" + divid).load(url2);
		});
	});

	lQuery("form").livequery("submit", function (e) {
		if (typeof CKEDITOR !== "undefined") {
			for (instance in CKEDITOR.instances) {
				var editor = CKEDITOR.instances[instance];
				var div = $(editor.element.$);
				var id = div.data("saveto");
				var tosave = $("#" + id);
				//editor.updateElement() //does not work
				var data = editor.getData();
				tosave.val(data);
			}
		}
	});

	lQuery("form.autosubmit").livequery(function () {
		var form = $(this);
		$("select", form).change(function () {
			$(form).trigger("submit");
		});
		$("input", form).on("focusout", function (event) {
			$(form).trigger("submit");
		});
		$("input", form).on("keyup", function (e) {
			$(form).trigger("submit");
		});
		$(
			'input[type="file"],input[name="date.after"],input[type="checkbox"]',
			form,
		).on("change", function () {
			$(form).trigger("submit");
		});
	});

	lQuery("form.ajaxautosubmit").livequery(function () {
		var theform = $(this);
		theform.find("select").change(function () {
			theform.submit();
		});
	});

	lQuery(".submitform").livequery("click", function (e) {
		e.preventDefault();
		var theform = $(this).closest("form");
		//console.log("Submit Form " + theform);
		theform.trigger("submit");
	});

	lQuery(".submitform-oehtml, .dialogsubmitbtn").livequery(
		"click",
		function (e) {
			var theform = $(this).closest("form");
			if (theform.length == 0) {
				//dialog form?
				var dialogform = $(this).attr("form");
				theform = $("#" + dialogform);
			}
			if (theform.length) {
				theform.data("readytosubmit", "true");
				theform.find(".oehtmlinput").trigger("blur");
				theform.trigger("submit");
			}
			e.preventDefault();
		},
	);

	lQuery(".oehtmlinputblur").livequery("click", function (e) {
		e.preventDefault();
		//console.log("Trigger ui");
		var theform = $(this).closest("form");
		if (theform.length == 0) {
			//dialog form?
			var dialogform = $(this).attr("form");
			theform = $("#" + dialogform);
		}
		if (theform.length) {
			//theform.data("readytosubmit","true");
			theform.find(".oehtmlinput").trigger("blur");

			/*
				var textarea_id = theform.find(".oehtmlinput").attr("id");
				for(var instanceName in CKEDITOR.instances) {
				    CKEDITOR.instances[instanceName].updateElement();
				    CKEDITOR.instances[instanceName].trigger('blur');
				}
			    */
			$("#startbutton").trigger("click");
		}

		e.stopPropagation();
	});

	lQuery(".form-currency").livequery("keyup", function () {
		var val = $(this).val().replace(/\,/g, "");
		val = val.replace(/\$/g, "");
		val = val.replace(/ /g, "");
		$(this).val(val);
	});

	lQuery(".quicksearch-toggler").livequery("click", function () {
		var navbar = $(this).data("target");
		$("#" + navbar).toggle();
	});

	lQuery(".emrowpicker table td").livequery("click", function (event) {
		event.preventDefault();

		var clicked = $(this);
		var row = clicked.closest("tr");
		var existing = row.hasClass("emrowselected");
		row.toggleClass("emrowselected");
		var id = row.data("id");

		var form = $(clicked.closest("form"));
		$(".emselectedrow", form).each(function () {
			if (form.hasClass("emmultivalue")) {
				var old = $(this).val();
				if (old) {
					if (existing) {
						// removing the value
						old = old.replace(id, "");
						old = old.replace("||", "|");
					} else {
						old = old + "|" + id;
					}
				} else {
					old = id;
				}
				$(this).val(old);
			} else {
				$(this).val(id);
			}
		});

		var targetdiv = form.data("targetdiv");
		if (typeof targetdiv != "undefined") {
			$(form).ajaxSubmit({
				target: "#" + $.escapeSelector(targetdiv),
			});
		} else {
			$(form).trigger("submit");
		}
		if (form.hasClass("autoclose")) {
			closeemdialog(form.closest(".modal"));
		}
	});

	lQuery("#emselectable table td").livequery("click", function (event) {
		var clicked = $(this);
		if (clicked.attr("noclick") == "true") {
			return true;
		}
		if ($(event.target).is("input")) {
			return true;
		}
		var emselectable = clicked.closest("#emselectable");
		var row = $(clicked.closest("tr"));
		if (row.hasClass("thickbox")) {
			var href = row.data("href");
			openFancybox(href);
		} else {
			emselectable.find("table tr").each(function (index) {
				clicked.removeClass("emhighlight");
			});
			row.addClass("emhighlight");
			row.removeClass("emborderhover");
			var table = row.closest("table");
			var id = row.attr("rowid");
			// var url = emselectable.data("clickpath");
			var url = table.data("clickpath");
			var form = emselectable.find("form");
			var data = row.data();
			if (form.length > 0) {
				emselectable.find("#emselectedrow").val(id);
				emselectable.find(".emneedselection").each(function () {
					clicked.removeAttr("disabled");
				});
				//form.submit();
				var targetdiv = form.data("targetdiv");

				if (typeof targetdiv != "undefined") {
					//$(form).setformdata(data);
					$(form).ajaxSubmit({
						target: "#" + $.escapeSelector(targetdiv),
						data: data,
					});
				} else {
					$(form).trigger("submit");
				}

				if (form.hasClass("autoclose")) {
					closeemdialog(form.closest(".modal"));
				}
			} else if (url != undefined) {
				if (url == "") {
					return true;
				}
				var link = url;
				var post = table.data("viewpostfix");
				if (post != undefined) {
					link = link + id + post;
				} else {
					link = link + id;
				}
				if (emselectable.hasClass("showmodal")) {
					showmodal(emselectable, link);
				} else {
					parent.document.location.href = link;
				}
			} else {
				parent.document.location.href = id;
			}
		}
	});

	showmodal = function (emselecttable, url) {
		var id = "modals";
		var modaldialog = $("#" + id);
		var width = emselecttable.data("dialogwidth");
		if (modaldialog.length == 0) {
			$("#emcontainer").append(
				'<div class="modal " tabindex="-1" id="' +
					id +
					'" style="display:none" ></div>',
			);
			modaldialog = $("#" + id);
		}

		var options = emselecttable.data();
		modaldialog.load(url, options, function () {
			$(".modal-lg").css("min-width", width + "px");
			modaldialog.modal({
				keyboard: true,
				backdrop: true,
				show: true,
			});

			var title = emselecttable.data("dialogtitle");
			if (title) {
				$(".modal-title", modaldialog).text(title);
			}

			$("form", modaldialog).find("*").filter(":input:visible:first").focus();
		});
	};

	lQuery("img.framerotator").livequery(function () {
		$(this).hover(
			function () {
				$(this).data("frame", 0);
				var path = this.sr$("select#speedC").selectmenu({
					style: "dropdown",
				});
				c.split("?")[0];
				var intval = setInterval(
					"nextFrame('" + this.id + "', '" + path + "')",
					1000,
				);
				$(this).data("intval", intval);
			},
			function () {
				var path = this.src.split("?")[0];
				this.src = path + "?frame=0";
				var intval = $(this).data("intval");
				clearInterval(intval);
			},
		);
	});

	lQuery(".jp-play").livequery("click", function () {
		// alert("Found a player, setting it up");
		var player = $(this).closest(".jp-audio").find(".jp-jplayer");
		var url = player.data("url");
		var containerid = player.data("container");
		var container = $("#" + containerid);

		player.jPlayer({
			ready: function (event) {
				player
					.jPlayer("setMedia", {
						mp3: url,
					})
					.jPlayer("play");
			},
			play: function () {
				// To avoid both jPlayers playing together.
				player.jPlayer("pauseOthers");
			},
			swfPath: apphome + "/components/javascript",
			supplied: "mp3",
			wmode: "window",
			cssSelectorAncestor: "#" + containerid,
		});

		// player.jPlayer("play");
	});

	lQuery(".select-dropdown-open").livequery("click", function () {
		if ($(this).hasClass("down")) {
			$(this).removeClass("down");
			$(this).addClass("up");
			$(this).siblings(".select-dropdown").show();
		} else {
			$(this).removeClass("up");
			$(this).addClass("down");
			$(this).siblings(".select-dropdown").hide();
		}
	});
	lQuery(".select-dropdown li a").livequery("click", function () {
		$(this)
			.closest(".select-dropdown")
			.siblings(".select-dropdown-open")
			.removeClass("up");
		$(this)
			.closest(".select-dropdown")
			.siblings(".select-dropdown-open")
			.addClass("down");
		$(this).closest(".select-dropdown").hide();
		//console.log("Clicked");
	});

	function select2formatResult(emdata) {
		/*
		 * var element = $(this.element); var showicon =
		 * element.data("showicon"); if( showicon ) { var type =
		 * element.data("searchtype"); var html = "<img
		 * class='autocompleteicon' src='" + themeprefix + "/images/icons/" +
		 * type + ".png'/>" + emdata.name; return html; } else { return
		 * emdata.name; }
		 */
		return emdata.name;
	}
	function select2Selected(selectedoption) {
		// "#list-" + foreignkeyid
		// var id = container.closest(".select2-container").attr("id");
		// id = "list-" + id.substring(5); //remove sid2_
		// container.closest("form").find("#" + id ).val(emdata.id);
		return selectedoption.name || selectedoption.text;
	}

	lQuery("input.defaulttext").livequery("click", function () {
		var theinput = $(this);
		var startingtext = theinput.data("startingtext");
		if (theinput.val() == startingtext) {
			theinput.val("");
		}
	});

	lQuery("select.listtags").livequery(function () {
		var theinput = $(this);

		var searchtype = theinput.data("searchtype");
		var searchfield = theinput.data("searchfield");
		var catalogid = theinput.data("listcatalogid");
		var sortby = theinput.data("sortby");
		var defaulttext = theinput.data("showdefault");
		if (!defaulttext) {
			defaulttext = "Search";
		}
		var allowClear = $(this).data("allowclear");
		if (allowClear == undefined) {
			allowClear = true;
		}
		var url =
			apphome +
			"/components/xml/types/autocomplete/tagsearch.txt?catalogid=" +
			catalogid +
			"&field=" +
			searchfield +
			"&operation=contains&searchtype=" +
			searchtype;

		theinput.select2({
			tags: true,
			placeholder: defaulttext,
			allowClear: allowClear,
			dropdownParent: getDropdownParent(theinput),
			selectOnBlur: true,
			delay: 150,
			minimumInputLength: 1,
			ajax: {
				// instead of writing
				// the function to
				// execute the request
				// we use Select2's
				// convenient helper
				url: url,
				dataType: "json",
				data: function (params) {
					var search = {
						page_limit: 15,
						page: params.page,
					};
					search[searchfield + ".value"] = params.term; // search
					// term
					search["sortby"] = sortby; // search
					// term
					return search;
				},
				processResults: function (data, params) {
					// parse the
					// results
					// into the
					// format
					// expected
					// by
					// Select2.
					params.page = params.page || 1;
					return {
						results: data.rows,
						pagination: {
							more: false,
							// (params.page * 30) <
							// data.total_count
						},
					};
				},
			},
			escapeMarkup: function (m) {
				return m;
			},
			templateResult: select2formatResult,
			templateSelection: select2Selected,
			tokenSeparators: ["|"],
			separator: "|",
		});
		theinput.on("select2:select", function () {
			if ($(this).parents(".ignore").length == 0) {
				$(this).valid();
			}
		});
		theinput.on("select2:unselect", function () {
			if ($(this).parents(".ignore").length == 0) {
				$(this).valid();
			}
		});
	});

	lQuery(".grabfocus").livequery(function () {
		var theinput = $(this);
		theinput.css("color", "#666");
		if (theinput.val() == "") {
			var newval = theinput.data("initialtext");
			theinput.val(newval);
		}
		theinput.on("click", function () {
			theinput.css("color", "#000");
			var initial = theinput.data("initialtext");
			//console.log(initial, theinput.val());
			if (theinput.val() === initial) {
				theinput.val("");
				theinput.unbind("click");
			}
		});

		theinput.focus();
	});

	lQuery(".emtabs").livequery(function () {
		var tabs = $(this);

		var tabcontent = $("#" + tabs.data("targetdiv"));

		// active the right tab
		var hash = window.location.hash;
		if (hash) {
			var activelink = $(hash, tabs);
			if (activelink.length == 0) {
				hash = false;
			}
		}
		if (!hash) {
			hash = "#" + tabs.data("defaulttab");
		}
		var activelink = $(hash, tabs);
		var loadedpanel = $(hash + "panel", tabcontent);
		if (loadedpanel.length == 0) {
			loadedpanel = $("#loadedpanel", tabcontent);
			loadedpanel.attr("id", activelink.attr("id") + "panel");
			activelink.data("tabloaded", true);
		}
		activelink.parent("li").addClass("emtabselected");
		activelink.data("loadpageonce", false);

		$("a:first-child", tabs).on("click", function (e) {
			e.preventDefault();

			var link = $(this); // activated tab
			$("li", tabs).removeClass("emtabselected");
			link.parent("li").addClass("emtabselected");

			var id = link.attr("id");

			var url = link.attr("href");
			var panelid = id + "panel";
			var tab = $("#" + panelid);
			if (tab.length == 0) {
				tab = tabcontent.append(
					'<div class="tab-pane" id="' + panelid + '" ></div>',
				);
				tab = $("#" + panelid);
			}

			var reloadpage = link.data("loadpageonce");
			var alwaysreloadpage = link.data("alwaysreloadpage");
			if (reloadpage || alwaysreloadpage) {
				if (window.location.href.endsWith(url)) {
					window.location.reload();
				} else {
					window.location.href = url;
				}
			} else {
				url = url + "#" + id;
				var loaded = link.data("tabloaded");
				if (link.data("allwaysloadpage")) {
					loaded = false;
				}
				if (!loaded) {
					var levels = link.data("layouts");
					if (!levels) {
						levels = "1";
					}
					$.get(
						url,
						{
							oemaxlevel: levels,
						},
						function (data) {
							tab.html(data);
							link.data("tabloaded", true);
							$(">.tab-pane", tabcontent).hide();
							tab.show();
							$(window).trigger("resize");
						},
					);
				} else {
					$(">.tab-pane", tabcontent).hide();
					tab.show();
					$(window).trigger("resize");
				}
			}
		});
	});

	lQuery(".closetab").livequery("click", function (e) {
		e.preventDefault();
		var tab = $(this);
		var nextpage = tab.data("closetab");
		$.get(
			nextpage,
			{
				oemaxlayout: 1,
			},
			function (data) {
				var prevtab = tab.closest("li").prev();
				prevtab.find("a").trigger("click");

				if (prevtab.hasClass("firstab")) {
					tab.closest("li").remove();
				}
			},
		);
		return false;
	});

	lQuery(".collectionclose").livequery("click", function (e) {
		e.preventDefault();
		var collection = $(this);
		var nextpage = collection.data("closecollection");
		$.get(
			nextpage,
			{
				oemaxlayout: 1,
			},
			function (data) {
				collection.closest("li").remove();
			},
		);
		return false;
	});

	lQuery(".createmedia-btn").livequery("click", function (e) {
		$(".createmedia-tab").removeClass("createmedia-selected");
		$(this).closest(".createmedia-tab").addClass("createmedia-selected");
	});

	lQuery("select.listautocomplete").livequery(function () {
		// select2
		var theinput = $(this);
		var searchtype = theinput.data("searchtype");
		if (searchtype != undefined) {
			// called twice due to
			// the way it reinserts
			// components
			var searchfield = theinput.data("searchfield");

			var foreignkeyid = theinput.data("foreignkeyid");
			var sortby = theinput.data("sortby");

			var defaulttext = theinput.data("showdefault");
			if (!defaulttext) {
				defaulttext = "Search";
			}
			var defaultvalue = theinput.data("defaultvalue");
			var defaultvalueid = theinput.data("defaultvalueid");

			var url =
				apphome +
				"/components/xml/types/autocomplete/datasearch.txt?" +
				"field=" +
				searchfield +
				"&operation=contains&searchtype=" +
				searchtype;
			if (defaultvalue != undefined) {
				url =
					url +
					"&defaultvalue=" +
					defaultvalue +
					"&defaultvalueid=" +
					defaultvalueid;
			}

			var allowClear = theinput.data("allowclear");
			if (allowClear == undefined) {
				allowClear = true;
			}

			theinput.select2({
				placeholder: defaulttext,
				allowClear: allowClear,
				minimumInputLength: 0,
				dropdownParent: getDropdownParent(theinput),
				ajax: {
					// instead of writing the
					// function to execute the
					// request we use Select2's
					// convenient helper
					url: url,
					dataType: "json",
					data: function (params) {
						var fkv = theinput
							.closest("form")
							.find("#list-" + foreignkeyid + "value")
							.val();
						if (fkv == undefined) {
							fkv = theinput
								.closest("form")
								.find("#list-" + foreignkeyid)
								.val();
						}
						var search = {
							page_limit: 15,
							page: params.page,
						};
						search[searchfield + ".value"] = params.term; // search
						// term
						if (fkv) {
							search["field"] = foreignkeyid; // search
							// term
							search["operation"] = "matches"; // search
							// term
							search[foreignkeyid + ".value"] = fkv; // search
							// term
						}
						if (sortby) {
							search["sortby"] = sortby; // search
							// term
						}
						return search;
					},
					processResults: function (data, params) {
						// parse the
						// results into
						// the format
						// expected by
						// Select2.
						var rows = data.rows;
						if (theinput.hasClass("selectaddnew")) {
							if (params.page == 1 || !params.page) {
								var addnewlabel = theinput.data("addnewlabel");
								var addnewdata = {
									name: addnewlabel,
									id: "_addnew_",
								};
								rows.unshift(addnewdata);
							}
						}
						// addnew
						params.page = params.page || 1;
						return {
							results: rows,
							pagination: {
								more: false,
								// (params.page * 30) <
								// data.total_count
							},
						};
					},
				},
				escapeMarkup: function (m) {
					return m;
				},
				templateResult: select2formatResult,
				templateSelection: select2Selected,
			});

			// TODO: Remove this?
			theinput.on("change", function (e) {
				if (e.val == "") {
					// Work around for a bug
					// with the select2 code
					var id = "#list-" + theinput.attr("id");
					$(id).val("");
				} else {
					// Check for "_addnew_" show ajax form
					var selectedid = theinput.val();

					if (selectedid == "_addnew_") {
						var clicklink = $("#" + theinput.attr("id") + "add");
						clicklink.trigger("click");

						e.preventDefault();
						theinput.select2("val", "");
						return false;
					}
					// Check for "_addnew_" show ajax form
					if (theinput.hasClass("selectautosubmit")) {
						if (selectedid) {
							//var theform = $(this).closest("form");
							var theform = $(this).parent("form");
							if (theform.hasClass("autosubmitform")) {
								theform.trigger("submit");
							}
						}
					}
				}
			});

			theinput.on("select2:open", function (e) {
				var selectId = $(this).attr("id");
				if (selectId) {
					$(
						".select2-search__field[aria-controls='select2-" +
							selectId +
							"-results']",
					).each(function (key, value) {
						value.focus();
					});
				} else {
					document
						.querySelector(".select2-container--open .select2-search__field")
						.focus();
				}
			});
		}
	});

	lQuery("select.safelistautocomplete").livequery(function () {
		// select2
		var theinput = $(this);
		var searchtype = theinput.data("searchtype");
		if (searchtype != undefined) {
			// called twice due to
			// the way it reinserts
			// components
			var searchfield = theinput.data("searchfield");

			var foreignkeyid = theinput.data("foreignkeyid");
			var sortby = theinput.data("sortby");

			var defaulttext = theinput.data("showdefault");
			if (!defaulttext) {
				defaulttext = "Search";
			}
			var defaultvalue = theinput.data("defaultvalue");
			var defaultvalueid = theinput.data("defaultvalueid");

			var url =
				apphome +
				"/views/modules/" +
				searchtype +
				"/autocomplete/datasearch.txt?" +
				"field=" +
				searchfield +
				"&operation=contains";
			if (defaultvalue != undefined) {
				url =
					url +
					"&defaultvalue=" +
					defaultvalue +
					"&defaultvalueid=" +
					defaultvalueid;
			}

			var allowClear = theinput.data("allowclear");
			if (allowClear == undefined) {
				allowClear = true;
			}

			theinput.select2({
				placeholder: defaulttext,
				allowClear: allowClear,
				minimumInputLength: 0,
				dropdownParent: getDropdownParent(theinput),
				ajax: {
					// instead of writing the
					// function to execute the
					// request we use Select2's
					// convenient helper
					url: url,
					dataType: "json",
					data: function (params) {
						var fkv = theinput
							.closest("form")
							.find("#list-" + foreignkeyid + "value")
							.val();
						if (fkv == undefined) {
							fkv = theinput
								.closest("form")
								.find("#list-" + foreignkeyid)
								.val();
						}
						var search = {
							page_limit: 15,
							page: params.page,
						};
						search[searchfield + ".value"] = params.term; // search
						// term
						if (fkv) {
							search["field"] = foreignkeyid; // search
							// term
							search["operation"] = "matches"; // search
							// term
							search[foreignkeyid + ".value"] = fkv; // search
							// term
						}
						if (sortby) {
							search["sortby"] = sortby; // search
							// term
						}
						return search;
					},
					processResults: function (data, params) {
						// parse the
						// results into
						// the format
						// expected by
						// Select2.
						var rows = data.rows;
						if (theinput.hasClass("selectaddnew")) {
							if (params.page == 1 || !params.page) {
								var addnewlabel = theinput.data("addnewlabel");
								var addnewdata = {
									name: addnewlabel,
									id: "_addnew_",
								};
								rows.unshift(addnewdata);
							}
						}
						// addnew
						params.page = params.page || 1;
						return {
							results: rows,
							pagination: {
								more: false,
								// (params.page * 30) <
								// data.total_count
							},
						};
					},
				},
				escapeMarkup: function (m) {
					return m;
				},
				templateResult: select2formatResult,
				templateSelection: select2Selected,
			});

			// TODO: Remove this?
			theinput.on("change", function (e) {
				if (e.val == "") {
					// Work around for a bug
					// with the select2 code
					var id = "#list-" + theinput.attr("id");
					$(id).val("");
				} else {
					// Check for "_addnew_" show ajax form
					var selectedid = theinput.val();

					if (selectedid == "_addnew_") {
						var clicklink = $("#" + theinput.attr("id") + "add");
						clicklink.trigger("click");

						e.preventDefault();
						theinput.select2("val", "");
						return false;
					}
					// Check for "_addnew_" show ajax form
					if (theinput.hasClass("selectautosubmit")) {
						if (selectedid) {
							//var theform = $(this).closest("form");
							var theform = $(this).parent("form");
							if (theform.hasClass("autosubmitform")) {
								theform.trigger("submit");
							}
						}
					}
				}
			});

			theinput.on("select2:open", function () {
				var selectId = $(this).attr("id");
				if (selectId) {
					$(
						".select2-search__field[aria-controls='select2-" +
							selectId +
							"-results']",
					).each(function (key, value) {
						value.focus();
					});
				} else {
					document
						.querySelector(".select2-container--open .select2-search__field")
						.focus();
				}
			});
		}
	});

	//-
	//List autocomplete multiple and accepting new options
	lQuery("select.listautocompletemulti").livequery(function () {
		// select2
		var theinput = $(this);
		var searchtype = theinput.data("searchtype");
		if (searchtype != undefined) {
			var searchfield = theinput.data("searchfield");

			var foreignkeyid = theinput.data("foreignkeyid");
			var sortby = theinput.data("sortby");

			var defaulttext = theinput.data("showdefault");
			if (!defaulttext) {
				defaulttext = "Search";
			}
			var defaultvalue = theinput.data("defaultvalue");
			var defaultvalueid = theinput.data("defaultvalueid");

			var url =
				apphome +
				"/components/xml/types/autocomplete/datasearch.txt?" +
				"field=" +
				searchfield +
				"&operation=contains&searchtype=" +
				searchtype;
			if (defaultvalue != undefined) {
				url =
					url +
					"&defaultvalue=" +
					defaultvalue +
					"&defaultvalueid=" +
					defaultvalueid;
			}

			var allowClear = theinput.data("allowclear");
			if (allowClear == undefined) {
				allowClear = true;
			}
			theinput.select2({
				placeholder: defaulttext,
				allowClear: allowClear,
				minimumInputLength: 0,
				tags: true,
				dropdownParent: getDropdownParent(theinput),
				ajax: {
					// instead of writing the
					// function to execute the
					// request we use Select2's
					// convenient helper
					url: url,
					dataType: "json",
					data: function (params) {
						var fkv = theinput
							.closest("form")
							.find("#list-" + foreignkeyid + "value")
							.val();
						if (fkv == undefined) {
							fkv = theinput
								.closest("form")
								.find("#list-" + foreignkeyid)
								.val();
						}
						var search = {
							page_limit: 15,
							page: params.page,
						};
						search[searchfield + ".value"] = params.term; // search
						// term
						if (fkv) {
							search["field"] = foreignkeyid; // search
							// term
							search["operation"] = "matches"; // search
							// term
							search[foreignkeyid + ".value"] = fkv; // search
							// term
						}
						if (sortby) {
							search["sortby"] = sortby; // search
							// term
						}
						return search;
					},
					processResults: function (data, params) {
						// parse the
						// results into
						// the format
						// expected by
						// Select2.
						var rows = data.rows;
						return {
							results: rows,
							pagination: {
								more: false,
								// (params.page * 30) <
								// data.total_count
							},
						};
					},
				},
				escapeMarkup: function (m) {
					return m;
				},
				templateResult: select2formatResult,
				templateSelection: select2Selected,
			});

			// TODO: Remove this?
			theinput.on("change", function (e) {
				if (e.val == "") {
					// Work around for a bug
					// with the select2 code
					var id = "#list-" + theinput.attr("id");
					$(id).val("");
				}
			});

			theinput.on("select2:open", function (e) {
				var selectId = $(this).attr("id");
				if (selectId) {
					$(
						".select2-search__field[aria-controls='select2-" +
							selectId +
							"-results']",
					).each(function (key, value) {
						value.focus();
					});
				} else {
					document
						.querySelector(".select2-container--open .select2-search__field")
						.focus();
				}
			});
		}
	});

	lQuery(".sidebarsubmenu").livequery("click", function (e) {
		e.stopPropagation();
	});

	lQuery(".mvpageclick").livequery("click", function (e) {
		$(".mvpageslist li").removeClass("current");
		$(this).closest("li").addClass("current");
		var pageurl = $(this).data("pageurl");
		$("#mainimage").attr("src", pageurl);
		$(".assetpanel-sidebar").removeClass("assetpanel-sidebar-ontop");
	});

	lQuery(".mvshowpages").livequery("click", function (e) {
		$(".assetpanel-sidebar").addClass("assetpanel-sidebar-ontop");
	});

	lQuery(".mvshowpages-toggle").livequery("click", function (e) {
		$(".assetpanel-sidebar").removeClass("assetpanel-sidebar-ontop");
		$(".assetpanel-sidebar").addClass("assetpanel-sidebar-hidden");
		$(".assetpanel-content").addClass("assetpanel-content-full");
		$(".mvshowpagestab").css("display", "block");
	});

	lQuery("#mainimageholder").livequery(function (e) {
		// Zooming code, only makes sense to run this when we
		// actually have the DOM
		if ($(this).position() == undefined) {
			// check if the
			// element isn't
			// there (best
			// practice
			// is...?)
			return;
		}
		var clickspot;
		var imageposition;
		var zoom = 30;
		var mainholder = $(this);
		var mainimage = $("#mainimage", mainholder);
		mainimage.width(mainholder.width());
		$(window).bind("mousewheel DOMMouseScroll", function (event) {
			var mainimage = $("#mainimage");
			if ($("#hiddenoverlay").css("display") == "none") {
				return true;
			}

			if (
				event.originalEvent.wheelDelta > 0 ||
				event.originalEvent.detail < 0
			) {
				// scroll up
				var w = mainimage.width();
				mainimage.width(w + zoom);
				var left = mainimage.position().left - zoom / 2;
				mainimage.css({
					left: left + "px",
				});
				return false;
			} else {
				// scroll down
				var w = mainimage.width();
				if (w > 100) {
					mainimage.width(w - zoom);
					var left = mainimage.position().left + zoom / 2;
					mainimage.css({
						left: left + "px",
					});
				}
				return false;
			}
		});

		mainimage.on("mousedown", function (event) {
			//console.log($(event.target));
			if ($(event.target).is(".zoomable")) {
				clickspot = event;
				imageposition = mainimage.position();
			}
			return false;
		});

		mainimage.on("mouseup", function (event) {
			clickspot = false;
			var mainimage = $("#mainimage");
			mainimage.removeClass("imagezooming");
			return false;
		});

		$(document).on("contextmenu", function (event) {
			clickspot = false;
		});

		mainimage.on("mousemove", function (event) {
			// if( isMouseDown() )

			if (clickspot) {
				//console.log(clickspot.pageX);
				var changetop = clickspot.pageY - event.pageY;
				var changeleft = clickspot.pageX - event.pageX;

				var left = imageposition.left - changeleft;
				var top = imageposition.top - changetop;
				var mainimage = $("#mainimage");
				mainimage.css({
					left: left + "px",
					top: top + "px",
				});
				mainimage.addClass("imagezooming");
			}
		});

		var dist1 = 0;

		mainimage.on("touchstart", function (e) {
			var touch = e.touches[0];
			var div = $(e.target);

			if (e.targetTouches.length == 2) {
				//check if two fingers touched screen
				dist1 = Math.hypot(
					//get rough estimate of distance between two fingers
					e.touches[0].pageX - e.touches[1].pageX,
					e.touches[0].pageY - e.touches[1].pageY,
				);
			} else {
				div.data("touchstartx", touch.pageX);
				div.data("touchstarty", touch.pageY);
			}
		});

		mainimage.on("touchend", function (e) {
			var touch = e.touches[0];
			var div = $(e.target);
			div.removeData("touchstartx");
			div.removeData("touchstarty");
		});

		var touchzoom = 10;
		var zoomed = false;
		var ww = window.innerWidth;
		var wh = window.innerHeight;

		mainimage.on("touchmove", function (e) {
			var div = $(e.target);
			//Zoom!
			if (e.targetTouches.length == 2 && e.changedTouches.length == 2) {
				// Check if the two target touches are the same ones that started
				var dist2 = Math.hypot(
					//get rough estimate of new distance between fingers
					e.touches[0].pageX - e.touches[1].pageX,
					e.touches[0].pageY - e.touches[1].pageY,
				);
				//alert(dist);
				var w = mainimage.width();

				if (dist1 > dist2) {
					//if fingers are closer now than when they first touched screen, they are pinching
					// Zoom out
					var neww = w - zoom;
					if (neww > 50) {
						//not smaller than 50px
						var newleft = mainimage.position().left + touchzoom / 2;
						var newright = newleft + mainimage.width();
						if (newleft < ww / 2 && newright > ww / 2) {
							mainimage.width(w - touchzoom);
							mainimage.css({
								left: left + "px",
							});
						}
						zoomed = true;
					} else {
						zoomed = false;
					}
				} else {
					//if fingers are further apart than when they first touched the screen, they are making the zoomin gesture
					// Zoom in

					var newleft = mainimage.position().left - touchzoom / 2;
					var newright = newleft + mainimage.width();
					if (newleft < ww / 2 && newright > ww / 2) {
						mainimage.width(w + touchzoom);
						mainimage.css({
							left: newleft + "px",
						});
					}
					zoomed = true;
				}
			} else {
				var touch = e.touches[0];
				//Move around only when zooming
				if (zoomed) {
					var left = mainimage.position().left;
					var top = mainimage.position().top;
					var newtop = left;

					var startingx = div.data("touchstartx");
					var startingy = div.data("touchstarty");
					var diffx = (touch.pageX - startingx) / 30; //?
					var diffy = (touch.pageY - startingy) / 30; //?

					if (Math.abs(diffx) > Math.abs(diffy)) {
						var change = Math.abs(diffx) / div.width();
						var newleft = left + diffx;
						var newright = newleft + mainimage.width();
						if (newleft < ww / 2 && newright > ww / 2) {
							mainimage.css({
								left: newleft + "px",
							});
						}
					} else {
						// up/down
						var change = Math.abs(diffy) / div.height();
						newtop = top + diffy;
						mainimage.css({
							top: newtop + "px",
						});
					}
				}

				/*
								 //Swipe?
								 var touch = e.touches[0];
									var startingx = div.data("touchstartx");
									var startingy = div.data("touchstarty");
									var diffx = touch.pageX - startingx;
									var diffy = touch.pageY - startingy;
									var swipe = false;
									if (Math.abs(diffx) > Math.abs(diffy)) {
										var change = Math.abs(diffx) / div.width();
										if (change > .2) {
											if (diffx > 0) {
												swipe = "swiperight";
											} else {
												swipe = "swipeleft";
											}
										}
									} else {
										// do swipeup and swipedown
										var change = Math.abs(diffy) / div.height();
										if (change > .2) {
											if (diffy > 0) {
												swipe = "swipedown";
											} else {
												swipe = "swipeup";
											}
										}

									}

									if (swipe) {
										//console.log(div);
										var event = {};
										event.originalEvent = e;
										event.preventDefault = function() {
										};
										// TODO: Find out why I can't trigger on $(e.target).trigger it
										// ignores us

										//$("#" + div.attr("id")).trigger(swipe);
									}
									*/
			}
		});
	});

	$("video").each(function () {
		$(this).append('controlsList="nodownload"');
		$(this).on("contextmenu", function (e) {
			e.preventDefault();
		});
	});

	lQuery(".dropdown-menu a.dropdown-toggle").livequery("click", function (e) {
		if (!$(this).next().hasClass("show")) {
			$(this)
				.parents(".dropdown-menu")
				.first()
				.find(".show")
				.removeClass("show");
		}
		var $subMenu = $(this).next(".dropdown-menu");
		$subMenu.toggleClass("show");

		$(this)
			.parents("li.nav-item.dropdown.show")
			.on("hidden.bs.dropdown", function (e) {
				$(".dropdown-submenu .show").removeClass("show");
			});

		return false;
	});
	lQuery(".dropdown-submenu .dropdown-menu a.dropdown-item").livequery(
		"click",
		function (e) {
			$(this).parents(".dropdown-menu.show").removeClass("show");
			return false;
		},
	);

	lQuery(".filterstoggle").livequery("click", function (e) {
		e.preventDefault();
		if ($("#col-filters").hasClass("filtersopen")) {
			//close
			$(".col-main").removeClass("filtersopen");
			saveProfileProperty("filtersbarstatus", false, function () {});
			$("#filterstoggle").show("fast", function () {
				$(document).trigger("resize");
			});
		} else {
			//open
			$("#filterstoggle").hide("fast", function () {
				$(document).trigger("resize");
			});
			$("#col-filters").addClass("filtersopen");
			$(".col-main").addClass("filtersopen");
			saveProfileProperty("filtersbarstatus", true, function () {});
		}
		return false;
	});

	//Left Column Toggle
	lQuery(".lefttoggle").livequery("click", function (e) {
		e.preventDefault();
		var colleftwidth = $("#col-left").data("colleftwidth");
		if (!$.isNumeric(colleftwidth)) {
			colleftwidth = $("#col-left").width();
		}
		if ($("#col-left").hasClass("leftopen")) {
			//close
			$("#col-left").removeClass("leftopen");
			$("#col-left").css("width", 0);
			$(".col-main").removeClass("leftopen");
			$(".pushcontent").css("margin-left", 0);
			$("#lefttoggle").show("fast", function () {
				$(document).trigger("resize");
			});
			saveProfileProperty("leftbarstatus", false, function () {});
		} else {
			//open
			$("#col-left").addClass("leftopen");
			if (colleftwidth) {
				$("#col-left").css("width", colleftwidth);
			}
			$(".col-main").addClass("leftopen");
			if (colleftwidth) {
				$(".pushcontent").css("margin-left", colleftwidth + "px");
			}
			$("#lefttoggle").hide("fast", function () {
				$(document).trigger("resize");
			});
			saveProfileProperty("leftbarstatus", true, function () {});
		}

		return false;
	});

	lQuery(".sidebar-toggler").livequery("click", function (e) {
		e.preventDefault();
		var toggler = $(this);
		var data = toggler.data;
		var targetdiv = toggler.data("targetdiv");

		if (toggler.data("action") == "hide") {
			//hide sidebar
			var url = apphome + "/components/sidebars/user/hide.html";

			$.ajax({
				url: url,
				async: false,
				data: data,
				success: function (data) {
					$("#" + targetdiv).html(data);
					$(".emrightcontent").removeClass("empushcontent");
					saveProfileProperty("usersidebarhidden", "true");
				},
			});
		} else {
			var url = apphome + "/components/sidebars/user/show.html";
			$.ajax({
				url: url,
				async: false,
				data: data,
				success: function (data) {
					$("#" + targetdiv).html(data);
					$(".emrightcontent").addClass("empushcontent");
					saveProfileProperty("usersidebarhidden", "false");
				},
			});
		}
		$(window).trigger("resize");
	});

	lQuery(".assetpicker .removefieldassetvalue").livequery(
		"click",
		function (e) {
			e.preventDefault();
			var picker = $(this).closest(".assetpicker");
			var detailid = $(this).data("detailid");

			picker.find("#" + detailid + "-file-info").html("");
			picker.find("#" + detailid + "-value").val("");
			picker.find("#" + detailid + "-file").val("");

			var theform = $(picker).closest("form");
			theform = $(theform);
			if (theform.hasClass("autosubmit")) {
				theform.trigger("submit");
			}
		},
	);

	$('[data-toggle="tooltipb"]').tooltip();

	//Sidebar Custom Width
	lQuery(".col-left-resize").livequery(function () {
		var slider = $(this);
		var column = $(this).closest(".col-left");
		var content = $(".pushcontent");

		var clickspot;
		var startwidth;
		var width;

		slider.on("mousedown", function (event) {
			if (!clickspot) {
				clickspot = event;
				startwidth = column.width();
				return false;
			}
		});
		$(window).on("mouseup", function (event) {
			if (clickspot) {
				clickspot = false;
				if (width != "undefined") {
					saveProfileProperty("colleftwidth", width, function () {
						$(document).trigger("resize");
					});
				}
				return false;
			}
		});
		$(window).on("mousemove", function (event) {
			if (clickspot) {
				width = 0;
				var changeleft = event.pageX - clickspot.pageX;
				width = startwidth + changeleft;
				if (width < 220) {
					width = 220;
				}
				column.width(width);
				column.data("colleftwidth", width);
				$(".pushcontent").css("margin-left", width + "px");
				event.preventDefault();
				$(document).trigger("resize");
				return false;
			}
		});
	});

	//Moved From settings.js
	lQuery("#datamanager-workarea th.sortable").livequery("click", function () {
		var table = $("#main-results-table");
		var args = {
			oemaxlevel: 1,
			hitssessionid: table.data("hitssessionid"),
			origURL: table.data("origURL"),
			catalogid: table.data("catalogid"),
			searchtype: table.data("searchtype"),
		};
		var column = $(this);
		var fieldid = column.data("fieldid");
		var apphome = app.data("home") + app.data("apphome");

		if (column.hasClass("currentsort")) {
			if (column.hasClass("up")) {
				args.sortby = fieldid + "Down";
			} else {
				args.sortby = fieldid + "Up";
			}
		} else {
			$("#datamanager-workarea th.sortable").removeClass("currentsort");
			column.addClass("currentsort");
			column.addClass("up");
			args.sortby = fieldid + "Up";
		}
		$("#datamanager-workarea").load(
			apphome + "/views/settings/lists/datamanager/list/columnsort.html",
			args,
		);
	});

	function replaceAll(str, find, replace) {
		find = escapeRegExp(find);
		return str.replace(new RegExp(find, "g"), replace);
	}

	function escapeRegExp(str) {
		return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
	}

	lQuery(".sortviews").livequery(function () {
		var sortable = $(this);
		var path = sortable.data("path");
		if (typeof sortable.sortable == "function") {
			sortable.sortable({
				axis: "y",
				cancel: ".no-sort",
				update: function (event, ui) {
					//debugger;
					var data = sortable.sortable("serialize");
					data = replaceAll(data, "viewid[]=", "|");
					data = replaceAll(data, "&", "");
					data = data.replace("|", "");
					var args = {};
					args.items = data;
					args.viewpath = sortable.data("viewpath");
					args.searchtype = sortable.data("searchtype");
					args.assettype = sortable.data("assettype");
					args.viewid = sortable.data("viewid");
					$.ajax({
						data: args,
						type: "POST",
						url: path,
					});
				},
				stop: function (event, ui) {
					//db id of the item sorted
					//alert(ui.item.attr('plid'));
					//db id of the item next to which the dragged item was dropped
					//alert(ui.item.prev().attr('plid'));
				},
			});
		}
	});

	var listtosort = $(".listsort");
	if (typeof listtosort.sortable == "function") {
		listtosort.sortable({
			axis: "y",
			cancel: ".no-sort",
			stop: function (event, ui) {
				var path = $(this).data("path");

				var data = "";

				// var ids = new Array();
				$(this)
					.find("li")
					.each(function (index) {
						if (!$(this).hasClass("no-sort")) {
							var id = $(this).attr("id");
							data = data + "ids=" + id + "&";
						}
					});
				// POST to server using $.post or $.ajax
				$.ajax({
					data: data,
					type: "POST",
					url: path,
				});
			},
		});
	}

	$("#setup-view").on("click", function () {
		if ($("#views-settings").hasClass("open")) {
			$("#views-header").height(16);
			$("#views-settings").hide();
			$("#views-settings").removeClass("open");
		} else {
			$("#views-header").height("auto");
			$("#views-settings").show();
			$("#views-settings").addClass("open");
		}
	});

	$("#renderastable").on("click", function () {
		if ($("#renderastable").is(":checked")) {
			$("#rendertableoptions").show();
		} else {
			$("#rendertableoptions").hide();
		}
	});

	lQuery(".copyToClipboard").livequery("click", function () {
		console.log("object");
		var copyText = $(this).data("copy");
		if (navigator.clipboard) {
			navigator.clipboard.writeText(copyText);
		} else {
			var textArea = document.createElement("textarea");
			textArea.value = copyText;
			document.body.appendChild(textArea);
			textArea.select();
			document.execCommand("copy");
			document.body.removeChild(textArea);
		}
		var copyFeedback = $(this).find(".copy-feedback");
		var prevText = copyFeedback.text();
		copyFeedback.text("Copied!");
		setTimeout(function () {
			copyFeedback.text(prevText);
		}, 3000);
	});

	lQuery(".copytoclipboard").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var btn = $(this);
		var copytextcontainer = btn.data("copytext");
		var copyText = $("#" + copytextcontainer);
		copyText.select();
		document.execCommand("copy");
		var alertdiv = btn.data("targetdiv");
		if (alertdiv) {
			//console.log(alertdiv);
			$("#" + alertdiv)
				.show()
				.fadeOut(2000);
		}
	});

	lQuery("#copyuserpicker .rowclick").livequery("click", function (e) {
		e.preventDefault();
		//$(this).closest(".modal").modal("hide");
		var picker = $("#copyuserpicker");
		var row = $(this);
		var rowid = row.attr("rowid");

		var targetdiv = picker.data("targetdiv");
		var targetdiv = $("#" + targetdiv);
		var nextpage = picker.data("nextpage");
		var options = picker.data();
		options.copyid = picker.data("userid");
		options.copytoid = rowid;
		$.get(nextpage, options, function (data) {
			targetdiv.replaceWith(data);
		});
	});

	lQuery(".pickemoticon .emoticonmenu span").livequery("click", function () {
		var menuitem = $(this);

		var aparent = $(menuitem.parents(".pickemoticon"));
		//console.log(aparent.data());

		var saveurl = aparent.data("toggleurl");
		//Save
		var options = aparent.data();
		options.oemaxlevel = 1;
		options.reactioncharacter = menuitem.data("hex");
		$.ajax({
			url: saveurl,
			async: true,
			data: options,
			success: function (data) {
				$("#chatter-message-" + aparent.data("messageid")).html(data);
				//reload message
			},
		});
	});
}; // uiload

var resizecolumns = function () {
	//make them same top
	var sidebarsposition = $("#resultsdiv").position();
	var sidebarstop = 0;
	if (typeof sidebarsposition != "undefined") {
		sidebarstop = sidebarsposition.top;
		$(".col-filters").css("top", sidebarstop + "px");
		$(".col-left").css("top", sidebarstop + "px");
	}

	var header_height = $("#header").outerHeight();
	var nav_height = $("#EMnav").outerHeight();
	var footer_height = $("#footer").outerHeight();
	var resultsheader_height = 0;

	if ($(".collection-header").outerHeight()) {
		resultsheader_height = $(".collection-header").outerHeight();
	} else if ($(".filtered").outerHeight()) {
		resultsheader_height = $(".filtered").outerHeight();
	}

	var allheights =
		header_height + nav_height + footer_height + resultsheader_height;
	var columnsheight = $("body").outerHeight() - allheights;

	var sidebartop = 1;
	$(".col-main").each(function () {
		var col = $(this);
		if (col.hasClass("col-left") && col.hasClass("fixedheight")) {
			return true;
		}
		var thisheight = col.outerHeight();
		if (col.children(0) && col.children(0).hasClass("col-main-inner")) {
			thisheight = col.children(0).outerHeight();
		}

		if (thisheight > columnsheight) {
			columnsheight = thisheight;
		}
	});
	$(".col-filters").css("height", columnsheight);
	if (!$(".col-left").hasClass("fixedheight")) {
		$(".col-left").css("height", columnsheight);
	} else {
		allheights = header_height + nav_height + resultsheader_height;
		var windowh = $(window).height();
		windowh = windowh - allheights;
		$(".col-left").css("height", columnsheight);
		$(".col-left > .col-main-inner").css("height", windowh);
	}
	$(".col-sidebar").css("min-height", columnsheight);
	if ($(".col-content-main").parent().hasClass("settingslayout")) {
		$(".col-content-main").css(
			"min-height",
			columnsheight + sidebarstop + "px",
		);
	} else {
		$(".col-content-main").css(
			"min-height",
			columnsheight + sidebarstop + "px",
		);
	}

	$(".pushcontent").css(
		"height",
		"calc(100% - " + resultsheader_height + "px)",
	);
};

var resizegallery = function () {
	var container = $("#emslidesheet");
	if (container.length) {
		var containerw = container.width();
		var boxes = Math.floor(containerw / 230);
		var boxw = Math.floor(containerw / boxes) - 12;
		$("#emslidesheet .emthumbbox").width(boxw);
	}
};

$(document).ready(function () {
	uiload();
	resizecolumns();
	resizegallery();
});

$(window).on("resize", function () {
	resizecolumns();
	resizegallery();
});

jQuery(window).on("ajaxsocketautoreload", function () {
	$(".ajaxsocketautoreload").each(function () {
		var cell = $(this);
		var path = cell.data("ajaxpath");
		jQuery.ajax({
			url: path,
			async: false,
			data: {},
			success: function (data) {
				cell.replaceWith(data);
			},
			xhrFields: {
				withCredentials: true,
			},
			crossDomain: true,
		});
	});
});
