package postdata

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.util.PathUtilities

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	HitTracker all = archive.query("postdata").startsWith("sourcepath", "knowledge").search();
	all.enableBulkOperations();
	for(Data post in all)
	{
		//String cat = post.get("knowledgebase_category");
		//String content = post.get("sourcepath");
		String content = post.get("maincontent");
		if ( content != null )
		{
			//log.info("BeforeXXXX: " + content);
			content = content.replace("src=\"https://entermediadb.org/wp-content/uploads","src=\"wp-content/uploads");
			content = content.replace("src=\"http://entermediasoftware.com/wp-content/uploads","src=\"wp-content/uploads");
			
			int index = 0;
			int lastended = 0
			StringBuffer output = new StringBuffer();
			while(index > -1)
			{
				int found = content.indexOf("src=\"wp-content/uploads",index);
				if( found > -1)
				{
					lastended = found;
					int ending = content.indexOf("\"",found + 7);
					String url = content.substring(found,ending);
					output.append(content.substring(index,lastended));
					
					String filename = PathUtilities.extractFileName(url);
					String fixedurl = url.replace("src=\"wp-content/uploads","src=\"/entermediadb/mediadb/AWb1TUgXDQQ8brMiwpXm/uploads");
					fixedurl = fixedurl.replace(filename,filename + "/" + filename);
					output.append( fixedurl );
					log.info("Appended: " + fixedurl);
					index = ending;
				}
				else
				{
					output.append(content.substring(index,content.length()));
					index = -1;
					
				}
			}
			// <img class="aligncenter size-full wp-image-164" src="wp-content/uploads/2014/11/CustomizeApplication_newapp.png" alt="CustomizeApplication_newapp" width="1024" height="457">
			//log.info("AfterXXXX: " + output);
			//if( cat != null)
			//log.info("content: " + content);
			//cat = cat.toLowerCase();
			//post.setValue("knowledgebase_category", cat);
			//post.setValue("sourcepath", content);
			post.setValue("maincontent", output.toString());
			archive.getSearcher("postdata").saveData(post);
		}
		
	}

}

init();
