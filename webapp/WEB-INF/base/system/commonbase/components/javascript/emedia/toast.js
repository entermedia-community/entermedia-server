var toastTO = {};

$(window).on("showToast", function (_, anchor) {
	if (!anchor || anchor.length == 0 || typeof anchor.data != "function") return;
	if (anchor.data("no-toast") === true || anchor.hasClass("no-toast")) {
		return;
	}
	var uid = Date.now();
	anchor.data("uid", uid);
	var delay = 10;
	var toastMessage = anchor.data("toastmessage");
	if (!toastMessage) delay = 1250;
	var toastSuccess = anchor.data("toastsuccess");
	var toastError = anchor.data("toasterror");
	if (!toastSuccess) {
		toastSuccess = toastMessage ? "Done!" : "Loaded!";
	}
	if (!toastError) {
		toastError = toastMessage ? "Failed!" : "Error processing the request";
	}
	if (!toastMessage) {
		toastMessage = "Loading...";
	}

	var toast = $(
		`<div class="toastContainer" role="alert" data-uid="${uid}">
			<div class="toastLoader"></div>
			<div class="toastMessage" data-success="${toastSuccess}"  data-error="${toastError}">
				${toastMessage}
			</div>
			<div class="toastClose">&times;</div>
		</div>`
	);

	toastTO[uid] = setTimeout(function () {
		$(".toastList").append(toast);
	}, delay);
});

customToast = function (message, options = {}) {
	var oldToast = null;
	if (options.id) {
		oldToast = $(".toastList").find(`.toastContainer[data-id="${options.id}"]`);
	}
	var autohide = options.autohide === undefined ? true : options.autohide;
	var autohideDelay = options.autohideDelay || 3000;
	var positive = options.positive === undefined ? true : options.positive;

	if (options.log) {
		console.log(options.log);
	}

	if (!positive) {
		$(".toastList")
			.find(".toastError")
			.each(function () {
				if ($(this).next().text() !== message) return;
				$(this).parent().remove();
			});
	}

	var btnText = options.btnText;
	var btnClass = options.btnClass || "";
	var icon = options.icon;
	var iconHtml = `<div class="toast${positive ? "Success" : "Error"}"></div>`;
	if (icon) {
		iconHtml = `<div class="toastIcon ${
			!positive ? "error" : ""
		}"><i class="bi bi-${icon}"></i></div>`;
	}
	if (options.loading) {
		iconHtml = '<div class="toastLoader"></div>';
		autohideDelay = options.autohideDelay || 5000;
	}

	var toast = $(
		`<div class="toastContainer ${
			positive ? "positive" : "negative"
		}" data-id="${options?.id || ""}" role="alert">
				${iconHtml}
				<div class="toastMessage">${message.trim()}</div>
				${btnText ? `<button class="${btnClass}">${btnText}</button>` : ""}
				<div class="toastClose">&times;</div>
			</div>`
	);

	if (oldToast && oldToast.length > 0) {
		oldToast.replaceWith(toast);
	} else {
		$(".toastList").append(toast);
	}
	if (autohide) {
		setTimeout(function () {
			if (!toast) return;
			toast.addClass("hide");
			setTimeout(function () {
				if (!toast) return;
				toast.remove();
			}, 500);
		}, autohideDelay);
	}
};

destroyToast = function (toast, success = true) {
	if (!toast) return;
	toast
		.find(".toastLoader")
		.replaceWith(
			success
				? '<div class="toastSuccess"></div>'
				: '<div class="toastError"></div>'
		);
	var msg = toast.find(".toastMessage").data(success ? "success" : "error");
	if (msg) {
		toast.find(".toastMessage").text(msg);
	}
	setTimeout(function () {
		if (!toast) return;
		toast.addClass("hide");
		setTimeout(function () {
			if (!toast) return;
			toast.remove();
		}, 500);
	}, 2000);
};

$(window).on("successToast", function (_, uid) {
	if (!uid) return;
	if (toastTO[uid]) clearTimeout(toastTO[uid]);
	var toast = $(".toastContainer[data-uid='" + uid + "']");
	destroyToast(toast);
});

$(window).on("errorToast", function (_, uid) {
	if (!uid) return;
	if (toastTO[uid]) clearTimeout(toastTO[uid]);
	var toast = $(".toastContainer[data-uid='" + uid + "']");
	destroyToast(toast, false);
});

$(document).ready(function () {
	lQuery(".toastClose").livequery("click", function () {
		var toast = $(this).closest(".toastContainer");
		toast.addClass("hide");
		setTimeout(function () {
			toast.remove();
		}, 500);
	});
});
