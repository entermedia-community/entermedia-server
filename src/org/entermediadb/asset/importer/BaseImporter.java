package org.entermediadb.asset.importer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.util.CSVReader;
import org.entermediadb.asset.util.Header;
import org.entermediadb.asset.util.ImportFile;
import org.entermediadb.asset.util.Row;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
import org.openedit.MultiValued;
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
	protected HashMap<String, Map> fieldLookUps;
	protected Searcher fieldSearcher;
	protected boolean fieldMakeId;

	public Searcher getSearcher()
	{
		return fieldSearcher;
	}

	public void importData() throws Exception
	{
		fieldSearcher = loadSearcher(context);

		String importpath = context.findValue("importpath");
		Page upload = getPageManager().getPage(importpath);
		Reader reader = upload.getReader();
		ArrayList data = new ArrayList();

		try
		{
			ImportFile file = new ImportFile();
			file.setParser(new CSVReader(reader, ',', '\"'));
			file.read(reader);

			createMetadata(file.getHeader());

			Row trow = null;
			int rowNum = 0;
			while ((trow = file.getNextRow()) != null)
			{
				rowNum++;

				Data target = null;
				String idCell = trow.get("id");
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

				if (target == null)
				{
					target = getSearcher().createNewData();
					target.setId(idCell);
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
				else
				{
					//target = findExistingRecord(trow);
					if (target == null)
					{
						continue;
					}
				}

				addProperties(fieldSearcher, trow, target);
				target.setId(idCell);
				data.add(target);
				if (data.size() == 10000)
				{
					getSearcher().saveAllData(data, context.getUser());
					log.info("imported 1000");
					data.clear();
				}
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
			getPageManager().removePage(upload);
		}
		getSearcher().saveAllData(data, context.getUser());
		log.info("imported " + data.size());
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
			HashMap<String, Map> fieldLookUps = new HashMap<String, Map>();
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
					//create it
					Searcher searcher = getSearcherManager().getSearcher(getSearcher().getCatalogId(), inTable);
					data = (Data) searcher.searchById(id);
					if (data == null)
					{
						data = searcher.createNewData();
						data.setId(id);
						data.setName(val);
						searcher.saveData(data, null);
					}
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
		if (datavalues == null)
		{
			datavalues = new HashMap();
			getLookUps().put(inField, datavalues);
			String id = PathUtilities.extractId(inField, true);
			PropertyDetails details = getSearcher().getPropertyDetails();
			PropertyDetail detail = details.getDetail(id);
			//if( detail.getL
			if (detail.getDataType() != "list")
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

	protected void createLookUp(Data inRow, String inField, String inTable)
	{
		inField = PathUtilities.extractId(inField, true);
		String value = inRow.get(inField);
		if (value != null)
		{
			int comma = value.indexOf(",");
			if (comma > 0)
			{
				value = value.substring(0, comma);
			}
			Map datavalues = loadValueList(inField, inTable, false);
			Data data = (Data) datavalues.get(value);
			if (data == null)
			{
				//create it
				String id = PathUtilities.extractId(value, true);
				Searcher searcher = getSearcherManager().getSearcher(getSearcher().getCatalogId(), inTable);
				data = (Data) searcher.searchById(id);
				if (data == null)
				{
					data = searcher.createNewData();
					data.setId(id);
					data.setName(value);
					searcher.saveData(data, null);
				}
				datavalues.put(value, data);
			}
			inRow.setProperty(inField, data.get("id"));
		}
	}

	protected void createMetadata(Header inHeader)
	{
		PropertyDetails details = getSearcher().getPropertyDetails();

		for (Iterator iterator = inHeader.getHeaderNames().iterator(); iterator.hasNext();)
		{
			String header = (String) iterator.next();
			//String header = inHeaders[i];
			String id = PathUtilities.extractId(header, true);
			PropertyDetail detail = details.getDetail(id);

			if (detail == null)
			{
				detail = new PropertyDetail();
				detail.setId(id);
				detail.getName(header);
				detail.setEditable(true);
				detail.setIndex(true);
				detail.setStored(true);
				detail.setCatalogId(getSearcher().getCatalogId());
				details.addDetail(detail);
				
				getSearcher().getPropertyDetailsArchive().savePropertyDetail(detail, getSearcher().getSearchType(), context.getUser());
			}
		}
	}

	protected void addProperties(Searcher searcher, Row inRow, Data inData)
	{
		for (int i = 0; i < inRow.getData().length; i++)
		{
			String val = inRow.getData(i);
			String header = inRow.getHeader().getColumn(i);
			String headerid = PathUtilities.extractId(header, true);
			if (header.contains("."))
			{
				String splits[] = header.split("\\.");
				if (splits.length > 1)
				{
					PropertyDetail detail = searcher.getDetail(splits[0]);
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
						inData.setValue(splits[0], map);
					}
				}
				
			}
			val = URLUtilities.escapeUtf8(val); //The XML parser will clean up the & and stuff when it saves it
			if ("sourcepath".equals(header))
			{
				inData.setSourcePath(val);
			}
			else if (val != null && val.length() > 0)
			{
				inData.setProperty(headerid, val);
			}
		}
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
