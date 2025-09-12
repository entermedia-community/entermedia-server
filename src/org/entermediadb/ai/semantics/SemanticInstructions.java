package org.entermediadb.ai.semantics;

import java.util.Set;

import org.entermediadb.ai.knn.KMeansIndexer;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;

public class SemanticInstructions implements CatalogEnabled
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
	
	public MultiValued fieldSemanticField;

	public MultiValued getSemanticField()
	{
		return fieldSemanticField;
	}
	public void setSemanticField(MultiValued inSemanticField)
	{
		fieldSemanticField = inSemanticField;
	}
	
protected KMeansIndexer fieldKMeansIndexer;
	
	public KMeansIndexer getKMeansIndexer()
	{
		if (fieldKMeansIndexer == null)
		{
			fieldKMeansIndexer = (KMeansIndexer)getModuleManager().getBean(getCatalogId(),"kMeansIndexer",false);
			fieldKMeansIndexer.loadSettings(getSemanticField());
			
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
