/*
 * Created on May 2, 2006
 */
package org.entermediadb.asset.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.MediaArchiveModule;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderManager;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.OutputFiller;
import org.openedit.util.ZipUtil;

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
			
			Set<String> fileset = new HashSet<String>();//set of unique filenames
			StringBuilder buf = new StringBuilder();//some accounting of filenames that are renamed
			
			for (Iterator iterator = orderitems.iterator(); iterator.hasNext();) {
				Data orderitem = (Data) iterator.next();
				Data preset = archive.getSearcherManager().getData(catalogid, "convertpreset", orderitem.get("presetid"));
				if(preset == null && "original".equals(orderitem.get("presetid"))){
					preset = archive.getSearcherManager().getData(catalogid, "convertpreset", "0");
				}
				
				String queid = orderitem.get("publishqueueid");
				if( queid == null )
				{
					log.error("publishqueueid should never be null, 0 for browser");
					continue;
				}
				
				Data publishtask = archive.getSearcherManager().getData(catalogid, "publishqueue", queid);
				Asset asset = archive.getAssetBySourcePath(orderitem.get("assetsourcepath"));
				
				ContentItem target = null;
				String filename = null;
				if(publishtask != null){
					filename = publishtask.get("exportname");
				}
				
				if(filename == null){
					filename = asset.getPrimaryFile();
				}
				if(filename == null){
					throw new OpenEditException("Filename was not set on publish task:  " + publishtask.getId());					
				}
				//handle duplicate filenames
				if (fileset.contains(filename)){
					buf.append(filename).append("\t").append(asset.getSourcePath()).append("\t");
					String filetype = "";
					if (filename.contains(".")){
						filetype = filename.substring(filename.lastIndexOf("."));
						filename = filename.substring(0,filename.lastIndexOf("."));
					}
					filename = new StringBuilder()
						.append(filename).append("_")
						.append(String.valueOf( (int) (Math.random() * 1000)))
						.append(filetype)
						.toString();
					buf.append(filename).append("\n");
					fileset.add(filename);
				} else {
					fileset.add(filename);
				}
				if(preset.getId().equals("0")){
					 target = archive.getOriginalContent(asset);
					 util.addTozip(target,filename , zos);
				}
				else{
					String pathToFile = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + orderitem.get("assetsourcepath") + "/" + preset.get("generatedoutputfile");
					target = archive.getPageManager().getContent(pathToFile);
					util.addTozip(target,filename , zos);
				}

				
				
				
				
				
			 	// <a class="btn small" href="$home/${applicationid}/views/modules/asset/downloads/generated/${asset.sourcepath}/${convertpreset.outputfile}/${publishqueue.exportname}">Download</a>
				
			}
			
			if (!buf.toString().isEmpty()){
				util.addTozip("Original Name\tSource Path\tNew Name\n"+buf.toString(),"files.txt",zos);
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
