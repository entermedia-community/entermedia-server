import java.util.Calendar
import java.util.GregorianCalendar

import org.openedit.Data
import org.openedit.data.Searcher

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

import org.openedit.entermedia.util.CSVWriter;
import com.openedit.users.User;


// Find all asset ids viewed in the last month
Searcher assetsearcher = mediaarchive.getAssetSearcher();

//getSearcher(mediaarchive.getCatalogId(),"asset");
SearchQuery aquery = assetsearcher.createSearchQuery();
GregorianCalendar cal = new GregorianCalendar();
cal.add(Calendar.MONTH, -2);
aquery.addAfter("assetaddeddate", cal.getTime());
aquery.addSortBy("assetaddeddate");

HitTracker assets = assetsearcher.search(aquery);

StringWriter output = new StringWriter();
CSVWriter csv = new CSVWriter(output);

String[] headers = ["user","email","assettitle","path","assetaddeddate","uploadteam"];

csv.writeNext(headers);

String pathprefix = context.findValue("applicationid");
String home = context.findValue("home");
if( home == null )
{
	home = "";
}
pathprefix = "${home}/${pathprefix}/views/assets";

for (Data hit : assets)
{
	String[] row = new String[6];
	User user = userManager.getUser( hit.get("owner") );
	String uname = "${user.getFirstName()} ${user.getLastName()}";
	row[0] = uname;
	row[1] = user.getEmail();
	row[2] = hit.get("assettitle");
	String path = pathprefix + "/" + hit.getSourcePath();
	row[3] = path;
	row[4] = hit.get("assetaddeddate");
	row[5] = hit.get("uploadteam");
	
	csv.writeNext(row);
}

context.putPageValue("csv",csv);
context.putPageValue("allrows",output);

