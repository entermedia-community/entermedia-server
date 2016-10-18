package org.entermediadb.data;

import java.util.Collection;

import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.users.User;
import org.openedit.xml.XmlArchive;

public interface DataArchive extends CatalogEnabled
{

	void setXmlArchive(XmlArchive inXmlArchive);

	void setDataFileName(String inDataFileName);

	void setElementName(String inSearchType);

	void setPathToData(String inPathToData);

	XmlArchive getXmlArchive();

	void delete(Data inData, User inUser);

	void saveData(Data inData, User inUser);
	
	public void saveAllData(Collection<Data> inAll, User inUser);

	void setCatalogId(String inId);
	String getCatalogId();


}
