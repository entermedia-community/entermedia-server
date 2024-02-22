package org.entermediadb.asset.publishing;

public class PublishResult
{
	protected String fieldErrorMessage;
	protected String fieldCompletedMessage;
	protected boolean fieldComplete;
	protected boolean fieldPending;

	protected boolean fieldReadyToPublish;
	
	public boolean isReadyToPublish()
	{
		return fieldReadyToPublish;
	}

	public void setReadyToPublish(boolean inReadyToPublish)
	{
		fieldReadyToPublish = inReadyToPublish;
	}

	public boolean isPending()
	{
		return fieldPending;
	}

	public void setPending(boolean inPending)
	{
		fieldPending = inPending;
	}

	public boolean isComplete()
	{
		return fieldComplete;
	}

	public void setComplete(boolean inComplete)
	{
		fieldComplete = inComplete;
	}
	
	public void setCompleteMessage( String inCompleteMessage )
	{
		fieldCompletedMessage = inCompleteMessage;
	}
	
	public String getCompleteMessage()
	{
		if (fieldCompletedMessage == null) {
			fieldCompletedMessage = "";
		}
		return fieldCompletedMessage;
	}

	public String getErrorMessage()
	{
		if (fieldErrorMessage == null) {
			fieldErrorMessage = "";
		}
		return fieldErrorMessage;
	}

	public void setErrorMessage(String inErrorMessage)
	{
		fieldErrorMessage = inErrorMessage;
	}
	public void appendCompleteMessage(String inErrorMessage) {
		if (fieldCompletedMessage != null) {
			fieldCompletedMessage += inErrorMessage;
		} else {
			fieldCompletedMessage = inErrorMessage;
		}
	}
	public void appendErrorMessage(String inErrorMessage) {
		if (fieldErrorMessage != null) {
			fieldErrorMessage += inErrorMessage;
		} else {
			fieldErrorMessage = inErrorMessage;
		}
	}
	public boolean isError()
	{
		if (fieldErrorMessage != null) {
			return true;
		} else {
			return false;
		}
	}
}
