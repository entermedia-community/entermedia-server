import org.entermediadb.asset.convert.ConversionManager
import org.entermediadb.asset.convert.ConvertInstructions
import org.openedit.Data

public void init()
{
	Data data = context.getPageValue("data");
	
	ConversionManager manager = mediaarchive.getTranscodeTools().getManagerByRenderType(data.get("inputtype"));
	
	ConvertInstructions instructions = manager.createInstructions(null, data);
	instructions.setAssetSourcePath("junk");
	String name = instructions.getOutputFile().getName();
	data.setValue("outputfile",name);

	mediaarchive.getSearcher("convertpreset").saveData(data,user);
}

init();