Workspace.factory 'toolkitService',
() ->
	[
        {
            name: 'draw'
            properties: {}
        },
        {
            name: 'move'
            properties: {}
        },
        {
            name: 'shape'
            properties: {}
            type: fabric.Circle
            events: {
                mouseup: ->
                mousedown: ->
                objectadded: ->
            }
        },
        {
            name: 'comment'
            properties: {}
            events: {
                mouseup: ->
                mousedown: ->
                objectadded: ->
            }
        },
        {
            name: 'arrow'
            properties: {}
        },
        {
            name: 'text'
            properties: {}
        },
        {
            name: 'zoom'
            properties: {}
        },
        {
            name: 'colorpicker'
            properties: {}
        }

    ]