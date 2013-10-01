package org.entermedia.connectors.lucene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.openedit.data.PropertyDetail;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetProcessor;
import org.openedit.repository.ContentItem;
import org.openedit.util.GenericsUtil;

import com.openedit.OpenEditException;
import com.openedit.users.User;

public class IndexAllAssets extends AssetProcessor
{
	static final Log log = LogFactory.getLog(IndexAllAssets.class);
	protected IndexWriter fieldWriter;
	protected LuceneAssetIndexer fieldIndexer;
	protected Boolean fieldIndexFolders;
	protected MediaArchive fieldMediaArchive;
	protected Set<String> fieldSourcePaths = GenericsUtil.createSet();
	protected int logcount = 0;
	protected TaxonomyWriter fieldTaxonomyWriter;
	
	
	
	public TaxonomyWriter getTaxonomyWriter()
	{
		return fieldTaxonomyWriter;
	}

	public void setTaxonomyWriter(TaxonomyWriter inTaxonomyWriter)
	{
		fieldTaxonomyWriter = inTaxonomyWriter;
	}

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
			
			updateFacets(doc,  getTaxonomyWriter());

			getIndexer().writeDoc(writer, id, doc, true);
			// remove it from mem
			getAssetArchive().clearAsset(asset);
			incrementCount();
			logcount++;
			if( logcount == 1000 )
			{
				log.info("Reindex processed " + getExecCount() + " index updates so far");
				logcount=0;
			}
		}
		else
		{
			log.info("Error loading asset: " + inSourcePath);
		}
	}
	//TODO: MOVE THIS code and the version in  
	protected void updateFacets(Document inDoc,  TaxonomyWriter inTaxonomyWriter) throws OpenEditException
	{
		
		if (inTaxonomyWriter == null)
		{
			return;
		}

		List facetlist = getMediaArchive().getAssetPropertyDetails().getDetailsByProperty("filter", "true");
		ArrayList<CategoryPath> categorypaths = new ArrayList();
		for (Iterator iterator = facetlist.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String value = inDoc.get(detail.getId());

			if (detail.isFilter())
			{
				if (value != null)
				{
					ArrayList<String> vals = new ArrayList();
					vals.add(detail.getId());
					vals.add(value);
					String[] components = vals.toArray(new String[vals.size()]);
					categorypaths.add(new CategoryPath(components));
				}
			}

		}

		FacetFields facetFields = new FacetFields(inTaxonomyWriter);
		try{
			facetFields.addFields(inDoc, categorypaths);
		} catch(Exception e){
			throw new OpenEditException(e);
		}
		// do stuff

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
