$.ajaxSetup({
	xhrFields: {
		withCredentials: true,
	},
	crossDomain: true,
	beforeSend: function (xhr) {
		xhr.setRequestHeader(
			"X-TimeZone",
			Intl.DateTimeFormat().resolvedOptions().timeZone,
		);
	},
});

(function ($) {
	$.fn.cleandata = function () {
		var element = $(this);
		var params = element.data();

		if (params === undefined) {
			console.log("Element not found", element);
			return;
		}

		var cleaned = {};
		var obj = Object.keys(params);

		obj.forEach(function (key) {
			var param = params[key];
			if (param !== undefined) {
				var thetype = typeof param;
				if (
					thetype === "string" ||
					thetype === "number" ||
					thetype === "boolean"
				) {
					cleaned[key] = param;
				}
			}
		});
		return cleaned;
	};

	var oldLoad = $.fn.load;
	$.fn.load = function (inArg, maybeData, inComplete) {
		var oldscope = this;
		if (typeof maybeData != "object") {
			inComplete = maybeData;
			maybeData = {};
		}
		var returned = oldLoad.call(oldscope, inArg, maybeData, function () {
			if (typeof inComplete == "function") {
				// They passed in parameters
				inComplete.call(this);
			}
			//console.log("html complete");
			$(document).trigger("domchanged", [$(oldscope).parent()]); //Child got replaced
		});
		return returned;
	};

	var oldhtml = $.fn.html;
	$.fn.html = function (arg) {
		if (arguments.length == 0) {
			var returned = oldhtml.call($(this));
			return returned;
		}

		var returned = oldhtml.call($(this), arg);
		$(document).trigger("domchanged", [$(this)]); //a component may be adding html that will call this
		return returned;
	};

	var oldreplaceWith = $.fn.replaceWith;
	$.fn.replaceWith = function (arg) {
		var parent = $(this).parent();
		var returned = oldreplaceWith.call($(this), arg);
		$(document).trigger("domchanged", [parent]);
		return returned;
	};

	var oldappend = $.fn.append;
	$.fn.append = function (arg) {
		var div = $(this);
		var returned = oldappend.call(div, arg);
		$(document).trigger("domchanged", [div]);
		return returned;
	};

	var oldinsertbefore = $.fn.insertBefore;
	$.fn.insertBefore = function (arg) {
		var div = $(this);
		var returned = oldinsertbefore.call(div, arg);
		$(document).trigger("domchanged", [div.parent()]);
		return returned;
	};

	var oldinsertafter = $.fn.insertAfter;
	$.fn.insertAfter = function (arg) {
		var div = $(this);
		var returned = oldinsertafter.call(div, arg);
		$(document).trigger("domchanged", [div.parent()]);
		return returned;
	};

	var oldajaxSubmit = $.fn.ajaxSubmit;
	$.fn.ajaxSubmit = function () {
		var form = $(this);

		var targetdiv = form.data("targetdiv") || form.data("targetdivinner");
		if (targetdiv) {
			var oemaxlevel = form.data("oemaxlevel");
			if (!oemaxlevel) {
				oemaxlevel = $("#" + targetdiv).data("oemaxlevel");
			}
			if (!oemaxlevel) {
				oemaxlevel = 1;
			}

			form.append(
				$(`<input type='hidden' name='oemaxlevel' value='${oemaxlevel}'>`),
			);
		}

		var params = arguments[0];
		var oldsucess = params.success;
		params.success = function (arg1, arg2, arg3, arg4) {
			//var oldcontent = $(targetdiv);
			//oldcontent.html("");

			if (oldsucess != null) {
				oldsucess.call(form, arg1, arg2, arg3, arg4);
			}
			//Grab target div? 		$(document).trigger("domchanged",null,$(this));
			if (targetdiv) {
				$(document).trigger("domchanged", [$("#" + targetdiv)]);
			} else {
				$(document).trigger("domchanged");
			}
		};
		var returned = oldajaxSubmit.call(form, params);
		params.success = oldsucess;
		return returned;
	};
})(jQuery);

(function ($) {
	var regelements = new Array();
	var eventregistry = new Array();
	//Listener

	$(document).on("domchanged", function (event, args) {
		// 		if( livequeryrunning && args == null )
		// 		{
		//console.log("Skipping reload" , args);
		//return;
		// 		}
		var element;
		if (typeof args == Array) {
			if (args.length > 1) {
				element = $(args[0], args[1]);
			}
		} else if (args != null) {
			element = args;
		}

		if (element == null) {
			element = document;
		}
		//console.log("domchanged reload on ",element);
		$.each(regelements, function () {
			//Everyone
			var item = this;
			var funct = item.function;
			$(item.selector, element).each(function () {
				try {
					var node = $(this);
					if (node.data("livequeryinit" + item.selector) == null) {
						//console.log("Not enabled: " + item.selector );
						node.data("livequeryinit" + item.selector, true);
						funct.call(node);
					}
				} catch (error) {
					console.log("Could not process: " + item.selector, error);
				}
			});
		});
		//TODO: Loop over events ones and register them
		$.each(eventregistry, function () {
			var listener = this;

			var check = element;

			if (String(listener.selector).indexOf(" ") > -1) {
				check = document;
			}

			$(listener.selector, check).each(function () {
				var node = $(this);

				if (node.data("livequery" + listener.selector) == null) {
					//console.log("Registering " + listener.selector );
					node.on(listener.event, listener.function);
				} else {
					//console.log("already Registered:	 ");
					//console.log(node);
				}
			});
		});
		//We need to do this as the end in case there are more than one click handlers on the same node
		$.each(eventregistry, function () {
			var listener = this;
			var check = element;
			if (String(listener.selector).indexOf(" ") > -1) {
				check = document;
			}

			$(listener.selector, check).each(function () {
				var node = $(this);
				if (node.data("livequery" + listener.selector) == null) {
					node.data("livequery" + listener.selector, true);
				}
			});
		});
	}); //document.on

	lQuery = function (selector) {
		if (selector == undefined || selector == null) {
			console.log("lQuery called with undefined selector");
			return;
		}
		var runner = {};
		runner.livequery = function () {
			var nodes = jQuery(selector);
			if (arguments.length == 1) {
				var func = arguments[0];
				var item = { selector: selector, function: func };
				/*
        if( selector.startsWith("#"))
        {
			regelements = $.grep(regelements, function (el, index) {
				 
			  	if( el.selector == selector)
			  	{
			        return false;
			    }
			    return true; // keep the element in the array
			});
		}
		*/
				regelements.push(item);
				try {
					nodes.each(function () {
						//We need to make sure each row is initially handled
						var onerow = $(this);
						onerow.data("livequeryinit" + selector, true);
						func.call(onerow);
					});
				} catch (error) {
					console.log("Could not process: " + selector, error);
				}
			} //Note: on does not support scope of selectors
			else {
				var eventtype = arguments[0]; //click
				var eventlistener = { selector: selector, event: eventtype };

				if (arguments.length == 2) {
					eventlistener["function"] = arguments[1];
					eventlistener["scope"] = document;
				} else {
					eventlistener["scope"] = arguments[1];
					eventlistener["function"] = arguments[2];
				}
				eventregistry.push(eventlistener);
				//console.log("Initial Registering  event" + eventlistener.selector );

				nodes.each(function () {
					//We need to make sure each row is initially handled
					var node = $(this);
					node.data("livequery" + selector, true);
					node.on(eventlistener.event, eventlistener.function);
					//$(document).on(eventlistener.event,eventlistener.selector,eventlistener.function);
				});
			}
			return this;
		};
		return runner;
	};
})(jQuery);
