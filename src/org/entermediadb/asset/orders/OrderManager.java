package org.entermediadb.asset.orders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.event.EventManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;

public interface OrderManager
{

	EventManager getEventManager();

	void setEventManager(EventManager inWebEventHandler);

	SearcherManager getSearcherManager();

	void setSearcherManager(SearcherManager inSearcherManager);

	Data placeOrder(String frontendappid, String inCatlogId, User inUser, HitTracker inAssets, Map inProperties);

	HitTracker findOrdersForUser(String inCatlogId, User inUser);

	void loadOrderHistoryForPage(HitTracker inPage);

	OrderHistory loadOrderHistory(String inCataId, Order order);

	HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid, Order inOrder);

	HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid, String inOrderId);

	/**
	 * @deprecated Use {@link WebPageRequest}
	 * @param inCatalogid
	 * @param inOrderId
	 * @return
	 */
	HitTracker findOrderAssets(String inCatalogid, String inOrderId);

	HitTracker findAssets(WebPageRequest inReq, String inCatalogid, Order inOrder);

	HitTracker findOrderHistory(String inCatalogid, Order inOrder);

	OrderHistory findRecentOrderHistory(String inCatalogid, String inOrderId);

	Order loadOrder(String catalogid, String orderid);

	Order createOrder(String catalogid, WebPageRequest inReq, boolean saveitems);

	ArrayList saveItems(String catalogid, WebPageRequest inReq, String[] fields, String[] items);

	Order createNewOrder(String inAppId, String inCatalogId, String inUsername);

	void removeItemFromOrder(String inCatId, Order inOrder, Asset inAsset);

	Data addItemToOrder(String inCatId, Order order, Asset inAsset, Map inProps);

	void saveOrder(String inCatalogId, User inUser, Order inBasket);

	void placeOrder(WebPageRequest inReq, MediaArchive inArchive, Order inOrder, boolean inResetId);

	//void saveOrderWithHistory(String inCatalogId, User inUser, Order inOrder, OrderHistory inHistory);

	//OrderHistory createNewHistory(String inCatId, Order inOrder, User inUser, String inStatus);

	List<String> addConversionAndPublishRequest(WebPageRequest inReq, Order order, MediaArchive archive, Map<String, String> properties, User inUser);

	String getPresetForOrderItem(String inCataId, Data inOrderItem);

	String getPublishDestinationForOrderItem(String inCataId, Data inOrderItem);

	void updateStatus(MediaArchive archive, Order inOrder);

	void updatePendingOrders(MediaArchive archive);

	int addItemsToBasket(WebPageRequest inReq, MediaArchive inArchive, Order inOrder, Collection inSelectedHits, Map inProps);

	boolean isAssetInOrder(String inCatId, Order inOrder, String inAssetId);

	void delete(String inCatId, Order inOrder);

	void removeItem(String inCatalogid, String inItemid);

	void removeMissingAssets(WebPageRequest inReq, MediaArchive archive, Order basket, Collection items);

	void toggleItemInOrder(MediaArchive inArchive, Order inBasket, Asset inAsset);
	
}