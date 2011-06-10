import org.openedit.data.Searcher 
import org.openedit.data.PropertyDetails;
import org.openedit.data.View 
import org.openedit.Data;
import org.openedit.data.PropertyDetailsArchive;
import java.util.*;
import org.openedit.Data 
import org.openedit.data.Searcher 
import com.openedit.page.manage.*;

Searcher searcher = searcherManager.getSearcher(mediaarchive.getCatalogId(), "assettype");

PropertyDetailsArchive archive =  searcherManager.getPropertyDetailsArchive(mediaarchive.getCatalogId());
PropertyDetails details = archive.getPropertyDetailsCached("asset");
View toplevel = archive.getDetails(details,"asset/searchselect", null);
toplevel.clear();

Collection hits = searcher.fieldSearch("id","*","textUp");
for(Data hit in hits)
{
	String assettype = hit.getId();
	View child = archive.getDetails(details,"asset/assettype/${assettype}/general", null);
	if( child != null &&  child.hasChildren() )
	{
		child.setTitle( hit.getName() );
		toplevel.add(child);
	}
}
archive.saveView(mediaarchive.getCatalogId(),toplevel,user);