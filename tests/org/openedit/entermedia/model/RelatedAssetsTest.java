/*
 * Created on Jun 9, 2005
 */
package org.openedit.entermedia.model;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.RelatedAsset;
import org.openedit.entermedia.modules.RelatedAssetsModule;

import com.openedit.WebPageRequest;

/**
 * @author cburkey
 * 
 */
public class RelatedAssetsTest extends BaseEnterMediaTest
{
	/**
	 * @param inArg0
	 */
	public RelatedAssetsTest(String inArg0)
	{
		super(inArg0);
	}

	public void testRelateProducts() throws Exception
	{
		RelatedAssetsModule module = (RelatedAssetsModule) getFixture().getModuleManager().getModule("RelatedAssetsModule");

		WebPageRequest context = getFixture().createPageRequest("/entermedia/index.html");
		MediaArchive archive = getMediaArchive();
		Asset source = createAsset();
		Asset target = createAsset();
		archive.saveAsset(source, context.getUser());
		archive.saveAsset(target, context.getUser());
		

		context.setRequestParameter("catalogid", getMediaArchive().getCatalogId());
		context.setRequestParameter("assetid", source.getId());
		context.setRequestParameter("relatedtoassetid", target.getId());
		context.setRequestParameter("type", "link");

		module.addRelatedAsset(context);
		source = archive.getAsset(source.getId());
		assertTrue(source.getRelatedAssets().size() > 0);
		assertTrue(source.isRelated(target));
		assertEquals("link", ((RelatedAsset) source.getRelatedAssets().iterator().next()).getType());
		getMediaArchive().getAssetArchive().deleteAsset(target);
		getMediaArchive().getAssetArchive().deleteAsset(source);

	}

	public void testMutliCatalog() throws Exception
	{

		RelatedAssetsModule module = (RelatedAssetsModule) getFixture().getModuleManager().getModule("RelatedAssetsModule");
		
		WebPageRequest context = getFixture().createPageRequest("/media/index.html");
		MediaArchive photo = getMediaArchive("media/catalogs/photo");
		Asset source = createAsset(photo);
		photo.saveAsset(source, context.getUser());
		
		MediaArchive audio = getMediaArchive("media/catalogs/audio");
		Asset target = createAsset(audio);
		audio.saveAsset(target, context.getUser());
		
		context.setRequestParameter("assetid", source.getId());
		context.setRequestParameter("catalogid", source.getCatalogId());
		context.setRequestParameter("relatedtoassetid", new String[] { target.getId() });
		context.setRequestParameter("relatedtocatalogid", new String[] { target.getCatalogId() });
		context.setRequestParameter("type", "state");

		module.addRelatedAsset(context);

		source = photo.getAsset(source.getId());
		assertEquals(1, source.getRelatedAssets().size());
		RelatedAsset p = (RelatedAsset) source.getRelatedAssets().iterator().next();
		target = getMediaArchive(p.getRelatedToCatalogId()).getAsset(p.getRelatedToAssetId());
		assertNotNull(target);
		assertEquals("state", p.getType());

		photo.getAssetArchive().deleteAsset(source);
		audio.getAssetArchive().deleteAsset(target);
	}

}
