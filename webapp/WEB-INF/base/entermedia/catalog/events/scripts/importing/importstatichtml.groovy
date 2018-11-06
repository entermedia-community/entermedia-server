package importing

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.page.Page
import org.openedit.repository.ContentItem
import org.openedit.users.User
import org.openedit.util.PathProcessor

public void init()
{
    MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
    
    HtmlPathProcessor processor = new HtmlPathProcessor();
    processor.setArchive(archive);
    processor.setRecursive(true);
    processor.setRootPath("/knowledge");
    processor.setPageManager(archive.getPageManager());
    processor.setIncludeMatches("*.html") ;
    processor.process();
    archive.getSearcher("postdata").saveAllData(processor.getBuffer(), null);
}

init();

class HtmlPathProcessor extends PathProcessor
{
	protected  MediaArchive archive;
	protected  List buffer = new ArrayList(100);
	
	
	public void setArchive(MediaArchive inArchive)
	{
		archive = inArchive;
	}
	
	public List getBuffer()
	{
		return buffer;
	}
	
    public void processFile(ContentItem inContent, User inUser)
    {
        String sourcepath = inContent.getPath();
		sourcepath = sourcepath.substring(sourcepath.indexOf("/")+1, sourcepath.length())
       // sourcepath = sourcepath.substring(getRootPath().length() + 1, inContent.getPath().length());
        Data post = archive.getSearcher("postdata").createNewData();
        Page page = getPageManager().getPage(inContent.getPath());
        String content = page.getContent();
        
        post.setValue("maincontent", content);
        post.setSourcePath(sourcepath);
        post.setValue("sourcepath", sourcepath);
        post.setValue("siteid", "entermediadb");
        
        buffer.add(post);
    }
};