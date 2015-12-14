/*
 * Created on Nov 8, 2006
 */
package org.entermediadb.modules.workflow;

public class Level
{
	protected int fieldId;
	protected String fieldName;
	public int getId()
	{
		return fieldId;
	}
	public void setId(int inId)
	{
		fieldId = inId;
	}
	public String getName()
	{
		return fieldName;
	}
	public void setName(String inName)
	{
		fieldName = inName;
	}
}
