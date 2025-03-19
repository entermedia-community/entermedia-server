package org.entermediadb.asset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.edit.AssetEditor;
import org.entermediadb.asset.edit.CategoryEditor;
import org.openedit.BaseTestCase;


public class BaseEnterMediaTest extends BaseTestCase
{
	
	private static final Log log = LogFactory.getLog(BaseEnterMediaTest.class);
	public BaseEnterMediaTest(String inName)
	{
		super(inName);
	}

	public BaseEnterMediaTest()
	{
	}
	
	public EnterMedia getEnterMedia(String inApplicationId)
	{
		EnterMedia media = (EnterMedia)getFixture().getModuleManager().getBean(inApplicationId, "enterMedia");
		media.setApplicationId(inApplicationId);
		return media;
	}
	
	public EnterMedia getEnterMedia()
	{
		return getEnterMedia("entermedia");
	}
	
	public MediaArchive getMediaArchive(String inCatalogId)
	{
		return getEnterMedia().getMediaArchive(inCatalogId);
	}
	
	public MediaArchive getMediaArchive()
	{
		return getMediaArchive(getFixture().getCategoryId());
	}
	
	protected CategoryEditor getCategoryEditor()
	{
		return getMediaArchive().getCategoryEditor();
	}
	
	protected AssetEditor getAssetEditor()
	{
		return getMediaArchive().getAssetEditor();
	}
	
	protected Asset createAsset(MediaArchive archive)
	{
		Asset asset = archive.getAssetEditor().createAsset();
		asset.setMediaArchive(archive);
		String newId = archive.getAssetSearcher().nextAssetNumber();
		asset.setId(newId); // just in case case matters
		asset.setName("Test asset");
		asset.setSourcePath("test/" + newId);
		//asset.setCatalogId(archive.getCatalogId());
		asset.setFolder(true);
		return asset;
	}
	
	protected Asset createVideoAsset(MediaArchive archive)
	{
		Asset asset = createAsset(archive);
		asset.setPrimaryFile("video.avi"); //original file
		asset.setAttachmentFileByType("image","thumb.jpg"); 
		return asset;
	}
	
	protected Asset createAsset()
	{
		return createAsset(getMediaArchive());
	}
	
	protected void oneTimeSetup() throws Exception
	{
	    //executed only once, before the first test
		log.info("Reindex start");
		
			MediaArchive archive = getMediaArchive(getFixture().getCategoryId());
			archive.getAssetSearcher().reIndexAll();
			Thread.sleep(1000);

	}
}
