//Finder
var uploadid = new Date().getTime();
var home = null;
var currentupload = 0;
var haderror = false;
var uploadid;

var allfiles = new Array();

// wait for the DOM to be loaded
$(document).ready(function () {
	// var isDesktop = $("#application").data("desktop");
	// var ipcRenderer;
	// if (isDesktop) {
	// 	const { ipcRenderer: ipc } = require("electron");
	// 	ipcRenderer = ipc;
	// }
	var siteroot = $("#application").data("siteroot");
	var apphome = siteroot + $("#application").data("apphome");
	var mediadb = $("#application").data("mediadbappid");

	lQuery("#createmediapanel").livequery(function (e) {
		//reset array
		allfiles = new Array();
	});

	lQuery("#dialogmediaentity").livequery(function (e) {
		//reset array
		allfiles = new Array();
	});

	lQuery(".upload_field").livequery(function () {
		var inputfield = $(this);
		inputfield.val("");
		inputfield.initUpload();
	});

	lQuery(".upload_folder").livequery(function () {
		var inputfield = $(this);
		inputfield.val("");
		inputfield.initUpload();
	});

	jQuery(".upload_field").val("");

	var uplist = $(".up-files-list");

	$.each(uplist, function () {
		$(this).empty();
	});

	lQuery(".removefile").livequery("click", function (e) {
		e.preventDefault();
		var row = $(this).closest(".uploadprogressrow");
		var fileindex = $(row).data("fileindex");
		var uploadformarea = $(this).closest(".uploadformarea");
		// var upload_field = uploadformarea.find(".upload_field");
		allfiles.splice(fileindex, 1);
		filesPicked(null, null, uploadformarea);
		console.log(allfiles);
		$(row).remove();
	});

	lQuery(".clear-upload-list").livequery("click", function (e) {
		var uploadformarea = $(this).closest(".uploadformarea");
		allfiles = new Array();
		filesPicked(null, null, uploadformarea);
	});

	lQuery(".filePicker").livequery("click", function (e) {
		e.preventDefault();
		var form = $(this).closest(".uploadformarea");
		$(form).find(".upload_field").trigger("click");
	});

	lQuery(".folderPicker").livequery("click", function (e) {
		e.preventDefault();
		var form = $(this).closest(".uploadformarea");
		$(form).find(".upload_folder").trigger("click");
	});

	//Global Upload
	lQuery(".globalfilePicker").livequery("click", function (e) {
		e.preventDefault();
		var uploadarea = $(".globaluploadarea");
		if (uploadarea) {
			var categoryid = $(this).data("categoryid");
			if (categoryid) {
				$(uploadarea).find("#nodeid").val(categoryid);
			}
			var createentity = $(this).data("createentity");
			if (createentity) {
				$(uploadarea).find("#createentity").val(createentity);
			}
			var moduleid = $(this).data("moduleid");
			if (moduleid) {
				$(uploadarea).find("#entitytype").val(moduleid);
			}
			var uploadsourcepath = $(this).data("uploadsourcepath");
			if (uploadsourcepath) {
				$(uploadarea).find("#uploadsourcepath").val(uploadsourcepath);
			}
			$(uploadarea).find(".upload_field").trigger("click");
		}
	});

	lQuery(".globalfolderPicker").livequery("click", function (e) {
		e.preventDefault();
		var uploadarea = $(".globaluploadarea");
		if (uploadarea) {
			var categoryid = $(this).data("categoryid");
			if (categoryid) {
				$(uploadarea).find("#nodeid").val(categoryid);
			}
			$(uploadarea).find(".upload_folder").trigger("click");
		}
	});

	lQuery(".startbutton").livequery("click", function (e) {
		e.preventDefault();
		if ("updateAllCK5" in window) updateAllCK5();
		var startbutton = $(this);
		var uploadformarea = startbutton.closest(".uploadformarea");
		if (startbutton.prop("disabled")) {
			return;
		}
		var valid = $("#uploaddata").validate().form();
		if (!valid) {
			startbuttonn;
		}

		startbutton.text(startbutton.data("textuploading"));
		startbutton.attr("disabled", "disabled");
		//startbutton.prop('disabled', true);
		$(uploadformarea)
			.find(".upload_field")
			.triggerHandler("html5_upload.start");
	});

	lQuery(".drop-over").livequery(function () {
		var div = $(this);

		div.on("dragover", function (e) {
			e.preventDefault();
			e.stopPropagation();
			div.addClass("uploaddragenter");
		});
		div.on("dragenter", function (e) {
			e.preventDefault();
			e.stopPropagation();
			div.addClass("uploaddragenter");
		});
		div.on("dragleave", function (e) {
			div.removeClass("uploaddragenter");
		});
		div.on("drop", function (e) {
			if (e.originalEvent.dataTransfer) {
				if (e.originalEvent.dataTransfer.files.length) {
					e.preventDefault();
					e.stopPropagation();
					var categoryid = $(this).data("categoryid");
					if (categoryid) {
						var uploadformarea = $(".globaluploadarea");
						if (uploadformarea) {
							$(uploadformarea).find("#nodeid").val(categoryid);
						}
					}
					var uploadformarea = $(div).closest(".uploadformarea");
					$(".upload_field", uploadformarea).triggerHandler(
						"html5_upload.filesPicked",
						[e.originalEvent.dataTransfer.files]
					);
				}
			}
			div.removeClass("uploaddragenter");
		});
	});
	lQuery(".resultsdiv").livequery(function () {
		var div = $(this);
		if (div.find(".webUploadButton").length == 0) {
			//console.log("No upload target!");
			return;
		}
		if (div.find(".drop-feedback").length == 0) {
			div.append(
				'<div class="drop-feedback"><div><i class="bi bi-upload"></i><p>Upload Files</p></div></div>'
			);
		}
		div.on("dragover", function (e) {
			e.preventDefault();
			e.stopPropagation();
			div.addClass("filehover");
		});
		div.on("dragenter", function (e) {
			e.preventDefault();
			e.stopPropagation();
		});
		div.on("dragleave", function (e) {
			div.removeClass("filehover");
		});
		div.on("drop", function (e) {
			if (e.originalEvent.dataTransfer) {
				var files = e.originalEvent.dataTransfer.files;
				if (files.length) {
					e.preventDefault();
					e.stopPropagation();
					var uploadTarget = $(".webUploadButton");
					// if (ipcRenderer) {
					// 	const { webUtils } = require("electron");
					// 	var filePaths = [];
					// 	for (const f of e.originalEvent.dataTransfer.files) {
					// 		filePaths.push(webUtils.getPathForFile(f));
					// 	}
					// 	var data = uploadTarget.data();
					// 	div.removeClass("filehover");
					// 	ipcRenderer.send("filesDropped", { data: data, files: filePaths });
					// 	customToast(
					// 		filePaths.length +
					// 			" file" +
					// 			(filePaths.length > 1 ? "s" : "") +
					// 			" added to upload process!",
					// 		{
					// 			autohideDelay: 5000,
					// 			btnText: "Show",
					// 			btnClass: "btn btn-sm px-2 mx-2 btn-outline-primary",
					// 		}
					// 	);
					// 	return;
					// }
					uploadTarget.data("toastmessage", "Processing files...");
					uploadTarget.data("toastsuccess", "Ready to upload!");
					uploadTarget.runAjax(function () {
						var dropDiv = $("#drop-area > .drop-over");
						var categoryid = dropDiv.data("categoryid");
						if (categoryid) {
							var uploadformarea = $(".globaluploadarea");
							if (uploadformarea) {
								$(uploadformarea).find("#nodeid").val(categoryid);
							}
						}
						var uploadformarea = $(dropDiv).closest(".uploadformarea");
						$(".upload_field", uploadformarea).triggerHandler(
							"html5_upload.filesPicked",
							[files]
						);
					});
				}
			}
			div.removeClass("filehover");
		});
	});
	lQuery(".dropentity").livequery(function () {
		var div = $(this);
		if (div.find(".drop-feedback").length == 0) {
			div.append(
				'<div class="drop-feedback"><div><i class="bi bi-upload"></i><p>Create Folders from Files</p></div></div>'
			);
		}
		div.on("dragover", function (e) {
			e.preventDefault();
			e.stopPropagation();
			div.addClass("filehover");
		});
		div.on("dragenter", function (e) {
			e.preventDefault();
			e.stopPropagation();
		});
		div.on("dragleave", function (e) {
			div.removeClass("filehover");
		});
		div.on("drop", async function (e) {
			e.preventDefault();
			e.stopPropagation();
			div.removeClass("filehover");
			if (e.originalEvent.dataTransfer) {
				var items = e.originalEvent.dataTransfer.items;
				var foldersDropped = false;
				if (items && items.length) {
					for (var i = 0; i < items.length; i++) {
						var item = items[i];
						if (item.kind === "file") {
							var entry = item.webkitGetAsEntry();
							if (entry && entry.isDirectory) {
								foldersDropped = true;
								break;
							}
						}
					}
				}
				if (foldersDropped) {
					customToast("Only files are supported. Folders were dropped.", {
						positive: false,
					});
					return;
				}
				var files = e.originalEvent.dataTransfer.files;

				if (files && files.length > 0) {
					e.preventDefault();
					e.stopPropagation();
					var moduleid = $(this).data("moduleid");
					if (!moduleid) {
						return;
					}
					var uploader = `${apphome}/views/modules/${moduleid}/editors/bulkentitycreator/dialog.html?edit=true&addnew=true&moduleid=${moduleid}&viewid=${moduleid}addnew`;

					var parentmoduleid = $(this).data("entitymoduleid");
					if (parentmoduleid) {
						var parententityid = $(this).data("entityid");
						if (parententityid) {
							uploader += `&parentmoduleid=${parentmoduleid}&parententityid=${parententityid}`;
						}
					}

					var dialog = $(
						`<a href="${uploader}" data-maxwidth="sm" title="Create Bulk Folders from Files"></a>`
					);
					allfiles = new Array();
					dialog.emDialog(function () {
						$(".upload_field")
							.last()
							.triggerHandler("html5_upload.filesPicked", [files]);
					});
				} else {
					customToast("No files were dropped.", {
						positive: false,
					});
				}
			} else {
				customToast("Browser does not support drag and drop.", {
					positive: false,
				});
			}
		});
	});

	//Detect Youtube Link
	$("#uploaddescription").on("keyup", function () {
		var input = $("#uploaddescription");
		var inputtext = input.val();
		var targetdiv = input.data("targetdiv");
		var targeturl = apphome + "/collective/channel/addnewlink.html";
		delay(function () {
			var p =
				/(https:\/\/www\.(yotu\.be\/|youtube\.com)\/)(?:(?:.+\/)?(?:watch(?:\?v=|.+&v=))?(?:v=)?)([\w_-]{11})(&\.+)?/;
			if (inputtext.match(p)) {
				var videoURL = inputtext.match(p)[0];
				var videoID = inputtext.match(p)[3];
				var removelink = inputtext.replace(p, "");
				input.val(removelink);

				$("#" + targetdiv).load(targeturl + "?videoID=" + videoID);
			} else {
			}
		}, 500);
	});

	lQuery(".hideuploadarea").livequery("click", function (e) {
		e.preventDefault();
		$(".globaluploadarea").toggle();
	});
});

var delay = (function () {
	var timer = 0;
	return function (callback, ms) {
		clearTimeout(timer);
		timer = setTimeout(callback, ms);
	};
})();

function bytesToSize(bytes, precision) {
	var kilobyte = 1024;
	var megabyte = kilobyte * 1024;
	var gigabyte = megabyte * 1024;
	var terabyte = gigabyte * 1024;

	if (bytes >= 0 && bytes < kilobyte) {
		return bytes + " B";
	} else if (bytes >= kilobyte && bytes < megabyte) {
		return (bytes / kilobyte).toFixed(precision) + " KB";
	} else if (bytes >= megabyte && bytes < gigabyte) {
		return (bytes / megabyte).toFixed(precision) + " MB";
	} else if (bytes >= gigabyte && bytes < terabyte) {
		return (bytes / gigabyte).toFixed(precision) + " GB";
	} else if (bytes >= terabyte) {
		return (bytes / terabyte).toFixed(precision) + " TB";
	} else {
		return bytes + " B";
	}
}

$.fn.initUpload = function () {
	var inputfield = $(this);
	var uploadformarea = inputfield.closest(".uploadformarea");
	var autostart = false;
	inputfield.html5_upload({
		filesPicked: filesPicked,
		url: function (number) {
			var data = uploadformarea.find("#uploaddata");
			var url = data.attr("action");
			var str = data.serialize();
			return url + "?" + str;
			// return prompt(number + " url", "/");
		},
		autostart: autostart,
		//			         extraFields: function() {
		//			        	 return [];
		//			         },
		sendBoundary: window.FormData || $.browser.mozilla,
		headers: {
			"Access-Control-Allow-Credentials": "true",
		},
		onStart: function (event, total, files) {
			//$(".uploadinstructions").hide();
			//console.log("On start " + files.length );
			var completed = uploadformarea.find(".up-files-list-pending li").clone();
			uploadformarea.find(".up-files-list-pending").empty();

			uploadformarea.find("#up-files-list-completed").prepend(completed);
			uploadformarea.find("#completed-uploads").show();

			uploadformarea.find("#upload-start").hide();
			uploadformarea.find("#upload-completed").show();

			uploadformarea.show();

			var entityuploadPicker = uploadformarea.find(".entityuploadPicker"); //Remove This. This is silly. Just close yourself in Javascript
			if (entityuploadPicker) {
				entityuploadPicker.hide();
				entityuploadPicker.parent().find(".hideonupload").hide();
				entityuploadPicker.next(".loadericon").css("display", "inline-block");
			}

			return true;
			//Loop over all the files. add rows
			//alert("start");
		},
		onStartOne: function (event, name, number, total) {
			//Set the currrent upload number?
			currentupload = number;
			return true;
		},
		onProgress: function (event, progress, name, number, total) {
			// console.log(progress, number);
		},
		//         genName: function(file, number, total) {
		//             return file;
		//         },
		//         setName: function(text) {
		//             $("#progress_report_name" + currentupload).text(text);
		//         },
		setStatus: function (text) {
			if (text == "Progress") {
				text = "Uploading";
			}
			uploadformarea.find(".progress_report_status" + currentupload).text(text);
		},
		setProgress: function (val) {
			uploadformarea
				.find(".progress_report_bar" + currentupload)
				.css("width", Math.ceil(val * 100) + "%");
		},
		onFinishOne: function (event, response, name, number, total) {
			var div = uploadformarea.find(".uploadcompleteText");
			var totaluploads = div.data("totaluploads");
			totaluploads++;
			div.data("totaluploads", totaluploads);
			div.html(totaluploads + div.data("postfix"));

			uploadformarea
				.find(".progress_report_bar" + currentupload)
				.css("width", "100%");
		},
		onError: function (event, name, error) {
			customToast("Error while uploading file " + name, {
				autohideDelay: 5000,
				positive: false,
			});
			haderror = true;
		},
		onFinish: function (event, total) {
			if (!haderror) {
				customToast(
					`Uploaded ${total} file${total > 1 ? "s" : ""} successfully!`
				);
				//do a search
				var startb = uploadformarea.find(".startbutton");

				startb.text(startb.data("textcomplete"));

				allfiles = new Array();

				var completed = uploadformarea.find(".up-files-list-completed li span");
				$.each(completed, function () {
					$(this).removeAttr("id");
				});

				//Go to the editor and submit its ajax
				var editdiv = startb.closest(".uploadshowuploads");
				if (editdiv.length) {
					editdiv.runAjax();
				} else {
					startb.prop("disabled", false);
				}

				var form = $(startb.closest("form"));

				$(window).trigger("checkautoreload", [form]);
				var formmodal = form.closest(".modal");
				if (formmodal.length > 0 && form.hasClass("autocloseform")) {
					closeemdialog(formmodal);
				}
			}
		},
	});
};

function filesPicked(_, files, uploadformarea = null) {
	//merge them together
	if (!uploadformarea) {
		uploadformarea = $(this).closest(".uploadformarea");
		for (var i = 0; i < files.length; i++) {
			var file = files[i];
			if (file.size > 0) {
				allfiles.push(file);
			}
		}
	}

	if (allfiles.length === 0) {
		uploadformarea.find(".clear-upload-list").hide();
		uploadformarea.find(".dropicon").show();
	} else {
		uploadformarea.find(".clear-upload-list").show();
		uploadformarea.find(".dropicon").hide();
	}

	files = allfiles;
	var inputbox = uploadformarea.find(".upload_field")[0];

	var upload_field = uploadformarea.find(".upload_field");
	upload_field.triggerHandler("html5_upload.setFiles", [allfiles]);

	//inputbox.count = allfiles.length;

	//$("#upload_field").setFiles( allfiles );

	//inline
	var uploadformareainline = $(this).closest(".uploadformareainline");
	uploadformareainline.find("#drop-area").hide();
	uploadformareainline.find("#completed-uploads").show();

	//Upload page
	var startb = uploadformarea.find(".startbutton");
	startb.text(startb.data("textstartupload"));

	startb.prop("disabled", false);
	uploadformarea.find(".uploadinstructionsafter").show();
	uploadformarea.find(".showonselect").show();

	var regex = new RegExp("currentupload", "g");

	var uplist = uploadformarea.find(".up-files-list");

	$.each(uplist, function () {
		$(this).empty();
	});

	//return confirm("You are trying to upload " + total + " files. Are you sure?");
	for (var i = 0; i < files.length; i++) {
		var file = files[i];
		if (file.size > 0) {
			if (i < 101) {
				var html = uploadformarea.find(".progress_report_template").html();

				html = html.replace(regex, i);
				uploadformarea.find(".up-files-list-pending").append(html);

				//TODO: set the name and size of each row
				var name = file.name;
				if (file.webkitRelativePath) {
					name = file.webkitRelativePath;
				}

				uploadformarea.find(".progress_report_name" + i).text(name);
				var size = bytesToSize(file.size, 2);
				uploadformarea.find(".progress_report_size" + i).text(size);
			}
		}
	}
	//console.log("Picked " + files.length );
	if (upload_field.data("autostartupload") == true) {
		upload_field.triggerHandler("html5_upload.start");
	}
}
