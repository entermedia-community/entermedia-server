import org.entermediadb.asset.util.CSVWriter
import org.openedit.Data
import org.openedit.data.*
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil
	

HitTracker hits = (HitTracker) context.getPageValue("hits");
if(hits == null){
 String sessionid = context.getRequestParameter("hitssessionid");
 hits = context.getSessionValue(sessionid);
}
hits.enableBulkOperations();
searcherManager = context.getPageValue("searcherManager");
searchtype = context.findValue("searchtype");
catalogid = context.findValue("catalogid");
searcher = searcherManager.getSearcher(catalogid, searchtype);
boolean friendly = Boolean.parseBoolean(context.getRequestParameter("friendly"));


PropertyDetails	details = searcher.getPropertyDetails();

int count = 0;
Writer output = context.getPageStreamer().getOutput().getWriter();
HitTracker languages = searcherManager.getList(catalogid, "locale");
int langcount = details.getMultilanguageFieldCount();
langcount = langcount * (languages.size() );
//StringWriter output  = new StringWriter();
//CSVWriter writer  = new CSVWriter(output,CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
CSVWriter writer  = new CSVWriter(output);

headers = new String[details.size() + langcount];
for (Iterator iterator = details.iterator(); iterator.hasNext();)
{
	PropertyDetail detail = (PropertyDetail) iterator.next();
	if(friendly){
	headers[count] = detail.getText();
	} else{
	if(detail.isMultiLanguage()){
						languages.each{
							String id = it.id ;
							headers[count] = detail.getId() + "." + id;
							count ++;
						}
					}
					else{
						headers[count] = detail.getId();
						count++;
					}
	}		
	
}

int rowcount = 0;
writer.writeNext(headers);
	log.info("about to start: " + hits.size() );

	for (Iterator iterator = hits.iterator(); iterator.hasNext();)
	{
		rowcount++;
		Data hit = null;
		try
		{
				hit =  iterator.next();
				nextrow = new String[details.size() + langcount];//make an extra spot for c
				int fieldcount = 0;
				for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) detailiter.next();
					String value = hit.get(detail.getId());
					//do special logic here
					if(value != null && detail.isList() && friendly)
					{
						//detail.get
						Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), value);
						if(remote != null)
						{
							value= remote.getName();
						}
					}
					else if(value != null && detail.isDate() )
					{
						value = DateStorageUtil.getStorageUtil().checkFormat(value);
					}
					String render = detail.get("render");
					if(render != null)
					{
						value = searcherManager.getValue(detail.getListCatalogId(), render, hit.getProperties());
					}
					
					
					if(detail.isMultiLanguage()){
						Object vals = hit.getValue(detail.getId())
						if(vals != null && vals instanceof Map)
						{
							languages.each
							{
								String lang = it.id ;
								String label = vals.getText(lang);
								if( label == null && detail.getId().equals("name") )
								{
									label = hit.getName(lang);
								}
								nextrow[fieldcount] = label;
								fieldcount ++;
							}
						}
						else
						{
							nextrow[fieldcount] = vals;
							fieldcount = fieldcount + languages.size();
						}	

					} else{

						 value = hit.get(detail.getId());
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
//context.putPageValue("export", finalout);
context.setHasRedirected(true);


