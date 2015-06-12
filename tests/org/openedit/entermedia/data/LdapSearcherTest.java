package org.openedit.entermedia.data;

import java.util.Collection;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.util.LdapSearcher;

public class LdapSearcherTest extends BaseEnterMediaTest
{
	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getBean("searcherManager");
	}
	
	public void testConnection() throws Exception
	{
		String catalog = "entermedia/catalogs/testcatalog";
		MediaArchive archive = getMediaArchive(catalog);
		LdapSearcher searcher  = (LdapSearcher)archive.getSearcher("ldap");
		Collection found = searcher.query().match("cn", "EC_emshare").search();
		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			System.out.println(hit.getName());
		}
	}
}
