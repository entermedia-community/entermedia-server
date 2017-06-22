/*
 * Created on Oct 2, 2005
 */
package org.entermediadb.asset.scanner;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseCategory;
import org.entermediadb.asset.CatalogConverter;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.ConvertStatus;
import org.entermediadb.asset.MediaArchive;
import org.openedit.page.Page;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.XmlUtil;

public class DvdImportConverter extends CatalogConverter
{
	// protected List fieldInputTypes;
	private static final Log log = LogFactory.getLog(DvdImportConverter.class);

	public void importAssets(MediaArchive inArchive, ConvertStatus inErrorLog) throws Exception
	{
		File input = new File(inArchive.getCatalogHome(), "/scanner/dvdimport.xml");
		if (input.exists())
		{
			Element root = new XmlUtil().getXml(input, "UTF-8");
			// List all the chapters if any

			File file = new File("/media/cdrom/video_ts");
			if (file.exists())
			{
				Dvd dvd = new Dvd();
				Exec exec = new Exec();
				List usercomlist = new ArrayList();
				usercomlist.add("dvdbackup");
				usercomlist.add("-I");
				usercomlist.add("-i/media/cdrom");
				ExecResult result = exec.runExec(usercomlist, true);
				String done = result.getStandardOut();
				loadChapters(dvd, file, done);
				loadTitle(file, dvd, done);

				Category category = findCategory(inArchive, dvd.getTitle());
				String destination = root.elementText("savepath");
				destination = destination + "/" + category.getId();
				dvd.setPath(destination);
				File exist = new File(dvd.getPath());
				if (exist.exists())
				{
					inErrorLog.add("Already exists" + dvd.getPath());
					log.error("Already exists" + dvd.getPath());
				}
				saveAssets(dvd, category, inArchive, inErrorLog);
				inArchive.getCategoryArchive().saveAll();
			}
		}
	}

	protected void loadChapters(Dvd indvd, File file, String done) throws Exception
	{
		// dvdbackup -I -i/dev/cdrom
		// context.putPageValue("done",done);
		if (done == null)
		{
			return;
		}
		List chapters = new ArrayList();

		// CHAPTERS
		int set = new Integer(1).intValue();
		while (true) // Section
		{
			int start = done.indexOf("Title set " + set);
			log.info("Found " + set);
			if (start == -1)
			{
				log.info("Found up to " + set);
				break;
			}
			int aspectstart = done.indexOf("is", start);
			// String aspect = done.substring(aspectstart,aspectstart+10);
			int countstart = done.indexOf("Title " + set + " has", aspectstart + 9);
			int countend = done.indexOf("chapter(s)", countstart);

			String count = done.substring(countstart + 11, countend);
			int totalc = Integer.parseInt(count.trim());
			for (int c = 0; c < totalc; c++)
			{
				chapters.add(set + "." + (c + 1));
			}
			set = set + 1;
		}
		indvd.setChapterNames(chapters);
		// context.putPageValue("chapters" , chapters);
	}

	protected void loadTitle(File file, Dvd inDvd, String done)
	{
		// LAST MOD
		Date dated = new Date(file.lastModified());
		DateFormat formater = new SimpleDateFormat("yyyy_MM_dd");
		String dirdate = formater.format(dated);
		dirdate = dirdate.replace('/', '_');
		// context.putPageValue( "dirdate",dirdate );
		// TITLE
		int titlestart = done.indexOf("with title");
		int titleend = done.indexOf("\n", titlestart);
		String title = done.substring(titlestart + 11, titleend);
		if (title.length() < 5)
		{
			title = dirdate;
		}
		inDvd.setLastModified(dirdate);
		title = title + "_" + inDvd.getChapterNames().size() + "chapters";
		inDvd.setTitle(title);
	}

	protected void saveAssets(Dvd inDvd, Category inCategory, MediaArchive inArchive, ConvertStatus inStatus) throws Exception
	{
		NumberFormat format = new DecimalFormat("00");
		List assets = new ArrayList();
		for (int i = 0; i < inDvd.getChapterNames().size(); i++)
		{
			String chapter = (String) inDvd.getChapterNames().get(i);

			Asset asset = new Asset(inArchive);
			String count = format.format(i + 1);
			chapter = chapter.replace('.', '_');

			String name = count + "_" + inCategory.getId() + "_" + chapter + ".mpg";
			asset.setName(name);
			asset.setId(extractId(name, true));
			asset.addCategory(inCategory);

			Exec execconvert = new Exec();
			List convertlist = new ArrayList();
			String script = findScript("extractdvd.sh", inArchive);
			convertlist.add(script);// tccat -i /media/cdrom -T 1,1 -a > out.avi
			chapter = chapter.replace('_', ',');
			convertlist.add(chapter);

			File out = new File(inDvd.getPath(), name);
			if (out.exists())
			{
				continue;
			}
			out.getParentFile().mkdirs();
			convertlist.add(out.getAbsolutePath());

			execconvert.runExec(convertlist);
			// var done = exec.getStandardOutput();
			log.info("Completed Chapter" + name);
			asset.setProperty("originalpath", out.getAbsolutePath());
			asset.setProperty("dvdtitle", inDvd.getTitle());
			// Aspect ration?
			inArchive.getAssetArchive().saveAsset(asset);
			assets.add(asset);
		}
		inArchive.getCategoryArchive().saveCategory(inCategory);

		inArchive.getAssetSearcher().updateIndex(assets);
		inStatus.addConvertedAssets(assets);
	}

	protected String findScript(String inScript, MediaArchive inArchive)
	{
		Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/admin/convert/" + inScript);

		// File script = new File(
		// inArchive.getArchiveDirectory(),"/admin/convert/" + inScript);
		// if ( !script.exists() )
		// {
		// script = new File(
		// inArchive.getArchiveDirectory().getParentFile(),"/base/archive/admin/convert/"
		// + inScript);
		// }
		// return script.getAbsoluteFile().getAbsolutePath();
		return page.getContentItem().getAbsolutePath();
	}

	private Category findCategory(MediaArchive inArchive, String inTitle)
	{
		String catid = extractId(inTitle, true);
		Category category = inArchive.getCategoryArchive().getCategory(catid);
		if (category == null)
		{
			category = new BaseCategory();
			// category.setProperty("dvddate", dirdate);
			category.setName(inTitle);
			category.setId(catid);
		}
		return category;
	}

	protected File[] findAssetXConfFiles(File inParent)
	{
		FileFilter filter = new FileFilter()
		{
			public boolean accept(File inDir)
			{
				String inName = inDir.getName().toLowerCase();
				if (inName.endsWith(".jpg") || inName.endsWith(".avi"))
				{
					return true;
				}
				if (inDir.isDirectory())
				{
					return true;
				}
				return false;
			}
		};
		return inParent.listFiles(filter);
	}

	protected void findFiles(File inSearchDirectory, List inAll, FileFilter inFilter)
	{
		File[] toadd = inSearchDirectory.listFiles(inFilter);

		for (int i = 0; i < toadd.length; i++)
		{
			File file = toadd[i];
			if (file.isDirectory())
			{
				findFiles(file, inAll, inFilter);
			}
			else
			{
				inAll.add(file);
			}
		}
	}
}
