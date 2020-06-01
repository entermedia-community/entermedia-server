$(document).ready(function()
{

	(function( $ ) {
		 
	    $.fn.emgrid = function() {
	 
            var grid = $( this );
            var colwidth = parseInt(grid.data("colwidth"));
            //get colum width divide by div width
            var colcount = grid.width() / colwidth; 
            colcount = Math.floor(colcount);
            console.log(colcount);
            //adjust the colwidth to spread out the extra space
            var remainder = grid.width() - (colcount * colwidth);
            colwidth = colwidth + remainder/colcount;
            
            var columns = [];
            for(var i=0;i<colcount;i++)
            {
            	columns[i] = 0;
            }
            //debugger;
            var col = 0;
	    	
	        this.children( ".emgridcell" ).each(function() 
	        {
      	        var cell = $(this);
      	        cell.css("width",colwidth-10 + "px");
      	        var curheight = columns[col];
      	        var cellheight = cell.height();
      	        cell.css("top",curheight + "px");
      	        columns[col] = curheight + cellheight + 10;
      	        //left
      	        var left = colwidth * col + 10;
      	        cell.css("left",left + "px");
      	        col++;
      	        if( col == colcount)
      	        {
      	        	col = 0;
      	        }
            });		
	        var tallest = 0;
	        for(var i=0;i<colcount;i++)
            {
            	if( tallest < columns[i] )
            	{
            		tallest = columns[i];
            	}
            }
            grid.css("height",tallest + 'px');
	        
	        return this;
	 
	    };
	 
	}( jQuery ));
	
	lQuery(".emgrid").livequery(function() {
		var thegrid = $(this);
		thegrid.emgrid();
	});
});
