package model.projects;

import java.util.Collection;
import com.openedit.WebPageRequest;

public interface ProjectManager
{

	public abstract String getCatalogId();

	public abstract void setCatalogId(String inCatId);

	public Collection<UserCollection> loadCollections(WebPageRequest inReq);
	
	
}