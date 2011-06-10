addAsset = function(inId)
{
	jQuery(document).ready( function()
		{
			var searchform = jQuery('#uploadsearchform');
			var field = "<input type='hidden' name='field' value='id'/>"
			searchform.append(field);
			field = "<input type='hidden' name='operation' value='matches'/>";
			searchform.append(field);
			field = "<input type='hidden' name='id.value' value='"+ inId + "'/>";
			searchform.append(field);
		});
}

validationOk = function(url)
{
	clearApplets();
	var form = jQuery('#uploadform');
	form.attr('action',url);
	form.attr("targetdiv", "uploadsection");
	form.submit();
}
