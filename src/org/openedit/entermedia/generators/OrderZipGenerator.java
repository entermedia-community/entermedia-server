/*
 * Created on May 2, 2006
 */
package org.openedit.entermedia.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.MediaArchiveModule;
import org.openedit.entermedia.orders.Order;
import org.openedit.entermedia.orders.OrderManager;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.BaseGenerator;
import com.openedit.generators.Output;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.util.OutputFiller;
import com.openedit.util.ZipUtil;

public class OrderZipGenerator extends BaseGenerator
{

	private static final Log log = LogFactory.getLog(OrderZipGenerator.class);
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager moduleManager)
	{
		fieldModuleManager = moduleManager;
	}

	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		ZipOutputStream zos = null;

		try {

			MediaArchiveModule archiveModule = (MediaArchiveModule) getModuleManager().getBean("MediaArchiveModule");
			MediaArchive archive = archiveModule.getMediaArchive(inReq);
			
			String orderid = inReq.getRequestParameter("orderid");
			
			OrderManager manager = (OrderManager) archive.getModuleManager().getBean(archive.getCatalogId(), "orderManager"); 
					
			Order order = manager.loadOrder(archive.getCatalogId(), orderid);
			
			HitTracker orderitems = manager.findOrderItems(inReq, archive.getCatalogId(), orderid);
			String catalogid = archive.getCatalogId();
			
			

			ZipUtil util = new ZipUtil();
			zos = new ZipOutputStream(inOut.getStream());
			zos.setLevel(1); // for speed since these are jpegs
			OutputFiller filler = new OutputFiller();
			List<Asset> missing = new ArrayList<Asset>();
			List<Asset> okAssets = new ArrayList<Asset>();
			
			
			for (Iterator iterator = orderitems.iterator(); iterator.hasNext();) {
				Data orderitem = (Data) iterator.next();
				Data preset = archive.getSearcherManager().getData(catalogid, "convertpreset", orderitem.get("presetid"));
				Data publishtask = archive.getSearcherManager().getData(catalogid, "publishqueue", orderitem.get("publishqueueid"));
				Asset asset = archive.getAssetBySourcePath(orderitem.get("assetsourcepath"));
				//TODO:  Handle duplicate filenames
				Page target = null;
				String filename = publishtask.get("exportname");
				if(filename == null){
					throw new OpenEditException("Filename was not set on publish task:  " + publishtask.getId());					
				}
				if(preset.getId().equals("0")){
					 target = archive.getOriginalDocument(asset);				
					 util.addTozip(target.getContentItem(),filename , zos);
				}
				else{
					String pathToFile = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + orderitem.get("assetsourcepath") + "/" + preset.get("outputfile");
					target = archive.getPageManager().getPage(pathToFile);
					util.addTozip(target.getContentItem(),filename , zos);
				}

				
				
				
				
				
			 	// <a class="btn small" href="$home/${applicationid}/views/modules/asset/downloads/generated/${asset.sourcepath}/${convertpreset.outputfile}/${publishqueue.exportname}">Download</a>
				
			}
			
			
		} catch (IOException e) {
			throw new OpenEditException(e);
		} finally{
			try {
				zos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		
		
		
//		ZipGroup zip = new ZipGroup();
//		zip.setEnterMedia(archiveModule.getEnterMedia(inReq.findValue("applicationid")));
//		zip.setUser(inReq.getUser());
//		zip.zipItems(assets, inOut.getStream());

	}

	public boolean canGenerate(WebPageRequest inReq)
	{
	
		boolean ok =  inReq.getPage().getMimeType().equals("application/x-zip");
		return ok;
	}

}
