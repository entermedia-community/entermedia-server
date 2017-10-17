import org.openedit.entermedia.MediaArchive

	

System.out.println("sending mails!");

hits =  context.getSessionValue("mailhits");
MediaArchive archive = context.getPageValue("mediaarchive");
postmail = archive.getModuleManager().getBean("postMail");
String appid = context.findValue("applicationid");
String module = context.findValue("module");

mailer = postmail.getTemplateWebEmail();
	template = archive.getPageManager().getPage("/${applicationid}/views/modules/${module}/quickmail/template.html");
	mailer.loadSettings(context.copy(template));
	mailer.setMailTemplatePage(template);
	String subject = context.getRequestParameter("subject");
	if(subject == null){
	subject = context.findValue("subject");
	}


	if(subject == null){
	subject ="Mail";
	}

	mailer.setSubject(subject);
	
	String from = context.getRequestParameter("from");
	if(from == null){
		from = context.findValue("from");
	}
	if(from == null){
		from="flowthink-mailer@flowthink.ca";
	}
	mailer.setFrom(from);
	
		for (Iterator iter = hits.getSelectedHits().iterator(); iter.hasNext();)
		{
			user =  iter.next();
			context.putPageValue("target", user);
			String email = user.get("email");
			if (email != null)
			{
				//recipient = new Recipient();
				//recipient.setEmailAddress(email);
				//recipient.setLastName(user.get("lastname"));
				//recipient.setFirstName(user.get("firstname"));
				mailer.setTo(email);
				mailer.setFrom(from);
				
				try{
				
				mailer.send();
				} catch (Exception e) {
				
				System.out.println("sending mails!" + e.printStackTrace());

				}
			}
		}
		context.putPageValue("ok", true);