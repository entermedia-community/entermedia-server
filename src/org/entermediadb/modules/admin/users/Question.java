/*
 * Created on Feb 21, 2006
 */
package org.entermediadb.modules.admin.users;

public class Question
{	
	protected String fieldDescription;
	protected String fieldId;
	protected String fieldAnswer;
	public String getAnswer()
	{
		return fieldAnswer;
	}
	public void setAnswer(String inAnswer)
	{
		fieldAnswer = inAnswer;
	}
	public String getDescription()
	{
		return fieldDescription;
	}
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public boolean checkAnswer(String inAnswer)
	{
		if ( getAnswer().equalsIgnoreCase( inAnswer) )
		{
			return true;
		}
		return false;
	}

}
