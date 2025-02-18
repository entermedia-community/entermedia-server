var emojiReg =
	/\p{RI}\p{RI}|\p{Emoji}(\p{EMod}+|\u{FE0F}\u{20E3}?|[\u{E0020}-\u{E007E}]+\u{E007F})?(\u{200D}\p{Emoji}(\p{EMod}+|\u{FE0F}\u{20E3}?|[\u{E0020}-\u{E007E}]+\u{E007F})?)+|\p{EPres}(\p{EMod}+|\u{FE0F}\u{20E3}?|[\u{E0020}-\u{E007E}]+\u{E007F})?|\p{Emoji}(\p{EMod}+|\u{FE0F}\u{20E3}?|[\u{E0020}-\u{E007E}]+\u{E007F})/gu;

var shouldntBeParsed =
	/IFRAME|NOFRAMES|NOSCRIPT|SCRIPT|SELECT|STYLE|TEXTAREA|[a-z]/;

function createText(text) {
	return document.createTextNode(text);
}

function emojiUnicode(input) {
	if (input.length === 1) {
		return input.charCodeAt(0).toString(16);
	} else if (input.length > 1) {
		const pairs = [];
		for (var i = 0; i < input.length; i++) {
			if (
				// high surrogate
				input.charCodeAt(i) >= 0xd800 &&
				input.charCodeAt(i) <= 0xdbff
			) {
				if (
					input.charCodeAt(i + 1) >= 0xdc00 &&
					input.charCodeAt(i + 1) <= 0xdfff
				) {
					// low surrogate
					pairs.push(
						(input.charCodeAt(i) - 0xd800) * 0x400 +
							(input.charCodeAt(i + 1) - 0xdc00) +
							0x10000
					);
				}
			} else if (input.charCodeAt(i) < 0xd800 || input.charCodeAt(i) > 0xdfff) {
				// modifiers and joiners
				pairs.push(input.charCodeAt(i));
			}
		}
		return pairs.map((val) => parseInt(val).toString(16)).join("-");
	}
	return "";
}

function getEmojiSrc(icon) {
	return "".concat(emojiUnicode(icon), ".svg");
}

window.parseEmojis = function (node) {
	var emojiBaseUrl =
		$("#application").data("apphome") + "/components/javascript/twemoji/svg/";
	var allText = node.childNodes; // grabAllTextNodes(node);
	var length = allText.length,
		modified,
		fragment,
		subnode,
		text,
		match,
		i,
		index,
		img,
		alt,
		icon,
		src;
	while (length--) {
		modified = false;
		fragment = document.createDocumentFragment();
		subnode = allText[length];
		text = subnode.nodeValue;
		if (!text) continue;
		text = text.trim();
		text = text.replace(/"/g, '\\"');
		text = text.replace(/\s/g, " ");
		text = JSON.parse(`"${text}"`);
		i = 0;
		while ((match = emojiReg.exec(text))) {
			index = match.index;

			icon = match[0];
			if (index !== i) {
				fragment.appendChild(createText(text.slice(i, index)));
			}
			i = index + icon.length;

			if (icon) {
				src = getEmojiSrc(icon);
				if (src) {
					img = new Image();
					img.setAttribute("draggable", "false");
					img.setAttribute("loading", "lazy");
					img.className = "emoji";
					img.src = emojiBaseUrl + src;
					modified = true;
					fragment.appendChild(img);
				}
			}
			if (!img) fragment.appendChild(createText(alt));
			img = null;
		}

		if (modified) {
			if (i < text.length) {
				fragment.appendChild(createText(text.slice(i)));
			}
			subnode.parentNode.replaceChild(fragment, subnode);
		}
	}
	return node;
};
