package data;

import org.dom4j.Attribute
import org.dom4j.Element
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.openedit.Data
import org.openedit.data.*
import org.openedit.util.*
import org.openedit.page.Page
import org.openedit.util.XmlUtil

public void init()
{
	Searcher sg = mediaarchive.getSearcher("settingsgroup");
	PropertyDetails details = sg.getPropertyDetails();
	
	ArrayList data = new ArrayList();

	Page upload = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + sg.getCatalogId() + "/dataexport/lists/settingsgroup.xml");
	
	XmlUtil util = new XmlUtil();
	Element root = util.getXml(upload.getReader(),"utf-8");
	for(Iterator iterator = root.elementIterator(); iterator.hasNext();)
	{
		Element row = iterator.next();
		List perms = new ArrayList();
		for(Iterator iterator2 = row.attributes().iterator(); iterator2.hasNext();)
		{
			Attribute attr = iterator2.next();
			if( Boolean.valueOf(attr.getValue() ) )
			{
				perms.add(attr.getQualifiedName() );
			}
		}
		String id = row.attributeValue("id");
		Data existing = sg.searchById(id);
		if( existing == null)
		{
			existing = sg.createNewData();
		}
		existing.setId(id);
		existing.setName(row.attributeValue("name"));
		existing.setValue("permissions", perms);
				log.info("Saving permissions " + perms );
		//sg.saveData(existing, null);
		data.add(existing);
	   }
	  sg.saveAllData(data,null);
}

init();