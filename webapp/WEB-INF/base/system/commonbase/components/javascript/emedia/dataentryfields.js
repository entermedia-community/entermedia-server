var openDetail = "";
var app, home, apphome, themeprefix;

$(document).ready(function () {
	app = $("#application");
	var mediadb = app.data("mediadbappid");
	home = app.data("home");
	apphome = app.data("apphome");
	themeprefix = app.data("themeprefix");
	if (home !== undefined) {
		apphome = home + apphome;
		themeprefix = home + themeprefix;
	}

	$(document).on("change", ".lenguagepicker", function () {
		//Makes sure the name matches the new value
		var select = $(this);
		var div = $(this).closest(".languagesaddform");
		var langinput = $(".langvalue", div);
		var detailid = select.data("detailid");
		var lang = select.val();
		langinput.attr("name", detailid + "." + lang + ".value");

		var trnslationsource = $(".trnslationsource", div);
		trnslationsource
			.find("input")
			.attr("id", detailid + "." + lang)
			.val(lang);
		trnslationsource.find("label").attr("for", detailid + "." + lang);
	});

	lQuery(".addlocale-ajax").livequery("click", function (event) {
		event.stopPropagation();
		event.preventDefault();

		var btn = $(this);
		addLanguageInput(btn);
	});

	function addLanguageInput(btn, val = "") {
		var detailid = btn.data("detailid");

		var languagecode = btn.data("languagecode");
		btn.hide();

		if (!languagecode) return;

		if ($(`#value-${detailid}-${languagecode}`).length > 0) return;

		btn.runAjax(function () {
			if (val) {
				$(`#value-${detailid}-${languagecode}`).val(val);
			}
			var languagesfield = btn.closest(".languagesfield");
			$(".hide-remove-btn", languagesfield).remove();
		});
	}

	function ellipses(text, maxLength = 20) {
		if (!text) return "";
		text = text.trim();
		if (text.length <= maxLength) {
			return text;
		} else {
			return text.substring(0, maxLength).trim() + "...";
		}
	}

	// $(document).on("focus", ".langvalue", autoSelectTranslationSource);
	// $(document).on("input", ".langvalue", autoSelectTranslationSource);

	// function autoSelectTranslationSource(forced = false) {
	// 	if (forced || $(this).val().trim().length > 0) {
	// 		var detailid = $(this).data("detailid");
	// 		var code = $(this).data("languagecode");
	// 		var locale = $(this).data("locale");
	// 		$(`#translate-dropdown-${detailid}`).val(code);
	// 	}
	// }

	lQuery(".remove-language").livequery("click", function (e) {
		e.preventDefault();
		var parent = $(this).parent();
		parent.find(".langvalue").val("");
	});

	function parseLangCodes(targets) {
		var codes = Array.from(targets);
		var langCodes = [];
		if (codes && codes.length > 0) {
			codes.forEach(function (code) {
				if (code === "zh-Hans") {
					code = "zh";
				} else if (code === "zh_TW" || code === "zh-Hant") {
					code = "zht";
				}
				if (code === "all" || code === "missing") return;
				langCodes.push(code);
			});
		}
		return langCodes;
	}

	function parseTranslations(translations) {
		var langTranslations = {};
		if (translations && Object.keys(translations).length > 0) {
			Object.keys(translations).forEach(function (code) {
				if (code === "zh") {
					langTranslations["zh-Hans"] = translations[code];
					langTranslations["zh_CN"] = translations[code];
				} else if (code === "zht") {
					langTranslations["zh-Hant"] = translations[code];
					langTranslations["zh_TW"] = translations[code];
				}
				langTranslations[code] = translations[code];
			});
		} else {
			return null;
		}
		return langTranslations;
	}

	lQuery(".translate-ajax").livequery("click", function (e) {
		e.preventDefault();
		var div = $(this).closest(".emdatafieldvalue");
		var sourceSelect = div.find(".sourcelocale");
		var targetSelect = div.find(".targetlocale");
		var source = sourceSelect.val();
		var target = targetSelect.val();

		if (!source) {
			customToast("Please select a source language!", {
				positive: false,
			});
			return;
		}

		if (!target) {
			customToast("Please select a target language!", {
				positive: false,
			});
			return;
		}

		if (target === source) {
			customToast("Source and target languages cannot be the same!", {
				positive: false,
			});
			return;
		}

		var text = "",
			targets = new Set();

		if (target === "all" || target === "missing") {
			targetSelect.find("option").each(function () {
				var val = $(this).attr("value");
				if (val) {
					targets.add(val);
				}
			});
			targets.delete(target);
			targets.delete(source);
		} else {
			targets.add(target);
		}

		var selectedLangs = div.find("textarea.langvalue");
		selectedLangs.each(function () {
			var code = $(this).data("languagecode");
			var val = $(this).val().trim();
			if (code == source) {
				text = val;
			}
			if (target === "missing" && val.length > 1) {
				targets.delete(code);
			}
		});

		if (text == "") {
			customToast("Source language is empty!", {
				positive: false,
			});
			return;
		}

		if (targets.size == 0) {
			customToast("No translation targets found!", { positive: false });
			return;
		}

		var mask = div.find(".translation-mask");
		mask.addClass("active");

		var parsedTargets = parseLangCodes(targets);
		$.ajax({
			url: `/${mediadb}/services/module/translation/translate.json`,
			method: "POST",
			data: JSON.stringify({
				source: source,
				targets: parsedTargets.join(","),
				text: text,
			}),
			contentType: "application/json",
			success: function (data) {
				if (data && data.response.status == "ok") {
					var translations = parseTranslations(data.data);
					if (translations) {
						selectedLangs.each(function () {
							var code = $(this).data("languagecode");
							if (translations[code]) {
								$(this).val(translations[code]);
							}
						});

						div.find(".addlocale-ajax").each(function () {
							var code = $(this).data("languagecode");
							if (code && translations[code]) {
								addLanguageInput($(this), translations[code]);
							}
						});
					}
				}
				mask.removeClass("active");
			},
			error: function (error) {
				customToast("Translation request failed!", {
					positive: false,
					log: error,
				});
				mask.removeClass("active");
			},
		});
	});

	lQuery("textarea.keeptab").livequery("keydown", function (e) {
		var $this, end, start;
		if (e.keyCode === 9) {
			start = this.selectionStart;
			end = this.selectionEnd;
			$this = $(this);
			$this.val(
				$this.val().substring(0, start) + "\t" + $this.val().substring(end)
			);
			this.selectionStart = this.selectionEnd = start + 1;
			return false;
		}
	});

	lQuery(".languagesavebtn").livequery("click", function (event) {
		event.stopPropagation();
		event.preventDefault();

		var btn = $(this);
		var url = btn.attr("href");
		var detailid = btn.data("detailid");

		var div = btn.closest(".emdatafieldvalue");

		var count = $("#languagesextra_" + detailid, div).data("count");
		count = count + 1;
		var languages = [];
		var args = {
			oemaxlevel: 1,
			detailid: detailid,
			count: count,
			usedlanguages: [],
		};

		$(".lenguagepicker", div).each(function () {
			var value = $(this).val();
			args.usedlanguages.push(value);
		});
		$.get(url, args, function (data) {
			var selectlist = $(".lenguagepicker option", data);
			if ($(selectlist).length > 0) {
				$("#languagesextra_" + detailid, div).append(data);
				$("#colanguagesextra_" + detailid, div).data("count", count);
				$(document).trigger("domchanged");
			}
		});
	});
	/*
	lQuery("textarea.htmleditor").livequery(new function() 
	{
		//$("textarea.htmleditor").each(function()
		{
			//console.log("Hit" + $(this));
			var value = $(this).val()
			var textname = $(this).attr("name");
			var oFCKeditor = new FCKeditor( textname );
			oFCKeditor.Config.StylesXmlPath		= '/system/tools/html/styles.xml';
			oFCKeditor.BasePath	= "/system/tools/html/fckeditor/";
		  	oFCKeditor.ToolbarSet = "Basic";
			//oFCKeditor.Config.DefaultLanguage = "$context.getLanguage()";
			oFCKeditor.Height = 300;
			oFCKeditor.Width = 300;
	        oFCKeditor.ReplaceTextarea();
	    }
	    
	});
	*/

	if ($.validator) {
		lQuery(".force-validate-inputs").livequery(function () {
			var theform = $(this).closest("form");

			theform.on("click", function () {
				theform.valid();
			});

			$.validator.setDefaults({
				ignore: ".ignore",
			});

			theform.validate({
				ignore: ".ignore",
			});
		});

		$.validator.methods.number = function (value, element) {
			var globalizedValue = value.replace(/[$,]/g, "");
			///$(element).val(globalizedValue);
			return (
				this.optional(element) ||
				/^-?(?:\d+|\d{1,3}(?:[\s\.,]\d{3})+)(?:[\., ]\d+)?$/.test(
					globalizedValue
				)
			);
		};

		$.validator.addClassRules("validateNumber", {
			number: true,
		});

		jQuery.validator.addMethod(
			"entityrequired",
			function (value, element) {
				var entitylistdiv = $(element).closest(".entitylistcontainer");
				var entitylist = entitylistdiv.find(".entity-value-list");
				if (entitylist.children("li").length > 0) {
					return true;
				}
				return false;
			},
			"This field is required."
		);

		$.validator.addClassRules("entityRequired", {
			entityrequired: true,
			number: true,
			min: 1,
		});

		$.validator.setDefaults({
			errorPlacement: function (error, element) {
				var elementid = element.attr("id");
				var elementparent = (elementparent = element.closest("div"));
				if (!elementparent.length) {
					$("#" + $.escapeSelector(elementid)).closest("div");
				}
				if (elementparent.length != 0) {
					//elementparent = $("#" + $.escapeSelector(elementid));
					error.insertAfter(elementparent);
				}
				if (
					element.is(".listdropdown, .listtags") &&
					element.next(".select2-container").length
				) {
					element
						.next(".select2-container")
						.find(".select2-selection")
						.addClass("error");
				} else if (element.hasClass("entityRequired")) {
					element.prev(".entity-value-list").addClass("error");
				}
			},
		});
	}

	lQuery(".entity-value-list-remove").livequery("click", function () {
		var entitylistdiv = $(this).closest(".entitylistcontainer");
		var input = entitylistdiv.find(".entity-value-list-count");
		if (input.lenght > 0) {
			var count = input.val();
			if ($.isNumeric(count) && count > 0) {
				input.val(count - 1);
			} else {
				input.val("0");
			}
		}
		$(this).closest("li").remove();
		return false;
	});

	lQuery(".inlinesave").livequery("click", function () {
		var form = $(this).closest(".inlinedata");
		var queryString = form.formSerialize();
		var url = form.attr("action");
		if (url == null) {
			url = apphome + "/views/settings/lists/datamanager/edit/inlinesave.json";
		}
		var targetselect = $(this).data("targetselect");
		var select = $("#" + targetselect);

		$.getJSON(url, queryString, function (data) {
			var newOption = new Option(data.name, data.id, true, true);
			select.append(newOption).trigger("change");
		});
		$(this).closest(".modal").modal("hide");
	});

	lQuery(".removeentityfromfield").livequery("click", function () {
		let listcontainer = $(this).closest(".entitylistcontainer");
		let countinput = listcontainer.find(".entity-value-list-count");
		let count = countinput.val();
		if (!$.isNumeric(count)) {
			count = 0;
		} else {
			count = count - 1;
		}
		countinput.val(count);
		let entityrow = $(this).closest("li");
		entityrow.remove();
	});

	//End of init
});

showPicker = function (detailid) {
	openDetail = detailid;
	if (!window.name) window.name = "admin_parent";
	window.open(
		home +
			"/system/tools/newpicker/index.html?parentName=" +
			window.name +
			"&detailid=" +
			detailid,
		"pickerwindow",
		"alwaysRaised=yes,menubar=no,scrollbars=yes,width=1000,x=100,y=100,height=600,resizable=yes"
	);
	return false;
};

//TODO: Does this need to be defined on the page itself?
SetPath = function (inUrl) {
	var input = document.getElementById(openDetail + ".value");
	input.value = inUrl;
};

/*
validate = function(inCatalogId, inDataType, inView , inFieldName)
{
	var val = $("#list-" + inFieldName).val();
	var div = '#error_' + inFieldName;
	var params = {
			catalogid: inCatalogId,
			searchtype: inDataType,
			view: inView,
			field: inFieldName,
			value: val
		};
	//alert( params );
	$(div).load(apphome + '/components/xml/validatefield.html', params);
}
*/

var listchangelisteners = []; //list nodes

findOrAddNode = function (inParentId) {
	//alert("Looking now: " + inParentId + listchangelisteners.length );
	for (var i = 0; i < listchangelisteners.length; i++) {
		var node = listchangelisteners[i];
		if (node.parentid == inParentId) {
			return node;
		}
	}
	var node = new Object();
	node.parentid = inParentId;
	node.children = [];
	listchangelisteners.push(node);

	return node;
};

addListListener = function (inParentFieldName, inFieldName) {
	//an array of array
	var node = findOrAddNode(inParentFieldName);
	node.children.push(inFieldName); //append the child
};

//When a field is changed we want to validate it and update any listeners
//Find all the lists in this form. Update all of them that are marked as a listener
//parent = businesscategory, child = lob, field = product
updatelisteners = function (catalogid, searchtype, view, fieldname) {
	var val = $("#" + fieldname + "value").val();
	//validate(catalogid, searchtype, view , fieldname);

	var node = findOrAddNode(fieldname);

	if (node.children) {
		for (var i = 0; i < node.children.length; i++) {
			var childfieldname = node.children[i];
			var element = $("#list-" + childfieldname);
			var valueselection;
			if (element.options !== undefined) {
				valueselection = element.options[element.selectedIndex].value;
			}
			var div = "listdetail_" + childfieldname;
			//we are missing the data element of the children
			//required: catalogid, searchtype, fieldname, value
			//optional: query, foreignkeyid and foreignkeyvalue

			var rendertype = $("#" + div).data("rendertype");
			if (rendertype == "multiselect") {
				$("#" + div).load(apphome + "/components/xml/types/multiselect.html", {
					catalogid: catalogid,
					searchtype: searchtype,
					view: view,
					fieldname: childfieldname,
					foreignkeyid: fieldname,
					foreignkeyvalue: val,
					value: valueselection,
					oemaxlevel: 1,
				});
			} else {
				$("#" + div).load(apphome + "/components/xml/types/list.html", {
					catalogid: catalogid,
					searchtype: searchtype,
					view: view,
					fieldname: childfieldname,
					foreignkeyid: fieldname,
					foreignkeyvalue: val,
					value: valueselection,
					oemaxlevel: 1,
				});
			}
		}
	}
};

//searchtype parentname

loadlist = function (
	indiv,
	catalogid,
	searchtype,
	inlabel,
	childfieldname,
	foreignkeyid,
	foreignkeyvalue,
	value
) {
	//what is this?
	$(indiv).load(apphome + "/components/xml/types/simplelist.html", {
		catalogid: catalogid,
		searchtype: searchtype,
		fieldname: childfieldname,
		label: inlabel,
		foreignkeyid: foreignkeyid,
		foreignkeyvalue: foreignkeyvalue,
		value: value,
		oemaxlevel: 1,
	});
};
//Don't use any form inputs named 'name'!
postForm = function (inDiv, inFormId) {
	var form = document.getElementById(inFormId);
	if ($) {
		var targetdiv = inDiv.replace(/\//g, "\\/");
		$(form).ajaxSubmit({
			target: "#" + targetdiv,
		});
	} else {
		form = Element.extend(form);
		var oOptions = {
			method: "post",
			parameters: form.serialize(true),
			evalScripts: true,
			asynchronous: false,
			onFailure: function (oXHR, oJson) {
				alert("An error occurred: " + oXHR.status);
			},
		};

		$("#" + inDiv).load(form.action, oOptions);
	}
	return false;
};

postPath = function (inCss, inPath, inMaxLevel) {
	if (inMaxLevel == null) {
		inMaxLevel = 1;
	}
	$("#" + inCss).load(inPath, { oemaxlevel: inMaxLevel });
	return false;
};

toggleBox = function (inId, togglePath, inPath) {
	$("#" + inId).load(home + togglePath, {
		pluginpath: inPath,
		propertyid: inId,
	});
};
