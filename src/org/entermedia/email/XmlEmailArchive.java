package org.entermedia.email;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.data.SearcherManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.IntCounter;
import com.openedit.util.URLUtilities;
import com.openedit.util.XmlUtil;

public class XmlEmailArchive implements EmailArchive {

	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	protected UserManager fieldUserManager;
	protected SearcherManager fieldSearcherManager;
	protected EmailSearcher fieldEmailSearcher;
	protected String fieldCatalogId;
	protected PostMail fieldPostMail;
	protected IntCounter fieldIdCounter;
	protected File fieldRoot;
	
	
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}
	

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();

		}

		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	

	public File getRoot() {
		return fieldRoot;
	}

	public void setRoot(File inRoot) {
		fieldRoot = inRoot;
	}

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	public void setIdCounter(IntCounter inIdCounter) {
		fieldIdCounter = inIdCounter;
	}

	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail) {
		fieldPostMail = inPostMail;
	}

	public void clear() {
		// TODO Auto-generated method stub

	}

	public TemplateWebEmail createEmail(String inId) throws OpenEditException {
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.setId(inId);
		return email;
	}

	public int createId() {
		int count = getIdCounter().incrementCount();
		return count;
	}

	
	//This is a sourcepath now.
	
	public TemplateWebEmail loadEmail(String inId) throws OpenEditException {

		Page page = getPageManager().getPage(getEmailPath() + inId + ".xml");
		getPageManager().clearCache(page);
		if (!page.exists()) {
			return null;
		}
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		
		Element root = getXmlUtil().getXml(page.getReader(), "UTF-8");
		email.setId(root.attributeValue("id"));
		email.setFrom(root.elementText("from"));
		ArrayList to = new ArrayList();
		for (Iterator iterator = root.elementIterator("to"); iterator.hasNext();) {
			Element property = (Element) iterator.next();
			String value = property.getText();
			Recipient r = new Recipient();
			r.setEmailAddress(property.attributeValue("email"));
			r.setFirstName(property.attributeValue("firstname"));
			r.setLastName(property.attributeValue("lastname"));

			to.add(r);

		}

		email.setRecipients(to);
		email.setSubject(root.elementText("subject"));
		email.setAlternativeMessage(URLUtilities.xmlUnescape(root.elementText("alternative")));
		email.setMessage(URLUtilities.xmlUnescape(root.elementText("message")));
		email.setMailTemplatePath(root.elementText("templatepath"));
		Element properties = root.element("properties");
		if(properties != null){
		for (Iterator iterator = properties.elementIterator("property"); iterator.hasNext();) {
			Element property = (Element) iterator.next();
			String id = property.attributeValue("id");
			String value = property.getText();
			email.setProperty(id, value);
			}
		}
		ArrayList files = new ArrayList();
		for (Iterator iterator = root.elementIterator("file"); iterator.hasNext();) {
			Element file = (Element) iterator.next();
			String filename = file.getText();
			files.add(filename);
		}
		email.setFileAttachments(files);
		String datestring = root.elementText("sent-date");
		if (datestring != null && datestring.length() > 0) 
		{
			Date date = DateStorageUtil.getStorageUtil().parseFromStorage(datestring);
			email.setSendDate(date);
		}
		String sent = root.elementText("sent");
		email.setSent(Boolean.parseBoolean(sent));
		String userId = root.elementText("user");
		if (userId != null) {
			User user = getUserManager().getUser(userId);
			email.setUser(user);
		}
		email.setSourcePath(inId);
		email.setProperty("sourcepath", inId);
		return email;

	}

	
	public String getEmailPath() {

		return "/WEB-INF/data/" + getCatalogId() + "/email/";
	}

	public void saveEmail(TemplateWebEmail inEmail, User inUser) {
		
		if(inEmail.getId() == null){
			int id = getIdCounter().incrementCount();
			inEmail.setId(String.valueOf(id));
		}
		Element root = DocumentHelper.createElement("email");
		root.addAttribute("id", inEmail.getId());
		if (inEmail.getFrom() != null) {
			root.addElement("from").setText(inEmail.getFrom());
		}
		if (inEmail.getFromName() != null) {
			root.addElement("from-name").setText(inEmail.getFromName());
		}
		if (inEmail.getRecipients() != null) {
			List recipients = inEmail.getRecipients();
			for (Iterator iterator = recipients.iterator(); iterator.hasNext();) {
				Recipient recip = (Recipient) iterator.next();
				Element to = root.addElement("to");
				if (recip.getEmailAddress() != null) {
					to.addAttribute("email", recip.getEmailAddress());
				}
				if (recip.getFirstName() != null) {
					to.addAttribute("firstname", recip.getFirstName());
				}
				if (recip.getLastName() != null) {
					to.addAttribute("lastname", recip.getLastName());
				}

			}
		}
		if (inEmail.getSubject() != null) {
			root.addElement("subject").setText(inEmail.getSubject());
		}
		if (inEmail.getAlternativeMessage() != null) {
			root.addElement("alternative").setText(URLUtilities.xmlEscape(inEmail.getAlternativeMessage()));
		}
		if (inEmail.getMessage() != null) {
			root.addElement("message").setText(URLUtilities.xmlEscape(inEmail.getMessage()));
		}

		if (inEmail.getMailTemplatePath() != null) {
			root.addElement("templatepath").setText(inEmail.getMailTemplatePath());
		}
		if(inEmail.getProperties().size() >0){
			Element properties = root.addElement("properties");
		for (Iterator iterator = inEmail.getProperties().keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String val = inEmail.getProperty(key);
			if(val != null){
				Element property = properties.addElement("property");
				property.addAttribute("id", key);
				property.setText(val);
			}
			if (inEmail.getSendDate() != null) {
				Element property = properties.addElement("property");
				property.addAttribute("id", "sent-date");
				property.setText(inEmail.getFormattedSendDate());
			}
		}
		}
		if (inEmail.getSendDate() != null) {
			root.addElement("sent-date").setText(inEmail.getFormattedSendDate());
		}
		for (Iterator iterator = inEmail.getFileAttachments().iterator(); iterator.hasNext();) {
			String filename = (String) iterator.next();
			root.addElement("file").setText(filename);
		}

		root.addElement("sent").setText(Boolean.toString(inEmail.isSent()));
		if (inEmail.getUser() != null) {
			root.addElement("user").setText(inEmail.getFormattedSendDate());
		}
		Page page = getPageManager().getPage(getEmailPath() + inEmail.getId() + ".xml");

		OutputStream out = getPageManager().saveToStream(page);
		getXmlUtil().saveXml(root, out, "UTF-8");
	}

	public IntCounter getIdCounter() {
		if (fieldIdCounter == null) {
			fieldIdCounter = new IntCounter();
			
			ContentItem item = getPageManager().getRepository().get("/" + getCatalogId() + "/data/sentemail.properties");
			File upload = new File(item.getAbsolutePath());
			upload.getParentFile().mkdirs();
			
			fieldIdCounter.setCounterFile(upload);
		}
		return fieldIdCounter;
	}


	public List getAllEmailIds() {
		ArrayList ids = new ArrayList();
		List strings = getPageManager().getChildrenPaths(getEmailPath(), false);
		for (Iterator iterator = strings.iterator(); iterator.hasNext();) {
			String path = (String) iterator.next();
			if(path.endsWith(".xml")){
				String id = path.substring(path.lastIndexOf("/")+1,path.indexOf(".xml"));
				ids.add(id);
			}
			
		}
		return ids;
		
		
	}

	public String getEmailArchiveHome() {
		return "/" + getCatalogId();
	}

	public EmailSearcher getEmailSearcher() {
		if (fieldEmailSearcher == null) {
			fieldEmailSearcher = (EmailSearcher) getSearcherManager().getSearcher(getCatalogId(), "email");
			fieldEmailSearcher.setEmailArchive(this);
		}
		return fieldEmailSearcher;
	}

}
