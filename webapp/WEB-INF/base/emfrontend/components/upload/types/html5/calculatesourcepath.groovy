import org.openedit.*
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.*
import com.openedit.util.Replacer

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		
		String prefix ="";
		String sourcepath = mediaArchive.getAssetImporter().getAssetUtilities().createSourcePath(context,mediaArchive);
		
		context.setRequestParameter("sourcepath",sourcepath);
		context.putPageValue("sourcepath",sourcepath);
		
}

init();