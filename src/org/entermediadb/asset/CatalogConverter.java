/*
 * Created on Sep 15, 2004
 */
package org.entermediadb.asset;

import java.util.List;

import org.entermediadb.asset.scanner.Scanner;
import org.openedit.ModuleManager;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public abstract class CatalogConverter extends Scanner
{
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;

	/*
	 * protected AssetArchive fieldAssetArchive;
	 * 
	 * public File assetDirectory() { return new File(
	 * getStoreDirectory(),"assets"); }
	 */
	/**
	 * 
	 * @param inOutputAllAssets
	 */
	protected void saveOutput(MediaArchive inStore, List inOutputAllAssets) throws Exception
	{
		for (int i = 0; i < inOutputAllAssets.size(); i++)
		{
			Asset asset = (Asset) inOutputAllAssets.get(i);
			// asset
			if (asset.getOrdering() == -1)
			{
				asset.setOrdering(i);
			}
			inStore.saveAsset(asset);
			// inStore.getAssetArchive().saveBlankAssetDescription(asset);
		}
	}

	// This was used to break up a description into two parts
	// this it is not used anymore
	public String parseDescription(String inString)
	{
		int start = inString.indexOf("[[");
		int end = inString.indexOf("]]");
		if (start == -1 || end == -1)
		{
			return inString;
		}
		else
		{
			StringBuffer out = new StringBuffer(inString.substring(0, start));
			out.append(inString.substring(end + 2, inString.length()));
			return out.toString().trim();
		}
	}

	public String parseKeywords(String inString)
	{
		int start = inString.indexOf("[[");
		int end = inString.indexOf("]]");
		if (start == -1 || end == -1)
		{
			return null;
		}
		else
		{
			return inString.substring(start + 2, end).trim();
		}
	}

	public String extractId(String inName, boolean inAllowUnderstores)
	{
		inName = inName.trim();
		return PathUtilities.extractId(inName, inAllowUnderstores);
	}

	public String extractAssetId(String name)
	{
		name = name.replace(" ", "sp");
		name = name.replace("&", "amp");
		name = name.replace("(", "lp");
		name = name.replace(")", "rp");
		name = name.replace(".", "dot");
		name = name.replace("_", "und");
		name = name.replace("+", "plus");
		name = name.replace("-", "min");
		name = extractId(name, false);
		return name;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

}
