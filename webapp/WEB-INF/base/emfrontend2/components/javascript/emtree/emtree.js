$(document).ready(function () {
	//Open and close the tree
	lQuery(".emtree-widget ul li div .cat-arrow").livequery(
		"click",
		function (e) {
			e.stopPropagation();
			toggleExpandNode.call(this);
		},
	);
	function toggleExpandNode(selecting = false) {
		console.log($(this), selecting);
		var tree = $(this).closest(".emtree");
		var node = $(this).closest(".noderow");
		var iscurrent = $(this).hasClass("cat-current");
		var nodeid = node.data("nodeid");
		var depth = node.data("depth");
		tree.find("ul li div").removeClass("selected");

		var home = $(this).closest(".emtree").data("home");

		if ($(this).find(".cat-arrow").hasClass("down")) {
			$(this).find(".cat-arrow").removeClass("down");
		} else {
			//Open it. add a UL
			$(this).find(".cat-arrow").addClass("down");
		}

		tree.find(nodeid + "_add").remove();
		node.load(
			home +
				"/components/emtree/tree.html?toggle=true&treename=" +
				tree.data("treename") +
				"&nodeID=" +
				nodeid +
				"&depth=" +
				depth +
				"&canupload=" +
				tree.data("canupload") +
				(selecting ? "&selecting=true" : "") +
				(iscurrent ? "&currentnodeid=" + nodeid : ""),
			function () {
				$(window).trigger("resize");
			},
		);
	}

	//Select a node
	lQuery(".emtree-widget ul li div .cat-name").livequery(
		"click",
		function (event) {
			event.stopPropagation();
			if (
				$(this).hasClass("cat-leaf") &&
				!$(this).parent().hasClass("expanded")
			) {
				toggleExpandNode.call($(this).siblings(".cat-arrow"), true);
			}
			var tree = $(this).closest(".emtree");
			var node = $(this).closest(".noderow");
			$("ul li div.cat-current", tree).addClass("categorydroparea");
			$("ul li div", tree).removeClass("selected cat-current");
			$("div:first", node)
				.addClass("cat-current")
				.removeClass("categorydroparea");
			var nodeid = node.data("nodeid");
			tree.data("currentnodeid", nodeid);
			var prefix = tree.data("urlprefix");
			var maxlevel = tree.data("maxlevelclick");
			if (maxlevel == undefined || maxlevel == "") {
				maxlevel = 2;
			}

			//Regular Tree
			var options = [];
			var resultsdiv = tree.closest(".resultsdiv");
			if (resultsdiv.length) {
				resultsdiv.data("categoryid", nodeid);
				resultsdiv.data("nodeID", nodeid);
			}
			gotopage(tree, node, maxlevel, prefix, options);

			var event = $.Event("emtreeselect");
			event.tree = tree;
			event.nodeid = nodeid;
			$(document).trigger(event);
		},
	);

	lQuery(".scrolltocat").livequery(function () {
		var tree = $(this).closest(".categorytreescrolllable");
		var scrollTo = $(this);
		if (tree.length == 0 || scrollTo.length == 0) return;
		var position =
			scrollTo.offset().top - tree.offset().top + tree.scrollTop() - 100;
		tree.scrollTop(position);
	});

	gotopage = function (tree, node, maxlevel, prefix, inOptions) {
		//postfix not used

		var treeholder = $("div#categoriescontent");
		var toplocation = parseInt(treeholder.scrollTop());
		var leftlocation = parseInt(treeholder.scrollLeft());
		var nodeid = node.data("nodeid");
		var home = tree.data("home");
		var depth = node.data("depth");
		var collectionid = node.data("collectionid");
		var reloadurl = "";
		var appnavtab = $("#appnavtab").data("openmodule");
		if (prefix == undefined || prefix == "") {
			//Asset Module
			reloadurl =
				home +
				"/views/modules/asset/viewfiles/" +
				nodeid +
				"/" +
				node.data("categorynameesc") +
				".html";
			prefix = reloadurl;
			//}
		} else {
			var customprefix = tree.data("customurlprefix");
			if (customprefix) {
				reloadurl = customprefix;
			} else {
				reloadurl = prefix;
				reloadurl = reloadurl + "?nodeID=" + nodeid;
			}
		}

		//reloadurl = reloadurl + "?nodeID="+ nodeid;

		var resultsdiv = tree.closest(".assetresults");
		if (!resultsdiv) {
			resultsdiv = $("#resultsdiv");
		}
		var hitssessionid = resultsdiv.data("hitssessionid");
		if (hitssessionid) {
			reloadurl = reloadurl + "?hitssessionid=" + hitssessionid;
		}

		var topmoduleid = resultsdiv.data("topmoduleid");
		if (!topmoduleid) {
			topmoduleid = "";
		}

		var options = structuredClone(tree.data());

		options["treenameme"] = tree.data("treename");
		options["nodeID"] = nodeid;
		options["treetoplocation"] = toplocation;
		options["treeleftlocation"] = leftlocation;
		options["depth"] = depth;
		options["categoryid"] = nodeid;
		options["rootcategory"] = tree.data("rootnodeid");
		options["topmoduleid"] = topmoduleid;
		options["hitssessionid"] = hitssessionid;

		if (collectionid) {
			options.collectionid = collectionid;
		}
		var searchchildren = tree.data("searchchildren");
		if (appnavtab == "asset") {
			//for now
			searchchildren = true;
		}
		options.searchchildren = searchchildren;

		if (inOptions["oemaxlevel"]) {
			options.oemaxlevel = inOptions["oemaxlevel"];
		}

		$(window).trigger("showLoader");

		//jQuery.get(prefix + nodeid + postfix,
		jQuery
			.get(prefix, options, function (data) {
				//data = $(data);

				var targetdiv = tree.data("targetdivinner");
				var onpage;
				if (targetdiv) {
					var cell = jQuery("#" + targetdiv);
					onpage = cell;
					cell.html(data);
				} else {
					targetdiv = tree.data("targetdiv");
					if (targetdiv) {
						var cell = jQuery("#" + targetdiv);
						onpage = cell.parent();
						cell.replaceWith(data);
					}
				}

				cell = findclosest(onpage, "#" + targetdiv);

				$(window).trigger("setPageTitle", [cell]);

				if (
					typeof global_updateurl !== "undefined" &&
					global_updateurl == false
				) {
					//globaly disabled updateurl
				} else {
					//Update Address Bar
					if (tree.data("updateaddressbar")) {
						history.pushState($("#application").html(), null, reloadurl);
					}
				}

				$(window).trigger("resize");
			})
			.always(function () {
				$(window).trigger("hideLoader");
			});
	};

	var treeTop = $(".cat-current");
	if (treeTop.length) {
		$("div#treeholder").scrollTop(parseInt(treeTop.offset().top));
	}

	//need to init this with the tree
	lQuery("div#treeholder").livequery(function () {
		var treeholder = $(this);
		var top = treeholder.data("treetoplocation");
		if (top) {
			var left = treeholder.data("treeleftlocation");
			var catcontent = $("div#categoriescontent");
			catcontent.scrollTop(parseInt(top));
			catcontent.scrollLeft(parseInt(left));
		}
	});

	lQuery("#treeholder input").livequery("click", function (event) {
		event.stopPropagation();
	});

	lQuery("#treeholder input").livequery("keyup", function (event) {
		var input = $(this);
		var node = input.closest(".noderow");
		var tree = input.closest(".emtree");
		var value = input.val();
		//console.log("childnode",node);
		var nodeid = node.data("nodeid");
		if (event.keyCode == 13) {
			//13 represents Enter key
			var action = input.data("action");
			if (action != "create") {
				action = "rename";
			}
			var rootid = tree.data("treename") + "root";
			var link =
				tree.data("home") +
				"/components/emtree/savenode.html?action=" +
				action +
				"&treename=" +
				tree.data("treename") +
				"&" +
				rootid +
				"=" +
				tree.data("rootnodeid") +
				"&depth=" +
				node.data("depth");

			var targetdiv = tree.closest("#treeholder");

			if (action == "rename" && nodeid != undefined) {
				link = link + "&nodeID=" + nodeid;
				link = link + "&adding=true";

				targetdiv = node;
			} else {
				node = node.parent(".noderow");
				nodeid = node.data("nodeid");
				if (nodeid != undefined) {
					link = link + "&parentNodeID=" + nodeid;
				}
				var currentnodeid = tree.data("currentnodeid");
				if (currentnodeid) {
					link = link + "&currentnodeID=" + currentnodeid;
				}
			}
			//tree.closest("#treeholder").load(link, {edittext: value}, function() {
			var options = tree.data();
			options["edittext"] = value;
			$.get(link, options, function (data) {
				targetdiv.replaceWith(data);
				//Reload tree in case it moved order
				//repaintEmTree(tree);
			});
		} else if (event.keyCode === 27) {
			//esc
			input.closest(".createnodetree").hide();
		}
	});

	getNode = function (clickedon) {
		var clickedon = $(clickedon);
		var contextmenu = $(clickedon.closest(".treecontext"));
		var node = contextmenu.data("selectednoderow");
		if (!node) {
			node = $(clickedon).closest(".noderow");
		}
		contextmenu.hide();
		return node;
	};
	lQuery(".treecontext #nodeproperties").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data("nodeid");
		var link =
			tree.data("home") +
			"/views/modules/category/edit/editdialog.html?categoryid=" +
			nodeid +
			"&id=" +
			nodeid +
			"&viewid=categorygeneral&viewpath=category/categorygeneral";
		$(this).attr("href", link);
		$(this).data("dialogid", "categoryproperties");
		emdialog($(this), event);
		//document.location = link;
		return false;
	});

	lQuery(".treedesktopdownload").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var categoryid = node.data("nodeid");
		if (categoryid == null) {
			categoryid = $(this).data("categoryid");
		}
		listCategoryAssets($(this), event, categoryid);
	});

	lQuery(".treecontext #addmedia, .cat-uploadfromtree").livequery(
		"click",
		function (event) {
			event.stopPropagation();
			var node = getNode(this);
			var nodeid = node.data("nodeid");
			var tree = node.closest(".emtree");
			var maxlevel = 2;

			//clear other entities on Upload Form
			var options = [];

			var customurladdmedia = tree.data("customurladdmedia");
			if (customurladdmedia) {
				var url = customurladdmedia;
				var maxlevel = 1;
				gotopage(tree, node, maxlevel, url, options);
			} else {
				var url = tree.data("home") + "/views/modules/asset/add/start.html";
				var maxlevel = 1;
				//options["oemaxlevel"] = $(this).data("oemaxlevel");
				options["oemaxlevel"] = tree.data("uploadmaxlevel");
				options["sidebarcomponent"] = "categories";
				gotopage(tree, node, maxlevel, url, options);
			}
			$(".treerow").removeClass("cat-current").addClass("categorydroparea");
			$("#" + nodeid + "_row > .treerow")
				.addClass("cat-current")
				.removeClass("categorydroparea");

			return false;
		},
	);

	lQuery(".addtomodule").livequery("click", function (event) {
		event.stopPropagation();

		var link = $(this);
		var node = getNode(this);
		var nodeid = node.data("nodeid");
		var tree = node.closest(".emtree");

		link.data("copyingcategoryid", nodeid);

		emdialog(link, event);

		return false;
	});

	lQuery(".treecontext #renamenode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data("nodeid");
		var link =
			tree.data("home") +
			"/components/emtree/rename.html?treename=" +
			tree.data("treename") +
			"&nodeID=" +
			nodeid +
			"&depth=" +
			node.data("depth");
		node.find("> .categorydroparea").load(link, function () {
			node.find("input").select().focus();
		});
		return false;
	});
	lQuery(".treecontext #deletenode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data("nodeid");
		var agree = confirm("Are you sure you want to delete?");
		if (agree) {
			//console.log("removing",node, nodeid);
			var link =
				tree.data("home") +
				"/components/emtree/delete.html?treename=" +
				tree.data("treename") +
				"&nodeID=" +
				nodeid +
				"&depth=" +
				node.data("depth");
			var options = tree.data();
			$.get(link, options, function (data) {
				//tree.closest("#treeholder").replaceWith(data);
				//Reload tree in case it moved order
				repaintEmTree(tree);
			});
		}
		return false;
	});
	lQuery(".treecontext #createnode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var link =
			tree.data("home") +
			"/components/emtree/create.html?treename=" +
			tree.data("treename") +
			"&depth=" +
			node.data("depth");
		$.get(link, function (data) {
			node.append(data);
			var theinput = $("#treeaddnodeinput");
			if (theinput.length > 0) {
				theinput.focus({ preventScroll: false });
			}
			$(document).trigger("domchanged");
		});
		return false;
	});

	lQuery(".createfoldertree").livequery("click", function (event) {
		event.stopPropagation();
		var link = $(this);

		var tree = $("#" + link.data("tree"));

		var node = tree.find(".rootnoderow");

		var link =
			tree.data("home") +
			"/components/emtree/create.html?treename=" +
			tree.data("treename") +
			"&depth=" +
			node.data("depth");
		$.get(link, function (data) {
			node.append(data);
			var theinput = node.find("input");
			theinput.focus({ preventScroll: false });
			//theinput.select();
			theinput.focus();
			$(document).trigger("domchanged");
		});
		return false;
	});

	lQuery(".treecontext #createcollection").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var nodeid = node.data("nodeid");

		var tree = node.closest(".emtree");
		var link =
			tree.data("home") +
			"/views/modules/librarycollection/createcollection.html";

		var catoptions = node.data();

		link =
			link +
			"?oemaxlevel=3&searchtype=librarycollection&field=rootcategory&rootcategory.value=" +
			catoptions.nodeid +
			"&field=name&name.value=" +
			catoptions.categoryname;

		var targetdiv = "application";

		$.get(link, function (data) {
			var cell = jQuery("#" + targetdiv);
			cell.replaceWith(data);

			//node.append(data);
			var theinput = node.find("input");
			theinput.focus({ preventScroll: false });
			//theinput.select();
			//theinput.focus();
			$(document).trigger("domchanged");
		});
		return false;
	});

	lQuery(".treecontext #downloadnode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var nodeid = node.data("nodeid");
		var catname = node.data("categorynameesc");

		var tree = node.closest(".emtree");

		var link =
			tree.data("home") +
			"/views/modules/asset/downloads/bycategory/" +
			nodeid +
			"/" +
			catname +
			".zip";
		window.location.href = link;
		return false;
	});

	function getPosition(e) {
		console.log(e);
		var posx = 0;
		var posy = 0;

		if (!e) var e = window.event;

		if (e.clientX || e.clientY) {
			posx = e.clientX;
			posy = e.clientY;
		} else if (e.pageX || e.pageY) {
			posx = e.pageX;
			posy = e.pageY;
		}

		return {
			x: posx,
			y: posy,
		};
	}

	var contextmenu = function (item, e) {
		var noderow = item;
		//var noderow = $(this); // LI is the think that has context .find("> .noderow");
		$(".categorydroparea").removeClass("selected");
		noderow.find("> .categorydroparea").addClass("selected");
		var emtreediv = noderow.closest(".emtree");
		var treename = emtreediv.data("treename");
		var contextMenu = $("#" + treename + "contextMenu");
		if (contextMenu.length > 0) {
			e.preventDefault();
			var pos = getPosition(e);
			var xPos = pos.x;
			if (xPos < 16) {
				xPos = xPos + 16;
			}
			var yPos = pos.y - 10;

			contextMenu.data("selectednoderow", noderow);
			var collectionid = noderow.data("collectionid");
			if (collectionid) {
				$("#" + treename + "contextMenu #createcollection").hide();
			} else {
				$("#" + treename + "contextMenu #createcollection").show();
			}

			contextMenu.css({
				display: "block",
				left: xPos,
				top: yPos,
			});
			e.stopPropagation();
			return false;
		}
	};

	$("body").on("contextmenu", ".noderow", function (e) {
		contextmenu($(this), e);
	});
	lQuery(".cat-menu").livequery("click", function (e) {
		contextmenu($(this).closest(".noderow"), e);
	});

	lQuery("body").livequery("click", function () {
		var $contextMenu = $(".treecontext");
		$contextMenu.hide();
		$(".categorydroparea").removeClass("selected");
	});

	$(document).keydown(function (e) {
		switch (e.which) {
			case 27: // esc
				var $contextMenu = $(".treecontext");
				if ($contextMenu.length) {
					$contextMenu.hide();
					$(".categorydroparea").removeClass("selected");
					e.preventDefault();
				}
				break;
			default:
				return; // exit this handler for other keys
		}
	});

	//end document ready
});

repaintEmTree = function (tree) {
	var home = tree.data("home");

	//	<div id="${treename}tree" class="emtree emtree-widget" data-home="$siteroot$apphome" data-treename="$treename" data-rootnodeid="$rootcategory.getId()"
	//		data-editable="$editable" data-url-prefix="$!prefix" data-url-postfix="$!postfix" data-targetdiv="$!targetdiv"
	//		>
	/*	var options = { 
			"treename": tree.data("treename"),
			"url-prefix":tree.data("urlprefix"),
			"url-postfix":tree.data("urlpostfix"),
			"targetdiv":tree.data("targetdiv"),
			"maxlevelclick":tree.data("maxlevelclick")
		};
*/

	var link = home + "/components/emtree/tree.html";
	var options = tree.data();
	options["treename"] = tree.data("treename"); //why?
	$.get(link, options, function (data) {
		tree.closest("#treeholder").replaceWith(data);
		$(document).trigger("domchanged");
	});
};
