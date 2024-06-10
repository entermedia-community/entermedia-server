package org.entermediadb.asset.importer;

import java.io.Writer;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.util.CSVWriter;
import org.entermediadb.location.Position;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.util.DateStorageUtil;

public class BaseExporter
{
	private static final Log log = LogFactory.getLog(BaseExporter.class);

	public void exportHits(WebPageRequest inReq) throws Exception
	{
		String name = inReq.findActionValue("hitsname");
		HitTracker hits = (HitTracker) inReq.getPageValue(name);
		if(hits == null)
		{
			 String sessionid = inReq.getRequestParameter("hitssessionid");
			 hits = (HitTracker)inReq.getSessionValue(sessionid);
		}
		hits.enableBulkOperations();
		SearcherManager searcherManager = (SearcherManager)inReq.getPageValue("searcherManager");
		String searchtype = inReq.findValue("searchtype");
		String catalogid = inReq.findValue("catalogid");
		Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
		String isfriendly = inReq.getRequestParameter("friendly");	

		boolean friendly = true;
	
		if( isfriendly != null)
		{
			friendly = Boolean.parseBoolean(isfriendly);
		}
		PropertyDetails	details = searcher.getPropertyDetails();
	
		int count = 0;
		Writer output = inReq.getPageStreamer().getOutput().getWriter();
		HitTracker languages = searcherManager.getList(catalogid, "locale");
		int langcount = details.getMultilanguageFieldCount();
		langcount = langcount * (languages.size() );
		//StringWriter output  = new StringWriter();
		//CSVWriter writer  = new CSVWriter(output,CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		CSVWriter writer  = new CSVWriter(output);
	
		String[] headers = new String[details.size() + langcount];
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if(detail.isMultiLanguage())
			{
				for (Iterator iterator2 = languages.iterator(); iterator2.hasNext();)
				{
					Data lang = (Data) iterator2.next();
					String id = lang.getId();
					headers[count] = detail.getId() + "." + id;
					count ++;
				}
			}
			else
			{
				if(friendly)
				{
					headers[count] = detail.getText(inReq);
				} 
				else
				{
					headers[count] = detail.getId();
				}
				count++;
			}		
		}
	
		int rowcount = 0;
		writer.writeNext(headers);
			log.info("about to start: " + hits.size() );
	
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				rowcount++;
				MultiValued hit = null;
				try
				{
						hit =  (MultiValued)iterator.next();
						String[] nextrow = new String[details.size() + langcount];//make an extra spot for c
						int fieldcount = 0;
						for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
						{
							PropertyDetail detail = (PropertyDetail) detailiter.next();
							if(detail.getBoolean("skipexport")) 
							{
								fieldcount++;
								continue;
							}
//							if("geo_point".equals(detail.getDataType())){
//								
//								log.info("GEO:" +hit.getValue("geo_point"));
//								Position pos = (Position)hit.getValue(detail.getId());
//								if(pos!= null) 
//								{
//									value = "${pos.getLatitude()},${pos.getLongitude()}";
//									nextrow[fieldcount] = value;
//									fieldcount ++;
//									continue;
//								}
//							}
							String value = null;
							//do special logic here
							if(detail.isList() && friendly)
							{
								//detail.get
								value = hit.get(detail.getId());
								Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), value);
								if(remote != null)
								{
									value= remote.getName();
								}
							}
							String render = detail.get("render");
							if(render != null)
							{
								value = searcherManager.getValue(detail.getListCatalogId(), render, hit.getProperties());
							}
							if(detail.isMultiLanguage())
							{
								Object vals = hit.getValue(detail.getId());
								if(vals != null && vals instanceof LanguageMap)
								{
									for (Iterator iterator2 = languages.iterator(); iterator2.hasNext();)
									{
										Data lang = (Data) iterator2.next();
										String label = (String)((LanguageMap)vals).getText(lang.getId());
										nextrow[fieldcount] = label;
										fieldcount ++;
									}
								}
								else
								{
									nextrow[fieldcount] = hit.get(detail.getId());
									fieldcount = fieldcount + languages.size();
								}	
							}
							else
							{
								if( value == null)
								{
									value = hit.get(detail.getId());
								}
								nextrow[fieldcount] = value;
								fieldcount++;
							}
						}	
						writer.writeNext(nextrow);
				}
				catch( Throwable ex)
				{
					log.error("Could not process row " + rowcount, ex );
					writer.flush();
					output.write("Could not process row " + rowcount + " " + ex );
					//output.write("Could not process path: " + hit.getSourcePath() + " id:" + hit.getId() );
					writer.flush();
				}
			}
	
		writer.close();
	
		//String finalout = output.toString();
		//inReq.putPageValue("export", finalout);
		inReq.setHasRedirected(true);

	}

}
