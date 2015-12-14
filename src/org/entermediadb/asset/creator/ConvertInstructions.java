package org.entermediadb.asset.creator;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.util.PathUtilities;


public class ConvertInstructions
{
	MediaArchive fieldMediaArchive;
	
	public ConvertInstructions(MediaArchive inArchive)
	{
		setMediaArchive(inArchive);
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	protected int fieldPageNumber = 1;  //This is 1 based
	//protected String fieldOutputExtension;
	//protected String fieldInputType;
	//protected String fieldWatermarkPlacement; 
	protected int fieldRotation = 360;
	//protected String fieldInputSourcePath;
	//protected String fieldSourceFile;
	protected Map<String, String> fieldProperties;
	protected Collection<Data> fieldParameters;
	protected Asset fieldAsset;

	public Asset getAsset()
	{
		if( fieldAsset == null)
		{
			fieldAsset = getMediaArchive().getAsset(getAssetId());
		}
		return fieldAsset;
	}

	public void setAsset(Asset inAsset)
	{
		fieldAsset = inAsset;
	}

	public Collection<Data> getParameters()
	{
		return fieldParameters;
	}

	public void setParameters(Collection<Data> inParameters)
	{
		fieldParameters = inParameters;
	}

	public boolean isForce() 
	{
		return Boolean.parseBoolean( getProperty("isforced") );
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
		String value = getProperties().get(inName);
		return value;
		
	}
	
	public void setForce(boolean force)
	{
		setProperty("isforced",String.valueOf(force));
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
		return Boolean.valueOf(getProperty("watermark"));
	}

	public void setWatermark(boolean inWatermark)
	{
		setProperty("watermark", String.valueOf(inWatermark));
	}

	public boolean isTransparencyMaintained(String inputtype)
	{
		String type = getOutputExtension();
		if( type == null || inputtype == null)
		{
			return false;
		}
		if(( type.equals("png")|| type.equals("gif") ) && (inputtype.equals("gif") || inputtype.equals("png")) )
		{
			return true;
		}
		return false;
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
		return Boolean.parseBoolean( getProperty("iscrop") );
	}

	public void setCrop(boolean inFieldCrop)
	{
		setProperty("iscrop",String.valueOf(inFieldCrop));

	}

	public int getRotation()
	{
		return fieldRotation;
	}

	public void setRotation(int inRotation)
	{
		fieldRotation = inRotation;
	}

	public String getAssetId()
	{
		if( getAsset() != null)
		{
			return getAsset().getId();
		}
		return getProperty("assetid");
	}

	public void setAssetId(String inInd)
	{
		addProperty("assetid", inInd);
	}

	public String getAssetSourcePath()
	{
		if( getAsset() != null)
		{
			return getAsset().getSourcePath();
		}
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
	
	public void calculateOutputPath(Data inPreset)
	{
		StringBuffer outputpage = new StringBuffer();
		outputpage.append("/WEB-INF/data/" );
		outputpage.append(getMediaArchive().getCatalogId());
		outputpage.append("/generated/" );
		outputpage.append(getAssetSourcePath() );
		outputpage.append("/" );
		String output = inPreset.get("outputfile");
		int pagenumber = getPageNumber();
		if( pagenumber > 1 )
		{
			String name = PathUtilities.extractPageName(output);
			String ext = PathUtilities.extractPageType(output);
			output = name + "page" + pagenumber + "." + ext;
		}
		outputpage.append(output);
		setOutputPath(  outputpage.toString() );
	}

	public void loadSettings(Map inSettings, Data inPreset)
	{
		setSettings(inSettings);
		loadPreset(inPreset);
		calculateOutputPath(inPreset);
		
	}

	protected void loadPreset(Data inPreset)
	{
		String presetdataid = get("presetdataid");
		if( presetdataid == null && inPreset != null)
		{
			presetdataid = inPreset.get("guid");
		}
		if( presetdataid != null )
		{
			Searcher paramsearcher = getMediaArchive().getSearcherManager().getSearcher(getMediaArchive().getCatalogId(), "presetparameter" );
			Collection params = paramsearcher.fieldSearch("parameterdata",presetdataid,"id");
			if( params.size() > 0 )
			{
				setParameters(params);
				//Is this needed?
				for (Iterator iterator = params.iterator(); iterator.hasNext();)
				{
					Data row = (Data) iterator.next();
					setProperty(row.getName(), row.get("value"));
				}
			}
		}

	}

	protected void setSettings(Map inSettings)
	{
		if( inSettings != null)
		{
			Map settings = new HashMap();
			for (Iterator iterator = inSettings.keySet().iterator(); iterator.hasNext();)
			{
				String key = iterator.next().toString();
				Object value = inSettings.get(key);
				if( value instanceof String || value instanceof Boolean)
				{
					settings.put(key, String.valueOf(value));
				}
			}
			getProperties().putAll(settings);
		}	
	}

	
}

