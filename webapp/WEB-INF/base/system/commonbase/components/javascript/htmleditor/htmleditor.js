import {
	ClassicEditor,
	Alignment,
	AutoImage,
	AutoLink,
	Autosave,
	BalloonToolbar,
	Bold,
	CloudServices,
	Essentials,
	Heading,
	SourceEditing,
	ImageBlock,
	ImageInline,
	ImageInsert,
	ImageInsertViaUrl,
	ImageResize,
	ImageStyle,
	ImageToolbar,
	// ImageUpload,
	Indent,
	IndentBlock,
	Italic,
	Link,
	List,
	Paragraph,
	RemoveFormat,
	// SimpleUploadAdapter,
	// SpecialCharacters,
	Strikethrough,
	Underline,
	ButtonView,
	Plugin,
} from "ckeditor5";

import prettifyHTML from "prettyhtml";

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
				const savepath = $(editor.sourceElement).data("savepath");
				const editpath = $(editor.sourceElement).data("editpath");

				const keepeditor = $(editor.sourceElement).data("keepeditor");

				var editorData = editor.getData();
				if (editorData) editorData = prettifyHTML(editor.getData());
				$(editor.sourceElement).val(editorData);
				$.ajax({
					url: savepath,
					type: "POST",
					data: {
						content: editorData,
						editPath: editpath,
					},
					success: function () {
						if (!keepeditor) {
							editor.updateSourceElement();
							editor
								.destroy()
								.then(() => {
									if (editor.sourceElement) editor.sourceElement = null;
								})
								.catch((error) => {
									console.log(error);
								});
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
				editor.sourceElement = null;
				editor.destroy();
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
				var findroot = $("#application").data("findroot");
				var anchor = document.createElement("a");
				anchor.href = findroot + "/blockiframe.html";
				anchor.classList.add("emdialog");
				document.body.appendChild(anchor);
				$(anchor).emDialog();
				anchor.remove();
			});

			return button;
		});
	}
}

const LICENSE_KEY = "GPL";

const editorConfig = (editOnly = false, hideImagePicker = false) => {
	var items = [
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
		// "specialCharacters",
		"link",
		// "insertImage",
		"|",
		"alignment",
		"|",
		"bulletedList",
		"numberedList",
		"outdent",
		"indent",
		"|",
		"undo",
		"redo",
		"|",
		"sourceEditing",
	];
	if (editOnly) {
		items = items.slice(3);
	}
	if (hideImagePicker) {
		var idx = items.indexOf("imagePicker");
		items.splice(idx, 1);
	}
	return {
		updateSourceElementOnDestroy: true,
		toolbar: {
			items,
			shouldNotGroupWhenFull: false,
		},
		plugins: [
			CloseButtonPlugin,
			SaveButtonPlugin,
			Alignment,
			AutoImage,
			AutoLink,
			Autosave,
			BalloonToolbar,
			Bold,
			CloudServices,
			Essentials,
			Heading,
			SourceEditing,
			ImageBlock,
			ImageInline,
			ImageInsert,
			ImageInsertViaUrl,
			ImageResize,
			ImageStyle,
			ImageToolbar,
			// ImageUpload,
			ImagePicker,
			Indent,
			IndentBlock,
			Italic,
			Link,
			List,
			Paragraph,
			RemoveFormat,
			// SimpleUploadAdapter,
			// SpecialCharacters,
			Strikethrough,
			Underline,
		],
		balloonToolbar: [
			"bold",
			"italic",
			"|",
			"link",
			// "insertImage",
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
		image: {
			toolbar: [
				"imageTextAlternative",
				"|",
				"imageStyle:inline",
				"imageStyle:wrapText",
				"imageStyle:breakText",
				"|",
				"resizeImage",
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
		// initialData: "",
		extraConfig: {
			oldcontent: "",
		},
	};
};

window.CK5Editor = null;

function createCK5Adv(target, editOnly, hideImagePicker) {
	ClassicEditor.create(target, editorConfig(editOnly, hideImagePicker))
		.then((editor) => {
			window.CK5Editor = editor;
			$(window).on("assetpicked", function (_, imageUrl) {
				setTimeout(() => {
					editor.execute("imageInsert", { source: imageUrl });
				});
			});
		})
		.catch((error) => {
			console.error(error);
		});
}

$(window).on("edithtmlstart", function (_, targetdiv) {
	const editonly = targetdiv.data("editonly");
	const hideImagePicker = targetdiv.data("imagepickerhidden");
	if (window.CK5Editor) {
		window.CK5Editor.destroy()
			.then(() => createCK5Adv(targetdiv[0], editonly, hideImagePicker))
			.catch((error) => {
				console.error(error);
			});
	} else {
		createCK5Adv(targetdiv[0], editonly, hideImagePicker);
	}
});

window.addEventListener("message", function (event) {
	if (event.origin !== window.location.origin) return;
	if (typeof event.data === "string" && event.data.startsWith("assetpicked:")) {
		var url = event.data.substring(12);
		$(window).trigger("assetpicked", [url]);
		closeallemdialogs();
	}
});

$(document).ready(function () {
	lQuery("textarea.htmleditor-advanced").livequery(function () {
		$(window).trigger("edithtmlstart", [$(this)]);
	});
});
