package org.entermediadb.asset.push;

import java.util.Collection;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;

public interface PushManager
{

	//TODO: Put a 5 minute timeout on this connection. This way we will reconnect
	HttpClient login(String inCatalogId);

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

	void acceptPush(WebPageRequest inReq, MediaArchive archive);
	
	//void pullApprovedAssets(WebPageRequest inReq, MediaArchive inArchive);

}