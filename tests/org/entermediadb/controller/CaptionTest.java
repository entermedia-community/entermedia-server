package org.entermediadb.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.video.VTT.Cue;
import org.entermediadb.video.VTT.webvtt.WebvttParser;
import org.entermediadb.video.VTT.webvtt.WebvttSubtitle;
import org.openedit.modules.translations.LanguageMap;

public class CaptionTest extends BaseEnterMediaTest {

	private static final Log log = LogFactory.getLog(CaptionTest.class);



	
	
	public void  testParseVtt() throws Exception{
		File testfile = new File( getRoot().getParentFile().getAbsolutePath() + "/etc/web.vtt");
		WebvttParser parser = new WebvttParser();
		WebvttSubtitle titles = parser.parse(new FileInputStream(testfile));
		assertNotNull(titles);
		assertNotNull(titles.getCues());
		assertEquals(8, titles.getCues().size());
	}

	
	public void  testSaveCues() throws Exception{
		File testfile = new File( getRoot().getParentFile().getAbsolutePath() + "/etc/web.vtt");
		WebvttParser parser = new WebvttParser();
		WebvttSubtitle titles = parser.parse(new FileInputStream(testfile));
		MediaArchive archive = getMediaArchive("media/catalogs/public");

		
		Asset asset = archive.getAsset("captiontest");
		if(asset != null){
			archive.getAssetSearcher().delete(asset, null);
		}
		asset = (Asset) archive.getAssetSearcher().createNewData();
		asset.setId("captiontest");
		asset.setSourcePath("captiontest");
		Collection captions = new ArrayList();
		for (Iterator iterator = titles.getCues().iterator(); iterator.hasNext();)
		{
			Cue cue = (Cue) iterator.next();
			HashMap cuemap = new HashMap();
			LanguageMap map = new LanguageMap();
			map.setText("en", cue.getText().toString());
			map.setText("fr", "French Version");
			cuemap.put("captiontext", map);
			cuemap.put("timecodestart", cue.getPosition());
			cuemap.put("alignment", cue.getAlignment());
			cuemap.put("timecodestart", Double.valueOf(cue.getPosition()));
			cuemap.put("timecodelength", cue.getSize());
			captions.add(cuemap);

			

		}
		asset.setValue("captions", captions);
		archive.getAssetSearcher().saveData(asset);
		asset = (Asset) archive.getAssetSearcher().searchById("captiontest");
		captions = (Collection) asset.getValue("captions");

		assertNotNull(captions);
		assertEquals(6,captions.size() );
		//Map values = captions.
		
		for (Iterator iterator = captions.iterator(); iterator.hasNext();)
		{
			Map cues = (Map) iterator.next();
			assertTrue(cues.get("captiontext") instanceof LanguageMap);
			
		
		}
		
		
		
	}

	
//	<property id="captiontext" index="true" stored="true" editable="true" multilanguage="true">
//    <name> 
//      <language id="en"><![CDATA[Caption Text]]></language>  
//    </name> 
//    </property>
//    <property id="timecodestart" index="true" stored="true" editable="true"  datatype="double" viewtype="videotime">Start Time</property>
//    <property id="timecodeend" index="true" stored="true" editable="true" datatype="double" viewtype="videolength">Duration</property>   
//    <property id="alignment" index="true" stored="true" editable="true" type="list" >Alignment</property>   
//    
//	
//	



}
