Workspace.factory 'fabricJsService', () ->
    globscale = 1
    init: (path) ->
        returnCanvas = {}
        (() ->
            docGet = (id) -> # do we need this anymore?
                document.getElementById id

            canvas = @__canvas = new fabric.Canvas 'annotation_canvas'
                # want to ditch this setup for instead using the tool selection to create parameters
                # {
                #     isDrawingMode: true
                # }

            canvas.on "after:render", () ->
                # canvas.calcOffset()
                em.unit

            # okay here's where I try to get around the weird bugs (misunderstandings) with fabric.Image
            # it seems like a clunky way to do this, but it works and nothing short of it did

            # first step is to cheat and use fabric's underlying logic to make an HTML element object

            fabric.util.loadImage path, (src) ->
                # second step is to turn this HTML element object into a fabric.Image
                realImage = new fabric.Image(src)
                # third step is set the dimensions of the canvas to those of the new image
                canvas.setWidth realImage.width
                canvas.setHeight realImage.height
                # fourth/fifth steps?  Try changing the size of the canvas to the image's dimensions
                center = canvas.getCenter()
                canvas.setBackgroundImage realImage, canvas.renderAll.bind canvas
                em.unit






            # canvas.setBackgroundImage path, canvas.renderAll.bind(canvas)

            # canvas.on('mouse:down', function(o){
            #     // select appropriate function based on selected tool
            #     // we need some var in the scope to keep track of 'active tool selection'
            #     // the value of this var will point to the function that should be passed to the event handlers
            #     // should the function be passed in directly or define another function that handles the
            #     // specialized event handling... if the latter, then the mouse event should control the
            #     // mouseDown variable exclusively to ensure no weird condition overlap

            returnCanvas = canvas

            em.unit
        )()

        (() ->
            # does this even work? maybe scrap if the call to calcOffset above is sufficient
            fabric.util.addListener fabric.window, 'load', () ->
                canvas = @__canvas || @canvas
                canvases = @__canvases || @canvases

                canvas and canvas.calcOffset and canvas.calcOffset()

                if canvases and canvases.length
                    for [0..canvases.length]
                        canvases[i].calcOffset()
                em.unit

        )()
        canvas: returnCanvas
