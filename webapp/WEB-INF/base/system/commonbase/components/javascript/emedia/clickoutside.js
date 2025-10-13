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
					existing.options.exclude
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

// example usage:
/*
$('#menu').clickOutside({
  event: 'mousedown',
  handler: function (ev, $el) {
    $(this).hide();
  },
  exclude: '#toggle' // clicking the toggle won't close the menu
});
 */
