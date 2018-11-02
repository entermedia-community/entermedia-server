package org.entermediadb.asset.modules;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.ConvertStatus;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.MathUtils;
import org.entermediadb.error.EmailErrorHandler;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageAction;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class MediaArchiveModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(MediaArchiveModule.class);
	protected EmailErrorHandler fieldEmailErrorHandler;
	//protected FileUpload fieldFileUpload;
	
	public MediaArchiveModule()
	{
	}

//	public void createThumbAndMedium(WebPageRequest inReq) throws Exception
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		archive.getTranscodeTools().run(true, true, false, false, archive.getAssetSearcher().getAllHits());
//	}

	public void voteUp(WebPageRequest inReq) throws Exception
	{
		String sourcepath = inReq.getRequestParameter("sourcepath");
		if (sourcepath != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			Asset asset = archive.getAssetBySourcePath(sourcepath);
			String vote = asset.get("voteup");
			int val = 1;
			if (vote != null)
			{
				val = Integer.parseInt(vote);
				val++;
			}
			asset.setProperty("voteup", String.valueOf(val));
			archive.saveAsset(asset, inReq.getUser());

			inReq.putPageValue("asset", asset);
			inReq.putPageValue("cell", asset);
		}
	}

	public void voteDown(WebPageRequest inReq) throws Exception
	{
		String sourcepath = inReq.getRequestParameter("sourcepath");
		if (sourcepath != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			Asset asset = archive.getAssetBySourcePath(sourcepath);
			String vote = asset.get("votedown");
			int val = -1;
			if (vote != null)
			{
				val = Integer.parseInt(vote);
				val--;
			}
			asset.setProperty("votedown", String.valueOf(val));
			archive.saveAsset(asset, inReq.getUser());
			inReq.putPageValue("asset", asset);
			inReq.putPageValue("cell", asset);
		}
	}

	public void createNewAsset(WebPageRequest inReq) throws OpenEditException
	{

		User user = inReq.getUser();
		String catalogid = inReq.getRequestParameter("catalogid");

		if (user.hasPermission("archive.register"))
		{
			MediaArchive archive = getMediaArchive(inReq);
			Category defaultcat = archive.getCategoryArchive().getCategory(catalogid);
			if (defaultcat == null)
			{
				defaultcat = archive.getCategoryArchive().getRootCategory();
			}
			String[] fields = inReq.getRequestParameters("field");
			PropertyDetails details = archive.getAssetPropertyDetails();
			Asset asset = (Asset) inReq.getPageValue("asset");
			asset.addCategory(defaultcat);
			for (int i = 0; i < fields.length; i++)
			{
				String field = fields[i];
				String value = inReq.getRequestParameter(field + ".value");
				PropertyDetail detail = details.getDetail(field);
				if (detail != null)
				{
					if (value != null)
					{
						if (detail.isDataType("date"))
						{
							// try the date format from the picker
							DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy"); //TODO: Pass in the format from the picker
							try
							{
								Date date = dateFormat.parse(value);
								String newval = DateStorageUtil.getStorageUtil().formatForStorage(date);
								value = newval;
							}
							catch (ParseException e)
							{
								// We'll ignore this and hope it gets handled
								// down the line via the detail object formater.
							}

						}
						asset.setProperty(field, value);
					}
					else
					{
						asset.removeProperty(field);
					}
				}
			}
			archive.saveAsset(asset, inReq.getUser());

			inReq.putPageValue("saved", "true");
		}
		else
		{
			throw new OpenEditException("No permissions to complete this action");
		}
	}

	public void convertData(WebPageRequest inPageRequest) throws Exception
	{
		boolean forced = Boolean.parseBoolean(inPageRequest.findValue("forced"));
		MediaArchive archive = getMediaArchive(inPageRequest);
		ConvertStatus errorlog = archive.convertCatalog(inPageRequest.getUser(), forced);
		if (inPageRequest != null)
		{
			inPageRequest.removeSessionValue("store");
			inPageRequest.putPageValue("exception-report", errorlog.getLog());
		}
		inPageRequest.putPageValue("logs", errorlog.getLog());
	}

	public void reindex(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		archive.reindexAll();
		archive.getCategoryArchive().reloadCategories();
	}

	public void clearSearchIndex(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);

		archive.getAssetSearcher().clearIndex();
	}

	public EmailErrorHandler getEmailErrorHandler()
	{
		return fieldEmailErrorHandler;
	}

	public void setEmailErrorHandler(EmailErrorHandler fieldEmailErrorHandler)
	{
		this.fieldEmailErrorHandler = fieldEmailErrorHandler;
	}

	public void toggleProperty(WebPageRequest inReq) throws Exception
	{
		User user = inReq.getUser();
		if (user != null)
		{
			String id = inReq.getRequestParameter("propertyid");
			if (id == null)
			{
				id = inReq.getRequestParameter("id");
			}
			if (id != null)
			{
				boolean has = user.hasProperty(id);
				if (has)
				{
					user.setValue(id,null);
				}
				else
				{
					user.setValue(id, String.valueOf(has));
				}
				getUserManager(inReq).saveUser(user);
			}
		}
	}

	public void createLogin(WebPageRequest inReq) throws OpenEditException
	{
		User user = inReq.getUser();
		if (user == null)
		{
			// only do this if we're not logged in..
			// sales people get nothing
			// assetion people get everything
			PageAction cAcction = inReq.getCurrentAction();
			String username = cAcction.getConfig().getChildValue("username");
			if (username == null)
			{
				username = inReq.getRequestParameter("username");
			}
			if (username != null)
			{
				String groupid = cAcction.getConfig().getChildValue("group");
				String virtual = inReq.findValue("virtualuser");
				if (!Boolean.parseBoolean(virtual))
				{
					user = getUserManager(inReq).getUser(username);
				}
				if (user == null)
				{
					user = getUserManager(inReq).createGuestUser(username, null, groupid);
					log.info("Creating virtual user " + username);
				}

//				String includerecords = cAcction.getConfig().getChildValue("limitrecords");
//				if (includerecords != null)
//				{
//					// excluderecords:PII_site:false
//					// user.put("includerecords", );
//					String val = inReq.getRequestParameter(includerecords);
//					if (val != null)
//					{
//						Group tmpGroup = getUserManager(inReq).createGroup();
//						tmpGroup.addPermission("limitrecords:" + includerecords + ":" + val);
//						user.addGroup(tmpGroup); // TODO: Keep this from
//						// saving
//					}
//					else
//					{
//						log.error("No value passed in for " + includerecords);
//					}
//				}
				// <username>ExternalUser</username>
				// <group>externalusers</group>
				// <includerecords>customernumber</includerecords>
				if (!user.isVirtual())
				{
					AdminModule umodule = (AdminModule) getModule("Admin");
					umodule.savePasswordAsCookie(user, inReq);
				}
			}
			inReq.putPageValue("user", user);
			inReq.putSessionValue("user", user);
			inReq.removeSessionValue("catalogTree"); // to reload it
			getMediaArchive(inReq); // To reload the MediaArchive object

		}
	}
	/**
	 * Requires catalog on the URL and sourcepath
	 * @param inReq
	 * @throws OpenEditException
	 */
	/*
	public void uploadAsset( WebPageRequest inReq ) throws OpenEditException
	{
		UploadRequest map = getFileUpload().parseArguments(inReq);
		MediaArchive archive = getMediaArchive(inReq);
		if ( map == null || map.getUploadItems().size() == 0)
		{
			log.info("no assets found, reloading page");
			return;
		}
		long utime = System.currentTimeMillis();
//		String temppath = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/" + inReq.getUserName() + "/tmp" + utime + "_" + map.getFirstItem().getName();
//		map.saveFirstFileAs(temppath, inReq.getUser());
		
		List unzipped = map.unzipFiles(false);
		inReq.putPageValue("uploadrequest", map);
		if( map.getFirstItem() != null)
		{
			Page first = map.getFirstItem().getSavedPage();
			inReq.putPageValue("firstfilepath", first.getPath());
		}
		inReq.putPageValue("unzippedfiles", unzipped);
		inReq.putPageValue("pageManager", getPageManager());
	}
	*/

//	public FileUpload getFileUpload()
//	{
//		return fieldFileUpload;
//	}
//
//	public void setFileUpload(FileUpload fileUpload)
//	{
//		fieldFileUpload = fileUpload;
//	}

	/**
	 * This must be called as a path-action
	 * @param inReq
	 * @throws Exception
	 */
	public void forceDownload(WebPageRequest inReq) throws Exception
	{
		if( inReq.getResponse() != null)
		{
			String embedded = inReq.findValue("embedded");
			if( !Boolean.parseBoolean(embedded))
			{
				Page content = inReq.getContentPage();
				String filename = content.getName(); 
				//filename = URLEncoder.encode(filename,content.getCharacterEncoding());
				filename = filename.replace(";", "");
				inReq.getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			}
		}
	}

	public void clearCache(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		archive.clearCaches();
	}
	public Asset getAssetAndPage(WebPageRequest inReq)
	{
		Asset asset = getAsset(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		//Find this on this tracker and match up the page
		HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
		if( tracker != null)
		{
			int index = tracker.indexOfId(asset.getId());
			if( index < 1)
			{
				index = 1;
			}
			double page = (double)(index + 1) / (double)tracker.getHitsPerPage();
			int gotopage = MathUtils.roundUp(page);
			if( gotopage > tracker.getTotalPages() )
			{
				gotopage = tracker.getTotalPages() - 1;
			}
			tracker.setPage(gotopage);
		}
		return asset;
	}
}

