package org.entermediadb.ai.informatics;


import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.MultiValued;


public class InformaticsContext extends AgentContext 
{
	private static final Log log = LogFactory.getLog(InformaticsContext.class);
	
	public InformaticsContext(AgentContext inContext)
	{
		super(inContext);
	}
	
	 public InformaticsContext()
	{
		// TODO Auto-generated constructor stub
	}

	 public InformaticsContext getParentInformaticContext()
	 {
		 if(fieldParentContext != null && fieldParentContext instanceof InformaticsContext)
		 {
			 return (InformaticsContext)fieldParentContext;
		 }
		 return null;
	 }
	
	protected Collection<MultiValued> fieldRecordsToProcess;

	public Collection<MultiValued> getRecordsToProcess()
	{
		if( fieldRecordsToProcess == null)
		{
			InformaticsContext parent = getParentInformaticContext();
			if( parent != null)
			{
				return parent.getRecordsToProcess();
			}
		}
		return fieldRecordsToProcess;
	}

	public void setRecordsToProcess(Collection<MultiValued> inRecordsToProcess)
	{
		fieldRecordsToProcess = inRecordsToProcess;
	}

	public Collection<MultiValued> getAssetsToProcess()
	{
		if( fieldAssetsToProcess == null)
		{
			InformaticsContext parent = getParentInformaticContext();
			if( parent != null)
			{
				return parent.getAssetsToProcess();
			}
		}

		return fieldAssetsToProcess;
	}

	public void setAssetsToProcess(Collection<MultiValued> inAssetsToProcess)
	{
		fieldAssetsToProcess = inAssetsToProcess;
	}
	protected Collection<MultiValued> fieldAssetsToProcess;

	
}
