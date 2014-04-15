package model.push;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.PathUtilities;
import com.openedit.util.XmlUtil;

import em.model.push.MediaUploader;

public class BaseMediaUploader implements MediaUploader
{
	private static final Log log = LogFactory.getLog(BaseMediaUploader.class);
	protected XmlUtil xmlUtil = new XmlUtil();
	protected UserManager fieldUserManager;
	
	protected ThreadLocal perThreadCache = new ThreadLocal();
	
	public boolean uploadOriginal(MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser )
	{
		Page original = archive.getOriginalDocument(inAsset);

		List<ContentItem> filestosend = new ArrayList<ContentItem>();

		//If using orginal then grab all the generated media as well
		String path = "/WEB-INF/data/${archive.getCatalogId()}/generated/${inAsset.getSourcePath()}";
		
		readFiles( archive.getPageManager(), path, path, filestosend );
		
		String server = inPublishDestination.get("server");
		String catalogid = inPublishDestination.get("bucket");
		
		upload(archive, inAsset, server, inUser, catalogid, original, filestosend);
		
		return true;
	}
	
	public boolean uploadGenerated(MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser )
	{
		Page original = archive.getOriginalDocument(inAsset);

		List<ContentItem> filestosend = new ArrayList<ContentItem>();

		//If using orginal then grab all the generated media as well
		String path = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath();
		
		readFiles( archive.getPageManager(), path, path, filestosend );
		
		String server = inPublishDestination.get("server");
		String catalogid = inPublishDestination.get("bucket");
		
		upload(archive, inAsset, server, inUser, catalogid, original, filestosend);
		
		return true;
	}
	
	protected Map<String, String> upload(MediaArchive inArchive, Asset inAsset, String inServer, User inUser, String inCatalogId, Page inOriginal, List<ContentItem> inGeneratedFiles)
	{
		String url = inServer + "/media/services/rest/handlesync.xml?catalogid=" + inCatalogId;
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
			for (Iterator iterator = inGeneratedFiles.iterator(); iterator.hasNext();)
			{
				ContentItem file = (ContentItem) iterator.next();
				String name  =  PathUtilities.extractFileName( file.getPath() );
				FilePart part = new FilePart("file." + count, name, new File( file.getAbsolutePath() ));
				parts.add(part);
				count++;
			}
			if( inOriginal != null)
			{
				FilePart part = new FilePart("original", inOriginal.getName(), new File( inOriginal.getContentItem().getAbsolutePath() ));
				parts.add(part);
			}
			for (Iterator iterator = inAsset.getProperties().keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				if( !key.equals("libraries"))  //handled below
				{
					parts.add(new StringPart("field", key));
					parts.add(new StringPart(key+ ".value", inAsset.get(key)));
				}
			}
			parts.add(new StringPart("name", inAsset.getName()));
			parts.add(new StringPart("sourcepath", inAsset.getSourcePath()));
			parts.add(new StringPart("id", prefix + inAsset.getId()));
			
			if( inAsset.getKeywords().size() > 0 )
			{
				StringBuffer buffer = new StringBuffer();
				for (Iterator iterator = inAsset.getKeywords().iterator(); iterator.hasNext();)
				{
					String keyword = (String) iterator.next();
					buffer.append( keyword );
					if( iterator.hasNext() )
					{
						buffer.append('|');
					}
				}
				parts.add(new StringPart("keywords", buffer.toString() ));
			}
			Collection libraries =  inAsset.getLibraries();
			if(  libraries != null && libraries.size() > 0 )
			{
				StringBuffer buffer = new StringBuffer();
				for (Iterator iterator = inAsset.getLibraries().iterator(); iterator.hasNext();)
				{
					String keyword = (String) iterator.next();
					buffer.append( keyword );
					if( iterator.hasNext() )
					{
						buffer.append('|');
					}
				}
				parts.add(new StringPart("libraries", buffer.toString() ));
			}

			Part[] arrayOfparts = parts.toArray(new Part[0]);

			method.setRequestEntity(new MultipartRequestEntity(arrayOfparts, method.getParams()));
			
			Element root = execute(inServer,inUser, method);
			Map<String, String> result = new HashMap<String, String>();
			for (Object o : root.elements("asset"))
			{
				Element asset = (Element) o;
				result.put(asset.attributeValue("id"), asset.attributeValue("sourcepath"));
			}
			log.info("Sent ${inServer}/" + inAsset.getSourcePath() + " with " + inGeneratedFiles.size() + " generated files");
			return result;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	} 
	
	protected void readFiles(PageManager pageManager, String inRootPath,  String inPath, List<ContentItem> inFilestosend)
	{
		List paths = pageManager.getChildrenPaths(inPath);
		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			ContentItem item = pageManager.getRepository().get(path);
			if( item.isFolder() )
			{
				readFiles(pageManager, inRootPath, path, inFilestosend);
			}
			else
			{
				inFilestosend.add( item );
			}
		}
	}
	
	protected Element execute(String inServer, User inUser, HttpMethod inMethod)
	{
		try
		{
			return send(inServer,inUser, inMethod);
		}
		catch (Exception e)
		{
			log.error(e);
			//try logging in again?
			perThreadCache.remove();
		}
		try
		{
			return send(inServer,inUser, inMethod);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected Element send(String inServer,User inUser, HttpMethod inMethod) throws IOException, HttpException, Exception, DocumentException
	{
		return send(getClient(inServer, inUser), inMethod);
	}
	
	protected Element send(HttpClient inClient, HttpMethod inMethod) throws IOException, HttpException, Exception, DocumentException
	{
		int status = inClient.executeMethod(inMethod);
		if (status != 200)
		{
			throw new Exception(" ${inMethod} Request failed: status code ${status}");
		}
		Element result = xmlUtil.getXml(inMethod.getResponseBodyAsStream(),"UTF-8");
		return result;
	}
	
	public HttpClient getClient(String inServer, User inUser)
	{
		HttpClient ref = (HttpClient) perThreadCache.get();
		if (ref == null)
		{
			if( ref == null)
			{
				ref = login(inServer, inUser);
				// use weak reference to prevent cyclic reference during GC
				perThreadCache.set(ref);
			}
		}
		return ref;
	}
	
	public HttpClient login(String inServer, User inUser)
	{
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(inServer + "/media/services/rest/login.xml");

		//TODO: Support a session key and ssl
		String account = inUser.getUserName();
		String password = getUserManager().decryptPassword(getUserManager().getUser(account));
		method.addParameter("accountname", account);
		method.addParameter("password", password);
		try
		{
			int status = client.executeMethod(method);
			if (status != 200)
			{
				throw new Exception(" ${method} Request failed: status code ${status}");
			}
		}
		catch ( Exception ex )
		{
			throw new OpenEditException(ex);
		}
		log.info("Login sucessful");
		return client;
	}
	
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.push.PushManager#getUserManager()
	 */
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.push.PushManager#setUserManager(com.openedit.users.UserManager)
	 */
	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

}