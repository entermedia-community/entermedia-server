package org.openedit.data.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.CorruptIndexException;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class LuceneIndexer
{
	private static final Log log = LogFactory.getLog(LuceneIndexer.class);
	protected NumberUtils fieldNumberUtils;
	protected SearcherManager fieldSearcherManager;
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}


	public NumberUtils getNumberUtils()
	{
		return fieldNumberUtils;
	}

	public void setNumberUtils(NumberUtils inNumberUtils)
	{
		fieldNumberUtils = inNumberUtils;
	}

	public void populateDateJoin(PropertyDetail inDetail, Document doc, Collection allParentCategories, String inField, boolean inIsStored)
	{
		Date tosave = null;
		boolean savebottom = "bottom".equals(inDetail.get("rangeposition"));

		for (Iterator iter = allParentCategories.iterator(); iter.hasNext();)
		{
			Object catalog = (Data) iter.next();

			try
			{
				String foo = null;
				if (catalog instanceof Data)
				{
					Data local = (Data) catalog;
					foo = local.get(inField);
				}
				else
				{
					Document local = (Document) catalog;
					foo = local.get(inField);
				}
				Date d1 = DateStorageUtil.getStorageUtil().parseFromStorage(foo);
				if (tosave == null)
				{
					tosave = d1;
					continue;
				}
				if (savebottom)
				{
					if (d1 != null && d1.before(tosave))
					{
						tosave = d1;
					}
				}
				else
				{
					if (d1 != null && d1.after(tosave))
					{
						tosave = d1;
					}
				}
			}
			catch (Exception ex)
			{
				log.error(ex);
			}
		}
		if (tosave != null)
		{
			String val = DateTools.dateToString(tosave, Resolution.SECOND);
			if (inIsStored)
			{
				doc.add(new Field(inDetail.getId(), val, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
			else
			{
				doc.add(new Field(inDetail.getId(), val, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		}
	}

	public void populateJoinData(PropertyDetail inDetail, Document doc, Collection inDataElements, String inField)
	{
		populateJoinData(inDetail.getId(), doc, inDataElements, inField, inDetail.isStored());
	}

	public void populateJoinData(String inType, Document doc, Collection inDataElements, String inField, boolean inIsStored)
	{
		StringBuffer buffer = new StringBuffer();
		for (Iterator iter = inDataElements.iterator(); iter.hasNext();)
		{
			Object next = iter.next();
			String append = null;
			if (next instanceof Data)
			{
				append = ((Data) next).get(inField);
			}
			else if (next instanceof Document)
			{
				append = ((Document) next).get(inField);
			}
			buffer.append(append);
			buffer.append(" ");
		}
		// Add in all the catalogs, price, gender, image on disk?, name+ full
		// text
		if (buffer.length() > 0)
		{
			if (inIsStored)
			{
				doc.add(new Field(inType, buffer.toString(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
			}
			else
			{
				doc.add(new Field(inType, buffer.toString(), Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
			}
		}
		/*
		 * Not used any more if ( item.getDepartment() != null) { doc.add( new
		 * Field("department", item.getDepartment(), Field.Store.YES,
		 * Field.Index..ANALYZED_NO_NORMS)); }
		 */

	}

	public void populateJoinDataCount(String inType, Document doc, Collection inDataElements)
	{
		int count = 0;
		if (inDataElements != null)
		{
			count = inDataElements.size();
		}
		//must use int or long since double only works on double values
		String sortable = getNumberUtils().long2sortableStr(count);
		doc.add(new Field(inType + "_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		doc.add(new Field(inType, String.valueOf(count), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
	}

	/**
	 * This is the main API
	 * 
	 * @param inData
	 * @param doc
	 * @param inDetails
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public void updateIndex(Data inData, Document doc, PropertyDetails inDetails)
	{
		doc.add(new Field("id", inData.getId(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
		String name = inData.getName();
		if (name == null || inData.getName().length() == 0)
		{
			name = inData.get("name");
		}
		if (name != null && name.length() > 0)
		{
			doc.add(new Field("name", name, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
			doc.add(new Field("namesorted", name, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		}

		StringBuffer keywords = new StringBuffer();
		keywords.append(inData.getId());
		keywords.append(" ");
		keywords.append(inData.getName());
		List details = inDetails.findIndexProperties();
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if (detail.getId().equals("id") || detail.getId().equals("name") || detail.getId().equals("deliverablestatus"))
			{
				continue;
			}
			updateProperty(inData, doc, keywords, detail);
		}
		doc.add(new Field("description", keywords.toString(), Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
		if(inData.getSourcePath() != null){
			doc.add(new Field("sourcepath",inData.getSourcePath(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
		}
	}

	public void updateProperty(Data inData, Document doc, StringBuffer keywords, PropertyDetail detail)
	{
		if (detail.getDataType() != null && detail.getDataType().endsWith("join"))
		{
			// get the values from another list
			String id = detail.getExternalId();
			if (id == null)
			{
				id = detail.getId();
			}
			String table = id.substring(0, id.indexOf('.'));
			String field = id.substring(id.indexOf('.') + 1);
			Searcher searcher = getSearcherManager().getSearcher(detail.getCatalogId(), table); //external list
			HitTracker tracker = searcher.fieldSearch(field, inData.getId());
			if (tracker != null)
			{
				if (detail.isDataType("datejoin"))
				{
					populateDateJoin(detail, doc, tracker, field, true);
				}
				else if (detail.getDataType().startsWith("total"))
				{
					populateJoinDataCount(detail.getId(), doc, tracker);
				}
				else
				{
					//friend.ownerid = user.id
					String q = detail.getQuery();
					field = q.substring(q.indexOf(".") + 1,q.indexOf("="));
					field = field.trim();
					
					populateJoinData(detail, doc, tracker, field);
				}
			}
		}

		String value = inData.get(detail.getId());
		updateIndex(inData, value, doc, keywords, detail);
	}

	public void updateIndex(Data inData, String value, Document doc, StringBuffer keywords, PropertyDetail detail)
	{
		if (value != null)
		{

			if (detail.isDataType("number") || detail.isDataType("long"))
			{
				try
				{
					//This is more standard way to save stuff
					doc.add(new Field(detail.getId(), value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					String sortable = getNumberUtils().long2sortableStr(value);
					doc.add(new Field(detail.getId() + "sorted", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
				catch (Exception e)
				{
					log.info("bad number: " + value);
				}
				finally
				{
					//	doc.add(new Field(detail.getId(), value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			} //doubles dont seem to work detail.isDataType("double")
			else if (detail.isDataType("boolean"))
			{
				if (Boolean.parseBoolean(value))
				{
					doc.add(new Field(detail.getId(), "true", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
				else
				{
					doc.add(new Field(detail.getId(), "false", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}

			}
			else if (detail.isDataType("date"))
			{
				Date target = DateStorageUtil.getStorageUtil().parseFromStorage(value);
				if (target != null)
				{
					String sortable = DateTools.dateToString(target, Resolution.SECOND);
					//log.info(inData.getId() +  " Saved " + sortable);
					doc.add(new Field(detail.getId(), sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}
			else if (detail.isStored())
			{
				doc.add(new Field(detail.getId(), value, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
			}
			else
			{
				doc.add(new Field(detail.getId(), value, Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
			}
			if (detail.isKeyword() && keywords != null)
			{
				keywords.append(" ");
				keywords.append(value);
			}
		}
		else
		//value was null
		{
			if (detail.isDataType("boolean"))
			{
				doc.add(new Field(detail.getId(), "false", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
			
			if(detail.isDataType("position")){
				String lat = inData.get(detail + "_lat");
				String lng = inData.get(detail + "_lng");
				
				try
				{
					if(lat != null && lng != null){
					String sortable = getNumberUtils().double2sortableStr(lat);
					doc.add(new Field(detail + "_lat_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					doc.add(new Field(detail + "_lat", lat, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					
					
					
					sortable = getNumberUtils().double2sortableStr(lng);
					doc.add(new Field(detail + "_lng_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					doc.add(new Field(detail + "_lng", lng, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					doc.add(new Field(detail + "_available", "true", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
					else{
						doc.add(new Field(detail + "_available", "false", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
					
					
					
				}
				catch (Exception e){
					log.info("no valid position data found");
				}
				finally
				{
				
					//	inDoc.add(new Field(detid, prop, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}
		}
	}
}
