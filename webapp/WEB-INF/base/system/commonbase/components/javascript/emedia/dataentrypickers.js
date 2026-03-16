$(document).ready(function () {
	lQuery(".assetpicker .assetInput").livequery("change", function () {
		var input = $(this);
		var detailId = input.data("detailid");
		var assetName = input.val();
		var assets = input.prop("files");
		if (assets.length == 0) return;
		var asset = assets[0];
		if (asset.name) assetName = asset.name;
		var fileReader = new FileReader();
		fileReader.onload = function (e) {
			if (!assetName && e.target.fileName) {
				assetName = e.target.fileName;
			}
			var preview = input
				.closest(".assetpicker")
				.find(".render-type-thumbnail");
			preview.html("");
			if (/\.(jpe?g|png|gif|webp)$/i.test(assetName)) {
				var img = $("<img>");
				img.attr("src", e.target.result);
				img.attr("height", "140px");
				img.attr("width", "auto");
				preview.append(img);
			} else if (/\.(mp4|mov|mpeg|avi)$/i.test(assetName)) {
				var img = $("<i>");
				img.attr("class", "bi bi-film");
				preview.append(img);
			}

			preview.append(
				`<div class="p-1"><span class="mr-2">${assetName}</span><a href="#" class="removefieldassetvalue" title="Remove Selected Asset" data-detailid="${detailId}"><i class="bi bi-x"></i> Remove</a></div>`,
			);
		};
		fileReader.readAsDataURL(asset);
	});

	lQuery(".assetpicker .removefieldassetvalue").livequery(
		"click",
		function (e) {
			e.preventDefault();
			var picker = $(this).closest(".assetpicker");
			var detailid = $(this).data("detailid");

			picker.find("#" + detailid + "-preview").html("");
			picker.find("#" + detailid + "-value").val("");
			picker.find("#" + detailid + "-file").val("");

			var theform = $(picker).closest("form");
			theform = $(theform);
			if (theform.hasClass("autosubmit")) {
				theform.trigger("submit");
			}
		},
	);

	//Not used? Or used in admin area?
	lQuery(".emrowpicker table td").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}

		var clicked = $(this);

		var row = clicked.closest("tr");
		var table = clicked.closest("table");
		var form = clicked.closest(".pickedcategoryform");

		var existing = row.hasClass("emrowselected");
		if (!form.hasClass("emmultivalue")) {
			$("tr", table).removeClass("emrowselected");
		}
		row.toggleClass("emrowselected");
		var id = row.data("id");

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

		form.trigger("submit");

		if (form.hasClass("autoclose")) {
			closeemdialog(form.closest(".modal"));
		}
	});

	//CB This works. Opens entities
	lQuery(".topmodules .resultsdivdata").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}
		var row = $(this);
		row.css("pointer-events", "none");

		var clickableresultlist = row.closest(".clickableresultlist");

		var moduleid = clickableresultlist.data("moduleid");

		var rowid = row.data("dataid");
		clickableresultlist.data("id", rowid);
		clickableresultlist.data("entityid", rowid);
		clickableresultlist.data("updateurl", true);
		var urlbar = clickableresultlist.data("baseurlbar");
		clickableresultlist.data("urlbar", urlbar + "?entityid=" + rowid);
		clickableresultlist.data("dialogid", "entitydialog");
		clickableresultlist.emDialog(null, function () {
			row.css("pointer-events", "auto");
		});
	});

	lQuery(".listofentities .resultsentitydata").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}

		var row = $(this);

		var clickableresultlist = row.closest(".clickopenentity");
		if (
			clickableresultlist.length < 1 ||
			row.hasClass("resultsassetcontainer")
		) {
			return true; //handled by asset picker
		}

		row.css("pointer-events", "none");
		var rowid = row.data("dataid");
		clickableresultlist.data("id", rowid);
		clickableresultlist.data("entityid", rowid);
		var entitymoduleid = row.data("entitymoduleid");

		clickableresultlist.data(
			"url",
			`${apphome}/views/modules/${entitymoduleid}/editors/default/tabs/index.html`,
		);

		clickableresultlist.data("updateurl", true);
		var urlbar = `${apphome}/views/modules/${entitymoduleid}/index.html?entityid=${rowid}`;
		clickableresultlist.data("urlbar", urlbar);
		clickableresultlist.data("dialogid", "entitydialog");
		clickableresultlist.emDialog(null, function () {
			row.css("pointer-events", "auto");
		});
	});

	//To open an entity in a submodule.
	lQuery(".editdiv.pickersubmodules .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var row = $(this);

			var clickableresultlist = row.closest(".clickableresultlist");
			clickableresultlist.data("id", row.data("dataid")); //They picked an entity

			clickableresultlist.runAjax();
		},
	);
	//To open an entity in a submodule. CB Lose Back button
	lQuery(".entitysubmodules .resultsdivdata").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}

		var row = $(this);
		var rowid = row.data("dataid");
		var submoduleOpener = row.closest(".clickableresultlist");
		submoduleOpener.data("entityid", row.data("dataid"));
		submoduleOpener.data("updateurl", true);
		var urlbar = submoduleOpener.data("baseurlbar");
		submoduleOpener.data("urlbar", urlbar + "?entityid=" + rowid);

		var editdiv = row.closest(".editdiv");
		submoduleOpener.data("parententityid", editdiv.data("entityid"));
		submoduleOpener.data(
			"parententitymoduleid",
			editdiv.data("entitymoduleid"),
		);

		submoduleOpener.runAjax();
	});

	//CB working. Uses edithome for searchcategory clicking
	lQuery(".listsearchcategories .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var row = $(this);

			var searchId = row.data("dataid");
			var submoduleOpener = row.closest(".clickableresultlistinline");
			submoduleOpener.data("updateurl", true);
			var url = `${submoduleOpener.data("url")}?searchcategoryid=${searchId}`;
			submoduleOpener.data("urlbar", url);
			submoduleOpener.data("searchcategoryid", searchId);
			submoduleOpener.runAjax();
		},
	);

	//CB working for entity fieldpicking
	lQuery(".pickerresults.pickerforfield .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var clickableresultlist = clicked.closest(".clickableresultlist");

			if (clickableresultlist.length) {
				clickableresultlist.data("dataid", rowid);
				clickableresultlist.runAjax();
				closeemdialog(clickableresultlist.closest(".modal"));
			}
		},
	);

	// CM-CB
	// 2024-12-03
	// working for asset pick an entity (Media Viewer)
	lQuery(".pickerresults.assetpickentity .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var clickableresultlist = clicked.closest(".clickableresultlist");

			if (clickableresultlist.length) {
				clickableresultlist.data("dataid", rowid);
				clickableresultlist.runAjax(function () {
					closeemdialog(clicked.closest(".modal"));
				});
			}
		},
	);

	// CB
	// 2024-12-04
	// Upload To dialog
	lQuery(".editdiv.pickerforuploading .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);
			clicked.css("pointer-events", "none");

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var clickableresultlist = clicked.closest(".clickableresultlist");

			if (clickableresultlist.length) {
				clickableresultlist.data("entityid", rowid);
				clickableresultlist.emDialog(null, function () {
					closeemdialog(clicked.closest(".modal"));
					clicked.css("pointer-events", "auto");
				});
			}
		},
	);

	// CM: 2024-12-17
	// Picker Folder (Category Tree) for Asset (Media Viewer)
	lQuery(".pickerresults.pickercategorytree .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);
			var rowid = clicked.data("dataid");
			var clickableresultlist = clicked.closest(".clickableresultlist");

			if (clickableresultlist.length) {
				clickableresultlist.data("categoryid", rowid);
				clickableresultlist.runAjax();
				closeemdialog(clickableresultlist.closest(".modal"));
			}
		},
	);

	//CB: assign a asset to a field
	lQuery(".pickerresults.pickerpickasset .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var editdiv = clicked.closest(".editdiv");

			if (editdiv.length) {
				var pickertarget = editdiv.data("pickertargetfield");
				pickertarget = $("#" + pickertarget); //This is the field itself
				if (pickertarget.length > 0) {
					var detailid = pickertarget.data("detailid");
					var detailinput = pickertarget.find("#" + detailid + "-value");
					detailinput.attr("value", rowid);

					let preview = pickertarget.find("#" + detailid + "-preview");
					preview.load(
						apphome +
							"/components/xml/types/assetpicker/preview.html?oemaxlevel=1&assetid=" +
							rowid,
						function () {},
					);
				}
				closeemdialog(clicked.closest(".modal"));
			}
		},
	);

	//http://einnovation.local.org:8080/site/blockfind/views/modules/asset/editors/oipickasset/index.html
	lQuery(".pickerresults.oipickasset .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var assetid = row.data("dataid");
			var sourcepath = row.data("sourcepath");

			var mediadb = $("#application").data("mediadbappid");

			var url =
				"/" +
				mediadb +
				"/services/module/asset/downloads/vieworiginal/" +
				sourcepath;

			$(window).trigger("assetpicked", [url]);
		},
	);

	//CB: Is good. for submodules
	lQuery(".pickerresults.entitypickersubmodule .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = clicked.closest(".resultsdivdata");
			var rowid = row.data("dataid");
			var editdiv = clicked.closest(".editdiv");

			if (editdiv.length) {
				var options = editdiv.cleandata();
				var clickurl = editdiv.data("clickurl");
				options.oemaxlevel = 1;
				options.id = rowid;
				var pickertarget = editdiv.data("targetdiv");
				pickertarget = $("#" + pickertarget);
				if (clickurl !== undefined && clickurl != "") {
					jQuery.ajax({
						url: clickurl,
						data: options,
						success: function (data) {
							var entity = $(".entitydialog");
							//autoreload(entity);
							$(window).trigger("autoreload", [entity]);
							//pickertarget.replaceWith(data);
						},
					});
				}
				closeemdialog(clicked.closest(".modal"));
			}
		},
	);

	//CB: Good assign a searchcategory to some selected entities
	lQuery(".pickerresults.picksearchcategory .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var pickerresults = clicked.closest(".clickableresultlist");
			pickerresults.data("id", rowid);

			pickerresults.runAjax(function () {
				//Chain
				closeemdialog(clicked.closest(".modal"));
				//Reload parent
			});
		},
	);

	lQuery(".editdiv.pickercopycategoryto .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var pickerresults = clicked.closest(".pickerresults");
			var clickurl = pickerresults.data("clickurl");
			var options = pickerresults.cleandata();
			options.oemaxlevel = 1;
			options.id = rowid;
			$(window).trigger("showToast", [pickerresults]);
			var toastUid = pickerresults.data("uid");
			jQuery.ajax({
				url: clickurl,
				data: options,
				success: function (data) {
					//show toast or reload page or both
					$(window).trigger("successToast", [toastUid]);
					closeemdialog(clicked.closest(".modal"));
				},
			});
		},
	);

	//CB: Good assign assets to a selected entity
	lQuery(".editdiv.entitypickerassets .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var pickerresults = clicked.closest(".pickerresults");
			var clickurl = pickerresults.data("clickurl");
			var options = pickerresults.cleandata();
			options.oemaxlevel = 1;
			options.id = rowid;
			$(window).trigger("showToast", [pickerresults]);
			var toastUid = pickerresults.data("uid");
			jQuery.ajax({
				url: clickurl,
				data: options,
				success: function () {
					$(window).trigger("successToast", toastUid);
					closeemdialog(clicked.closest(".modal"));
				},
			});
		},
	);

	/**
	 * CM
	 * Assign Person to a Faceprofile (From MediaViewer)
	 */

	lQuery(".pickerresults.facepickerperson .resultsdivdata").livequery(
		"click",
		function (e) {
			if (!isValidTarget(e)) {
				return true;
			}

			var clicked = $(this);

			var row = $(clicked.closest(".resultsdivdata"));
			var rowid = row.data("dataid");
			var clickableresultlist = clicked.closest(".clickableresultlist");

			if (clickableresultlist.length) {
				clickableresultlist.data("dataid", rowid);
				clickableresultlist.runAjax();
				closeemdialog(clickableresultlist.closest(".modal"));
				closeemdialog($("#faceprofiledialog"));
			}
		},
	);

	$(window).on("assetpicked", function (_, params) {
		if (typeof params === "string") {
			var params = JSON.parse(params);
		}
		var assetid = params.assetid;
		var messageid = $("#blockfindpicker").data("messageid");

		if (messageid !== undefined) {
			var div = $("#attachasset" + messageid);
			//Submit this assetid and reload the chat box we are in
			var form = div.closest(".chatattachasset");
			form.find(".pickedassetid").val(assetid);
			form.trigger("submit");
			//close the form
		}
	});

	lQuery(".orderpending .resultsdivdata").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}
		var row = $(this);
		row.css("pointer-events", "none");

		var clickableresultlist = row.closest(".clickableresultlist");

		var rowid = row.data("dataid");
		clickableresultlist.data("id", rowid);
		clickableresultlist.data("entityid", rowid);
		clickableresultlist.emDialog(null, function () {
			row.css("pointer-events", "auto");
		});
	});

	lQuery(".orderuserlist .resultsdivdata").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}
		var row = $(this);
		row.css("pointer-events", "none");

		var clickableresultlist = row.closest(".clickableresultlist");

		var rowid = row.data("dataid");
		clickableresultlist.data("id", rowid);
		clickableresultlist.data("entityid", rowid);
		clickableresultlist.emDialog(null, function () {
			row.css("pointer-events", "auto");
		});
	});

	//Assets or Categories and you import into a entity
	lQuery(".pickerresultscopy .resultsdivdata").livequery("click", function (e) {
		if (!isValidTarget(e)) {
			return true;
		}

		var clicked = $(this);

		var row = $(clicked.closest(".resultsdivdata"));
		var rowid = row.data("dataid");
		var pickerresults = clicked.closest(".pickerresults");

		var options = pickerresults.data();
		options.id = rowid;
		var clickurl = pickerresults.data("clickurl");
		var targetdiv = pickerresults.data("clicktargetdiv");
		var targettype = pickerresults.data("targettype");

		if (clickurl !== undefined && clickurl != "") {
			jQuery.ajax({
				url: clickurl,
				data: options,
				success: function (data) {
					if (!targetdiv.jquery) {
						targetdiv = $("#" + targetdiv);
					}
					if (targettype == "message") {
						if (targetdiv !== undefined) {
							targetdiv.prepend(data);
							targetdiv.find(".fader").fadeOut(3000, "linear");
						}
					} else {
						//regular targetdiv
						if (targetdiv !== undefined) {
							targetdiv.replaceWith(data);
						}
					}
					closeemdialog(clicked.closest(".modal"));
				},
			});
		}
	});
});

//Not used?
$(window).on(
	"updatepickertarget",
	function (e, pickertargetid, dataid, dataname) {
		var pickertarget = $("#" + pickertargetid);
		if (pickertarget.length > 0) {
			updateentityfield(pickertarget, dataid, dataname);
		}
	},
);

//not used?
updateentityfield = function (pickertarget, id, name) {
	var template = $("#pickedtemplateREPLACEID", pickertarget).html(); //clone().appendTo(pickertarget);
	var newcode = template.replaceAll("REPLACEID", id);
	newcode = newcode.replaceAll("REPLACEFIELDNAME", "");
	var ismulti = pickertarget.data("ismulti");
	if (ismulti == undefined || !ismulti) {
		//clear others
		pickertarget.find("li:not(#pickedtemplateREPLACEID)").remove();
	}
	pickertarget.prepend("<li>" + newcode + "</li>");
	var newrow = pickertarget.find("li:first");
	newrow.attr("id", id);
	newrow.find("a:first").text(name);
	newrow.show();
};

showmodal = function (emselecttable, url) {
	trackKeydown = true;
	var id = "modals";
	var modaldialog = $("#" + id);
	var width = emselecttable.data("dialogwidth");
	if (modaldialog.length == 0) {
		$("#application").append(
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
