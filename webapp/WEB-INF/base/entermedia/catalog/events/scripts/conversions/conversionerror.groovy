package conversions;

import org.openedit.data.Searcher 
import org.openedit.Data 
import org.openedit.entermedia.modules.*;
import org.openedit.entermedia.edit.*;

import com.openedit.WebPageRequest;
import com.openedit.page.*;
import org.openedit.entermedia.*;
import org.openedit.data.Searcher;
import com.openedit.hittracker.*;
import org.openedit.entermedia.creator.*;

import com.openedit.util.*;

import org.openedit.xml.*;
import org.openedit.entermedia.episode.*;

import java.util.List;
import java.util.ArrayList;

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.entermedia.util.*


public void sendNotify()
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	//Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher(mediaarchive.getCatalogId(), "conversiontask");
	
	def taskid = event.get("taskid");
	def task = mediaarchive.getSearcherManager().getData(mediaarchive.getCatalogId(), "conversiontask", taskid);

	Asset asset = mediaarchive.getAsset(task.get("assetid"));
	if( asset != null)
	{
		notifyUploader(task, asset, mediaarchive);
	}
	else
	{
		log.error("asset removed");
	}
}

public void notifyUploader(Data hit, def mediaarchive)
{
	Asset asset = mediaarchive.getAssetBySourcePath(hit.get("sourcepath"));
	Searcher usersearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "user");
	def user = usersearcher.getUser(asset.owner);
	def admin = usersearcher.getUser("admin");
	
	Data setting = mediaarchive.getCatalogSetting("events_notify_app");
	
	if(user.email != null)
	{
		context.putPageValue("asset", asset);
		String appid = setting.get("value");
		def url = "/${appid}/components/notification/userclearedtaskerror.html"
		context.putPageValue("toemail", user);
		sendEmail(context, user.email, url);
		context.putPageValue("toemail", admin);
		sendEmail(context, admin.email, url);
	}
	
}

protected void sendEmail(WebPageRequest context, String email, String templatePage){
	Page template = pageManager.getPage(templatePage);
	WebPageRequest newcontext = context.copy(template);
	TemplateWebEmail mailer = getMail();
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	mailer.setSubject("Conversion Error Notice");
	mailer.send();
	log.info("conversion error email sent to ${email}");
}

protected TemplateWebEmail getMail() {
	PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
	return mail.getTemplateWebEmail();
}

sendNotify();

