package org.entermediadb.model;

import java.util.ArrayList;

import javax.mail.internet.InternetAddress;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.openedit.TestFixture;

public class MailTest extends BaseEnterMediaTest
{
	/**
	 * Constructor for ItemEditTest.
	 * @param arg0
	 */
	public MailTest(String arg0)
	{
		super(arg0);
	}
	
	public void testBCCEmail()
	{
		TestFixture ts = getFixture();
		PostMail pm = (PostMail) ts.getModuleManager().getBean("postMail");
		
		TemplateWebEmail mail = pm.getTemplateWebEmail();
		//build two lists of recipients
		String [][] cclist = new String[][]{
			{"shawn","best","shawn@silverstead.com"}//add more here
		};
		String [][] bcclist = new String[][]{
			{"shawn","best","shawn@ijsolutions.com"}//add more here
		};
		ArrayList<InternetAddress> ccs = new ArrayList<InternetAddress>();
		for (String[] details:cclist){
			InternetAddress r = new InternetAddress();
			r.setAddress(details[2]);
			ccs.add(r);
		}
		ArrayList<InternetAddress> bccs = new ArrayList<InternetAddress>();
		for (String[] details:bcclist){
			InternetAddress r = new InternetAddress();
			//r.setFirstName(details[0]);
			//r.setLastName(details[1]);
			r.setAddress(details[2]);
			bccs.add(r);
		}
		mail.setRecipients(ccs);
		mail.setBCCRecipients(bccs);
		mail.setSubject("JUnit Test");
		mail.setMessage("JUnit Test - Testing Mail");
		mail.setFrom("noreply@entermediasoftware.com");
		
//		PostMailStatus status = mail.sendAndCollectStatus();
//		
//		assertNotNull(status);
//		assertTrue(status.isSent());
//		assertNotNull(status.getId());
//		assertNotNull(status.getStatus());
		
		/*if (pm instanceof ElasticPostMail)
		{
			ElasticPostMailStatus estatus = ((ElasticPostMail) pm).getMailStatus(status.getId());
			System.out.println(estatus);
			System.out.println("waiting 2 minutes");
			try{
				Thread.sleep(1000 * 60 * 2);
			}catch (Exception e){}
			estatus = ((ElasticPostMail) pm).getMailStatus(status.getId());
			System.out.println(estatus);
		}*/
		
	}
	
}
