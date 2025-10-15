package org.entermediadb.video;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;

public class BaseTranscriber implements CatalogEnabled {
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected MediaArchive fieldMediaArchive;


	public MediaArchive getMediaArchive() {
		if (fieldMediaArchive == null) {
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive) {
		fieldMediaArchive = inMediaArchive;
	}

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

}
