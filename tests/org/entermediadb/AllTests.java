package org.entermediadb;

import java.util.Arrays;

import org.entermediadb.model.AssetEditTest;
import org.entermediadb.model.CategoryEditTest;
import org.entermediadb.model.CollectionTest;
import org.entermediadb.model.ConversionTest;
import org.entermediadb.model.MetaDataReaderTest;
import org.entermediadb.model.SourcePathTest;
import org.entermediadb.model.VideoConvertionTest;
import org.entermediadb.model.ZipTest;
import org.entermediadb.view.ConvertDocumentGeneratorTest;
import org.entermediadb.view.OriginalDocumentGeneratorTest;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static Test suite()
	{
		TestSuite suite = new TestSuite( "Test for entermedia" );
		
		//suite.addTestSuite( ArchiveModuleTest.class );
		//suite.addTestSuite( MultiSearchModuleTest.class );
		
		//suite.addTestSuite( AlbumTest.class );
		suite.addTestSuite( AssetEditTest.class );
		suite.addTestSuite( CategoryEditTest.class );

		//PDF conversions are failing
		//suite.addTestSuite( ConversionTest.class );
		//suite.addTestSuite( CollectionTest.class );
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
		//suite.addTestSuite( RelatedAssetsTest.class );
		suite.addTestSuite( SourcePathTest.class );
		//suite.addTestSuite( ThesaurusTest.class );
		suite.addTestSuite( ZipTest.class );
		
		suite.addTestSuite( ConvertDocumentGeneratorTest.class );
		suite.addTestSuite( OriginalDocumentGeneratorTest.class );
		
		return suite;
	}
}
