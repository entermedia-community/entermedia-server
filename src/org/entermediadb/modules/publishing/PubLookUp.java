package org.entermediadb.modules.publishing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;

public class PubLookUp implements CatalogEnabled {
	
	private static final Log log = LogFactory.getLog(ContentModule.class);
	
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	
	protected ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive");
		return archive;
	}
	
	private String RootCategory = "PRINTPRODUCTION";
	

	public Category lookUpbyPubId(String pubitem) {
		//Search path that matches
		/*
		 * ignore none 00- ?
		 * ignore zz
		 * numeric id (00-###) -> ####-####/###-##/###  
		 * alphanumeric (00-ABC) ->  Warner-A/ABC
		 * 
		 * */
		
		Category found = null;
		String foundcatid = null;
		
		String [] splits = pubitem.split("-");
		
		//ignore none 00-xxxx
		if(splits.length != 2) {
			return null;
		}
		
		//ignore non starting with 00-
		if (!splits[0].equals("00"))
		{
			return null;
		}
		
		String pubid = splits[1];
		//Boolean isDigit = Character.isDigit(pubid.charAt(0));
		
		String numericRegex = "^(\\d+)(\\w?)";
		
		Pattern pattern = Pattern.compile(numericRegex);
        Matcher matcher = pattern.matcher(pubid);

		
		if(matcher.find())
		{
			
	        Long pubidnumeric =  Long.parseLong(matcher.group(1));
	        
	        //boolean isNumeric = Pattern.matches(numericRegex, pubid);
			
			if(pubidnumeric < 10000) {
				//00000-09999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "00000-09999");
			}
			else if(pubidnumeric < 20000) {
				//10000-19999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "10000-19999");
			}
			else if(pubidnumeric < 30000) {
				//20000-29999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "20000-29999");
			}
			else if(pubidnumeric < 40000) {
				//30000-39999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "30000-39999");
			}
			else if(pubidnumeric < 50000) {
				//40000-49999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "40000-49999");
			}
			else if(pubidnumeric < 60000) {
				//50000-59999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "50000-59999");
			}
			else if(pubidnumeric < 200000) {
				//100000-199999
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "100000-199999");
			}
			else if(pubidnumeric < 880000) {
				//250000
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "250000");
			}
			else if(pubidnumeric < 900000) {
				//880000
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "880000");
			}
			else if(pubidnumeric < 1000000) {
				//900000-1000000
				foundcatid = lookUpNumericId(pubid, pubidnumeric, "900000-1000000");
			}
			else  {
				//No numeric folder-range matched
				log.info("No numeric folder-range matched for: " + pubidnumeric);
			}
			
		}
		else 
		{
			//log.info("Second part not Numeric: "  + pubitem);
			String firstChar = String.valueOf(pubid.charAt(0));
			foundcatid = lookUpByChar(pubid, firstChar);
			
		}
		
		if (foundcatid != null)
		{
			found = getMediaArchive().getCategory(foundcatid);
		}
		
		return found;
	}
	
	public String lookUpByChar(String pubid, String firstChar)
	{
		String searchCategory = "Warner-" + firstChar.toUpperCase();
		Data foundlevel1 = null;
		Category parentcat = getMediaArchive().getCategory("4"); //Print Production
		//Search fist level -- or search by parent and name?
		List categories  = getMediaArchive().getCategorySearcher().findChildren(parentcat);
		for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
			Data cat = (Data) iterator.next();
			if (searchCategory.equals(cat.getName()))
			{
				foundlevel1 = cat;
				break;
			}
		}
		if (foundlevel1 != null)
		{
			//second level
			Category foundcat = getMediaArchive().getCategory(foundlevel1.getId());
			categories  = getMediaArchive().getCategorySearcher().findChildren(foundcat);
			Data foundlevel2 = null;
			for (Iterator iterator2 = categories.iterator(); iterator2.hasNext();) {
				Data cat = (Data) iterator2.next();
				String catname = cat.getName();
				String regex = "^" + Pattern.quote(pubid);
				
				Pattern pattern = Pattern.compile(regex);
		        Matcher matcher = pattern.matcher(catname);
		        if (matcher.find()) {
		        	return cat.getId();
		        }
			}
		}
		return null;
	}
	
	public String lookUpNumericId(String pubid, Long pubidnumeric, String pathmatch)
	{
		Data foundlevel1 = null;
		Category parentcat = getMediaArchive().getCategory("4"); //Print Production
		//Search fist level
		List categories  = getMediaArchive().getCategorySearcher().findChildren(parentcat);
		for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
			Data cat = (Data) iterator.next();
			
			if (cat.getName().equals(pathmatch))
			{
				//log.info("Found: " + cat.getName() + " <> " + pubid.toString());
				foundlevel1 = cat;
				break;
			}
		}
		if(foundlevel1 != null) 
		{
			//second level
			Category foundcat = getMediaArchive().getCategory(foundlevel1.getId());
			categories  = getMediaArchive().getCategorySearcher().findChildren(foundcat);
			Data foundlevel2 = null;
			for (Iterator iterator2 = categories.iterator(); iterator2.hasNext();) {
				Data cat = (Data) iterator2.next();
				String catname = cat.getName();
				//String [] splits = catname.split("-");
				String regex = "\\d+";
		        Pattern pattern = Pattern.compile(regex);
		        Matcher matcher = pattern.matcher(catname);
		        List<Long> numbers = new ArrayList<>();
		        while (matcher.find()) {
		            numbers.add(Long.parseLong(matcher.group()));
		        }
		        if(numbers.size() == 1) 
		        {
		        	//single NUMERIC folder or "NUMERIC STRING"
		        }
		        if(numbers.size() == 2) 
		        {
		        	//is a range
		        	if(pubidnumeric > numbers.get(0) && pubidnumeric < numbers.get(1))
		        	{
		        		//log.info("Found Category: " + cat.getName() + " for: " + pubid.toString());
		        		//return cat.getId();
		        		foundlevel2 = cat;
		        		break;
		        	}
		        }
			}
			if (foundlevel2 != null) 
			{
				//final level
				Category foundcat2 = getMediaArchive().getCategory(foundlevel2.getId());
				categories  = getMediaArchive().getCategorySearcher().findChildren(foundcat2);
				Data foundlevel3 = null;
				for (Iterator iterator3 = categories.iterator(); iterator3.hasNext();) {
					Data cat = (Data) iterator3.next();
					String catname = cat.getName();
					String numericRegex = "(\\d+)(\\w)";
					
					Pattern pattern = Pattern.compile(numericRegex);
			        Matcher matcher = pattern.matcher(catname);
			        
					if (matcher.find())
					{
						String catnamepubid = matcher.group(0);
						{
							if(catnamepubid.equals(pubid)) 
							{
								return cat.getId();
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	
	
}
