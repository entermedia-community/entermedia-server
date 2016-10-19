importPackage( Packages.org.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.org.entermediadb.modules.update );




var war = "http://dev.entermediasoftware.com/jenkins/job/@BRANCH@entermedia-server/lastSuccessfulBuild/artifact/deploy/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.info("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.info("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

var files = new FileUtils();

log.info("3. UPGRADE BASE DIR");
files.deleteAll( root + "/WEB-INF/base/manager");
files.deleteAll( root + "/WEB-INF/base/entermedia");
files.deleteAll( root + "/WEB-INF/base/emfrontend");
files.deleteAll( root + "/WEB-INF/base/modulefrontend");
files.deleteAll( root + "/WEB-INF/base/system");
files.deleteAll( root + "/WEB-INF/base/mediadb");
files.deleteAll( root + "/WEB-INF/base/emgallery");
files.deleteAll( root + "/WEB-INF/base/reporting");
files.deleteAll( root + "/WEB-INF/base/themes/baseem");

files.copyFiles( tmp + "/WEB-INF/base/entermedia", root + "/WEB-INF/base/entermedia");
files.copyFiles( tmp + "/WEB-INF/base/reporting", root + "/WEB-INF/base/reporting");
files.copyFiles( tmp + "/WEB-INF/base/manager", root + "/WEB-INF/base/manager");
files.copyFiles( tmp + "/WEB-INF/base/emfrontend", root + "/WEB-INF/base/emfrontend");
files.copyFiles( tmp + "/WEB-INF/base/modulefrontend", root + "/WEB-INF/base/modulefrontend");
files.copyFiles( tmp + "/WEB-INF/base/system", root + "/WEB-INF/base/system");
files.copyFiles( tmp + "/WEB-INF/base/mediadb", root + "/WEB-INF/base/mediadb");
files.copyFiles( tmp + "/WEB-INF/base/emgallery", root + "/WEB-INF/base/emgallery");


log.info("4. REPLACE LIBS");
files.deleteMatch( web + "/lib/em9_entermedia-server*.jar");
files.deleteMatch( web + "/lib/em9_entermedia-9*.jar");
files.deleteMatch( web + "/lib/em9dev_entermedia-server*.jar");
files.deleteMatch( web + "/lib/em9dev_entermedia-9*.jar");



files.deleteMatch( web + "/lib/dev_entermedia-server*.jar");
files.deleteMatch( web + "/lib/dev_entermedia-5*.jar");
files.deleteMatch( web + "/lib/dev_entermedia-8*.jar");

files.deleteMatch( web + "/lib/entermedia-server*.jar");
files.deleteMatch( web + "/lib/entermedia-5*.jar");
files.deleteMatch( web + "/lib/entermedia-7*.jar");
files.deleteMatch( web + "/lib/entermedia-8*.jar");


files.deleteMatch( web + "/lib/groovy-*.jar");
//files.deleteMatch( web + "/lib/aws-*.jar");
files.deleteMatch( web + "/lib/gson-*.jar");
files.deleteMatch( web + "/lib/mp4parser*.jar");
files.deleteMatch( web + "/lib/PDFBox*.jar");
files.deleteMatch( web + "/lib/lucene*.jar");
files.deleteMatch( web + "/lib/spring-*.jar");
files.deleteMatch( web + "/lib/aopalliance-*.jar");
files.deleteMatch( web + "/lib/jaxen-*.jar");
files.deleteMatch( web + "/lib/commons-codec*.jar");
files.deleteMatch( web + "/lib/guava-*.jar");
files.deleteMatch( web + "/lib/velocity-tools*.jar");
files.deleteMatch( web + "/lib/http*.jar");
files.deleteMatch( web + "/lib/servlet-gzip.jar");
files.deleteMatch( web + "/lib/json-simple*.jar");
files.deleteMatch( web + "/lib/org.apache.oltu.*.jar");
files.deleteMatch( web + "/lib/slf*.jar");


/*
files.copyFileByMatch( tmp + "/WEB-INF/lib/entermedia*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/dev_entermedia*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/groovy-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/aws-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/gson-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/mp4parser-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/PDFBox*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/lucene*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/commons-net-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/spring-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/aopalliance-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/jaxen-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/commons-codec*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/guava*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/velocity-tools*.jar", web + "/lib/");
*/
files.copyFileByMatch( tmp + "/WEB-INF/lib/*.jar", web + "/lib/");


log.info("5. CLEAN UP");
files.deleteAll(tmp);
