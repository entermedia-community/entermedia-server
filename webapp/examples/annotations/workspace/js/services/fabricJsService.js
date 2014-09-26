Workspace.factory('fabricJsService', function() {
  var globscale;
  globscale = 1;
  return {
    init: function(path) {
      var returnCanvas;
      returnCanvas = {};
      (function() {
        var canvas, docGet;
        docGet = function(id) {
          return document.getElementById(id);
        };
        canvas = this.__canvas = new fabric.Canvas('annotation_canvas');
        canvas.on("after:render", function() {
          return em.unit;
        });
        fabric.util.loadImage(path, function(src) {
          var center, realImage;
          realImage = new fabric.Image(src);
          canvas.setWidth(realImage.width);
          canvas.setHeight(realImage.height);
          center = canvas.getCenter();
          canvas.setBackgroundImage(realImage, canvas.renderAll.bind(canvas));
          return em.unit;
        });
        returnCanvas = canvas;
        return em.unit;
      })();
      (function() {
        return fabric.util.addListener(fabric.window, 'load', function() {
          var canvas, canvases, _i, _ref;
          canvas = this.__canvas || this.canvas;
          canvases = this.__canvases || this.canvases;
          canvas && canvas.calcOffset && canvas.calcOffset();
          if (canvases && canvases.length) {
            for (_i = 0, _ref = canvases.length; 0 <= _ref ? _i <= _ref : _i >= _ref; 0 <= _ref ? _i++ : _i--) {
              canvases[i].calcOffset();
            }
          }
          return em.unit;
        });
      })();
      return {
        canvas: returnCanvas
      };
    }
  };
});
