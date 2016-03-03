import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.store.*

import org.openedit.page.*

MediaArchive mediaarchive = context.getPageValue("mediaarchive");

String assetid = context.getRequestParameter("assetid");
Asset asset = mediaarchive.getAsset(assetid);
asset.setProperty("editstatus","1");
asset.setProperty("importstatus", "needsdownload");

Data transcoder = mediaarchive.getData("transcoder","wowza");

String serverid = transcoder.get("serverid");

asset.setProperty("fetchurl", "http://${serverid}/livestreams/content/livestream${asset.getId()}.flv")

mediaarchive.saveAsset(asset, context.getUser());

mediaarchive.fireSharedMediaEvent("importing/fetchdownloads");


String appid = context.findValue("applicationid");

String path = "/" + appid + "/views/modules/asset/editor/viewer/index.html?assetid=${asset.getId()}";
context.redirect(path);

