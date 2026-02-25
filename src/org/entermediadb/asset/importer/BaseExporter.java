package org.entermediadb.asset.importer;

import java.io.Writer;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.util.CSVWriter;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.data.ViewFieldList;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;

public class BaseExporter
{
	private static final Log log = LogFactory.getLog(BaseExporter.class);

	public void exportHits(WebPageRequest inReq) throws Exception
	{
		String name = inReq.findActionValue("hitsname");
		HitTracker hits = (HitTracker) inReq.getPageValue(name);
		if(hits == null)
		{
			//log.error("No such hits: " + name);
			String sessionid = inReq.getRequestParameter("hitssessionid");
			hits = (HitTracker)inReq.getSessionValue(sessionid);
			if(hits == null)
			{
				log.error("Export failed, no such hits nor sessions: " + sessionid);
				return;
			}
			// String moduleid = inReq.findPathValue("module");
			// hits = loadHitTracker(inReq, moduleid);
			// if(hits == null)
			// {
			// 	log.error("No hittracker found");
			// 	return;
			// }
			String exportselection = inReq.findValue("exportselection");
			if ("true".equals(exportselection))
			{
				hits = hits.getSelectedHitracker();
			}
			
		}
		hits.enableBulkOperations();
		SearcherManager searcherManager = (SearcherManager)inReq.getPageValue("searcherManager");
		String searchtype = inReq.findValue("searchtype");
		String catalogid = inReq.findValue("catalogid");
		Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
		PropertyDetails	details = new PropertyDetails();
		String isfriendly = inReq.getRequestParameter("friendly");	
		boolean friendly = Boolean.parseBoolean(isfriendly);
	
		if( friendly )
		{
			String view = inReq.findValue("view");
			if (view != null)
			{
				//details = searcher.getPropertyDetails();
				ViewFieldList viewlist = searcher.getDetailsForView(searchtype+"resultstable", inReq.getUserProfile());
				if (viewlist == null)
				{
					searcher.getDetailsForView("resultstable",inReq.getUserProfile());
				}
				if (!viewlist.isEmpty())
				{
					for (Iterator iterator = viewlist.iterator(); iterator.hasNext();)
					{
						PropertyDetail viewdetail = (PropertyDetail) iterator.next();
						details.add(viewdetail);
						
					}
				}
			}
		}
		else 
		{
			//if not friendly export all fields
			details = searcher.getPropertyDetails();
		}
		
		if (details == null)
		{
			log.error("No details to export");
			return;
		}
	
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
			log.info("Exporting: " + hits.size()  + " records from: " + searchtype);
	
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
								Object value = null;
								//do special logic here
								if(detail.isList() && friendly)
								{
									//detail.get
									String val = hit.get(detail.getId());
									Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), val);
									if(remote != null)
									{
										value= remote.getName();
									}
								}
								if(value == null && detail.get("rendermask") != null)
								{
									value = searcherManager.getValue(hit, detail, inReq.getLocale());
								}
								else 
								{
									value = hit.get(detail.getId());
								}
								if(value == null)
								{
									value = "";
								}
								nextrow[fieldcount] = value.toString();
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
		log.info("Export successfully");
		//String finalout = output.toString();
		//inReq.putPageValue("export", finalout);
		inReq.setHasRedirected(true);

	}

}
