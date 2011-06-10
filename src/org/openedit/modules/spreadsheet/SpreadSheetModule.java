package org.openedit.modules.spreadsheet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.UploadRequest;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;
import com.openedit.page.Page;

public class SpreadSheetModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(SpreadSheetModule.class);
	protected XmlArchive fieldXmlArchive;
	protected Map fieldSpreadSheets;
	protected FileUpload fileUpload;
	
	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public Map getSpreadSheets()
	{
		if (fieldSpreadSheets == null)
		{
			fieldSpreadSheets = new HashMap();
		}

		return fieldSpreadSheets;
	}

	public void setSpreadSheets(Map inSpreadSheets)
	{
		fieldSpreadSheets = inSpreadSheets;
	}

	public SpreadSheet loadSheet(WebPageRequest inReq) throws Exception
	{
		String inPath = inReq.getRequestParameter("sheet");
		if( inPath == null)
		{
			return null;
		}
		SpreadSheet sheet = cacheSheet(inPath);
		inReq.putPageValue("sheet", sheet);
		return sheet;
	}

	protected SpreadSheet cacheSheet(String inPath)
	{
		SpreadSheet sheet = (SpreadSheet)getSpreadSheets().get(inPath);
		if( sheet == null)
		{
			sheet = new SpreadSheet();
			XmlFile file = getXmlArchive().getXml(inPath);
			sheet.setXmlFile(file);
			sheet.setId(inPath);
			getSpreadSheets().put(inPath,sheet);
		}
		return sheet;
	}
	
	public Collection listSheets(WebPageRequest inReq) throws Exception
	{
		String base = findSheetPath( inReq);
		if( base == null)
		{
			return null;
		}
		List children = getPageManager().getChildrenPaths(base);
		List hits = new ArrayList();
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String onepath = (String) iterator.next();
			if( onepath.endsWith(".xml"))
			{
				//inReq.setRequestParameter("sheet", onepath);
				SpreadSheet sheet = cacheSheet(onepath);
				hits.add(sheet);
			}
		}
		inReq.putPageValue("hits", hits);
		return hits;
	}
	public String findSheetPath(WebPageRequest inReq)
	{
		String postfix = inReq.findValue("sspostfix");
		
		String baseDir = inReq.findValue("spreadsheetdir");
		if( postfix != null && postfix.indexOf("..") > -1 )
		{
			throw new IllegalArgumentException(".. not allowed in " + postfix);
		}
		if(baseDir == null)
		{
			return null;
		}
		if( postfix == null)
		{
			return baseDir;
		}
		return baseDir + postfix;
	}
	public void loadCell(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		
		String inRowId = inReq.getRequestParameter("row");
		String inCellId = inReq.getRequestParameter("cell");
		
		//sheet.setValue(inRowId, inCellId, value);
		Row row = sheet.getRow( inRowId);
		Cell cell = row.getCell(inCellId);
		inReq.putPageValue("row", row);
		inReq.putPageValue("cell", cell);
		log.info("Loading " + inCellId);
	}
	public void saveValue(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		
		String inRowId = inReq.getRequestParameter("row");
		String inCellId = inReq.getRequestParameter("cell");
		String value = inReq.getRequestParameter("value");
		
		//sheet.setValue(inRowId, inCellId, value);
		Row row = sheet.getRow( inRowId);
		Cell cell = row.setValue(inCellId, value);
		inReq.putPageValue("row", row);
		inReq.putPageValue("cell", cell);
		sheet.setLastEditedBy(inReq.getUserName());
		getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
	}
	
	public void saveRow(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		
		String inRowId = inReq.getRequestParameter("row");
		String value = inReq.getRequestParameter("height");
		
		sheet.saveRowHeight(inRowId, value);
		sheet.setLastEditedBy(inReq.getUserName());
		
		getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
	}
	public void saveColumn(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		
		String inRowId = inReq.getRequestParameter("column");
		String value = inReq.getRequestParameter("width");
		
		sheet.saveColumnWidth(inRowId, value);
		sheet.setLastEditedBy(inReq.getUserName());
		getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
	}
	
	public void insert(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		
		String inColIndex = inReq.getRequestParameter("colindex");
		if( inColIndex != null)
		{
			sheet.insertColumn(Integer.parseInt( inColIndex ) );
		}
		String inRowIndex = inReq.getRequestParameter("rowindex");
		if( inRowIndex != null)
		{
			sheet.insertRow(Integer.parseInt( inRowIndex ) );
		}
		sheet.setLastEditedBy(inReq.getUserName());
		getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
	}
	
	public void delete(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		
		String inColId = inReq.getRequestParameter("colid");
		if( inColId != null)
		{
			sheet.deleteColumn(inColId );
		}
		String rowid = inReq.getRequestParameter("rowid");
		if( rowid != null)
		{
			sheet.deleteRow( rowid  );
		}
		sheet.setLastEditedBy(inReq.getUserName());
		getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
	}
	public void importData(WebPageRequest inReq) throws Exception
	{
		UploadRequest upload = getFileUpload().parseArguments(inReq);
		SpreadSheet sheet = loadSheet(inReq);
		String path = sheet.getPath() + ".tmp.csv";
		upload.saveFirstFileAs(path, inReq.getUser());
		
		Page page = getPageManager().getPage(path);
		sheet.importCsv(page);
		getPageManager().removePage(page);
		sheet.setLastEditedBy(inReq.getUserName());
		getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
		
	}
	public void createNew(WebPageRequest inReq) throws Exception
	{
		String base = findSheetPath( inReq);
		Page template = getPageManager().getPage("/WEB-INF/base/entermedia/tools/spreadsheet/template.xml");
		
		String filename = inReq.findValue("filename");
		if( filename != null )
		{
			if( !filename.endsWith(".xml"))
			{
				filename = filename + ".xml";
			}
		}
		Page dest = getPageManager().getPage(base + "/" + filename);
		getPageManager().copyPage(template, dest);
		inReq.setRequestParameter("sheet", dest.getPath());
		loadSheet(inReq);
	}

	public FileUpload getFileUpload()
	{
		return fileUpload;
	}

	public void setFileUpload(FileUpload inFileUpload)
	{
		fileUpload = inFileUpload;
	}
	
	public void deleteSheet(WebPageRequest inReq) throws Exception
	{
		String inOk = inReq.getRequestParameter("deleteok");
		if( Boolean.parseBoolean(inOk))
		{
			SpreadSheet sheet = loadSheet(inReq);
			inReq.removePageValue("sheet");
			Page page = getPageManager().getPage(  sheet.getXmlFile().getPath() );
			getPageManager().removePage(page);
			listSheets(inReq);
		}
	}	
	public void renameSheet(WebPageRequest inReq) throws Exception
	{
		String newname = inReq.getRequestParameter("newname");
		//log.info(inReq.getRequest().getQueryString());
		if( newname != null )
		{

			SpreadSheet sheet = loadSheet(inReq);
			inReq.removePageValue("sheet");
			Page page = getPageManager().getPage(  sheet.getXmlFile().getPath() );

			Page dest = getPageManager().getPage(page.getDirectory() + "/" + newname + ".xml");
			getPageManager().movePage(page, dest);
			listSheets(inReq);
		}
		
	}	
	
	public void addColorRule(WebPageRequest inReq) throws Exception
	{
		String color = inReq.getRequestParameter("color");
		String contains = inReq.getRequestParameter("contains");
		boolean row = Boolean.parseBoolean(inReq.getRequestParameter("row"));
		
		SpreadSheet sheet = loadSheet(inReq);
		
		if (sheet != null && color != null && contains != null)
		{
			ColorRule rule = new ColorRule();
			rule.setColor(color);
			rule.setContains(contains);
			rule.setRow(row);
			sheet.addColorRule(rule);
	
			sheet.setLastEditedBy(inReq.getUserName());
			getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
		}
	}
	
	public void deleteColorRule(WebPageRequest inReq) throws Exception
	{
		SpreadSheet sheet = loadSheet(inReq);
		String contains = inReq.getRequestParameter("contains");
		
		if (sheet != null && contains != null)
		{
			sheet.removeRule(contains);
			sheet.setLastEditedBy(inReq.getUserName());
			getXmlArchive().saveXml(sheet.getXmlFile(), inReq.getUser());
		}
	}
}
