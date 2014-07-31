# this webserver is a hybrid of the "old" one and the socket.io chat tutorial
# the two are rather different methods of creating the server so who knows if I did right

em = unit: {}

# express = require('express')()

http = require('http') # .Server express

fs = require 'fs'

path = require 'path'

requestHandler = (request, response) ->
	fileName = path.normalize request.url
	regex = /(\.css)|(\.js)|(\.tpl\.html)|(\.jpg)|(\.png)/
	fileName = if regex.test(fileName) then fileName else '/index.html'
	localFolder = __dirname
	content = localFolder + fileName
	fs.readFile content, (err, contents) ->
		if not err then response.end contents else console.dir err
		em.unit
	em.unit

http = http.createServer requestHandler

io = require('socket.io') http

# fabric = require 'fabric' # so we can listen for fabric JSON - fails due to node_module not present, errors on compile

# express.get '/#/', (req, res) ->
#   res.sendfile 'index.html'
#   em.unit

io.on 'connection', (socket) ->
	# ...
	# when the user connects, client needs to ask server for proper state
	# ...
	socket.on 'updateAnnotation', (data) ->
		# this listener must act as an interloper between
		# the local client canvas app and all connected clients
		# this ensures that no changes are actually made locally
		# without first asking the server and pushing those
		# changes to all connected clients so no race conditions
		# can exist with user editing
		console.log 'updateAnnotation'
		socket.broadcast.emit 'updateAnnotationResponse', data
		# this should pass the annotation object with any modifications necessary
		# any updates to the client can be done upon response, such that all clients are equal

		em.unit

	socket.on 'removeAnnotation', (data) ->

		console.log 'removeAnnotation'
		socket.broadcast.emit 'removeAnnotationResponse', data
		# this event handles telling the clients that a comment was removed
		# therefore the clients can remove the necessary objects and update annotations model
		em.unit

	em.unit

http.listen 3000, () ->
	console.log 'listening on *:3000'
	em.unit


# # new stuff for possible node-fabric synergy with JSON
# # 	canvas = fabric.createCanvasForNode 800, 600
# # 	response.writeHead 200, 'Content-Type': 'image/png'

# # 	canvas.loadFromJSON params.query.data, () ->
# # 		canvas.renderAll()

# # 		stream = canvas.createPNGStream()
# # 		stream.on 'data', (chunk) ->
# # 			response.write chunk
# # 		stream.on 'end', () ->
# # 			response.end()
# # 		em.unit
# http.createServer(requestHandler).listen 8124
