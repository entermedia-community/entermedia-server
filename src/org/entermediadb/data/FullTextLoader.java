package org.entermediadb.data;

import org.openedit.Data;

public interface FullTextLoader
{

	public String getFulltext(Data inSearchHitData);
	public String getFulltext(Data inSearchHitData, String type);

}
