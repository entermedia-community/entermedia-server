package org.entermedia.connectors.lucene;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.openedit.entermedia.search.AssetProcessor;
import org.openedit.repository.ContentItem;
import org.openedit.util.GenericsUtil;

public abstract class IndexAllAssets extends AssetProcessor
{
	static final Log log = LogFactory.getLog(IndexAllAssets.class);
	//protected Boolean fieldIndexFolders;
	protected Set<String> fieldSourcePaths = GenericsUtil.createSet();
	protected int logcount = 0;


	public void processDir(ContentItem inContent)
	{
//		String path = makeSourcePath(inContent) + "/";
//		processSourcePath(path);
	}

}
