package org.entermediadb.modules;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.CSVReader;
import org.entermediadb.modules.publishing.PubLookUp;
import org.junit.Test;
import org.openedit.OpenEditException;
import org.openedit.page.Page;

public class PubLookUpTest extends BaseEnterMediaTest
{
	protected void setUp() throws Exception
	{
		
		Category parent = getCategoryEditor().getCategory("PRINTPRODUCTION"); 
		if( parent == null)
		{
			parent = getCategoryEditor().addNewCategory( "PRINTPRODUCTION","Print Production");
			getCategoryEditor().saveCategory(parent);
			
			Category child1 = getCategoryEditor().addNewCategory( "0000009999","000000-09999");
			child1.setParentCategory(parent);
			getCategoryEditor().saveCategory(child1);
			
			Category child2 = getCategoryEditor().addNewCategory( "00000999","00000-0999");
			child2.setParentCategory(child1);
			getCategoryEditor().saveCategory(child2);
		}
		assertNotNull( parent );
	}
	@Test
	public void testLookPubIdLookUp()
	{
		List rows = new ArrayList();
		String csvpath = "/"+getMediaArchive().getCatalogId()+"/imports/ALF2.txt";
		Page upload = getMediaArchive().getPageManager().getPage(csvpath);
		Reader reader = upload.getReader();
		String found = null;
		try{
			Integer li = 1;
			CSVReader read = new CSVReader(reader, (char)'\t', true);
			String[] headers = read.readNext();
			String[] line;
			while ((line = read.readNext()) != null){
				String pubitem = line[2]; //Pub-Item
				found = PubLookUp.lookUpbyPubId(pubitem);
			}
		} catch (Exception e){
			throw new OpenEditException(e);
		}
		assertNotNull("Null", found);
		
		
	}


}
