/*
 * Created on Apr 30, 2004
 */
package org.entermediadb.model;

import java.util.Collection;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.facedetect.FaceProfileManager;
import org.openedit.Data;
import org.openedit.util.FileUtils;

import com.google.gson.JsonParser;

/**
 * @author cburkey
 *
 */
public class FaceDetectManagerTest extends BaseEnterMediaTest
{

	Asset testvideo;
	Asset testimage;
	/**
	 * Constructor for ItemEditTest.
	 * @param arg0
	 */
	public FaceDetectManagerTest(String arg0)
	{
		super(arg0);
	}

	protected void setUp() throws Exception
	{
//		if( testvideo == null)
//		{
//			testvideo = createAsset();
//			//TODO: Download this file:
//			FileUtils util = new FileUtils();
//			//util.copyFiles("/tmp/manyfaces.mp4", getMediaArchive().getCatalogHome() + "/originals/faces/manyfaces.mp4");
//			testvideo.setSourcePath("face/manyfaces.mp4");
//		}
		if( testimage == null)
		{
			testimage = createAsset();
			FileUtils util = new FileUtils();
			testimage.setSourcePath("faces/billg.jpeg");
			testimage.setValue("fileformat","jpg");
		}
		//getMediaArchive().reindexAll();
		getMediaArchive().getSearcher("catalogsettings").reIndexAll();
	}
	/*
	public void testScan4Profiles() throws Exception
	{
		FaceDetectManager manager = (FaceDetectManager)getMediaArchive().getBean("faceDetectManager");
		assertTrue("Must be true" ,manager.extractFaces(getMediaArchive(), testvideo) );
		
	}
	*/
	public void testCompareValues() throws Exception
	{
		FaceProfileManager manager = (FaceProfileManager)getMediaArchive().getBean("faceProfileManager");
		Data parent = getMediaArchive().getCachedData("faceembedding", "123");
		Data child = getMediaArchive().getCachedData("faceembedding", "432");
		
		Collection parentvalues =  (Collection)JsonParser.parseString(parent.get("facedatajson"));
		double[] parentd = manager.collectDoubles(parentvalues); 

		Collection childvalues =  (Collection)JsonParser.parseString(parent.get("facedatajson"));
		double[] childd = manager.collectDoubles(childvalues); 
		
		assertFalse( manager.compareVectors(parentd, childd, .6D) );

//		manager.extractFaces(testimage);
//		
//		Map profiles = (Map)testimage.getValue("faceprofiles");
//		assertNotNull(profiles);
	}
}
