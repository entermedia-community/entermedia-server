jQuery(document).ready(function() 
 { 
	jQuery(".expandorder").livequery('click', function(e)
		{
			var url = jQuery(this).attr("href");
			var currentrow = jQuery(this).parent("tr");
			
			var str = jQuery(this).children(':first').attr('src');
			
			if (str.indexOf("plus") >= 0)
			{
				jQuery(this).children(':first').attr('src', "/visualdata/theme/images/orders/minus.png");
				jQuery.get(url, 
						function(result) 
						{
							var colspans = currentrow.children("td").length;
							var html = '<tr id="orderassettable"><td colspan="' + colspans + '">' + result + '</td></tr>';
							currentrow.after(html);
						}
					);
			}
			else
			{
				jQuery(this).children(':first').attr('src', "/visualdata/theme/images/orders/plus.png");
				currentrow.next('#orderassettable').remove();
			}
			
			
			return false;
		}
	);
 }
); 