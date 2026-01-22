import {
	Alignment,
	AutoImage,
	AutoLink,
	Autosave,
	BalloonToolbar,
	Bold,
	ButtonView,
	ClassicEditor,
	CloudServices,
	CodeBlock,
	Essentials,
	FileRepository,
	GeneralHtmlSupport,
	Heading,
	ImageBlock,
	ImageUpload,
	ImageInline,
	ImageInsert,
	ImageInsertViaUrl,
	ImageResize,
	ImageStyle,
	ImageToolbar,
	Indent,
	IndentBlock,
	InlineEditor,
	Italic,
	Link,
	List,
	Paragraph,
	Plugin,
	RemoveFormat,
	SourceEditing,
	Strikethrough,
	Underline,
	Clipboard,
	Image,
	DragDrop,
	Table,
	TableToolbar,
} from "ckeditor5";

import prettifyHTML from "prettyhtml";

function disposeCKEditor(editor, callback = null) {
	if (!editor) {
		if (callback) callback();
		return;
	}
	const uid = editor.sourceElement.id;
	if (window.CK5Editor[uid]) {
		delete window.CK5Editor[uid];
	}
	if (window.CK5EditorInline[uid]) {
		delete window.CK5EditorInline[uid];
	}
	$(editor.sourceElement).data("ck5Initialized", false);
	editor.sourceElement = null;
	editor.destroy().then(function () {
		if (callback) callback();
	});
}

class SaveButtonPlugin extends Plugin {
	init() {
		const editor = this.editor;
		editor.ui.componentFactory.add("saveButton", () => {
			const button = new ButtonView();

			button.set({
				label: "Save",
				icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" fill="red"><path d="M433.9 129.9l-83.9-83.9A48 48 0 0 0 316.1 32H48C21.5 32 0 53.5 0 80v352c0 26.5 21.5 48 48 48h352c26.5 0 48-21.5 48-48V163.9a48 48 0 0 0 -14.1-33.9zM224 416c-35.3 0-64-28.7-64-64 0-35.3 28.7-64 64-64s64 28.7 64 64c0 35.3-28.7 64-64 64zm96-304.5V212c0 6.6-5.4 12-12 12H76c-6.6 0-12-5.4-12-12V108c0-6.6 5.4-12 12-12h228.5c3.2 0 6.2 1.3 8.5 3.5l3.5 3.5A12 12 0 0 1 320 111.5z"/></svg>',
				keystroke: "Ctrl+S",
				class: "text-primary",
			});

			button.on("execute", () => {
				const data = $(editor.sourceElement).data();
				const savePath = $(editor.sourceElement).data("savepath");
				const editPath = $(editor.sourceElement).data("editpath");
				const keepEditor = $(editor.sourceElement).data("keepeditor");

				let content = editor.getData();
				if (content) content = prettifyHTML(editor.getData());
				$(editor.sourceElement).val(content);
				$.ajax({
					url: savePath,
					type: "POST",
					data: {
						content,
						editPath,
						...data,
					},
					success: function () {
						if (!keepEditor) {
							editor.updateSourceElement();
							disposeCKEditor(editor);
						}
					},
					error: function () {
						alert("Error");
					},
				});
			});

			return button;
		});
	}
}

class CloseButtonPlugin extends Plugin {
	init() {
		const editor = this.editor;
		editor.ui.componentFactory.add("closeButton", () => {
			const button = new ButtonView();

			button.set({
				label: "Close",
				icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M256 8C119 8 8 119 8 256s111 248 248 248 248-111 248-248S393 8 256 8zm121.6 313.1c4.7 4.7 4.7 12.3 0 17L338 377.6c-4.7 4.7-12.3 4.7-17 0L256 312l-65.1 65.6c-4.7 4.7-12.3 4.7-17 0L134.4 338c-4.7-4.7-4.7-12.3 0-17l65.6-65-65.6-65.1c-4.7-4.7-4.7-12.3 0-17l39.6-39.6c4.7-4.7 12.3-4.7 17 0l65 65.7 65.1-65.6c4.7-4.7 12.3-4.7 17 0l39.6 39.6c4.7 4.7 4.7 12.3 0 17L312 256l65.6 65.1z"/></svg>',
				class: "ck-close-button",
				keystroke: "ctrl+esc",
			});

			button.on("execute", () => {
				editor.sourceElement.style.display = "block";
				disposeCKEditor(editor);
			});
			return button;
		});
	}
}

class ImagePicker extends Plugin {
	init() {
		const editor = this.editor;
		editor.ui.componentFactory.add("imagePicker", () => {
			const button = new ButtonView();

			button.set({
				label: "Image Picker",
				icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M1.201 1c-.662 0-1.2.47-1.2 1.1v14.248c0 .64.533 1.152 1.185 1.152h6.623v-7.236L6.617 9.15a.694.694 0 0 0-.957-.033L1.602 13.55V2.553l14.798.003V9.7H18V2.1c0-.63-.547-1.1-1.2-1.1H1.202Zm11.723 2.805a2.094 2.094 0 0 0-1.621.832 2.127 2.127 0 0 0 1.136 3.357 2.13 2.13 0 0 0 2.611-1.506 2.133 2.133 0 0 0-.76-2.244 2.13 2.13 0 0 0-1.366-.44Z"></path><path clip-rule="evenodd" d="M19.898 12.369v6.187a.844.844 0 0 1-.844.844h-8.719a.844.844 0 0 1-.843-.844v-7.312a.844.844 0 0 1 .843-.844h2.531a.843.843 0 0 1 .597.248l.838.852h4.75c.223 0 .441.114.6.272a.844.844 0 0 1 .247.597Zm-1.52.654-4.377.02-1.1-1.143H11v6h7.4l-.023-4.877Z"></path></svg>',
				class: "ck-image-picker-button",
			});

			button.on("execute", () => {
				//TODO: Open image picker dialog
				let findRoot = $("#application").data("findroot");
				if (!findRoot) {
					const siteRoot = $("#application").data("siteroot");
					findRoot = `${siteRoot}/blockfind`;
				}
				const anchor = document.createElement("a");
				anchor.id = "dialogpickerassetpicker";
				anchor.href = findRoot + "/blockiframe.html?targetfieldid=htmleditor";
				anchor.classList.add("emdialog");
				document.body.appendChild(anchor);
				$(anchor).emDialog();
				anchor.remove();
			});

			return button;
		});
	}
}

class UploadFileButtonPlugin extends Plugin {
	init() {
		const editor = this.editor;
		editor.ui.componentFactory.add("uploadFile", (locale) => {
			const view = new ButtonView(locale);

			view.set({
				label: "Upload File",
				withText: true,
				tooltip: true,
			});

			view.on("execute", () => {
				const input = document.createElement("input");
				input.type = "file";
				input.multiple = false;
				input.accept = "*/*";
				input.style.display = "none";

				input.addEventListener("change", async () => {
					if (input.files.length > 0) {
						const file = input.files[0];
						const adapter = new EmUploadAdapter(
							file,
							editor,
							editor.sourceElement.dataset.uploadUrl,
						);

						try {
							// Use uploadAndReturnModelFragment for custom HTML insertion
							const modelFragment =
								await adapter.uploadAndReturnModelFragment();
							editor.model.insertContent(modelFragment);
						} catch (e) {
							console.error("Upload failed", e);
						}
					}
				});

				document.body.appendChild(input);
				input.click();
				document.body.removeChild(input);
			});

			return view;
		});
	}
}

class EmUploadAdapter {
	constructor(fileOrLoader, editor, uploadUrl) {
		// Detect whether we are receiving a File or a Loader
		if (fileOrLoader && typeof fileOrLoader.read === "function") {
			// It's a loader (drag and drop, paste)
			this.loader = fileOrLoader;
		} else {
			// It's a raw file (manual upload button)
			this.file = fileOrLoader;
		}

		this.editor = editor;
		this.uploadUrl = uploadUrl;
		this.xhr = null;
	}

	async uploadAndReturnModelFragment() {
		const file = this.file || (await this.loader.file);

		return new Promise((resolve, reject) => {
			const formData = new FormData();
			formData.append("upload", file);

			$.ajax({
				url: this.uploadUrl,
				type: "POST",
				data: formData,
				dataType: "json",
				processData: false,
				contentType: false,
				success: (response) => {
					if (!response || response.error || !response.html) {
						return reject(response?.error?.message || "Upload failed");
					}

					const viewFragment = this.editor.data.processor.toView(response.html);
					const modelFragment = this.editor.data.toModel(viewFragment);
					resolve(modelFragment);
				},
				error: (_, __, errorThrown) => reject(errorThrown || "Upload failed"),
			});
		});
	}

	upload() {
		return this.loader.file.then(
			(file) =>
				new Promise((resolve, reject) => {
					const formData = new FormData();
					formData.append("upload", file);

					$.ajax({
						url: this.uploadUrl,
						type: "POST",
						data: formData,
						dataType: "json",
						processData: false,
						contentType: false,
						xhr: () => {
							const xhr = new window.XMLHttpRequest();
							this.xhr = xhr;
							xhr.upload.addEventListener("progress", (evt) => {
								if (evt.lengthComputable) {
									this.loader.uploadTotal = evt.total;
									this.loader.uploaded = evt.loaded;
								}
							});
							return xhr;
						},
						success: (response) => {
							if (!response || response.error) {
								return reject(response?.error?.message || "Upload failed");
							}

							//  Insert the raw HTML into the editor manually
							const viewFragment = this.editor.data.processor.toView(
								response.html,
							);
							const modelFragment = this.editor.data.toModel(viewFragment);
							this.editor.model.insertContent(modelFragment);

							// Resolve without passing url
							resolve({ default: "" });
						},
						error: (jqXHR, textStatus, errorThrown) => {
							reject(errorThrown || textStatus);
						},
					});
				}),
		);
	}

	abort() {
		if (this.xhr) {
			this.xhr.abort();
		}
	}
}

function EmUploadAdapterPlugin(editor) {
	editor.plugins.get("FileRepository").createUploadAdapter = (loader) => {
		const sourceElement = editor.sourceElement;
		let uploadUrl = sourceElement?.dataset?.uploadUrl;

		if (!uploadUrl) {
			const fallback = $(sourceElement).closest("[data-ckeditor-upload-url]");
			if (fallback.length) {
				uploadUrl = fallback.data("ckeditor-upload-url");
			}
		}

		return new EmUploadAdapter(loader, editor, uploadUrl);
	};
}

const LICENSE_KEY = "GPL";

const editorConfig = (options, isInline = false) => {
	let items = [
		"closeButton",
		"saveButton",
		"|",
		"heading",
		"|",
		"bold",
		"italic",
		"underline",
		"strikethrough",
		"removeFormat",
		"|",
		"imagePicker",
		"|",
		"link",
		"|",
		"alignment",
		"|",
		"bulletedList",
		"numberedList",
		"outdent",
		"indent",
		"|",
		"codeBlock",
		"|",
		"undo",
		"redo",
		"|",
		"insertTable",

		"sourceEditing",
	];

	const plugins = [
		Alignment,
		AutoImage,
		AutoLink,
		Autosave,
		BalloonToolbar,
		Bold,
		CloudServices,
		Essentials,
		FileRepository,
		EmUploadAdapterPlugin,
		UploadFileButtonPlugin,
		Heading,
		GeneralHtmlSupport,
		Indent,
		IndentBlock,
		Italic,
		Link,
		List,
		Paragraph,
		RemoveFormat,
		Strikethrough,
		Underline,
		CodeBlock,
		ImageUpload,
		Table,
		TableToolbar,
	];

	let image = undefined;

	if (options.hideSaving) {
		items = items.slice(3);
	} else {
		plugins.push(CloseButtonPlugin, SaveButtonPlugin);
	}

	const sourceElement = options.sourceElement;
	const uploadUrl = sourceElement?.dataset?.uploadUrl;

	if (uploadUrl) {
		// Only show upload button if upload URL is provided
		items.splice(items.indexOf("uploadFile"), 0, "uploadFile");
		plugins.push(UploadFileButtonPlugin);
		plugins.push(ImageUpload);
		plugins.push(Clipboard);
		plugins.push(Image);
		plugins.push(DragDrop);
		plugins.push(
			ImagePicker,
			ImageBlock,
			ImageInline,
			ImageInsert,
			ImageInsertViaUrl,
			ImageResize,
			ImageStyle,
			ImageToolbar,
			ImageUpload,
		);
		image = {
			toolbar: [
				"imageTextAlternative",
				"|",
				"imageStyle:inline",
				"imageStyle:wrapText",
				"imageStyle:breakText",
				"|",
				"resizeImage",
			],
		};
	}

	if (options.hideImagePicker) {
		const idx = items.indexOf("imagePicker");
		items.splice(idx, 2);
	} else {
		plugins.push(
			ImagePicker,
			ImageBlock,
			ImageInline,
			ImageInsert,
			ImageInsertViaUrl,
			ImageResize,
			ImageStyle,
			ImageToolbar,
			ImageUpload,
		);

		image = {
			toolbar: [
				"imageTextAlternative",
				"|",
				"imageStyle:inline",
				"imageStyle:wrapText",
				"imageStyle:breakText",
				"|",
				"resizeImage",
			],
		};
	}

	if (options.hideSourceEditing) {
		items.splice(-2);
	} else {
		plugins.push(SourceEditing);
	}
	const htmlSupportConfig = {
		htmlSupport: {
			allowEmpty: ["i"],
			allow: [
				{
					name: /.*/, // allow all elements
					attributes: true, // allow all attributes
					classes: true, // allow all classes
					styles: true, // allow all styles
				},
			],
		},
	};

	if (isInline) {
		plugins.push(GeneralHtmlSupport);
	}

	return {
		updateSourceElementOnDestroy: true,
		toolbar: {
			items,
			shouldNotGroupWhenFull: false,
		},
		plugins,
		image,
		balloonToolbar: [
			"bold",
			"italic",
			"underline",
			"|",
			"link",
			"|",
			"bulletedList",
			"numberedList",
		],
		heading: {
			options: [
				{
					model: "paragraph",
					title: "Paragraph",
					class: "ck-heading_paragraph",
				},
				{
					model: "heading1",
					view: "h1",
					title: "Heading 1",
					class: "ck-heading_heading1",
				},
				{
					model: "heading2",
					view: "h2",
					title: "Heading 2",
					class: "ck-heading_heading2",
				},
				{
					model: "heading3",
					view: "h3",
					title: "Heading 3",
					class: "ck-heading_heading3",
				},
				{
					model: "heading4",
					view: "h4",
					title: "Heading 4",
					class: "ck-heading_heading4",
				},
				{
					model: "heading5",
					view: "h5",
					title: "Heading 5",
					class: "ck-heading_heading5",
				},
				{
					model: "heading6",
					view: "h6",
					title: "Heading 6",
					class: "ck-heading_heading6",
				},
			],
		},
		codeBlock: {
			languages: [
				{ language: "plaintext", label: "Plain text" },
				{ language: "html", label: "HTML" },
				{ language: "css", label: "CSS" },
				{ language: "javascript", label: "JavaScript" },
				{ language: "java", label: "Java" },
				{ language: "xml", label: "XML" },
				{ language: "json", label: "JSON" },
				{ language: "bash", label: "Bash" },
			],
		},
		licenseKey: LICENSE_KEY,
		link: {
			addTargetToExternalLinks: true,
			defaultProtocol: "https://",
			decorators: {
				toggleDownloadable: {
					mode: "manual",
					label: "Downloadable",
					attributes: {
						download: "file",
					},
				},
			},
		},
		placeholder: "Type or paste your content here!",
		...htmlSupportConfig,
	};
};

window.CK5Editor = {};
window.CK5EditorInline = {};

function createCK5(target, options = {}) {
	let uid = target.id;
	if (!uid) {
		uid = "ck5editor" + Object.keys(window.CK5Editor).length;
		target.id = uid;
	}
	ClassicEditor.create(target, editorConfig(options))
		.then((editor) => {
			window.CK5Editor[uid] = editor;
			if (!options.hideImagePicker) {
				$(window).on("assetpicked", function (_, params) {
					const imageUrl = params.assetpicked;
					setTimeout(() => {
						editor.execute("imageInsert", { source: imageUrl });
					});
				});
			}

			//If CKEditor is inside a modal, we need to remove the tabindex attribute from the modal to make the link popup focusable
			const modal = $(target).closest(".modal");
			if (modal.length > 0) {
				modal.attr("tabindex", "");
			}
		})
		.catch((error) => {
			console.error(error);
		});
}

$(window).on("edithtmlstart", function (_, targetDiv) {
	if (targetDiv.length === 0) return;
	const hideSaving = targetDiv.data("editonly");
	const hideImagePicker = targetDiv.data("imagepickerhidden");
	const options = {
		hideImagePicker,
		hideSaving,
		sourceElement: targetDiv[0],
	};
	const uid = targetDiv[0].id;
	if (uid && window.CK5Editor[uid]) {
		disposeCKEditor(window.CK5Editor[uid], () => {
			createCK5(targetDiv[0], options);
		});
	} else {
		createCK5(targetDiv[0], options);
	}
});

window.addEventListener("message", function (event) {
	if (event.origin !== window.location.origin) return;
	if (
		typeof event.data === "object" &&
		event.data.name === "eMediaAssetPicked" &&
		event.data.target === "htmleditor"
	) {
		$(window).trigger("assetpicked", [event.data]);
		var pickermodal = $("#dialogpickerassetpicker");
		if (!pickermodal.length) {
			pickermodal = $("#blockfindpicker").closest(".modal");
		}
		closeemdialog(pickermodal);
	}
});

window.updateAllCK5 = function () {
	const keys = Object.keys(window.CK5Editor);
	if (keys.length == 0) return;
	keys.forEach((key) => {
		window.CK5Editor[key].updateSourceElement();
	});
};

function createInlineCK5(target, options = {}) {
	let uid = target.id;
	if (!uid) {
		uid = "ck5inline" + Object.keys(window.CK5EditorInline).length;
		target.id = uid;
	}

	if (window.CK5EditorInline[uid]) {
		disposeCKEditor(window.CK5EditorInline[uid], doCreate);
	} else {
		doCreate();
	}

	function doCreate() {
		InlineEditor.create(target, editorConfig(options, true))
			.then((editor) => {
				const targetContainer = $(target).data("targetcontainer");
				if (targetContainer) {
					editor.model.document.on("change:data", function () {
						const editorData = editor.getData();
						$(targetContainer).val(editorData);
					});
				}

				window.CK5EditorInline[uid] = editor;
				editor.editing.view.focus();

				if (!options.hideImagePicker) {
					$(window).on("assetpicked", function (_, params) {
						const imageUrl = params.assetpicked;
						setTimeout(() => {
							editor.execute("imageInsert", { source: imageUrl });
						});
					});
				}

				const modal = $(target).closest(".modal");
				if (modal.length > 0) {
					modal.attr("tabindex", "");
				}
			})
			.catch((error) => {
				console.error(error);
			});
	}
}

$(window).on("inlinehtmlstart", function (_, targetDiv) {
	if (targetDiv.length === 0) return;

	if (targetDiv.data("ck5Initialized")) {
		console.log("Already initialized, skipping setup.");
		return;
	}

	const hideSaving = targetDiv.data("editonly");
	const hideImagePicker = targetDiv.data("imagepickerhidden");
	const options = {
		hideImagePicker,
		hideSaving,
		sourceElement: targetDiv[0],
	};

	const uid = targetDiv[0].id;

	targetDiv.data("ck5Initialized", true);

	if (uid && window.CK5Editor[uid]) {
		disposeCKEditor(window.CK5Editor[uid], () => {
			createInlineCK5(targetDiv[0], options);
		});
	} else {
		createInlineCK5(targetDiv[0], options);
	}
});

$(document).ready(function () {
	lQuery("textarea.htmleditor-advanced").livequery(function () {
		$(window).trigger("edithtmlstart", [$(this)]);
	});
	lQuery("textarea.htmleditor").livequery(function () {
		const $this = $(this).get(0);
		const uid = $this.id;
		const options = {
			hideImagePicker: true,
			hideSaving: true,
			// hideSourceEditing: true,
		};
		if (uid && window.CK5Editor[uid]) {
			disposeCKEditor(window.CK5Editor[uid], () => {
				createCK5($this, options);
			});
		} else {
			createCK5($this, options);
		}
	});
});
