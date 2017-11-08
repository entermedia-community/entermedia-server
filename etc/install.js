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


//files.deleteMatch( web + "/lib/groovy-*.jar");
////files.deleteMatch( web + "/lib/aws-*.jar");
//files.deleteMatch( web + "/lib/gson*.jar");
//files.deleteMatch( web + "/lib/mp4parser*.jar");
//files.deleteMatch( web + "/lib/isoparser*.jar");
//files.deleteMatch( web + "/lib/PDFBox*.jar");
//files.deleteMatch( web + "/lib/pdfbox*.jar");
//files.deleteMatch( web + "/lib/FontBox*.jar");
//files.deleteMatch( web + "/lib/fontbox*.jar");
//files.deleteMatch( web + "/lib/lucene*.jar");
//files.deleteMatch( web + "/lib/spring*.jar");
//files.deleteMatch( web + "/lib/aopalliance*.jar");
//files.deleteMatch( web + "/lib/jaxen*.jar");
//files.deleteMatch( web + "/lib/commons-*.jar");
//files.deleteMatch( web + "/lib/guava*.jar");
//files.deleteMatch( web + "/lib/velocity-tools*.jar");
//files.deleteMatch( web + "/lib/http*.jar");
//files.deleteMatch( web + "/lib/servlet*.jar");
//files.deleteMatch( web + "/lib/json-simple*.jar");
//files.deleteMatch( web + "/lib/org.apache*.jar");
//files.deleteMatch( web + "/lib/slf*.jar");
//files.deleteMatch( web + "/lib/slf*.jar");
//files.deleteMatch( web + "/lib/t-digest*.jar");
//files.deleteMatch( web + "/lib/transport*.jar");
//files.deleteMatch( web + "/lib/spatial*.jar");
//files.deleteMatch( web + "/lib/snakeyaml*.jar");
//files.deleteMatch( web + "/lib/securesm*.jar");
//files.deleteMatch( web + "/lib/plugin-cli*.jar");
//files.deleteMatch( web + "/lib/netty*.jar");
//files.deleteMatch( web + "/lib/jts*.jar");
//files.deleteMatch( web + "/lib/js*.jar");
//files.deleteMatch( web + "/lib/jopt*.jar");
//files.deleteMatch( web + "/lib/joda*.jar");
//files.deleteMatch( web + "/lib/jna*.jar");
//files.deleteMatch( web + "/lib/jcif*.jar");
//files.deleteMatch( web + "/lib/javax.mail*.jar");
//files.deleteMatch( web + "/lib/java-version*.jar");
//files.deleteMatch( web + "/lib/jackson*.jar");
//files.deleteMatch( web + "/lib/hppc*.jar");
//files.deleteMatch( web + "/lib/Hdr*.jar");
//files.deleteMatch( web + "/lib/elasticsearch*.jar");
//files.deleteMatch( web + "/lib/compress*.jar");
//files.deleteMatch( web + "/lib/compiler*.jar");
//files.deleteMatch( web + "/lib/asm*.jar");
//files.deleteMatch( web + "/lib/activation*.jar");
//files.deleteMatch( web + "/lib/mail*.jar");
//files.deleteMatch( web + "/lib/jtidy*.jar");
//files.deleteMatch( web + "/lib/cvslib*.jar");
//files.deleteMatch( web + "/lib/jazzy*.jar");
//files.deleteMatch( web + "/lib/log4j*.jar");
//files.deleteMatch( web + "/lib/EdenLib.jar");
//files.deleteMatch( web + "/lib/eiiusersystem*.jar");
//files.deleteMatch( web + "/lib/eiistrainer*.jar");
//files.deleteMatch( web + "/lib/repository*.jar");
//files.deleteMatch( web + "/lib/openedit-editor*.jar");
//files.deleteMatch( web + "/lib/openedit-5*.jar");
//files.deleteMatch( web + "/lib/openedit-4*.jar");
//files.deleteMatch( web + "/lib/openedit-3*.jar");
files.deleteAll( root + "/WEB-INF/lib");  //Do a full clean, other plugins can be installed after


files.copyFileByMatch( tmp + "/WEB-INF/lib/*.jar", web + "/lib/");


log.info("5. CLEAN UP");
files.deleteAll(tmp);
