package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.highlight.HighlightField;
import org.entermediadb.data.FullTextLoader;
import org.entermediadb.location.Position;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SaveableData;
import org.openedit.data.SearchData;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.util.DateStorageUtil;

public class SearchHitData extends BaseData implements Data, MultiValued, SaveableData,SearchData 
{
	private static final Log log = LogFactory.getLog(SearchHitData.class);

	protected Searcher fieldSearcher;

//	public SearchHitData()
//	{
//	}
	public SearchHitData(SearchHit inHit, Searcher inSearcher) 
	{
		setSearcher(inSearcher);
		setSearchHit(inHit);
	}

	public SearchHitData(Searcher inSearcher)
	{
		setSearcher(inSearcher);
	}
	public Searcher getSearcher()
	{
		return fieldSearcher;
	}

	public void setSearcher(Searcher inSearcher)
	{
		if( inSearcher == null)
		{
			throw new OpenEditException("Searcher cannot be null");
		}
		fieldSearcher = (Searcher)inSearcher;
	}

	public SearchHit getSearchHit() {
		return getProperties().getSearchHit();
	}

	public void setSearchHit(SearchHit inSearchHit) 
	{
		if( inSearchHit == null)
		{
			throw new OpenEditException("inSearchHit cannot be null");
		}
		getProperties().setSearchHit( inSearchHit );
		setId(inSearchHit.getId());
		setVersion(inSearchHit.getVersion());

	}
	@Override
	public void setSearchData(Map inSearchHit)
	{
		getProperties().setSearchData(inSearchHit);
	}
	
	public Long getVersion() 
	{
		Long l = getProperties().getLong(".version");
		return l;
	}

	public void setVersion(long inVersion) 
	{
		setValue(".version", inVersion);
	}
	
	@Override
	public Object getValue(String inId) {
		if (inId == null) {
			return null;
		}

		if ("fulltext".equals(inId))
		{
			if (getSearcher() != null)
			{
				if( getSearchHit() == null)
				{
					log.info("Missing search hit");
					return ((FullTextLoader)getSearcher()).getFulltext(this);
				}
				else
				{
					return ((FullTextLoader)getSearcher()).getFulltext(this,getSearchHit().getType());
				}
			}
			else
			{
				log.info("Missing searcher");
			}
		}

		Object svalue = super.getValue(inId);
		
		return svalue;
	}


//
	public ValuesMapWithSearchData getProperties() 
	{
		if (fieldProperties == null)
		{
			fieldProperties = new ValuesMapWithSearchData();
			getProperties().setPropertyDetails(getSearcher().getPropertyDetails());

		}
		return (ValuesMapWithSearchData)fieldProperties;
	}
//		if (fieldProperties == null)
//		{
//			fieldProperties = new ValuesMap();
//			Set set = keySet();
//			for (Iterator iterator = set.iterator(); iterator.hasNext();) 
//			{
//				String key = (String) iterator.next();
//				Object val = getValue(key);
//				if( val != null)
//				{
//					fieldProperties.put(key, val);
//				}
//			}
//		}
//		return fieldProperties;
//	}

	public String toString() 
	{
		String val = getName();
		if (val != null)
		{
			return val;
		} else {
			return getId();
		}
	}
	
	public String toJsonString()
	{
		StringBuffer output = new StringBuffer();
		output.append("{ \"_id\": \"" + getId() + "\",");
		output.append(" \"_source\" :");
		output.append(getSearchHit().getSourceAsString());
		output.append(" \n}");
		return output.toString();
	}
	
	
	public List getHighlights(String inField) {
		ArrayList highlights = new ArrayList();

		if(getSearchHit() == null) {
			return highlights;
		}
		Map<String, HighlightField> highlightFields = getSearchHit().getHighlightFields();
		HighlightField field = highlightFields.get(inField);
		if(field == null) {
			return highlights;
		}
		Text[] fragments = field.getFragments();
		for (Text text : fragments)
		{
			String frag = text.string();
			highlights.add(frag);
		}
		return highlights;
	}
	
	public Map getEmRecordStatus()
	{
		return (Map)getProperties().getFromDb("emrecordstatus");
	}
	@Override
	public Map getSearchData()
	{
		return getProperties().getSearchData();
	}
	
}
