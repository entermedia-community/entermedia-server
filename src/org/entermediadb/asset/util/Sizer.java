package org.entermediadb.asset.util;

import java.math.BigDecimal;

public class Sizer
{
	public String inEnglish(String inNum)
	{
		if(inNum == null || inNum.length() == 0)
		{
			return "";
		}
		return inEnglish(Long.parseLong(inNum));
	}
	public String inEnglish(long inNum)
	{
		return inEnglish(new Long(inNum));
	}
	
	public String inEnglish(double inNum){
		return inEnglish((long) inNum);
	}
	
	public String inEnglish(Long inNum)
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
		else if ( inNum.longValue() < 1024000000L)
		{
			double ks = (double)inNum.doubleValue()/1024000D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return ks  + " MB";
			
		}
		else
		{
			double ks = (double)inNum.doubleValue()/1024000000D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return ks  + "GB";
			
		}
		
		//return FileUtils.byteCountToDisplaySize(i.longValue());
	}
}
