importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.com.openedit.modules.update );
importPackage( Packages.com.openedit.modules.scheduler );

var war = "http://dev.entermediasoftware.com/projects/entermedia-server/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.add("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.add("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

log.add("3. REPLACE LIBS");
var files = new FileUtils();
files.deleteMatch( web + "/lib/entermedia-server*.jar");
files.deleteMatch( web + "/lib/entermedia-5*.jar");
files.deleteMatch( web + "/lib/groovy-*.jar");
files.deleteMatch( web + "/lib/aws-*.jar");

files.copyFileByMatch( tmp + "/WEB-INF/lib/entermedia*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/groovy-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/aws-*.jar", web + "/lib/");


log.add("4. UPGRADE BASE DIR");
files.deleteAll( root + "/WEB-INF/base/entermedia");
files.deleteAll( root + "/WEB-INF/base/emfrontend");
files.deleteAll( root + "/WEB-INF/base/system");
files.deleteAll( root + "/WEB-INF/base/themes/baseem");

files.copyFiles( tmp + "/WEB-INF/base/entermedia", root + "/WEB-INF/base/entermedia");
files.copyFiles( tmp + "/WEB-INF/base/emfrontend", root + "/WEB-INF/base/emfrontend");
files.copyFiles( tmp + "/WEB-INF/base/system", root + "/WEB-INF/base/system");
files.copyFiles( tmp + "/WEB-INF/base/themes/baseem", root + "/WEB-INF/base/themes/baseem");
files.copyFiles( tmp + "/WEB-INF/base/themes/rational", root + "/WEB-INF/base/themes/rational");

log.add("5. CLEAN UP");
files.deleteAll(tmp);
