package org.entermediadb.ai.classify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;

public class RenderValues
{
	protected MediaArchive fieldMediaArchive;
	
	 public MultiValued getData()
	{
		return inData;
	}

	public void setData(MultiValued inInData)
	{
		inData = inInData;
	}

	public Collection<PropertyDetail> getInFields()
	{
		return inFields;
	}

	public void setInFields(Collection<PropertyDetail> inInFields)
	{
		inFields = inInFields;
	}

	public boolean isIncludeasset()
	{
		return includeasset;
	}

	public void setIncludeasset(boolean inIncludeasset)
	{
		includeasset = inIncludeasset;
	}

	 MultiValued inData;
	 Collection<PropertyDetail> inFields;
	 boolean includeasset; 
	
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	public String renderAll()
	{
		String render = render(true);
		return render;
	}
	
	public String render(boolean includeasset)
	{
		StringBuilder output = new StringBuilder();
		for (Iterator iterator = getInFields().iterator(); iterator.hasNext();)
		{
			PropertyDetail field = (PropertyDetail) iterator.next();
			
			if( field.isDate() || !getData().hasValue(field.getId()))
			{
				continue;
			}
			output.append(field.getName());
			output.append(": ");
			renderField(field, getData(), output);
			if( iterator.hasNext() )
			{
				output.append(",");
			}
			output.append("\n");
		}
		if( includeasset && !(getData() instanceof Asset) )
		{
			String assetid = inData.get("primarymedia");
			if( assetid == null)
			{
				assetid = inData.get("primaryimage");
			}
			
			Asset primaryasset = getMediaArchive().getAsset(assetid);
			if (primaryasset != null)
			{
				List<String> fields = Arrays.asList("longcaption", "keywordsai", "semantictopics","headline");
				Searcher assetsearcher = getMediaArchive().getAssetSearcher();
				for (Iterator iterator = fields.iterator(); iterator.hasNext();)
				{
					String prop = (String) iterator.next();
					PropertyDetail field = assetsearcher.getDetail(prop); 
					if( !primaryasset.hasValue(field.getId()))
					{
						continue;
					}
					output.append(field.getName());
					output.append(": ");
					renderField(field, primaryasset, output);
					if( iterator.hasNext() )
					{
						output.append(",");
					}
					output.append("\n");
				}
			}
		}
		return output.toString();
	}

	protected void renderField(PropertyDetail field, MultiValued inData, StringBuilder output)
	{
		if(field.isList())
		{
			for (Iterator iterator2 = getMediaArchive().getValueList(field, inData ).iterator(); iterator2.hasNext();)
			{
				Data type = (Data)iterator2.next();
				output.append(type.getName());
				if(iterator2.hasNext() )
				{
					output.append(",");
				}
			}
		}
		else if(field.isBoolean())
		{
			if(getData().getBoolean(field.getId()))
			{
				output.append("Yes");
			}
			else
			{
				output.append("No");
			}
		}
		else if(field.isDate())
		{
			// SKIP
		}
		else
		{
			String value = inData.get(field.getId());
			output.append(value);
		}
	}
}
