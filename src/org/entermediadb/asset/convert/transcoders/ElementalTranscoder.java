package org.entermediadb.asset.convert.transcoders;

import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.elemental.ElementalManager;
import org.openedit.Data;

public class ElementalTranscoder extends BaseTranscoder
{

	


	@Override
	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ElementalManager manager = (ElementalManager) inStructions.getMediaArchive().getBean("elementalManager");
		String jobid = manager.createJob(inStructions);
		ConvertResult result = new ConvertResult();
		result.setProperty("externalid", jobid);
		result.setComplete(false);
		result.setOk(true);
		// TODO Auto-generated method stub
		return null;
	}

	
	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions)
	{
		String jobid = inTask.get("jobid");
		ElementalManager manager = (ElementalManager) inStructions.getMediaArchive().getBean("elementalManager");
		return manager.updateJobStatus( inTask);
		
		
		
		// TODO Auto-generated method stub
	}
	
	
	
}
