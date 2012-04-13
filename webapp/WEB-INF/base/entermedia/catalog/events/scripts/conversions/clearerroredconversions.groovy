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
import conversions.*;

import java.util.List;
import java.util.ArrayList;

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail

public void clearerrors()
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
	SearchQuery query = tasksearcher.createSearchQuery();
	query.addOrsGroup("status", "error");
	HitTracker newtasks = tasksearcher.search(query);
	List errors = new ArrayList(newtasks);
	
	for (Data hit in errors)
	{
		def submitted = newtasks.getDateValue(hit, "submitted");
		def grace_period = context.findValue("grace_period");
		if (submitted_days_ago(grace_period, submitted)){
			tasksearcher.delete(hit, user);
			notifyUploader(hit, mediaarchive)
		}
	}
	
}

public boolean submitted_days_ago(def num, def submitted){
	def grace_period_in_milli = num * 1000*60*60*24
	//preset time - grace period is be greater than the conversion task submission date, return true for deletion
	def is_ready_for_deletion = new Date().time - grace_period_in_milli > submitted.getTime()
	return is_ready_for_deletion
}

public void notifyUploader(Data hit, def mediaarchive){
	Asset asset = mediaarchive.getAssetBySourcePath(hit.get("sourcepath"));
	Searcher usersearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "user");
	def user = usersearcher.getUser(asset.owner);
	def admin = usersearcher.getUser("admin");
	if(user.email != null)
	{
		context.putPageValue("asset", asset);
		def url = "${mediaarchive.getCatalogHome()}/components/notification/userclearedtaskerror.html"
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

clearerrors();

