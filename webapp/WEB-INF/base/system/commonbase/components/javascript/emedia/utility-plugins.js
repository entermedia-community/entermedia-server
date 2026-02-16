/**
 * =================================================================
 * jQuery ClickOutside Plugin
 * Detects clicks outside a specified element and triggers a handler
 * =================================================================
 */
(function ($) {
	var pluginName = "clickOutside";
	var doc = $(document);
	// Default options
	var defaults = {
		event: "mousedown", // 'mousedown' | 'click' | 'mouseup'
		exclude: null, // selector or jQuery/DOM element or array of selectors/elements
		handler: null, // function (event, $targetElement) {}
		once: false, // auto-destroy after first invocation
		capture: false, // not used here (DOM capture) — kept for API symmetry
		stopPropagation: false, // if true, plugin will stop event propagation after calling handler
		allowRightClick: false, // if false, ignore right-clicks (event.which === 3)
	};

	function normalizeExcludes(ex) {
		if (!ex) return null;
		if (Array.isArray(ex)) return ex;
		return [ex];
	}

	function isExcluded(eventTarget, $el, excludes) {
		if (!excludes) return false;
		for (var i = 0; i < excludes.length; i++) {
			var e = excludes[i];
			if (!e) continue;
			if (typeof e === "string") {
				if ($(eventTarget).closest(e).length) return true;
			} else {
				// DOM element or jQuery
				var $e = $(e);
				if (($e.length && $e.is(eventTarget)) || $e.has(eventTarget).length)
					return true;
				// Also if target is inside the excluded element
				if ($.contains($e[0], eventTarget)) return true;
			}
		}
		// also exclude clicks inside the plugin element itself
		if ($el.length && ($el.is(eventTarget) || $el.has(eventTarget).length))
			return true;
		return false;
	}

	// Main plugin function
	$.fn[pluginName] = function (optionOrMethod) {
		var args = Array.prototype.slice.call(arguments, 1);

		// method calls like .clickOutside('destroy')
		if (typeof optionOrMethod === "string") {
			var method = optionOrMethod;
			if (method === "destroy") {
				return this.each(function () {
					var $this = $(this);
					var data = $this.data(pluginName);
					if (!data) return;
					doc.off(data._eventNamespace);
					$this.removeData(pluginName);
				});
			} else if (method === "disable") {
				return this.each(function () {
					var $this = $(this);
					var data = $this.data(pluginName);
					if (data) data._enabled = false;
				});
			} else if (method === "enable") {
				return this.each(function () {
					var $this = $(this);
					var data = $this.data(pluginName);
					if (data) data._enabled = true;
				});
			} else if (method === "update") {
				// update options
				return this.each(function () {
					var $this = $(this);
					var data = $this.data(pluginName);
					if (!data) return;
					var newOpts = args[0] || {};
					data.options = $.extend({}, data.options, newOpts);
					data.options._excludes = normalizeExcludes(data.options.exclude);
					$this.data(pluginName, data);
				});
			} else {
				$.error("Unknown method " + method);
			}
		}

		// initialization
		var options = $.extend({}, defaults, optionOrMethod || {});

		return this.each(function () {
			var $el = $(this);
			var existing = $el.data(pluginName);
			if (existing) {
				// update handler & options
				existing.options = $.extend({}, existing.options, options);
				existing.options._excludes = normalizeExcludes(
					existing.options.exclude,
				);
				$el.data(pluginName, existing);
				return;
			}

			var ns = "." + pluginName + "-" + Math.random().toString(36).substr(2, 8);
			var eventType = options.event || defaults.event;
			var eventName = eventType + ns;
			var data = {
				options: $.extend({}, options),
				_eventNamespace: eventName,
				_enabled: true,
			};
			data.options._excludes = normalizeExcludes(data.options.exclude);

			function docHandler(e) {
				if (!data._enabled) return;
				// ignore right click if not allowed
				if (!data.options.allowRightClick && e.which === 3) return;

				// If target is excluded OR inside the element, do nothing
				if (isExcluded(e.target, $el, data.options._excludes)) return;

				// call the user handler
				if (typeof data.options.handler === "function") {
					try {
						data.options.handler.call($el[0], e, $el);
					} catch (err) {
						// swallow errors but log
						window.console &&
							console.error &&
							console.error("clickOutside handler error:", err);
					}
				}

				if (data.options.stopPropagation) {
					e.stopPropagation && e.stopPropagation();
				}

				if (data.options.once) {
					// auto destroy after first call
					doc.off(eventName);
					$el.removeData(pluginName);
				}
			}

			doc.on(eventName, docHandler);

			$el.data(pluginName, data);
		});
	};

	// Convenience shortcut on document: trigger handler manually for debugging/testing
	$.fn[pluginName].defaults = defaults;
})(jQuery);

// Usage example:
//
// $('#menu').clickOutside({
//   event: 'mousedown',
//   handler: function (ev, $el) {
//     $(this).hide();
//   },
//   exclude: '#toggle' // clicking the toggle won't close the menu
// });

/**
 * ===============================================================
 * jQuery ScrollIntoView Plugin
 * Smoothly scrolls an element into view with customizable options
 * ===============================================================
 */
(function ($) {
	$.fn.scrollIntoView = function (options) {
		// Default settings
		const settings = $.extend(
			{
				duration: 500, // Animation duration in ms
				offset: 0, // Additional offset from top (can be negative)
				easing: "swing", // jQuery easing function
				container: null, // Scrollable container (null = window)
				align: "top", // 'top', 'center', or 'bottom'
				callback: null, // Function to call after scroll completes
				onlyIfNeeded: false, // Only scroll if element is not already visible
			},
			options,
		);

		return this.each(function () {
			const $el = $(this);
			const $container = settings.container
				? $(settings.container)
				: $("html, body");
			// Calculate element position
			let elementTop = $el.offset().top;
			let containerScrollTop = settings.container
				? $(settings.container).scrollTop()
				: $(window).scrollTop();

			// If using a custom container, adjust the calculation
			if (settings.container && $(settings.container).offset()) {
				const containerTop = $(settings.container).offset().top;
				elementTop = elementTop - containerTop + containerScrollTop;
			}

			// Calculate scroll position based on alignment
			let scrollTo = elementTop;
			const viewportHeight = settings.container
				? $(settings.container).height()
				: $(window).height();
			const elementHeight = $el.outerHeight();

			switch (settings.align) {
				case "center":
					scrollTo = elementTop - viewportHeight / 2 + elementHeight / 2;
					break;
				case "bottom":
					scrollTo = elementTop - viewportHeight + elementHeight;
					break;
				default: // 'top'
					scrollTo = elementTop;
			}

			// Apply offset
			scrollTo += settings.offset;

			// Check if scroll is needed
			if (settings.onlyIfNeeded) {
				const currentScroll = containerScrollTop;
				const elementBottom = elementTop + elementHeight;
				const viewportBottom = currentScroll + viewportHeight;

				// Element is already fully visible
				if (elementTop >= currentScroll && elementBottom <= viewportBottom) {
					if (settings.callback) settings.callback.call($el[0]);
					return;
				}
			}

			// Perform the scroll animation
			$container.animate(
				{
					scrollTop: scrollTo,
				},
				settings.duration,
				settings.easing,
				function () {
					if (settings.callback) {
						settings.callback.call($el[0]);
					}
				},
			);
		});
	};
})(jQuery);

// Usage Examples:
//
// Basic usage:
// $('#myElement').scrollIntoView();
//
// With options:
// $('#myElement').scrollIntoView({
//   duration: 800,
//   offset: -50,
//   align: 'center'
// });
//
// Scroll within a container:
// $('#myElement').scrollIntoView({
//   container: '#scrollableDiv',
//   duration: 600
// });
//
// Only scroll if needed:
// $('#myElement').scrollIntoView({
//   onlyIfNeeded: true,
//   callback: function() {
//     console.log('Scrolled to element');
//   }
// });

getDropdownParent = function (theinput) {
	var dropdownParent = theinput.data("dropdownparent");
	if (dropdownParent && $("#" + dropdownParent).length) {
		return $("#" + dropdownParent);
	}

	var parent = theinput.closest("#main-media-container");
	if (parent.length) {
		return parent;
	}
	if (theinput.data("searchtype")) {
		var parent = $(".detail-" + theinput.data("searchtype"));
		if (parent.length) {
			return parent;
		}
	}

	var inmodal = theinput.closest(".modal");
	if (inmodal.length) {
		if (theinput.closest("form").length) {
			return theinput.closest("form");
		}
		return inmodal.find(".modal-body");
	}

	return undefined;
};

(function ($) {
	$.fn.autoGrow = function (options) {
		var settings = $.extend(
			{
				minHeight: null,
				maxHeight: null,
				extraSpace: 0,
			},
			options,
		);

		return this.each(function () {
			var $textarea = $(this);
			var offset = this.offsetHeight - this.clientHeight;

			var originalRows = $textarea.attr("rows") || 1;

			var minHeight = settings.minHeight;
			if (minHeight === null && originalRows) {
				var lineHeight = parseInt($textarea.css("line-height"));
				var padding =
					parseInt($textarea.css("padding-top")) +
					parseInt($textarea.css("padding-bottom"));
				minHeight = lineHeight * originalRows + padding;
			}

			var resize = function () {
				$textarea.css("height", "auto");

				var newHeight = this.scrollHeight + offset + settings.extraSpace;

				if (minHeight && newHeight < minHeight) {
					newHeight = minHeight;
				}

				if (settings.maxHeight && newHeight > settings.maxHeight) {
					newHeight = settings.maxHeight;
					$textarea.css("overflow-y", "auto");
				} else {
					$textarea.css("overflow-y", "hidden");
				}

				$textarea.css("height", newHeight + "px");
			};

			$textarea
				.on("input change cut paste drop keydown", resize)
				.on("focus", resize);

			// to ensure DOM is fully ready
			setTimeout(function () {
				resize.call($textarea[0]);
			}, 0);
		});
	};
})(jQuery);

$(document).ready(function () {
	lQuery("textarea.form-control").livequery(function () {
		var minHeight = $(this).data("minheight") || null;
		var maxHeight = $(this).data("maxheight") || null;
		var extraSpace = $(this).data("extraspace") || 0;
		$(this).autoGrow({
			minHeight: minHeight,
			maxHeight: maxHeight,
			extraSpace: extraSpace,
		});
	});
});
