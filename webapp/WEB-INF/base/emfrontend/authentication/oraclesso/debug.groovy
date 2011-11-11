import com.entermedia.soap.SoapUserManager

		SoapUserManager mgr =   new SoapUserManager();// groovyClass.newInstance();	
		mgr.setUserManager(userManager);
		mgr.setSearcherManager( moduleManager.getBean("searcherManager") );
		mgr.setXmlUtil(moduleManager.getBean("xmlUtil"));
		mgr.setContext( context );
String personid = context.getRequestParameter("personid");
String val = mgr.debug(personid);
context.putPageValue("debug",val);
