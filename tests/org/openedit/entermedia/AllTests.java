package org.openedit.entermedia;

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.openedit.entermedia.controller.ArchiveModuleTest;
import org.openedit.entermedia.controller.MultiSearchModuleTest;
import org.openedit.entermedia.model.AssetEditTest;
import org.openedit.entermedia.model.CategoryEditTest;
import org.openedit.entermedia.model.ConvertionTest;
import org.openedit.entermedia.model.MetaDataReaderTest;
import org.openedit.entermedia.model.RelatedAssetsTest;
import org.openedit.entermedia.model.SourcePathTest;
import org.openedit.entermedia.model.ThesaurusTest;
import org.openedit.entermedia.model.VideoConvertionTest;
import org.openedit.entermedia.model.ZipTest;
import org.openedit.entermedia.view.ConvertDocumentGeneratorTest;
import org.openedit.entermedia.view.OriginalDocumentGeneratorTest;

import com.openedit.util.Exec;
import com.openedit.util.ExecResult;

public class AllTests {
	public static Test suite()
	{
		TestSuite suite = new TestSuite( "Test for entermedia" );
		
		suite.addTestSuite( ArchiveModuleTest.class );
		//suite.addTestSuite( MultiSearchModuleTest.class );
		
		//suite.addTestSuite( AlbumTest.class );
		suite.addTestSuite( AssetEditTest.class );
		suite.addTestSuite( CategoryEditTest.class );
		suite.addTestSuite( ConvertionTest.class );
		//suite.addTestSuite( XmlFileSearcherTest.class );
		boolean ffmpeg = true;
		try
		{
			ExecResult result = new Exec().runExec(Arrays.asList(new String[] {"ffmpeg", "-h"}));
			ffmpeg = result.isRunOk();
		}
		catch(Exception e)
		{
			ffmpeg = false;
		}
		
		if (ffmpeg)
		{
			suite.addTestSuite( VideoConvertionTest.class );
		}

		suite.addTestSuite( MetaDataReaderTest.class );
		suite.addTestSuite( RelatedAssetsTest.class );
		suite.addTestSuite( SourcePathTest.class );
		suite.addTestSuite( ThesaurusTest.class );
		suite.addTestSuite( ZipTest.class );
		
		suite.addTestSuite( ConvertDocumentGeneratorTest.class );
		suite.addTestSuite( OriginalDocumentGeneratorTest.class );
		
		return suite;
	}
}
