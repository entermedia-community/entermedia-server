import org.entermediadb.asset.convert.ConversionManager
import org.entermediadb.asset.convert.ConvertInstructions
import org.openedit.Data

public void init()
{
	Data data = context.getPageValue("data");
	
	
	Collection types = data.getValues("inputtype");
	
	for(String type in types)
	{
		if("all".contentEquals(type))
		{
			continue;
		}
		ConversionManager manager = mediaarchive.getTranscodeTools().getManagerByRenderType(type);
		
		ConvertInstructions instructions = manager.createInstructions(null, data);
		instructions.setProperty("timeoffset", ConvertInstructions.NULL);
		
		instructions.setAssetSourcePath("junk");
		String name = instructions.getOutputFile().getName();
		data.setValue("generatedoutputfile",name);
	
		mediaarchive.getSearcher("convertpreset").saveData(data,user);
	}	
}

init();