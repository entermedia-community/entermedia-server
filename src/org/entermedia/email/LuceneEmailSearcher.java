package org.entermedia.email;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.users.LuceneUserSearcher;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.users.User;

public class LuceneEmailSearcher extends BaseLuceneSearcher implements Searcher, EmailSearcher {

	protected EmailArchive fieldEmailArchive; 
	public EmailArchive getEmailArchive() {
		
		return fieldEmailArchive;
	}
	public void setEmailArchive(EmailArchive inEmailArchive) {
		fieldEmailArchive = inEmailArchive;
		inEmailArchive.setCatalogId(getCatalogId());
		
	}
	
	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
		getEmailArchive().setCatalogId(getCatalogId());
		
	}
	
	private static final Log log = LogFactory.getLog(LuceneUserSearcher.class);
	
	protected void reIndexAll(IndexWriter writer)
	{
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		// writer.mergeFactor = 10;
		writer.setMergeFactor(100);
		writer.setMaxBufferedDocs(2000);
		try 
		{
			final PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached("email");
			List emails = getEmailArchive().getAllEmailIds();
			for (Iterator iterator = emails.iterator(); iterator.hasNext();) 
			{
				String emailid = (String) iterator.next();
				TemplateWebEmail email = getEmailArchive().loadEmail(emailid);
				populateEmail(writer, email, true, details);
			}

			writer.optimize();

		}
		catch(Exception ex)
		{
			throw new OpenEditException(ex);
		}
		// HitCollector
		log.info("Reindex done");

	}
	private void populateEmail(IndexWriter inWriter, TemplateWebEmail inEmail, boolean add, PropertyDetails inDetails) throws IOException {
		Document doc = new Document();
		Field id = new Field("id", inEmail.getId(), Field.Store.YES, Field.Index.ANALYZED);
		doc.add(id); // Why is this tokenized? Guess so we can find later
		super.updateIndex(inEmail, doc, inDetails);
		try {
			if (add) {
				inWriter.addDocument(doc, getAnalyzer());
			} else {
				Term term = new Term("id", inEmail.getId().toLowerCase());
				inWriter.updateDocument(term, doc, getAnalyzer());
			}
			log.debug("Indexed " + inEmail.getId());
		} catch (IOException ex) {
			throw new OpenEditRuntimeException(ex);
		}
		
	}
	
	
	public  void saveData(Data inData, User inUser){
		if(inData instanceof TemplateWebEmail){
			getEmailArchive().saveEmail((TemplateWebEmail) inData, inUser);
		}
		updateIndex(inData);
	}
	
	
	
	
	
}
