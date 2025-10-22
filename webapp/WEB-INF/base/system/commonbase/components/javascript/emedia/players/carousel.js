function __setOverlayPosition(img) {
	var width = img.naturalWidth;
	var height = img.naturalHeight;
	var ratio = width / height;

	var containerWidth = img.offsetWidth;
	var containerHeight = img.offsetHeight;

	var renderWidth, renderHeight;
	if (ratio > 1) {
		renderWidth = containerWidth;
		renderHeight = containerWidth / ratio;
	} else {
		renderWidth = containerHeight * ratio;
		renderHeight = containerHeight;
	}

	if (renderHeight > containerHeight) {
		renderHeight = containerHeight;
		renderWidth = containerHeight * ratio;
	}
	if (renderWidth > containerWidth) {
		renderWidth = containerWidth;
		renderHeight = containerWidth / ratio;
	}

	var renderBottom = Math.max(0, (containerHeight - renderHeight) / 2);
	var renderLeft = Math.max(0, (containerWidth - renderWidth) / 2);

	var overlay = img.parentElement.querySelector(".__img_overlay");
	overlay.style.bottom = renderBottom + "px";
	overlay.style.left = renderLeft + "px";
	overlay.style.width = renderWidth + "px";

	var caption = overlay.querySelector(".__img_caption");
	var creator = overlay.querySelector(".__img_creator");

	if (creator || caption) {
		overlay.style.opacity = 1;
	}
}

window.addEventListener("resize", function () {
	var imgs = document.querySelectorAll(".__image img");
	for (var i = 0; i < imgs.length; i++) {
		__setOverlayPosition(imgs[i]);
	}
});
document.querySelectorAll(".__overlay_minimizer").forEach(function (btn) {
	btn.addEventListener("click", function (e) {
		e.preventDefault();
		var overlay = btn.parentElement;
		if (overlay.classList.contains("__minimized")) {
			overlay.classList.remove("__minimized");
		} else {
			overlay.classList.add("__minimized");
		}
	});
});
var __carousel_el = document.getElementById("__carousel");
var __main = new Splide(__carousel_el, {
	rewind: true,
	pagination: false,
	autoplay: __carousel_el.getAttribute("data-autoplay") === "true",
	lazyLoad: "nearby",
	keyboard: "global",
});
__main.on("mounted", function () {
	var lang = document.documentElement.lang;
	if (!lang) {
		var drupalSelector = $(
			"script[data-drupal-selector='drupal-settings-json']"
		).text();
		if (drupalSelector) {
			var drupalSettings = JSON.parse(drupalSelector);
			lang = drupalSettings.dataLayer.defaultLang;
		}
	}
	if (!lang) lang = window.navigator.language || window.navigator.userLanguage;
	if (lang.indexOf("-") != -1 || lang.indexOf("_") != -1) {
		var loc = lang.split(/[-_]/)[0];
		var variant = lang.split(/[-_]/)[1];
		if (loc.toLowerCase() == "zh") {
			if (
				variant.toLowerCase() == "hant" ||
				variant.toLowerCase() == "tw" ||
				variant.toLowerCase() == "hk"
			) {
				lang = "zh_TW";
			} else {
				lang = "zh";
			}
		}
	}
	var captions = document.querySelectorAll(".__img_caption");
	captions.forEach(function (caption) {
		var locales = caption.getAttribute("data-locales");
		if (locales) {
			try {
				locales = JSON.parse(locales);
				if (locales[lang] || locales["en"]) {
					caption.innerHTML = locales[lang] ? locales[lang] : locales["en"];
				} else {
					caption = null;
				}
			} catch (e) {
				// ignore
			}
		}
	});
});
__main.on("active", function (newSlide) {
	if (!newSlide) return;
	var image = newSlide.slide.querySelector(".__image img");
	__setOverlayPosition(image);
});
__main.on("inactive", function (newSlide) {
	if (!newSlide) return;
	var overlay = newSlide.slide.querySelector(".__img_overlay");
	overlay.style.opacity = 0;
});
var __thumbnail = new Splide("#__carousel-thumbnails", {
	fixedWidth: 95,
	fixedHeight: 60,
	gap: 10,
	rewind: true,
	pagination: false,
	isNavigation: true,
	arrows: false,
	focus: "center",
	breakpoints: {
		600: {
			fixedWidth: 60,
			fixedHeight: 44,
		},
	},
});
__main.sync(__thumbnail);
__main.mount();
__thumbnail.mount();
