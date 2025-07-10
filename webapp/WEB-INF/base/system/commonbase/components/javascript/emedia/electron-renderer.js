/*jshint esversion: 6 */

(function () {
  "use strict";

  const SYNC_PROGRESS_UPDATE = "sync-progress-update",
    SYNC_FOLDER_DELETED = "sync-folder-deleted",
    SYNC_CANCELLED = "sync-cancelled",
    SYNC_STARTED = "sync-started",
    SYNC_FOLDER_COMPLETED = "sync-folder-completed",
    SYNC_FULLY_COMPLETED = "sync-fully-completed",
    FILE_PROGRESS_UPDATE = "file-progress-update",
    FILE_STATUS_UPDATE = "file-status-update",
    CHECK_SYNC = "check-sync";

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

  jQuery(document).ready(function () {
    if (typeof require !== "function") {
      $("#desktopLoading").remove();
      return;
    }

    const { ipcRenderer } = require("electron");

    const app = $("#application");
    const mediadb = app.data("mediadbappid");
    const apphome = app.data("apphome");
    const entermediakey = app.data("entermediakey");
    const requiredDesktopVersion = parseInt(
      app.data("required-desktop-version")
    );

    const headers = { "X-tokentype": "entermedia", "X-token": entermediakey };

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
              document.title = title;
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

          const updateLoader = () => {
            var loaderPreview = $(".desktopSyncPreview");
            if (loaderPreview.find(".work-folder.processing").length > 0) {
              loaderPreview
                .find(".syncIcon")
                .removeClass("fa-desktop")
                .addClass("fa-spinner fa-spin");
            } else {
              loaderPreview
                .find(".syncIcon")
                .removeClass("fa-spinner fa-spin")
                .addClass("fa-desktop");
            }
          };

          let updateTO = null;
          let lastCompleted = null;
          function updateSyncUI(syncfolderid, callback = null, force = false) {
            function handleCallback() {
              if (callback) callback();
              updateLoader();
              if (lastCompleted && lastCompleted.trim().length > 0) {
                $(
                  ".work-folder.processing[data-syncfolderid='" +
                    syncfolderid +
                    "']"
                )
                  .find(".fileName")
                  .text(lastCompleted);
              }
            }
            if (force) {
              clearTimeout(updateTO);
              updateTO = null;
              $("#desktoppendingpopover").runAjax(handleCallback);
              $("#desktopsynchistory").runAjax(handleCallback);
            } else if (!updateTO) {
              updateTO = setTimeout(() => {
                $("#desktoppendingpopover").runAjax(handleCallback);
                $("#desktopsynchistory").runAjax(handleCallback);
                clearTimeout(updateTO);
                updateTO = null;
              }, 1000);
            }
          }

          function desktopSyncStarter(formData, callback = null) {
            formData.set("desktop", computerName);
            jQuery.ajax({
              url:
                "/" +
                mediadb +
                "/services/module/asset/entity/desktopsyncstart.json",
              type: "POST",
              data: formData,
              processData: false,
              contentType: false,
              "Content-Type": "multipart/form-data",
              success: function (res) {
                if (res.data && res.data.id) {
                  updateSyncUI(res.data.id);
                  if (callback) callback(res.data);
                } else {
                  console.log("desktopSyncStarter", res);
                }
              },
              error: function (_xhr, _status, error) {
                console.log("desktopSyncStarter", error);
              },
            });
          }
          function desktopSyncRestart(formData, callback = null) {
            formData.set("desktop", computerName);
            jQuery.ajax({
              url:
                "/" +
                mediadb +
                "/services/module/asset/entity/desktopsyncrestart.json",
              type: "POST",
              data: formData,
              processData: false,
              contentType: false,
              "Content-Type": "multipart/form-data",
              success: function (res) {
                if (res.data) {
                  updateSyncUI(res.data.id);
                  if (callback) callback(res.data);
                }
              },
              error: function (_xhr, _status, error) {
                console.log("desktopSyncRestart", error);
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
              const id = $(this).data("syncfolderid");
              const isDownload = $(this).hasClass("download");
              ipcRenderer.send("deleteSync", {
                identifier: id,
                delId: id,
                isDownload,
              });
            }
          });

          ipcRenderer.on(
            SYNC_FOLDER_DELETED,
            (_, { delId, success = true }) => {
              updateSyncUI(delId, () => {
                if (success) {
                  $("#wf-" + delId).remove();
                  customToast("Sync task deleted successfully!");
                } else {
                  customToast("Error deleting sync task!", {
                    positive: false,
                  });
                }
              });
            }
          );

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
            const entitymoduleid = $(this).data("entitymoduleid");
            const uploadsourcepath = $(this).data("path");

            let categorypath = uploadsourcepath;
            categorypath = categorypath.replace(/\\/g, "/");
            categorypath = categorypath.replace(/\/+/g, "/");
            categorypath = categorypath.replace(/\/$/g, "");

            const formData = new FormData();
            formData.set("categorypath", categorypath);
            formData.set("entityid", entityid);
            formData.set("entitymoduleid", entitymoduleid);
            formData.set("desktopimportstatus", "scan-started");
            formData.set("isdownload", "true");
            desktopSyncStarter(formData, function (synfolder) {
              console.log("quick-download", synfolder);
              ipcRenderer
                .invoke("lightboxDownload", {
                  categoryPath: uploadsourcepath,
                  syncFolderId: synfolder.id,
                })
                .then((downloadStatus) => {
                  if (downloadStatus === "OK") {
                    // OK
                  } else if (downloadStatus === "DUPLICATE_DOWNLOAD") {
                    customToast(
                      "Already running a download task in this folder, wait until it finishes",
                      {
                        positive: false,
                        autohideDelay: 5000,
                      }
                    );
                  } else if (downloadStatus === "TOO_MANY_DOWNLOADS") {
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
          });

          lQuery(".redownload").livequery("click", function (e) {
            e.preventDefault();
            e.stopPropagation();

            var folder = $(this).closest(".work-folder");
            var syncfolderid = folder.data("syncfolderid");

            $(this).prop("disabled", true);
            $(this).addClass("active");

            const formData = new FormData();
            formData.set("syncfolderid", syncfolderid);

            desktopSyncRestart(formData, function (synfolder) {
              ipcRenderer
                .invoke("lightboxDownload", {
                  categoryPath: synfolder.categorypath,
                  syncFolderId: synfolder.id,
                })
                .then((downloadStatus) => {
                  if (downloadStatus === "OK") {
                    // OK
                  } else if (downloadStatus === "DUPLICATE_DOWNLOAD") {
                    customToast(
                      "Already running a download task in this folder, wait until it finishes",
                      {
                        positive: false,
                        autohideDelay: 5000,
                      }
                    );
                    $(this).prop("disabled", false);
                    $(this).removeClass("active");
                  } else if (downloadStatus === "TOO_MANY_DOWNLOADS") {
                    customToast(
                      "Wait for at least one other download task to finish",
                      {
                        positive: false,
                        autohideDelay: 5000,
                      }
                    );
                    $(this).prop("disabled", false);
                    $(this).removeClass("active");
                  }
                })
                .catch((error) => {
                  console.log("lightboxDownload", error);
                  $(this).prop("disabled", false);
                  $(this).removeClass("active");
                });
            });
          });

          lQuery(".lightbox-header-btns").livequery(function () {
            const headerBtns = $(this);

            const uploadsourcepath = headerBtns.data("path");
            const entityid = headerBtns.data("entityid");
            const entitymoduleid = headerBtns.data("entitymoduleid");

            let categorypath = uploadsourcepath;
            categorypath = categorypath.replace(/\\/g, "/");
            categorypath = categorypath.replace(/\/+/g, "/");
            categorypath = categorypath.replace(/\/$/g, "");

            const formData = new FormData();
            formData.set("entitymoduleid", entitymoduleid);
            formData.set("entityid", entityid);
            formData.set("categorypath", categorypath);

            headerBtns.on("click", ".download-lightbox", function () {
              customToast(
                elideCat(categorypath) + " download task added to Cloud Sync",
                { id: categorypath }
              );

              $(this).prop("disabled", true);
              $(this).find("span").text("Downloading...");

              $(this).removeClass("has-changes");

              formData.set("desktopimportstatus", "scan-started");
              formData.set("isdownload", "true");
              desktopSyncStarter(formData, function (synfolder) {
                console.log("download", synfolder);
                ipcRenderer
                  .invoke("lightboxDownload", {
                    categoryPath: uploadsourcepath,
                    syncFolderId: synfolder.id,
                  })
                  .then((downloadStatus) => {
                    if (downloadStatus === "OK") {
                      headerBtns
                        .closest(".desktopSyncPreview")
                        .addClass("processing");
                    } else if (downloadStatus === "DUPLICATE_DOWNLOAD") {
                      customToast(
                        "Already running a download task in this folder, wait until it finishes",
                        {
                          positive: false,
                          autohideDelay: 5000,
                        }
                      );
                    } else if (downloadStatus === "TOO_MANY_DOWNLOADS") {
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
            });

            headerBtns.on("click", ".upload-lightbox", function () {
              customToast(
                elideCat(categorypath) + " upload task added to Cloud Sync",
                { id: categorypath }
              );

              $(this).prop("disabled", true);
              $(this).find("span").text("Uploading...");

              $(this).removeClass("has-changes");

              formData.set("desktopimportstatus", "scan-started");

              desktopSyncStarter(formData, function (synfolder) {
                console.log("upload", synfolder);
                ipcRenderer
                  .invoke("lightboxUpload", {
                    categoryPath: uploadsourcepath,
                    syncFolderId: synfolder.id,
                  })
                  .then((uploadStatus) => {
                    if (uploadStatus === "OK") {
                      headerBtns
                        .closest(".desktopSyncPreview")
                        .addClass("processing");
                    } else if (uploadStatus === "DUPLICATE_UPLOAD") {
                      customToast(
                        "Already running an upload task in this folder, wait until it finishes",
                        {
                          positive: false,
                          autohideDelay: 5000,
                        }
                      );
                    } else if (uploadStatus === "TOO_MANY_UPLOADS") {
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
            });
          });

          jQuery(document).on("click", ".cancelSync", function (e) {
            e.preventDefault();
            e.stopPropagation();
            const btn = $(this);
            const identifier = btn.data("syncfolderid");
            const isDownload = btn.hasClass("download");
            ipcRenderer.send("cancelSync", {
              identifier,
              isDownload,
            });
          });

          const progItem = (identifier, dl = false) => {
            const el = $(
              `.work-folder${
                dl ? ".download" : ".upload"
              }[data-syncfolderid="${identifier}"]`
            );
            if (el.length > 0) {
              return el;
            }
            return null;
          };

          // <all sync events>
          ipcRenderer.on(SYNC_STARTED, (_, data) => {
            console.log(SYNC_STARTED, data);
            customToast(
              `${
                data.isDownload ? "Downloading" : "Uploading"
              } files from ${elideCat(data.currentFolder)}!`,
              { id: data.identifier }
            );
            const idEl = progItem(data.identifier, data.isDownload);
            if (idEl) {
              idEl.find(".fileCompletedCount").text(`0 of `);
              idEl.find(".filesStats").show();
            }
            updateSyncUI(data.identifier, null, true);
          });

          ipcRenderer.on(SYNC_PROGRESS_UPDATE, (_, data) => {
            console.log(SYNC_PROGRESS_UPDATE, data);
            function update() {
              const idEl = progItem(data.identifier, data.isDownload);
              if (!idEl) return;
              if (data.completedSize > 0) {
                idEl
                  .find(".fileCompletedSize")
                  .text(`${humanFileSize(data.completedSize)} / `);
              }
              if (data.completed >= 0) {
                idEl.find(".fileCompletedCount").text(`${data.completed} of `);
                idEl.find(".filesStats").show();
              }
            }
            updateSyncUI(data.identifier, update);
          });

          ipcRenderer.on(
            SYNC_FULLY_COMPLETED,
            (_, { identifier, categoryPath, isDownload }) => {
              updateSyncUI(identifier, null, true);
              if (!isDownload) {
                const dataeditedreload = $(".dataeditedreload");
                dataeditedreload.each(function () {
                  $(window).trigger("autoreload", [
                    $(this),
                    null,
                    "dataeditedreload",
                  ]);
                });
              }
              if (categoryPath) {
                customToast(
                  `${
                    isDownload ? "Downloaded" : "Uploaded"
                  } all files from ${elideCat(categoryPath)}!`,
                  { id: identifier }
                );
              }
            }
          );

          ipcRenderer.on(SYNC_FOLDER_COMPLETED, (_, data) => {
            console.log(SYNC_FOLDER_COMPLETED, data);
            updateSyncUI(data.identifier);
          });

          ipcRenderer.on(SYNC_CANCELLED, (_, { identifier, isDownload }) => {
            const idEl = progItem(identifier, isDownload);
            if (idEl) {
              idEl.removeClass("processing");
              idEl.find(".currentSync").remove();
            }
            updateSyncUI(identifier, null, true);
          });

          ipcRenderer.on(FILE_STATUS_UPDATE, (_, data) => {
            console.log(FILE_STATUS_UPDATE, data);
            if (data.status !== "completed") return;
            lastCompleted = data.name;
          });

          ipcRenderer.on(FILE_PROGRESS_UPDATE, (_, data) => {
            console.log(FILE_PROGRESS_UPDATE, data);
            lastCompleted = data.name;
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

          lQuery("#desktoppendingpopover").livequery(function () {
            const oldTask = $(this).find(".work-folder.processing");
            if (oldTask.length > 0) {
              ipcRenderer.send(CHECK_SYNC, {
                syncFolderId: oldTask.data("syncfolderid"),
                isDownload: oldTask.hasClass("download"),
              });
            }
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
