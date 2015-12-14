/*
 * Created on Feb 18, 2004
 *
 */
package org.entermediadb.modules.update;

/**
 * @author dbrown
 *
 */
public class ProductVersion
{
	protected String fieldVersion;
	protected String fieldJar;
	
	public ProductVersion( String inVersion )
	{
		fieldVersion = inVersion;
	}

	public String getVersion()
	{
		return fieldVersion;
	}

	public void setVersion( String inString )
	{
		fieldVersion = inString;
	}

	public String toString()
	{
		if( fieldVersion == null)
		{
			return "dev";
		}
		return fieldVersion;
	}
	public String getJar()
	{
		return fieldJar;
	}
	public void setJar(String inJar)
	{
		fieldJar = inJar;
	}
}
