package org.entermediadb.asset.generators;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.Generator;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.error.ContentNotAvailableException;
import org.openedit.generators.FileGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

/**
 * This generator generates original asset documents from an MediaArchive based
 * on paths of the form <tt>.../<var>assetid</var>/<var>filename.ext</var></tt>.
 */
public class PublishedGenerator extends FileGenerator
{
	protected ModuleManager fieldModuleManager;
	protected SearcherManager fieldSearcherManager;
	
	public Generator getVelocityGenerator()
	{
		return fieldVelocityGenerator;
	}

	public void setVelocityGenerator(Generator inVelocityGenerator)
	{
		fieldVelocityGenerator = inVelocityGenerator;
	}

	public Generator getMp4Generator()
	{
		return fieldMp4Generator;
	}

	public void setMp4Generator(Generator inMp4Generator)
	{
		fieldMp4Generator = inMp4Generator;
	}

	public Generator getConvertGenerator()
	{
		return fieldConvertGenerator;
	}

	public void setConvertGenerator(Generator inConvertGenerator)
	{
		fieldConvertGenerator = inConvertGenerator;
	}
	protected Generator fieldVelocityGenerator;
	protected Generator fieldMp4Generator;
	protected Generator fieldConvertGenerator;
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	
	
	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		String catalogid = inReq.findValue("catalogid");

		String path = null;
		String publishedroot = inPage.get("publishedroot");

		path = inPage.getPath().substring(publishedroot.length() + 1);

		
		String[] paths = path.split("/");
		String guid = paths[0];
		
		String presetid = "0";
		if (paths.length > 1) {
		  presetid = paths[1];
		}
		Data dist = getSearcherManager().getCachedData(catalogid, "distribution", guid);
		if (dist == null) {
			throw new ContentNotAvailableException("Distribution Not Available", path);
		}
		Data asset = getSearcherManager().getCachedData(catalogid, "asset", dist.get("assetid"));
		if (asset == null) {
			throw new ContentNotAvailableException("Asset Not Available", path);
		}
		Data preset = getSearcherManager().getCachedData(catalogid, "convertpreset", presetid);
		if (preset == null) {
			throw new ContentNotAvailableException("Distribution Preset Not Available", path);
		}

		MediaArchive archive = (MediaArchive)getModuleManager().getBean(catalogid,"mediaArchive",true);
		
		inReq.setRequestParameter("sourcepath", asset.getSourcePath());		
		if(presetid.equals("0")) {
			if ((Boolean)dist.getValue("alloworiginal") == true) {
				String genpath = archive.asLinkToOriginal(asset);			
				Page gen = getPageManager().getPage(genpath);
				inReq.getResponse().setContentType(gen.getMimeType());
				getConvertGenerator().generate(inReq, gen, inOut);
				return;
			} else { 
				throw new ContentNotAvailableException("Distribution Orignal Not Available", path);
			}
		}
		if( presetid.contains("videohls"))
		{
			if(inPage.getName().endsWith("videohls") )
			{
				inReq.putPageValue("asset",asset);
				inReq.putPageValue("mediaarchive",archive);
				//return an html velocity page
				String applicationid = inReq.findValue("applicationid");
				Page player = getPageManager().getPage("/" + applicationid + "/services/module/asset/players/embed/video.html");
				inReq.getResponse().setContentType("text/html");
				getVelocityGenerator().generate(inReq, player, inOut);
				return;
			}
			else
			{
				renderHlsFile(inReq, catalogid, asset, path, inPage, inOut);				
			}
		}
		else if( presetid.contains("video"))
		{
			String genpath = archive.asLinkToPreview(asset, preset.get("generatedoutputfile"));
			Page mp4 = getPageManager().getPage(genpath);
			getMp4Generator().generate(inReq, mp4, inOut);
		}
		else
		{
			String genpath = archive.asLinkToPreview(asset, preset.get("generatedoutputfile"));
			Page gen = getPageManager().getPage(genpath);
			inReq.getResponse().setContentType(gen.getMimeType());
			getConvertGenerator().generate(inReq, gen, inOut);
		}
	}
	protected void renderHlsFile(WebPageRequest inReq, String catalogid, Data asset, String path, Page inPage, Output inOut)
	{
		ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + catalogid + "/generated/" + asset.getSourcePath() + "/" + path);
		Page output = null;
		boolean existed = item.exists();
		if (existed)
		{

			output = new Page()
			{
				public boolean isHtml()
				{
					return false;
				}
			};
			output.setPageSettings(inPage.getPageSettings());
			output.setContentItem(item);
		}
		if (!existed && !output.exists())
		{
			throw new ContentNotAvailableException("Missing: " + output.getPath(), output.getPath());
		}
		else
		{
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content", output);
			super.generate(copy, output, inOut);
			// archive.logDownload(sourcePath, "success", inReq.getUser());
		}
	}
	
	

}


