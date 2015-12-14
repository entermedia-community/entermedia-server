package org.entermediadb.asset;

/**
 * An interface that maps a asset ID to a full path.
 * 
 * @author Eric Galluzzo
 */
public interface AssetPathFinder
{
	/**
	 * Converts the given ID to a relative path, not including any file
	 * extension.
	 * 
	 * @param inAssetId
	 *            The asset ID
	 * 
	 * @return The full path
	 */
	String idToPath(String inAssetId);
}
