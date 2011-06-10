import com.openedit.WebPageRequest 
import com.openedit.entermedia.scripts.EnterMediaObject 
import com.openedit.entermedia.scripts.ScriptLogger;
import com.openedit.page.Page 
import com.openedit.servlet.OpenEditEngine 
//import org.junit.Test 
import org.openedit.Data 
import org.openedit.entermedia.Asset 
import org.openedit.entermedia.MediaArchive 
import org.openedit.entermedia.modules.OrderModule 
import org.openedit.entermedia.orders.Order 


class Test extends EnterMediaObject
{
	public MediaArchive getMediaArchive()
	{
		return context.getPageValue("mediaarchive");
	}
	
	public OpenEditEngine getOpenEditEngine()
	{
		return (OpenEditEngine)getModuleManager().getBean("OpenEditEngine");
	}
	public boolean assertNotNull(Object inObj, String inMessage)
	{
		if( inObj == null)
		{
			log.info(inMessage + " was null");
			return false;
		}
		return true;
	}
	public boolean assertEquals(Object inWhat, Object inEquals)
	{
		if( !inWhat.equals(inEquals))
		{
			log.error(inWhat + " != " + inEquals);
			return false;
		}
		return true;
	}
	public boolean assertTrue(Object inCheck)
	{
		if(!Boolean.parseBoolean(String.valueOf( inCheck ) ) )
		{
			log.error("Not true: ${inCheck}");
			return false;
		}
		return true;
	}
	
	public WebPageRequest createPageRequest(String inPath)
	{
		Page page = getPageManager().getPage(inPath);
		return context.copy(page);
	}

	public void testEmailOrder() throws Exception
	{
		String appid = context.findValue("applicationid");
		WebPageRequest req = createPageRequest("/${appid}/views/activity/processorder.html");
		
		OrderModule om = (OrderModule)getMediaArchive().getModuleManager().getModule("OrderModule");
		
		String catalogid = getMediaArchive().getCatalogId();
		Order order = om.getOrderManager().createNewOrder(appid, catalogid, "admin");
		
		om.getOrderManager().saveOrder(catalogid, req.getUser(), order);

		Asset asset = getMediaArchive().getAsset(context.findValue("testassetid"));
		if( !assertNotNull(asset,"Asset was null") )
		{
			return;
		}
		Data item = om.getOrderManager().addItemToBasket(catalogid, order, asset, null);	
		
		req.setRequestParameter("orderid", order.getId());

		//null req.setRequestParameter("presetid.value", "1");
		//req.setRequestParameter("publishdestination.value", "1");
		req.setRequestParameter("searchtype", "order");
		req.setRequestParameter("field", [ "publishdestination", "sharewithemail","sharesubject","sharenote"] as String[] ); //order stuff
		req.setRequestParameter("sharewithemail.value", "cburkey@openedit.org");
		req.setRequestParameter("sharesubject.value", "Test email");
		req.setRequestParameter("sharenote.value", "Sent from a test. Would contain a link to ? ${asset.getId()}");
		
		req.setRequestParameter("itemid", item.getId());
		
		getOpenEditEngine().executePathActions(req);
		getOpenEditEngine().executePageActions(req);
		Thread.sleep(24000); //just had to call the events in order
		order = om.getOrderManager().loadOrder(catalogid, order.getId());
		Collection items = om.getOrderManager().findOrderAssets(catalogid, order.getId());
		if( !assertEquals(1, items.size()) )
		{
			return;
		}
		log.info('Order status was: ' + order.get("orderstatus"));		
		item = (Data)items.iterator().next();
		log.info('Item status was: ' + item.get("status"));		
			
		String emailsent = order.get("emailsent");

		if( !assertEquals("true",emailsent) )
		{
			return;
		}
		//orders are save in the data directory and there is an order and orderitem searcher
		log.info("test is green");
	}
	
	public boolean testPublishOrder() throws Exception
	{
		String appid = context.findValue("applicationid");
		
		WebPageRequest req = createPageRequest("/${appid}/views/activity/processorder.html");
		
		OrderModule om = (OrderModule)getModuleManager().getModule("OrderModule");
		
		String catalogid = getMediaArchive().getCatalogId();
		Order order = om.getOrderManager().createNewOrder(appid, catalogid, "admin");
		
		om.getOrderManager().saveOrder(getMediaArchive().getCatalogId(), req.getUser(), order);

		Asset asset = getMediaArchive().getAsset(context.findValue("testassetid"));
		Data item = om.getOrderManager().addItemToBasket(getMediaArchive().getCatalogId(), order, asset, null);	
		
		req.setRequestParameter("orderid", order.getId());

		req.setRequestParameter("field", [ "publishdestination","presetid"] as String[]); //order stuff
		req.setRequestParameter("publishdestination.value", "1");
		req.setRequestParameter("searchtype", "order");

		req.setRequestParameter("itemid", item.getId());
		req.setRequestParameter(item.getId() + ".presetid.value", "2"); //outputffmpeg.avi
		
		getOpenEditEngine().executePathActions(req);
		getOpenEditEngine().executePageActions(req);
		
		Thread.sleep(24000);

		Page page = getPageManager().getPage("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + asset.getSourcePath() + "/outputffmpeg.avi");
		if( !assertTrue(page.exists()) )
		{
			return;
		}
		
		order = om.getOrderManager().loadOrder(getMediaArchive().getCatalogId(), order.getId());
		Collection items = om.getOrderManager().findOrderAssets(getMediaArchive().getCatalogId(), order.getId());
		assertEquals(1, items.size());
		item = (Data)items.iterator().next();
		String emailsent = order.get("emailsent");
		
		if( !assertEquals("true",emailsent) )
		{
			return;
		}
		//orders are save in the data directory and there is an order and orderitem searcher
		log.info("test is green");
	}

	
	public boolean testPublishRhozetOrder() throws Exception
	{
		String appid = context.findValue("applicationid");
		String catalogid = getMediaArchive().getCatalogId();
		WebPageRequest req = createPageRequest("/${appid}/views/activity/processorder.html");

		OrderModule om = (OrderModule)getModuleManager().getModule("OrderModule");
		
		Order order = om.getOrderManager().createNewOrder(appid, catalogid, "admin");
		
		om.getOrderManager().saveOrder(catalogid, req.getUser(), order);

		Asset asset = getMediaArchive().getAsset(context.findValue("testassetid"));
		Data item = om.getOrderManager().addItemToBasket(catalogid, order, asset, null);	
		
		req.setRequestParameter("orderid", order.getId());

		req.setRequestParameter("field", ["publishdestination","presetid"] as String[]); //order stuff
		req.setRequestParameter("publishdestination.value", "1");
		req.setRequestParameter("searchtype", "order");

		req.setRequestParameter("itemid", item.getId());
		req.setRequestParameter(item.getId() + ".presetid.value", "rhozet-test"); //outputffmpeg.avi
		
		getOpenEditEngine().executePathActions(req);
		getOpenEditEngine().executePageActions(req);
		
		Thread.sleep(12000);
		
		WebPageRequest conversions = createPageRequest("/${catalogid}/events/conversions/runconversions.html");
		
		getOpenEditEngine().executePathActions(conversions);
		getOpenEditEngine().executePageActions(conversions);
		
		
		Thread.sleep(12000);
		Page page = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated/" + asset.getSourcePath() + "/h264rhozet.mp4");
		if(!assertTrue(page.exists()))
		{
			return;
		}
		
		order = om.getOrderManager().loadOrder(catalogid, order.getId());
		Collection items = om.getOrderManager().findOrderAssets(catalogid, order.getId());
		assertEquals(1, items.size());
		item = (Data)items.iterator().next();
		String emailsent = order.get("emailsent");
		if(!assertEquals("true",emailsent)){
			return;
		}
		
		//orders are save in the data directory and there is an order and orderitem searcher
		
		log.info("test is green");

	}
	
	public boolean testPublishAmazon() throws Exception
	{
		String appid = context.findValue("applicationid");
		String catalogid = getMediaArchive().getCatalogId();
		WebPageRequest req = createPageRequest("/${appid}/views/activity/processorder.html");

		OrderModule om = (OrderModule)getModuleManager().getModule("OrderModule");
		
		Order order = om.getOrderManager().createNewOrder(appid, catalogid, "admin");
		
		om.getOrderManager().saveOrder(catalogid, req.getUser(), order);

		Asset asset = getMediaArchive().getAsset(context.findValue("testassetid"));
		Data item = om.getOrderManager().addItemToBasket(catalogid, order, asset, null);
		
		req.setRequestParameter("orderid", order.getId());

		req.setRequestParameter("field", ["publishdestination","presetid"] as String[]); //order stuff
		req.setRequestParameter("publishdestination.value", "4");
		req.setRequestParameter("searchtype", "order");

		req.setRequestParameter("itemid", item.getId());
		req.setRequestParameter(item.getId() + ".presetid.value", "2"); //outputffmpeg.avi
		
		getOpenEditEngine().executePathActions(req);
		getOpenEditEngine().executePageActions(req);
		
		Thread.sleep(12000);
		
		WebPageRequest conversions = createPageRequest("/${catalogid}/events/conversions/runconversions.html");
		
		getOpenEditEngine().executePathActions(conversions);
		getOpenEditEngine().executePageActions(conversions);
		
		
		Thread.sleep(12000);
		Page page = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated/" + asset.getSourcePath() + "/h264rhozet.mp4");
		if(!assertTrue(page.exists()))
		{
			return;
		}
		
		order = om.getOrderManager().loadOrder(catalogid, order.getId());
		Collection items = om.getOrderManager().findOrderAssets(catalogid, order.getId());
		assertEquals(1, items.size());
		item = (Data)items.iterator().next();
		String emailsent = order.get("emailsent");
		if(!assertEquals("true",emailsent)){
			return;
		}
		
		//orders are save in the data directory and there is an order and orderitem searcher
		
		log.info("test is green");

	}
	
	public boolean testPublishSmartJog() throws Exception
	{
		String appid = context.findValue("applicationid");
		
		WebPageRequest req = createPageRequest("/${appid}/views/activity/processorder.html");
		
		OrderModule om = (OrderModule)getModuleManager().getModule("OrderModule");
		
		String catalogid = getMediaArchive().getCatalogId();
		Order order = om.getOrderManager().createNewOrder(appid, catalogid, "admin");
			
		om.getOrderManager().saveOrder(getMediaArchive().getCatalogId(), req.getUser(), order);

		Asset asset = getMediaArchive().getAsset(context.findValue("testassetid"));
		Data item = om.getOrderManager().addItemToBasket(getMediaArchive().getCatalogId(), order, asset, null);
		
		req.setRequestParameter("orderid", order.getId());

		req.setRequestParameter("field", [ "publishdestination","presetid"] as String[]); //order stuff
		req.setRequestParameter("publishdestination.value", "3");
		req.setRequestParameter("searchtype", "order");

		req.setRequestParameter("itemid", item.getId());
		req.setRequestParameter(item.getId() + ".presetid.value", "rhozet-test"); //outputffmpeg.avi
		
		getOpenEditEngine().executePathActions(req);
		getOpenEditEngine().executePageActions(req);
		
		Thread.sleep(12000);
		
	
		
		order = om.getOrderManager().loadOrder(getMediaArchive().getCatalogId(), order.getId());
		Collection items = om.getOrderManager().findOrderAssets(getMediaArchive().getCatalogId(), order.getId());
		if (!assertEquals(1, items.size()))
		{
			return;
		}
		
		item = (Data)items.iterator().next();
				
		String remotePath = item.get('remotePath');
		if (!assertTrue(remotePath != null))
		{
			log.info("Remote path not set on item.");
			return;
		}
		
		Page inputpage = getMediaArchive().getOriginalDocument(asset);
		
		String publishFile = "/WEB-INF/publish/smartjog/" + remotePath;
		Page publishPage = getMediaArchive().getPageManager().getPage(publishFile);
		
		if (!assertTrue(publishPage.exists()))
		{
			log.info("Could not find published file on remote server.");
			return;
		}
		
		String emailsent = order.get("emailsent");
		
		if( !assertEquals("true",emailsent) )
		{
			return;
		}
		//orders are save in the data directory and there is an order and orderitem searcher
		log.info("test is green");
	}
}
logs = new ScriptLogger();
logs.startCapture();
try
{
	Test test = new Test();
	test.setLog(logs);
	test.setContext(context);
	test.setModuleManager(moduleManager);
	test.setPageManager(pageManager);
	
	logs.info("<h2>testEmailOrder()</h2>")
	test.testEmailOrder();

	logs.info("<h2>testPublishOrder()</h2>")
	test.testPublishOrder();

	logs.info("<h2>testPublishRhozetOrder()</h2>")
	test.testPublishRhozetOrder();
	
	logs.info("<h2>testPublishAmazon()</h2>")
	test.testPublishAmazon();
	
	
	logs.info("<h2>testPublishSmartJog()</h2>")
	test.testPublishSmartJog();

	//logs.info("<h2>testPublishRhozetOrderToAspera()</h2>")
	//test.testPublishRhozetOrderToAspera();
	
}
finally
{
	logs.stopCapture();
}
context.putPageValue("messages",logs.getLogs());
