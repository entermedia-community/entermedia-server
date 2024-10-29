package org.entermediadb.asset.importer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.entermediadb.asset.util.CSVReader;
import org.entermediadb.asset.util.Header;
import org.entermediadb.asset.util.ImportFile;
import org.entermediadb.asset.util.Row;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.util.EmStringUtils;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class BaseImporter extends EnterMediaObject
{
	public void setSearcher(Searcher inSearcher)
	{
		fieldSearcher = inSearcher;
	}
	protected Data fieldLastNonSkipData;
	protected char fieldSeparator = ',';
	protected String fieldNewdDetailPrefix = "";
	
	
	public String getNewdDetailPrefix() {
		return fieldNewdDetailPrefix;
	}

	public void setNewdDetailPrefix(String inNewdDetailPrefix) {
		fieldNewdDetailPrefix = inNewdDetailPrefix;
	}

	public char getSeparator()
	{
		return fieldSeparator;
	}

	public void setSeparator(char inSeparator)
	{
		fieldSeparator = inSeparator;
	}
	protected HashMap<String, Map> fieldLookUps;
	protected HashMap<String, String[]> fieldLookUpFilters;
	
	protected HashMap<String, String[]> getLookUpFilters()
	{
		if (fieldLookUpFilters == null)
		{
			fieldLookUpFilters = new HashMap();
		}
		return fieldLookUpFilters;
	}

	protected Searcher fieldSearcher;
	protected boolean fieldMakeId = false;
	protected String fieldPrefix;
	protected boolean fieldStripPrefix;
	
	protected boolean fieldLookUpLists = true;
	
	protected boolean isLookUpLists()
	{
		return fieldLookUpLists;
	}

	protected void setLookUpLists(boolean inLookUpLists)
	{
		fieldLookUpLists = inLookUpLists;
	}

	protected Set fieldDbLookUps = new HashSet();
	public Set getDbLookUps()
	{
		return fieldDbLookUps;
	}

	public void setDbLookUps(Set inDbLookUps)
	{
		fieldDbLookUps = inDbLookUps;
	}

	public void addDbLookUp(String inLookUp)
	{
		getDbLookUps().add(inLookUp);
	}
	public void addDbLookUpFilter(String inDetailId,String inKey, String inValue)
	{
		String[] values = new String[]{inKey,inValue};
		getLookUpFilters().put(inDetailId,values);
	}
	protected boolean fieldAddNewData = true;
	
	
	public boolean isAddNewData()
	{
		return fieldAddNewData;
	}

	public void setAddNewData(boolean inAddNewData)
	{
		fieldAddNewData = inAddNewData;
	}

	public String getPrefix() {
		return fieldPrefix;
	}

	public void setPrefix(String inPrefix) {
		fieldPrefix = inPrefix;
	}

	public boolean isStripPrefix() {
		return fieldStripPrefix;
	}

	public void setStripPrefix(boolean inStripPrefix) {
		fieldStripPrefix = inStripPrefix;
	}

	public Searcher getSearcher()
	{
		return fieldSearcher;
	}
	protected Page fieldImportPage;
	
	
	public Page getImportPage() {
		return fieldImportPage;
	}

	public void setImportPage(Page inImportPage) {
		fieldImportPage = inImportPage;
	}

	public void importData() throws Exception
	{
		fieldSearcher = loadSearcher(context);

		if( fieldImportPage == null)
		{
			String importpath = context.findValue("importpath");
			fieldImportPage = getPageManager().getPage(importpath);
		}
		Reader reader = fieldImportPage.getReader();
		ArrayList data = new ArrayList();
		int rowNum = 0;

		try
		{
			ImportFile file = new ImportFile();
			file.setParser(new CSVReader(reader, getSeparator(), '\"'));
			file.read(reader);

			Row trow = null;
			while ((trow = file.getNextRow()) != null)
			{
				rowNum++;
				
				if( skipRow(trow))
				{
					continue;
				}
				Data target = findExistingRecord(trow);
				if( target == null)
				{
					String idCell = trow.get("id");
					if( idCell == null)
					{
						idCell = trow.get("ID");
					}
					if(fieldStripPrefix){
						if(idCell.startsWith(getPrefix()))
						{
							idCell = idCell.substring(getPrefix().length(), idCell.length());
						}
					}
					
					PropertyDetail parent = getSearcher().getDetail("_parent");
					String parentid = null;
					if (parent != null)
					{
						parentid = trow.get("_parent");
						if (parentid != null)
						{
							target = findExistingData(idCell, parentid);
						}
					}
	
					if (target == null && idCell != null && idCell.trim().length() > 0)
					{
						target = findExistingData(idCell, null);
					}
	
					if (isAddNewData() && target == null)
					{
						target = getSearcher().createNewData();
						target.setId(idCell); //could be null
						if (parent != null)
						{
							target.setProperty("_parent", parentid);
						}
					}
					else if (isMakeId())
					{
						target = getSearcher().createNewData();
						idCell = getSearcher().nextId();
					}
					if (target == null)
					{
						continue;
					}
					target.setId(idCell); 
				}
				if (target == null)
				{
					continue;
				}
				fieldLastNonSkipData = target;
				addProperties(trow, target);
				data.add(target);
				if (data.size() == 3000)
				{
					getSearcher().saveAllData(data, context.getUser());
					log.info("imported 3000");
					data.clear();
				}
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
			getPageManager().removePage(getImportPage());
		}
		getSearcher().saveAllData(data, context.getUser());
		log.info("imported " + rowNum);
	}

	public boolean skipRow(Row inTrow)
	{
		//Add fields to this string or two?
		return false;
	}

	protected Data findExistingRecord(Row inRow)
	{
		return null;
	}

	/** Might be overriden by scripts */
	protected Data findExistingData(String inId, String inParent)
	{

		Searcher searcher = getSearcher();

		PropertyDetail parent = searcher.getDetail("_parent");
		Data newdata = null;
		if (parent == null)
		{
			newdata = (Data) getSearcher().searchById(inId);
		}
		else
		{

			newdata = searcher.query().match("id", inId).match("_parent", inParent).searchOne();

		}

		return newdata;
	}

	protected HashMap<String, Map> getLookUps()
	{
		if (fieldLookUps == null)
		{
			fieldLookUps = new HashMap<String, Map>();
			//fieldLookUps.put("Division", "val_Archive_Division");
		}
		return fieldLookUps;
	}

	
	protected void createMultiSelect(MultiValued inRow, String inField, String inTable)
	{
		inField = PathUtilities.extractId(inField, true);

		String value = inRow.get(inField);
		if (value != null)
		{
			HashMap datavalues = (HashMap) loadValueList(inField, inTable, true);
			Collection<String> values = (Collection<String>) EmStringUtils.split(value);
			ArrayList valueids = new ArrayList();
			for (String val : values)
			{
				String id = PathUtilities.extractId(val, true);
				Data data = (Data) datavalues.get(id);
				if (data == null)
				{
					data = findOrCreateById(inTable, id, val);
					datavalues.put(id, data);
				}
				valueids.add(id); //save it
			}
			inRow.setValue(inField, valueids);
		}

	}

	protected HashMap loadValueList(String inField, String inTableName, boolean inMulti)
	{
		HashMap datavalues = (HashMap) getLookUps().get(inField);
		//if (datavalues == null)
		if (datavalues == null || datavalues.isEmpty())
		{
			datavalues = new HashMap();
			getLookUps().put(inField, datavalues);
			String id = PathUtilities.extractId(inField, true);
			PropertyDetails details = getSearcher().getPropertyDetails();
			PropertyDetail detail = details.getDetail(id);
			
			if (detail.getDataType() == null ||  !detail.getDataType().equals("list"))
			{
				detail.setDataType("list");
				detail.setListId(inTableName);
				if (inMulti)
				{
					detail.setViewType("multiselect");
				}
				getSearcher().getPropertyDetailsArchive().savePropertyDetail(detail, getSearcher().getSearchType(), context.getUser());
			}

		}
		return datavalues;
	}

//	protected void createLookUp(Data inRow, String inField, String inTable)
//	{
//		inField = PathUtilities.extractId(inField, true);
//		String value = inRow.get(inField);
//		if (value != null)
//		{
//			int comma = value.indexOf(",");
//			if (comma > 0)
//			{
//				value = value.substring(0, comma);
//			}
//			Data data = findOrCreateData(inTable, inField, value);
//			inRow.setProperty(inField, data.get("id"));
//		}
//	}

	protected Data findOrCreateData(PropertyDetail inDetail, String value)
	{
		String inTable = inDetail.getListId();
		String inField = inDetail.getId();
		Map datavalues = loadValueList(inField, inTable, false);
		Data data = (Data) datavalues.get(value);
		if (data == null)
		{
			//search by name
			
			Searcher searcher = getSearcherManager().getSearcher(getSearcher().getCatalogId(), inTable);
			
			String[] filter = getLookUpFilters().get(inField);
			if( filter != null)
			{
				data = (Data) searcher.query().exact(filter[0], filter[1]).match("name", value).searchOne(); //How do we include collectionid? Preload lookups?
			}
			else
			{
				data = (Data) searcher.query().match("name", value).searchOne(); //How do we include collectionid? Preload lookups?				
			}
			if(data == null) 
			{
				data = (Data) searcher.searchById(value);
				if(data == null) 
				{
					//create it?
					if( getDbLookUps().contains(inDetail.getId()))
					{
						String id = PathUtilities.extractId(value, true);
						data = findOrCreateById(inTable, id, value);
					}
				}
			}
			if( data == null)
			{
				data = new BaseData();
				data.setId(value);
				data.setName(value);
			}
			datavalues.put(value, data);
		}
		return data;
	}

	protected Data findOrCreateById(String inTable, String id, String value)
	{
		Data data;
		Searcher searcher = getSearcherManager().getSearcher(getSearcher().getCatalogId(), inTable);
		data = (Data) searcher.searchById(id);
		if (data == null)
		{
			data = searcher.createNewData();
			data.setId(id);
			data.setName(value);
			searcher.saveData(data, null);
		}
		return data;
	}

//	protected void createMetadata(Header inHeader)
//	{
//		PropertyDetails details = getSearcher().getPropertyDetails();
//
//		for (Iterator iterator = inHeader.getHeaderNames().iterator(); iterator.hasNext();)
//		{
//			String header = (String) iterator.next();
//			if(header.contains(".")){  //TODO: What about english and spanish language imports?
//				continue;
//			}
//			//String header = inHeaders[i];
//			PropertyDetail detail = getDetailByColLabel(header);  //Creates em
//		}
//	}

	protected PropertyDetail getDetailByColLabel(String inHeader) {
		
		PropertyDetails details = getSearcher().getPropertyDetails();
		PropertyDetail detail = details.getDetailByName(inHeader);
		String id = PathUtilities.extractId(inHeader, true);
		if(detail == null){
			detail = details.getDetail(id);
		}			
		if(detail == null){
			id = getNewdDetailPrefix() + id;
			detail = details.getDetail(id);
		}			
		if (detail == null && !inHeader.contains("."))
		{				
			detail = getSearcher().getPropertyDetailsArchive().createDetail(id, inHeader);
			
			if( getDbLookUps().contains(id))
			{
				detail.setDataType("list");
			}
			getSearcher().getPropertyDetailsArchive().savePropertyDetail(detail, getSearcher().getSearchType(), context.getUser());
			getSearcher().putMappings();
		}
		
		return detail;
	}

	protected void addProperties(Row inRow, Data inData)
	{
		PropertyDetails details = getSearcher().getPropertyDetails();
		int size = inRow.getHeader().getSize()-1;

		for (int i = 0; i < inRow.getData().length; i++)
		{
			String val = inRow.getData(i);
			if(i > size){
				continue;
			}
			String header = inRow.getHeader().getColumn(i);
			if (header.contains("."))
			{
				//Assume Language map import
				String splits[] = header.split("\\.");
				if (splits.length > 1)
				{
					PropertyDetail detail = getDetailByColLabel(splits[0]);
					
					if (detail != null && detail.isMultiLanguage())
					{
						LanguageMap map = null;
						Object values = inData.getValue(detail.getId());
						if (values instanceof LanguageMap)
						{
							map = (LanguageMap) values;
						}
						if (values instanceof String)
						{
							map = new LanguageMap();
							map.put("en", values);
						}
						if (map == null)
						{
							map = new LanguageMap();
						}
						map.put(splits[1], val);
						inData.setValue(detail.getId(), map);
						continue;
					}
					else
					{
						header = splits[0]; //No . allowed
					}
				}
			}
			
			PropertyDetail detail = getDetailByColLabel(header);
			
			val = URLUtilities.escapeUtf8(val); //The XML parser will clean up the & and stuff when it saves it
//			if ("sourcepath".equals(headerid))
//			{
//				inData.setSourcePath(val);
//			}
			if (val != null && val.length() > 0)
			{
				val = val.trim(); 	//trim spaces and clean
				Object value = lookUpListValIfNeeded(detail,val);
				inData.setValue(detail.getId(), value);
			}
			else
			{
				inData.setValue(detail.getId(), null);  //This ok?
			}
		}
		addCustomProperties(inRow,inData);
	}

	protected void addCustomProperties(Row inRow, Data inData)
	{
		//Uerride as needed
	}
	protected Object lookUpListValIfNeeded(PropertyDetail inDetail, String inVal)
	{
		Object value = null;
		if( inDetail.isList() && (isLookUpLists() || getDbLookUps().contains(inDetail.getId())))
		{
			if(inDetail.isMultiValue())
			{
				Collection values = new ArrayList();
				String[] vals = MultiValued.VALUEDELMITER.split(inVal);
				for (int j = 0; j < vals.length; j++)
				{
					Object newvalue = findOrCreateData(inDetail, vals[j].trim());
					values.add(newvalue);			
				}
				value = values;
			}
			else
			{
				value = findOrCreateData(inDetail,inVal);
			}
		}
		else
		{
			value = getSearcher().createValue(inDetail.getId(), inVal);
		}
		Object finished = scrubValueIfNeeded(inDetail,value);
		return finished;
	}

	public Object scrubValueIfNeeded(PropertyDetail inDetail, Object inVal)
	{
		Object finalVal = inVal;
		//TO be overrriden by scripts
		return finalVal;
	}
	
	public boolean isMakeId()
	{
		return fieldMakeId;
	}

	public void setMakeId(boolean inVal)
	{
		fieldMakeId = inVal;
	}
}
