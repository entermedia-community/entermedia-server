
//Controller
var AnnotationEditor = function(scope) {
	
	var out = {
		currentAnnotatedAsset : null,
		fabricModel: null,
		scope : scope,
		userData: null,
		imageCarouselPageAssetCount: 8,
		imageCarouselPageIndex: 1,
		currentTool: null,
		connection: null,
		colorPicker: null,
		loadSelectors  : function()
		{
			var editor = this;
			lQuery(".colorpicker-input").livequery(function()
			{
				var picker = $(this);
				var dialog = picker.colorpicker(
					{
						autoOpen:false,closeOnOutside:false,
						select: function(event,color) {
							var val = color.formatted;
							scope.userColor = "#" + val;
						},
						close: function(event,color) {
							jAngular.render("#colortoolbararea", scope);
							var tool = scope.annotationEditor.currentTool.name;
							var type = scope.annotationEditor.currentTool.type;
							
							scope.annotationEditor.fabricModel.selectTool(tool,type);
						}
					});
				out.colorPicker = dialog;
			});
		}
		,
		showColorPicker: function()
		{
			this.colorPicker.colorpicker('open');
		}
		,
		loadModels : function()
		{
			var scope = this.scope;

			loadFabricModel(scope);
			
			// load asset data
			this.connect();

		}
		,
		removeAnnotation: function(annotationid)
		{
			var editor = this;
			
			var annotationToRemove = editor.currentAnnotatedAsset.getAnnotationById(annotationid);
			this.notifyAnnotationRemoved(annotationToRemove);
					
		}
		,
		toggleCommentEdit: function(annotationid)
		{
			var annotation = this.currentAnnotatedAsset.getAnnotationById(annotationid);
			var html = $("#annotation-template").html();
			
			$("#annotation" + annotationid).html(html); //replace div
			var localscope = this.scope.createScope();
			localscope.annotation = annotation;
			jAngular.render("#annotation" + annotationid, localscope);
		}
		,
		saveComment: function(annotationid)
		{
			var annotation = this.currentAnnotatedAsset.getAnnotationById(annotationid);
		
			var comment = $("#annotation" + annotationid + " .user-comment-input").val();
		
			annotation.comment = comment;
			
			//update UI?
			
			//jAngular.render("#annotationlist", scope); //changed
			this.notifyAnnotationModified(annotation);
		
		}
		,
		cancelComment: function(annotationid)
		{
			jAngular.render("#annotationlist", scope); //changed
		}
		,
		createAnnotatedAsset: function(inAssetJson)
		{
			var aa = new AnnotatedAsset();
			aa.assetData = inAssetJson.assetData;
			aa.scope = scope;
			aa.annotationIndex = inAssetJson.annotations.length;
			aa.annotations = [];
			
			//Loop over annotations and make objects
			$.each(inAssetJson.annotations, function(index, annotationData)
			{
				aa.annotations.push(new Annotation(annotationData));
			})			
			return aa;
		}
		,
		setCurrentAnnotatedAsset: function(annotatedAssetData)
		{
			this.currentAnnotatedAsset = this.createAnnotatedAsset(annotatedAssetData);
			var url = this.scope.apphome + "/views/modules/asset/downloads/preview/extralarge/" + this.currentAnnotatedAsset.assetData.sourcepath + "/image.jpg";
			
			this.fabricModel.clearCanvas();

			this.fabricModel.setBackgroundImage(url);

		},
		renderAnnotatedAsset: function(inAnnotatedAsset)
		{	
			var editor = this;
			// need to go through annotations and put them on the newly instantiated canvas
			// this is safe to do now because the only place this is called is in loading from network

			this.scope.annotations = inAnnotatedAsset.annotations;
			editor.fabricModel.clearCanvas();

			$.each(inAnnotatedAsset.annotations, function(index, annotation)
			{
				editor.addAnnotationToCanvas(annotation);
			});
			/*
			// Maybe this entire function is not necessary.
			// I put the enlivening code into a new function called 'refreshAnnotation'
			// Right now all this really does is renderAll and refresh the tab
			
			var editor = this;
			this.scope.annotations = inAnnotatedAsset.annotations;
			editor.fabricModel.clearCanvas();
			$.each(this.scope.annotations, function(index, annotation)
			{
				var oldAnnotations = annotation.fabricObjects;
				// annotation.fabricObjects = [];
				if (annotation.isLive())
				{
					$.each(oldAnnotations, function(index, item)
					{
						// annotation.fabricObjects.push(item);
						item.annotationid = annotation.id;
						editor.fabricModel.canvas.addInternal(item);
					});
				} 
				else 
				{
					fabric.util.enlivenObjects(oldAnnotations, function(group)
						{
						 if (editor.getAnnotationById(annotation.id) == null)
						 {
							 origRenderOnAddRemove = editor.scope.fabricModel.canvas.renderOnAddRemove
							 editor.scope.fabricModel.canvas.renderOnAddRemove = false
							 $.each(group, function(index, item) {
							 	 //item.junk = "21412124";
								 annotation.fabricObjects[index] = item;
								 item.annotationid = annotation.id;
							     editor.scope.fabricModel.canvas.addInternal(item);
							 });
							 editor.scope.fabricModel.canvas.renderOnAddRemove = origRenderOnAddRemove;
						 }
						});
				}

				// below code might be needed for recreating objects from JSON data
				// currently the whole objects are saved rather than parsed

				
			});
			*/
			editor.fabricModel.canvas.renderAll();
			jAngular.render("#annotationarea", scope); 

			// it seems like we trash the data when we render, thus may need to update the active status post-render

			///TODO Mark the ones that have annotations on it
			$("a.thumb").each(function()
				{
					var element = $(this);
					if (element.attr("id") == "thumb" + inAnnotatedAsset.assetData.id)
					{
						element.parent().addClass("active");
					}
					else
					{
						element.parent().removeClass("active");
					}
				}
			);
			
			// jAngular.render("#annotationlist"); // shouldn't have to do this
			// this method also needs to clear the canvas and comments and update from the persisted data
			// DONE: Clear canvas state, refresh with AnnotatedAsset data
			// DONE: Clear comments, refresh with AnnotatedAsset data
			// TODO: above two things with server persisted data instead of client for when page is refreshed
		
		
		}
		,
		createNewAnnotation: function(annotatedAsset)
		{
			var annot = new Annotation();
			annot.user = this.userData.userid;
			annot.assetid = annotatedAsset.assetData.id;
			annot.id = Math.floor(Math.random() * 100000000).toString();
			annot.indexCount = annotatedAsset.nextIndex();
			annot.setDate(new Date());
			annot.color = scope.userColor;
			return annot;
		}
		,
		addAnnotationToCanvas: function(inAnnotation) 
		{
			var editor = this;
			fabric.util.enlivenObjects(inAnnotation.fabricObjects, function(group)
			{
				 origRenderOnAddRemove = editor.fabricModel.canvas.renderOnAddRemove;
				 editor.fabricModel.canvas.renderOnAddRemove = false;
				 $.each(group, function(index, item)
				 {
					inAnnotation.fabricObjects[index] = item;
					item.annotationid = inAnnotation.id;
					editor.fabricModel.canvas.addInternal(item); // try without it?
				 });
				 editor.fabricModel.canvas.renderOnAddRemove = origRenderOnAddRemove;
			});
			editor.fabricModel.canvas.renderAll();
		}
		,
		fabricObjectAdded: function(fabricObject)
		{
			
			var currentAnnotation = this.createNewAnnotation(this.currentAnnotatedAsset);				
			
			// need to make sure the object is not selectable by default
			// we have mouse:move events which may be the best bet for toggling
			// can also toggle it off on selection:cleared? maybe that is too expensive
			// looks like easiest way to implement move tool is a loop through the existing objects on selectTool
			
			fabricObject.selectable = false;
			
			// make object immobile ?
			
			fabricObject.evented = false;
			currentAnnotation.pushFabricObject(fabricObject);

			this.currentAnnotatedAsset.pushAnnotation( currentAnnotation );
			
			this.scope.add("annotations",this.currentAnnotatedAsset.annotations);
			
			jAngular.render("#annotationlist", scope); //changed
			return currentAnnotation;
		}
		,
		notifyAnnotationAdded: function(currentAnnotation)
		{
			//Update network?
			var command = SocketCommand("annotation.added");
			command.annotationdata = currentAnnotation;
			this.sendSocketCommand( command,currentAnnotation.assetid );
		},
		notifyAnnotationModified: function(currentAnnotation)
		{
			var command = SocketCommand("annotation.modified");
			command.annotationdata = currentAnnotation;
			this.sendSocketCommand( command,currentAnnotation.assetid );
		}
		,
		notifyAnnotationRemoved: function(currentAnnotation)
		{
			var command = SocketCommand("annotation.removed");
			command.annotationid = currentAnnotation.id;
			this.sendSocketCommand( command,currentAnnotation.assetid );
		}
		,
		findAssetData: function(inAssetId)
		{
			var outAsset = null;
			$.each(this.scope.assets,function(index,asset)
			{
				if( asset.id == inAssetId )
				{
					outAsset = asset;
				}
			});
			return outAsset;
		}
		,
		connect : function()
		{
			//socket initialization
			if (window.WebSocket) {
				socket = WebSocket;
			} else if (window.MozWebSocket) {
				socket = MozWebSocket;
			} else {
				console.log("We're screwed");
				socket = null;
			}
		
			if (socket)
			{
				var scope = this.scope;
				var editor = this;
				
				//scope.siteroot.replace("http")
				var l = window.location;
				var rootpath = ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + (((l.port != 80) && (l.port != 443)) ? ":" + l.port : "");
				
				var base_destination =  rootpath + "/entermedia/services/websocket/org/entermediadb/websocket/annotation/AnnotationConnection";
				var final_destination = base_destination + "?catalogid=" + scope.catalogid;
				connection = new socket(final_destination);
				connection.onopen = function(e)
				{
					//console.log('Opened a connection!');
					//console.log(e);
					// get user data, should this be in connect?
					var options = {};
					options.assetid = scope.assetid;
					$.getJSON( scope.dbhome + '/services/module/asset/annotation/listannotations.json', options )
					  .done(function( json ) 
					  {
						editor.setCurrentAnnotatedAsset(json);
						editor.loadSelectors();					  
					  }).fail(function( jqxhr, textStatus, error ) {
					    var err = textStatus + ", " + error;
					    console.log( "Request Failed: " + err );
					});
					
					/*
					$.getJSON(scope.dbhome + '/services/module/user/pickcolor.json', function(data) {
						scope.annotationEditor.userData = data;
						editor.loadSelectors();
					});
					*/
				};
				connection.onclose = function(e)
				{
					console.log('Closed a connection!');
					console.log(e);
				};
				connection.onerror = function(e)
				{
					console.log('Connection error!');
					console.log(e);
				};
				connection.sendCommand = function(command)
				{
					this.send( JSON.stringify(command));
				};
				connection.onmessage = function(e)
				{
				 	var received_msg = e.data;
					var command = JSON.parse(received_msg);
					var assetid = command.assetid;
					
					//We can ignore non-loaded assets
					if( editor.currentAnnotatedAsset.assetData.id != assetid )
					{
						return;
					}
					
				  	if( command.command == "annotation.added" )
					{
						//Show it on the screen
						var anonasset = editor.currentAnnotatedAsset;
						
						// we only want to push the annotation if it doesn't already exist
						var newannotation = new Annotation(data);

						var existing = anonasset.getAnnotationById(newannotation.id);

						if (existing == null)
						{
							anonasset.pushAnnotation( newannotation );
							
							editor.currentAnnotatedAsset.currentAnnotation = newannotation;
							editor.addAnnotationToCanvas(newannotation);
							jAngular.render("#annotationlist", scope); //changed	
							scope.annotations = newannotation.annotations;
						}
						else
						{
							console.log("Already had annotation" + newannotation.id);
						}
					} 
					else if (command.command == "annotation.modified")
					{
						/*
						check if client has annotation (getAnnotationById)
						if they don't have it, this is bad, so log it for now
						if they do have it we'll need to enliven the command data
						and replace the existing annotation, then re-render
						currently we re-render by (switchToAsset)
						*/
						console.log("annotation.modified: ", command);
						
						var modifiedAnnotation = new Annotation(command.annotationdata);

						var modasset = editor.currentAnnotatedAsset();
						var foundAnnotationIndex = modasset.getAnnotationIndexById(modifiedAnnotation.id);
						modasset.annotations[foundAnnotationIndex] = modifiedAnnotation;
						
						editor.removeAnnotationFromCanvas(modifiedAnnotation.assetid,modifiedAnnotation.id);
						editor.addAnnotationToCanvas(modifiedAnnotation);
						scope.annotations = modasset.annotations;
													
					}
					else if (command.command == "annotation.removed")
					{
						var annotationid = command.annotationid;
						editor.currentAnnotatedAsset.removeAnnotation(annotationid);
						editor.removeAnnotationFromCanvas(assetid,annotationid);
						scope.annotations = editor.currentAnnotatedAsset.annotations;
					}
				};
			this.connection = connection; // connection lives on the editor. more explicit
			}
			
		}
		,
		removeAnnotationFromCanvas: function(assetid,annotationid)
		{
			var editor = this;
			console.log("Starting remove",editor.fabricModel.canvas.getObjects());
			//TODO: Make sure we are on the right assetid
			// var objectsToRemove = [];
			var canvasObjects = editor.fabricModel.canvas.getObjects().slice();
			$.each(canvasObjects, function(index, item)
			{
				if (item.annotationid == annotationid)
				{
					//objectsToRemove.push(item);
					editor.fabricModel.canvas.remove(item);
					//canvasObjects.remove(index);
				}
			});
			editor.fabricModel.canvas.renderAll();
		}
		,
		selectAnnotation: function(annotationid)
		{
			console.log(annotationid);
			$("#annotationlist .comment").removeClass("selected");
			$("#annotation" + annotationid).addClass("selected");
		}
		,
		deselectAnnotation: function()
		{
			$("#annotationlist .comment").removeClass("selected");
		}
		,
		sendSocketCommand: function( inSocketCommand, assetid )
		{
			// send out info here
			// too many layers?
			inSocketCommand.catalogid = this.scope.catalogid;
			inSocketCommand.collectionid = this.scope.collectionid;
			if( assetid )
			{
				inSocketCommand.assetid = assetid;
			}
			this.connection.sendCommand( inSocketCommand );
		}
	}
	return out;
}   

var SocketCommand = function(inCommand) {
	var out = {
		command : inCommand,
		assetid: null,
		data: null
	};
	return out; 
}

var AnnotatedAsset = function(inAssetData) {   
	var out = {
		assetData: null,
		annotations : [],
		currentAnnotation: null,
		annotationIndex: 1
	};
	if (inAssetData)
	{
		$.each(Object.keys(inAssetData), function(index, key)
		{
			if (key == "annotations")
			{
				var tempAnnotations = [];
				$.each(inAssetData.annotations, function(index, annotationData)
				{
					tempAnnotations.push(new Annotation(annotationData));
				})
				out.annotations = tempAnnotations;
			}
			else
			{
				out[key] = inAssetData[key];  //have to update the object references before we define our methods
			}
		});
	}
		
	out.pushAnnotation = function( inAnnotation )
		{
			this.annotations.push( inAnnotation );
		};
	out.nextIndex = function() {
			return this.annotationIndex++;
		};
	out.getAnnotationById = function(inAnnotationId) {
			var outAnnotation = null;
			$.each(this.annotations, function(index, annotation)
			{
				if (annotation.id === inAnnotationId)
				{
					outAnnotation = annotation;
					return true;
				}
			});
			return outAnnotation;
		};
	out.getAnnotationIndexById = function(inAnnotationId)
		{
			var outAnnotationIndex = -1;
			$.each(this.annotations, function(index, annotation)
			{
				if (annotation.id === inAnnotationId)
				{
					outAnnotationIndex = index;
					return true;
				}
			});
			return outAnnotationIndex;
		};
		out.removeAnnotation = function(annotationid)
		{
			var annotationToRemove = this.getAnnotationById(annotationid);
			this.annotations = _.without(this.annotations, annotationToRemove);
		};
	
	return out; 
}

var Annotation = function(inAnnotationData) {   
	var out = {
		id: null,
		indexCount: null,
		user: null,
		comment: "",
		date: null,
		color: null,
		fabricObjects: [], 
		assetid: null
	};
	if (inAnnotationData)
	{
		$.each(Object.keys(inAnnotationData), function(index, key)
		{
			out[key] = inAnnotationData[key];
		});
	}
		out.getUserName = function()
		{
			var userOut = "demouser";
			if (this.user !== null)
			{
				userOut = this.user;
			}
			return userOut;
		};
		out.pushFabricObject = function( inObject )
		{
			this.fabricObjects.push( inObject );
		};
		out.isLive = function() 
		{
			if (this.fabricObjects.length > 0 && this.fabricObjects[0].annotationid)
			{
				return true;
			}
			return false;
		};
		out.hasObject = function(inObj)
		{
			return $.inArray(inObj, this.fabricObjects) !== -1;
		};
		out.getFormattedDate = function()
		{
			var inDate = new Date(this.date);
			var timestring = inDate.toTimeString();
			return $.datepicker.formatDate($.datepicker._defaults.dateFormat, inDate) + " " + timestring.substring(0, timestring.indexOf("GMT")-1);
			
		};
		out.setDate = function(inDate)
		{
			var datestring = inDate.toISOString().replace("T", " ");
			var zonestring = inDate.toString();
			this.date = datestring.substring(0, datestring.indexOf("Z")-4) + " " + zonestring.substring(zonestring.indexOf("GMT") + 3, zonestring.indexOf("(")- 1)
					
		};
	return out; 
}


var loadFabricModel = function(scope)
{
	var fabricModel = new FabricModel(scope);
	scope.annotationEditor.fabricModel = fabricModel;
	scope.add("fabricModel",fabricModel);

}