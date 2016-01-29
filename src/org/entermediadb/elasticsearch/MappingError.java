package org.entermediadb.elasticsearch;

public class MappingError
{
	protected String fieldSearchType;
	protected String fieldDetail;
	
	public String getSearchType()
	{
		return fieldSearchType;
	}
	public void setSearchType(String inSearchType)
	{
		fieldSearchType = inSearchType;
	}
	protected String fieldError;
	public String getError()
	{
		return fieldError;
	}
	public void setError(String inError)
	{
		fieldError = inError;
	}
	public String getDetail()
	{
		return fieldDetail;
	}
	public void setDetail(String inDetail)
	{
		fieldDetail = inDetail;
	}
	
}
