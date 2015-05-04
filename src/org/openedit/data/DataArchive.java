package org.openedit.data;

import java.util.Collection;

import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.xml.XmlArchive;

import com.openedit.users.User;

public interface DataArchive
{

	void setXmlArchive(XmlArchive inXmlArchive);

	void setDataFileName(String inDataFileName);

	void setElementName(String inSearchType);

	void setPathToData(String inPathToData);

	XmlArchive getXmlArchive();

	void delete(String inCatalogId, Data inData, User inUser);

	void saveData(String inCatalogId, Data inData, User inUser);
	
	public void saveAllData(String inCatalogId, Collection<Data> inAll, User inUser);


}
