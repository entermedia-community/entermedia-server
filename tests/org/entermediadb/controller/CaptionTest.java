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
import org.openedit.Data;
import org.openedit.data.Searcher;
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

	
		Collection captions = new ArrayList();
		
		Searcher searcher = archive.getSearcher("videotrack");
		searcher.deleteAll(null);
		Data cuetest = searcher.createNewData();
		cuetest.setName("Cuepoint Testing");

		
		for (Iterator iterator = titles.getCues().iterator(); iterator.hasNext();)
		{
			Cue cue = (Cue) iterator.next();
			
			HashMap cuemap = new HashMap();
			cuemap.put("captiontext", cue.getText().toString());
			cuemap.put("timecodestart", cue.getPosition());
			cuemap.put("alignment", cue.getAlignment());
			cuemap.put("timecodestart", Double.valueOf(cue.getPosition()));
			cuemap.put("timecodelength", cue.getSize());
			captions.add(cuemap);


		}
		cuetest.setValue("captions", captions);

		searcher.saveData(cuetest);
		
	
		cuetest = archive.getData("videotrack", cuetest.getId());
		
		captions = (Collection) cuetest.getValue("captions");

		assertNotNull(captions);
		assertEquals(8,captions.size() );
		//Map values = captions.
		
		
		
		
		
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
