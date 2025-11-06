package org.entermediadb.ai.informatics;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.classify.ClassifyManager;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.MultiValued;

public abstract class InformaticsProcessor extends BaseAiManager 
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);

	public abstract void processInformaticsOnAssets(ScriptLogger inLog,MultiValued inConfig, Collection<MultiValued> assets );
	public abstract void processInformaticsOnEntities(ScriptLogger inLog,MultiValued inConfig, Collection<MultiValued> records );
	
	
	protected String loadImageContent(MultiValued inEntity)
	{
		boolean isDocPage = inEntity.get("entitydocument") != null;
		

		String base64EncodedString = null;
		if(isDocPage && inEntity.hasValue("pagenum") )
		{
			String parentasset = inEntity.get("parentasset");
			if(parentasset != null)
			{
				Asset parentAsset = getMediaArchive().getAsset(parentasset);
				//Do the conversion with page number in it
				Map params = new HashMap();
				params.put("pagenum",inEntity.get("pagenum") );
				ConvertResult result = getMediaArchive().getTranscodeTools().createOutputIfNeeded(null,params,parentAsset.getSourcePath(), "image3000x3000.webp"); 
				if( result.isOk() )
				{
					base64EncodedString = loadBase64Image(result.getOutput());
				}
			}
		}
		else
		{
			String primarymedia = inEntity.get("primarymedia");
			Asset inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
			if(inPrimaryAsset == null)
			{
				primarymedia = inEntity.get("primaryimage");
				inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
			}
			if(inPrimaryAsset != null)
			{
				base64EncodedString = loadBase64Image(inPrimaryAsset, "image3000x3000");
			}
		}
		return base64EncodedString;
	}

}
