var chatconnection;
var chatopen = false;
var loadingmore = false;

lQuery("a.chatEmDialog").livequery("click", function (e) {
	e.preventDefault();
	e.stopPropagation();
	$(this).emDialog(function () {
		setTimeout(function () {
			scrollToChat();
		});
	});
});

function chatterbox() {
	//console.log('loaded');

	//reloadAll();

	if (chatopen) {
		return;
	}

	connect();
	keepAlive(); //this should be here so it doesn't keep getting requeued.

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

		if (chatconnection.readyState === chatconnection.CLOSED) {
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
		}
	});

	lQuery("#closeattachfileonchat").livequery("click", function () {
		var button = jQuery(this);
		button.closest(".attachfileonchat").html("");
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

	chatopen = true;
}
lQuery("#supportchat").livequery("shown.bs.collapse	", function (e) {
	scrollToChat();
});
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
		//console.log(message);

		var channel = message.channel;
		var id = message.messageid;
		message.id = id;
		var existing = jQuery("#chatter-message-" + id);
		if (existing.length) {
			var chatMsg = $(existing).find(".chat-msg");
			var msgBody = $(chatMsg).find(".msg-body-content");
			if (msgBody.length) {
				msgBody.html(message.message);
			} else {
				chatMsg.html(message.message);
			}
			return;
		}
		var chatter = jQuery('div[data-channel="' + channel + '"]');
		var listarea = chatter.find(".chatterbox-message-list");
		var url = chatter.data("rendermessageurl");
		if (!url) {
			url = apphome + "/components/chatterbox/message.html";
		}

		if (message.command === "aithinking") {
			showSpinner();
			return;
		} else {
			hideSpinner();
		}

		scrollToChat();

		jQuery.get(url, message, function (data) {
			listarea.append(data);
			$(document).trigger("domchanged");
			scrollToChat();
		});

		registerServiceWorker();

		/*Check if you are the sender, play sound and notify. "message.topic != message.user" checks for private chat*/
		var user = app.data("user");

		if (message.user != user) {
			/*Desktop notifications - mando*/
			function showNotification() {
				if ($("#aichatsearch").length) return;
				const header = "New Message";
				if (message.name !== undefined) {
					header = message.name;
				}
				if (message.topic != undefined) {
					header += " in " + message.topic;
				}
				const notification = new Notification(header, {
					//TODO: URL?
					body: message.message,
					renotify: false,
					tag: message.message,
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
		console.log("already loading");
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

var timerID = 0;
var wasconnected;

function keepAlive() {
	var timeout = 20000;
	if (chatconnection.readyState == chatconnection.OPEN) {
		wasconencted = true;
		var command = new Object();
		command.command = "keepalive";

		var userid = jQuery(".chatterbox").data("user"); //TODO: Use app?
		command.userid = userid;

		var chatter = jQuery(".chatterbox").data("channel");
		command.channel = chatter;

		var json = JSON.stringify(command);
		chatconnection.send(json);
	}

	if (chatconnection.readyState === chatconnection.CLOSED) {
		connect();
		//reloadAll();
	}

	timerId = setTimeout(keepAlive, timeout);
}

function cancelKeepAlive() {
	if (timerId) {
		clearTimeout(timerId);
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
const pushServerPublicKey =
	"BIN2Jc5Vmkmy-S3AUrcMlpKxJpLeVRAfu9WBqUbJ70SJOCWGCGXKY-Xzyh7HDr6KbRDGYHjqZ06OcS3BjD7uAm8";

function registerServiceWorker() {
	if (navigator.serviceWorker !== undefined) {
		navigator.serviceWorker.register("sw.js");
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

jQuery(document).ready(function () {
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

	// lQuery(".typeout").livequery(function () {
	// 	var check = $(this).data("typed");
	// 	if (check) {
	// 		return;
	// 	}
	// 	$(this).data("typed", true);
	// var strings = $(this).text().split("\n");
	// console.log(strings);
	// 	var typed = new Typed($(this), {
	// 		strings,
	// 		typeSpeed: 40,
	// 		backSpeed: 0,
	// 		loop: true,
	// 	});
	// 	typed.start();
	// });

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

	lQuery(".chatter-emoji").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var href = $(this).attr("href");
		var target = $(this).parent();
		if (target.find("#emojipicker").length > 0) {
			return;
		}
		jQuery.get(href, { oemaxlevel: 1 }, function (data) {
			target.prepend(data);
		});
	});

	$("body").on("click", "#emojinav a", function (e) {
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

	$("body").on("click", ".emjbtn", function () {
		var emoji = $(this).text();
		var prev = $("#chatter-msg").val() || "";
		$("#chatter-msg").val(prev + emoji);
		$("#emojipicker").fadeOut(function () {
			$(this).remove();
		});
	});

	$(document).on("click", function (e) {
		if ($(e.target).closest("#emojipicker").length === 0) {
			$("#emojipicker").fadeOut(function () {
				$(this).remove();
			});
		}
	});
});
