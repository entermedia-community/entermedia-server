package org.entermediadb.email;

import java.io.Serializable;

public class PostMailStatus implements Serializable {
	
	private static final long serialVersionUID = -2752936824544138538L;
	
	public static final String ID = "postmail_id";
	
	protected boolean fieldSent;
	protected String fieldId;
	protected String fieldStatus;
	
	public boolean isSent()
	{
		return fieldSent;
	}
	public void setSent(boolean inSent)
	{
		fieldSent = inSent;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String getStatus()
	{
		return fieldStatus;
	}
	public void setStatus(String inStatus)
	{
		fieldStatus = inStatus;
	}
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("id: ").append(fieldId).append(" [")
			.append("isSent: ").append(fieldSent).append(", ")
			.append("status: ").append(fieldStatus).append("]");
		return buf.toString();
	}
	

}
