package org.entermediadb.asset.generators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.FileGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class OriginalPreviewDocumentGenerator extends FileGenerator
{
		private static final Log log = LogFactory.getLog(OriginalPreviewDocumentGenerator.class);
		protected ModuleManager moduleManager;

		public ModuleManager getModuleManager()
		{
			return moduleManager;
		}

		public void setModuleManager(ModuleManager moduleManager)
		{
			this.moduleManager = moduleManager;
		}

		public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
		{
			String catalogid = inReq.findPathValue("catalogid");
			MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
			String assetid = inReq.getRequestParameter("assetid");
			Asset asset = archive.getAsset(assetid, inReq);
			
			Page content = archive.getOriginalDocument(asset);
			String version = inReq.getRequestParameter("version");
			if (version == null) {
				log.error("Version Not found");
				return;
				//error
			}
			ContentItem revision = archive.getPageManager().getRepository().getVersion(content.getContentItem(), version);
			String folder = PathUtilities.extractDirectoryPath(revision.getPath());
			Page preview = archive.getPageManager().getPage( folder + "/" + inPage.getName());
			
			if (!preview.exists()) {
				log.error("preview Not found " + preview.getPath());
				return;
				//error
			}
			WebPageRequest req = inReq.copy(preview);
			req.putProtectedPageValue(PageRequestKeys.CONTENT, preview);
			super.generate(req, preview, inOut);
			//error?
		}

		public boolean canGenerate(WebPageRequest inReq)
		{
			return true;
		}

}
