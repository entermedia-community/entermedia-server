var chatconnection;
var loadingmore = false;

function chatterbox() {
	console.log(
		"Starting chat in channel: " + jQuery(".chatterbox").data("channel")
	);

	cancelKeepAlive();
	connect();
	keepAlive();

	lQuery(".chatter-send").livequery("click", function () {
		var button = jQuery(this);
		var chatter = button.closest(".chatterbox");
		var data = chatter.data();

		data = jQuery.extend({}, data); //So we can edit it
		data.command = button.data("command");

		var input = $("#chatter-msg");
		var replytoid = input.data("replytoid");
		if (replytoid) {
			data.replytoid = replytoid;
		}
		var message = input.val();
		data.message = message;

		var json = JSON.stringify(data);

		if (chatconnection.readyState == chatconnection.CLOSED) {
			connect();
			//IF we do a reconnect render the whole page
		}
		var toggle = button.data("toggle");
		if (toggle == true) {
			jQuery(".chatter-toggle").toggle();
		}

		if (jQuery("#chatter-msg").val() != "") {
			chatconnection.send(json);

			//Clear editing area
			var area = jQuery("#chatterbox-write");
			$("#chatter-msg", area).val("");
			$("#chatter-msg").data("replytoid", "");
			$(".chatterboxreplyto", area).hide();
			//console.log($("#chatter-msg").data("replytoid"));
			//scroll down, delay a little?
			scrollToChat();

			var ais = $(".ai-suggestions");
			if (ais.length > 0) {
				ais.remove();
			}
			var ses = $(".sessionhistory-item.active");
			if (ses.length > 0) {
				var span = ses.find(".item span");
				if (span.length > 0 && span.text() === "Current Session") {
					span.text(message.substring(0, 25));
				}
			}
		}
	});

	lQuery(".ai-suggest").livequery("click", function () {
		var button = jQuery(this);
		var message = button.text();
		var input = $("#chatter-msg");
		input.val(message);
		input.focus();
		setTimeout(function () {
			$(".chatter-send").trigger("click");
			button.closest(".msg-bubble").remove();
		});
	});

	lQuery("#chatterboxreplycancel").livequery("click", function () {
		var button = jQuery(this);
		button.closest(".chatterboxreplyto").hide();
	});

	lQuery(".chatter-text").livequery("keydown", function (e) {
		if (e.keyCode == 13 && !e.shiftKey) {
			//jQuery("#chatter-msg").val("");
			e.preventDefault();
			var button = jQuery('button[data-command="messagereceived"]');
			button.trigger("click");
			return false;
		} else {
			var scroll_height = $(this).get(0).scrollHeight;
			if (!$(".chatterbox").hasClass("chatterlongtext") && scroll_height > 30) {
				$(".chatterbox").addClass("chatterlongtext");
				scrollToChat();
			}
		}
	});

	lQuery('button[data-command="messagereceived"]').livequery(
		"click",
		function (e) {
			//jQuery("#chatter-msg").val("");
		}
	);

	lQuery(".chatter-save").livequery("click", function (e) {
		e.preventDefault();
		var button = jQuery(this);
		var form = button.closest(".chatter-edit-form");
		var chatdiv = form.find(".chatter-msg-edit");
		var text = chatdiv.html();
		form.find(".chatter-msg-input").val(text);
		/*var button = jQuery('submit');		    	
    	button.trigger("#submit");*/
		form.trigger("submit");
	});

	lQuery("a.ajax-edit-msg").livequery("click", function (e) {
		e.stopPropagation();
		e.preventDefault();
		var editbtn = $(this);
		var targetDiv = editbtn.data("targetdiv");
		var options = editbtn.data();
		options.oemaxlevel = 1;
		var nextpage = editbtn.attr("href");
		$.get(nextpage, options, function (data) {
			//var cell = findclosest($(this), "#" + targetDiv);
			var cell = editbtn.closest("#" + targetDiv);
			cell.replaceWith(data);
			scrollToEdit(targetDiv);
		});
	});

	lQuery(".chatterbox-body-inside").livequery("scroll", function (e) {
		if ($(this).scrollTop() < 50) {
			//loadMoreChats();
		}
	});

	lQuery("a.appendgoalbutton").livequery("click", function (e) {
		var parent = $(this).closest(".goalstatusopen");
		if (parent) {
			parent[0].scrollIntoView();
		}
	});
}

function scrollToChat() {
	setTimeout(function () {
		var inside = $(".chatterbox-body-inside");
		if (inside.length > 0) {
			inside.animate({ scrollTop: inside.get(0).scrollHeight }, 30);
		}
	});
}

function scrollToEdit(targetDiv) {
	var messagecontainer = $("#" + targetDiv);
	messagecontainer.get(0).scrollIntoView();
}

function connect() {
	if (chatconnection && chatconnection.readyState != chatconnection.CLOSED) {
		return;
	}
	var tabID =
		sessionStorage.tabID && sessionStorage.closedLastTab !== "2"
			? sessionStorage.tabID
			: (sessionStorage.tabID = Math.random());
	sessionStorage.closedLastTab = "2";
	$(window).on("unload beforeunload", function () {
		sessionStorage.closedLastTab = "1";
	});

	var app = jQuery("#application");
	var userid = app.data("user");
	var protocol = location.protocol;

	var url =
		"/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=" +
		tabID +
		"&userid=" +
		userid;

	//Get the channel
	var channel = jQuery(".chatterbox").data("channel");
	if (channel != null) {
		url = url + "&channel=" + channel;
	}

	if (protocol === "https:") {
		chatconnection = new WebSocket("wss://" + location.host + url);
	} else {
		chatconnection = new WebSocket("ws://" + location.host + url);
	}

	chatconnection.onmessage = function (event) {
		var app = jQuery("#application");
		var apphome = app.data("home") + app.data("apphome");
		jQuery(window).trigger("ajaxsocketautoreload");
		var message = JSON.parse(event.data);
		var id = message.messageid;

		var channel = message.channel;
		var chatbox = jQuery('div.chatterbox[data-channel="' + channel + '"]');

		if (chatbox.length == 0) {
			//Channel Not on the screen
			return;
		}

		message.id = id;
		var existing = jQuery("#chatter-message-" + id);
		if (existing.length) {
			if (message.command === "messageremoved") {
				existing.remove();
				return;
			}
			var chatMsg = $(existing).find(".chat-msg");
			var msgBody = $(chatMsg).find(".msg-body-content");
			if (msgBody.length) {
				msgBody.html(message.message);
			} else {
				chatMsg.html(message.message);
			}
			scrollToChat();
			return;
		}

		var listarea = chatbox.find(".chatterbox-message-list");
		var url = chatbox.data("rendermessageurl");
		if (!url) {
			url = apphome + "/components/chatterbox/message.html";
		}

		scrollToChat();

		var options = chatbox.cleandata();
		if (!options) options = {};
		var editdiv = chatbox.closest(".editdiv");
		if (
			chatbox.data("includeeditcontext") === undefined ||
			chatbox.data("includeeditcontext") == true
		) {
			if (editdiv.length > 0) {
				var otherdata = editdiv.cleandata();
				options = {
					...otherdata,
					...options,
				};
			}
		}

		options.id = message.id;

		/*
		var params = {};
		params.id = message.id;
		params.channel = message.channel;
		if (message.entityid != null) {
			params.entityid = message.entityid;
			params.collectionid = message.entityid;
		} else {
			params.entityid = message.collectionid;
			params.collectionid = message.collectionid;
		}*/

		jQuery.get(url, options, function (data) {
			var $div = jQuery("<div></div>");
			$div.html(data);
			var $data = $div.find("#chatter-message-" + message.id);
			var inserted = false;
			try {
				var createddat = $data.data("createdat");
				var timestamp = new Date(createddat).getTime();
				if (!isNaN(timestamp)) {
					var messages = listarea.find(".msg-bubble");
					messages.each(function () {
						var msgcreatedat = jQuery(this).data("createdat");
						var msgtimestamp = new Date(msgcreatedat).getTime();
						if (!isNaN(msgtimestamp)) {
							if (timestamp < msgtimestamp) {
								jQuery(this).before($data);
								inserted = true;
								return false;
							}
						}
					});
				}
			} catch (e) {
				// ignore
			} finally {
				if (!inserted) {
					listarea.append($data);
				}
				scrollToChat();
			}
		});

		registerServiceWorker();

		/*Check if you are the sender, play sound and notify. "message.topic != message.user" checks for private chat*/
		var user = app.data("user");

		if (message.user != user && message.user != "agent") {
			if (!document.hasFocus()) {
				function showNotification() {
					var header = "New Message";
					if (message.name !== undefined) {
						header = message.name;
					}
					if (message.topic != undefined) {
						header += " in " + message.topic;
					}
					var messagebody = message.message;
					if (messagebody != null) {
						messagebody = "New message...";
					}
					var notification = new Notification(header, {
						//TODO: URL?
						body: message.message,
						renotify: false,
						tag: messagebody,
						icon: apphome + "/theme/images/logo.png",
					});
					notification.addEventListener("click", function (event) {
						//window.open('http://www.mozilla.org', '_blank');
					});
					play();
				}

				/*Check para permissions and ask.*/
				if (Notification.permission === "granted") {
					showNotification();
				} else if (Notification.permission !== "denied") {
					createNotificationSubscription();

					Notification.requestPermission().then((permission) => {
						if (permission === "granted") {
							showNotification();
						}
					});
				}
			}
		}
	};
}

/*--------------Begin Functions List--------------*/
function reloadAll() {
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	jQuery(".chatterbox").each(function () {
		var chatter = $(this);
		var url = chatter.data("renderurl");
		if (!url) {
			url = apphome + "/components/chatterbox/index.html";
		}
		var chatterdiv = $(this);
		var mydata = $(this).data();
		jQuery.get(url, mydata, function (data) {
			chatterdiv.html(data);

			scrollToChat();
		});
	});
}

function showSpinner() {
	//TODO:  Shakil Add a spinner somewhere to let us know the chat bot is about to say something
	console.log("AI about to respond");
	jQuery(".chatterspinner").show();
	scrollToChat();
}

function hideSpinner() {
	//TODO:  Shakil Add a spinner somewhere to let us know the chat bot is about to say something
	console.log("AI done");
	jQuery(".chatterspinner").hide();
}

function loadMoreChats() {
	//already loading
	if (loadingmore) {
		//console.log("already loading");
		//skip
		return;
	}
	loadingmore = true;
	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");

	jQuery(".chatterbox").each(function () {
		var url = apphome + "/components/chatterbox/loadmessages.html";

		var chatterdiv = $(this);
		var mydata = $(this).data();
		mydata.lastloaded = chatterdiv
			.find(".chatterbox-messages")
			.data("lastloaded");
		jQuery.get(url, mydata, function (data) {
			chatterdiv.find(".chatterbox-message-list").prepend(data);
			//scrollToChat();
			loadingmore = false;
			console.log("stop loading now");
		});
	});
}

var keepAliveTimeoutID = 0;

function keepAlive() {
	if (!chatconnection) {
		return;
	}
	var timeout = 20000;
	if (chatconnection.readyState == chatconnection.OPEN) {
		var command = new Object();
		command.command = "keepalive";

		var userid = jQuery(".chatterbox").data("user"); //TODO: Use app?
		command.userid = userid;

		var chatter = jQuery(".chatterbox").data("channel");
		command.channel = chatter;

		var json = JSON.stringify(command);
		chatconnection.send(json);
	}

	if (chatconnection.readyState == chatconnection.CLOSED) {
		connect();
		//reloadAll();
	}

	keepAliveTimeoutID = setTimeout(keepAlive, timeout);
}

function cancelKeepAlive() {
	if (keepAliveTimeoutID) {
		clearTimeout(keepAliveTimeoutID);
	}
}

function play() {
	var app = jQuery("#application");
	var apphome = app.data("apphome");
	var home = app.data("home");
	if (home !== undefined) {
		apphome = home + apphome;
	}
	var urls = apphome + "/components/chatterbox/stairs.wav";

	var snd = new Audio(urls); // buffers automatically when created
	snd.play();
}

/*-------Start Push and Notification --------*/
var pushServerPublicKey =
	"BIN2Jc5Vmkmy-S3AUrcMlpKxJpLeVRAfu9WBqUbJ70SJOCWGCGXKY-Xzyh7HDr6KbRDGYHjqZ06OcS3BjD7uAm8";

function registerServiceWorker() {
	var app = jQuery("#application");
	var apphome = app.data("apphome");
	var home = app.data("home");
	if (home !== undefined) {
		apphome = home + apphome;
	}
	if (navigator.serviceWorker !== undefined) {
		navigator.serviceWorker.register(apphome + "/components/chatterbox/sw.js");
	}
}

function initializePushNotifications() {
	return Notification.requestPermission(function (result) {
		return result;
	});
}
function isPushNotificationSupported() {
	return "serviceWorker" in navigator && "PushManager" in window;
}

function createNotificationSubscription() {
	//wait for service worker installation to be ready, and then
	return navigator.serviceWorker.ready.then(function (serviceWorker) {
		// subscribe and return the subscription
		return serviceWorker.pushManager
			.subscribe({
				userVisibleOnly: true,
				applicationServerKey: pushServerPublicKey,
			})
			.then(function (subscription) {
				// send this to Entermedia backend with a user id
				// 'subscription' == PushSubscription (object)
				console.log("User is subscribed.", subscription);
				console.log(subscription.endpoint);
				return subscription;
			});
	});
}

function hideAttachFile() {
	if ($(".message-attach-box").is(":visible")) {
		$(".message-attach-box").fadeOut(function () {
			$(this).remove();
			$(".chatter-attachfile").removeClass("active");
		});
	}
}
function hideEmojiPicker() {
	if ($(".emoji-picker").is(":visible")) {
		$(".emoji-picker").fadeOut(function () {
			$(this).remove();
			$(".chatter-emoji").removeClass("active");
		});
	}
}

jQuery(document).ready(function () {
	lQuery("a.chatEmDialog").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		$(this).emDialog(function () {
			setTimeout(function () {
				scrollToChat();
			});
		});
	});
	lQuery("#supportchat").livequery("shown.bs.collapse	", function (e) {
		scrollToChat();
	});
	lQuery("#chatter-msg").livequery(function () {
		var $this = $(this);
		setTimeout(function () {
			$this.focus();
		});
	});

	lQuery(".chatterbox").livequery(function () {
		chatterbox();
		scrollToChat();
	});

	lQuery(".expandaisearchtable").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		$(this).toggleClass("expanded");
		$(this)
			.closest(".aisearchtable-container")
			.find(".aisearchtable")
			.collapse("toggle");
	});

	lQuery(".chat-msg").livequery(function () {
		var emojiparsed = $(this).data("emojiparsed");
		if (emojiparsed) {
			return;
		}
		$(this).data("emojiparsed", true);
		var msgContent = $(this).find(".msg-body-content");
		var reacts = $(this).find("span.emote");
		if (window.parseEmojis != undefined) {
			if (msgContent.length > 0) {
				window.parseEmojis(msgContent[0]);
			}
			if (reacts.length > 0) {
				window.parseEmojis(reacts[0]);
			}
		}
	});

	var chatSelectionStart = null;

	lQuery(".chatter-emoji").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var textarea = $("#emojipicker").data("textarea");
		if (textarea) {
			textarea = $("#" + textarea);
		} else {
			textarea = $("#chatter-msg");
		}
		chatSelectionStart = textarea.prop("selectionStart");
		hideAttachFile();
		$(this).addClass("active");
		$(this).runAjax();
	});
	lQuery(".chatter-attachfile").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		hideEmojiPicker();
		$(this).addClass("active");
		$(this).runAjax();
	});

	lQuery("#emojinav a").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var goTo = $(this).data("id");
		if (goTo == "smileys") {
			$(".emoji-wrapper").animate({ scrollTop: 0 }, 500);
			return;
		}
		$(".emoji-wrapper").scrollTop(0);
		var dest =
			$("#" + goTo).offset().top -
			$("#" + goTo)
				.offsetParent()
				.offset().top;
		$(".emoji-wrapper").animate({ scrollTop: dest - 70 }, 500);
	});

	lQuery(".emjbtn").livequery("click", function () {
		var textarea = $("#emojipicker").data("textarea");
		if (textarea) {
			textarea = $("#" + textarea);
		} else {
			textarea = $("#chatter-msg");
		}

		var emoji = $(this).text();
		var prev = textarea.val() || "";
		if (chatSelectionStart != null) {
			prev =
				prev.slice(0, chatSelectionStart) +
				emoji +
				prev.slice(chatSelectionStart);
		} else {
			prev += emoji;
		}

		textarea.val(prev);

		$(".emoji-picker").fadeOut(function () {
			textarea.focus();
			$(this).remove();
		});
	});
	function hideChatPickers(e) {
		if ($(e.target).closest("#emojipicker").length === 0) {
			hideEmojiPicker();
		}
	}

	lQuery("#closeattachfileonchat").livequery("click", function () {
		hideAttachFile();
	});

	lQuery("window").livequery("click", hideChatPickers);
	lQuery(".modal").livequery("click", hideChatPickers);

	/**Attachments */

	lQuery(".chat-msg-attachments-asset .removefieldassetvalueZ").livequery(
		"click",
		function (e) {
			$(this).runAjax();
		}
	);

	lQuery("a.lightbox").livequery(function () {
		var slb = $(this).simpleLightbox({
			captionSelector: "self",
			captionType: "data",
			captionsData: "caption",
			captionDelay: 250,
			widthRatio: 0.98,
			heightRatio: 0.98,
			overlayOpacity: 1,
			fadeSpeed: 60,
		});

		slb.on("shown.simplelightbox", function () {
			var lang = document.documentElement.lang;
			if (lang) {
				var locales = $(this).data("locales");
				if (locales) {
					if (typeof locales === "string") {
						var txt = document.createElement("textarea");
						txt.innerHTML = locales;
						locales = JSON.parse(txt.value);
					}
					var localeCaption = locales[lang];
					if (localeCaption) {
						$(".sl-caption").html(localeCaption);
					}
				}
			}

			var dl = $(this).data("downloadlink");
			var al = $(this).data("assetlink");
			if (dl || al) {
				$(".simple-lightbox").append("<div class='sl-actions'></div>");

				if (dl) {
					$(".simple-lightbox .sl-actions").append(
						"<a class='sl-btn sl-dl' href='" + dl + "' target='_blank'></a>"
					);
				}
				//Asset Link

				if (al) {
					$(".simple-lightbox .sl-actions").append(
						"<a class='sl-btn sl-al' href='" + al + "' target='_blank'></a>"
					);
				}
			}
		});
	});

	lQuery(".autoprefixchatmsg").livequery("click", function () {
		var prefix = $(this).data("prefix");
		var editorid = $(this).data("editorid");
		if (!editorid) {
			editorid = "chatter-msg";
		}
		var editor = $("#" + editorid);
		editor.val(prefix);
		editor.focus();
		$(this).parent().removeClass("show");
	});
});
