// Only used in mobile view at the moment
(function ($) {
  $.fn.drawer = function () {
    var button = $(this);

    var drawerTarget = button.data("target");
    if (!drawerTarget) drawerTarget = "#mobileDrawer";
    var drawer = $(drawerTarget);
    if (drawer.length == 0) {
      console.error("Could not find drawer element: #" + drawerTarget);
      return;
    }
    var contentSource = button.data("source");
    var sourceElement = $(contentSource);
    if (sourceElement.length == 0) {
      console.error("Could not find source element: " + contentSource);
      return;
    }
    var placement = button.data("placement");
    if (placement) {
      drawer.attr("class", "drawer drawer-" + placement);
    }
    var title = button.data("title");
    if (title && title.length > 0) {
      drawer.find(".drawer-title").html(title);
    }
    var content = sourceElement.html();
    if (content.length > 0) {
      drawer.find(".drawer-body").html(content);
    } else {
      drawer.find(".drawer-body").html("Nothing to show");
    }
    drawer.addClass("open");
    $(document.body).append(
      '<div class="drawer-backdrop fade show" role="presentation"></div>'
    );
    drawer.css("visibility", "visible");
    drawer.focus();
    if (drawer.width() < 420) {
      drawer.css("width", $(window).width() - 20);
    } else {
      drawer.css("width", 400);
    }
  };
})(jQuery);

$(document).ready(function () {
  lQuery(".emdrawer").livequery("click", function (e) {
    e.preventDefault();
    e.stopPropagation();
    $(this).drawer();
  });
  function closeDrawer(drawer = null) {
    if (!drawer) drawer = $(".drawer");
    drawer.removeClass("open");
    setTimeout(function () {
      drawer.css("visibility", "hidden");
    }, 300);
    drawer.find(".drawer-title").html("");
    drawer.find(".drawer-body").html("");
    $(".drawer-backdrop").fadeOut(200, function () {
      $(this).remove();
    });
  }
  lQuery("#drawerClose").livequery("click", function () {
    var drawer = $(this).closest(".drawer");
    closeDrawer(drawer);
  });

  lQuery(".drawer-backdrop").livequery("mousedown", function () {
    closeDrawer();
  });

  lQuery(".drawer a, .drawer button").livequery("click", function (e) {
    closeDrawer($(this).closest(".drawer"));
  });
});
