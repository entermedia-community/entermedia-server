const CONFIG = {
	ignore: [],
	strict: false,
	tab_size: 2,
	trim: [],
};
function isHtml(content) {
	const regex = /<(?<Element>[A-Za-z]+\b)[^>]*(?:.|\n)*?<\/{1}\k<Element>>/;
	return regex.test(content);
}
function protectElement(match, capture) {
	return match.replace(capture, function (match) {
		return match
			.replace(/</g, "&lt;")
			.replace(/>/g, "&gt;")
			.replace(/\n/g, "&#10;")
			.replace(/\r/g, "&#13;")
			.replace(/\s/g, "&nbsp;");
	});
}
function unprotectElement(match, capture) {
	return match.replace(capture, function (match) {
		return match
			.replace(/&lt;/g, "<")
			.replace(/&gt;/g, ">")
			.replace(/&#10;/g, "\n")
			.replace(/&#13;/g, "\r")
			.replace(/&nbsp;/g, " ");
	});
}
function ignoreElement(html, ignore, mode = "protect") {
	for (let e = 0; e < ignore.length; e++) {
		const regex = new RegExp(
			`<${ignore[e]}[^>]*>((.|\n)*?)<\/${ignore[e]}>`,
			"g"
		);
		html = html.replace(
			regex,
			mode === "protect" ? protectElement : unprotectElement
		);
	}
	return html;
}
function trimify(html, trim) {
	for (let e = 0; e < trim.length; e++) {
		const leading_whitespace = new RegExp(`(<${trim[e]}[^>]*>)\\s+`, "g");
		const trailing_whitespace = new RegExp(`\\s+(</${trim[e]}>)`, "g");
		html = html
			.replace(leading_whitespace, "$1")
			.replace(trailing_whitespace, "$1");
	}
	return html;
}

function entify(html) {
	html = html.replace(
		/<textarea[^>]*>((.|\n)*?)<\/textarea>/g,
		function (match, capture) {
			return match.replace(capture, function (match) {
				return match
					.replace(/</g, "&lt;")
					.replace(/>/g, "&gt;")
					.replace(/"/g, "&quot;")
					.replace(/'/g, "&apos;")
					.replace(/\n/g, "&#10;")
					.replace(/\r/g, "&#13;")
					.replace(/\s/g, "&nbsp;");
			});
		}
	);
	return html;
}
function minify(html) {
	html = entify(html);
	return html
		.replace(/\n|\t/g, "")
		.replace(/[a-z]+="\s*"/gi, "")
		.replace(/>\s+</g, "><")
		.replace(/\s+/g, " ")
		.replace(/\s>/g, ">")
		.replace(/<\s\//g, "</")
		.replace(/>\s/g, ">")
		.replace(/\s</g, "<")
		.replace(/class=["']\s/g, function (match) {
			return match.replace(/\s/g, "");
		})
		.replace(/(class=.*)\s(["'])/g, "$1" + "$2");
}

const void_elements = [
	"area",
	"base",
	"br",
	"col",
	"embed",
	"hr",
	"img",
	"input",
	"link",
	"meta",
	"param",
	"source",
	"track",
	"wbr",
];

function closify(html) {
	return html.replace(/<([a-zA-Z\-0-9]+)[^>]*>/g, function (match, name) {
		if (void_elements.indexOf(name) > -1) {
			return `${match.substring(0, match.length - 1)} />`.replace(
				/\/\s\//g,
				"/"
			);
		}
		return match.replace(/[\s]?\/>/g, `></${name}>`);
	});
}

let strict;
let trim;
const convert = {
	line: [],
};

function enqueue(html) {
	convert.line = [];
	let i = -1;
	html = html.replace(/<[^>]*>/g, function (match) {
		convert.line.push(match);
		i++;
		return `\n[#-# : ${i} : ${match} : #-#]\n`;
	});
	return html;
}

function preprocess(html) {
	html = closify(html);
	if (trim.length > 0) html = trimify(html, trim);
	html = minify(html);
	html = enqueue(html);
	return html;
}

function process(html, step) {
	let indents = "";
	convert.line.forEach(function (source, index) {
		html = html
			.replace(/\n+/g, "\n")
			.replace(`[#-# : ${index} : ${source} : #-#]`, function (match) {
				let subtrahend = 0;
				let prevLine = `[#-# : ${index - 1} : ${
					convert.line[index - 1]
				} : #-#]`;
				indents += "0";
				if (index === 0) subtrahend++;
				if (match.indexOf(`#-# : ${index} : </`) > -1) subtrahend++;
				if (prevLine.indexOf("<!doctype") > -1) subtrahend++;
				if (prevLine.indexOf("<!--") > -1) subtrahend++;
				if (prevLine.indexOf("/> : #-#") > -1) subtrahend++;
				if (prevLine.indexOf(`#-# : ${index - 1} : </`) > -1) subtrahend++;
				const offset = indents.length - subtrahend;
				indents = indents.substring(0, offset);
				if (strict && match.indexOf("<!--") > -1) return "";
				const result = match
					.replace(`[#-# : ${index} : `, "")
					.replace(" : #-#]", "");
				return result.padStart(result.length + step * offset);
			});
	});
	html = html.replace(
		/>[^<]*?[^><\/\s][^<]*?<\/|>\s+[^><\s]|<script[^>]*>\s+<\/script>|<(\w+)>\s+<\/(\w+)|<([\w\-]+)[^>]*[^\/]>\s+<\/([\w\-]+)>/g,
		function (match) {
			return match.replace(/\n|\t|\s{2,}/g, "");
		}
	);
	if (strict) html = html.replace(/\s\/>/g, ">");
	const lead_newline_check = html.substring(0, 1);
	const tail_newline_check = html.substring(html.length - 1);
	if (lead_newline_check === "\n") html = html.substring(1, html.length);
	if (tail_newline_check === "\n") html = html.substring(0, html.length - 1);
	return html;
}

const prettifyHTML = function (html) {
	if (!isHtml(html)) return html;
	strict = CONFIG.strict;
	const ignore = CONFIG.ignore.length > 0;
	trim = CONFIG.trim;
	if (ignore) {
		html = ignoreElement(html, CONFIG.ignore);
	}
	html = preprocess(html);
	html = process(html, CONFIG.tab_size);
	if (ignore) {
		html = ignoreElement(html, CONFIG.ignore, "unprotect");
	}
	return html;
};
export { prettifyHTML as default };
