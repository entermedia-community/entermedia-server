var FabricModel = function (scope) {
	var _this = null;
	var out = {
		canvas: null,
		scope: scope,
		lastClick: {
			left:0,
			top:0
		},
		findType: function(name) {
			var retval = null;
			$.each(_this.toolkit, function(index, item) {
				if (item.name == name) {
					retval = item;
					return false;
				}
			});
			return retval;
		},
		toolkit: [
			{
				name: 'disabled',
				properties: {
					isDrawingMode: false
				},
				annotating: false
			}, {
				name: 'draw',
				properties: {
					isDrawingMode: true
				},
				annotating: true,
				whenSelected: function() {
			      _this.canvas.freeDrawingBrush.color = scope.userColor;
			      _this.canvas.freeDrawingBrush.width = 3;
				}
			}, {
				name: 'move',
				properties: {
					isDrawingMode: false
				},
				annotating: false,
				whenSelected: function() {
					$.each(scope.annotationEditor.currentAnnotatedAsset.annotations, function(index, annotation)
					{
						$.each(annotation.fabricObjects, function(index, object)
						{
							// this makes object selectable
							object.selectable = true;
							// this makes object mobile
							object.evented = true;
							object.lockMovementX = object.lockMovementY = false;

						});
					});
					scope.fabricModel.canvas.calcOffset();
					scope.fabricModel.canvas.renderAll();
				},
				whenDeselected: function() {
					$.each(scope.annotationEditor.currentAnnotatedAsset.annotations, function(index, annotation)
					{
						$.each(annotation.fabricObjects, function(index, object)
						{
							// this makes object unselectable
							object.selectable = false;
							//this makes object immobile
							object.evented = false;
							object.lockMovementX = object.lockMovementY = true;
						});
					});
					scope.fabricModel.canvas.calcOffset();
					scope.fabricModel.canvas.renderAll();
				}
			}, {
				name: 'shape',
				properties: {
					isDrawingMode: false
				},
				annotating: true,
				type: 'circle',
				types: [
					{
						name: 'circle',
						type: fabric.Circle,
						blank: {
							radius: 2,
							strokeWidth: 3,
							selectable: false,
							//hasControls: false,
							//evented:true,
							fill: "",
							originX: 'left',
							originY: 'top',
							// lockScalingX: true,
							// lockScalingY: true,
							lockMovementX: true,
							active:true,
							lockMovementY: true
						},
						drawparams: function(pointer) {
							var initX = _this.lastClick.left;
							var initY = _this.lastClick.top;
							var curX = pointer.x;
							var curY = pointer.y;
							// pretend we're drawing a rectangle which has a top/left of either initX/Y or pointerX/Y
							// pythagorean theorem states that:
							var a = curX - initX;
							var b = curY - initY;
							var c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
							var radius = c / 2;
							// this is a basic 2d distance formula and is always true regardless of where the circle is
							// now that we have the size of the circle, it needs to be positioned correctly
							var midX = initX + a/2;
							var midY = initY + b/2;
							
							return {
								radius: radius,
								top: midY - radius,
								left: midX - radius
								}
						}
					}, {  //(target && target.selectable && !shouldGroup
						name: 'rectangle',
						type: fabric.Rect,
						blank: {
							height: 5,
							width: 5,
							strokeWidth: 3,
							selectable: true,
							fill: "",
							originX: 'left',
							originY: 'top',
							lockScalingX: true,
							lockScalingY: true,
							lockMovementX: true,
							lockMovementY: true
						},
						drawparams: function(pointer) {
							var lastClick = _this.lastClick;
							return {
								width: -lastClick.left + pointer.x,
								height: -lastClick.top + pointer.y
							};
						}
					}
				],
				events: {
					mouseup: function(e, canvas) {
						/*
						 could fire object:modified on the canvas
						 tricky bit is making sure the object we pass
						 in is our shape and not some other object.
						 It's probably always the right object though.
						*/
						//Just for circles
						
						var annotation =  scope.annotationEditor.currentTool.currentAnnotation;
						if( annotation )
						{
							scope.annotationEditor.notifyAnnotationModified(annotation);
						}
		
					},
					mousedown: function(e, canvas) {
						var pointer, shape, spec, type, we;
						pointer = canvas.getPointer(e.e);
						we = _this.findType('shape');
						type = _.findWhere(scope.annotationEditor.currentTool.types, {
							name: scope.annotationEditor.currentTool.type
						});
						spec = type.blank;
						spec.stroke = scope.userColor;
						spec.left = pointer.x;
						spec.top = pointer.y;
						shape = new type.type(spec);
						canvas.add(shape);
						
					},
					//objectadded: null,
					mousemove: function(e, canvas) {
						var pointer, shape, type, we;
						pointer = canvas.getPointer(e.e);
						var mouseDown = e.e.which === 1;
						if (mouseDown) {
							we = _this.findType('shape');
							shape = canvas.getObjects()[canvas.getObjects().length - 1];
							type = _.findWhere(scope.annotationEditor.currentTool.types, {
								name: scope.annotationEditor.currentTool.type
							});
							//_performTransformAction
							if (shape)
							{
							shape.set(type.drawparams(pointer));
							//canvas._transformObject(e);
							canvas.renderAll();
							}
						}
						
					}
				}
			}, {
				name: 'comment',
				properties: {
					isDrawingMode: false
				},
				annotating: true,
				events: {
					mouseup: null,
					mousedown: null,
					objectadded: null
				}
			}, {
				name: 'arrow',
				properties: {
					isDrawingMode: false
				},
				annotating: true
			}, {
				name: 'text',
				properties: {
					isDrawingMode: false
				},
				annotating: true
			}, {
				name: 'zoom',
				properties: {
					isDrawingMode: false
				},
				annotating: false,
				events: {
					mouseup: null,
					mousemove: function(e, canvas) {
						var SCALE_FACTOR, delta, klass, objects, pointer, transform, _i, _len, mouseDown;
						mouseDown = e.e.which === 1;
						if (mouseDown) {
							SCALE_FACTOR = 0.01;
							pointer = canvas.getPointer(e.e);
							delta = scope.left - pointer.x;
							objects = canvas.getObjects();
							delta = delta * SCALE_FACTOR;
							transform = [1 + delta, 0, 0, 1 + delta, 0, 0];
							// console.log(transform);
							for (_i = 0, _len = objects.length; _i < _len; _i++) {
								klass = objects[_i];
								klass.transformMatrix = transform;
								klass.setCoords();
							}
							canvas.backgroundImage.transformMatrix = transform;
							canvas.setWidth(canvas.backgroundImage.width * canvas.backgroundImage.transformMatrix[0]);
							canvas.setHeight(canvas.backgroundImage.height * canvas.backgroundImage.transformMatrix[3]);
						}
					},
					mousedown: function(e, canvas) {
						_this.lastClick.left = e.e.x;
						_this.lastClick.top = e.e.y;
					}
				}
			}, {
				name: 'colorpicker',
				properties: {},
				annotating: false
			}, {
				name: 'load',
				properties: {},
				annotating: false
			}, {
				name: 'export',
				properties: {},
				annotating: false
			}
		],
		setBackgroundImage: function(inPath) {
			fabric.util.loadImage(inPath, function(src) {
				var center, realImage, canvas;
				canvas = _this.canvas;
				realImage = new fabric.Image(src);
				canvas.setWidth(realImage.width);
				canvas.setHeight(realImage.height);
				// center = canvas.getCenter();
				canvas.setBackgroundImage(realImage, canvas.renderAll.bind(canvas));
			});
			
		},
		selectTool: function(toolname,type) {
		  var prop;
		  
		  var selected = type;
		  if( !selected)
		  {
		  	selected = toolname;
		  }
		  $(".annotations-toolbar li").removeClass("active");
		  $(".annotations-toolbar #" + selected + "tool").addClass("active");
		  
		  // if (scope.readyToComment != null) {
		  if (true) {
		    if (scope.annotationEditor.currentTool !== null && typeof(scope.annotationEditor.currentTool.whenDeselected) !== "undefined") {
		    	scope.annotationEditor.currentTool.whenDeselected();
		    }
		    scope.annotationEditor.currentTool = _.findWhere(scope.fabricModel.toolkit, {
		      name: toolname
		    });
		    for (prop in scope.annotationEditor.currentTool.properties) {
		      _this.canvas[prop] = scope.annotationEditor.currentTool.properties[prop];
		    }
		    if (typeof(scope.annotationEditor.currentTool.whenSelected) !== "undefined") {
		    	scope.annotationEditor.currentTool.whenSelected();
		    }
		  }
		  
		},
		setShapeTypeFromUi: function(shapename) {
			_this.selectTool('shape',shapename);
			_this.scope.annotationEditor.currentTool.type = shapename;
		},
		clearCanvas: function() {
			_this.canvas.clear();
			// _this.scope
		}
	} // end of out definition

	_this = out; // worth a try

	(function() {
		var canvas = _this.__canvas = new fabric.Canvas('annotation_canvas');
		canvas.on("after:render", function() {
			canvas.calcOffset();
		});
		// _this.setBackgroundImage(inPath);
		out.canvas = canvas;
	})();
	(function() {
		fabric.util.addListener(fabric.window, 'load', function() {
			var canvas, canvases, _i, _ref;
			canvas = _this.__canvas || _this.canvas;
			canvases = _this.__canvases || _this.canvases;
			canvas && canvas.calcOffset && canvas.calcOffset();
			if (canvases && canvases.length) {
				for (_i = 0, _ref = canvases.length; 0 <= _ref ? _i <= _ref : _i >= _ref; 0 <= _ref ? _i++ : _i--) {
					canvases[i].calcOffset();
				}
			}
		})
	})();

   readyToComment = function() {
      //scope.fabricModel.selectTool('disabled');
      //scope.fabricModel.readyToComment = true;
      //scope.fabricModel.canvas.isDrawingMode = false;
      
     /*
      $timeout((function() {
        $('#user-comment-input').focus();
        return em.unit;
      }), 100);
      $('.upper-canvas').css({
        'background': 'rgba(255,255,255,0.7)'
      });
      return em.unit;
    */  
    };
 

	out.canvas.on('mouse:down', function(e) {
		var pointer, _ref;
		pointer = _this.canvas.getPointer(e.e);
		// scope.mouseDown = true;
		_this.lastClick.left = pointer.x;
		_this.lastClick.top = pointer.y;
		if (!scope.readyToComment) {
			if (scope.annotationAction !== null) {
				// console.log('canceling annotationAction');
				clearTimeout(scope.annotationAction);
			}
			// console.log('DOING SOMETHING!!! Something is wrong with disabled tool/ offset calculations');
			if ((_ref = scope.annotationEditor.currentTool.events) != null) {
				if (typeof _ref.mousedown === "function") {
					_ref.mousedown(e, _this.canvas);
				}
			}
		}
		
	});



	out.canvas.on('mouse:up', function(e) {
		var _ref;
		// mouse down: e.e.which == 1
		// mouse not down: e.e.which == 0
		if (scope.annotationEditor.currentTool.annotating && !scope.readyToComment) {
			if (scope.annotationEditor.currentTool.name === 'comment') {
				// console.log('Calling readyToComment() now');
				readyToComment();
			} else {
				// console.log('Calling delayed readyToComment()');
				scope.annotationAction = setTimeout(readyToComment, 1000);
			}
		}
		if ((_ref = scope.annotationEditor.currentTool.events) != null) {
			if (typeof _ref.mouseup === "function") {
				_ref.mouseup(e, _this.canvas);
				
				/*
				var objects = _this.canvas.getObjects();
				for (_i = 0, _len = objects.length; _i < _len; _i++) {
					var klass = objects[_i];
					klass.setCoords();
				}
				*/
				
			}
		}
		scope.annotationEditor.fabricModel.selectTool("move");
		
		
		
	});

	out.canvas.on('mouse:move', function(e) {
		var _ref;
		if ((_ref = scope.annotationEditor.currentTool.events) != null) {
			if (typeof _ref.mousemove === "function") {
				_ref.mousemove(e, _this.canvas);
			}
		}
		
	});

	out.canvas.on('object:modified', function(e)
	{
		var object = e.target;
		if( object && object.annotationid)
		{
			var annotation = scope.annotationEditor.currentAnnotatedAsset.getAnnotationById(object.annotationid);
			//TODO loop over all the objects looking for more canvas objects. annotation
			annotation.fabricObjects[0] = object;
			scope.annotationEditor.notifyAnnotationModified(annotation);
		}
	});

	out.canvas.on('object:selected', function(e)
	{
		var object = e.target;
		if( object && object.annotationid)
		{
			scope.annotationEditor.selectAnnotation(object.annotationid);
		}
	})
	
	out.canvas.on('selection:cleared', function(e)
	{
		//var object = e.target;
		//if( object && object.annotationid)
		//{
			scope.annotationEditor.deselectAnnotation();
		//}
	})

	out.canvas.on('object:added', function(obj) {
		var _ref;
		
		if( obj.source != this )
		{
			return;
		}
		
		if (scope.annotationEditor.currentTool.annotating) {
			var annotation = scope.annotationEditor.fabricObjectAdded(obj.target);
			
			obj.target.annotationid = annotation.id;
			scope.annotationEditor.currentTool.currentAnnotation = annotation;
			scope.annotationEditor.notifyAnnotationAdded(annotation);
		}
		if ((_ref = scope.annotationEditor.currentTool.events) != null) {
			if (typeof _ref.objectadded === "function") {
				_ref.objectadded(obj, _this.canvas);
			}
		}
		// _this.canvas.calcOffset();
		// _this.canvas.renderAll();
		if (!_this.lastClick.left) {
			_this.lastClick.left = obj.target.left;
		}
		if (!_this.lastClick.top) {
			_this.lastClick.top = obj.target.top;
		}
		
	});

	return out;

}
