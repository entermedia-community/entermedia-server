import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.store.*

import com.openedit.page.*


MediaArchive mediaarchive = context.getPageValue("mediaarchive");

Asset asset = new Asset();
asset.setId(mediaarchive.getAssetSearcher().nextAssetNumber());
String sourcepath = "newassets/${context.getUserName()}/${asset.id}";
asset.setSourcePath(sourcepath);
asset.setFolder(true);
asset.setProperty("owner", context.userName);
asset.setProperty("importstatus", "complete")
asset.setProperty("datatype", "original");
asset.setProperty("fileformat", "flv");
asset.setProperty("editstatus", "recording");

String assettype = context.getRequestParameter("assettype");
asset.setProperty("assettype", assettype);
branch = mediaarchive.getCategoryArchive().createCategoryTree("/newassets/${context.getUserName()}");
asset.addCategory(branch);

String[] fields = context.getRequestParameters("field");
if(fields != null) {
	mediaarchive.getAssetSearcher().updateData(context,fields,asset);
}

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

String tosourcepath = context.findValue("redirecttosourcepath");

if (Boolean.parseBoolean(tosourcepath))
{
	String path = "$apphome/views/upload/create/video/recorder/index.html?assetid=${asset.id}&edit=true";
	
	context.redirect(path);
}

