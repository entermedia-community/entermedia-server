import org.openedit.*
import org.entermediadb.asset.MediaArchive

import org.openedit.hittracker.*
import org.openedit.util.Replacer

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		
		String prefix ="";
		String name = context.getRequestParameter("name");
		//String sourcepath = mediaArchive.getAssetImporter().getAssetUtilities().createSourcePath(context,mediaArchive,name);
		
		//context.setRequestParameter("sourcepath",sourcepath);
		//context.putPageValue("sourcepath",sourcepath);
		
}

//init();