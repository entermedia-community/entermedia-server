# working js file is located at AnnotationDetailsCtrl_orig.js
# this is a buggy CoffeeScript
Workspace.controller 'AnnotationDetailsCtrl',
['$rootScope', '$scope', '$stateParams', '$timeout', 'annotationService', 'fabricJsService', 'annotationSocket',
($rootScope, $scope, $stateParams, $timeout, annotationService, fabricJsService, annotationSocket) ->

	$rootScope.$broadcast 'navigatedTo', 'Annotations'
	annotationSocket.forward 'updateAnnotationResponse', $scope
	annotationSocket.forward 'removeAnnotationResponse', $scope

	###
	INITIALIZE CANVAS
	###

	$scope.currentAnnotation = _.find annotationService.mockData,
	(item) ->
		item.annotation.id is parseInt $stateParams.annotationID
	# uses init function to create the fabric environment
	$scope.fabric = fabricJsService.init $scope.currentAnnotation.annotation.path

	###
	/ INITIALIZE CANVAS
	###

	$scope.selectable = false
	$scope.canSelect = () -> $scope.selectable
	$scope.colorpicker =
		hex: '#000fff'
	$scope.brushWidth = 5
	$scope.mouseDown = null
	$scope.left = 0 # !!!
	$scope.top = 0 # !!!
	$scope.currentAnnotationIndex = 1 # should probably be deprecated to have annotation index tied to comment index
	$scope.newCommentText = null
	$scope.annotations = []	# holds all annotation groups (should be one per unique annotation w/ comment)
	$scope.eventIndex = 0
	$scope.annotationAction = null
	$scope.currentAnnotationGroup = []
	$scope.currentAnnotationGroupId = 0

	###
	USER AUTHENTICATION LOGIC
	###

	# check validity of user object and do whatever necessary if not authenticated

	# if not metaUser.authenticated

	metaUser =
		userid: 1
		name: 'Rob'
		email: md5 'jrchipman1@gmail.com'

	$scope.currentUser = metaUser

	$.getJSON '/entermedia/services/json/users/status.json', (data) ->
			$scope.currentUser = data
			console.log data
			em.unit

	# $scope.getUser = () ->
	# 	console.log 'We are actually clicking this button'
	# 	# console.log $.getJSON '/entermedia/services/json/users/status.json', (data) ->
	# 	# 	alert data
	# 	$.ajax {
	# 		type: "GET"
	# 		url: '/entermedia/services/json/users/status.json'
	# 		success: (data) ->
	# 			alert data
	# 			em.unit
	# 		,
	# 		failure: (errMsg) ->
	# 			alert errMsg
	# 			em.unit
	# 	}
	# 	em.unit

	###
	/ USER AUTHENTICATION LOGIC
	###

	$scope.thumbs = [
		{ name: 'Maybe Art', src: '$home$apphome/components/annotations/img/BlueBus.jpg', id: 104 }
		{ name: 'Stupid Art', src: 'img/ForMom.jpg', id: 101 }
		{ name: 'Nice Art', src: 'img/FenceDog.jpg', id: 102 }
		{ name: 'Great Art', src: 'img/TigerTug.jpg', id: 103 }
	]

	###
	FABRIC TOOLS SETUP
	###

	getSelf = (name) ->
		_.find(toolkit, name: name)

	toolkit = [
		{
			name: 'disabled'
			properties: {
				isDrawingMode: false
			}
			annotating: false,
		},
		{
			name: 'draw'
			properties: {
				isDrawingMode: true # this may be the only thing necessary
			}
			annotating: true
		},
		{
			name: 'move'
			properties: {
				isDrawingMode: false
			}
			annotating: false
		},
		{
			name: 'shape'
			properties: {
				isDrawingMode: false
			}
			annotating: true
			type: 'circle'
			# index of types is same as blanks, useful or dumb
			types: [
				{} =
					name: 'circle'
					type: fabric.Circle
					blank:
						radius: 1
						strokeWidth: 5
						selectable: false
						fill: ""
						originX: 'left'
						originY: 'top'
					drawparams: (pointer) ->
						radius: Math.abs $scope.left - pointer.x
				{} =
					name: 'rectangle'
					type: fabric.Rect
					blank:
						height: 1
						width: 1
						strokeWidth: 5
						selectable: false
						fill: ""
						originX: 'left'
						originY: 'top'
					drawparams: (pointer) ->
						width: -$scope.left + pointer.x
						height: -$scope.top + pointer.y
			]
			events: {
				mouseup: (e, canvas) ->
					# not sure if this is best way to do this
					# do I even need to pass 'canvas' if it will be valid within executing scope?
					# definitely don't want to put a lot of junk on $scope if I don't have to
					$scope.mouseDown = false # theses $scope properties are probably a really bad convention, but it works
				mousedown: (e, canvas) ->
					$scope.mouseDown = true # gotta be a better way !!!
					pointer = canvas.getPointer e.e
					we = getSelf 'shape'
					type = _.findWhere $scope.currentTool.types, name: $scope.currentTool.type
					spec = type.blank
					spec.stroke = $scope.colorpicker.hex	# this is how we avoid the stupid broken color problem
					spec.left = pointer.x
					spec.top = pointer.y
					shape = new type.type spec
					canvas.add shape
					em.unit
				objectadded: null
				mousemove: (e, canvas) ->
					if $scope.mouseDown # just awful !!!
						we = getSelf('shape')
						pointer = canvas.getPointer e.e
						# need to find some way to get the shape now
						shape = canvas.getObjects()[canvas.getObjects().length-1]
						type = _.findWhere $scope.currentTool.types, name: $scope.currentTool.type
						shape.set type.drawparams pointer
						canvas.renderAll()
					em.unit
				}
			},
				{
					name: 'comment'
					properties: {
						isDrawingMode: false
					}
					annotating: true # this is possibly broken because currently the pin is placed at last object
					events: {
						mouseup: null # will want to put something here !!!
						mousedown: null
						objectadded: null # then we should fully integrate the
					}
				},
				{
					name: 'arrow' # see below $$$
					properties: {
						isDrawingMode: false
					}
					annotating: true
				},
				{
					name: 'text' # fabric has an existing text tool, need to find out how to use $$$
					properties: {
						isDrawingMode: false
					}
					annotating: true
				},
				{
					name: 'zoom' # this implementation sucks !!! $$$
					properties: {
						isDrawingMode :false
					}
					annotating: false
					events: {
						mouseup: null
						mousemove: (o, canvas) ->
							if $scope.mouseDown
								# this just doesn't work very well !!!
								SCALE_FACTOR = 0.01
								pointer = canvas.getPointer o.e
								delta = $scope.left - pointer.x
								objects = canvas.getObjects()
								# needs changes !!!
								delta = delta * SCALE_FACTOR
								transform = [1+delta,0,0,1+delta,0,0]
								console.log transform
								for klass in objects
									klass.transformMatrix = transform
									klass.setCoords()
								# can we also transform the canvas background?
								canvas.backgroundImage.transformMatrix = transform  # works
								canvas.setWidth canvas.backgroundImage.width * canvas.backgroundImage.transformMatrix[0]
								canvas.setHeight canvas.backgroundImage.height * canvas.backgroundImage.transformMatrix[3]
								# apparently, yes!
								# works great but doesn't affect pins yet
						mousedown: (o, canvas) ->
							$scope.left = canvas.getPointer(o.e).x
					}
				},
				{
					name: 'colorpicker' # no implementation $$$
					properties: {}
					annotating: false
				},
				{
					name: 'load' # temporary?
					properties: {}
					annotating: false
				},
				{
					name: 'export'
					properties: {}
					annotating: false
				}
	]

	$scope.fabric.toolkit = toolkit # in case it needs to be accessible from other controllers

	###
	/ FABRIC TOOLS SETUP
	###


	$scope.testSocket = () ->
		# emit the 'test socket' event to be heard by the listener

		em.unit

	###
	JSON IMPORT/EXPORT LOGIC
	###

	applicationid = "emshare"	

	$scope.loadImages = () ->
		markers = {
			"query": [
				{
					"field": "id"
					"operator": "matches"
					"values": ["*"]
				}
			]
		}

		# applicationid = /emsite/workspace -- this is already added by the ajax request
		# #{applicationid}/views/modules/asset/downloads/preview/thumbsmall/#{obj.sourcepath}/thumb.jpg
		# the above should be the actual location of the thumbnail image
		# this worked once:
		# "/entermedia/services/json/search/data/asset?catalogid=media/catalogs/public"

		# this is what emshare's asset thumb is:
		# localhost:8080/emshare/views/modules/asset/downloads/preview/mediumplus/#{obj.sourcepath}/thumb.jpg
		# this all temporarily works, but it isn't the way it should be in the end

		# since JSON automatically adds the applicationid to the path, it doesn't get thumbs correctly
		# when my assets are on emshare and my app is on emsite, so I hacked it to work for now

		$.ajax {
			type: "POST"
			url: "/entermedia/services/json/search/data/asset?catalogid=media/catalogs/public"
			data: JSON.stringify markers
			contentType: "application/json; charset=utf-8"
			dataType: "json"
			async: false
			success: (data) ->
				tempArray = []
				$.each data.results, (index, obj) ->
					# this path should be changed according to the specifications above
					path = "http://localhost:8080/emshare/views/modules/asset/downloads/preview/thumbsmall/#{obj.sourcepath}/thumb.jpg"
					console.log path
					# $scope.thumbs.push path
					console.log fabric.util.loadImage path, (src) ->
						# $scope.fabric.canvas.add new fabric.Image(src)
						em.unit
					em.unit
				em.unit
			,
			failure: (errMsg) ->
				alert errMsg
				em.unit
			}
		em.unit

	$scope.exportCanvas = () ->
		data = JSON.stringify $scope.fabric.canvas.toJSON()
		$.ajax {
			type: "POST"
			url: "services/json/search/data/annotation?save=true?field=annotationid&field.value=#{$scope.currentAnnotation.id}&field=json&field.value=#{data}"
			success: (data) ->
				alert "Success!"
				em.unit
			,
			failure: (errMsg) ->
				alert errMsg
				em.unit
			}
		em.unit

	###
	/ JSON IMPORT/EXPORT LOGIC
	###

	###
	TOOLBAR SELECTOR METHODS
	###

	$scope.shapeToolType = 'circle' # initialize shape selector to a valid state

	$scope.selectTool = (toolname) ->
		if not $scope.readyToComment
			$scope.currentTool = _.findWhere $scope.fabric.toolkit, name: toolname
			# do whatever else needs to happen !!!
			for prop of $scope.currentTool.properties
				$scope.fabric.canvas[prop] = $scope.currentTool.properties[prop]
			# this shit is broke !!!
			if $scope.currentTool.name is 'draw'
				$scope.fabric.canvas.freeDrawingBrush.color = $scope.colorpicker.hex
				$scope.fabric.canvas.freeDrawingBrush.width = $scope.brushWidth
		em.unit

	$scope.setShapeTypeFromUi = (type) ->
		$scope.currentTool = _.findWhere $scope.fabric.toolkit, name: 'shape'
		$scope.currentTool.type = type
		$scope.shapeToolType = $scope.currentTool.type
		for prop of $scope.currentTool.properties
			$scope.fabric.canvas[prop] = $scope.currentTool.properties[prop]
		em.unit


	# $scope.setApproval = (user, approvalState) ->
	# 	$scope.approvalHash[user] = approvalState # totally unsafe

	# $scope.getApprovals = () ->
	# 	(user for user of $scope.approvalHash when $scope.approvalHash[user] is true)

	# $scope.getRejections =
	# () ->
	# 	(user for user of $scope.approvalHash when $scope.approvalHash[user] is false)
	# _.contains(array, entry) -> bool is entry in array
	###
	This whole process is muddled, what should happen is simple:
	user clicks to draw a shape, that shape is added to the current group upon object:added
	a timeout function begins to check if they are done annotating
	if the user clicks again within a time window, the timeout function is cancelled
	repeat process until...
	user finishes annotation, they should be prompted for a comment
	a pin should be created and added into the annotationGroup data
	the pin should be rendered on screen somewhere appropriate and...
	the comment should be added to scope with annotationGroup data to be attached to comment
	###

	$scope.setShapeType = (type) ->
		if type is 'circle'
			$scope.currentTool.type = fabric.Circle
		else if type is 'rectangle'
			$scope.currentTool.type = fabric.Rect
		em.unit

	$scope.selectTool 'disabled' # initialize state with no editing option selected

	###
	/ TOOLBAR SELECTOR METHODS
	###

	commentPin = () ->
		# this needs to be fixed to use a properly bordered circle instead of two circles
		new fabric.Group [
			new fabric.Circle({
				radius: 18.5
				fill: "#fff"
			})

			new fabric.Circle({
				radius: 14
				fill: "#4fabe5"
				top: 5
				left: 5
			})
			,
			new fabric.Text $scope.currentAnnotationIndex.toString(),
				{
					fontSize: 20
					fill: "#fff"
					left: 13
					top: 4
				}
		],
			{
				evented: true
				top: $scope.top - 15
				left: $scope.left - 15
				lockScalingX: false
				lockScalingY: false
				selectable: true
			}

	readyToComment = () ->
		$scope.readyToComment = true
		$scope.fabric.canvas.isDrawingMode = false
		# Let's make the pin after we get the objects back from the server

		$timeout (() ->
			$('#user-comment-input').focus()
			em.unit
		), 100
		$scope.selectTool 'disabled'
		$('.upper-canvas').css({'background':'rgba(255,255,255,0.7)'})
		em.unit

	$scope.addComment = () ->
		pin = commentPin()
		$scope.fabric.canvas.add pin
		$scope.currentAnnotationGroup.push pin
		annotationSpec =
			id: $scope.currentAnnotationIndex++
			group: $scope.currentAnnotationGroup
			user: $scope.currentUser
			comment:
				type: 'normal'
				text: $scope.newCommentText
				timestamp: moment().fromNow()

		# now push annotation info to scope for longer-term tracking
		# console.log annotationSpec
		$scope.annotations.unshift annotationSpec
		$scope.currentAnnotationGroup = []
		$scope.newCommentText = null
		$scope.readyToComment = false
		$scope.selectTool 'disabled'
		$('.upper-canvas').css({'background':'none'})
		$scope.left = null
		$scope.top = null
		# annotationSpec object describing individual annotation group is pushed to socket
		annotationSocket.emit 'updateAnnotation', JSON.stringify annotationSpec
		em.unit

	$scope.removeComment = (annotationid) ->
		annotationToRemove = _.findWhere $scope.annotations, id: annotationid

		# send request to server to remove annotation by id
		annotationSocket.emit 'removeAnnotation', annotationid

		# remove all local objects from canvas
		_.forEach annotationToRemove.group, (item) ->
			$scope.fabric.canvas.remove item

		# reset annotations model to remove comments from comment panel
		$scope.annotations = _.without $scope.annotations, annotationToRemove
		em.unit

	$scope.cancelComment = () ->
		_.forEach $scope.currentAnnotationGroup, (item) ->
			$scope.fabric.canvas.remove item
		$scope.readyToComment = false
		$scope.newCommentText = null
		$scope.currentAnnotationGroup = []
		$('.upper-canvas').css({'background':'none'})
		em.unit



	###
	Because of fabric.js quirks with object addition (on addition to the canvas, the object is mutated to a state where it cannot be added again)
	the logic for transmitting the state of the canvas must not rely on the ability to pass the objects
	Luckily, Fabric.js includes handy toJSON and loadFromJSON methods that can send the entire canvas object
	All we need to do is attach the information we need to that object and let our data be transmitted along with the canvas
	Then we can load the canvas, let fabric do all of its logic, and insert our own data into hte current $scope when it is finished
	This may cause complications with being able to track the comments, because the efforts to link annotation with comment
	will be thwarted using this method of canvas transport.  This also means that large annotations may be slow to load
	since the process involves completely destroying and rebuilding the entire canvas

	It also remains to be seen if attributes added to the canvas object will still be populated on the loaded canvas (don't see why not)

	###

	###
	FABRIC CANVAS EVENT HANDLERS
	###

	# Server raised events
	$scope.$on 'socket:updateAnnotationResponse', (e, data) ->
		# the data object is now the JSONified annotationSpec, let's rebuild
		# if this works, at least we know we can update the canvas, though this probably screws up anyone
		# who is currently annotating

		annotationGroup = JSON.parse data

		# reset annotation index to make sure no dupes
		annotationGroup.id = $scope.currentAnnotationIndex++

		fabric.util.enlivenObjects annotationGroup.group, (group) ->
			origRenderOnAddRemove = $scope.fabric.canvas.renderOnAddRemove
			# looks like we have to change this canvas setting so the errors don't kill us
			$scope.fabric.canvas.renderOnAddRemove = false
			_.forEach group, (item) ->
				$scope.fabric.canvas.add item
			$scope.fabric.canvas.renderOnAddRemove = origRenderOnAddRemove
			$scope.fabric.canvas.renderAll()
			em.unit
		$scope.annotations.unshift annotationGroup
		em.unit

	$scope.$on 'socket:removeAnnotationResponse', (e, id) ->
		# id here is just the annotation id to remove
		# check annotations model to find all of the moving parts
		clientCopy = _.findWhere $scope.annotations, id: id
		console.log 'trying to remove this: ', id, ': ', clientCopy
		_.forEach clientCopy.group, (item) ->
			$scope.fabric.canvas.remove item
		$scope.annotations = _.without $scope.annotations, clientCopy
		em.unit


	$scope.fabric.canvas.on 'mouse:down', (e) ->
		pointer = $scope.fabric.canvas.getPointer e.e
		$scope.mouseDown = true
		$scope.left = pointer.x
		$scope.top = pointer.y
		if not $scope.readyToComment
			if $scope.annotationAction isnt null
				console.log 'canceling annotationAction'
				$timeout.cancel $scope.annotationAction
				# $scope.annotationAction = null
			# if $scope.currentTool.name is 'comment'
			# 	$scope.left = pointer.x
			# 	$scope.top = pointer.y
			console.log 'DOING SOMETHING!!! Something is wrong with disabled tool/ offset calculations'
			$scope.currentTool.events?.mousedown? e, $scope.fabric.canvas
		em.unit

	$scope.fabric.canvas.on 'mouse:up', (e) ->
		$scope.mouseDown = false
		if $scope.currentTool.annotating and not $scope.readyToComment
			if $scope.currentTool.name is 'comment'
				console.log 'Calling readyToComment() now'
				readyToComment()
			else
				console.log 'Calling delayed readyToComment()'
				$scope.annotationAction = $timeout readyToComment, 1000
		$scope.currentTool.events?.mouseup? e, $scope.fabric.canvas
		em.unit

	$scope.fabric.canvas.on 'mouse:move', (e) ->
		$scope.fabric.canvas.calcOffset()
		$scope.currentTool.events?.mousemove? e, $scope.fabric.canvas
		em.unit

	$scope.fabric.canvas.on 'object:added', (obj) ->
		if $scope.currentTool.annotating
			obj.target.selectable = $scope.canSelect() # this likely does not work anymore
			$scope.currentAnnotationGroup.push obj.target
		$scope.currentTool.events?.objectadded? obj, $scope.fabric.canvas
		# this may not be the best place for these, but it needs to happen somewhat regularly
		$scope.fabric.canvas.calcOffset()
		$scope.fabric.canvas.renderAll()
		$scope.left = obj.target.left if !$scope.left
		$scope.top = obj.target.top if !$scope.top
		em.unit
	em.unit

	###
	/ FABRIC CANVAS EVENT HANDLERS
	###
]
