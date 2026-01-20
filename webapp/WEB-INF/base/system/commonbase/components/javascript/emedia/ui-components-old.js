var siteroot;
var apphome;

window.VIDEOJS_NO_DYNAMIC_STYLE = true;

function initializeUI() {
	var app = jQuery("#application");
	siteroot = app.data("siteroot");
	apphome = app.data("apphome");
	var themeprefix = app.data("themeprefix");
	if (siteroot !== undefined) {
		themeprefix = siteroot + themeprefix;
	}

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
	function getSelect2Placeholder() {
		var placeholder = $(this).attr("placeholder");
		if (!placeholder) {
			placeholder = $(this).data("placeholder");
		}
		if (!placeholder) {
			placeholder = $(this).find("option[value='']").text();
		}
		if (!placeholder) {
			var label = $(this).closest(".inputformrow").find("label");
			//console.log(label);
			if (label.length) {
				placeholder = label.text().trim();
				if (placeholder) {
					return "Select " + placeholder.toLowerCase();
				}
			}
		}
		if (!placeholder) {
			return "Select an option";
		}
		return placeholder;
	}

	lQuery("select.select2").livequery(function () {
		var theinput = $(this);
		var allowClear = $(this).data("allowclear");
		if (allowClear == undefined) {
			allowClear = true;
		}
		var placeholder = getSelect2Placeholder.call(this);

		var dropdownCssClass = "";
		if (theinput.data("dropdowncssclass") != null) {
			dropdownCssClass = theinput.data("dropdowncssclass");
		}

		if ($.fn.select2) {
			theinput.select2({
				allowClear: allowClear,
				placeholder: placeholder,
				dropdownParent: getDropdownParent(theinput),
				dropdownCssClass: dropdownCssClass,
			});
			theinput.on("select2:open", function (e) {
				var selectId = $(this).attr("id");
				if (selectId) {
					$(
						".select2-search__field[aria-controls='select2-" +
							selectId +
							"-results']"
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
	/*
	$(".select2simple").select2({
		 minimumResultsForSearch: Infinity
	});
	*/
	lQuery("select.listdropdown").livequery(function () {
		var theinput = $(this);

		var placeholder = getSelect2Placeholder.call(this);

		var allowClear = theinput.data("allowclear");

		if (allowClear == undefined) {
			allowClear = true;
		}
		if ($.fn.select2) {
			theinput.select2({
				placeholder: placeholder,
				allowClear: allowClear,
				minimumInputLength: 0,
				dropdownParent: getDropdownParent(theinput),
			});
			theinput.on("change", function (e) {
				//console.log("XX changed")
				if (theinput.hasClass("uifilterpicker")) {
					var selectedids = theinput.val();
					if (selectedids) {
						//console.log("XX has class" + selectedids);
						var parent = theinput.closest(".filter-box-options");
						//console.log(parent.find(".filtercheck"));
						parent.find(".filtercheck").each(function () {
							var filter = $(this);
							filter.prop("checked", false); //remove?
						});
						for (i = 0; i < selectedids.length; i++) {
							//$entry.getId()${fieldname}_val
							var selectedid = selectedids[i];
							var fieldname = theinput.data("fieldname");
							var targethidden = $("#" + selectedid + fieldname + "_val");
							targethidden.prop("checked", true);
						}
					}
				}
				if ($(this).parents(".ignore").length == 0) {
					$(this).valid();
					var form = $(this).closest("form");
					form.valid();
				}
			});
			theinput.on("select2:open", function (e) {
				var selectId = $(this).attr("id");
				if (selectId) {
					$(
						".select2-search__field[aria-controls='select2-" +
							selectId +
							"-results']"
					).each(function (key, value) {
						value.focus();
					});
				} else {
					document
						.querySelector(".select2-container--open .select2-search__field")
						.focus();
				}
			});
			/*
			theinput.on("select2:select", function () {
				if ($(this).parents(".ignore").length == 0) {
					$(this).valid();
					var form = $(this).closest("form");
					if(form)
					{
					form.validate();
					}
				}
			});

			theinput.on("select2:unselect", function () {
				if ($(this).parents(".ignore").length == 0) {
					$(this).valid();
					var form = $(this).closest("form");
					if(form)
					{
					form.validate();
					}
				}
			});*/
		}
	});

	lQuery(".TODELETEselect2editable").livequery(function () {
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
		if ($.fn.select2) {
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
							"-results']"
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

	lQuery("select.ajax").livequery("change", function (e) {
		var inlink = $(this);
		var nextpage = inlink.data("href");
		nextpage = nextpage + inlink.val();
		var targetDiv = inlink.data("targetdiv");
		if (!targetDiv) {
			targetDiv = inlink.attr("targetdiv");
		}

		var options = inlink.data();
		options[inlink.attr("name")] = inlink.val();
		$.get(nextpage, options, function (data) {
			if (targetDiv) {
				var cell = $("#" + targetDiv);
				cell.html(data);
			} else {
				if (!targetDiv) {
					targetDiv = inlink.data("targetdivinner");
					var cell = $("#" + targetDiv);
					cell.replaceWith(data);
				}
			}
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
	function humanFileSize(bytes) {
		var thresh = 1000;
		if (Math.abs(bytes) < thresh) {
			return bytes + " B";
		}
		var units = ["kB", "MB", "GB", "TB"];
		var u = -1;
		do {
			bytes /= thresh;
			++u;
		} while (
			Math.round(Math.abs(bytes) * 10) / 10 >= thresh &&
			u < units.length - 1
		);
		return bytes.toFixed(1) + " " + units[u];
	}
	lQuery(".autoFileSize").livequery(function () {
		var size = $(this).text();
		size = parseInt(size);
		if (!isNaN(size)) {
			$(this).text(humanFileSize(size));
		}
	});

	// deprecated, use data-confirm
	lQuery(".confirm").livequery("click", function (e) {
		if ($(this).hasClass("ajax")) {
			return;
		}
		var inText = $(this).data("confirm");
		if (!inText) {
			inText = "Are you sure?";
		}
		if (confirm(inText)) {
			return;
		} else {
			e.preventDefault();
			e.stopImmediatePropagation();
		}
	});

	lQuery(".uibutton").livequery(function () {
		$(this).button();
	});

	lQuery(".fader").livequery(function () {
		var _this = $(this);
		if (_this.hasClass("alert-save")) {
			_this.prepend('<span class="bi bi-check-circle-fill ns"></span>');
			_this.append('<button><span class="bi bi-x-circle ns"></span>');
		} else if (_this.hasClass("alert-error")) {
			_this.prepend('<span class="bi bi-info-circle-fill ns"></span>');
			_this.append('<button><span class="bi bi-x ns"></span>');
		}
		var timeout = 4000;
		if (_this.hasClass("fade-quick")) {
			timeout = 2000;
		}
		setTimeout(function () {
			_this.fadeOut(500, function () {
				_this.remove();
			});
		}, timeout);
		_this.find("button").click(function () {
			_this.fadeOut(500, function () {
				_this.remove();
			});
		});
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

	lQuery(".redirecttopage").livequery(function () {
		var url = $(this).data("redirectok");
		window.location.href = url;
	});

	lQuery("form.autosubmit").livequery(function () {
		var form = $(this);

		$("select", form).change(function (e) {
			e.stopPropagation();
			$(form).trigger("submit");
		});
		/* Todo: use onblur
		$("input", form).on("focusout", function (event) {
			$(form).trigger("submit");
		});
		*/
		$("input", form).on("keyup", function (e) {
			//Enter Key handled by default the submit
			if (e.keyCode == 13) {
				return;
			}
			e.preventDefault();
			e.stopPropagation();
			$(form).trigger("submit");
		});
		$(
			'input[type="file"],input[name="date.after"],input[type="checkbox"]',
			form
		).on("change", function (e) {
			e.stopPropagation();
			$(form).trigger("submit");
		});
	});

	lQuery("select.ajaxautosubmitselect").livequery(function () {
		var select = $(this);
		select.change(function () {
			var targetdiv = select.data("targetdiv");
			var link = select.data("url");
			var param = select.data("parametername");

			var url = link + "?" + param + "=" + select.val();

			var options = select.data();
			$("#" + targetdiv).load(url, options, function () {});
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
		e.stopPropagation();
		e.stopImmediatePropagation();

		if ("updateAllCK5" in window) updateAllCK5();

		$(this).prop("disabled", true);
		var theform = $(this).closest("form");

		var clicked = $(this);
		if (clicked.data("updateaction")) {
			var newaction = clicked.attr("href");
			theform.attr("action", newaction);
		}
		console.log("Submit Form " + theform);
		theform.trigger("submit");

		return false;
	});
	lQuery(".submitform").livequery("dblclick", function (e) {
		e.preventDefault();
		console.log("DblClick!");
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
				e.preventDefault();
				e.stopImmediatePropagation();
				e.stopPropagation();

				if ("updateAllCK5" in window) updateAllCK5();

				theform.data("readytosubmit", "true");
				theform.find(".oehtmlinput").trigger("blur");
				theform.trigger("submit");
			}
		}
	);

	lQuery(".selectsubmitform").livequery("change", function (e) {
		e.preventDefault();
		var theform = $(this).closest("form");
		theform.trigger("submit");
	});

	lQuery(".clearallfilters").livequery("click", function () {
		closetypeaheadmodal();
	});

	function closetypeaheadmodal() {
		var modal = $(".typeaheadmodal");
		if (modal.length > 0) {
			modal.hide();
		}
		$("#searchinput").val("");
	}

	lQuery(".entitydialogback").livequery("click", function (event) {
		var link = $(this);
		event.preventDefault();
		var parentcontainerid = link.data("parentcontainerid");
		var parent = $("#" + parentcontainerid);
		if (parent.length) {
			var grandparent = parent.parent().closest(".entitydialog");
			var urlbar = parent.data("urlbar");
			$(window).trigger("autoreload", [parent]);
			tabbackbutton(grandparent);
			if (urlbar !== undefined) {
				history.pushState($("#application").html(), null, urlbar);
			}
		}
	});

	lQuery("a.triggerjs").livequery("click", function (event) {
		event.preventDefault();
		var link = $(this);
		var id = link.data("triggerid");
		var action = link.data("triggeraction");
		if (id) {
			$("#" + id).trigger(action); //callback?
		}
		return;
	});

	lQuery("a.entity-tab-close").livequery("click", function (event) {
		event.preventDefault();
		if ($(".entity-tab").length > 1) {
			//only remove if more than one tab
			var tabcontainer = $(this).closest(".entity-tab");
			var open = $(".current-entity");
			if (open.data("entityid") == $(this).data("entityid")) {
				open = tabcontainer.prev();
			}
			if (open.length == 0) {
				open = tabcontainer.next();
			}
			var entityid = open.find(".entity-tab-label").data("entityid");
			$(".entity-tab").removeClass("current-entity");
			open.addClass("current-entity");
			tabcontainer.remove();
			$(
				'div[data-id="' + $(this).data("entityid") + '"].entity-tab-content'
			).remove();
			$(".entity-tab-content").hide();
			$('div[data-id="' + entityid + '"].entity-tab-content').show();
		} else {
			//close dialog
			closeemdialog($(this).closest(".modal"));
		}
	});

	//to be removed
	lQuery(".entitytabactions").livequery("click", function (event) {
		event.preventDefault();
		var link = $(this);
		var tabaction = link.data("tabtype");
		var uploadmedia = link.data("uploadmedia");
		var lightboxid = link.data("lightboxid");
		var tabsection = link.data("tabsection");
		var entity = link.closest(".entitydialog");
		entity.data("entitytabopen", tabaction);
		entity.data("uploadmedia", uploadmedia);
		entity.data("tabsection", tabsection);
		entity.data("lightboxid", lightboxid);
		var parent = entity.parent(".entitydialog");
		$(window).trigger("autoreload", [entity]);
		if (parent !== undefined) {
			tabbackbutton(parent);
		}
	});

	lQuery(".btn-savepublishing").livequery("click", function (event) {
		var form = $(this).closest("form");

		if ($("#wgEnabledLabel", form).is(":checked")) {
			$(".tabactionpublishing").addClass("statusenabled");
		} else {
			$(".tabactionpublishing").removeClass("statusenabled");
		}
	});

	var wgst;
	lQuery(".wg-autosave").livequery("change", function () {
		$(this).closest("form").submit();
		$("#wgAutoSaved").show();
		if (wgst) clearTimeout(wgst);
		wgst = setTimeout(function () {
			$("#wgAutoSaved").fadeOut();
		}, 2000);
	});

	lQuery(".autoopenemdialog").livequery(function (e) {
		e.preventDefault();
		e.stopPropagation();
		$(this).emDialog();
	});

	lQuery(".mediaboxheader").livequery("click", function (event) {
		event.preventDefault();
		var box = $(this).closest(".entity-media-box");
		box.toggleClass("collapsedmediabox");
		if (box.hasClass("collapsedmediabox")) {
			$(this)
				.find(".expandmediaboxicon")
				.removeClass("fa-caret-down")
				.addClass("fa-caret-right");
		} else {
			$(this)
				.find(".expandmediaboxicon")
				.removeClass("fa-caret-right")
				.addClass("fa-caret-down");
		}
	});

	function expandmediabox(item) {
		var box = item.closest(".entity-media-box");
		box.removeClass("collapsedmediabox");
		$(this)
			.find(".expandmediaboxicon")
			.removeClass("fa-caret-right")
			.addClass("fa-caret-down");
	}

	lQuery(".expandmediabox").livequery("click", function (e) {
		expandmediabox($(this));
	});

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
					1000
				);
				$(this).data("intval", intval);
			},
			function () {
				var path = this.src.split("?")[0];
				this.src = path + "?frame=0";
				var intval = $(this).data("intval");
				clearInterval(intval);
			}
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
		var isSelectMode = theinput.hasClass("choose-select");

		var searchtype = theinput.data("searchtype");
		var searchfield = theinput.data("searchfield");
		// var catalogid = theinput.data("listcatalogid");
		var sortby = theinput.data("sortby");
		var defaulttext = theinput.data("showdefault");
		if (!defaulttext) {
			defaulttext = "Search";
		}
		var allowClear = $(this).data("allowclear");
		if (allowClear == undefined) {
			allowClear = true;
		}

		var url = theinput.data("url");
		if (!url) {
			url = apphome + "/components/xml/types/autocomplete/tagsearch.txt";
		}

		if ($.fn.select2) {
			var options = theinput.children("option");
			var preloadedData = [];
			options.each(function () {
				var option = $(this);
				if (isSelectMode && option.val() != "" && option.text() != "") {
					preloadedData.push({
						id: option.val(),
						name: option.text(),
					});
				}
			});

			theinput.select2({
				data: preloadedData,
				tags: true,
				placeholder: defaulttext,
				allowClear: allowClear,
				dropdownParent: getDropdownParent(theinput),
				selectOnBlur: true,
				delay: 150,
				ajax: {
					url: url,
					xhrFields: {
						withCredentials: true,
					},
					crossDomain: true,
					dataType: "json",
					data: function (params) {
						var search = {
							page_limit: 15,
							page: params.page || 1,
						};
						search["field"] = searchfield;
						search["operation"] = "contains";
						search["searchtype"] = searchtype;
						search[searchfield + ".value"] = params.term || "";
						search["sortby"] = sortby;
						return search;
					},
					processResults: function (data, params) {
						params.page = params.page || 1;
						var results = data.rows;
						if (results.length == 0 && isSelectMode) {
							results = preloadedData;
						}
						return {
							results: results,
							pagination: {
								more: false,
							},
						};
					},
				},
				escapeMarkup: function (m) {
					return m;
				},
				templateResult: select2formatResult,
				templateSelection: select2Selected,
				tokenSeparators: ["|", ","],
				separator: "|",
			});
		}

		theinput.on("select2:open", function (e) {
			var selectId = $(this).attr("id");
			if (selectId) {
				$(
					".select2-search__field[aria-controls='select2-" +
						selectId +
						"-results']"
				).each(function (key, value) {
					value.focus();
				});
			} else {
				document
					.querySelector(".select2-container--open .select2-search__field")
					.focus();
			}
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

	lQuery("select.searchtags").livequery(function () {
		var theinput = $(this);

		var searchfield = theinput.data("searchfield");
		var defaulttext = theinput.data("showdefault");
		if (!defaulttext) {
			defaulttext = "Search";
		}
		var allowClear = $(this).data("allowclear");
		if (allowClear == undefined) {
			allowClear = true;
		}

		var url = theinput.data("url");
		if (!url) {
			url = apphome + "/components/xml/types/autocomplete/tagsearch.txt";
		}

		var form = theinput.closest("form");

		if ($.fn.select2) {
			theinput.select2({
				tags: true,
				placeholder: defaulttext,
				allowClear: allowClear,
				dropdownParent: getDropdownParent(theinput),
				selectOnBlur: true,
				delay: 150,
				minimumInputLength: 2,
				ajax: {
					url: url,
					xhrFields: {
						withCredentials: true,
					},
					crossDomain: true,
					dataType: "json",
					data: function (params) {
						var search = {
							page_limit: 15,
							page: params.page,
						};
						search["field"] = searchfield;
						search["operation"] = "freeform";
						search[searchfield + ".value"] = params.term;

						return search;
					},
					processResults: function (data, params) {
						var term = params.term;
						var currentterms = $(theinput).val();
						if (currentterms) {
							term = currentterms.join(" ") + " " + term;
						}

						$("#descriptionvalue", form).val(term.trim());
						form.trigger("submit");

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
					var selected = $(this).select2("data");
					var terms = "";
					selected.forEach(function (item, index, arr) {
						terms = terms + " " + item["id"] + "";
					});
					$("#descriptionvalue", form).val(terms);
					form.trigger("submit");
				}
			});
			theinput.on("select2:unselect", function () {
				if ($(this).parents(".ignore").length == 0) {
					$(this).valid();
					var selected = $(this).select2("data");
					var terms = "";
					selected.forEach(function (item, index, arr) {
						terms = terms + " " + item["id"] + "";
					});
					$("#descriptionvalue", form).val(terms);
					form.trigger("submit");
				}
			});
		}
	});

	lQuery("a.changeuserprofile").livequery("click", function (e) {
		e.stopImmediatePropagation();
		e.preventDefault();
		var link = $(this);
		var propertyname = link.data("propertyname");
		var newvalue = link.data(propertyname);
		var redir = link.data("redirect");
		if (!redir) {
			redir = link.attr("href");
		}
		saveProfileProperty(propertyname, newvalue, function () {
			if (redir) {
				window.location.href = redir;
			}
		});
	});

	lQuery("input.changeuserprofile").livequery("change", function () {
		var input = $(this);
		var propertyname = input.data("propertyname");
		var checked = input.is(":checked");
		var newvalue = null;
		if (checked) {
			newvalue = input.val();
		}
		saveProfileProperty(propertyname, newvalue, function () {
			$(window).trigger("checkautoreload", [input]);
		});
	});

	lQuery(".resetsearch").livequery("click", function () {
		saveProfileProperty("mainsearchmodule", "", function () {});
	});

	lQuery(".filter-showall-group").livequery(function () {
		var parent = $(this);
		var input = parent.find("#filter-showall");
		// var hint = parent.find("small.hint");
		function handleFilerShowAllChange(e) {
			var checked = input.is(":checked");
			if (!checked) {
				parent.addClass("inclusive");
				parent.removeClass("exclusive");
				//     hint.html(
				//       "Shows results that match <b><u>ANY</u></b> selected filter."
				//     );
			} else {
				parent.removeClass("inclusive");
				parent.addClass("exclusive");
				//     hint.html(
				//       "Shows results that match <b><u>ALL</u></b> selected filters."
				//     );
			}
		}
		handleFilerShowAllChange();
		input.change(handleFilerShowAllChange);
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
					'<div class="tab-pane" id="' + panelid + '" ></div>'
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
						}
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
				prevtab.find("a").click();

				if (prevtab.hasClass("firstab")) {
					tab.closest("li").remove();
				}
			}
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
			}
		);
		return false;
	});

	lQuery(".createmedia-btn").livequery("click", function (e) {
		$(".createmedia-tab").removeClass("createmedia-selected");
		$(this).closest(".createmedia-tab").addClass("createmedia-selected");
	});

	lQuery("select.listautocomplete").livequery(function () // select2
	{
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
			if ($.fn.select2) {
				theinput.select2({
					placeholder: defaulttext,
					allowClear: allowClear,
					minimumInputLength: 0,
					dropdownParent: getDropdownParent(theinput),
					ajax: {
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
						if (theinput.hasClass("uifilterpicker")) {
							//Not used?
							//$entry.getId()${fieldname}_val
							var fieldname = theinput.data("fieldname");
							var targethidden = $("#" + selectedid + fieldname + "_val");
							targethidden.prop("checked", true);
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
								"-results']"
						).each(function (key, value) {
							value.focus();
						});
					} else {
						document
							.querySelector(".select2-container--open .select2-search__field")
							.focus();
					}
					$(document).on("click", function (evt) {
						if (!$(evt.target).closest(".select2-container").length) {
							theinput.select2("close");
							$(this).off(evt);
						}
					});
				});
			}
		}
	});
	//-
	//List autocomplete multiple and accepting new options
	lQuery("select.listautocompletemulti").livequery(function () // select2
	{
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
							"-results']"
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
	
	
	lQuery("select.customlistautocomplete").livequery(function () // select2
		{
			var theinput = $(this);
			var url = theinput.data("url");
			var defaulttext = theinput.data("showdefault");
			if (!defaulttext) {
				defaulttext = "Search";
			}

			var allowClear = theinput.data("allowclear");
			if (allowClear == undefined) {
				allowClear = true;
			}
			if ($.fn.select2) {
				theinput.select2({
					placeholder: defaulttext,
					allowClear: allowClear,
					minimumInputLength: 0,
					dropdownParent: getDropdownParent(theinput),
					ajax: {
						url: url,
						dataType: "json",
						data: function (params) {
							var search = {
								page_limit: 15,
								page: params.page,
							};
							search["term"] = params.term; // search
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
						if (theinput.hasClass("uifilterpicker")) {
							//Not used?
							//$entry.getId()${fieldname}_val
							var fieldname = theinput.data("fieldname");
							var targethidden = $("#" + selectedid + fieldname + "_val");
							targethidden.prop("checked", true);
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
								"-results']"
						).each(function (key, value) {
							value.focus();
						});
					} else {
						document
							.querySelector(".select2-container--open .select2-search__field")
							.focus();
					}
					$(document).on("click", function (evt) {
						if (!$(evt.target).closest(".select2-container").length) {
							theinput.select2("close");
							$(this).off(evt);
						}
					});
				});
			}
		});

	lQuery(".sidebarsubmenu").livequery("click", function (e) {
		e.stopPropagation();
	});

	lQuery(".mvpageclick").livequery("click", function (e) {
		e.preventDefault();
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
		//mainimage.width(mainholder.width());
		$(window).bind("mousewheel DOMMouseScroll", function (event) {
			var mainimage = $("#mainimage");
			if (
				$("#hiddenoverlay").css("display") == "none" ||
				!$("#mainimage").length
			) {
				return true;
			}

			event.preventDefault();

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
					e.touches[0].pageY - e.touches[1].pageY
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
					e.touches[0].pageY - e.touches[1].pageY
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

		jQuery(document).ready(function () {
			mainimage.width(mainholder.width());
		});
	});

	$("video").each(function () {
		$(this).append('controlsList="nodownload"');
		$(this).on("contextmenu", function (e) {
			e.preventDefault();
		});
	});




	//Sidebar Custom Width
	lQuery(".sidebar-toggler-resize").livequery(function () {
		var slider = $(this);
		var column = $(this).closest(".col-main");

		var clickspot;
		var startwidth;
		var width;

		slider.on("mouseover", function () {
			$(this).css("opacity", "0.6");
		});
		slider.on("mouseout", function () {
			if (!clickspot) {
				$(this).css("opacity", "0");
			}
		});
		slider.on("mousedown", function (event) {
			if (!clickspot) {
				clickspot = event;
				startwidth = column.width();
				return false;
			}
		});

		//$(".sidebar-toggler-resize").show();

		$(window).on("mouseup", function (event) {
			if (clickspot) {
				clickspot = false;
				$(this).css("opacity", "0");
				if (width != "undefined") {
					saveProfileProperty("sidebarwidth", width, function () {
						$(window).trigger("resize");
					});
				}
				return false;
			}
		});
		$(window).on("mousemove", function (event) {
			if (clickspot) {
				$(this).css("opacity", "0.6");
				width = 0;
				var changeleft = event.pageX - clickspot.pageX;
				width = startwidth + changeleft;
				width = width + 32;
				if (width < 200) {
					width = 200;
				}
				if (width > 380) {
					//break sidebarfilter columns
					column.addClass("sidebarwide");
				} else {
					column.removeClass("sidebarwide");
				}
				if (width > 500) {
					width = 500;
				}
				column.width(width);
				column.data("sidebarwidth", width);
				$(".pushcontent").css("margin-left", width + "px");
				event.preventDefault();
				$(window).trigger("resize");
				return false;
			}
		});
	});

	lQuery(".col-resize").livequery(function () {
		var slider = $(this);
		var column = $(this).closest(".col-main");

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

		//$(".sidebar-toggler-resize").show();

		$(window).on("mouseup", function (event) {
			if (clickspot) {
				clickspot = false;
				if (width != "undefined") {
					saveProfileProperty("sidebarwidth", width, function () {
						$(window).trigger("resize");
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
				if (width < 200) {
					width = 200;
				}
				if (width > 480) {
					width = 480;
				}
				//console.log("W " , width);
				column.width(width);
				column.data("sidebarwidth", width);
				$(".pushcontent").css("margin-left", width + "px");
				event.preventDefault();
				$(window).trigger("resize");
				return false;
			}
		});
	});

	lQuery(".sidebarselected").livequery("click", function () {
		$("#sidebar-entities li").removeClass("current");
		$("#sidebar-list-upload").addClass("current");
	});

	//Moved From settings.js
	lQuery("#datamanager-workarea th.sortable").livequery("click", function (e) {
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
			function (response, status, xhr) {
				$(window).trigger("resize");
			}
		);
		e.stopPropagation();
	});

	lQuery(".tabnav a").livequery("click", function () {
		$(".tabnav a").removeClass("current");
		$(this).addClass("current");
	});

	lQuery("select.eventsjump").livequery("change", function () {
		var val = $(this).val();
		var url = $(this).data("eventurl");
		var targetdiv = $(this).data("targetdiv");

		$("#" + targetdiv).load(url + "?oemaxlevel=1&type=" + val, function () {
			$(window).trigger("resize");
		});
	});
	lQuery(".permissionsroles").livequery("change", function () {
		var val = $(this).val();
		var targetdiv = $(this).data("targetdiv");
		var url = $(this).data("urlprefix");
		var permissiontype = $(this).data("permissiontype");
		if (val == "new") {
			$("#" + targetdiv).load(
				url + "addnew.html?oemaxlevel=1&groupname=New",
				function () {
					$(window).trigger("resize");
				}
			);
			$("#module-picker").hide();
		} else {
			$("#" + targetdiv).load(
				url +
					"index.html?oemaxlevel=1&permissiontype=" +
					permissiontype +
					"&settingsgroupid=" +
					val,
				function () {
					$(window).trigger("resize");
				}
			);
			$("#module-picker").show();
		}
	});

	lQuery(".permission-radio").livequery("click", function () {
		var val = jQuery(this).val();

		if (val == "partial") {
			jQuery(this).parent().find(".sub-list").show();
		} else {
			jQuery(this).parent().find(".sub-list").hide();
		}
	});

	lQuery("#module-picker select").livequery("change", function () {
		var rolesval = $(".permissionsroles").val();
		var val = $(this).val();
		if (val == "all") {
			jQuery(".togglesection").show();
		} else {
			jQuery(".togglesection").hide();
			jQuery("." + val).show();
		}
	});

	function replaceAll(str, find, replace) {
		find = escapeRegExp(find);
		return str.replace(new RegExp(find, "g"), replace);
	}

	function escapeRegExp(str) {
		return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
	}

	lQuery("#setup-view").livequery("click", function () {
		if ($("#setup-view").hasClass("open")) {
			$("#views-header").height(44);
			$("#views-settings").hide();
			$("#setup-view").removeClass("open");
		} else {
			$("#views-header").height("auto");
			$("#views-settings").show();
			$("#setup-view").addClass("open");
		}
	});

	$("#renderastable").click(function () {
		if ($("#renderastable").is(":checked")) {
			$("#rendertableoptions").show();
		} else {
			$("#rendertableoptions").hide();
		}
	});

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
				stop: function (event, ui) {},
			});
		}
	});

	lQuery(".listsort").livequery(function () {
		var listtosort = $(this);
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
						success: function (data) {
							$(window).trigger("checkautoreload", [listtosort]);
						},
					});
				},
			});
		}
	});

	lQuery(".tablesort").livequery(function () {
		var listtosort = $(this);
		if (typeof listtosort.sortable == "function") {
			listtosort.sortable({
				axis: "y",
				cancel: ".no-sort",
				stop: function (event, ui) {
					var path = $(this).data("path");

					var data = "";

					// var ids = new Array();
					$(this)
						.find("tr")
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
	});

	lQuery(".favclick").livequery("click", function (e) {
		e.preventDefault();
		var item = $(this);
		var itemid = item.data("id");
		var moduleid = item.data("moduleid");
		var favurl = item.data("favurl");
		var targetdiv = item.data("targetdiv");
		var options = item.data();
		if (itemid) {
			if (item.hasClass("itemfavorited")) {
				jQuery.ajax({
					url:
						apphome +
						"/components/userprofile/favoritesremove.html?profilepreference=" +
						"favorites_" +
						moduleid +
						"&profilepreference.value=" +
						itemid,
					success: function () {
						//item.removeClass("ibmfavorited");
						jQuery.get(favurl, options, function (data) {
							$("." + targetdiv)
								.replaceWith(data)
								.hide()
								.fadeIn("slow");
						});
					},
				});
			} else {
				jQuery.ajax({
					url:
						apphome +
						"/components/userprofile/favoritesadd.html?profilepreference=" +
						"favorites_" +
						moduleid +
						"&profilepreference.value=" +
						itemid,
					success: function () {
						//item.addClass("ibmfavorited");
						jQuery.get(favurl, options, function (data) {
							$("." + targetdiv)
								.replaceWith(data)
								.hide()
								.fadeIn();
						});
					},
				});
			}
		}
	});

	lQuery(".seemorelink").livequery("click", function (e) {
		e.preventDefault();
		var textbox = $(this).data("seemore");
		if (textbox) {
			$("#" + textbox).removeClass("seemoreclosed");
			$(this).hide();
		}
	});

	lQuery("#assetcollectionresultsdialog .rowclick").livequery(
		"click",
		function (e) {
			closeemdialog($(this).closest(".modal"));
			var rowid = $(this).attr("rowid");
			$("#submitcollectionid").val(rowid);
			$("#colelectform").trigger("submit");
		}
	);

	lQuery(".copyembed").livequery("click", function (e) {
		e.preventDefault();
		var embedbtn = $(this);
		var loaddiv = embedbtn.data("targetdivinner");
		var nextpage = embedbtn.attr("href");
		jQuery.get(nextpage, function (data) {
			$("#" + loaddiv).html(data);
			var copyText = $("#" + loaddiv).children("textarea");
			if (typeof copyText != "undefined") {
				copyText.select();
				document.execCommand("copy");
			}
			$(window).trigger("resize"); //need this?
		});
	});

	lQuery(".toggle-upload-details").livequery("click", function (e) {
		toggleuploaddetails($(this));
	});

	toggleuploaddetails = function (detail, status = "") {
		if (status == "") {
			status = detail.data("status");
		}
		if (status == "open") {
			detail.next(".toggle-content").hide();
			detail
				.children(".fas")
				.removeClass("fa-caret-down")
				.addClass("fa-caret-right");
			detail.data("status", "closed");
		} else {
			detail.next(".toggle-content").show();
			detail
				.children(".fas")
				.removeClass("fa-caret-right")
				.addClass("fa-caret-down");
			detail.data("status", "open");
		}
	};

	lQuery(".togglesharelink").livequery("change", function (e) {
		var url = $("input.sharelink").val();
		var value = $(this).data("value");
		if (url.includes(value)) {
			url = url.replace("?" + value, "");
		} else {
			url = url + "?" + value;
		}
		$("span.sharelink").html(url);
		$("input.sharelink").val(url);
	});

	lQuery(".reloadcontainer").livequery("click", function (event) {
		event.preventDefault();
		var link = $(this);
		if (link.data("reloadcontainer")) {
			var container_ = link.data("reloadcontainer");
			var container = $("#" + container_);
			if (container.length) {
				$(window).trigger("autoreload", [container]);
			}
		}
		return;
	});

	lQuery(".emcarousel-link").livequery("click", function (e) {
		e.preventDefault();
		var image = $("#emcarousel-image");
		var link = $(this);
		image.attr("src", link.attr("href"));
		image.attr("alt", link.attr("title"));
		image.data("assetid", link.data("assetid"));
	});

	lQuery(".actions-enable-checkbox").livequery("change", function (e) {
		var parent = $(this).closest(".actions-row");
		var actions = parent.find(".actions-elements");
		var status = true;
		if (actions.hasClass("actions-disabled")) {
			status = false;
		}

		actions.find(".action-control").each(function () {
			var control = $(this);
			control.prop("disabled", status);
		});

		actions.toggleClass("actions-disabled");
	});

	lQuery(".pickemoticon .emoticonmenu span").livequery("click", function () {
		var menuitem = $(this);
		//var aparent = $(menuitem.parents(".pickemoticon"));
		var aparent = $(menuitem.closest(".message-menu").find(".pickemoticon"));
		var saveurl = aparent.data("toggleurl");
		var messageid = aparent.data("messageid");
		var options = aparent.data();
		options.reactioncharacter = menuitem.data("hex");
		$.ajax({
			url: saveurl,
			async: true,
			data: options,
			success: function (data) {
				$("#chatter-message-" + messageid).replaceWith(data);
			},
		});
	});

	function posiitionSubmitButtons() {
		var submitBtns = $(".form-submit-btns");
		if (!submitBtns.length) return;
		if (submitBtns.hasClass("sticky")) {
			return;
		}
		var offsetTop = submitBtns.offset().top;
		if (offsetTop > $(window).height()) {
			submitBtns.addClass("sticky");
		}
	}
	//posiitionSubmitButtons(); Dont run this here becuase it slows down loading

	lQuery(".form-submit-btns").livequery(function () {
		posiitionSubmitButtons();
	});
}

jQuery(document).ready(function () {
	initializeUI();

	jQuery(window).trigger("resize");
});

function switchsubmodulebox(item) {
	var parent = item.closest(".editentitymetadata-submodule");
	var results = item.data("targetdiv");
	parent.find("#" + results).hide("fast");
	parent.addClass("editingsubmodule");
	item.hide();
}

//Old not used
replaceelement = function (url, div, options, callback) {
	jQuery.ajax({
		url: url,
		async: false,
		data: options,
		success: function (data) {
			//Look for img and add ?cache=false
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

lQuery(".changeimportmodule").livequery("change", function () {
	var select = $(this);
	var moduleid = select.val();

	app = $("#application");
	siteroot = app.data("siteroot");
	apphome = app.data("apphome");
	var property = "desktop_lastselected_module";

	var targetdiv = select.data("targetdiv");

	jQuery.ajax({
		url:
			apphome +
			"/views/modules/" +
			moduleid +
			"/components/sidebars/localdrives/index.html?profilepreference=" +
			property +
			"&profilepreference.value=" +
			moduleid,
		success: function (data) {
			var cell = $("#" + targetDiv);
			cell.replaceWith(data);
		},
		xhrFields: {
			withCredentials: true,
		},
		crossDomain: true,
	});
});
