package resilio;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.orders.Order
import org.entermediadb.asset.orders.OrderSearcher
import org.entermediadb.asset.upload.FileUpload
import org.entermediadb.asset.upload.FileUploadItem
import org.entermediadb.asset.upload.UploadRequest
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.episode.*
import org.openedit.entermedia.modules.*
import org.openedit.entermedia.util.*
import org.openedit.page.Page
import org.openedit.users.User
import org.openedit.util.DateStorageUtil
import org.openedit.xml.*

import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.util.*


public void handleUpload() {
	
	MediaArchive archive = (MediaArchive)context.getPageValue("mediaarchive");
	archive.getModuleManager().getBean(archive.getCatalogId(), "resilioManager");
	
}
handleUpload();

