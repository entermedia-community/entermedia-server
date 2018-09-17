package org.entermediadb.asset.scanner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.Script;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.scripts.ScriptManager;
import org.entermediadb.scripts.TextAppender;
import org.openedit.Data;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.OutputFiller;

public class ScriptMetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(ScriptMetadataExtractor.class);
	protected PageManager fieldPageManager;

	OutputFiller filler = new OutputFiller();

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{

		HitTracker sidecarformats = (HitTracker) inArchive.getList("metadatascript");

		ScriptManager manager = (ScriptManager) inArchive.getBean("scriptManager");
		for (Iterator iterator = sidecarformats.iterator(); iterator.hasNext();)
		{
			Data sidecar = (Data) iterator.next();
			String scriptpath = sidecar.get("script");

			HashMap map = new HashMap();
			map.put("catalogid", inArchive.getCatalogId());

			String script = inArchive.getReplacer().replace(scriptpath, map);
			Page page = getPageManager().getPage(script);

			Script reportscript = manager.loadScript(page.getPath());

			final StringBuffer output = new StringBuffer();
			TextAppender appender = new TextAppender()
			{
				public void appendText(String inText)
				{
					output.append(inText);
					output.append("<br>");
				}
			};

			ScriptLogger logs = new ScriptLogger();
			logs.setPrefix(reportscript.getType());
			logs.setTextAppender(appender);
			try
			{
				logs.startCapture();
				Map variableMap = new HashMap();
				variableMap.put("asset", inAsset);
				variableMap.put("input", inFile);
				variableMap.put("archive", inArchive);
				variableMap.put("log", logs);

				Object returned = manager.execScript(variableMap, reportscript);
				if (returned != null)
				{
					output.append("returned: " + returned);
				}
			}
			finally
			{
				logs.stopCapture();
			}

			//detail ID

		}

		//Where do we store the sidecar files?  Generated?  Originals?
		//Start by checking next to file, add a mask?  ${asset.name}.xls

		return false;
	}

}
