# Workspace.controller 'AnnotationDetailsCtrl', 
# ['$scope', '$stateParams', 'annotationService',
# ($scope, $stateParams, annotationService) ->
#     $scope.currentAnnotation = _.find annotationService.mockData,
#     (item) ->
#     	item.annotation.id is parseInt $stateParams.annotationID
#     em.unit
# ]


# working js file is located at AnnotationDetailsCtrl_orig.js
# this is a buggy CoffeeScript
Workspace.controller 'AnnotationDetailsCtrl', 
['$scope', '$stateParams', '$timeout', 'annotationService', 'fabricJsService',
($scope, $stateParams, $timeout, annotationService, fabricJsService) ->
	self.mouseDown = null # look I defined this here in the controller, this is probably bad !!!
	self.origX = 0 # !!!
	self.origY = 0 # !!!
	$scope.currentCommentIndex = 3 # should probably be deprecated to have annotation index tied to comment index
	$scope.newCommentText = null
	$scope.approvalHash = {} # empty obj for user: approval kv pairs
	# placeholder JSON function

	comment = 
	{
	    type: 'normal'
	    name: 'Rob'
	    email: md5 'jrchipman1@gmail.com' 
	    text: 'This is a comment that some dude left on here. cool.'
	    annotationId: 3
	    timestamp: moment().fromNow()
	}

	comment2 = 
	{
	    type: 'normal'
	    name: 'Chris'
	    email: md5 'test@gmail.com' 
	    text: 'Hey, what about the thing on the right here, don\'t forget to do the stuff.'
	    annotationId: 2
	    timestamp: moment().subtract('minutes', 30).fromNow()
	}

	comment3 = 
	{
	    type: 'normal'
	    name: 'Adam'
	    email: md5 'test@gmail.com' 
	    text: 'I dont feel like the sky is as blue as it could be, perhaps we should revisit?'
	    annotationId: 1
	    timestamp: moment().subtract('days', 1).fromNow()
	}

	# Highlights UI stuff, to be deleted
	$scope.comments = [comment,comment2,comment3]
	$scope.approved = [1..4]	# deprecated? unless property approach more favored in angular vs methods?
	$scope.rejected = [1]		# deprecated? maybe not since method method doesn't work?
	$scope.images = [1..6]		# deprecated but so is below line since thumb generation is free from EM
	$scope.loadImages = (collectionid) ->
		markers = {
			"query": [
				{
					"field": "id"
					"operator": "matches"
					"values": ["*"]
		    	}
			]
		}
		em.unit
		# applicationid = /emsite/workspace
		# #{applicationid}/views/modules/asset/downloads/preview/thumbsmall/#{more.sourcepath}/thumb.jpg
		doJSON = () ->
			$.ajax {
				type: "POST"
				url: "/entermedia/services/json/search/data/asset?catalogid=media/catalogs/public"
				data: JSON.stringify markers
				contentType: "application/json; charset=utf-8"
				dataType: "json"
				async: false
				success: (data) ->
					tempArray = $.each data.results, (index, more) ->
				    	more.sourcepath
				   	console.log tempArray
				,
				failure: (errMsg) ->
					alert errMsg
				}
			em.unit

	

	$scope.addComment =
	() ->
	    $scope.comments.unshift {
	        type: 'normal'
	        name: 'Rob'
	        email: md5 'jrchipman1@gmail.com'
	        text: $scope.newCommentText
	        annotationId: ++$scope.currentCommentIndex
	        timestamp: moment().fromNow()
	    }
	    $scope.newCommentText = null
	    em.unit

    $scope.selectTool = (toolname) ->
        $scope.currentTool = _.findWhere $scope.fabric.toolkit, name: toolname
        # do whatever else needs to happen !!!
        for prop of $scope.currentTool.properties
        	$scope.fabric.canvas[prop] = $scope.currentTool.properties[prop]
        console.log $scope.currentTool
        em.unit

	$scope.setApproval =
	(user, approvalState) ->
		$scope.approvalHash[user] = approvalState # totally unsafe

	$scope.getApprovals =
	() ->
		(user for user of $scope.approvalHash when $scope.approvalHash[user] is true)

	$scope.getRejections = 
	() ->
		(user for user of $scope.approvalHash when $scope.approvalHash[user] is false)

	$scope.annotations = []		# holds all annotation groups (should be one per unique annotation w/ comment)
	$scope.events = []			# events attribute holds information about the unique event
	usefulKeys = ['']			# i dunno
	$scope.currentAnnotation = _.find annotationService.mockData, 
	(item) ->
		item.annotation.id is parseInt $stateParams.annotationID 
	# uses init function to create the fabric environment
	$scope.fabric = fabricJsService.init $scope.currentAnnotation.annotation.path
	$scope.selectTool('draw')
	$scope.eventIndex = 0
	$scope.annotationAction = null
	$scope.currentAnnotationGroup = []
	$scope.currentAnnotationGroupId = 0
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



	commentPin = () ->
		dropPoint = $scope.fabric.canvas.getObjects()[$scope.fabric.canvas.getObjects().length-1]
		# should handle this drop point some better way
		# currently this method does not support the use of the comment tool (irony)

		new fabric.Group [new fabric.Circle({
		        radius: 15
		        fill: "#000fff"
		        borderColor: "#fff"
		    }),

	      	new fabric.Text $scope.currentCommentIndex.toString(), 
		      	{
			        fontSize: 30
			        color: "#ffffff"
			        left: 5
			        top: -5
		      	}
    	],
		    {
		        evented: false
		        top: dropPoint.top
		        left: dropPoint.left
		    }

	timeoutFunc = () ->
    	$scope.events.push {id: $scope.eventIndex++,  text: 'Object added!'}
	    # lazy prompting and comment addition
	    $scope.newCommentText = prompt "Enter a comment:" || "<no comment?>"
	    # add little pin to canvas???
	    annotationSpec = 
	    {
	        id: $scope.currentCommentIndex+1
	        group: $scope.currentAnnotationGroup
	        user: $scope.currentUser
	        comment: $scope.newCommentText
	    }
	    $scope.addComment()
	    # oh please tell me there is a non-ugly way to do this (I bet that's what Coffee is for)

	    # fix this ^^^
	    $scope.fabric.canvas.add commentPin()
	    # now push annotation info to scope for longer-term tracking
	    $scope.annotations.push annotationSpec
	    $scope.currentAnnotationGroup = []
	    # alert("You added an object group!");
	    $scope.$apply()  # is this even necessary here?
	    em.unit

	$scope.fabric.canvas.on 'mouse:down', (e) ->
		self.mouseDown = true
		if $scope.annotationAction isnt null
	    	$timeout.cancel $scope.annotationAction
	    pointer = $scope.fabric.canvas.getPointer e.e
	    self.origX = pointer.x
	    self.origY = pointer.y
	    $scope.currentTool.events?.mousedown? e, $scope.fabric.canvas
	    em.unit

	$scope.fabric.canvas.on 'mouse:up', (e) ->
		self.mouseDown = false
		if $scope.currentTool.annotating
		  	$scope.annotationAction = 
		  		$timeout timeoutFunc, 2000
	  	$scope.currentTool.events?.mouseup? e, $scope.fabric.canvas
	  	em.unit
	$scope.fabric.canvas.on 'mouse:move', (e) ->
		$scope.currentTool.events?.mousemove? e, $scope.fabric.canvas
		em.unit

	$scope.fabric.canvas.on 'object:added', (obj) ->
		if $scope.currentTool.annotating
			$scope.currentAnnotationGroup.push obj
		$scope.currentTool.events?.objectadded? obj, $scope.fabric.canvas
		# this may not be the best place for these, but it needs to happen somewhat regularly
		$scope.fabric.canvas.renderAll()
		$scope.fabric.canvas.calcOffset()
		em.unit
	em.unit
]