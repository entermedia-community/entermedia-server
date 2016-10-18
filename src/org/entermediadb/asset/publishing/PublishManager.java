package org.entermediadb.asset.publishing;

import java.util.Date;

import org.dom4j.Element;
import org.openedit.CatalogEnabled;
import org.openedit.data.SearcherManager;
import org.openedit.page.manage.PageManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

public class PublishManager implements CatalogEnabled {

	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	protected String fieldCatalogId;
	protected XmlArchive fieldXmlArchive;
	protected Date fieldLastChecked;
	
	public Date getLastCheckedDate() {
		XmlFile settings = getXmlArchive().getXml(
				"/" + getCatalogId() + "/configuration/publishing.xml");
		if(!settings.isExist()){
			return null;
		}
		Element config = settings.getRoot();
		if(config == null){
			return null;
		}
		Element lastelem = config.element("lastchecked");
		if(lastelem == null){
			return null;
		}
		String lastchecked = lastelem.getText();
		
		return DateStorageUtil.getStorageUtil().parseFromStorage(lastchecked);
	}

	
	public void saveLastCheckedDate(Date inDate) {
		XmlFile settings = getXmlArchive().getXml(
				"/" + getCatalogId() + "/configuration/publishing.xml");
		Element config = settings.getRoot();
		Element lastchecked = config.element("lastchecked");
		if(inDate != null){
			
		
		
		if(lastchecked == null){
			lastchecked = config.addElement("lastchecked");
		}
		lastchecked.setText(DateStorageUtil.getStorageUtil().formatForStorage(inDate));
		}
		else{
			if(lastchecked != null){
				config.remove(lastchecked);
			}
		}
		getXmlArchive().saveXml(settings, null);
	}
	public void saveLastCheckedDate() {
		saveLastCheckedDate(new Date());
	}
	
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	

	public XmlArchive getXmlArchive() {
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive) {
		fieldXmlArchive = inXmlArchive;
	}
   
	
	public void clearLastRun() {
	saveLastCheckedDate(null);

	}	
	
}
