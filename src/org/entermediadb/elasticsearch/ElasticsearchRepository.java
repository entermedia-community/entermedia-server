package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.repository.filesystem.StringItem;

public class ElasticsearchRepository extends FileRepository {
	private static final Log log = LogFactory.getLog(StringItem.class);

	protected SearcherManager fieldSearcherManager;
	protected ElasticPageSearcher fieldElasticPageSearcher;
	
	
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public ElasticPageSearcher getElasticPageSearcher() {
		return (ElasticPageSearcher) getSearcherManager().getSearcher("system", "page");
	}

	public void setElasticPageSearcher(ElasticPageSearcher inElasticPageSearcher) {
		fieldElasticPageSearcher = inElasticPageSearcher;
	}

	@Override
	public ContentItem get(String inPath) throws RepositoryException {
		Data page = (Data) getElasticPageSearcher().searchByField("sourcepath", inPath);
		ElasticContentItem elasticitem = null;
		if(page != null){
			 elasticitem = new ElasticContentItem();
			 elasticitem.setElasticData(elasticitem);
		}
		
		
		//get it from the super class and create one on the fly?  IE, if the pages already exist...
		if (page == null) {
			ContentItem item = super.get(inPath);
			if (item != null) {
				
				elasticitem = new ElasticContentItem();
				page = getElasticPageSearcher().createNewData();
				page.setSourcePath(inPath);
				
				elasticitem.setElasticData(page);
				
			}
		} else{
			elasticitem = new ElasticContentItem();
			elasticitem.setElasticData(page);
			
		}
		
		return elasticitem;
	}



	@Override
	public boolean doesExist(String inPath) throws RepositoryException {
		Data page = (Data) getElasticPageSearcher().searchByField("sourcepath", inPath);
		if (page == null) {
			return false;
		}
		return true;

	}

	@Override
	public void put(ContentItem inContent) throws RepositoryException {

		if(inContent instanceof ElasticContentItem){
			ElasticContentItem item = (ElasticContentItem) inContent;
			getElasticPageSearcher().saveData(item);
		}
		
		
		
		

	}

	@Override
	public void copy(ContentItem inSource, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(ContentItem inSource, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(ContentItem inSource, Repository inSourceRepository, ContentItem inDestination)
			throws RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(ContentItem inPath) throws RepositoryException {
		if(inPath instanceof ElasticContentItem){
			ElasticContentItem item = (ElasticContentItem) inPath;
			getElasticPageSearcher().delete(item, null);
		}

	}

	@Override
	public List getVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentItem getLastVersion(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getChildrenNames(String inParent) throws RepositoryException {
		return new ArrayList();
	}

	@Override
	public void deleteOldVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub

	}

	protected ContentItem createContentItem( String inPath )
	{
		ElasticContentItem contentItem = new ElasticContentItem();
		contentItem.setPath( inPath );
		contentItem.setAbsolutePath( getAbsolutePath(inPath ) );
		return contentItem;
	}
	public ContentItem getStub( String inPath ) throws RepositoryException
	{
		return createContentItem(inPath);
	}
	
}
