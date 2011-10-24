
var openDetail = "";
	
showPicker = function(detailid)
{
	openDetail=detailid;
	if(!window.name)window.name='admin_parent';
	window.open( '$home/system/tools/newpicker/index.html?parentName='+window.name+'&detailid='+detailid, 'pickerwindow','alwaysRaised=yes,menubar=no,scrollbars=yes,width=1000,x=100,y=100,height=600,resizable=yes' );
	return false;
}

//TODO: Does this need to be defined on the page itself? 
SetPath = function( inUrl )
{
	var input = document.getElementById(openDetail + ".value");
	input.value = inUrl;
}
	

validate = function(inCatalogId, inDataType, inView , inFieldName, detailprefix)
{
	if( !detailprefix)
		{
		detailprefix = "";
		}
	var field = detailprefix + inFieldName + '.value';
	var val = document.getElementById(field).value;
	var div = '#error_' +detailprefix +  inFieldName;
	var params = {
			catalogid: inCatalogId,
			searchtype: inDataType,
			view: inView,
			field: inFieldName,
			value: val
		};
	//alert( params );
	jQuery(div).load('{$home}${apphome}/components/xml/validatefield.html', params);
}

//Delete this feature. Too complex to maintain
validateall = function()
{
	alert(" Not implemented");
	return;
	
	params = "catalogid=$searcher.getCatalogId()&view=$view&type=$searcher.getFieldName()";
/*	#foreach ($detail in $details)
		#if ($detail.isEditable())
			field = '${detail.id}.value';
			val = document.getElementById(field).value;
			params = params + '&field=$detail.id&' + field + '=' + val;
			validate('$detail.id');
		#end
	#end
*/	
	jQuery("#"+'allerrors').load( '$home${apphome}/components/xml/validateallfields.html', { parameters: params });
}


//Don't use any form inputs named 'name'!
postForm = function(inDiv, inFormId)
{
	var form = document.getElementById(inFormId);
	if( jQuery )
	{
		var targetdiv = inDiv.replace(/\//g, "\\/");
		jQuery(form).ajaxSubmit( 
			{
				target:"#" + targetdiv
			}
		);	
	}
	else
	{
		form = Element.extend(form);
		var oOptions = { 
		    method: 'post',
		    parameters: form.serialize(true), 
		    evalScripts: true,
			asynchronous: false,
	        onFailure: function (oXHR, oJson) {
	              alert("An error occurred: " + oXHR.status);
	        }
	     };
	
		jQuery("#"+inDiv).load( form.action, oOptions);
	}	
	return false;
}	

postPath = function(inCss, inPath, inMaxLevel)
{
	if( inMaxLevel == null )
	{
		inMaxLevel = 1;
	}
	jQuery("#"+inCss).load( inPath,{oemaxlevel: inMaxLevel });	
	return false;
}

toggleBox = function(inId, togglePath, inPath)
{
	jQuery("#"+inId).load( '$home' + togglePath,{ pluginpath: inPath, propertyid: inId });
}	

jQuery(document).ready(function() 
{ 
		jQuery(".ajaxlistreload").livequery(
			function()
			{
				var f = jQuery(this);
				var detailprefix = f.attr("detailprefix");
				var foreignkeyid = f.attr("parentforeignkeyid");
				var parent = document.getElementById( detailprefix + foreignkeyid + '.value');		
				jQuery(parent).change(
					function()
					{
						var catalogid = f.attr("catalogid");			
						var searchtype = f.attr("searchtype");			
						var view = f.attr("view");			
						var fieldname = f.attr("fieldname");			
						var div = f.attr("targetdiv");			
						var val = jQuery(this).val();
						
						//value:valueselection,
									
						//required: catalogid, searchtype, fieldname, value
						//optional: query, foreignkeyid and foreignkeyvalue
						//alert({catalogid:catalogid, searchtype:searchtype, view:view, fieldname:fieldname, foreignkeyid:foreignkeyid, foreignkeyvalue:val, detailprefix:detailprefix});
						//alert( "#" + div + "=" + "$home${apphome}/components/xml/list.html?catalogid=" + catalogid + "&searchtype=" + searchtype + "&view=" + view + "&fieldname=" + fieldname + "&foreignkeyid=" + foreignkeyid + "&foreignkeyvalue="+ val + "&detailprefix=" + detailprefix);
						var target = document.getElementById( div); //must escape .
						jQuery(target).load('$home${apphome}/components/xml/list.html', {catalogid:catalogid, searchtype:searchtype, view:view, fieldname:fieldname, foreignkeyid:foreignkeyid, foreignkeyvalue:val, detailprefix:detailprefix});
					}
				);	
			}
		);
	
}); 