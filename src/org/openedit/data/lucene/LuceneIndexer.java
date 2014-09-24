package org.openedit.data.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;

public class LuceneIndexer
{
	protected List fieldStandardProperties = null;
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
			doc.add(new Field(inDetail.getId(), val, ID_FIELD_TYPE));
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
			if (iter.hasNext())
			{
				buffer.append(" | ");
			}
		}
		// Add in all the catalogs, price, gender, image on disk?, name+ full
		// text
		if (buffer.length() > 0)
		{
			doc.add(new Field(inType, buffer.toString(), INPUT_FIELD_TYPE ));
		}
		/*
		 * Not used any more if ( item.getDepartment() != null) { doc.add( new
		 * Field("department", item.getDepartment(), Field.Store.YES,
		 * Field.Index..ANALYZED_NO_NORMS)); }
		 */

	}

//	public void populateJoinDataCount(String inType, Document doc, Collection inDataElements)
//	{
//		int count = 0;
//		if (inDataElements != null)
//		{
//			count = inDataElements.size();
//		}
//		//must use int or long since double only works on double values
//		String sortable = getNumberUtils().long2sortableStr(count);
//		doc.add(new Field(inType + "_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
//		doc.add(new Field(inType, String.valueOf(count), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
//	}

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
		StringBuffer keywords = new StringBuffer();
		readStandardProperties(inDetails, inData, keywords, doc);

		List details = inDetails.getDetails();
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			readProperty(inData, doc, keywords, detail);
		}

		readDescription(doc, keywords);

	}

	protected void readDescription(Document doc, StringBuffer keywords)
	{
		String trimkeywords = keywords.toString().trim();
		if (trimkeywords.length() > 0)
		{
			//TextField field = new TextField("description", trimkeywords, Field.Store.NO);
			Field field = new Field("description", trimkeywords, ALL_SEARCH_TYPE );
			doc.add(field);

		}
	}

	protected List getStandardProperties()
	{
		if (fieldStandardProperties == null)
		{
			fieldStandardProperties = Arrays.asList("name", "id", "deliverablestatus", "description", "sourcepath");
		}
		return fieldStandardProperties;
	}

	protected void readStandardProperties(PropertyDetails inDetails, Data inData, StringBuffer keywords, Document doc)
	{
		PropertyDetail detail = inDetails.getDetail("id");
		docAdd(detail, doc, "id", inData.getId());
		String name = inData.getName();
		if (name == null || inData.getName().length() == 0)
		{
			name = inData.get("name");
		}
		if (name != null && name.length() > 0)
		{
			detail = inDetails.getDetail("name");
			docAdd(detail, doc, "name", name);
		}
		keywords.append(inData.getId());
		keywords.append(" ");
		keywords.append(inData.getName());

		if (inData.getSourcePath() != null)
		{
			detail = inDetails.getDetail("sourcepath");
			docAdd(detail, doc, "sourcepath", inData.getSourcePath());
		}
	}

//	public void docAdd(PropertyDetail inDetail, Document doc, String inId, String inValue, Store inStore, boolean isText)
//	{
//		docAdd(inDetail, doc, inId, inValue, null, inStore, isText);
//	}

	/**
	 * This method will replace the old way of indexing fields
	 * 
	 * @param inDetail
	 * @param doc
	 * @param inId
	 * @param inValue
	 */
	protected void docAdd(PropertyDetail inDetail, Document doc, String inId, String inValue)
	{
		if( inDetail == null)
		{
			return;
		}
		if (inDetail.isDataType("boolean"))
		{
			inValue = String.valueOf( Boolean.parseBoolean(inValue) );
		}
		if( inValue == null)
		{
			return;
		}
					
		Field field = null;//new Field(inId, inValue ,inStore, inIndex);
		
		boolean treatasnumber = isNumber(inDetail);
		if (inDetail.isDataType("date"))
		{
			Date target = DateStorageUtil.getStorageUtil().parseFromStorage(inValue);
			if (target != null)
			{
				inValue = DateTools.dateToString(target, Resolution.SECOND);
			}
		}
		else if(treatasnumber)
		{
			try
			{
				if (inDetail.isDataType("double"))
				{
					Double l = Double.parseDouble(inValue);
					field = new DoubleField(inId, l, Field.Store.YES);
				}
				else
				{
					String targetval = inValue.replace("$", "");
					targetval = targetval.replace(",", "");
					Long l = Long.parseLong(targetval);
					field = new LongField(inId, l, Field.Store.YES);
				}
			}
			catch (Exception e)
			{
				log.info("invalid number value skipped: " + inId);
				return;
			}
		}
		else if( inId.equals("id") || inDetail.isList() )
		{
			field = new Field(inId, inValue, ID_FIELD_TYPE );
		}
		else if( inId.contains("sourcepath") )
		{
			field = new Field(inId, inValue, INTERNAL_FIELD_TYPE );
		}
		if( field == null)
		{
			field = new Field(inId, inValue, INPUT_FIELD_TYPE ); //tokenized for searchability
		}
		doc.add(field);

		if (inDetail.isList() && inDetail.isSortable() )
		{
			String id = inDetail.getSortProperty();
			if (inId.equals(id) && !treatasnumber)
			{
				return;
			}
			else if ( inDetail.getListCatalogId() != null)
			{
				Data row = getSearcherManager().getData(inDetail.getListCatalogId(), inDetail.getListId(), inValue);
				if (inValue != null)
				{
					inValue = inValue.toString().toLowerCase();
				}
				doc.add(new Field(id, inValue, SORT_FIELD_TYPE ));
			}
		}
		
	}
/*
	protected void docAdd(PropertyDetail inDetail, Document doc, String inId, String inValue, Data text, Store inStore, boolean isText)
	{
		boolean treatasnumber = isNumber(inDetail);
		//TODO: Remove this method and use the new data type specific fields
		Field field = null;//new Field(inId, inValue ,inStore, inIndex);
		if (isText)
		{
			//field = new StringField(inId,inValue,inStore);  //This causes case sensitivity to be lost
			field = new Field(inId, inValue, inStore, Field.Index.ANALYZED_NO_NORMS);
		}
		else if (treatasnumber)
		{
			try
			{
				if (inDetail.isDataType("double"))
				{
					Double l = Double.parseDouble(inValue);
					field = new DoubleField(inId, l, Field.Store.YES);
				}
				else
				{
					String targetval = inValue.replace("$", "");
					targetval = targetval.replace(",", "");
					Long l = Long.parseLong(targetval);
					//					FieldType type = new FieldType();
					//					type.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
					//					//type.setIndexOptions(value);
					//					type.setIndexed(true);
					//					type.setStored(true);
					//					type.setNumericType(FieldType.NumericType.LONG);
					field = new LongField(inId, l, Field.Store.YES);
				}

			}
			catch (Exception e)
			{
				log.info("invalid number value skipped: " + inId);
				return;
			}
			//new LongField(inId, inValue ,inStore, Field.Index.ANALYZED_NO_NORMS );
		}
		else
		{
			//TODO: Replace with specific field types earlier on in the stack trace
			field = new Field(inId, inValue, inStore, Field.Index.NOT_ANALYZED_NO_NORMS);
		}
		//field.setOmitNorms(true);

		doc.add(field);

		if (inDetail != null && (inDetail.isSortable()))
		{
			String id = inDetail.getSortProperty();
			if (inId.equals(id) && !treatasnumber)
			{
				return;
			}
			//doc.add(new Field(detail.getId() + "_sorted", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			//			if( treatasnumber )
			//			{
			//				try
			//				{	
			//					inValue = getNumberUtils().long2sortableStr(inValue);
			//				}
			//				catch( Exception ex )
			//				{
			//					//ex
			//				}
			//			}
			else if (inDetail.isDataType("list") && inDetail.getCatalogId() != null)
			{
				//				Data row = getSearcherManager().getData(inDetail.getListCatalogId(), inDetail.getListId(), inValue);
				if (text != null)
				{
					inValue = text.toString();
				}
			}

			doc.add(new Field(id, inValue.toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
	}
*/
	protected boolean isNumber(PropertyDetail inDetail)
	{
		boolean treatasnumber = false;

		if (inDetail != null)
		{
			if (inDetail.isDataType("double") || inDetail.isDataType("number") || inDetail.isDataType("long"))
			{
				treatasnumber = true;
			}
		}
		return treatasnumber;
	}

	protected void readProperty(Data inData, Document doc, StringBuffer keywords, PropertyDetail detail)
	{
		String detid = detail.getId();

		if (getStandardProperties().contains(detid))
		{
			return;
		}
		String value = inData.get(detail.getId());
		Data text = null;
		if (value != null && detail.isKeyword())
		{
			if (detail.isList())
			{
				//Loop up the correct text for the search. Should combine this with the lookup for sorting
				text = getSearcherManager().getData(detail.getListCatalogId(), detail.getListId(), value);
				if (text != null)
				{
					keywords.append(" ");
					keywords.append(text.getName());
				}
			}
			else
			{
				keywords.append(" ");
				keywords.append(value);
			}
		}

		if (!detail.isIndex())
		{
			return;
		}

		if (populateJoin(inData, doc, detail))
		{
			return;
		}

		docAdd(detail, doc, detail.getId(), value);

		if (detail.isDataType("position"))
		{
			String lat = inData.get(detail + "_lat");
			String lng = inData.get(detail + "_lng");

			try
			{
				if (lat != null && lng != null)
				{

					Double l = Double.parseDouble(lat);
					Field field = new DoubleField(detid + "_lat", l, Field.Store.YES);

					doc.add(field);

					l = Double.parseDouble(lng);
					field = new DoubleField(detid + "_lng", l, Field.Store.YES);

					doc.add(field);
				}
				else
				{
					//doc.add(new Field(detail + "_available", "false", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}
			catch (Exception e)
			{
				log.info("no valid position data found");
			}
		}
	}

	protected boolean populateJoin(Data inData, Document doc, PropertyDetail detail)
	{
		String type = detail.getDataType();
		if (type != null && type.endsWith("join"))
		{
			if (type.equals("searchjoin"))
			{
				return true; //purely virtual
			}
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
//				else if (type.startsWith("total")) Use facets
//				{
//					populateJoinDataCount(detail.getId(), doc, tracker);
//				}
				else
				{
					//friend.ownerid = user.id
					String q = detail.getQuery();
					field = q.substring(q.indexOf(".") + 1, q.indexOf("="));
					field = field.trim();

					populateJoinData(detail, doc, tracker, field);
				}
			}
			return true;
		}
		return false;
	}

	public void updateFacets(PropertyDetails inDetails, Document inDoc, TaxonomyWriter inTaxonomyWriter)
	{
		if (inTaxonomyWriter == null)
		{
			return;
		}

		List facetlist = inDetails.getDetailsByProperty("filter", "true");
		ArrayList<CategoryPath> categorypaths = new ArrayList();
		for (Iterator iterator = facetlist.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String value = inDoc.get(detail.getId());

			if (detail.isFilter())
			{
				if (value != null)
				{
					//split the values and add a path for each one?
					String[] values = null;
					if (value.contains("|"))
					{
						values = MultiValued.VALUEDELMITER.split(value);
					}
					else
					{
						values = new String[] { value };
					}
					for (int i = 0; i < values.length; i++)
					{
						if (detail.getId().equals("category") && values[i].equals("index"))
						{
							continue;
						}
						String val = values[i];
						if (val != null && !val.trim().isEmpty())
						{
							String[] vals = new String[2];
							vals[0] = detail.getId().replace('/', '_');
							vals[1] = values[i].replace('/', '_');
							categorypaths.add(new CategoryPath(vals));
						}
					}
					//log.info("Adding: " + vals);

				}
			}

		}

		if (categorypaths.size() > 0)
		{
			FacetFields facetFields = new FacetFields(inTaxonomyWriter);
			try
			{
				facetFields.addFields(inDoc, categorypaths);
			}
			catch (IOException e)
			{
				throw new OpenEditException(e);
			}
		}
		// do stuff

	}

	/**
	 * Used for tokenized text fields
	 */
	
	//Input Fields - Search Name column should be case insensitive and should allow for search of John OR smith or Wild Cards.  (YES analized or tokeninzed stoare) 


//ID field what you put in is what you get out. Good for ID column all formating is off (not analized or tokeninzed) Always stored Option: sortable
	
	protected static final FieldType INPUT_FIELD_TYPE = getInputFieldType();

	static FieldType getInputFieldType()
	{
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
		type.setStored(true);
		type.setTokenized(true);
		type.setOmitNorms(false); //Makes it sortable?
		return type;
	}
	
	protected static final FieldType ID_FIELD_TYPE = getIdFieldType();

	static FieldType getIdFieldType()
	{
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
		type.setStored(true);
		type.setTokenized(false);
		type.setOmitNorms(true); 
		return type;
	}

	protected static final FieldType INTERNAL_FIELD_TYPE = getInternalFieldType();

	static FieldType getInternalFieldType()
	{
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
		type.setStored(false);
		type.setTokenized(true);
		type.setOmitNorms(true); //This saves memory, at the expense of scoring (results not ranked)
		return type;
	}

	protected static final FieldType SORT_FIELD_TYPE = getIdFieldType();

	static FieldType getSortFieldType()
	{
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
		type.setStored(false);
		type.setTokenized(false);
		type.setOmitNorms(false); 
		return type;
	}

	protected static final FieldType ALL_SEARCH_TYPE = getAllFieldType();

	static FieldType getAllFieldType()
	{
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS);  //TODO: Move this to freq check
		type.setStored(false);
		type.setTokenized(true);
		type.setOmitNorms(true); //Norms are faster for dates and numbers
		return type;
	}

}
