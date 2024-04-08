package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderDownload;
import org.entermediadb.asset.orders.OrderManager;
import org.entermediadb.asset.orders.PresetOption;
import org.entermediadb.asset.orders.RenderTypeOptions;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.DateStorageUtil;

public class OrderModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(OrderModule.class);
//	protected PostMail fieldPostMail;
//
//	public PostMail getPostMail()
//	{
//		return fieldPostMail;
//	}
//
//	public void setPostMail(PostMail inPostMail)
//	{
//		fieldPostMail = inPostMail;
//	}

	public OrderManager getOrderManager(WebPageRequest inReq)
	{
		OrderManager manager = (OrderManager)getModuleManager().getBean(inReq.findPathValue("catalogid"),"orderManager");
		inReq.putPageValue("ordermanager",manager);
		return manager;
	}


	/*
	 * public Data placeOrder(WebPageRequest req) { String catalogid =
	 * req.findPathValue("catalogid");
	 * 
	 * Album album = getEnterMedia(req).getAlbumArchive().loadAlbum("4",
	 * req.getUserName()); HitTracker assets = album.getAssets(catalogid, req);
	 * if (assets.size() > 0) { Map props = new HashMap();
	 * 
	 * String applicationid = req.findValue("applicationid"); Data order =
	 * getOrderManager(inReq).placeOrder(applicationid, catalogid, req.getUser(),
	 * assets, props); req.putPageValue("order", order);
	 * req.setRequestParameter("orderid", order.getId()); List realassets = new
	 * ArrayList(); // this is really bizarre. We're loading the assets into
	 * memory // simply to have them turned into BaseData objects. // Need a
	 * better way to remove stuff from an album. for (Iterator iterator =
	 * assets.iterator(); iterator.hasNext();) { Data hit = (Data)
	 * iterator.next(); Asset asset =
	 * getMediaArchive(catalogid).getAsset(hit.getId()); if (asset != null) {
	 * realassets.add(asset); } } album.removeAssets(realassets, req);
	 * 
	 * // <property name="subject">Order Placed</property> // TODO: Move these
	 * to generic fields String prefix = req.findValue("subjectprefix"); if
	 * (prefix == null) { prefix = "Order received:"; } prefix = prefix + " " +
	 * order.getId();
	 * 
	 * String postfix = req.findValue("subjectpostfix"); if (postfix != null) {
	 * prefix = prefix + " " + postfix; } req.putPageValue("subject", prefix);
	 * 
	 * return order; } else { req.setCancelActions(true); }
	 * 
	 * return null; }
	 */
	public Data createNewOrder(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		String applicationid = inReq.findValue("applicationid");

		Order order = (Order) getOrderManager(inReq).createNewOrder(applicationid, catalogid, inReq.getUserName());
		inReq.putPageValue("order", order);

		//OrderHistory history = getOrderManager(inReq).createNewHistory(catalogid, order, inReq.getUser(), "newrecord");
		
		
		String desktopid = inReq.findValue("desktop"); 
		if(desktopid != null) {
			order.setProperty("desktopid", desktopid);
		}
		
		order.setProperty("downloadedstatus", "new"); 
		
		getOrderManager(inReq).saveOrder(catalogid, inReq.getUser(), order);
		inReq.setRequestParameter("orderid", order.getId());
		return order;
	}

	public Order createOrderFromSelections(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");

		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		
		
		HitTracker assets = null;
		if (hitssessionid != null)
		{
			assets = (HitTracker) inReq.getSessionValue(hitssessionid);
		}
		else
		{
			assets = new ListHitTracker();
			String[] sourcepaths = inReq.getRequestParameters("sourcepath");
			if (sourcepaths == null)
			{
				log.error("No assets passed in");
				return null;
			}
			for (int i = 0; i < sourcepaths.length; i++)
			{
				Data hit = new BaseData();
				hit.setSourcePath(sourcepaths[i]);
				assets.add(hit);
				assets.addSelection(hit.getId());
			}
		}
		if (assets == null)
		{
			return null;
		}
		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
		List orderitems = new ArrayList();
		
		String selectall = inReq.getRequestParameter("selectall");
		if (Boolean.parseBoolean(selectall)) 
		{
			assets.selectAll();
		}

		if (assets.hasSelections())
		{
			Map props = new HashMap();
			


			String applicationid = inReq.findValue("applicationid");
			Order order = (Order) getOrderManager(inReq).createNewOrder(applicationid, catalogid, inReq.getUserName());
			inReq.putPageValue("order", order);
			inReq.setRequestParameter("orderid", order.getId());

			for (Iterator iterator = assets.getSelectedHitracker().iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				Asset asset = getMediaArchive(catalogid).getAssetBySourcePath(hit.getSourcePath());
				if( asset != null)
				{
					getOrderManager(inReq).addItemToOrder(catalogid, order, asset, props);
				}
			}
			if (order.get("expireson") == null)
			{
				String days = getMediaArchive(catalogid).getCatalogSettingValue("orderexpiresdays");
				if (days == null)
				{
					days = "30";
				}
				GregorianCalendar cal = new GregorianCalendar();
				cal.add(Calendar.DAY_OF_YEAR, Integer.parseInt(days));
				order.setProperty("expireson", DateStorageUtil.getStorageUtil().formatForStorage(cal.getTime()));
			}
			
			String desktopid = inReq.findValue("desktop"); 
			if(desktopid != null) {
				order.setProperty("desktopid", desktopid);
			}
			
			order.setProperty("downloadedstatus", "new");
			
			getOrderManager(inReq).saveOrder(catalogid, inReq.getUser(), order);

			return order;
		}
		else
		{
			inReq.setCancelActions(true);
		}

		return null;
	}

	public Order createOrderFromData(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");

		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String mergefield = inReq.getRequestParameter("mergefield");
		if (mergefield == null)
		{
			mergefield = "assetid";
		}
		HitTracker datalist = null;
		if (hitssessionid != null)
		{
			datalist = (HitTracker) inReq.getSessionValue(hitssessionid);
		}

		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
		List orderitems = new ArrayList();

		if (datalist.getSelectedHitracker().size() > 0)
		{
			Map props = new HashMap();

			String applicationid = inReq.findValue("applicationid");
			Order order = (Order) getOrderManager(inReq).createNewOrder(applicationid, catalogid, inReq.getUserName());
			inReq.putPageValue("order", order);
			inReq.setRequestParameter("orderid", order.getId());

			for (Iterator iterator = datalist.getSelectedHitracker().iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				String targetid = hit.get(mergefield);
				Asset asset = getMediaArchive(catalogid).getAsset(targetid);
				getOrderManager(inReq).addItemToOrder(catalogid, order, asset, null);
			}
			if (order.get("expireson") == null)
			{
				String days = getMediaArchive(catalogid).getCatalogSettingValue("orderexpiresdays");
				if (days == null)
				{
					days = "30";
				}
				GregorianCalendar cal = new GregorianCalendar();
				cal.add(Calendar.DAY_OF_YEAR, Integer.parseInt(days));
				order.setProperty("expireson", DateStorageUtil.getStorageUtil().formatForStorage(cal.getTime()));
			}
			
			String desktopid = inReq.findValue("desktop"); 
			if(desktopid != null) {
				order.setProperty("desktopid", desktopid);
			}
			
			order.setProperty("downloadedstatus", "new");

			getOrderManager(inReq).saveOrder(catalogid, inReq.getUser(), order);

			return order;
		}
		else
		{
			inReq.setCancelActions(true);
		}

		return null;
	}

	public Collection saveItems(WebPageRequest inReq) throws Exception
	{
		String[] items = inReq.getRequestParameters("itemid");
		if (items != null)
		{
			String[] fields = inReq.getRequestParameters("field");
			String catalogid = inReq.findPathValue("catalogid");
			ArrayList toSave = getOrderManager(inReq).saveItems(catalogid, inReq, fields, items);
			return toSave;
		}
		return null;
	}

	public Order loadOrder(WebPageRequest inReq)
	{
		Order order = (Order) inReq.getPageValue("order");
		
		String orderid = inReq.findValue("orderid");
		if (orderid == null)
		{
			orderid = inReq.getRequestParameter("id");
		}

		
		if (order != null && (order.getId().equals(orderid) || orderid == null))
		{
			return order;
		}

		String catalogid = inReq.findPathValue("catalogid");
				if (orderid == null)
		{
			return null;

		}
		order = getOrderManager(inReq).loadOrder(catalogid, orderid);
		inReq.putPageValue("order", order);
		return order;
	}

	public Order saveOrder(WebPageRequest inReq) throws Exception
	{
		return saveOrder(inReq, false);
	}

	public Order createOrder(WebPageRequest inReq) throws Exception
	{
		return saveOrder(inReq, true);
	}

	public Order updateOrder(WebPageRequest inReq) throws Exception
	{
		Order order = loadOrder(inReq);
		String[] fields = inReq.getRequestParameters("field");
		String catalogid = inReq.findPathValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
		
		String[] sharewithemail = inReq.getRequestParameters("sharewithemail.value");
		String sharewithemail_plain = "";
		if (sharewithemail != null) {
			if (sharewithemail.length == 1) {
				sharewithemail_plain = sharewithemail[0];
			}
			else {
				for (int j = 0; j < sharewithemail.length; j++) {
					sharewithemail_plain += sharewithemail[j] + ", ";
				}
				sharewithemail_plain = sharewithemail_plain.substring(0, sharewithemail_plain.length() - 2);
			}
			inReq.setRequestParameter("sharewithemail.values", sharewithemail_plain);
		}
		

		Collection tosave = new ArrayList(Arrays.asList(fields));
		tosave.remove("status");
		fields = (String[])tosave.toArray(new String[tosave.size()]);
		searcher.updateData(inReq, fields, order);
		searcher.saveData(order, inReq.getUser());
		return order;
	}
	
	public Order rebuildShareOrder(WebPageRequest inReq) throws Exception
	{
		Order order = loadOrder(inReq);
		if (order != null)
		{
			String catalogid = inReq.findPathValue("catalogid");
			//Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
			
			HitTracker items = findOrderItems(inReq);
			Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
			itemsearcher.deleteAll(items, inReq.getUser());

			//TODO: Update existing items or/and delete old deleted records

		}
		return order;
	}

	public Order saveOrder(WebPageRequest inReq, boolean saveitems) throws Exception
	{
		Order order = loadOrder(inReq);
		if (order != null)
		{
			String catalogid = inReq.findPathValue("catalogid");
			order = getOrderManager(inReq).createOrder(catalogid, inReq, saveitems);
			inReq.putPageValue("savedok", "true");
		}
		return order;
	}

	public HitTracker findOrdersForUser(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		User owner = (User) inReq.getPageValue("owner");
		if (owner == null)
		{
			owner = inReq.getUser();
		}
		HitTracker orders = getOrderManager(inReq).findOrdersForUser(catalogid, owner);
		inReq.putPageValue("orders", orders);
		inReq.putPageValue("searcher", getSearcherManager().getSearcher(catalogid, "order"));
		
		return orders;
	}

	
	public HitTracker findOrderItems(WebPageRequest inReq)
	{
		Order order = loadOrder(inReq);
		if (order != null)
		{
			String catalogid = inReq.findPathValue("catalogid");
			String orderid = order.getId();
			if (orderid == null)
			{
				orderid = inReq.getRequestParameter("orderid");
			}
			HitTracker items = getOrderManager(inReq).findOrderItems(inReq, catalogid, order);
			inReq.putPageValue("orderitems", items);
			return items;
		}
		return null;
	}

	public void filterOrderItems(WebPageRequest inReq)
	{
		ArrayList<String> list = new ArrayList<String>(); //add omitted orders to a list
		String publishtype = inReq.getRequestParameter("publishdestination.value");
		String catalogid = inReq.findPathValue("catalogid");
		HitTracker items = (HitTracker) inReq.getPageValue("orderitems");
		if (items == null)
		{
			Order order = loadOrder(inReq);
			if (order != null)
			{
				String orderid = order.getId();
				if (orderid == null)
				{
					orderid = inReq.getRequestParameter("orderid");
				}
				items = getOrderManager(inReq).findOrderItems(inReq, catalogid, order);
			}
		}
		if (items != null)
		{
			//get searchers
			Searcher publishdestsearcher = getMediaArchive(inReq).getSearcherManager().getSearcher(catalogid, "publishdestination");
			Data publisher = (Data) publishdestsearcher.searchById(publishtype);
			String publishername = publisher.getName();
			Searcher convertpresetsearcher = getMediaArchive(inReq).getSearcherManager().getSearcher(catalogid, "convertpreset");
			//see if convertpreset has the appropriate field
			String publishtofield = "publishto" + publishername.replace(" ", "").toLowerCase();
			if (convertpresetsearcher.getDetail(publishtofield) != null)//field is present
			{
				for (int i = 0; i < items.size(); i++)
				{
					Data data = items.get(i);
					Asset asset = getMediaArchive(inReq).getAsset(data.get("assetid"));
					String fileformat = asset.get("fileformat");
					String rendertype = getMediaArchive(inReq).getMediaRenderType(fileformat);
					//build query
					SearchQuery presetquery = convertpresetsearcher.createSearchQuery();
					presetquery.append(publishtofield, "true").append("inputtype", rendertype);
					//execute query
					HitTracker hits = convertpresetsearcher.search(presetquery);
					if (hits.size() > 0)
						continue;
					list.add(asset.getId());
				}
			}
		}
		inReq.putPageValue("invaliditems", list);//process this in step2
	}

	public HitTracker findOrderAssets(WebPageRequest inReq)
	{
		Order order = loadOrder(inReq);
		if (order != null)
		{
			String catalogid = inReq.findPathValue("catalogid");
			String orderid = order.getId();
			if (orderid == null)
			{
				orderid = inReq.getRequestParameter("orderid");
			}
			HitTracker items = getOrderManager(inReq).findAssets(inReq, catalogid, order);
			inReq.putPageValue("orderassets", items);
			inReq.putPageValue("hits", items);

			return items;
		}
		return null;
	}


	public boolean checkItemApproval(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String orderid = inReq.getRequestParameter("orderid");
		
		Order order = getOrderManager(inReq).loadOrder(archive.getCatalogId(), orderid);
		if( order == null)
		{
			return false;
		}
		
		//Check expired
		Date expireson = order.getDate("expireson");
		if( expireson == null )
		{
			Date date = order.getDate("date");
			expireson = new Date(date.getTime() + (1000L * 60L * 60L * 24L * 30L));
		}
		
		Date today = new Date();
		if( today.after( expireson) )
		{
			log.error("Order is expired " + orderid);
			return false;
		}
		String status = order.get("checkoutstatus");
		if( status != null && status.equals("approved"))
		{
			return true;
		}
		return false;
//		Asset asset = (Asset) inReq.getPageValue("asset");
//		String sourcepath = null;
//		if (asset != null)
//		{
//			sourcepath = asset.getSourcePath();
//		}
//		else
//		{
//			sourcepath = archive.getSourcePathForPage(inReq);
//		}
//		if (sourcepath == null)
//		{
//			return false;
//		}
//		Searcher itemsearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");
//		SearchQuery search = itemsearcher.createSearchQuery();
//		search.addExact("userid", inReq.getUser().getId());
//		search.addExact("assetsourcepath", sourcepath);
//		search.addMatches("status", "approved");
//		HitTracker results = itemsearcher.search(search);
//		if (results.size() > 0)
//		{
//			return true;
//		}
//		return false;
	}

	public void removeSelectionFromOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = loadOrderBasket(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		HitTracker assets = (HitTracker) inReq.getSessionValue(hitssessionid);

		for (Iterator iterator = assets.getSelectedHitracker().iterator(); iterator.hasNext();)
		{

			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive(archive.getCatalogId()).getAsset(hit.getId());
			getOrderManager(inReq).removeItemFromOrder(archive.getCatalogId(), basket, asset);
		}
		inReq.removeSessionValue(hitssessionid);
		loadAssets(inReq);
	}

	public void toggleItemInOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = loadOrderBasket(inReq);
		String assetid = inReq.getRequestParameter("assetid");

		Asset asset = archive.getAsset(assetid);
		inReq.putPageValue("asset", asset);
		getOrderManager(inReq).toggleItemInOrder(archive, basket, asset);
	}

	public Data addItemToOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = loadOrderBasket(inReq);
		String[] assetids = inReq.getRequestParameters("assetid");
		String[] fields = inReq.getRequestParameters("field");
		Map props = new HashMap();
		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String key = fields[i];
				String value = inReq.getRequestParameter(key + ".value");
				props.put(key, value);
			}
		}
		
		for (int i = 0; i < assetids.length; i++)
		{
			String assetid = assetids[i];
			Asset asset = archive.getAsset(assetid);
			getOrderManager(inReq).addItemToOrder(archive.getCatalogId(), basket, asset, props);
		}

		return basket;
	}

	public Data addSelectionsToOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = loadOrderBasket(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		HitTracker assets = (HitTracker) inReq.getSessionValue(hitssessionid);

		String[] fields = inReq.getRequestParameters("field");
		Map props = new HashMap();
		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String key = fields[i];
				String value = inReq.getRequestParameter(key + ".value");
				props.put(key, value);
			}
		}

		int added = getOrderManager(inReq).addItemsToBasket(inReq, archive, basket, assets.getSelectedHitracker(), props);
		inReq.putPageValue("added", Integer.valueOf(added));
		return basket;
	}

	public Order loadOrderBasket(WebPageRequest inReq)
	{
		Order basket = null;
		try
		{
			MediaArchive archive = getMediaArchive(inReq);
			basket = (Order) inReq.getPageValue("orderbasket");
	
			if (basket == null)
			{
				String id = inReq.getUserName() + "_orderbasket";
				String appid = inReq.findValue("applicationid");
				Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "order");
					basket = (Order) searcher.searchById(id);
					if (basket == null)
					{
						basket = getOrderManager(inReq).createNewOrder(appid, archive.getCatalogId(), inReq.getUserName());
						basket.setId(id);
						basket.setProperty("ordertype", "basket");
						getOrderManager(inReq).saveOrder(archive.getCatalogId(), inReq.getUser(), basket);
					}
					basket.setProperty("basket", "true");
					basket.setProperty("ordertype", "basket");
		
					inReq.putSessionValue("orderbasket", basket);
			}
			inReq.putPageValue("order", basket);
			inReq.putPageValue("orderbasket", basket);
	
			HitTracker items = (HitTracker)inReq.getPageValue("orderitems");
			if( items == null)
			{
				items = loadOrderManager(inReq).findOrderItems(inReq, archive.getCatalogId(), basket);
			}
			if( items != null)
			{
				inReq.putPageValue("orderitems", items);
				inReq.putSessionValue(items.getSessionId(), items);
			}	
		}
		catch ( Throwable ex )
		{
			log.error(ex);
		}

		return basket;
	}

	public HitTracker loadAssets(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		Order order = loadOrder(inReq);
		if (order != null) {
			HitTracker re = getOrderManager(inReq).findAssets(inReq, catalogid, order);
			
			if( re != null)
			{
				re.setHitsPerPage(500);
				inReq.putPageValue("orderassets", re);
				inReq.putSessionValue(re.getSessionId(), re);
				return re;
			}
		}
		return null;
	}
	
	public void preprocessOrder(WebPageRequest inReq)
	{		
//		String [] orderids = inReq.getRequestParameters("itemid");
//		if( orderids != null)
//		{
//			for(String orderid:orderids)
//			{
//				String formatkey = new StringBuilder().append(orderid).append(".itemfiletype.value").toString();
//				
//				
//				if (!inReq.getParameterMap().containsKey(formatkey)){
//					continue;
//				}
//				String format = inReq.getParameterMap().get(formatkey).toString();
//				String presetkey = new StringBuilder().append(format).append(".presetid.value").toString();
//				if (!inReq.getParameterMap().containsKey(presetkey)){
//					continue;
//				}
//				String preset = inReq.getParameterMap().get(presetkey).toString();
//				String itempresetkey = new StringBuilder().append(orderid).append(".presetid.value").toString();
//				inReq.setRequestParameter(itempresetkey, preset);
//			}
//		}
	}

	public void createConversionAndPublishRequest(WebPageRequest inReq)
	{

		// Order and item should be created from previous step.
		// now we get the items and update the destination information
		Order order = loadOrder(inReq);

		OrderManager manager = getOrderManager(inReq);
		String catalogid = inReq.findPathValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
		if (order == null)
		{
			order = (Order) searcher.createNewData();
		}
		String[] fields = inReq.getRequestParameters("field");
		searcher.updateData(inReq, fields, order);

		MediaArchive archive = getMediaArchive(inReq);
		Map params = inReq.getParameterMap();
		if (order.get("publishdestination") == null)
		{
			String publishdestination = inReq.findValue("publishdestination.value");
			if(publishdestination != null){
				order.setProperty("publishdestination", publishdestination);//assume 0 for most orders, 0 can be told to use Aspera
			} else{
			//do something? default it to browser?
			order.setProperty("publishdestination", "0");//assume 0 for most orders, 0 can be told to use Aspera
			}
		}
		List assetids = manager.addConversionAndPublishRequest(inReq, order, archive, params, inReq.getUser());
		// OrderHistory history =
		// getOrderManager(inReq).createNewHistory(archive.getCatalogId(), order,
		// inReq.getUser(), "pending");
		// history.setAssetIds(assetids);
		// manager.saveOrderWithHistory(archive.getCatalogId(), inReq.getUser(),
		// order, history);
//		if (assetids.size() > 0)
//		{
//			order.setProperty("orderstatus", "ordered"); //what is pendig
//		}
//		manager.saveOrder(archive.getCatalogId(), inReq.getUser(), order);
		log.info("Added conversion and publish requests for order id:" + order.getId());
	}
	/**
	 * Is this needed?
	 * @deprecated
	 * @param inReq
	 */
	public void createQuickOrder(WebPageRequest inReq)
	{


		MediaArchive archive = getMediaArchive(inReq);
		
		OrderManager manager = getOrderManager(inReq);
		String catalogid = inReq.findPathValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");

		Order order = (Order) searcher.createNewData();
		order.setProperty("userid", inReq.getUserName());
		String quickpublishid = inReq.getRequestParameter("quickid");
		Data publishtemplate = archive.getData("quickpublish", quickpublishid);
		
		order.setProperty("publishdestination", publishtemplate.get("publishdestination"));//assume 0 for most orders, 0 can be told to use Aspera
		searcher.saveData(order, inReq.getUser());
		
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
		for (Iterator iterator = hits.getSelectedHitracker().iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Data item = itemsearcher.createNewData();
			item.setProperty("presetid", publishtemplate.get("convertpreset"));
			item.setProperty("assetid", hit.getId());
			item.setProperty("orderid", order.getId());

			itemsearcher.saveData(item, inReq.getUser());
					
		}
		
		
		
		List assetids = manager.addConversionAndPublishRequest(inReq, order, archive, new HashMap(), inReq.getUser());
		// OrderHistory history =
		// getOrderManager(inReq).createNewHistory(archive.getCatalogId(), order,
		// inReq.getUser(), "pending");
		// history.setAssetIds(assetids);
		// manager.saveOrderWithHistory(archive.getCatalogId(), inReq.getUser(),
		// order, history);
//		if (assetids.size() > 0)
//		{
//			order.setProperty("orderstatus", "ordered");
//		}
//		manager.saveOrder(archive.getCatalogId(), inReq.getUser(), order);
		inReq.putPageValue("order", order);
		inReq.putPageValue("data", order);

		log.info("Added conversion and publish requests for order id:" + order.getId());
	}
	
	

	public Order placeOrderById(WebPageRequest inReq)
	{
		Order order = loadOrder(inReq);
		getOrderManager(inReq).placeOrder(inReq, getMediaArchive(inReq), order, false);

		inReq.removeSessionValue("orderbasket");
		inReq.putPageValue("order", order);
		return order;
		// change the status of all the items and the order and save everything
		// fire event
		

	}

	public Order placeOrderFromBasket(WebPageRequest inReq)
	{
		Order order = loadOrderBasket(inReq);
		boolean resetid = Boolean.parseBoolean(inReq.findValue("resetid"));
		String prefix = inReq.findValue("subjectprefix");
		if (prefix == null)
		{
			prefix = "Order received:";
		}
		prefix = prefix + " " + order.getId();

		String postfix = inReq.findValue("subjectpostfix");
		if (postfix != null)
		{
			prefix = prefix + " " + postfix;
		}
		inReq.putPageValue("subject", prefix);

		getOrderManager(inReq).placeOrder(inReq, getMediaArchive(inReq), order, resetid);

		inReq.removeSessionValue("orderbasket");
		inReq.putPageValue("order", order);
		return order;
	}

	public Order createOrderWithItems(WebPageRequest inReq)
	{
		String orderid = inReq.findValue("orderid");
		if (orderid != null)
		{
			return loadOrder(inReq);
		}
		String[] assetids = inReq.getRequestParameters("assetid");
		if (assetids != null && assetids.length > 0 && assetids[0].length() > 0 && !assetids[0].startsWith("multiedit:hits"))
		{
			return createOrderFromAssets(inReq);
		}
		return createOrderFromSelections(inReq);
	}

	public Order loadOrderFromAssets(WebPageRequest inReq)
	{
		String catalogId = inReq.findPathValue("catalogid");
		MediaArchive archive = getMediaArchive(catalogId);
		String[] selectedids = inReq.getRequestParameters("assetid");

		List assetids = selectAssets(inReq, selectedids);
		Order order = getOrderManager(inReq).findOrderFromAssets(catalogId,inReq.getUser(),assetids);
		if( order == null)
		{
			order = createOrderFromAssets(inReq);
		}
		inReq.putPageValue("order", order);
		return order;
		
	}

	protected List selectAssets(WebPageRequest inReq, String[] selectedids)
	{
		List assetids = new ArrayList();
		if(selectedids == null) {
			String hitssessionid = inReq.getRequestParameter("hitssessionid");
			HitTracker assets = null;
			if (hitssessionid != null)
			{
				assets = (HitTracker) inReq.getSessionValue(hitssessionid);
				if (assets != null && assets.hasSelections())
				{
					for (Iterator iterator = assets.getSelectedHitracker().iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						assetids.add(data.getId());
					}
				}
			}
		}
		else {
			for (int i = 0; i < selectedids.length; i++)
			{
				String id = selectedids	[i];
				if (id.startsWith("multiedit:hits"))
				{
					HitTracker hits = (HitTracker) inReq.getSessionValue(id.substring("multiedit:".length()));
					if (hits != null)
					{
						for (Iterator iterator = hits.iterator(); iterator.hasNext();)
						{
							Data data = (Data) iterator.next();
							assetids.add(data.getId());
						}
					}
				}
				else
				{
					assetids.add(id);
				}
			}
		}
		return assetids;
	}
	public Order createOrderFromAssets(WebPageRequest inReq)
	{
		String catalogId = inReq.findPathValue("catalogid");
		MediaArchive archive = getMediaArchive(catalogId);
		String[] selectedids = inReq.getRequestParameters("assetid");
		

		
		Order order = getOrderManager(inReq).createNewOrder(inReq.findValue("applicationid"), catalogId, inReq.getUserName());

		List assetids = selectAssets(inReq, selectedids);

		for (Iterator iterator = assetids.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Asset asset = archive.getAsset(id);
			getOrderManager(inReq).addItemToOrder(catalogId, order, asset, null);
		}
		order.setValue("orderassetids",assetids);
		
		order.setProperty("downloadedstatus", "new");
		
		String desktopid = inReq.findValue("desktop"); 
		if(desktopid != null) {
			order.setProperty("desktopid", desktopid);
		}
		
		getOrderManager(inReq).saveOrder(catalogId, inReq.getUser(), order);
		
		inReq.putPageValue("order", order);

		return order;
	}

//	public Order createOrderFromUpload(WebPageRequest inReq)
//	{
//		String catalogId = inReq.findPathValue("catalogid");
//		MediaArchive archive = getMediaArchive(catalogId);
//		Collection assets = (Collection) inReq.getPageValue("uploadedassets");
//
//		Order order = getOrderManager(inReq).createNewOrderWithId(inReq.findValue("applicationid"), catalogId, inReq.getUserName());
//		// order.setProperty("orderstatus", "newupload");
//		List assetids = new ArrayList();
//		for (Iterator iter = assets.iterator(); iter.hasNext();)
//		{
//			Asset asset = (Asset) iter.next();
//			assetids.add(asset.getId());
//			getOrderManager(inReq).addItemToOrder(catalogId, order, asset, null);
//		}
//		// Order history needs to be updated
//		OrderHistory history = getOrderManager(inReq).createNewHistory(catalogId, order, inReq.getUser(), "newupload");
//		history.setAssetIds(assetids);
//		getOrderManager(inReq).saveOrderWithHistory(catalogId, inReq.getUser(), order, history);
//
//		// getOrderManager(inReq).saveOrder(catalogId, inReq.getUser(), order);
//		inReq.putPageValue("order", order);
//
//		return order;
//	}

	public OrderManager loadOrderManager(WebPageRequest inReq)
	{
		inReq.putPageValue("orderManager", getOrderManager(inReq));
		return getOrderManager(inReq);
	}

//	public Data addUserStatus(WebPageRequest inReq) throws Exception
//	{
//		Order order = loadOrder(inReq);
//		if (order != null)
//		{
//			String catalogid = inReq.findPathValue("catalogid");
//			String[] fields = inReq.getRequestParameters("field");
//			String userstatus = inReq.findValue("userstatus.value");
//			OrderHistory history = getOrderManager(inReq).createNewHistory(catalogid, order, inReq.getUser(), userstatus);
//
//			Searcher searcher = getSearcherManager().getSearcher(catalogid, "orderhistory");
//			searcher.updateData(inReq, fields, history);
//
//			getOrderManager(inReq).saveOrderWithHistory(catalogid, inReq.getUser(), order, history);
//
//		}
//		return order;
//	}
	/**
	 * Update the history of pending orders
	 * @param inReq
	 * @throws Exception
	 */
	public void updatePendingOrders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order order = loadOrder(inReq);
		if (order != null)
		{
			getOrderManager(inReq).updateStatus(archive, order);
		}
		else
		{
			getOrderManager(inReq).updatePendingOrders(archive);
		}
	}
	
	
	public void updateReadyStatusOrder(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order order = loadOrder(inReq);
		if (order != null)
		{
			getOrderManager(inReq).updateReadyStatus(archive, order);
		}
		else
		{
			//getOrderManager(inReq).updatePendingOrders(archive);
		}
	}
	
	/*
	public void updateDownloadedStatus(WebPageRequest inReq) throws Exception
	{
		String catalogId = inReq.findPathValue("catalogid");
		MediaArchive archive = getMediaArchive(catalogId);
		Order order = loadOrder(inReq);
		if (order != null)
		{
			order.setValue("downloadedstatus", "complete");
			getOrderManager(inReq).saveOrder(catalogId, inReq.getUser(), order);
		}
		else
		{
			//getOrderManager(inReq).updatePendingOrders(archive);
		}
	}
	*/

	public void clearOrderItems(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker items = findOrderItems(inReq);
		Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");
		for (Iterator iterator = items.iterator(); iterator.hasNext();)
		{
			Data item = (Data) iterator.next();
			searcher.delete(item, inReq.getUser());
		}

	}

	/*
	 * public Data createMultiEditData(WebPageRequest inReq) throws Exception {
	 * Order order = loadOrder(inReq); MediaArchive archive =
	 * getMediaArchive(inReq); HitTracker hits =
	 * getOrderManager(inReq).findAssets(inReq, archive.getCatalogId(), order);
	 * CompositeAsset composite = new CompositeAsset(); for (Iterator iterator =
	 * hits.iterator(); iterator.hasNext();) { Data target = (Data)
	 * iterator.next(); Asset p = null; if (target instanceof Asset) { p =
	 * (Asset) target; } else { String sourcepath = target.getSourcePath(); p =
	 * archive.getAssetBySourcePath(sourcepath); } if (p != null) {
	 * composite.addData(p); } } composite.setId("multiedit:" +
	 * hits.getHitsName()); // set request param?
	 * inReq.setRequestParameter("assetid", composite.getId());
	 * inReq.putPageValue("data", composite); inReq.putPageValue("asset",
	 * composite); inReq.putSessionValue(composite.getId(), composite);
	 * 
	 * return composite; }
	 */
	/*
	public void sendOrderEmail(WebPageRequest inReq)
	{
		// just a basic email download
		Order order = loadOrder(inReq);
		MediaArchive archive = getMediaArchive(inReq);
		String catalogid = archive.getCatalogId();
		String[] emails = inReq.getRequestParameters("sharewithemail.value");
		//String[] organizations = inReq.getRequestParameters("organization.value");
		HitTracker orderItems = getOrderManager(inReq).findOrderItems(inReq, archive.getCatalogId(), order);
		inReq.putPageValue("orderitems", orderItems);
		inReq.putPageValue("order", order);

		try
		{

			OrderHistory history = getOrderManager(inReq).createNewHistory(catalogid, order, inReq.getUser(), "created");
			inReq.putPageValue("order", order);
			TemplateWebEmail mailer = getPostMail().getTemplateWebEmail();
			String templatepage = inReq.findValue("sharetemplate");
			Page template = getPageManager().getPage(templatepage);
			mailer.loadSettings(inReq.copy(template));
			mailer.setMailTemplatePage(template);
			String subject = mailer.getWebPageContext().findValue("subject");
			if (subject == null)
			{
				subject = "Share notification";
			}

			mailer.setSubject(subject);
			mailer.setRecipientsFromStrings(Arrays.asList(emails));
			mailer.send();

			getOrderManager(inReq).saveOrderWithHistory(catalogid, inReq.getUser(), order, history);

		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}

	}
	*/
	public void deleteOrder(WebPageRequest inReq) throws Exception
	{
		Order order = loadOrder(inReq);
		String catalogid = inReq.findPathValue("catalogid");
		getOrderManager(inReq).delete(catalogid, order);
	}

	public void removeItem(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		String itemid = inReq.getRequestParameter("id");
		getOrderManager(inReq).removeItem(catalogid, itemid);
	}

	public Order loadOrderForVisitor(WebPageRequest inReq)
	{
		Order order = loadOrder(inReq);
		//check the expriation
		if (order.isExpired())
		{
			inReq.putPageValue("expired", Boolean.TRUE);
		}
		else
		{
			inReq.putPageValue("expired", Boolean.FALSE);
		}
		//load up hits and select all the items. 
		HitTracker hits = loadAssets(inReq);
		hits.selectAll();
		inReq.putPageValue("hits", hits);
		inReq.putSessionValue("vieworder", order);
		
		return order;
	}

	public Boolean canViewAsset(WebPageRequest inReq)
	{
//		String orderid = inReq.getRequestParameter("orderid");
//		if (orderid == null)
//		{
//			return false;
//		}
		Order order = loadOrder(inReq);
		if( order == null)
		{
			order = (Order)inReq.getSessionValue("vieworder");
		}
		if( order == null )
		{
			return false;
		}
		Asset asset = getAsset(inReq);
		if( asset == null)
		{
			log.info("Asset not found");
			return false;
		}
		String catalogid = inReq.findPathValue("catalogid");
		HitTracker assets = getOrderManager(inReq).findOrderItems(inReq, catalogid, order);
		int found = assets.findRow("assetid", asset.getId());
		if( found > -1 && !order.isExpired() )
		{
			return true;
		}
		return false;
	}
	
	public Data checkoutCart(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		String applicationid = inReq.findValue("applicationid");
		MediaArchive archive = getMediaArchive(inReq);

		Order basket = loadOrderBasket(inReq);

		Order order = (Order) getOrderManager(inReq).createNewOrder(applicationid, catalogid, inReq.getUserName());
		order.setValue("ordertype", "checkout");
		order.setValue("orderstatus", "processing");
		order.setValue("checkoutstatus", "pending");
		
		if( inReq.getUser().getEmail() == null)
		{
			throw new OpenEditException("Please set an email address");
		}
		order.setValue("sharewithemail", inReq.getUser().getEmail());
		
		String[] fields = inReq.getRequestParameters("field");
		archive.getSearcher("order").updateData(inReq, fields, order);
		
		order.setValue("date", new Date());
		//Expiration
		
		inReq.putPageValue("order", order);

		//OrderHistory history = getOrderManager(inReq).createNewHistory(catalogid, order, inReq.getUser(), "newrecord");
		
		String presetid = inReq.getRequestParameter("presetid");
		if( presetid == null)
		{
			presetid = "0";
		}
		
		inReq.setRequestParameter("orderid", order.getId());
		
		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
		HitTracker basketitems = getOrderManager(inReq).findOrderItems(inReq, catalogid, basket);
		List tosave = new ArrayList();
		
		for (Iterator iterator = basketitems.iterator(); iterator.hasNext();)
		{
			Data orderitem = (Data) iterator.next();
			orderitem =  itemsearcher.loadData(orderitem);
			orderitem.setValue("orderid", order.getId());
			orderitem.setValue("presetid", presetid); //for now
			String assetid = orderitem.get("assetid");
			Asset asset = archive.getAsset(assetid);
			//Save the publishqueue
			//Data publishqueue = getOrderManager(inReq).createPublishQueue(archive,inReq.getUser(),asset,"0","0");
			
			//orderitem.setValue("publishqueueid",publishqueue.getId());
			
			tosave.add(orderitem);
		}
		itemsearcher.saveAllData(tosave, null);
		//order.setValue("emailsent", true);
		getOrderManager(inReq).saveOrder(catalogid, inReq.getUser(), order);

		UserManager userManager = getUserManager(inReq);

		//Send an email
		getOrderManager(inReq).sendEmailForApproval(catalogid, archive, userManager, applicationid, order);
		
		return order;
	}
	
	
	
	public void loadPresetOptionsForOrder(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker items = findOrderItems(inReq);
		Map<String,RenderTypeOptions> rendertypeoptions = new HashMap<String,RenderTypeOptions>();
		
		String categoryid = inReq.getRequestParameter("categoryid");
		
		Category startingpoint = archive.getCategory(categoryid);
		
		for (Iterator iterator = items.iterator(); iterator.hasNext();)
		{
			Data item = (Data) iterator.next();
			Asset asset = archive.getAsset(item.get("assetid"));
			String fileformat = asset.getFileFormat();
			String rendertypeid = null;
			if(fileformat != null)
			{
				rendertypeid = archive.getMediaRenderType(fileformat);
			}
			else
			{
				rendertypeid = "none";
			}	
			RenderTypeOptions option = rendertypeoptions.get(rendertypeid);
			if( option == null)
			{
				option = new RenderTypeOptions();
				Data rendertype = archive.getCachedData("rendertype",rendertypeid);
				option.setRenderType(rendertype);
				rendertypeoptions.put(option.getRenderTypeId(),option);

				Collection presets = archive.query("convertpreset").or().exact("inputtype",rendertypeid).exact("inputtype","all").sort("ordering").search();
				for (Iterator iterator2 = presets.iterator(); iterator2.hasNext();)
				{
					Data preset = (Data) iterator2.next();
					PresetOption presetoption = new PresetOption();
					presetoption.setPreset(preset);
					option.addPresetOption(presetoption);
				}
			}
			for (Iterator iterator2 = option.getPresetOptions().iterator(); iterator2.hasNext();)
			{
				PresetOption presetoption = (PresetOption) iterator2.next();
				presetoption.addOrderItem(item); 
				String downloadpath = archive.asLinkToDownload(asset, presetoption.getPreset());
				String exportname = org.openedit.util.PathUtilities.extractFileName(downloadpath);
						//archive.asExportFileName(inReq.getUser(), asset, presetoption.getPreset());

				String savetopath = "";
				
				if( startingpoint != null)
				{
					for (Iterator iterator3 = asset.getCategories().iterator(); iterator3.hasNext();)
					{
						Category sub = (Category) iterator3.next();
						if( startingpoint.getId().equals(sub.getId()) )
						{
							savetopath = "";
							break; //do nothing
						}
						else if(sub.hasParent(startingpoint.getId())) 
						{
							savetopath = sub.getCategoryPath().substring(startingpoint.getCategoryPath().length());
							break;
						}
					}
				}
				if( presetoption.getPreset().getName().toLowerCase().contains("proxy") ) //Change to a setting 
				{
					savetopath = savetopath + "/proxy/" + exportname;
				}
				else
				{
					savetopath = savetopath + "/" + exportname;
				}
				//
				
				presetoption.addDownloadPath(downloadpath, savetopath, asset);
			}
		}
		inReq.putPageValue("rendertypeoptions",rendertypeoptions);
	}	

	public void refreshPendingOrders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//If there are pending. fire publish,conversion and order status events
		archive.fireSharedMediaEvent("conversions/runconversions");
		archive.fireSharedMediaEvent("publishing/publishassets"); //This calls update order status
		updatePendingOrders(inReq);
	}

	public void updateOrderItem(WebPageRequest inReq)
	{
		//just a basic email download
		MediaArchive archive = getMediaArchive(inReq);
		String orderitemid = inReq.getRequestParameter("orderitemid");
		String publishstatus = inReq.getRequestParameter("publishstatus");   //new readytopublish publishing complete
		String downloaditemdownloadedfilesize = inReq.getRequestParameter("downloaditemdownloadedfilesize");
		String publisheddate = inReq.getRequestParameter("publisheddate");
		String downloadstartdate = inReq.getRequestParameter("downloadstartdate");
		
		Data data = archive.getData("orderitem", orderitemid);
		if( data != null)
		{
			data.setValue("publishstatus",publishstatus); //publishexternal or complete
			if( downloaditemdownloadedfilesize != null)
			{
				data.setValue("downloaditemdownloadedfilesize",downloaditemdownloadedfilesize);
			}
			if(publisheddate != null)
			{
				data.setValue("publisheddate",DateStorageUtil.getStorageUtil().parseFromStorage(publisheddate));
			}
			if (downloadstartdate != null) 
			{
				data.setValue("downloadstartdate",DateStorageUtil.getStorageUtil().parseFromStorage(downloadstartdate));
			}
			archive.saveData("orderitem",data);
		}
		else
		{
			throw new OpenEditException("No such item");
		}
		Data order = archive.getData("order", data.get("orderid"));
		inReq.putPageValue("order",order);
		
		//String[] organizations = inReq.getRequestParameters("organization.value");
	}

	
	public void findActiveDownloadOrdersForUser(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		User owner = (User) inReq.getPageValue("owner");
		if (owner == null)
		{
			owner = inReq.getUser();
		}
		
		//Any pending orders?
		boolean yespending = getOrderManager(inReq).hasPendingDownloadForUser(inReq, catalogid, owner);
		inReq.putPageValue("haspendingorders", yespending);
		inReq.putPageValue("ordermananger",getOrderManager(inReq));
//		//Any complete order not yet show
//		HitTracker unshown = getOrderManager(inReq).findUnshownDownloadOrdersForUser(req, catalogid, owner);
//		req.putPageValue("showorders", unshown);
//		
//		//Browser should have shown order so flip flag
//		if (!unshown.isEmpty()) 
//		{
//			for (Iterator iterator = unshown.iterator(); iterator.hasNext();)
//			{
//				Data order = (Data) iterator.next();
//				order.setValue("downloadedstatus","complete");
//			}
//			getMediaArchive(req).getSearcher("order").saveAllData(unshown,null);
//			
//			req.putPageValue("showorder", unshown.first());
//		}
////			//already tracking an order?
////			Order order = (Order) req.getPageValue("order");
////			if (order == null) {
////				req.putPageValue("order", pendingorders.first());
////				//order.setValue("downloadedstatus", "shown"); 
////
////			}
////		}
//		
//		
//		//req.putPageValue("activeorders", orders);
//		//req.putPageValue("searcher", getSearcherManager().getSearcher(catalogid, "order"));
		
	}	
	
	public void findUserDownloadOrderItems(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		User owner = (User) inReq.getUser();
		if (owner == null)
		{
			owner = inReq.getUser();
		}
		Collection<OrderDownload> orders = getOrderManager(inReq).findDownloadOrdersForUser( inReq,  owner);

		int hitsperpage = 3;
		if(inReq.getRequestParameter("hitsperpage") != null)
		{
			hitsperpage = Integer.parseInt(inReq.getRequestParameter("hitsperpage"));
		}
		Collection openitems = new ArrayList();
		for (Iterator iterator = orders.iterator(); iterator.hasNext();)
		{
			OrderDownload download = (OrderDownload) iterator.next();
			for (Iterator iterator2 = download.getItemList().iterator(); iterator2.hasNext();)
			{
				Data orderitem = (Data) iterator2.next();
				String status = orderitem.get("publishstatus");
				if("readytopublish".equals(status) || "publishingexternal".equals(status))
				{
					openitems.add(orderitem);
				}
				if( openitems.size() >= hitsperpage)
				{
					break;
				}
			}
			if( openitems.size() >= hitsperpage)
			{
				break;
			}
		}
		inReq.putPageValue("downloaditems", openitems);
		
	}
	
}
