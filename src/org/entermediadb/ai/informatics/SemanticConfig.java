package org.entermediadb.ai.informatics;

import java.util.Set;

import org.entermediadb.ai.knn.KMeansIndexer;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

public class SemanticConfig implements CatalogEnabled
{
	
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	public void setKMeansIndexer(KMeansIndexer inKMeansIndexer)
	{
		fieldKMeansIndexer = inKMeansIndexer;
	}
	boolean fieldSkipExistingRecords = true;
	
	public boolean isSkipExistingRecords()
	{
		return fieldSkipExistingRecords;
	}
	public void setSkipExistingRecords(boolean inUpdateExistingFace)
	{
		fieldSkipExistingRecords = inUpdateExistingFace;
	}
	public Set<String> getExistingEntityIds()
	{
		return fieldExistingEntityIds;
	}
	public void setExistingEntityIds(Set<String> inDataIds)
	{
		fieldExistingEntityIds = inDataIds;
	}
	protected Set<String> fieldExistingEntityIds;
	
	double fieldConfidenceLimit;
	public double getConfidenceLimit()
	{
		return fieldConfidenceLimit;
	}
	public void setConfidenceLimit(double inConfidenceLimit)
	{
		fieldConfidenceLimit = inConfidenceLimit;
	}
	
	public MultiValued fieldInstructionDetails;

	public MultiValued getInstructionDetails()
	{
		return fieldInstructionDetails;
	}
	public void setInstructionDetails(MultiValued inInstructicsField)
	{
		fieldInstructionDetails = inInstructicsField;
	}
	
protected KMeansIndexer fieldKMeansIndexer;
	
	public KMeansIndexer getKMeansIndexer()
	{
		if (fieldKMeansIndexer == null)
		{
			fieldKMeansIndexer = (KMeansIndexer)getModuleManager().getBean(getCatalogId(),"kMeansIndexer",false);
			if( fieldInstructionDetails == null)
			{
				throw new OpenEditException("Instructions not set");
			}
			fieldKMeansIndexer.loadSettings(getInstructionDetails());
			
		}
		return fieldKMeansIndexer;
	}
	
	public String getFieldName()
	{
		return getKMeansIndexer().getFieldName();
	}

	
	public String getSearchType()
	{
		return getKMeansIndexer().getSearchType();
	}
}
