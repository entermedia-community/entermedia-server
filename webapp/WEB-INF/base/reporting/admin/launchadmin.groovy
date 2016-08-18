




public void init(){
	String id = context.getRequestParameter("id");
	 archive = context.getPageValue("mediaarchive");
	
	 pageManager = archive.getPageManager();
	 applicationid = context.findValue("applicationid");
	 catalogid = context.findValue("catalogid");
	 fallback = "/WEB-INF/base/emshare";

	 targetfolder = "admin/${applicationid}";
	if(!pageManager.getPage("/${targetfolder}").exists()){
		deployApp(pageManager, catalogid, targetfolder, fallback, properties);
	}
	
	context.redirect("/${targetfolder}");	
	
}
init();


public void deployApp( pageManager, String inAppcatalogid, String inDestinationAppId, String fallback, Map inProperties) {


	//tweak the xconf
	 homesettings = pageManager.getPageSettingsManager().getPageSettings("/" + inDestinationAppId + "/_site.xconf");
	homesettings.setProperty("applicationid", inDestinationAppId);
	homesettings.setProperty("catalogid", inAppcatalogid);
	homesettings.setProperty("fallbackdirectory",fallback);
	for (Iterator iterator = inProperties.keySet().iterator(); iterator.hasNext();){
		String key = iterator.next();
		if (!(inProperties.get(key) instanceof String)){
			continue;
		}
		String val = inProperties.get(key);
		if(val != null){
			homesettings.setProperty(key, val);
		}
	}

	pageManager.getPageSettingsManager().saveSetting(homesettings);




}
