/*jshint esversion: 6 */

(function () {
	"use strict";

	function humanFileSize(bytes, htm = false) {
		if (typeof bytes === "string") bytes = parseInt(bytes);
		if (isNaN(bytes)) return "";
		const thresh = 1000;
		if (Math.abs(bytes) < thresh) {
			return bytes + " B";
		}
		const units = ["kB", "MB", "GB", "TB"];
		let u = -1;
		do {
			bytes /= thresh;
			++u;
		} while (
			Math.round(Math.abs(bytes) * 10) / 10 >= thresh &&
			u < units.length - 1
		);
		if (htm) return `<b>${bytes.toFixed(1)}</b> ${units[u]}`;
		return bytes.toFixed(1) + units[u];
	}

	function elideCat(text, maxLength = 80) {
		text = text.replace(/\//g, " › ");
		if (text.length <= maxLength) {
			return text;
		}
		const charsPerSide = Math.floor((maxLength - 3) / 2);
		const leftSide = text.substring(0, charsPerSide);
		const rightSide = text.substring(text.length - charsPerSide);
		return leftSide + "..." + rightSide;
	}

	function isDuplicateIdentifier(identifier, identifiers) {
		for (let i = 0; i < identifiers.length; i++) {
			const identifier2 = identifiers[i];
			if (identifier === identifier2) return true;
			else if (identifier.startsWith(identifier2)) return true;
			else if (identifier2.startsWith(identifier)) return true;
		}
		return false;
	}

	jQuery(document).ready(function () {
		const { ipcRenderer } = require("electron");

		const app = $("#application");
		const siteroot = app.data("siteroot");
		const mediadb = app.data("mediadbappid");
		const apphome = app.data("apphome");
		const entermediakey = app.data("entermediakey");
		const requiredDesktopVersion = parseInt(
			app.data("required-desktop-version")
		);

		const headers = { "X-tokentype": "entermedia", "X-token": entermediakey };

		function getMediadb() {
			return siteroot + "/" + mediadb;
		}
		ipcRenderer
			.invoke("connection-established", {
				headers: headers,
				key: entermediakey,
				mediadb:
					window.location.protocol +
					"//" +
					window.location.host +
					"/" +
					mediadb,
			})
			.then(
				({ computerName, rootPath, downloadPath, currentDesktopVersion }) => {
					console.log({ currentDesktopVersion, requiredDesktopVersion });
					if (
						!isNaN(currentDesktopVersion) &&
						currentDesktopVersion !== requiredDesktopVersion
					) {
						customToast(
							`Incompatible desktop API version v${currentDesktopVersion}, required API version is <a href='https://emedialibrary.com/downloads.html?v=${requiredDesktopVersion}' target='_blank'>v${requiredDesktopVersion}</a>`,
							{
								positive: false,
								autohide: false,
							}
						);
					}
					$("#desktopLoading").remove();
					console.log($("#desktopLoading"));
					app.data("local-root", rootPath);
					app.data("local-download", downloadPath);
					app.data("computer-name", computerName);

					ipcRenderer.on(
						"siteLoaded",
						(_, { rootPath: rP, downloadPath: dP }) => {
							app.data("local-root", rP);
							app.data("local-download", dP);
							$("#desktopLoading").remove();
						}
					);
					ipcRenderer.on("page-title-updated", (_, title) => {
						if (title && title != document.title) {
							window.trigger("setPageTitle", [title]);
						}
					});

					ipcRenderer.on("electron-log", (_, ...log) => {
						console.log("Desktop ▼");
						if (Array.isArray(log)) {
							log.forEach((l) => {
								console.log(l);
							});
						} else {
							console.log(log);
						}
					});

					ipcRenderer.on("electron-error", (_, ...error) => {
						console.log("Desktop ▼");
						if (Array.isArray(error)) {
							error.forEach((l) => {
								console.error(l);
							});
						} else {
							console.error(error);
						}
						customToast("Desktop Error: Check log for details.", {
							autohideDelay: 5000,
							positive: false,
						});
					});

					function desktopImportStatusUpdater(formData, callback = null) {
						let moduleid = formData.get("moduleid");
						if (!moduleid) moduleid = "asset";
						formData.set("desktop", computerName);
						jQuery.ajax({
							url:
								apphome +
								"/views/modules/" +
								moduleid +
								"/components/sidebars/localdrives/updatefolderstatus.html",
							type: "POST",
							data: formData,
							processData: false,
							contentType: false,
							"Content-Type": "multipart/form-data",
							success: function (res) {
								$("#syncFolderList").html(res);
								if (callback) callback();
							},
							error: function (_xhr, _status, error) {
								console.log("desktopImportStatusUpdater", error);
							},
						});
					}

					lQuery(".open-file-default").livequery("click", function () {
						const categorypath = $(this).data("categorypath");
						const filename = $(this).data("filename");
						const dlink = $(this).data("dlink");
						ipcRenderer.send("openFileWithDefault", {
							categorypath,
							filename,
							dlink,
						});
					});

					lQuery(".open-folder").livequery("click", function () {
						let path = $(this).data("path");
						if (!path) {
							path = $(this).closest(".ofl-path").data("path");
						}
						const customRoot = $(this).data("root");
						const dropFromFolderPath = $(this).data("removecategorysubfolder");
						if (path) {
							ipcRenderer.send("openFolder", {
								customRoot,
								folderPath: path,
								dropFromFolderPath,
							});
						}
					});

					lQuery(".dir-picker").livequery("click", function (e) {
						e.preventDefault();
						window.postMessage({
							type: "dir-picker",
							targetDiv: $(this).data("target"),
							currentPath: $(this).prev().val(),
						});
					});

					ipcRenderer.on("dir-picked", (_, { targetDiv, path }) => {
						$("#" + targetDiv).val(path);
					});

					lQuery("#localRootPathInput").livequery(function () {
						$(this).val(app.data("local-root"));
					});

					lQuery("#localDownloadPathInput").livequery(function () {
						$(this).val(app.data("local-download"));
					});

					lQuery("#changeDesktopSettings").livequery("click", function (e) {
						e.preventDefault();
						const rootPath = $("#localRootPathInput").val();
						const downloadPath = $("#localDownloadPathInput").val();
						ipcRenderer.send("changeDesktopSettings", {
							rootPath,
							downloadPath,
						});
						closeemdialog($(this).closest(".modal"));
					});

					lQuery(".deleteSyncFolder").livequery("click", function () {
						if (confirm("Are you sure you want to remove this sync task?")) {
							const delId = $(this).data("id");
							const identifier = $(this).data("categorypath");
							const isDownload = $(this).hasClass("download");
							ipcRenderer.send("cancelSync", { identifier, isDownload });
							jQuery.ajax({
								type: "DELETE",
								url:
									getMediadb() +
									"/services/module/desktopsyncfolder/data/" +
									delId,
								success: function () {
									$("#wf-" + delId).remove();
									customToast("Sync task deleted successfully!");
								},
								error: function (xhr, status, error) {
									console.log("deleteSyncFolder", error);
									customToast("Error deleting sync task!", {
										positive: false,
									});
								},
							});
						}
					});

					lQuery(".show-sync-progress").livequery("click", function () {
						$(this).prop("disabled", true);
						$("#col-sidebars").load(
							apphome + "/components/sidebars/index.html",
							{
								propertyfield: "sidebarcomponent",
								sidebarcomponent: "localdrives",
								"sidebarcomponent.value": "localdrives",
							}
						);
						closeemdialog($(this).closest(".modal"));
						$(window).trigger("resize");
					});

					lQuery(".quick-download").livequery("click", function () {
						$(this).prop("disabled", true);
						$("#col-sidebars").load(
							apphome + "/components/sidebars/index.html",
							{
								propertyfield: "sidebarcomponent",
								sidebarcomponent: "localdrives",
								"sidebarcomponent.value": "localdrives",
							}
						);
						$(window).trigger("resize");

						const entityid = $(this).data("entityid");
						const moduleid = $(this).data("moduleid");
						const uploadsourcepath = $(this).data("path");

						let categorypath = uploadsourcepath;
						categorypath = categorypath.replace(/\\/g, "/");
						categorypath = categorypath.replace(/\/+/g, "/");
						categorypath = categorypath.replace(/\/$/g, "");

						const formData = new FormData();
						formData.set("categorypath", categorypath);
						formData.set("entityid", entityid);
						formData.set("moduleid", moduleid);
						formData.set("desktopimportstatus", "scan-started");
						formData.set("isdownload", "true");

						ipcRenderer
							.invoke("lightboxDownload", uploadsourcepath)
							.then((scanStatus) => {
								if (scanStatus === "OK") {
									desktopImportStatusUpdater(formData);
								} else if (scanStatus === "DUPLICATE_DOWNLOAD") {
									customToast(
										"Already running a download task in this folder, wait until it finishes",
										{
											positive: false,
											autohideDelay: 5000,
										}
									);
								} else if (scanStatus === "TOO_MANY_DOWNLOADS") {
									customToast(
										"Wait for at least one other download task to finish",
										{
											positive: false,
											autohideDelay: 5000,
										}
									);
								}
							})
							.catch((error) => {
								console.log("quick-download", error);
							})
							.finally(() => {
								$(this).prop("disabled", false);
							});
					});

					lQuery(".lightbox-header-btns").livequery(function () {
						ipcRenderer.send("check-sync");

						const headerBtns = $(this);

						const uploadsourcepath = headerBtns.data("path");
						const entityId = headerBtns.data("entityid");
						const moduleid = headerBtns.data("moduleid");

						let categorypath = uploadsourcepath;
						categorypath = categorypath.replace(/\\/g, "/");
						categorypath = categorypath.replace(/\/+/g, "/");
						categorypath = categorypath.replace(/\/$/g, "");

						const formData = new FormData();
						formData.set("moduleid", moduleid);
						formData.set("entityid", entityId);
						formData.set("categorypath", categorypath);

						headerBtns.on("click", ".download-lightbox", function () {
							customToast(
								elideCat(categorypath) + " download task added to Cloud Sync"
							);

							$(this).prop("disabled", true);
							$(this).find("span").text("Downloading...");

							$(this).removeClass("has-changes");

							formData.set("desktopimportstatus", "scan-started");
							formData.set("isdownload", "true");

							ipcRenderer
								.invoke("lightboxDownload", uploadsourcepath)
								.then((scanStatus) => {
									if (scanStatus === "OK") {
										desktopImportStatusUpdater(formData);
										headerBtns.find(".view-task-progress").show();
									} else if (scanStatus === "DUPLICATE_DOWNLOAD") {
										customToast(
											"Already running a download task in this folder, wait until it finishes",
											{
												positive: false,
												autohideDelay: 5000,
											}
										);
									} else if (scanStatus === "TOO_MANY_DOWNLOADS") {
										customToast(
											"Wait for at least one other download task to finish",
											{
												positive: false,
												autohideDelay: 5000,
											}
										);
									}
								})
								.catch((error) => {
									console.log("lightboxDownload", error);
								});
						});

						headerBtns.on("click", ".upload-lightbox", function () {
							customToast(
								elideCat(categorypath) + " upload task added to Cloud Sync"
							);

							$(this).prop("disabled", true);
							$(this).find("span").text("Uploading...");

							$(this).removeClass("has-changes");

							formData.set("desktopimportstatus", "scan-started");

							ipcRenderer
								.invoke("lightboxUpload", uploadsourcepath)
								.then((scanStatus) => {
									if (scanStatus === "OK") {
										desktopImportStatusUpdater(formData);
										headerBtns.find(".view-task-progress").show();
									} else if (scanStatus === "DUPLICATE_UPLOAD") {
										customToast(
											"Already running an upload task in this folder, wait until it finishes",
											{
												positive: false,
												autohideDelay: 5000,
											}
										);
									} else if (scanStatus === "TOO_MANY_UPLOADS") {
										customToast(
											"Wait for at least one other upload task to finish",
											{
												positive: false,
												autohideDelay: 5000,
											}
										);
									}
								})
								.catch((error) => {
									console.log("lightboxUpload", error);
								});
						});

						headerBtns.on("click", ".scan-changes", function () {
							customToast(
								"Scanning for unsynced files in " + elideCat(categorypath),
								{ id: categorypath }
							);

							$(this).prop("disabled", true);
							$(this).addClass("scanning");

							const idEl = headerBtns;
							ipcRenderer
								.invoke("scanChanges", uploadsourcepath)
								.then(({ hasUploads, hasDownloads }) => {
									const ch = [];

									if (hasUploads) {
										ch.push("upload");
										idEl.find(".upload-lightbox").addClass("has-changes");
									} else {
										idEl.find(".upload-lightbox").removeClass("has-changes");
									}
									if (hasDownloads) {
										ch.push("download");
										idEl.find(".download-lightbox").addClass("has-changes");
									} else {
										idEl.find(".download-lightbox").removeClass("has-changes");
									}
									if (ch.length > 0) {
										customToast(
											"Found new files to " +
												ch.join(" & ") +
												"in " +
												elideCat(categorypath),
											{
												id: categorypath,
											}
										);
									} else {
										customToast(
											"No unsynced files found in " + elideCat(categorypath),
											{ id: categorypath }
										);
									}
								})
								.catch((error) => {
									console.log("scanChanges", error);
								})
								.finally(() => {
									$(this).prop("disabled", false);
									$(this).removeClass("scanning");
								});
						});
					});

					function shouldDisableUploadSyncBtn(data) {
						$(".lightbox-header-btns").forEach(function () {
							const identifier = $(this).data("path");
							if (!identifier) {
								return;
							}
							const btn = $(this).find(".upload-lightbox");
							const taskProg = $(this).find(".view-task-progress");

							if (isDuplicateIdentifier(identifier, data)) {
								btn.prop("disabled", true);
								btn.find("span").text("Uploading...");
								taskProg.show();
							} else {
								btn.prop("disabled", false);
								btn.find("span").text("Upload");
								taskProg.hide();
							}
						});
					}

					ipcRenderer.on(
						"check-sync",
						(_, { up_identifiers, dn_identifiers }) => {
							if (up_identifiers && up_identifiers.length > 0) {
								shouldDisableUploadSyncBtn(up_identifiers);
							}
							if (dn_identifiers && dn_identifiers.length > 0) {
								shouldDisableDownloadSyncBtn(dn_identifiers);
							}
						}
					);

					function shouldDisableDownloadSyncBtn(data) {
						$(".lightbox-header-btns").forEach(function () {
							const identifier = $(this).data("path");
							if (!identifier) {
								return;
							}

							const btn = $(this).find(".download-lightbox");
							const taskProg = $(this).find(".view-task-progress");

							if (isDuplicateIdentifier(identifier, data)) {
								btn.prop("disabled", true);
								btn.find("span").text("Downloading...");
								taskProg.show();
							} else {
								btn.prop("disabled", false);
								btn.find("span").text("Download");
								taskProg.hide();
							}
						});
					}

					lQuery(".cancelSync").livequery("click", function (e) {
						e.preventDefault();
						e.stopPropagation();
						const btn = $(this);
						const identifier = btn.data("categorypath");
						const isDownload = btn.hasClass("download");
						ipcRenderer.send("cancelSync", { identifier, isDownload });
					});

					lQuery(".cancel-both").livequery("click", function (e) {
						e.preventDefault();
						e.stopPropagation();
						const consent = confirm(
							"Are you sure you want to cancel the upload/download tasks?"
						);
						if (!consent) return;
						const identifier = $(this)
							.closest(".lightbox-header-btns")
							.data("path");
						ipcRenderer.send("cancelSync", {
							identifier,
							both: true,
						});
					});

					const progItem = (identifier, dl = false) => {
						const el = $(
							`.work-folder${
								dl ? ".download" : ".upload"
							}[data-categorypath="${identifier}"]`
						);
						if (el.length > 0) {
							return el;
						}
						return null;
					};

					// <all sync events>
					ipcRenderer.on("sync-started", (_, data) => {
						console.log("sync-started", data);
						const idEl = progItem(data.identifier, data.isDownload);
						if (idEl) {
							idEl.addClass("processing");
							if (data.isDownload) {
								idEl.addClass("download");
							} else {
								idEl.addClass("upload");
							}
							idEl.find(".filesCompleted").text(0);
							idEl.find(".filesTotal").text(data.total);
							idEl.find(".filesFailed").text(0);
							idEl.find(".fileProgress").css("width", "0%");
							idEl.find(".fileProgressLoaded").text(0);
							idEl.find(".fileProgressTotal").text(0);
						}
					});

					ipcRenderer.on("sync-progress-update", (_, data) => {
						console.log("sync-progress-update", data);
						const idEl = progItem(data.identifier, data.isDownload);
						if (idEl) {
							idEl.find(".filesCompleted").text(data.completed);
							idEl.find(".filesFailed").text(data.failed);
							idEl.find(".filesTotal").text(data.total);
						}
					});

					ipcRenderer.on("sync-completed", (_, data) => {
						console.log("sync-completed", data);
						const idEl = progItem(data.identifier, data.isDownload);
						if (idEl) {
							idEl.removeClass("processing");
							idEl.data("index", undefined);
						}

						const formData = new FormData();
						formData.append("completedfiles", data.completed || 0);
						formData.append("failedfiles", data.failed || 0);
						formData.append("categorypath", data.identifier);
						formData.append("desktopimportstatus", "sync-completed");
						if (data.isDownload) formData.append("isdownload", "true");
						desktopImportStatusUpdater(formData, () => {
							if (data.isDownload) {
								shouldDisableDownloadSyncBtn(data.remaining);
							} else {
								shouldDisableUploadSyncBtn(data.remaining);
							}
							customToast(
								`${
									data.isDownload ? "Downloaded" : "Uploaded"
								} all files from ${elideCat(data.identifier)}!`,
								{ id: data.identifier }
							);
						});
						ipcRenderer.send("check-sync");
						const dataeditedreload = $(".dataeditedreload");
						dataeditedreload.each(function () {
							$(window).trigger("autoreload", [
								$(this),
								null,
								"dataeditedreload",
							]);
						});
					});

					ipcRenderer.on(
						"sync-cancelled",
						(_, { identifier, isDownload, both }) => {
							console.log("sync-cancelled", identifier);
							function cancelSync(isD) {
								const idEl = progItem(identifier, isD);
								let filesCompleted = 0;
								if (idEl) {
									idEl.removeClass("processing");
									idEl.data("index", undefined);
									filesCompleted = idEl.find(".filesCompleted").text();
								}

								const formData = new FormData();
								formData.append("categorypath", identifier);
								formData.append("desktopimportstatus", "sync-cancelled");
								if (isD) formData.append("isdownload", "true");
								if (filesCompleted >= 0)
									formData.append("completedfiles", filesCompleted);
								desktopImportStatusUpdater(formData);
							}
							if (both) {
								cancelSync(true);
								cancelSync(false);
							} else {
								cancelSync(isDownload);
							}
							ipcRenderer.send("check-sync");
						}
					);

					ipcRenderer.on("file-status-update", (_, data) => {
						console.log("file-status-update", data);
						const idEl = progItem(data.identifier, data.isDownload);
						if (!idEl) return;
						idEl.data("index", data.index);
						if (data.status === "uploading" || data.status === "downloading") {
							idEl.addClass("processing");
							idEl.find(".fileName").text(data.name);
							idEl.find(".fileProgress").css("width", "0%");
							idEl.find(".fileProgressLoaded").text(0);
							idEl.find(".fileProgressTotal").text(humanFileSize(data.size));
						} else if (data.status === "completed") {
							idEl.find(".fileProgress").css("width", "100%");
							idEl.find(".fileProgressLoaded").text(humanFileSize(data.size));
							idEl.find(".fileProgressTotal").text(humanFileSize(data.size));
						}
					});

					ipcRenderer.on("file-progress-update", (_, data) => {
						console.log("file-progress-update", data);
						const idEl = progItem(data.identifier, data.isDownload);
						if (!idEl) return;
						// const index = idEl.data("index");
						// if (index === undefined) return;

						const progressEl = idEl.find(".fileProgress");
						progressEl.css("width", Math.min(data.percent * 100, 100) + "%");

						const loadedEl = idEl.find(".fileProgressLoaded");
						loadedEl.text(humanFileSize(data.loaded));

						const totalEl = idEl.find(".fileProgressTotal");
						totalEl.text(humanFileSize(data.total));
					});
					// </all sync events>

					// <single file download events>
					ipcRenderer.on("download-update", (_, data) => {
						const toastId = data.filename.replace(/[^a-zA-Z0-9]/g, "_");
						customToast(data.message, {
							id: toastId,
							positive: !data.error,
						});
					});
					// </single file download events>

					lQuery(".work-folder.processing").livequery(function () {
						if ($(this).hasClass("upload-started")) return;
						if ($(this).hasClass("download-started")) return;
						if ($(this).hasClass("sync-completed")) return;
						const uploadsourcepath = $(this).data("categorypath");

						let categorypath = uploadsourcepath;
						categorypath = categorypath.replace(/\\/g, "/");
						categorypath = categorypath.replace(/\/+/g, "/");
						categorypath = categorypath.replace(/\/$/g, "");

						const isDownload = $(this).hasClass("download");

						ipcRenderer
							.invoke("continueSync", {
								categoryPath: uploadsourcepath,
								isDownload,
							})
							.then((scanStatus) => {
								if (scanStatus === "OK")
									customToast(
										"Continuing " +
											(isDownload ? "download" : "upload") +
											" task for " +
											elideCat(categorypath)
									);
							})
							.catch((err) => {
								console.error("continueSync", err);
							});
					});

					lQuery(".desktopdirectdownload").livequery("click", function (e) {
						e.preventDefault();
						ipcRenderer.send("directDownload", $(this).attr("href"));
					});

					window.addEventListener("offline", () => {
						customToast("Network connection lost", {
							positive: false,
							id: "network-connection",
							autohideDelay: 5000,
						});
						console.log("The network connection has been lost.");
					});

					window.addEventListener("online", () => {
						customToast("Network connection restored", {
							id: "network-connection",
						});
						console.log("The network connection has been restored.");
					});
				}
			)
			.catch((err) => {
				console.error(err);
				customToast("Error establishing connection with Electron!", {
					positive: false,
				});
			});
	});
})();
