(function ($) {
	$.fn.jsonCodeEditor = function (options) {
		var settings = $.extend(
			{
				mode: "application/json",
				defaultTheme: "yeti",
				darkTheme: "dracula",
				lightTheme: "yeti",
				lineNumbers: true,
				lineWrapping: true,
				indentUnit: 2,
				tabSize: 2,
				indentWithTabs: false,
				matchBrackets: true,
				autoCloseBrackets: true,
				styleActiveLine: true,
				highlightSelectionMatches: { showToken: /\w/, annotateScrollbar: true },
				themeStorageKey: "code-theme",
				selectors: {
					controls: ".controls",
					textArea: "textarea",
					themeToggle: ".theme-toggle",
					status: ".status",
					errorMessage: ".error-message",
					formatButton: ".formatJSON",
					minifyButton: ".minifyJSON",
					copyButton: ".copyJSON",
				},
			},
			options,
		);

		var container = $(this);
		var controls = container.find(settings.selectors.controls);
		var textArea = container.find(settings.selectors.textArea)[0];
		var themeToggleBtn = container.find(settings.selectors.themeToggle);

		if (!textArea || typeof CodeMirror === "undefined") {
			return;
		}

		var existingInstance = container.data("jsonCodeEditor");
		if (existingInstance && existingInstance.editor) {
			existingInstance.editor.toTextArea();
		}
		container.removeData("jsonCodeEditor");

		var editor = CodeMirror.fromTextArea(textArea, {
			mode: settings.mode,
			theme: settings.defaultTheme,
			lineNumbers: settings.lineNumbers,
			lineWrapping: settings.lineWrapping,
			indentUnit: settings.indentUnit,
			tabSize: settings.tabSize,
			indentWithTabs: settings.indentWithTabs,
			matchBrackets: settings.matchBrackets,
			autoCloseBrackets: settings.autoCloseBrackets,
			styleActiveLine: settings.styleActiveLine,
			highlightSelectionMatches: settings.highlightSelectionMatches,
		});

		controls.show();

		function validateJSON() {
			var content = editor.getValue();
			var status = container.find(settings.selectors.status);
			var errorMsg = container.find(settings.selectors.errorMessage);

			if (!content.trim()) {
				status.html("<i class='bi bi-check-circle'></i>");
				status.removeClass("invalid");
				errorMsg.removeClass("show");
				return;
			}

			try {
				JSON.parse(content);
				status.html("<i class='bi bi-check-circle'></i>");
				status.removeClass("invalid");
				errorMsg.removeClass("show");
				textArea.value = content;
			} catch (e) {
				status.html("<i class='bi bi-exclamation-circle'></i>");
				status.addClass("invalid");
				errorMsg.text("Error: " + e.message);
				errorMsg.addClass("show");
			}
		}

		function showError(message) {
			var errorMsg = container.find(settings.selectors.errorMessage);
			errorMsg.text(message);
			errorMsg.addClass("show");
		}

		editor.on("change", validateJSON);
		container.data("jsonCodeEditor", { editor: editor });

		themeToggleBtn
			.off("click.jsonCodeEditor")
			.on("click.jsonCodeEditor", function () {
				var isDark = container.hasClass("light-theme");

				if (isDark) {
					container.removeClass("light-theme");
					container.addClass("dark-theme");
					editor.setOption("theme", settings.darkTheme);
					themeToggleBtn.html("<i class='bi bi-moon'></i>");
					localStorage.setItem(settings.themeStorageKey, "dark");
				} else {
					container.addClass("light-theme");
					container.removeClass("dark-theme");
					editor.setOption("theme", settings.lightTheme);
					themeToggleBtn.html("<i class='bi bi-sun'></i>");
					localStorage.setItem(settings.themeStorageKey, "light");
				}
			});

		var savedTheme =
			localStorage.getItem(settings.themeStorageKey) || settings.defaultTheme;
		if (savedTheme === "light") {
			container.addClass("light-theme");
			container.removeClass("dark-theme");
			editor.setOption("theme", settings.lightTheme);
			themeToggleBtn.html("<i class='bi bi-sun'></i>");
		} else {
			container.removeClass("light-theme");
			container.addClass("dark-theme");
			editor.setOption("theme", settings.darkTheme);
			themeToggleBtn.html("<i class='bi bi-moon'></i>");
		}

		container
			.find(settings.selectors.formatButton)
			.off("click.jsonCodeEditor")
			.on("click.jsonCodeEditor", function () {
				try {
					var content = editor.getValue();
					var parsed = JSON.parse(content);
					var formatted = JSON.stringify(parsed, null, settings.indentUnit);
					editor.setValue(formatted);
					validateJSON();
				} catch (e) {
					showError("Cannot format: " + e.message);
				}
			});

		container
			.find(settings.selectors.minifyButton)
			.off("click.jsonCodeEditor")
			.on("click.jsonCodeEditor", function () {
				try {
					var content = editor.getValue();
					var parsed = JSON.parse(content);
					var minified = JSON.stringify(parsed);
					editor.setValue(minified);
					validateJSON();
				} catch (e) {
					showError("Cannot minify: " + e.message);
				}
			});

		container
			.find(settings.selectors.copyButton)
			.off("click.jsonCodeEditor")
			.on("click.jsonCodeEditor", function () {
				var content = editor.getValue();
				try {
					window.navigator.clipboard.writeText(content);
				} catch (_) {
					$(textArea).show().focus().select();
					try {
						document.execCommand("copy");
					} catch (err) {
						showError("Failed to copy: " + err);
					} finally {
						$(textArea).hide();
					}
				}
			});
	};
})(jQuery);
