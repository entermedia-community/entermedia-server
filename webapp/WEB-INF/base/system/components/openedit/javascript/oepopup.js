var OEPopUp = {
    offX: 0,
    offY: 0,
    showDelay: 100,
    hideDelay: 500,
    timer:null, 
	showing:null,
showPopUp: function(inLink, inShowDivId, inXOff, inYOff )
{
    if ( typeof OEPopUp == "undefined" ) return;

	
    if (this.timer) { clearTimeout(this.timer);	this.timer = 0; }

	if( isNaN(inXOff ) )
	{
		inXOff = this.offX;
	}
	if( isNaN(inYOff ) )
	{
		inYOff = this.offY;
	}

	var thediv = document.getElementById(inShowDivId);
	if( thediv )
	{
		//Close lists?
		var i,j;
		var forms = document.forms;
		for (i = 0; i < forms.length; i++)
		{
			var elems = forms[i].elements;
			for (j=0; j < elems.length; j++)
			{
				elems[j].blur();
			}
		}
		
		//popup in the correct place?
		if( this.showing )
		{
			if( this.showing != inShowDivId )
			{
				//why do this position here?
				//this.positionTip( inLink, thediv,inXOff, inYOff  ); //Moves it up if we are near the bottom of page for some reason
				this.toggleVis(this.showing,'hidden'); //hide what we have now showing
			}
		}		
		else
		{
			//only move this if needed
			this.positionTip( inLink, thediv , inXOff, inYOff); //Moves it up if we are near the bottom of page for some reason
		}
		this.timer = setTimeout("OEPopUp.toggleVis('" + inShowDivId + "', 'visible')", this.showDelay);	
	}
},
hide: function(inDivId) 
{
    if ( typeof OEPopUp == "undefined"  ) return;

    if (this.timer) 
    {
    	 clearTimeout(this.timer);	this.timer = 0; 
    }
    this.timer = setTimeout("OEPopUp.toggleVis('" + inDivId + "', 'hidden')", this.hideDelay);

},
positionTip: function(inLink, inShowDiv, inXOff, inYOff) 
{
    if ( inShowDiv.style ) 
    {
        oeviewport.getAll();
    	var clicked = getAbsolutePosition(inLink);

        // put e.pageX/Y first! (for Safari)
        var x = clicked.x + oeviewport.scrollX;
        var y = clicked.y + oeviewport.scrollY;
    
        // put e.pageX/Y first! (for Safari)
        if ( x + inShowDiv.offsetWidth + inXOff > oeviewport.width + oeviewport.scrollX ) 
        {
            x = x - inShowDiv.offsetWidth - inXOff;
            if ( x < 0 ) 
            {
            	x = 0;
            }
        }
        else
        {
        	x = x + inXOff;
        }
        if ( y + inShowDiv.offsetHeight + inYOff > oeviewport.height + oeviewport.scrollY ) 
        {
            y = y - inShowDiv.offsetHeight - inYOff;
            if ( y < oeviewport.scrollY ) 
            {
            	y = oeviewport.height + oeviewport.scrollY - inShowDiv.offsetHeight;
            }
        } 
        else 
        {
        	y = y + inYOff;
        }
        //Since setting a left and top is relative to the last absolute container we need to subtract the container
        var parentWindowLocation = getAbsoluteContainerPosition(inShowDiv);
		x = x - parentWindowLocation.x;
		y = y - parentWindowLocation.y;

        inShowDiv.style.left = x + "px"; inShowDiv.style.top = y + "px";
    }
},
toggleVis: function(id, vis) 
{ 
    var el = document.getElementById(id);
    if (el) 
    {
	    el.style.visibility = vis;
	    if( vis = "visible" )
	    {
	    	this.showing = id;
	    }
	    else
	    {
	    	this.showing = null;
	    }
	}
	else
	{
		//alert( "Could not find" + id )
	}
}
}

getAbsolutePosition = function(element) {
    var r = { x: element.offsetLeft, y: element.offsetTop };
    if (element.offsetParent) {
      var tmp = getAbsolutePosition(element.offsetParent);
      r.x += tmp.x;
      r.y += tmp.y;
    }
    return r;
  };
  
getAbsoluteContainerPosition  = function(element) 
{
	if( element.style.position == "absolute" )
	{
		return getAbsolutePosition( element );	
	} 
	else if(element.offsetParent)
    {
      var tmp = getAbsoluteContainerPosition(element.offsetParent);
	  return tmp;
	}
    var r = { x: 0, y: 0 };
    return r;
};

  
var oeviewport = {
  getWinWidth: function () { //total width of the window
    this.width = 0;
    if (window.innerWidth) this.width = window.innerWidth - 18;
    else if (document.documentElement && document.documentElement.clientWidth) 
  		this.width = document.documentElement.clientWidth;
    else if (document.body && document.body.clientWidth) 
  		this.width = document.body.clientWidth;
  	return this.width;
  },
  getWinHeight: function () {
    this.height = 0;
    if (window.innerHeight) this.height = window.innerHeight - 18;
  	else if (document.documentElement && document.documentElement.clientHeight) 
  		this.height = document.documentElement.clientHeight;
  	else if (document.body && document.body.clientHeight) 
  		this.height = document.body.clientHeight;
  	return this.height;
  },
  getScrollX: function () { //how far over are we on the X axis
    this.scrollX = 0;
  	if (typeof window.pageXOffset == "number") this.scrollX = window.pageXOffset;
  	else if (document.documentElement && document.documentElement.scrollLeft)
  		this.scrollX = document.documentElement.scrollLeft;
  	else if (document.body && document.body.scrollLeft) 
  		this.scrollX = document.body.scrollLeft; 
  	else if (window.scrollX) this.scrollX = window.scrollX;
  	return this.scrollX;
  },
  getScrollY: function () {
    this.scrollY = 0;    
    if (typeof window.pageYOffset == "number") this.scrollY = window.pageYOffset;
    else if (document.documentElement && document.documentElement.scrollTop)
  		this.scrollY = document.documentElement.scrollTop;
  	else if (document.body && document.body.scrollTop) 
  		this.scrollY = document.body.scrollTop; 
  	else if (window.scrollY) this.scrollY = window.scrollY;
  	
  	return this.scrollY;
  },
  getAll: function () {
    this.getWinWidth(); this.getWinHeight();
    this.getScrollX();  this.getScrollY();
  }
}

