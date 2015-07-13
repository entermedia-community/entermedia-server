import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.store.*
import org.openedit.util.DateStorageUtil
import com.openedit.page.*

MediaArchive mediaarchive = context.getPageValue("mediaarchive");

String assetid = context.getRequestParameter("assetid");
Asset asset = mediaarchive.getAsset(assetid);
asset.setProperty("editstatus","1");
asset.setProperty("importstatus","uploading");

mediaarchive.saveAsset(asset, context.getUser());

String appid = context.findValue("applicationid");

String path = "/" + appid + "/views/modules/asset/editor/viewer/index.html?assetid=${asset.getId()}";
context.redirect(path);

