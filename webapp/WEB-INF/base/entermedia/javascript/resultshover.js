/*************************************************************************

    dw_event.js (version date Feb 2004)

        

    This code is from Dynamic Web Coding at http://www.dyn-web.com/

    See Terms of Use at http://www.dyn-web.com/bus/terms.html

    regarding conditions under which you may use this code.

    This notice must be retained in the code as is!

*************************************************************************/



var dw_event = {

  

  add: function(obj, etype, fp, cap) {

    cap = cap || false;

    if (obj.addEventListener) obj.addEventListener(etype, fp, cap);

    else if (obj.attachEvent) obj.attachEvent("on" + etype, fp);

  }, 



  remove: function(obj, etype, fp, cap) {

    cap = cap || false;

    if (obj.removeEventListener) obj.removeEventListener(etype, fp, cap);

    else if (obj.detachEvent) obj.detachEvent("on" + etype, fp);

  }, 



  DOMit: function(e) { 

    e = e? e: window.event;

    e.tgt = e.srcElement? e.srcElement: e.target;

    

    if (!e.preventDefault) e.preventDefault = function () { return false; }

    if (!e.stopPropagation) e.stopPropagation = function () { if (window.event) window.event.cancelBubble
 = true; }

        

    return e;

  }

  

}

/*************************************************************************

  dw_tooltip.js   requires: dw_event.js and dw_viewport.js

  version date: May 21, 2005 moved init call to body onload

  (March 14, 2005: minor changes in position algorithm and timer mechanism)

  

  This code is from Dynamic Web Coding at dyn-web.com

  Copyright 2003-5 by Sharon Paine 

  See Terms of Use at www.dyn-web.com/bus/terms.html

  regarding conditions under which you may use this code.

  This notice must be retained in the code as is!

*************************************************************************/



var Tooltip = {

    followMouse: true,

    offX: 8,

    offY: 12,

    tipID: "tipDiv",

    showDelay: 900,

    hideDelay: 200,

    

    ready:false, timer:null, tip:null, 

  

    init: function() {  

        if ( document.createElement && document.body && typeof document.body.appendChild
 != "undefined" ) {

            if ( !document.getElementById(this.tipID) ) {

                var el = document.createElement("DIV");

                el.id = this.tipID; document.body.appendChild(el);

            }

            this.ready = true;

        }

    },

    

    show: function(e, msg) {

        if (this.timer) { clearTimeout(this.timer);	this.timer = 0; }

        this.tip = document.getElementById( this.tipID );

        if (this.followMouse) // set up mousemove 

            dw_event.add( document, "mousemove", this.trackMouse, true );

        this.writeTip("");  // for mac ie

        this.writeTip(msg);

        viewport.getAll();

        this.positionTip(e);

        this.timer = setTimeout("Tooltip.toggleVis('" + this.tipID + "', 'visible')",
 this.showDelay);

    },

    

    writeTip: function(msg) {

        if ( this.tip && typeof this.tip.innerHTML != "undefined" ) this.tip.innerHTML
 = msg;

    },

    

    positionTip: function(e) {

        if ( this.tip && this.tip.style ) {
			
            // put e.pageX/Y first! (for Safari)

            var x = e.pageX? e.pageX: e.clientX + viewport.scrollX;

            var y = e.pageY? e.pageY: e.clientY + viewport.scrollY;

            if ( x + this.tip.offsetWidth + this.offX > viewport.width + viewport.scrollX ) {

                x = x - this.tip.offsetWidth - this.offX;

                if ( x < 0 ) x = 0;

            } else 
            {
            	x = x + this.offX;
            }

            if ( y + this.tip.offsetHeight + this.offY > viewport.height + viewport.scrollY ) {

                y = y - this.tip.offsetHeight - this.offY;

                if ( y < viewport.scrollY ) y = viewport.height + viewport.scrollY - this.tip.offsetHeight;

            } else 
            {
            	y = y + this.offY;
            }

            this.tip.style.left = x + "px"; this.tip.style.top = y + "px";

        }

    },

    

    hide: function() {

        if (this.timer) { clearTimeout(this.timer);	this.timer = 0; }

        this.timer = setTimeout("Tooltip.toggleVis('" + this.tipID + "', 'hidden')",
 this.hideDelay);

        if (this.followMouse) // release mousemove

            dw_event.remove( document, "mousemove", this.trackMouse, true );

        this.tip = null; 

    },



    toggleVis: function(id, vis) { // to check for el, prevent (rare) errors

        var el = document.getElementById(id);

        if (el) el.style.visibility = vis;

    },

    

    trackMouse: function(e) {

    	e = dw_event.DOMit(e);

     	Tooltip.positionTip(e);

    }

    

}

/*************************************************************************



  dw_viewport.js

  version date Nov 2003

  

  This code is from Dynamic Web Coding 

  at http://www.dyn-web.com/

  Copyright 2003 by Sharon Paine 

  See Terms of Use at http://www.dyn-web.com/bus/terms.html

  regarding conditions under which you may use this code.

  This notice must be retained in the code as is!



*************************************************************************/  

  

var viewport = {

  getWinWidth: function () {

    this.width = 0;

    if (window.innerWidth) this.width = window.innerWidth - 18;

    else if (document.documentElement && document.documentElement.clientWidth) 

  		this.width = document.documentElement.clientWidth;

    else if (document.body && document.body.clientWidth) 

  		this.width = document.body.clientWidth;

  },

  

  getWinHeight: function () {

    this.height = 0;

    if (window.innerHeight) this.height = window.innerHeight - 18;

  	else if (document.documentElement && document.documentElement.clientHeight) 

  		this.height = document.documentElement.clientHeight;

  	else if (document.body && document.body.clientHeight) 

  		this.height = document.body.clientHeight;

  },

  

  getScrollX: function () {

    this.scrollX = 0;

  	if (typeof window.pageXOffset == "number") this.scrollX = window.pageXOffset;

  	else if (document.documentElement && document.documentElement.scrollLeft)

  		this.scrollX = document.documentElement.scrollLeft;

  	else if (document.body && document.body.scrollLeft) 

  		this.scrollX = document.body.scrollLeft; 

  	else if (window.scrollX) this.scrollX = window.scrollX;

  },

  

  getScrollY: function () {

    this.scrollY = 0;    

    if (typeof window.pageYOffset == "number") this.scrollY = window.pageYOffset;

    else if (document.documentElement && document.documentElement.scrollTop)

  		this.scrollY = document.documentElement.scrollTop;

  	else if (document.body && document.body.scrollTop) 

  		this.scrollY = document.body.scrollTop; 

  	else if (window.scrollY) this.scrollY = window.scrollY;

  },

  

  getAll: function () {

    this.getWinWidth(); this.getWinHeight();

    this.getScrollX();  this.getScrollY();

  }

  

}

/*************************************************************************
  This code is from Dynamic Web Coding at dyn-web.com
  Copyright 2003-5 by Sharon Paine 
  See Terms of Use at www.dyn-web.com/bus/terms.html
  regarding conditions under which you may use this code.
  This notice must be retained in the code as is!
*************************************************************************/

function doTooltip(e, ar) {
    if ( typeof Tooltip == "undefined" )
    {
    	return;
    }
    if( !Tooltip.ready ) 
    {
    	Tooltip.init();
    }
    var cntnt = wrapTipContent(ar);
    var tip = document.getElementById( Tooltip.tipID );
    Tooltip.show(e, cntnt);
    //alert(document.getElementById('hoverimg').width);
   // document.getElementById('hovertext').width = document.getElementById('hoverimg').width;
}

function hideTip() {
    if ( typeof Tooltip == "undefined" || !Tooltip.ready ) return;
    Tooltip.hide();
}

function wrapTipContent(ar) {
    var cntnt = "";
    if ( ar[0] ) cntnt += '<div class="img"><img src="' + ar[0] + '"></div>';
    if ( ar[1] ) cntnt += '<div style="width: 300px;" class="txt">' + ar[1] + '</div>';
    return cntnt;
}