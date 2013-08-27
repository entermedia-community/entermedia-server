package org.openedit.entermedia.push;

import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public interface PushManager
{

	//TODO: Put a 5 minute timeout on this connection. This way we will reconnect
	HttpClient login(String inCatalogId);

	UserManager getUserManager();

	void setUserManager(UserManager inUserManager);

	SearcherManager getSearcherManager();

	void setSearcherManager(SearcherManager inSearcherManager);

	HttpClient getClient(String inCatalogId);

	void processPushQueue(MediaArchive archive, User inUser);

	/**
	 * This will just mark assets as error?
	 * @param archive
	 * @param inUser
	 */
	void processPushQueue(MediaArchive archive, String inAssetIds, User inUser);

	void processDeletedAssets(MediaArchive archive, User inUser);

	void uploadGenerated(MediaArchive archive, User inUser, Asset target, List savequeue);

	/*
		protected boolean checkPublish(MediaArchive archive, Searcher pushsearcher, String assetid, User inUser)
		{
			Data hit = (Data) pushsearcher.searchByField("assetid", assetid);
			String oldstatus = null;
			Asset asset = null;
			if (hit == null)
			{
				hit = pushsearcher.createNewData();
				hit.setProperty("assetid", assetid);
				oldstatus = "none";
				asset = archive.getAsset(assetid);
				hit.setSourcePath(asset.getSourcePath());
				hit.setProperty("assetname", asset.getName());
				hit.setProperty("assetfilesize", asset.get("filesize"));
			}
			else
			{
				oldstatus = hit.get("status");
				if( "1pushcomplete".equals( oldstatus ) )
				{
					return false;
				}
				asset = archive.getAssetBySourcePath(hit.getSourcePath());
			}
			if( log.isDebugEnabled() )
			{
				log.debug("Checking 		String server = inArchive.getCatalogSettingValue("push_server_url");
			//String account = inArchive.getCatalogSettingValue("push_server_username");
			String targetcatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
			//String password = getUserManager().decryptPassword(getUserManager().getUser(account));
	
			String url = server + "/media/services/rest/" + "handlesync.xml?catalogid=" + targetcatalogid;
			PostMethod method = new PostMethod(url);
	
			String prefix = inArchive.getCatalogSettingValue("push_asset_prefix");
			if( prefix == null)
			{
				prefix = "";
			}
			
			try
			{
				List<Part> parts = new ArrayList();
				int count = 0;
				for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
				{
					File file = (File) iterator.next();
					FilePart part = new FilePart("file." + count, file.getName(),upload file);
					parts.add(part);
					count++;
				}
	//			parts.add(new StringPart("username", account));
	//			parts.add(new StringPart("password", password));
				for (Iterator iterator = inAsset.getProperties().keySet().iterator(); iterator.hasNext();)
				{
					String key = (String) iterator.next();
					parts.add(new StringPart("field", key));
					parts.add(new StringPart(key+ ".value", inAsset.get(key)));
				}
				parts.add(new StringPart("sourcepath", inAsset.getSourcePath()));
				
				if(inAsset.getName() != null )
				{upload(target, archive, filestosend);
					parts.add(new StringPart("original", inAsset.getName())); //What is this?
				}
				parts.add(new StringPart("id", prefix + inAsset.getId()));
				
	//			StringBuffer buffer = new StringBuffer();
	//			for (Iterator iterator = inAsset.getCategories().iterator(); iterator.hasNext();)
	//			{
	//				Category cat = (Category) iterator.next();
	//				buffer.append( cat );
	//				if( iterator.hasNext() )
	//				{
	//					buffer.append(' ');
	//				}
	//			}
	//			parts.add(new StringPart("catgories", buffer.toString() ));
				
				Part[] arrayOfparts = parts.toArray(new Part[] {});
	
				method.setRequestEntity(new MultipartRequestEntity(arrayOfparts, method.getParams()));
				
				Element root = execute(inArchive.getCatalogId(), method);
				Map<String, String> result = new HashMap<String, String>();
				for (Object o : root.elements("asset"))
				{
					Element asset = (Element) o;
					result.put(asset.attributeValue("id"), asset.attributeValue("sourcepath"));
				}
				log.info("Sent " + server + "/" + inAsset.getSourcePath());
				return result;
			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
	asset: " + asset);
			}
			
			if(asset == null)
			{
				return false;
			}
			String rendertype = archive.getMediaRenderType(asset.getFileFormat());
			if( rendertype == null )
			{
				rendertype = "document";
			}
			boolean readyforpush = true;
			Collection presets = archive.getCatalogSettingValues("push_convertpresets");
			for (Iterator iterator2 = presets.iterator(); iterator2.hasNext();)
			{
				String presetid = (String) iterator2.next();
				Data preset = archive.getSearcherManager().getData(archive.getCatalogId(), "convertpreset", presetid);
				if( rendertype.equals(preset.get("inputtype") ) )
				{
					Page tosend = findInputPage(archive, asset, preset);
					if (!tosend.exists())
					{
						if( log.isDebugEnabled() )
						{
							log.debug("Convert not ready for push " + tosend.getPath());
						}
						readyforpush = false;
						break;
					}
				}
			}
			String newstatus = null;
			if( readyforpush )
			{
				newstatus = "3readyforpush";
				hit.setProperty("percentage","0");
			}
			else
			{
				newstatus = "2converting";			
			}
			if( !newstatus.equals(oldstatus) )
			{
				hit.setProperty("status", newstatus);
				pushsearcher.saveData(hit, inUser);
			}
			return readyforpush;
		}
	 */
	void resetPushStatus(MediaArchive inArchive, String oldStatus, String inNewStatus);

	Collection getCompletedAssets(MediaArchive inArchive);

	Collection getPendingAssets(MediaArchive inArchive);

	Collection getNoGenerated(MediaArchive inArchive);

	Collection getErrorAssets(MediaArchive inArchive);

	Collection getImportCompleteAssets(MediaArchive inArchive);

	Collection getImportPendingAssets(MediaArchive inArchive);

	Collection getImportErrorAssets(MediaArchive inArchive);

	void pushAssets(MediaArchive inArchive, List<Asset> inAssetsSaved);

	void pollRemotePublish(MediaArchive inArchive);

	void toggle(String inCatalogId);

	void acceptPush(WebPageRequest inReq, MediaArchive archive, String sourcepath);

}