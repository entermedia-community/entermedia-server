package org.openedit.entermedia.creator;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.util.PathUtilities;


public class ConvertInstructions
{
	protected int fieldPageNumber = 1;  //This is 1 based
	protected boolean fieldWatermark;
	//protected String fieldOutputExtension;
	//protected String fieldInputType;
	protected boolean fieldForce = false;
	//protected String fieldWatermarkPlacement; 
	protected int fieldRotation = 360;
	//protected String fieldInputSourcePath;
	//protected String fieldSourceFile;
	protected Map<String, String> fieldProperties;
	protected boolean fieldCrop;
	
	public boolean isForce() {
		return fieldForce;
	}

	public void addProperty(String inName, String inValue)
	{
		getProperties().put(inName, inValue);
	}
	
	
	public void setProperty(String inName, String inValue)
	{
		addProperty(inName, inValue);
	}
	public int intValue(String inName, int inDefault)
	{
		String val = get(inName);
		if( val == null)
		{
			return inDefault;
		}
		return Integer.parseInt(val);
	}
	public String get(String inName)
	{
		return getProperty(inName);
	}
	public String getProperty(String inName)
	{
		if( fieldProperties == null)
		{
			return null;
		}
		return getProperties().get(inName);
	}
	
	public void setForce(boolean force) {
		fieldForce = force;
	}

	public Dimension getMaxScaledSize()
	{
		String w = getProperty("prefwidth");
		String h = getProperty("prefheight");

		if (w != null && h != null) //both must be set
		{
			return new Dimension(Integer.parseInt(w), Integer.parseInt(h));
		}		
		return null;
	}

	public void setMaxScaledSize(Dimension inMaxScaledSize)
	{
		setProperty("prefwidth", inMaxScaledSize.width);
		setProperty("prefheight", inMaxScaledSize.height);
	}
	
	private void setProperty(String inName, int inVal)
	{
		setProperty(inName, String.valueOf(inVal));
		
	}

	public void setMaxScaledSize(int width, int height)
	{
		setProperty("prefwidth", width);
		setProperty("prefheight", height);
	}
	/**
	 * This starts at 1
	 * @return
	 */
	public int getPageNumber() {
		return fieldPageNumber;
	}

	public void setPageNumber(int inPageNumber) {
		fieldPageNumber = inPageNumber;
	}

	public void setPageNumber(String inProperty) {
		if( inProperty != null )
		{
			setPageNumber(Integer.parseInt(inProperty));
		}
	}

	public boolean isWatermark()
	{
		return fieldWatermark;
	}

	public void setWatermark(boolean inWatermark)
	{
		fieldWatermark = inWatermark;
	}

	public String getOutputExtension()
	{
		return getProperty("outputextension");
	}

	public void setOutputExtension(String inOutputExtension)
	{
		addProperty("outputextension", inOutputExtension);
	}
	public String getOutputPath()
	{
		return getProperty("outputpath");
	}

	public void setOutputPath(String inoutputpath)
	{
		addProperty("outputpath", inoutputpath);
	}
	
	
	public boolean doesConvert()
	{
		return (getMaxScaledSize() != null || getPageNumber() > 1 || getOutputExtension() != null);
	}
	
	
	public String createWatermarkPath(String inSourcePath)
	{
		String path = inSourcePath;

		if (getMaxScaledSize() != null) // If either is set then
		{
			path = path
					+ Math.round(getMaxScaledSize().getWidth())
					+ "x"
					+ Math.round(getMaxScaledSize().getHeight());
		}
		if (getPageNumber() > 1)
		{
			path = path + "page" + getPageNumber();
		}
		if (isWatermark())
		{
			path = path + "FPO";
		}
		if (getOutputExtension() != null)
		{
			path = path + "." + getOutputExtension();
		}
		else if (!path.endsWith(inSourcePath))
		{
			path = path + "." + PathUtilities.extractPageType(inSourcePath);
		}

		return path;
	}

	public String getInputExtension()
	{
		return getProperty("inputextension");
	}

	public void setInputExtension(String inInputExtension)
	{
		addProperty("inputextension", inInputExtension);
	}

	public String getWatermarkPlacement()
	{
		return getProperty("watermarkplacement");
	}

	public void setWatermarkPlacement(String inWatermarkPlacement)
	{
		addProperty("watermarkplacement", inWatermarkPlacement);
	}
	
	public boolean isCrop()
	{
		return fieldCrop;
	}

	public void setCrop(boolean inFieldCrop)
	{
		fieldCrop = inFieldCrop;
	}

	public int getRotation()
	{
		return fieldRotation;
	}

	public void setRotation(int inRotation)
	{
		fieldRotation = inRotation;
	}

	public String getAssetSourcePath()
	{
		return getProperty("assetsourcepath");
	}

	public void setAssetSourcePath(String inInputSourcePath)
	{
		addProperty("assetsourcepath", inInputSourcePath);
	}

	public String getInputPath()
	{
		return getProperty("inputpath");
	}

	public void setInputPath(String inInputPath)
	{
		addProperty("inputpath", inInputPath);
	}

	
	protected Map<String, String> getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new HashMap<String, String>();
		}
		return fieldProperties;
	}
	public void setProperties(Map<String, String> inProperties)
	{
		fieldProperties = inProperties;
	}

	public void addPageProperties(Page inPage) 
	{
		for (Iterator iterator = inPage.getPageSettings().getAllProperties().iterator(); iterator.hasNext();)
		{
			PageProperty type = (PageProperty) iterator.next();
			getProperties().put(type.getName(), type.getValue());
		}
	}

	public void addPageValues(Map inPageMap) 
	{
		for (Iterator iterator = inPageMap.keySet().iterator(); iterator.hasNext();)
		{
			String key = iterator.next().toString();
			Object value = inPageMap.get(key);
			if( value instanceof String || value instanceof Boolean)
			{
				getProperties().put(key, value.toString());
			}
		}

		
	}
}

