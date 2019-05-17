package server

import org.entermediadb.asset.MediaArchive
import org.entermediadb.modules.update.Downloader
import org.openedit.node.NodeManager;
import org.openedit.page.Page
import org.openedit.page.manage.PageManager
import org.openedit.util.ZipUtil

public void runit()
{
	NodeManager nodeManager = moduleManager.getBean("elasticNodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	String key = context.findValue("entermediakey");
	String username = context.findValue("username");
	String password = context.findValue("password");
	String serverroot = context.findValue("server");
	if(!serverroot.endsWith("/")) {
		serverroot = serverroot + "/";
	}
	Downloader downloader = new Downloader();
	
	String baseurl = serverroot + "openedit/views/filemanager/download/zip/base.zip?path=/WEB-INF/base&entermedia.key=" + key;
	String lib = serverroot + "openedit/views/filemanager/download/zip/lib.zip?path=/WEB-INF/lib&entermedia.key=" + key;
	downloader.download(baseurl, mediaArchive.getFileForPath("/WEB-INF/temp/base.zip"));
	downloader.download(lib, mediaArchive.getFileForPath("/WEB-INF/temp/lib.zip"));
	PageManager pm = mediaArchive.getPageManager();
	
	
	
	ZipUtil utils = new ZipUtil();
	utils.unzip(mediaArchive.getFileForPath("/WEB-INF/temp/base.zip"),mediaArchive.getFileForPath("/WEB-INF/temp/basenew/"));
	utils.unzip(mediaArchive.getFileForPath("/WEB-INF/temp/lib.zip"),mediaArchive.getFileForPath("/WEB-INF/temp/libnew/"));
	
	Page oldlib = pm.getPage("/WEB-INF/lib/");
	Page backuplib = pm.getPage("/WEB-INF/libold/");
	Page oldbase = pm.getPage("/WEB-INF/base/");
	Page backupbase = pm.getPage("/WEB-INF/baseold/");
	
	Page newlib = pm.getPage("/WEB-INF/temp/basenew/WEB-INF/lib/");
	Page newbase = pm.getPage("/WEB-INF/temp/libnew/WEB-INF/base/");	
	if(backuplib.exists()) {
		pm.removePage(backuplib);
	}
	if(backupbase.exists()) {
		pm.removePage(backupbase);
	}

	if(oldbase.exists()) {
		pm.movePage(oldbase, backupbase);
	}
	
	pm.movePage(newbase, oldbase);
	
	if(oldlib.exists()) {
	pm.movePage(oldlib, backuplib);
	}
	
	pm.movePage(newlib, oldlib);
	
	
	
	
	
	
}

runit();

