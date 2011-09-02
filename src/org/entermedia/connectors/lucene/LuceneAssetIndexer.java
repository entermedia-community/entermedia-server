package org.entermedia.connectors.lucene;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.lucene.LuceneIndexer;
import org.openedit.data.lucene.RecordLookUpAnalyzer;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetSecurityArchive;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;

public class LuceneAssetIndexer extends LuceneIndexer
{
	static final Log log = LogFactory.getLog(LuceneAssetIndexer.class);
	protected Analyzer fieldAnalyzer;
	protected boolean usesSearchSecurity = false;
	protected MediaArchive fieldMediaArchive;
	protected File fieldRootDirectory;
	protected AssetSecurityArchive fieldAssetSecurityArchive;
	
	public File getRootDirectory()
	{
		return fieldRootDirectory;
	}

	public void setRootDirectory(File inRootDirectory)
	{
		fieldRootDirectory = inRootDirectory;
	}

	public boolean usesSearchSecurity()
	{
		return usesSearchSecurity;
	}

	public void setUsesSearchSecurity(boolean inUsesSearchSecurity)
	{
		usesSearchSecurity = inUsesSearchSecurity;
	}

	public Analyzer getAnalyzer()
	{
		return fieldAnalyzer;
	}

	public void setAnalyzer(Analyzer inAnalyzer)
	{
		fieldAnalyzer = inAnalyzer;
	}

	/**
	 * Builds a set of all categories (including parents) that include this asset
	 * @param inAsset
	 * @return
	 */
	protected Set buildCategorySet(Asset inAsset)
	{
		HashSet allCatalogs = new HashSet();
		Collection catalogs = inAsset.getCategories();
		allCatalogs.addAll(catalogs);
		for (Iterator iter = catalogs.iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			buildCategorySet(catalog, allCatalogs);
		}
		return allCatalogs;
	}

	/**
	 * Builds a set of all parent categories
	 * @param inCatalog
	 * @param inCatalogSet
	 */
	protected void buildCategorySet(Category inCatalog, Set inCatalogSet)
	{
		inCatalogSet.add(inCatalog);
		Category parent = inCatalog.getParentCategory();
		if (parent != null)
		{
			buildCategorySet(parent, inCatalogSet);
		}
	}
	public Document createAssetDoc(Asset asset, PropertyDetails inDetails)
	{
		Document doc = new Document();
		updateIndex(asset, doc, inDetails);

		return doc;
	}

	public void updateIndex(Data inData, Document doc, PropertyDetails inDetails)
	{
		Asset asset = (Asset)inData;
		String datatype = asset.getProperty("datatype");
		if (datatype == null)
		{
			datatype = "original"; //What is this for?
		}
		doc.add(new Field("datatype", datatype, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

		if(asset.getId() != null)
		{
			Field id = new Field("id", asset.getId(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS);
			doc.add(id); // Why is this tokenized? Guess so we can find lower
							// case versions
		}

		Field path = new Field("sourcepath", asset.getSourcePath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
		doc.add(path);

		String primaryfile = asset.getPrimaryFile();
		if (primaryfile != null)
		{
			Field imagename = new Field("primaryfile", primaryfile, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
			doc.add(imagename);
		}
		String fileformat = asset.getFileFormat();
		if(fileformat != null)
		{
			Field format = new Field("fileformat", fileformat, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
			doc.add(format);
		}
		
		if (asset.getCatalogId() == null)
		{
			asset.setCatalogId(getMediaArchive().getCatalogId());
		}
		Field catalogid = new Field("catalogid", asset.getCatalogId(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
		doc.add(catalogid);

		// this may be invalid field of -1 but we still need to add it for
		// the search to work
		if (asset.getOrdering() != -1)
		{
			doc.add(new Field("ordering", Integer.toString(asset.getOrdering()), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
		
		String tagString = getTagString(asset);
		doc.add(new Field("keywords", tagString, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));

		Set catalogs = buildCategorySet(asset);
		populateDescription(doc, asset, inDetails, catalogs, tagString);

		/*
		 * 'category' contains all categories, including parents 
		 */
		populateJoinData("category", doc, catalogs, "id", true);
		
		
		Searcher searcher = getSearcherManager().getSearcher(asset.getCatalogId(),"assetalbums");
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("assetid", asset.getId());
		HitTracker tracker = searcher.search(query);
		populateJoinData("album", doc, tracker, "albumid", true);
		
		// populateSecurity(doc, asset, catalogs);
		if (usesSearchSecurity())
		{
			populatePermission(doc, asset, "viewasset");
		}
		
		/*
		 * 'exact-category' only contains categories that we immediately belong to
		 */
		populateExactCategory(doc, asset);
		
		populateProperties(doc, asset, inDetails);
	}
/*
	protected void addAttachment(Document inDoc, Asset inAsset, String inType, String orig,String origtype)
	{
		String value = inAsset.getAttachmentByType(inType);
		if( value == null && origtype != null && inType.startsWith(origtype))
		{
			value = orig;
		}
		if( value != null)
		{
			Field path = new Field(inType, value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
			inDoc.add(path);
		}
	}
*/
	public void populateAsset(IndexWriter writer, Asset asset, boolean add, PropertyDetails inDetails) throws OpenEditException
	{
		Document doc = createAssetDoc(asset, inDetails);
		writeDoc(writer, asset.getId().toLowerCase(), doc, add);
	}

	protected void populatePermission(Document inDoc, List inAccessList, String inPermission)
	{
		if (inAccessList.size() == 0)
		{
			inAccessList.add("true");
		}

		// permission is "viewasset" for earch security
		StringBuffer buffer = new StringBuffer();

		for (Iterator iterator = inAccessList.iterator(); iterator.hasNext();)
		{
			String allow = (String) iterator.next();
			buffer.append(" ");
			buffer.append(allow);
		}
		inDoc.add(new Field(inPermission, buffer.toString(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
	}

//	protected void populatePermission(Document inDoc, Page inPage, String inPermission, Asset inAsset) throws OpenEditException
//	{
//		List add = getAssetSecurityArchive().getAccessList(getMediaArchive(), inPage, inAsset);
//		populatePermission(inDoc, add, inPermission);
//	}

	protected void populatePermission(Document inDoc, Asset inAsset, String inPermission) throws OpenEditException
	{
		if(getAssetSecurityArchive() == null)
		{
			return;
		}
		List add = getAssetSecurityArchive().getAccessList(getMediaArchive(), inAsset);
		populatePermission(inDoc, add, inPermission);
	}

	protected void populateProperties(Document inDoc, Asset inAsset, PropertyDetails inDetails) throws OpenEditException
	{
		List list = inDetails.findIndexProperties();
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			PropertyDetail det = (PropertyDetail) iter.next();
			String detid = det.getId();
			if (detid.equals("name") || detid.equals("description") || detid.equals("primaryfile") ||
					detid.equals("id") || detid.equals("category") || detid.equals("datatype"))
			{
				// These are already hardcoded
				continue;
			}
			if (det.getDataType() != null && det.getDataType().endsWith("join"))
			{
				// get the values from another list
				String id = det.getExternalId();
				if (id == null)
				{
					id = detid;
				}
				String table = id.substring(0, id.indexOf('.'));
				String field = id.substring(id.indexOf('.') + 1);
				Searcher searcher = getSearcherManager().getSearcher(det.getCatalogId(getCatalogId()), table);
				// get this data assetstates.stateid
				// search by asset id
				Collection values = searcher.fieldSearch("assetid", inAsset.getId());
				if (values != null)
				{
					if (det.getDataType().startsWith("date"))
					{
						populateDateJoin(det, inDoc, values, field, true);
					}
					else if ( det.getDataType().startsWith("total"))
					{
						populateJoinDataCount(det.getId(), inDoc, values);
					}
					else
					{
						populateJoinData(det, inDoc, values, field);
					}
				}
				continue;
			}
			String prop = inAsset.get(detid);
			if (prop != null && prop.length() > 0)
			{
				if (det.isDate())
				{
					String date = inAsset.getProperty(detid);
					if (date != null && date.length() > 0)
					{
						Date realdate = DateStorageUtil.getStorageUtil().parseFromStorage(date);
						if( realdate != null)
						{
							prop = DateTools.dateToString(realdate, Resolution.SECOND);
							inDoc.add(new Field(detid, prop, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
						}
					}
				}
				else if (det.isDataType("double") || det.isDataType("number") || det.isDataType("long"))
				{
					try
					{
						String sortable = getNumberUtils().double2sortableStr(prop);
						inDoc.add(new Field(detid + "_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
					finally
					{
						inDoc.add(new Field(detid, prop, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
				}
				else if (det.isStored())
				{
					inDoc.add(new Field(detid, prop, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
				}
				else
				{
					inDoc.add(new Field(detid, prop, Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
				}
			}
			
			if(det.isDataType("position")){
				String lat = inAsset.get(detid + "_lat");
				String lng = inAsset.get(detid + "_lng");
				
				try
				{
					if(lat != null && lng != null){
					String sortable = getNumberUtils().double2sortableStr(lat);
					inDoc.add(new Field(detid + "_lat_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					inDoc.add(new Field(detid + "_lat", lat, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					
					
					
					sortable = getNumberUtils().double2sortableStr(lng);
					inDoc.add(new Field(detid + "_lng_sortable", sortable, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					inDoc.add(new Field(detid + "_lng", lng, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					inDoc.add(new Field(detid + "_available", "true", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
					else{
						inDoc.add(new Field(detid + "_available", "false", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
					}
					
					
					
				}
				catch (Exception e){
					log.info("no valid position data found");
				}
				finally
				{
				
					//	inDoc.add(new Field(detid, prop, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}
			
			
			
			else if (det.isDataType("permission"))
			{
				populatePermission(inDoc, inAsset, detid);
			}
			else if (det.isDataType("boolean"))
			{
				inDoc.add(new Field(det.getId(), "false", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}

		}

	}


	protected void populateExactCategory(Document doc, Asset item)
	{
		// the idea here is to add a field that allows you to search for
		// assets in a category WITHOUT sub category assets showing.
		StringBuffer buffer = new StringBuffer();
		for (Iterator iter = item.getCategories().iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			buffer.append(catalog.getId());
			buffer.append(" ");
		}

		if (buffer.length() > 0)
		{
			doc.add(new Field("category-exact", buffer.toString(), Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
		}
		/*
		 * Not used any more if ( item.getDepartment() != null) { doc.add( new
		 * Field("department", item.getDepartment(), Field.Store.YES,
		 * Field.Index.ANALYZED_NO_NORMS)); }
		 */
	}

	protected void populateDescription(Document doc, Asset asset, PropertyDetails inDetails, Set inCategories, String inTagString)
	{
		if (asset.getName() != null)
		{
			// This cannot be used in sorts since it is TOKENIZED. For sorts use
			doc.add(new Field("name", asset.getName(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
			doc.add(new Field("name_sortable", asset.getName().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
		String htmlPath = asset.getSourcePath() + ".html";
		// Low level reading in of text
		StringBuffer fullDesc = new StringBuffer();
		fullDesc.append(asset.getName());
		fullDesc.append(' ');
		
		fullDesc.append(asset.getFileFormat());
		fullDesc.append(' ');
		
		fullDesc.append(asset.getId());
		fullDesc.append(' ');
		fullDesc.append(inTagString);
		populateKeywords(fullDesc, asset, inDetails);
		// add a bunch of stuff to the full text field
		//never need this anymore
		/*
		File descriptionFile = new File(getRootDirectory(), "/" + getCatalogId() + "/assets/" + htmlPath);
		if (descriptionFile.exists() || descriptionFile.length() > 0)
		{
			FileReader descread = null;
			try
			{
				descread = new FileReader(descriptionFile);
				StringWriter out = new StringWriter();
				new OutputFiller().fill(descread, out);
				fullDesc.append(out.toString());
			}
			catch (IOException ex)
			{
				throw new OpenEditException(ex);
			}
			finally
			{
				FileUtils.safeClose(descread);
			}
		}
		*/
		fullDesc.append(' ');
		for (Iterator iter = inCategories.iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			fullDesc.append(cat.getName());
			fullDesc.append(' ');
		}

		String[] dirs = asset.getSourcePath().split("/");

		for (int i = 0; i < dirs.length; i++)
		{
			fullDesc.append(dirs[i]);
			fullDesc.append(' ');
		}

//		try
		{
			String result = fullDesc.toString();//fixInvalidCharacters(fullDesc.toString());
			doc.add(new Field("description", result, Field.Store.NO, Field.Index.ANALYZED));
		}
//		catch (IOException ex)
//		{
//			throw new OpenEditException(ex);
//		}
	}

	/**
	 * This is here to help the stemmer handle weird cases of words For example:
	 * century21 should contain both centuri and century21 in the search index
	 * 
	 * @param inString
	 * @return
	 * @throws IOException
	 
	protected String fixInvalidCharacters(String inString) throws IOException
	{
		StringBuffer fixed = new StringBuffer(inString.length() + 20);
		RecordLookUpAnalyzer analyser = new RecordLookUpAnalyzer();
		TokenStream stream = analyser.tokenStream("description", new StringReader(inString));
		boolean hasnext = stream.incrementToken();
		while (hasnext)
		{
			char[] text = stream. token.termBuffer();
			if (text.length > 3)
			{
				// loop over all the words until we find an invalid one
				for (int i = 0; i < text.length; i++)
				{
					fixed.append(text[i]);
					// Checking for Y in the middle of words: harleydavidson
					// will now
					// index as harley davidson.
					if (text[i] == 'y')
					{
						break;
					}
				}
				fixed.append(' ');
			}
			// Always put the original back in there
			fixed.append(text);
			fixed.append(' ');
			token = stream.next();
		}
		return fixed.toString();
	}
	*/

	protected void populateKeywords(StringBuffer inFullDesc, Asset inAsset, PropertyDetails inDetails)
	{
		for (Iterator iter = inDetails.getDetails().iterator(); iter.hasNext();)
		{
			PropertyDetail det = (PropertyDetail) iter.next();
			if (det.isKeyword())
			{
				String prop = inAsset.getProperty(det.getId());
				if (prop != null)
				{
					inFullDesc.append(prop);
					inFullDesc.append(' ');
				}
			}
		}
	}
	
	protected String getTagString(Asset inAsset)
	{
		StringBuffer buffer = new StringBuffer();
		if (inAsset.hasKeywords())
		{
			for (Iterator iter = inAsset.getKeywords().iterator(); iter.hasNext();)
			{
				String desc = (String) iter.next();
				desc = desc.replace('/', ' '); // Is this needed?
				desc = desc.replace('\\', ' ');
				buffer.append(desc);
				buffer.append(' ');
			}
		}		
		return buffer.toString();
	}

	public String pad(String inValue)
	{

		// return getDecimalFormatter().format(inShortprice);

		String all = "0000000000000" + inValue;
		String cut = all.substring(all.length() - 10); // 10 is the max width
		// of integers
		return cut;
	}

	public void writeDoc(IndexWriter writer, String inId, Document doc, boolean add)
	{
		try
		{
			if (add)
			{
				writer.addDocument(doc, getAnalyzer());
			}
			else
			{
				Term term = new Term("id", inId);
				writer.updateDocument(term, doc, getAnalyzer());
			}
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	protected String getCatalogId()
	{
		return getMediaArchive().getCatalogId();
	}

	public AssetSecurityArchive getAssetSecurityArchive()
	{
		return fieldAssetSecurityArchive;
	}

	public void setAssetSecurityArchive(AssetSecurityArchive inAssetSecurityArchive)
	{
		fieldAssetSecurityArchive = inAssetSecurityArchive;
	}
}
