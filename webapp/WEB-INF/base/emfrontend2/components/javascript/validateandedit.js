
var openDetail = "";
var app,home,apphome,themeprefix;



$(document).ready(function() 
{
	app = $("#application");
	home =  app.data("home");
	apphome = home + app.data("apphome");
	themeprefix = app.data("home") + app.data("themeprefix");
	
	$(document).on('change', ".lenguagepicker", function()
	{
		//Makes sure the name matches the new value
		var select = $(this);
		var div = $(this).closest(".languagesaddform");
		var langinput = $(".langvalue",div);
		var detailid = select.data("detailid");
		var lang = select.val();
		langinput.attr("name",detailid + "." +  lang + ".value" );
	});
	
	lQuery("textarea").livequery("keydown",function(e) {
		  var $this, end, start;
		  if (e.keyCode === 9) {
		    start = this.selectionStart;
		    end = this.selectionEnd;
		    $this = $(this);
		    $this.val($this.val().substring(0, start) + "\t" + $this.val().substring(end));
		    this.selectionStart = this.selectionEnd = start + 1;
		    return false;
		  }
		});
	
	lQuery(".languagesavebtn").livequery('click', function(event){
		event.stopPropagation();
		event.preventDefault();
		
		var btn = $(this);
		var url =  btn.attr('href');
		var detailid = btn.data('detailid');
		
		var div = btn.closest(".emdatafieldvalue"); 
		
		var count = $("#languagesextra_" + detailid, div ).data("count");
		count = count + 1;
		var languages = [];
		var args = {oemaxlevel : 1, 
					detailid : detailid,
					count: count,
					usedlanguages : [] 
					};
		
		$(".lenguagepicker", div).each(function()
		{
			var value = $(this).val();
			args.usedlanguages.push(value); 
		});
		$.get(url,args, function(data) {
			var selectlist = $(".lenguagepicker option", data);
			if( $(selectlist).length > 0)
			{
				$("#languagesextra_"+detailid,div).append(data);
				$("#colanguagesextra_" + detailid,div ).data("count",count);
				$(document).trigger("domchanged");
			}	
		})	
		
	});
	/*
	lQuery("textarea.htmleditor").livequery(new function() 
	{
		//$("textarea.htmleditor").each(function()
		{
			//console.log("Hit" + $(this));
			var value = $(this).val()
			var textname = $(this).attr("name");
			var oFCKeditor = new FCKeditor( textname );
			oFCKeditor.Config.StylesXmlPath		= '/system/tools/html/styles.xml';
			oFCKeditor.BasePath	= "/system/tools/html/fckeditor/";
		  	oFCKeditor.ToolbarSet = "Basic";
			//oFCKeditor.Config.DefaultLanguage = "$context.getLanguage()";
			oFCKeditor.Height = 300;
			oFCKeditor.Width = 300;
	        oFCKeditor.ReplaceTextarea();
	    }
	    
	});
	*/ 
	
	lQuery(".force-validate-inputs").livequery(function() 
	{
		var theform = $(this).closest("form");

		theform.on("click", function() {
			theform.valid();
		});

		$.validator.setDefaults({
			ignore : ".ignore"
		});

		theform.validate({
			ignore : ".ignore"
		});

	});
	
	$.validator.addClassRules("validateNumber", {
	     number: true
	});

	lQuery(".inlinesave").livequery("click", function()
	{
			var queryString = $(this).closest('.inlinedata').formSerialize(); 
            var url = apphome + "/views/settings/lists/datamanager/edit/inlinesave.json";
            var targetselect = $(this).data("targetselect");
            //debugger;
			$.getJSON(url, queryString, function(data) {
				$('#' + targetselect).append($('<option>', {
				    value: data.id,
				    text: data.name
				}));
				$("#" + targetselect).val(data.id);
				
			});
			$(this).closest(".modal").modal('hide');
			
		});
	
	//End of init
	
});


showPicker = function(detailid)
{
	openDetail=detailid;
	if(!window.name)window.name='admin_parent';
	window.open( home + '/system/tools/newpicker/index.html?parentName='+window.name+'&detailid='+detailid, 'pickerwindow','alwaysRaised=yes,menubar=no,scrollbars=yes,width=1000,x=100,y=100,height=600,resizable=yes' );
	return false;
}

//TODO: Does this need to be defined on the page itself? 
SetPath = function( inUrl )
{
	var input = document.getElementById(openDetail + ".value");
	input.value = inUrl;
}
	
/*
validate = function(inCatalogId, inDataType, inView , inFieldName)
{
	var val = $("#list-" + inFieldName).val();
	var div = '#error_' + inFieldName;
	var params = {
			catalogid: inCatalogId,
			searchtype: inDataType,
			view: inView,
			field: inFieldName,
			value: val
		};
	//alert( params );
	$(div).load(apphome + '/components/xml/validatefield.html', params);
}
*/

var listchangelisteners = []; //list nodes

findOrAddNode = function(inParentId)
{
	//alert("Looking now: " + inParentId + listchangelisteners.length );
	for (var i=0; i<listchangelisteners.length; i++) 
	{
		var node = listchangelisteners[i];
		if( node.parentid == inParentId )
		{
			return node;
		}
	}
	var node = new Object();
	node.parentid = inParentId;
	node.children = [];
	listchangelisteners.push(node);
	
	return node;
}

addListListener = function( inParentFieldName, inFieldName )
{
	//an array of array
	var node = findOrAddNode(inParentFieldName);
	node.children.push(inFieldName); //append the child
}

//When a field is changed we want to validate it and update any listeners
//Find all the lists in this form. Update all of them that are marked as a listener
//parent = businesscategory, child = lob, field = product
updatelisteners = function(catalogid, searchtype,view , fieldname)
{
	var val = $("#list-" + fieldname).val();
	//validate(catalogid, searchtype, view , fieldname);

	var node = findOrAddNode(fieldname);
	
	if( node.children )
	{
		for( var i=0;i< node.children.length;i++)
		{
			var childfieldname = node.children[i];
			var element = $("#list-" + childfieldname);
			var valueselection;
			if(element.options !== undefined) {
				valueselection = element.options[element.selectedIndex].value;
			}
			var div="listdetail_" + childfieldname;
			//we are missing the data element of the children
			//required: catalogid, searchtype, fieldname, value
			//optional: query, foreignkeyid and foreignkeyvalue
			
			var rendertype = $("#" + div).data("rendertype");
			if(rendertype == "multiselect"){
				$("#" + div).load(apphome + '/components/xml/multiselect.html', {catalogid:catalogid, searchtype:searchtype, view:view, fieldname:childfieldname, foreignkeyid:fieldname, foreignkeyvalue:val, value:valueselection, oemaxlevel:1});
			} else{
				$("#" + div).load(apphome + '/components/xml/list.html', {catalogid:catalogid, searchtype:searchtype, view:view, fieldname:childfieldname, foreignkeyid:fieldname, foreignkeyvalue:val, value:valueselection, oemaxlevel:1});
			}
		}
	}
}


//searchtype parentname 

loadlist = function(indiv, catalogid, searchtype, inlabel, childfieldname, foreignkeyid, foreignkeyvalue, value )
{
	//what is this?
	$(indiv).load(apphome + '/components/xml/types/simplelist.html', {catalogid:catalogid, searchtype:searchtype, fieldname:childfieldname, label:inlabel, foreignkeyid:foreignkeyid, foreignkeyvalue:foreignkeyvalue, value:value, oemaxlevel:1});
}
//Don't use any form inputs named 'name'!
postForm = function(inDiv, inFormId)
{
	var form = document.getElementById(inFormId);
	if( $ )
	{
		var targetdiv = inDiv.replace(/\//g, "\\/");
		$(form).ajaxSubmit({
					target:"#" + targetdiv
				 });
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
	
		$("#"+inDiv).load( form.action, oOptions);
	}	
	return false;
}	

postPath = function(inCss, inPath, inMaxLevel)
{
	if( inMaxLevel == null )
	{
		inMaxLevel = 1;
	}
	$("#"+inCss).load( inPath,{oemaxlevel: inMaxLevel });	
	return false;
}

toggleBox = function(inId, togglePath, inPath)
{
	$("#"+inId).load( home + togglePath,{ pluginpath: inPath, propertyid: inId });
}	
