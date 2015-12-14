/*
 * Created on Feb 6, 2005
 */
package org.entermediadb.links;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;

/**
 * @author cburkey
 *
 */
public class FileSize
{
	//all size stores in 16 bit bytes
	//a 1K is 1024 bytes
	
	protected Map fieldMapOfSizesInBytes;
	protected PageManager fieldPageManager;
	protected File fieldRoot;
	public String inEnglish(long inNum) throws OpenEditException
	{
		return inEnglish(new Long( inNum));
	}
	public String inEnglish(Long inNum) throws OpenEditException
	{
		if ( inNum == null)
		{
			return "";
		}
		if ( inNum.longValue() < 1024)
		{
			return inNum.toString() + " bytes";
		}
		else if ( inNum.longValue() < 1024000)
		{
			double ks = (double)inNum.doubleValue()/1024D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return ks + " KB";
		}
		else if ( inNum.longValue() < 1024000000)
		{
			double ks = (double)inNum.doubleValue()/1024000000D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return ks  + " MB";
			
		}
		else
		{
			double ks = (double)inNum.doubleValue()/1024000000000D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return ks  + " GB";
			
		}
		
		//return FileUtils.byteCountToDisplaySize(i.longValue());
	}
	public String inEnglish(String inPath)  throws OpenEditException
	{
		Long i = getSize(inPath);
		return inEnglish(i);
	}
	
	public String stringToEnglish(String size) throws OpenEditException
	{
		long i = Long.parseLong(size);
		return inEnglish(new Long(i));
	}
	
	public String inKs(String inPath) throws OpenEditException
	{
		//look up this file and check the size
		Long i = getSize(inPath);
		if ( i == null)
		{
			return "";
		}
		double ks = (double)i.intValue()/1024D;
		//need to trim this off
/*		ks = ks*100;
		ks = Math.round(ks);
		ks = ks/100;
*/
		String value = String.valueOf(Math.round(ks));
/*		if ( value.indexOf(".") > 2)
		{
			value = value.substring(0,value.indexOf(".") + 2);
		}
		*/
		return value;
	}
	public String inBytes( String inPath ) throws OpenEditException
	{
		Long i = getSize(inPath);
		if( i == null)
		{
			return "missing";
		}
		return i.toString();
	}
	public Long getSize(String inPath) throws OpenEditException
	{
		Long i = (Long)getMapOfSizesInBytes().get(inPath);
		if( i == null)
		{
			Page page = getPageManager().getPage(inPath);
			if ( page.exists())
			{
				long length = 0;
				if( page.isFolder() )
				{
					File root = new File( getRoot(), page.getContentItem().getPath() );
					File[] children = root.listFiles();
					for (int j = 0; j < children.length; j++)
					{
						if( !children[j].isDirectory())
						{
							length = length + children[j].length();
						}
					}
				}
				else
				{
					length = page.getContentItem().getLength();
				}
				i = new Long(length);
				getMapOfSizesInBytes().put(inPath,i);
			}
		}
		return i;
	}
	
	protected Map getMapOfSizesInBytes()
	{
		if ( fieldMapOfSizesInBytes == null)
		{
			fieldMapOfSizesInBytes = new HashMap();
		}
		return fieldMapOfSizesInBytes;
	}
	protected PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	protected File getRoot()
	{
		return fieldRoot;
	}

	protected void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
}
