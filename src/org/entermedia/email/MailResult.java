package org.entermedia.email;

public class MailResult {
	
	protected boolean fieldSent;
	protected String fieldTrackingNumber;
	protected String fieldStatus;
	
	public boolean isSent() {
		return fieldSent;
	}
	public void setSent(boolean inSent) {
		fieldSent = inSent;
	}
	public String getTrackingNumber() {
		return fieldTrackingNumber;
	}
	public void setTrackingNumber(String inTrackingNumber) {
		fieldTrackingNumber = inTrackingNumber;
	}
	public String getStatus() {
		return fieldStatus;
	}
	public void setStatus(String inStatus) {
		fieldStatus = inStatus;
	}

}
