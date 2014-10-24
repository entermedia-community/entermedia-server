package importing;

import org.openedit.entermedia.MediaArchive 
import groovy.util.slurpersupport.GPathResult

import org.openedit.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.creator.*

import com.openedit.hittracker.*
import com.openedit.page.Page

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	Page page = archive.getPageManager().getPage("/cumulus/CUMULUSMETADATA/assetMetaData.xml");
	GPathResult path = new XmlSlurper().parse(page.getReader());
	path.fields.each{
		println it;
	}
	
}

init();
