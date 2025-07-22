function __setOverlayPosition(img) {
		var width = img.naturalWidth;
		var height = img.naturalHeight;
		var ratio = width / height; 

		var containerWidth = img.offsetWidth;
		var containerHeight = img.offsetHeight;

		var renderWidth, renderHeight;
		if(ratio > 1) {
			renderWidth = containerWidth;
			renderHeight = containerWidth / ratio;
		} else {
			renderWidth = containerHeight * ratio;
			renderHeight = containerHeight;
		}

		if(renderHeight > containerHeight) {
			renderHeight = containerHeight;
			renderWidth = containerHeight * ratio;
		}
		if(renderWidth > containerWidth) {
			renderWidth = containerWidth;
			renderHeight = containerWidth / ratio;
		}
		
		var renderBottom = Math.max(0, (containerHeight-renderHeight) / 2);
		var renderLeft = Math.max(0, (containerWidth-renderWidth) / 2);
		
		var overlay = img.parentElement.querySelector(".__img_overlay");
		overlay.style.bottom = renderBottom + "px";
		overlay.style.left = renderLeft + "px";
		overlay.style.width = renderWidth + "px";

		var caption = overlay.querySelector(".__img_caption");
		var creator = overlay.querySelector(".__img_creator");
		
		if(caption) {
			var locales = caption.getAttribute("data-locales");
			if(locales) {
				try {
					locales = JSON.parse(locales);
					var lang = document.documentElement.lang;
					if(!locales[lang]) {
						lang = "en";
					}
					if(locales[lang]) {
						caption.innerHTML = locales[lang];
					} else {
						caption = null;
					}
				} catch(e) {
					// ignore
				}
			}
		}

		if(creator || caption) {
			overlay.style.opacity = 1;
		}
	}

	window.addEventListener("resize", function() {
		var imgs = document.querySelectorAll(".__image img");
		for(var i = 0; i < imgs.length; i++) {
			__setOverlayPosition(imgs[i]);
		}
	})
	document.querySelectorAll(".__overlay_minimizer").forEach(function(btn) {
		btn.addEventListener("click", function(e) {
			e.preventDefault();
			var overlay = btn.parentElement;
			if(overlay.classList.contains("__minimized")) {
				overlay.classList.remove("__minimized");
			} else {
				overlay.classList.add("__minimized"); 
			}
		})
	})
	
	
	var __main = new Splide("#__carousel", {
		rewind: true,
		pagination: false,
		lazyLoad: "nearby",
		keyboard: 'global',
		
	});
	__main.on("active", function(newSlide) {
		if(!newSlide) return;
		var image = newSlide.slide.querySelector(".__image img");
		__setOverlayPosition(image);
	})
	__main.on("inactive", function(newSlide) {
		if(!newSlide) return;
		var overlay = newSlide.slide.querySelector(".__img_overlay");
		overlay.style.opacity = 0;
	})
	var __thumbnail = new Splide("#__carousel-thumbnails", {
		fixedWidth: 95,
		fixedHeight: 60,
		gap: 10,
		rewind: true,
		pagination: false,
		isNavigation: true,
		arrows: false,
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