package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.convert.ConversionManager
import org.entermediadb.asset.convert.ConvertInstructions
import org.entermediadb.asset.scanner.Parse
import org.entermediadb.asset.scanner.PdfParser
import org.openedit.repository.ContentItem

public void init(){

	
	MediaArchive archive = archive;
	Asset asset = asset;
	ContentItem inputFile = input;
	
	
	
	ContentItem custom = archive.getContent( "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/sidecar.pdf");
	if( !custom.exists() )
	{
		ConversionManager c = archive.getTranscodeTools().getManagerByRenderType("document");
		ConvertInstructions instructions = c.createInstructions(inAsset);
		
		instructions.setAssetSourcePath(asset.getSourcePath());
		instructions.setAsset(asset);
		instructions.setOutputExtension("pdf");
		instructions.setInputFile(inputFile);
		instructions.setOutputFile(custom);
		c.createOutput(instructions);
	}
	PdfParser parser = new PdfParser();
	
	
	Parse results = parser.parse(custom.getInputStream()); //Do we deal with encoding?
	//We need to limit this size
	String fulltext = results.getText();
	asset.setValue("sidecartext", fulltext);
	
	
	
	
	
	
	
}
init();