package asset;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page


public void init(){
	
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher notificationsearcher = archive.getSearcher("notification");
	Date now = new Date();
	HitTracker pendingnotifications = notificationsearcher.query().before("senddate", now).exact("notificationstatus", "pending").search();
	ArrayList tosave = new ArrayList();
	pendingnotifications.each{
		Data hit = notificationsearcher.loadData(it);
		if(dispatchEmail(archive, context, hit)){
		
		hit.setValue("notificationstatus", "sent");
		tosave.add(hit);
		}
		
	}

	pendingnotifications = notificationsearcher.query().exact("notificationstatus", "pending").search();
	pendingnotifications.each{
		Data hit = notificationsearcher.loadData(it);
		if( hit.getValue("notificationdate") == null){
			if(dispatchEmail(archive, context, hit)){
				hit.setValue("notificationstatus", "sent");
				tosave.add(hit);
			}
		}
	}
	
	notificationsearcher.saveAllData(tosave, null);
	
	
	
	
	
}

public boolean dispatchEmail(MediaArchive inArchive, WebPageRequest inReq, Data inNotificationRequest){
	PostMail mail = (PostMail)inArchive.getModuleManager().getBean( "postMail");
	
	Data setting = inArchive.getCatalogSetting("events_notify_app");
	String appid = setting.get("value");
	
	String templatePath = "/${appid}/theme/emails/${inNotificationRequest.notificationtype}-template.html";
	Page template = inArchive.getPageManager().getPage(templatePath);
	if(!template.exists()){
		 template = inArchive.getPageManager().getPage("/${appid}/theme/emails/generic-template.html");
		
	}
	String from = inNotificationRequest.get("notificationfrom");
	if(from == null){
		from = inArchive.getCatalogSettingValues("system_from_email");
	}
	
	String subject = inNotificationRequest.get("notificationsubject");
	if(subject == null){
		subject = "EnterMedia Notification";
	}
	
	
	
	
	WebPageRequest newcontext = inReq.copy(template);
	newcontext.putPageValue("notification",inNotificationRequest);
	TemplateWebEmail mailer = mail.getTemplateWebEmail();
	mailer.setFrom(from);
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePath);
	mailer.setRecipientsFromCommas(inNotificationRequest.getValues("notificationemails"));
	
	mailer.setSubject(subject);
	mailer.send();
	return mailer.isSent();
}





init();