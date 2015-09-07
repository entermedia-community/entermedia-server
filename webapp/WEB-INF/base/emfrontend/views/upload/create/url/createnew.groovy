import groovy.json.JsonSlurper

import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.store.*
import org.openedit.util.DateStorageUtil

import com.openedit.page.*


MediaArchive mediaarchive = context.getPageValue("mediaarchive");

Asset asset = new Asset();
asset.setId(mediaarchive.getAssetSearcher().nextAssetNumber());
String sourcepath = "newassets/${context.getUserName()}/${asset.id}";
asset.setSourcePath(sourcepath);
asset.setFolder(true);
asset.setProperty("owner", context.userName);
asset.setProperty("importstatus", "needsdownload")
asset.setProperty("datatype", "original");
asset.setProperty("assetaddeddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));


//String assettype = context.getRequestParameter("assettype");
//asset.setProperty("assettype", assettype);
branch = mediaarchive.getCategoryArchive().createCategoryTree("/newassets/${context.getUserName()}");
asset.addCategory(branch);

String[] fields = context.getRequestParameters("field");
if(fields != null) {
	mediaarchive.getAssetSearcher().updateData(context,fields,asset);
}

//n7GxnhQjBaw/hqdefault.jpg
String externalmediainput = context.getRequestParameter("externalmediainput");
String fetchthumb = null;
if( externalmediainput.startsWith("https://youtu.be/") )
{
	//set the thumbnail
	//https://youtu.be/n7GxnhQjBaw
	String link = externalmediainput.substring(17);
	fetchthumb = "http://img.youtube.com/vi/" + link + "/hqdefault.jpg";
}
else if (externalmediainput.contains("youtube.com/") )
{
	//https://www.youtube.com/watch?v=n7GxnhQjBaw
	String link = externalmediainput.substring(externalmediainput.indexOf("watch?v=") + 8);
	fetchthumb = "http://img.youtube.com/vi/" + link + "/hqdefault.jpg";
}
else if (externalmediainput.contains("vimeo") )
{
	//482375085
	String vimeoVideoID = externalmediainput.substring(externalmediainput.lastIndexOf("/") + 1);
	
	URL apiUrl = new URL('https://vimeo.com/api/v2/video/' + vimeoVideoID + '.json');

	String text = apiUrl.text;
	def video = new JsonSlurper().parseText(text).get(0);
	
	//fetchthumb = "http://i.vimeocdn.com/video/" + link + ".webp?mw=960&mh=540";
	fetchthumb = video.thumbnail_large;
	asset.setProperty("longcaption", video.description);
	asset.setProperty("assettitle", video.title);
	
	asset.addKeywords(video.tags);
}


//TODO: Use some parser interface and grab more metadata from youtube or vimeo, flickr
if( fetchthumb != null)
{
	asset.setProperty("fetchurl",fetchthumb);
	asset.setProperty("linkurl",externalmediainput);
}
else
{
	asset.setProperty("fetchurl",externalmediainput);	
}
//String embed =  context.getRequestParameter("embeddedurl.value") 
//if( embed != null )
//{
//	asset.setProperty("fileformat","embedded");	
//}

asset.setProperty("importstatus","needsdownload");
//See if embed video is set? if not then fill it in?

mediaarchive.saveAsset(asset, context.getUser());

context.putPageValue("asset", asset);
context.setRequestParameter("assetid", asset.id);
context.setRequestParameter("sourcepath", asset.sourcePath);


//category = product.defaultCategory;
//webTree = context.getPageValue("catalogTree");
//webTree.treeRenderer.setSelectedNode(category);
//webTree.treeRenderer.expandNode(category);
//
//context.putPageValue("category", category);
//moduleManager.execute("CatalogModule.loadCrumbs", context );

//String sendto = context.findValue("sendtoeditor");
//
//if (Boolean.parseBoolean(sendto))
//{
//	context.redirect("/" + editor.store.catalogId + "/admin/products/editor/" + product.id + ".html");
//}

mediaarchive.fireSharedMediaEvent("importing/fetchdownloads");

String tosourcepath = context.findValue("redirecttosourcepath");

if (Boolean.parseBoolean(tosourcepath))
{
	String path = "$apphome/views/modules/asset/editor/generalinformation/index.html?assetid=${asset.id}&edit=true";
	
	context.redirect(path);
}

