package org.openedit.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.XmlArchive;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;
import com.openedit.util.PathProcessor;
import com.openedit.util.XmlUtil;

public class LuceneLogSearcher extends BaseLuceneSearcher implements WebEventListener {

	private static final Log log = LogFactory.getLog(LuceneLogSearcher.class);

	protected XmlArchive fieldLogFiles;
	protected int fieldMaxLength = 500000;
	protected PageManager fieldPageManager;
	protected String fieldFolderName;
	protected XmlUtil fieldXmlUtil;
	protected DateFormat fieldLogDateFormat;
	protected File fieldRootLogsDirectory;
	protected DateFormat fieldFileDateFormat;

	public LuceneLogSearcher()
	{
		setFireEvents(false);
	}
	public HitTracker recentSearch(WebPageRequest inReq) throws OpenEditException 
	{
		//SearchQuery search = addStandardSearchTerms(inReq);
		SearchQuery search = createSearchQuery();
		search.setResultType("log");
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(GregorianCalendar.MONTH, -1 );
		search.addAfter("date", cal.getTime());
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		search.putInput("date", format.format(cal.getTime()));
		if (search == null) {
			return null; // Noop
		}
		return search(inReq, search);
	}
	
	public void reIndexAll(final IndexWriter writer) 
	{
		try 
		{
			writer.setMergeFactor(100);
			writer.setMaxBufferedDocs(2000);

			PathProcessor process = new PathProcessor()
			{
				public void processFile(ContentItem inItem, User inUser) {
					//Loop over all events in file
					if (inItem.exists())
					{
						if( inItem.getPath().endsWith(".xml") )
						{
							InputStream is = inItem.getInputStream();
							Element root = getXmlUtil().getXml(is,"UTF-8");
							writeChangeFromElement(root, writer);
						}
					}
				}
			};
			process.setPageManager(getPageManager());
			process.setRecursive(false);
			process.setRootPath("/WEB-INF/logs/"+getFolderName()+"/");
			process.process();
			writer.optimize();
			flushRecentChanges(true);
		}
		catch (Exception ex) 
		{
			throw new OpenEditException(ex);
		}

	}
	private void  writeChangeFromElement(Element doc, IndexWriter inWriter) 
	{
		//newest ones come first
		for (Iterator iterator = doc.elementIterator("event"); iterator.hasNext();) 
		{
			Element	webEvent = (Element) iterator.next();
			updateIndex(  createChange(webEvent), inWriter );
		}
	}

	
	protected WebEvent createChange(Element inElement) 
	{
		WebEvent webEvent = new WebEvent();
		for (Iterator iter = inElement.attributeIterator(); iter.hasNext();)
		{
			Attribute attrib = (Attribute) iter.next();
			String name = attrib.getName();
			if( name.equals("date"))
			{
				String date = inElement.attributeValue("date");
				if( date !=null)
				{
					webEvent.setDate(DateStorageUtil.getStorageUtil().parseFromStorage(date));
				}				
			}
			else if( name.equals("user"))
			{
				webEvent.setUsername(attrib.getValue());
			}
			else if( name.equals("operation"))
			{
				webEvent.setOperation(attrib.getValue());
			}
			else
			{
				webEvent.addDetail(attrib.getName(), attrib.getValue());
			}
		}
		return webEvent;
	}

	public void updateIndex(WebEvent inChange, IndexWriter inWriter) 
	{
		// TODO Auto-generated method stub
		Document doc = new Document();
		populateEntry(doc, inChange, false);
		try 
		{
			inWriter.addDocument(doc, getAnalyzer());
		} catch (Exception e) 
		{
			throw new OpenEditException(e);
		}
	}
	protected void populateEntry(Document inDoc, WebEvent inChange, boolean add) throws OpenEditException {

		//Field id = new Field("id", inChange.getId(), Field.Store.YES, Field.Index.TOKENIZED);
		//inDoc.add(id); // Why is this tokenized? Guess so we can find lower

		Date date = inChange.getDate();
		String prop = DateTools.dateToString(date, Resolution.SECOND);
		inDoc.add(new Field("date", prop, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		
		populateProperties(inDoc, inChange);

	}

	private void populateProperties(Document doc, WebEvent inChange) {
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
		if( details == null)
		{
			throw new OpenEditException("No Details found for " + getCatalogId() + "/configuration/" + getSearchType() + "properties.xml");
		}
		for (Iterator iterator = details.getDetails().iterator(); iterator.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if (detail.isIndex()) {
				if( detail.getId().equals("user") )
				{
					if(inChange.getUsername() != null)
					{
						doc.add(new Field("user", inChange.getUsername(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
				}
				else if( detail.getId().equals("operation") )
				{
					if ( inChange.getOperation() != null)
					{
						doc.add(new Field("operation", inChange.getOperation() ,Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
				}
				else 
				{
					String val = inChange.get(detail.getId());
					if ( val != null)
					{
						if (detail.isStored()) 
						{
							doc.add(new Field(detail.getId(), val, Field.Store.YES, Field.Index.ANALYZED));
						} 
						else 
						{
	
							doc.add(new Field(detail.getId(), val, Field.Store.NO, Field.Index.ANALYZED));
						}
					}
				}
			}
		}
	}
	public HitTracker getAllHits(WebPageRequest inReq)
	{
		//hmmm no way to quickly do this?
		SearchQuery q = createSearchQuery();
		q.addAfter("date",new Date(0));
		if( inReq == null)
		{
			return search(q);
		}
		else
		{
			return cachedSearch(inReq,q);
		}
	}

	public String getFolderName()
	{
		return getCatalogId() + "/" + getSearchType();
	}

	public void setMaxLength(int i)
	{
		fieldMaxLength = i;
	}

	public int getMaxLength()
	{
		return fieldMaxLength;
	}
	
	
	/**
	 * Save the list of changes to the days webEvent log file
	 *
	 */
	public synchronized void save(WebEvent inChange)
	{
		try
		{
			File recent = new File( getRootDirectory(),getFolderName() + "/recent.log");
			FileWriter out = null;
			if( !recent.exists() || recent.length() == 0 )
			{
		      recent.getParentFile().mkdirs();
				out = new FileWriter(recent);
			}
			else
			{
				out = new FileWriter(recent,true);
			}
			try
			{
				Element element = DocumentHelper.createElement("event");
				populateElement(inChange, element);
				element.addAttribute("indexed", "false");
				out.write(element.asXML());
				out.write("\n");
				//TODO: For top performance we can skip this. And update
				//the index as needed within the getLiveSearcher()
	//			updateIndex(inChange, getIndexWriter());
				clearIndex();
			}
			finally
			{
				FileUtils.safeClose(out);
			}
			
			if (recent.length() >= getMaxLength())
			{
				moveRecent(recent);
			}
		}
		catch (Exception ex)
		{
			log.error(ex);
			ex.printStackTrace();
			return;
		}
	}

	private synchronized void moveRecent(File recent) throws Exception
	{
		flushRecentChanges();
		//setTimeStampCounter(0);
		//add these to the index
		//no guarantee there is a searcher initially.indexed
		//reIndexAll();
		
		String stampedFileName = getFileDateFormat().format(new Date()) + ".xml";
		File archive = new File( getRootLogsDirectory(),stampedFileName);
		RandomAccessFile archiveout = null;
		FileInputStream finalreader = null;
		try
		{
			if( !archive.exists() || archive.length() == 0 )
			{
				//create file and headers
				archiveout = new RandomAccessFile(archive, "rw");
				archiveout.write("<log>\n".getBytes());
			}
			else
			{
				archiveout = new RandomAccessFile(archive,"rws");
				archiveout.seek(archive.length() - "</log>".length()); //Back up the /log
			}
			finalreader = new FileInputStream(recent);
			byte[] bytes = new byte[1024];
			int iRead = -1;
			while (true)
			{
				iRead = finalreader.read(bytes);
				if (iRead != -1)
				{
					archiveout.write(bytes, 0, iRead);
				}
				else
				{
					break;
				}
			}					
			archiveout.write("</log>".getBytes());
		}
		finally
		{
			FileUtils.safeClose(finalreader);
			if( archiveout != null )
			{
				archiveout.close();				
			}
		}
		
		recent.delete();
		log.info(getFolderName() + " log rotated");
	}
	
	protected void flushRecentChanges() throws IOException
	{
		flushRecentChanges(false);
	}
	
	protected void flushRecentChanges(boolean force) throws IOException
	{
		// TODO Look recent events and look for unindexed records
		File recent = new File( getRootDirectory(),getFolderName() + "/recent.log");
		if (!recent.exists())
		{
			return;
		}
		StringWriter tmp = new StringWriter();
		tmp.write("<events>");
		Reader filer = new FileReader(recent);
		try
		{
			new OutputFiller().fill(filer, tmp);
		}
		finally
		{
			FileUtils.safeClose(filer);
		}
			tmp.write("</events>");
		
		
		Element root = getXmlUtil().getXml(new StringReader(tmp.toString() ),"UTF-8");
		boolean foundone = false;
		for (Iterator iterator = root.elementIterator("event"); iterator.hasNext();) 
		{
			Element	webEvent = (Element) iterator.next();
			if(force || !Boolean.parseBoolean( webEvent.attributeValue("indexed") ) )
			{
				foundone = true;
				updateIndex(  createChange(webEvent), getIndexWriter() );
				webEvent.addAttribute("indexed","true");
			}
		}
		if( foundone)
		{
			flush();
			StringWriter done = new StringWriter();
			//Save back out to recent.log
			for (Iterator iterator = root.elementIterator("event"); iterator.hasNext();) {
				Element	event = (Element) iterator.next();
				done.write(event.asXML());
				done.write('\n');
			}

			FileWriter filew = new FileWriter(recent);
			try
			{
				new OutputFiller().fill(new StringReader(done.toString()), filew);
			}
			finally
			{
				FileUtils.safeClose(filew);
			}

		}
		
	}

	protected String getLogFilePath()
	{
		return "/WEB-INF/logs/"+getFolderName()+"/recent.log";
	}

	/*
	protected org.dom4j.Document buildDocument(List inStack)
	{
		Element root = DocumentHelper.createElement("log");
		//root.addAttribute("timeStampCounter", String.valueOf(getTimeStampCounter()));

		for (Iterator iter = inStack.iterator(); iter.hasNext();)
		{
			WebEvent webEvent = (WebEvent) iter.next();
			Element changeElem = root.addElement("event");
			
			populateElement(webEvent, changeElem);
		}

		return DocumentHelper.createDocument(root);
	}
	*/
	private void populateElement(WebEvent webEvent, Element changeElem)
	{
		changeElem.addAttribute("operation", webEvent.getOperation());
		changeElem.addAttribute("user", webEvent.getUsername());
		for (Iterator iterator = webEvent.getProperties().keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			changeElem.addAttribute(key, webEvent.get(key));
		}
		changeElem.addAttribute("date", DateStorageUtil.getStorageUtil().formatForStorage(webEvent.getDate()));
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public void addChange(WebEvent inChange)
	{
		if( inChange.getDate() == null)
		{
			inChange.setDate(new Date());
		}
		save(inChange);
		
	}
	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}
	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}
//	public DateFormat getDateFormat()
//	{
//		if( fieldLogDateFormat == null)
//		{
//			fieldLogDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//		}
//		return fieldLogDateFormat;
//	}
//	public void setDateFormat(DateFormat inDateFormat)
//	{
//		fieldLogDateFormat = inDateFormat;
//	}
	/*
	public List getAllEntriesFrom(Date inStart, Date inEnd) throws Exception
	{
		List allchanges = new ArrayList();
		String stampedPath = PathUtilities.extractDirectoryPath(getLogFilePath());
		List l  = getPageManager().getChildrenPaths(stampedPath);
		Collections.sort(l);
		
		List recent = loadRecentEvents(inStart);
		if( recent != null )
		{
			allchanges.addAll(recent);
		}
		
		for (Iterator iter = l.iterator(); iter.hasNext();)
		{
			String path = (String) iter.next();
			if( path.endsWith(".xml"))
			{
				String find = getFolderName() +"-";
				String datestring = path.substring(path.indexOf(find) + find.length());
				Date dated = getFileDateFormat().parse(datestring);
				
				if( dated.before(inStart))
				{
					
					continue;
				}
				if( dated.after(inStart) && dated.before(inEnd))
				{
					List changeList = loadChangeLog(path);
					allchanges.addAll(changeList);					
				}
				else if (dated.after(inEnd) )
				{
					//add this one extra in case of some older logs
					List changeList = loadChangeLog(path);
					allchanges.addAll(changeList);				
					break;
				}
			}
		}
		List done = new ArrayList(allchanges.size());
		for (Iterator iter = allchanges.iterator(); iter.hasNext();)
		{
			WebEvent webEvent = (WebEvent) iter.next();
			if( webEvent.getDate().after(inStart) && webEvent.getDate().before(inEnd))
			{
				done.add(webEvent);
			}
		}
		
		return done;
	}
	*/
	public File getRootLogsDirectory()
	{
		if (fieldRootLogsDirectory == null)
		{
			fieldRootLogsDirectory = new File( getRootDirectory(),getFolderName());
		}
		return fieldRootLogsDirectory;
	}
	
	public DateFormat getFileDateFormat()
	{
		if( fieldFileDateFormat == null)
		{
			fieldFileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		}
		return fieldFileDateFormat;
	}
	public void setFileDateFormat(DateFormat inFileDateFormat)
	{
		fieldFileDateFormat = inFileDateFormat;
	}
//	public List getAllEntries() throws Exception
//	{
//		List allchanges = new ArrayList();
//		String stampedPath = PathUtilities.extractDirectoryPath(getLogFilePath());
//		List l  = getPageManager().getChildrenPaths(stampedPath);
//		Collections.sort(l);
//		
//		List recent = loadAllRecentEvents();
//		if( recent != null )
//		{
//			allchanges.addAll(recent);
//		}
//		
//		for (Iterator iter = l.iterator(); iter.hasNext();)
//		{
//			String path = (String) iter.next();
//			if( path.endsWith(".xml"))
//			{
//				String find = getFolderName() +"-";
//				String datestring = path.substring(path.indexOf(find) + find.length());
//				Date dated = getFileDateFormat().parse(datestring);
//				
//				List changeList = loadChangeLog(path);
//				allchanges.addAll(changeList);					
//				
//			}
//		}
//		return allchanges;
//	}
//	
	public String getIndexPath()
	{
		if (fieldIndexPath == null)
		{
			fieldIndexPath = "/" + getFolderName() + "/index";
		}
		return fieldIndexPath;
	}


	public void eventFired(WebEvent inEvent)
	{
		save(inEvent);
	}

/*
	public void flushCacheToIndex() throws Exception {
		// TODO Auto-generated method stub
		List recent = loadAllRecentEvents();
		if( recent != null )
		{
			for (Iterator iterator = recent.iterator(); iterator.hasNext();) 
			{
				WebEvent change = (WebEvent) iterator.next();
				Document doc = new Document();
				populateEntry( doc, change, false);

				getIndexWriter().addDocument(doc, getAnalyzer());
			}
			File recentlog = new File( getRootDirectory(),getFolderName() + "/recent.log");
			moveRecent(recentlog);
			flush();
		}
	}
*/
	
	
	public void saveData(Object inData, User inUser)
	{
		if( inData instanceof WebEvent)
		{
			save((WebEvent)inData);
		}
		else
		{
			super.saveData(inData, inUser);
		}
	}
	
	
}
