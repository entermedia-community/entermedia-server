package org.entermedia.connectors.lucene;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetProcessor;
import org.openedit.repository.ContentItem;
import org.openedit.util.GenericsUtil;

import com.openedit.users.User;

public class IndexAllAssets extends AssetProcessor
{
	static final Log log = LogFactory.getLog(IndexAllAssets.class);
	protected IndexWriter fieldWriter;
	protected LuceneAssetIndexer fieldIndexer;
	protected Boolean fieldIndexFolders;
	protected MediaArchive fieldMediaArchive;
	protected Set<String> fieldSourcePaths = GenericsUtil.createSet();
	
	public LuceneAssetIndexer getIndexer()
	{
		return fieldIndexer;
	}

	public void setIndexer(LuceneAssetIndexer inIndexer)
	{
		fieldIndexer = inIndexer;
	}

	public AssetArchive getAssetArchive()
	{
		return getMediaArchive().getAssetArchive();
	}

	public Analyzer getAnalyzer()
	{
		return getIndexer().getAnalyzer();
	}

	public IndexWriter getWriter()
	{
		return fieldWriter;
	}

	public void setWriter(IndexWriter inWriter)
	{
		fieldWriter = inWriter;
	}

	public void processSourcePath(String inSourcePath)
	{
		IndexWriter writer = getWriter();
		
		//TODO: This is now done automatically
//		if (writer.ramSizeInBytes() > 1024 * 35000) // flush every
//		// 35 megs
//		{
//			log.info("Flush writer in reindex mem: " + writer.ramSizeInBytes() + " finished " + getExecCount() + " records ");
//			try
//			{
//				writer.commit();
//			}
//			catch (Exception e)
//			{
//				throw new OpenEditException(e);
//			}
//		}
		Asset asset = null;
		try
		{
			asset = getMediaArchive().getAssetArchive().getAssetBySourcePath(inSourcePath);
		}
		catch( Exception ex)
		{
			log.error("Could not read asset: " + inSourcePath + " continuing " + ex,ex);
			
		}

		if (asset != null)
		{
			// This should try to convert the Id into a path. The path will be null if the asset is not in the index.
			if(fieldSourcePaths.contains(asset.getSourcePath()))
			{
				return;
			}
			fieldSourcePaths.add(asset.getSourcePath());
			Document doc = getIndexer().createAssetDoc(asset, getMediaArchive().getAssetPropertyDetails());
			String id = asset.getId().toLowerCase();
			getIndexer().writeDoc(writer, id, doc, true);
			// remove it from mem
			getAssetArchive().clearAsset(asset);
			incrementCount();
		}
		else
		{
			log.info("Error loading asset: " + inSourcePath);
		}
	}

	public void processDir(ContentItem inContent)
	{
//		String path = makeSourcePath(inContent) + "/";
//		processSourcePath(path);
	}

	public void processFile(ContentItem inContent, User inUser)
	{
		String path = makeSourcePath(inContent);
		processSourcePath(path);
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

}
