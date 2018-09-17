package org.entermediadb.asset.convert;

import java.awt.Dimension;
import java.io.OutputStream;
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
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class ConvertInstructions
{
	protected MediaArchive fieldMediaArchive;
	protected Data fieldConvertPreset;
	protected int fieldPageNumber = 1; //This is 1 based
	protected int fieldRotation = 360;
	protected Map<String, String> fieldProperties;
	protected Collection<Data> fieldPresetParameters;
	protected Asset fieldAsset;
	protected ContentItem fieldOutputFile;
	protected ContentItem fieldInputFile;
	public static final String NULL = "null";
	protected boolean fieldStreaming = false;
	protected OutputStream fieldOutputStream;
	
	
	
	
	public OutputStream getOutputStream() {
		return fieldOutputStream;
	}

	public void setOutputStream(OutputStream inOutputStream) {
		fieldOutputStream = inOutputStream;
	}

	public boolean isStreaming() {
		return fieldStreaming;
	}

	public void setStreaming(boolean fieldStreaming) {
		this.fieldStreaming = fieldStreaming;
	}

	public ConvertInstructions copy(Data inNewPreset)
	{
		ConvertInstructions copy = new ConvertInstructions(fieldMediaArchive);
		copy.fieldPageNumber = fieldPageNumber;
		//copy.fieldRotation = fieldRotation;
		//copy.fieldProperties = fieldProperties;
		//copy.fieldPresetParameters = fieldPresetParameters;
		copy.fieldAsset = fieldAsset;
		//copy.fieldOutputFile = fieldOutputFile;
		copy.fieldInputFile = fieldInputFile;
		copy.setAssetSourcePath(getAssetSourcePath());
		copy.setAssetId(getAssetId());
		copy.loadPreset(inNewPreset);
		copy.setProperty("timeoffset", getTimeOffset());
		return copy;
	}
	
	public Collection<Data> getPresetParameters()
	{
		return fieldPresetParameters;
	}

	public void setPresetParameters(Collection<Data> inPresetParameters)
	{
		fieldPresetParameters = inPresetParameters;
	}
	
	public Data getConvertPreset()
	{
		if( fieldConvertPreset == null)
		{
			String presetdataid = get("presetdataid");
			if( presetdataid != null)
			{
				fieldConvertPreset = getMediaArchive().getData("convertpreset",presetdataid);
				loadPreset(fieldConvertPreset);
			}
//			if( fieldConvertPreset == null)
//			{
//				throw new OpenEditException("Convert preset not set");
//			}
//			else
//			{
//				String name = PathUtilities.extractFileName(getOutputPath()); 
//				fieldConvertPreset = getMediaArchive().getPresetManager().getPresetByOutputName(getMediaArchive(),name);
//			}
		}
		return fieldConvertPreset;
	}

	public void setConvertPreset(Data inConvertPreset)
	{
		fieldConvertPreset = inConvertPreset;
	}

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

	public ContentItem findOutputFile()
	{
		if(isStreaming()) {
			return null;
		}
		StringBuffer path = new StringBuffer();
		//legacy for people who want to keep their images in the old location
		String prefix = getProperty("pathprefix");
		if( prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(getMediaArchive().getCatalogHome());
			path.append("/generated/");
		}
		path.append(getAssetSourcePath());
		path.append("/");
	
		String postfix = getProperty("pathpostfix");
		if( postfix != null)
		{
			path.append(postfix);
		}
	//		String cachefilename = get("cachefilename");
	//		if( cachefilename != null)
	//		{
	//			path.append(cachefilename);
	//			return getMediaArchive().getContent( path.toString() );
	//		}
	
	//		if( "pdf".equals(getOutputExtension()) )
	//		{
	//			path.append("document");
	//		}
	//		else
	//		{
			
			String output = getProperty("outputfile");
			if( output != null && !output.isEmpty())
			{
				path.append( output );
			}
			else
			{
				String rendertype = getOutputRenderType();
				path.append( rendertype );
				
				//path.append(getCacheName()); //part of filename
		//		}
				if( rendertype.equals("image") || rendertype.equals("document") || rendertype.equals("video"))
				{
					Dimension maxScaledSize = getMaxScaledSize();
					if (maxScaledSize != null) // If either is set then
					{
						path.append(Math.round(maxScaledSize.getWidth()));
						path.append("x");
						path.append(Math.round(maxScaledSize.getHeight()));
					}
					if (getPageNumber() > 1)
					{
						path.append("page");
						path.append(getPageNumber());
					}
				}
				if(getProperty("timeoffset") != null)
				{
					path.append("offset");
					path.append(getProperty("timeoffset"));
				}
				if(isWatermark())
				{
					path.append("wm");
				}
				String frame = getProperty("frame");
				if( frame != null)
				{
					path.append("frame" + frame );
				}
		
				if(getProperty("colorspace") != null){
					path.append(getProperty("colorspace"));
				}
				if(isCrop() || Boolean.parseBoolean( getProperty("extent") ) )
				{
					path.append("cropped");
				}
				if (getOutputExtension() != null)
				{
					path.append("." + getOutputExtension());
				}
			}
		return getMediaArchive().getContent( path.toString() );
	}
	
	public ContentItem getOutputFile()
	{
		if( fieldOutputFile == null)
		{
			fieldOutputFile = findOutputFile();
		}
		return fieldOutputFile;
	}

	public void setOutputFile(ContentItem inOutputFile)
	{
		fieldOutputFile = inOutputFile;
	}


	//Should this be a stack?
	public ContentItem getInputFile()
	{
		return fieldInputFile;
	}

	public void setInputFile(ContentItem inInputFile)
	{
		fieldInputFile = inInputFile;
	}

	public Asset getAsset()
	{
		if (fieldAsset == null)
		{
			String assetid = getProperty("assetid");
			if (assetid != null)
			{
				fieldAsset = getMediaArchive().getAsset(assetid);
			}
			if (fieldAsset == null && getAssetSourcePath() != null)
			{
				fieldAsset = getMediaArchive().getAssetBySourcePath(getAssetSourcePath());
			}
		}
		return fieldAsset;
	}

	public void setAsset(Asset inAsset)
	{
		fieldAsset = inAsset;
	}

//	public Collection<Data> getParameters()
//	{
//		return fieldParameters;
//	}
//
//	public void setParameters(Collection<Data> inParameters)
//	{
//		fieldParameters = inParameters;
//	}

	public boolean isForce()
	{
		return Boolean.parseBoolean(getProperty("isforced"));
	}

	public void setProperty(String inName, String inValue)
	{
		if(inValue == null){
			inValue = NULL;

		}
		getProperties().put(inName, inValue);
		fieldOutputFile = null;
	}

	public int intValue(String inName, int inDefault)
	{
		String val = get(inName);
		if (val == null)
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
		
		
		
		if (fieldProperties != null)
		{
			String value = getProperties().get(inName);
			if(value == NULL){
				return null;
			}
			if( value != null)
			{
				return value;
			}
		}
		if( getPresetParameters() != null )
		{
			for (Iterator iterator = fieldPresetParameters.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String id = data.getName(); 
				if( id.equals(inName))
				{
					String value = data.get("value");
					if( value != null)
					{
						return value;
					}
				}
			}
		}
		if( fieldConvertPreset != null)
		{
			return getConvertPreset().get(inName);
		}
		return null;

	}

	public void setForce(boolean force)
	{
		setProperty("isforced", String.valueOf(force));
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
		if(inMaxScaledSize != null){
		setProperty("prefwidth", inMaxScaledSize.width);
		setProperty("prefheight", inMaxScaledSize.height);
		}
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
	 * 
	 * @return
	 */
	public int getPageNumber()
	{
		return fieldPageNumber;
	}

	public void setPageNumber(int inPageNumber)
	{
		fieldPageNumber = inPageNumber;
	}

	public void setPageNumber(String inProperty)
	{
		if (inProperty != null)
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
		if (type == null || inputtype == null)
		{
			return false;
		}
		if ((type.equals("png") || type.equals("gif")) && (inputtype.equals("gif") || inputtype.equals("png")))
		{
			return true;
		}
		return false;
	}

	public String getOutputExtension()
	{
		String ext = getProperty("outputextension");
		return ext;
	}

	public String getOutputPath()
	{
		return getOutputFile().getPath();
	}

	public boolean doesConvert()
	{
		return (getMaxScaledSize() != null || getPageNumber() > 1 || getOutputExtension() != null);
	}

	//	public String getInputExtension()
	//	{
	//		return getProperty("inputextension");
	//	}
	//
	//	public void setInputExtension(String inInputExtension)
	//	{
	//		addProperty("inputextension", inInputExtension);
	//	}

	public String getWatermarkPlacement()
	{
		return getProperty("watermarkplacement");
	}

	public void setWatermarkPlacement(String inWatermarkPlacement)
	{
		setProperty("watermarkplacement", inWatermarkPlacement);
	}

	public boolean isCrop()
	{
		return Boolean.parseBoolean(getProperty("crop"));
	}

	public void setCrop(boolean inFieldCrop)
	{
		setProperty("crop", String.valueOf(inFieldCrop));

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
		if (getAsset() != null)
		{
			return getAsset().getId();
		}
		return getProperty("assetid");
	}

	public void setAssetId(String inInd)
	{
		setProperty("assetid", inInd);
	}

	public String getAssetSourcePath()
	{
		if (fieldAsset != null)
		{
			return fieldAsset.getSourcePath();
		}
		return getProperty("assetsourcepath");
	}

	public void setAssetSourcePath(String inInputSourcePath)
	{
		setProperty("assetsourcepath", inInputSourcePath);
	}

	public String getInputPath()
	{
		return getProperty("inputpath");
	}

	public void setInputPath(String inInputPath)
	{
		setProperty("inputpath", inInputPath);
	}

	public Map<String, String> getProperties()
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
			if (value instanceof String || value instanceof Boolean)
			{
				getProperties().put(key, value.toString());
			}
		}
	}

	public void loadSettings(Map inSettings, Data inPreset)
	{
		loadSettings(inSettings);
		loadPreset(inPreset);
	}

	protected void loadPreset(Data inPreset)
	{
		setConvertPreset(inPreset);
		setPresetParameters(null);
		if( inPreset == null)
		{
			return;
		}
		String outputext = inPreset.get("outputextension");
		setOutputExtension(outputext);
		String presetdataid = get("presetdataid");
		if (presetdataid == null && inPreset != null)
		{
			presetdataid = inPreset.get("guid");
		}

		if (presetdataid != null)
		{
			Searcher paramsearcher = getMediaArchive().getSearcherManager().getSearcher(getMediaArchive().getCatalogId(), "presetparameter");
			Collection params = paramsearcher.query().exact("parameterdata", presetdataid).sort("id").search();
			if (params.size() > 0)
			{
				setPresetParameters(params);
			}
		}
		String exportname = inPreset.get("generatedoutputfile");		
		setProperty("cachefilename", exportname); //TODO: remove this
		//setProperty("cachefilename", inPreset.get("outputfile")); //TODO: remove this
		
		if( getOutputExtension() == null)
		{
			setOutputExtension(PathUtilities.extractPageType(exportname));
		}	
	}

	public void loadSettings(Map inSettings)
	{
		setSettings(inSettings);
		String pageString = getProperty("pagenum");
		// changed to take a request parameter.
		if (pageString != null && pageString.length() == 0)
		{
			pageString = null;
		}
		if (pageString != null)
		{
			setPageNumber(Integer.parseInt(pageString));
		}

		// Create temporary location for previews
		String w = getProperty("prefwidth");
		String h = getProperty("prefheight");

		if (w != null || h != null) // If either is set then set both
		{
			if (w == null || "".equals(w))
			{
				w = "10000";
			}
			if (h == null || "".equals(h))
			{
				h = "10000";
			}
			setMaxScaledSize(new Dimension(Integer.parseInt(w), Integer.parseInt(h)));
		}

		String crop = getProperty("crop");
		if (crop != null && Boolean.parseBoolean(crop))
		{
			setCrop(Boolean.parseBoolean(crop));
		}

		String watermark = getProperty("canforcewatermarkasset");
		if (watermark != null)
		{
			setWatermark(Boolean.valueOf(watermark));
		}

		String watermarkselected = getProperty("watermark");
		if (watermarkselected != null)
		{
			setWatermark(Boolean.valueOf(watermarkselected));
		}

	}

	protected void setSettings(Map inSettings)
	{
		if (inSettings != null)
		{
			Map settings = new HashMap();
			for (Iterator iterator = inSettings.keySet().iterator(); iterator.hasNext();)
			{
				String key = iterator.next().toString();
				Object value = inSettings.get(key);
				if (value instanceof String || value instanceof Boolean)
				{
					settings.put(key, String.valueOf(value));
				}
				else if( value instanceof String[] )
				{
					String[] vals = (String[])value;
					if( vals.length > 0)
					{
						settings.put(key, vals[0]);
					}
				}
			}
			getProperties().putAll(settings);
		}
	}

	public long getConversionTimeout()
	{
		long timeout = -1;
		if (getAsset() != null)
		{
			String fileformat = getAsset().get("fileformat");
			if (fileformat != null && !fileformat.isEmpty())
			{
				Data format = getMediaArchive().getData("fileformat", fileformat);
				if (format != null && format.get("conversiontimeout") != null)
				{
					try
					{
						timeout = Long.parseLong(format.get("conversiontimeout"));
					}
					catch (Exception e)
					{
					} //not handled
				}
			}
		}
		return timeout;
	}

	public ContentItem getOriginalDocument()
	{
		return getMediaArchive().getOriginalContent(getAsset());
	}

	public boolean isDocumentFormat()
	{
		String type = getMediaArchive().getMediaRenderType(getAsset());
		return "document".equals(type);
	}

	public String getTimeOffset()
	{
		return get("timeoffset");
	}

	public void setOutputExtension(String inType)
	{
		setProperty("outputextension", inType);
	}

	public String getOutputRenderType()
	{
		String type =  get("outputrendertype");
		if( type == null)
		{
			type = getMediaArchive().getMediaRenderType(getOutputExtension());
		}	
		return type;
	}
	public void setOutputRenderType(String inType)
	{
		setProperty("outputrendertype", inType);
	}

	
}
