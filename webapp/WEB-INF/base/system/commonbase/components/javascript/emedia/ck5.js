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
	// ImageBlock,
	// ImageInline,
	// ImageInsert,
	// ImageInsertViaUrl,
	// ImageResize,
	// ImageStyle,
	// ImageToolbar,
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
} from "ckeditor5";

const LICENSE_KEY = "GPL"; // or <YOUR_LICENSE_KEY>.

const editorConfig = () => ({
	toolbar: {
		items: [
			"heading",
			"|",
			"bold",
			"italic",
			"underline",
			"strikethrough",
			"removeFormat",
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
		],
		shouldNotGroupWhenFull: false,
	},
	plugins: [
		Alignment,
		AutoImage,
		AutoLink,
		Autosave,
		BalloonToolbar,
		Bold,
		CloudServices,
		Essentials,
		Heading,
		// ImageBlock,
		// ImageInline,
		// ImageInsert,
		// ImageInsertViaUrl,
		// ImageResize,
		// ImageStyle,
		// ImageToolbar,
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
	// image: {
	// 	toolbar: [
	// 		"imageTextAlternative",
	// 		"|",
	// 		"imageStyle:inline",
	// 		"imageStyle:wrapText",
	// 		"imageStyle:breakText",
	// 		"|",
	// 		"resizeImage",
	// 	],
	// },
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
});

window.CK5Editor = {};

function createCK5(target) {
	let uid = target.id;
	if (!uid) {
		uid = "ck5editor" + window.CK5Editor.length;
		target.id = uid;
	}
	ClassicEditor.create(target, editorConfig())
		.then((editor) => {
			window.CK5Editor[uid] = editor;
			var modal = $(target).closest(".modal");
			if (modal.length > 0) {
				modal.attr("tabindex", "");
			}
		})
		.catch((error) => {
			console.error(error);
		});
}

lQuery("textarea.htmleditor").livequery(function () {
	var $this = $(this).get(0);
	var uid = $this.id;
	if (uid && window.CK5Editor[uid]) {
		window.CK5Editor[uid]
			.destroy()
			.then(() => createCK5($this))
			.catch((error) => {
				console.error(error);
			});
	} else {
		createCK5($this);
	}
});

window.updateAllCK5 = function () {
	var keys = Object.keys(window.CK5Editor);
	if (keys.length == 0) return;
	keys.forEach((key) => {
		window.CK5Editor[key].updateSourceElement();
	});
};
