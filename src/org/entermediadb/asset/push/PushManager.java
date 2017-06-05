package org.entermediadb.asset.push;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;

public interface PushManager
{


	Collection getCompletedAssets(MediaArchive inArchive);

	Collection getPendingAssets(MediaArchive inArchive);

	Collection getNoGenerated(MediaArchive inArchive);

	Collection getErrorAssets(MediaArchive inArchive);

	Collection getImportCompleteAssets(MediaArchive inArchive);

	Collection getImportPendingAssets(MediaArchive inArchive);

	Collection getImportErrorAssets(MediaArchive inArchive);

	void toggle(String inCatalogId);

	void pollRemotePublish(MediaArchive inArchive);

	void processPushQueue(MediaArchive archive, String inAssetIds, User inUser);

	void processDeletedAssets(MediaArchive archive, User inUser);
	void resetPushStatus(MediaArchive inArchive, String oldStatus, String inNewStatus);
	void acceptPush(WebPageRequest inReq, MediaArchive archive);
	
	public void pushAssets(MediaArchive inArchive, List<Asset> inAssetsSaved);



}