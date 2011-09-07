package org.openedit.entermedia.publishing;

public class PublishResult
{
	protected String fieldErrorMessage;

	public String getErrorMessage()
	{
		return fieldErrorMessage;
	}

	public void setErrorMessage(String inErrorMessage)
	{
		fieldErrorMessage = inErrorMessage;
	}
	public boolean isError()
	{
		return fieldErrorMessage != null;
	}
}
