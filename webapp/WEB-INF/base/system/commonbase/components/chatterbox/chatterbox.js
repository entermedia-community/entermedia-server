jQuery(document).ready(function () {
	"use strict";

	let chatConnection;
	let loadingMore = false;

	const app = $("#application");
	let appHome = app.data("apphome");
	const home = app.data("home");
	if (home !== undefined) {
		appHome = home + appHome;
	}
	const userid = app.data("user");

	function chatterbox() {
		cancelKeepAlive();
		connect();
		keepAlive();

		lQuery(".chatter-send").livequery("click", function () {
			const button = $(this);
			const chatter = button.closest(".chatterbox");
			let data = chatter.data();

			data = $.extend({}, data); //So we can edit it
			data.command = button.data("command");

			const input = $("#chatter-msg");
			const replytoid = input.data("replytoid");
			if (replytoid) {
				data.replytoid = replytoid;
			}
			const message = input.val();
			data.message = message;

			const json = JSON.stringify(data);

			if (chatConnection.readyState === chatConnection.CLOSED) {
				connect();
				//IF we do a reconnect render the whole page
			}
			const toggle = button.data("toggle");
			if (toggle === true) {
				$(".chatter-toggle").toggle();
			}

			if ($("#chatter-msg").val() !== "") {
				chatConnection.send(json);

				//Clear editing area
				const area = $("#chatterbox-write");
				$("#chatter-msg", area).val("");
				$("#chatter-msg").data("replytoid", "");
				$(".chatterboxreplyto", area).hide();

				scrollToChat();

				const ses = $(".sessionhistory-item.active");
				if (ses.length > 0) {
					const span = ses.find(".item span");
					if (span.length > 0 && span.text() === "Current Session") {
						span.text(message.substring(0, 25));
					}
				}
			}
		});

		lQuery(".ai-suggest").livequery("click", function () {
			const button = $(this);
			const message = button.text();
			const input = $("#chatter-msg");
			input.val(message);
			input.trigger("focus");
			setTimeout(() => {
				$(".chatter-send").trigger("click");
				button.closest(".msg-bubble").remove();
			});
		});

		lQuery("#chatterboxreplycancel").livequery("click", function () {
			const button = $(this);
			button.closest(".chatterboxreplyto").hide();
		});

		lQuery(".chatter-text").livequery("keydown", function (e) {
			if (e.keyCode === 13 && !e.shiftKey) {
				//$("#chatter-msg").val("");
				e.preventDefault();
				const button = $('button[data-command="messagereceived"]');
				button.trigger("click");
				return false;
			} else {
				const scrollHeight = $(this).get(0).scrollHeight;
				if (
					!$(".chatterbox").hasClass("chatterlongtext") &&
					scrollHeight > 30
				) {
					$(".chatterbox").addClass("chatterlongtext");
					scrollToChat();
				}
			}
		});

		lQuery('button[data-command="messagereceived"]').livequery(
			"click",
			function (e) {
				//$("#chatter-msg").val("");
			},
		);

		lQuery(".chatter-save").livequery("click", function (e) {
			e.preventDefault();
			const button = $(this);
			const form = button.closest(".chatter-edit-form");
			const chatdiv = form.find(".chatter-msg-edit");
			const text = chatdiv.html();
			form.find(".chatter-msg-input").val(text);
			/*var button = $('submit');		    	
    	button.trigger("#submit");*/
			form.trigger("submit");
		});

		lQuery("a.ajax-edit-msg").livequery("click", function (e) {
			e.stopPropagation();
			e.preventDefault();
			const editbtn = $(this);
			const targetDiv = editbtn.data("targetdiv");
			const options = editbtn.cleandata();
			options.oemaxlevel = 1;
			const nextpage = editbtn.attr("href");
			$.get(nextpage, options, function (data) {
				//var cell = findclosest($(this), "#" + targetDiv);
				const cell = editbtn.closest("#" + targetDiv);
				cell.replaceWith(data);
				scrollToEdit(targetDiv);
			});
		});

		// lQuery(".chatterbox-body-inside").livequery("scroll", function (e) {
		// 	if ($(this).scrollTop() < 50) {
		// 		loadMoreChats();
		// 	}
		// });

		lQuery("a.appendgoalbutton").livequery("click", function (e) {
			const parent = $(this).closest(".goalstatusopen");
			if (parent) {
				parent[0].scrollIntoView();
			}
		});
	}

	function scrollToChat() {
		setTimeout(function () {
			const inside = $(".chatterbox-body-inside");
			if (inside.length > 0) {
				inside.animate({ scrollTop: inside.get(0).scrollHeight }, 30);
			}
		});
	}

	function scrollToEdit(targetDiv) {
		const messagecontainer = $("#" + targetDiv);
		if (messagecontainer.length) {
			messagecontainer.get(0).scrollIntoView();
		}
	}

	function connect() {
		if (chatConnection && chatConnection.readyState !== chatConnection.CLOSED) {
			return;
		}
		const tabID =
			sessionStorage.tabID && sessionStorage.closedLastTab !== "2"
				? sessionStorage.tabID
				: (sessionStorage.tabID = Math.random());
		sessionStorage.closedLastTab = "2";
		$(window).on("unload beforeunload", function () {
			sessionStorage.closedLastTab = "1";
		});

		const protocol = location.protocol;

		let url = `/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=${tabID}&userid=${userid}`;

		//Get the channel
		const channel = $(".chatterbox").data("channel");
		if (channel != null) {
			url = `${url}&channel=${channel}`;
		}

		if (protocol === "https:") {
			chatConnection = new WebSocket(`wss://${location.host}${url}`);
		} else {
			chatConnection = new WebSocket(`ws://${location.host}${url}`);
			// console.log(new Date().toISOString(), "Chat initialized with ws");
		}

		chatConnection.addEventListener("message", function (event) {
			// console.info(new Date().toISOString(), "Received message");

			$(window).trigger("ajaxsocketautoreload");
			const message = JSON.parse(event.data);
			const channel = message.channel;
			const chatbox = $(`div.chatterbox[data-channel="${channel}"]`);

			if (message && chatbox.length === 1) {
				//Channel on the screen no need to notify

				channelUpdateMessage(chatbox, message);

				return;
			}

			registerServiceWorker();

			/*Check if you are the sender, play sound and notify. "message.topic != message.user" checks for private chat*/
			if (message.user !== userid && message.user !== "agent") {
				console.log(`Got a message: ${document.hasFocus()}`);
				if (!document.hasFocus()) {
					function showNotification() {
						console.log("Showing notification...");
						let header = "New Message";
						if (message.name !== undefined) {
							header = message.name;
						}
						if (message.topic !== undefined) {
							header += ` in ${message.topic}`;
						}
						let messagebody = message.message;
						if (messagebody !== null && messagebody !== undefined) {
							messagebody = "New message...";
						}
						const notification = new Notification(header, {
							//TODO: URL?
							body: message.message,
							renotify: false,
							tag: messagebody,
							icon: `${apphome}/theme/images/logo.png`,
						});
						notification.addEventListener("click", function (event) {
							//window.open('http://www.mozilla.org', '_blank');
						});
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
					} else {
						console.log(
							`Notification Browser permission:${Notification.permission}`,
						);
					}
				}
			}
		});
		chatConnection.addEventListener("open", function () {
			keepAlive();
			// console.info(new Date().toISOString(), "Chat Connection Opened");
			// console.log("Chat Connection Opened");
		});
		chatConnection.addEventListener("close", function () {
			// console.info(new Date().toISOString(), "Chat Connection Closed");
			// console.log("Chat Connection Closed");
		});
		chatConnection.addEventListener("error", function (event) {
			// console.error(new Date().toISOString(), "Chat Connection Error", event);
		});
	}

	const messages = {};

	function channelUpdateMessage(chatbox, message) {
		//Cancel an existing one
		if (messages[message.messageid]) {
			messages[message.messageid] = setTimeout(function () {
				updateMessage(chatbox, message);
			}, 1000);
		} else {
			messages[message.messageid] = true;
			updateMessage(chatbox, message);
		}
	}

	function updateMessage(chatbox, message) {
		console.info(new Date().toISOString(), message);

		const listarea = chatbox.find(".chatterbox-message-list");
		let url = chatbox.data("rendermessageurl");
		if (!url) {
			url = apphome + "/components/chatterbox/message.html";
		}

		const existing = listarea.find("#chatter-message-" + message.messageid);
		if (existing.length) {
			if (message.command === "messageremoved") {
				existing.remove();
			} else {
				const msgBody = $(existing).find(".msg-body-content");
				if (msgBody.length) {
					msgBody.html(message.message);
				} else {
					const chatMsg = $(existing).find(".chat-msg");
					chatMsg.html(message.message);
				}
			}

			scrollToChat();
			return;
		}

		scrollToChat();

		let options = chatbox.cleandata();
		if (!options) options = {};
		const editdiv = chatbox.closest(".editdiv");
		if (
			chatbox.data("includeeditcontext") === undefined ||
			chatbox.data("includeeditcontext") === true
		) {
			if (editdiv.length > 0) {
				const otherdata = editdiv.cleandata();
				options = {
					...otherdata,
					...options,
				};
			}
		}

		options.id = message.messageid;

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

		$.get(url, options, function (data) {
			listarea.append(data);
			sortChatterbox(chatbox.find(".chatterbox-message-list"));
			scrollToChat();
		});
	}

	function sortChatterbox(container) {
		//var messages = Array.from(container.querySelectorAll(".msg-bubble"));
		const messages = Array.from(container.find(".msg-bubble"));

		messages
			.sort((a, b) => {
				const dateA = new Date(a.dataset.createdat);
				const dateB = new Date(b.dataset.createdat);
				return dateA - dateB;
			})
			.forEach((el) => container.append(el));
		console.log("sorted...");
	}
	/*
lQuery(".chatterbox-message-list").livequery(function () {
	if ($(this).hasClass("observing")) return;
	$(this).addClass("observing");
	var container = this.get(0);
	if (!container) return;
	var observer = new MutationObserver(function () {
		sortChatterbox(container);
	});
	observer.observe(container, {
		childList: true,
	});
});
*/
	/*--------------Begin Functions List--------------*/
	function reloadAll() {
		$(".chatterbox").each(function () {
			const chatter = $(this);
			let url = chatter.data("renderurl");
			if (!url) {
				url = appHome + "/components/chatterbox/index.html";
			}
			const chatterdiv = $(this);
			const mydata = $(this).data();
			$.get(url, mydata, function (data) {
				chatterdiv.html(data);

				scrollToChat();
			});
		});
	}

	function showSpinner() {
		//TODO:  Shakil Add a spinner somewhere to let us know the chat bot is about to say something
		console.log("AI about to respond");
		$(".chatterspinner").show();
		scrollToChat();
	}

	function hideSpinner() {
		//TODO:  Shakil Add a spinner somewhere to let us know the chat bot is about to say something
		console.log("AI done");
		$(".chatterspinner").hide();
	}

	function loadMoreChats() {
		//already loading
		if (loadingMore) {
			//console.log("already loading");
			//skip
			return;
		}
		loadingMore = true;

		$(".chatterbox").each(function () {
			const url = appHome + "/components/chatterbox/loadmessages.html";

			const chatterdiv = $(this);
			const mydata = $(this).data();
			mydata.lastloaded = chatterdiv
				.find(".chatterbox-messages")
				.data("lastloaded");
			$.get(url, mydata, function (data) {
				chatterdiv.find(".chatterbox-message-list").prepend(data);
				//scrollToChat();
				loadingMore = false;
				console.log("stop loading now");
			});
		});
	}

	let keepAliveTimeoutID = 0;

	function keepAlive() {
		if (!chatConnection) {
			return;
		}
		const timeout = 20000;
		if (chatConnection.readyState === chatConnection.OPEN) {
			const command = {};
			command.command = "keepalive";

			command.userid = userid;

			const chatter = $(".chatterbox").data("channel");
			command.channel = chatter;

			const json = JSON.stringify(command);
			chatConnection.send(json);
		}

		if (chatConnection.readyState === chatConnection.CLOSED) {
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

	/*-------Start Push and Notification --------*/
	const pushServerPublicKey =
		"BIN2Jc5Vmkmy-S3AUrcMlpKxJpLeVRAfu9WBqUbJ70SJOCWGCGXKY-Xzyh7HDr6KbRDGYHjqZ06OcS3BjD7uAm8";

	function registerServiceWorker() {
		if (navigator.serviceWorker !== undefined) {
			navigator.serviceWorker.register(
				appHome + "/components/chatterbox/sw.js",
			);
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
		const $this = $(this);
		setTimeout(function () {
			$this.trigger("focus");
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
		const emojiparsed = $(this).data("emojiparsed");
		if (emojiparsed) {
			return;
		}
		$(this).data("emojiparsed", true);
		const msgContent = $(this).find(".msg-body-content");
		const reacts = $(this).find("span.emote");
		if (window.parseEmojis !== undefined) {
			if (msgContent.length > 0) {
				window.parseEmojis(msgContent[0]);
			}
			if (reacts.length > 0) {
				window.parseEmojis(reacts[0]);
			}
		}
	});

	let chatSelectionStart = null;

	lQuery(".chatter-emoji").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		let textarea = $("#emojipicker").data("textarea");
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
		const goTo = $(this).data("id");
		if (goTo === "smileys") {
			$(".emoji-wrapper").animate({ scrollTop: 0 }, 500);
			return;
		}
		$(".emoji-wrapper").scrollTop(0);
		const dest =
			$("#" + goTo).offset().top -
			$("#" + goTo)
				.offsetParent()
				.offset().top;
		$(".emoji-wrapper").animate({ scrollTop: dest - 70 }, 500);
	});

	lQuery(".emjbtn").livequery("click", function () {
		let textarea = $("#emojipicker").data("textarea");
		if (textarea) {
			textarea = $("#" + textarea);
		} else {
			textarea = $("#chatter-msg");
		}

		const emoji = $(this).text();
		let prev = textarea.val() || "";
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
			textarea.trigger("focus");
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
		},
	);

	lQuery("a.lightbox").livequery(function () {
		const slb = $(this).simpleLightbox({
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
			const lang = document.documentElement.lang;
			if (lang) {
				let locales = $(this).data("locales");
				if (locales) {
					if (typeof locales === "string") {
						const txt = document.createElement("textarea");
						txt.innerHTML = locales;
						locales = JSON.parse(txt.value);
					}
					const localeCaption = locales[lang];
					if (localeCaption) {
						$(".sl-caption").html(localeCaption);
					}
				}
			}

			const dl = $(this).data("downloadlink");
			const al = $(this).data("assetlink");
			if (dl || al) {
				$(".simple-lightbox .sl-actions").remove();
				$(".simple-lightbox").append("<div class='sl-actions'></div>");

				if (dl) {
					$(".simple-lightbox .sl-actions").append(
						"<a class='sl-btn sl-dl' href='" + dl + "' target='_blank'></a>",
					);
				}
				//Asset Link

				if (al) {
					$(".simple-lightbox .sl-actions").append(
						"<a class='sl-btn sl-al' href='" + al + "' target='_blank'></a>",
					);
				}
			}
		});
	});
});
