package org.entermediadb.asset;

/**
 * An implementation of {@link AssetPathFinder} that places assets in
 * subdirectories based on segments of their ID. For example, a asset with ID
 * <tt>abcdefghij</tt> might become <tt>abc/def/abcdefghij</tt>, if the
 * segment length is 3 and the maximum number of segments is 2.
 * 
 * @author Eric Galluzzo
 */
public class SegmentedAssetPathFinder implements AssetPathFinder
{
	protected boolean fieldGroupInTopCategory; // _
	protected int fSegmentLength = 0;
	protected String fieldPrefix;
	protected boolean fieldReverse;

	public boolean isReverse()
	{
		return fieldReverse;
	}

	public void setReverse(boolean inReverse)
	{
		fieldReverse = inReverse;
	}

	/**
	 * Creates a segmented asset path finder that will never use
	 * subdirectories.
	 */
	public SegmentedAssetPathFinder()
	{
	}

	/**
	 * Creates a segmented asset path finder that will create segments of the
	 * given length up to the given maximum.
	 * 
	 * @param inSegmentLength
	 *            The length of each path segment (e.g. 3 for
	 *            <tt>abc/def/...</tt>)
	 * @param inMaxSegments
	 *            The maximum number of segments
	 */
	public SegmentedAssetPathFinder(int inSegmentLength)
	{
		fSegmentLength = inSegmentLength;
	}

	public int getSegmentLength()
	{
		return fSegmentLength;
	}

	public void setSegmentLength(int inSegmentLength)
	{
		fSegmentLength = inSegmentLength;
	}

	public String idToPath(String inAssetId)
	{
		if (inAssetId == null)
		{
			return null;
		}

		if (getSegmentLength() == 0 && getPrefix() == null && !isGroupInTopCategory())
		{
			return inAssetId;
		}

		StringBuffer sb = new StringBuffer();
		if (getPrefix() != null)
		{
			sb.append(getPrefix());
		}
		if (isGroupInTopCategory())
		{
			int unds = inAssetId.lastIndexOf('_');
			if (unds > -1)
			{
				if (sb.length() > 0)
				{
					sb.append("/");
				}
				String group = inAssetId.substring(unds + 1);

				sb.append(group); // directory
				sb.append("/");

			}
		}
		if (getSegmentLength() > 0)
		{
			if (isReverse())
			{
				if (isGroupInTopCategory())
				{
					int unds = inAssetId.lastIndexOf('_');
					if (unds == -1)
					{
						unds = inAssetId.length();
					}
					String chunk = inAssetId.substring(unds - getSegmentLength(), unds);
					sb.append(chunk);
				}
				else
				{
					String chunk = inAssetId.substring(inAssetId.length() - getSegmentLength());
					sb.append(chunk);
				}
			}
			else
			{
				String chunk = inAssetId.substring(0, getSegmentLength());
				sb.append(chunk);
			}
			sb.append("/");
		}

		// for ( int segmentCount = 0; segmentCount < getMaxSegments();
		// segmentCount++ )
		// {
		// int index = segmentCount * getSegmentLength();
		// if ( index + getSegmentLength() >= inAssetId.length() )
		// {
		// break;
		// }
		// sb.append( inAssetId
		// .substring( index, index + getSegmentLength() ) );
		// sb.append( "/" );
		// }

		sb.append(inAssetId);

		return sb.toString();
	}

	public String getPrefix()
	{
		return fieldPrefix;
	}

	public void setPrefix(String inPrefix)
	{
		fieldPrefix = inPrefix;
	}

	public boolean isGroupInTopCategory()
	{
		return fieldGroupInTopCategory;
	}

	public void setGroupInTopCategory(boolean inGroupInTopCategory)
	{
		fieldGroupInTopCategory = inGroupInTopCategory;
	}
}
