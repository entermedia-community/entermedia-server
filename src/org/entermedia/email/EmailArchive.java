package org.entermedia.email;



import java.util.List;

import com.openedit.OpenEditException;
import com.openedit.users.User;

public interface EmailArchive {

	
	
	
	public TemplateWebEmail loadEmail(String inId) throws OpenEditException;
	public void saveEmail(TemplateWebEmail inId, User inUser);
	public TemplateWebEmail createEmail(String inJobNumber) throws OpenEditException;
    public void clear();
	int createId();
	public List getAllEmailIds();
	public void setCatalogId(String inCatalogid);
	public String getEmailArchiveHome();
	public EmailSearcher getEmailSearcher();
	
}
