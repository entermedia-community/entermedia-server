package org.entermediadb.asset.sources;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

public class S3CmdAssetSource extends BaseAssetSource
{
	private static final Log log = LogFactory.getLog(S3CmdAssetSource.class);
	protected Exec fieldExec;
	protected FileUtils fieldFileUtils;
	
	public FileUtils getFileUtils()
	{
		if (fieldFileUtils == null)
		{
			fieldFileUtils = new FileUtils();
		}
		return fieldFileUtils;
	}

	public void setFileUtils(FileUtils inFileUtils)
	{
		fieldFileUtils = inFileUtils;
	}

	protected String getBucket()
	{
		return getConfig().get("bucket");
	}

	protected String getSecretKey()
	{
		return getConfig().get("secretkey");
	}

	protected String getAccessKey()
	{
		return getConfig().get("accesskey");
	}
	
	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec inExec)
	{
		fieldExec = inExec;
	}

	@Override
	public InputStream getOriginalDocumentStream(Asset inAsset)
	{
		ContentItem item = getOriginalContent(inAsset);
		return item.getInputStream();
	}

	protected File download(Asset inAsset, File file)
	{
		List cmd = new ArrayList();
		//aws s3 cp file.txt s3://
		cmd.add("s3");
		cmd.add("cp");
		
		String awskey = inAsset.getSourcePath().substring(getFolderPath().length() + 1);
		
		cmd.add("s3://" + getBucket() + "/" + awskey );
		cmd.add(file.getAbsolutePath());
		ExecResult res = getExec().runExec("aws", cmd,true);
		if( !res.isRunOk() )
		{
			throw new OpenEditException("Could not download " + res.getStandardOut() + " " + cmd + " " );
		}
		//How do we set the timestamp? From the asset?
		return file;
	}

	protected void upload(Asset inAsset, File file)
	{
		List cmd = new ArrayList();
		//aws s3 cp file.txt s3://
		cmd.add("s3");
		cmd.add("cp");

		cmd.add(file.getAbsolutePath());

		String awskey = inAsset.getSourcePath().substring(getFolderPath().length() + 1);
		
		cmd.add("s3://" + getBucket() + "/" + awskey );
		ExecResult res = getExec().runExec("aws", cmd,true);
		if( !res.isRunOk() )
		{
			throw new OpenEditException("Could not upload " + res.getStandardOut() + " " + cmd + " " );
		}
	}

	protected File getFile(Asset inAsset)
	{
		String sp = getSourcePath(inAsset);
		sp = sp.substring(getFolderPath().length() + 1);
		String abpath = getExternalPath() + "/" + sp;
		String primaryname = inAsset.getPrimaryFile();
		if(primaryname != null && inAsset.isFolder() )
		{
			abpath = abpath + "/" + primaryname;
		}

		return new File(abpath);
	}

	@Override
	public ContentItem getOriginalContent(Asset inAsset)
	{
		return getOriginalContent(inAsset,true);
	}
	public ContentItem getOriginalContent(Asset inAsset, boolean downloadifNeeded)
	{
		File file = getFile(inAsset);
		FileItem item = new FileItem(file);
		
		String path = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/";
		path = path + inAsset.getSourcePath(); //Check archived?
		
		String primaryname = inAsset.getPrimaryFile();
		if(primaryname != null && inAsset.isFolder() )
		{
			path = path + "/" + primaryname;
		}
		item.setPath(path);
		if(downloadifNeeded)
		{
			//Check it exists and it matches
			long size = inAsset.getLong("filesize");
			if( item.getLength() != size)
			{
				download(inAsset, file);
			}
		}
		
		return item;
	}

	@Override
	public boolean handles(Asset inAsset)
	{
		String name = getFolderPath();
		if( inAsset.getSourcePath().startsWith(name))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOriginal(Asset inAsset)
	{
		//aws s3 rm s3://mybucket/test2.txt
		List cmd = new ArrayList();
		//aws s3 cp file.txt s3://
		cmd.add("s3");
		cmd.add("rm");
		String sp = inAsset.getSourcePath();
		if( sp.length() < 4)
		{
			throw new OpenEditException("Invalid sourcepath: " +sp);
		}
		String awskey = sp.substring(getFolderPath().length() + 1);
		cmd.add("s3://" + getBucket() + "/" + awskey );
		ExecResult res2 = getExec().runExec("aws", cmd, true);
		if( !res2.isRunOk() )
		{
			throw new OpenEditException("Could not delete " + res2.getStandardOut() + " " + cmd + " " );
		}
		File old = getFile(inAsset);
		boolean ok = old.delete();
		//String out2 = res2.getStandardOut();
		return ok;
	}

	@Override
	public Asset addNewAsset(Asset inAsset, List<ContentItem> inTemppages)
	{
		//Move the pages where they go
//		if( inAsset.isFolder())
//		{
//			for (Iterator iterator = inTemppages.iterator(); iterator.hasNext();)
//			{
//				ContentItem contentItem = (ContentItem) iterator.next();
//				//If it's a folder then put them all in there
//				
//			}
//		}
		if( inTemppages.size() == 1)
		{
			ContentItem one = inTemppages.iterator().next();
			String path = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/";
			path = path + inAsset.getSourcePath();

			File file = getFile(inAsset);

			if(!one.getPath().equals(path))
			{
				//move contents
				FileItem dest = new FileItem(file);
				getMediaArchive().getPageManager().getRepository().move(one, dest);
			}
			upload(inAsset, file);
		}
		else
		{
			throw new OpenEditException("Dont support folder uploading");
		}
		return inAsset;
	}

	@Override
	public Asset replaceOriginal(Asset inAsset, List<ContentItem> inTemppages)
	{
		throw new OpenEditException("Not implemented");
	}
	
	/**
	 * The move is already done for us
	 */
	@Override
	public Asset assetOrginalSaved(Asset inAsset)
	{
		File file = getFile(inAsset);
		upload(inAsset, file);
		return inAsset;
	}

	@Override
	public void detach()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveConfig()
	{
		//Save aws properties file
		String home = System.getenv("HOME");
		File cred = new File(home + "/.aws/credentials");
		cred.getParentFile().mkdirs();
/*		
		[default]
				aws_access_key_id = AKIAJXJZ3VUYQRWEFxxx
				aws_secret_access_key = YNjnyHrbINjLQ9izF86Iz+XJ4NJHixxxx
*/
		try (PrintWriter out = new PrintWriter(cred)) 
		{
		    out.println("[default]");
		    out.println("aws_access_key_id = " + getAccessKey());
		    out.println("aws_secret_access_key = " + getSecretKey());
		    out.close();
		    
		    Set<PosixFilePermission> perms = new HashSet<>();
		    perms.add(PosixFilePermission.OWNER_READ);
		    perms.add(PosixFilePermission.OWNER_WRITE);

		    Files.setPosixFilePermissions(cred.toPath(), perms);
		    
		}
		catch( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		
	}

	/**
	 * Loop over all the results since last time we ran it
	 * aws s3api list-objects --bucket "bucket-name" --query 'Contents[?LastModified>=2016-05-20][].{Key: Key}'
	 * aws s3api list-objects --bucket text-content --query 'Contents[].{Key: Key, Size: Size}'
	 * aws s3api list-objects --bucket <YOURBUCKETNAME> --query 'Contents?LastModified>=`2015-07-30`][.{Key: Key, LastModified: LastModified}' --output text
	 * aws s3api list-objects --bucket <bucket name> --query "Contents[?LastModified > '2017-08-03T23' && LastModified < '2017-08-03T23:15']"
	 */
	@Override
	public int importAssets(String inBasepath)
	{
		List cmd = new ArrayList();
		//aws s3 cp file.txt s3://
		cmd.add("s3api");
		cmd.add("list-objects");
		cmd.add("--max-items");
		cmd.add("1000");
		cmd.add("--page-size"); //1000 by default
		cmd.add("1000");		
		cmd.add("--bucket");
		cmd.add(getBucket());
		cmd.add("--cli-read-timeout");
		cmd.add("0");

		
		//2017-08-03T23
		String since = getConfig().get("lastscanstart");
		if( since != null)
		{
			cmd.add("--query");
			//2013-09-17T00:55:03.000Z //Amazon
			//"yyyy-MM-dd'T'HH:mm:ssZ"  https://developers.google.com/gmail/markup/reference/datetime-formatting
			Date date = DateStorageUtil.getStorageUtil().parseFromStorage(since);//, "yyyy-MM-dd'T'HH:mm:ssZ");
			
			SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
			since = dateFormatGmt.format(date);
			
			cmd.add("Contents[?LastModified > '" + since + "']");
		}
		Date started = new Date();
		
		ExecResult res = getExec().runExec("aws", cmd, true);
		if( !res.isRunOk() )
		{
			throw new OpenEditException("Could not download " + res.getStandardOut() + " " + cmd + " " );
		}
		String out = res.getStandardOut();
		if( out.startsWith("[]"))	
		{
			getConfig().setValue("lastscanstart", started);
			getMediaArchive().saveData("hotfolder", getConfig());
			return 0;
		}
//		if( !out.startsWith("{"))
//		{
//			throw new OpenEditException("Could not parse returned " + out);	
//		}
		try
		{
			//log.info(out);
			Object parsed = new JSONParser().parse(out);
			/*
			 
			 "NextToken": "eyJNYXJrZXIiOiAiMTgwMjA4L0NhcHR1cmUvMTgwMjA4XzAwNDIuTkVGIn0=", 
    "Contents": [
        {
            "LastModified": "2018-07-24T15:02:14.000Z", 
            "ETag": "\"3fa04f8eb0bb5852d0d24f6b6eb206b2\"", 
            "StorageClass": "STANDARD", 
            "Key": "180208/180208.cosessiondb", 
            "Owner": {
                "DisplayName": "cory", 
                "ID": "515e06f26dc591bc438c596a2618c7f9028137adf01423b285610d59a09b18db"
            }, 
            "Size": 2486272
        }, 
			 */
			//save assets
			ImportResult result = saveParsedAssets(parsed);
			int counted = result.count + importPagesOfAssets(result.token);
			
			getConfig().setValue("lastscanstart", started);
			getMediaArchive().saveData("hotfolder", getConfig());
			
			return counted;
			
			
		}
		catch (ParseException e)
		{
			throw new OpenEditException(e);
		}
	}

	class ImportResult
	{
		String token;
		int count;
	}
	
	protected ImportResult saveParsedAssets(Object inParsed)
	{
		ImportResult result = new ImportResult();
		
		Collection assets = null;
		if( inParsed instanceof JSONObject)
		{
			JSONObject json =(JSONObject)inParsed;
			assets = (Collection)json.get("Contents");
			result.token = (String)json.get("NextToken");			
		}
		else
		{
			assets = (Collection)inParsed; //One result
		}
		result.count = importAssets(assets);

		return result;
	}

	protected int importPagesOfAssets(String token) throws ParseException
	{
		int counted = 0;
		while( token != null)
		{
			//aws s3api list-objects --bucket my-bucket --max-items 100 --starting-token
			ArrayList cmd2 = new ArrayList();
			//aws s3 cp file.txt s3://
			cmd2.add("s3api");
			cmd2.add("list-objects");
			cmd2.add("--max-items");
			cmd2.add("1000");
			cmd2.add("--page-size"); //1000 by default
			cmd2.add("1000");		
			cmd2.add("--bucket");
			cmd2.add(getBucket());
			cmd2.add("--starting-token");
			cmd2.add(token);
			cmd2.add("--cli-read-timeout");
			cmd2.add("0");
			ExecResult res2 = getExec().runExec("aws", cmd2, true);
			if( !res2.isRunOk() )
			{
				throw new OpenEditException("Could not download " + res2.getStandardOut() + " " + cmd2 + " " );
			}
			String out2 = res2.getStandardOut();
			JSONObject parsed2 = (JSONObject)new JSONParser().parse(out2);
			ImportResult result = saveParsedAssets(parsed2);
			token = result.token;
			counted = counted + result.count;
		}
		return counted;
	}

	protected int importAssets(Collection inAssets)
	{
		log.info("Importing " + inAssets.size() + " assets");
		Searcher assetsearcher = getMediaArchive().getAssetSearcher();
		List tosave = new ArrayList(inAssets.size());
		
		//TODO: Speed up. DO a group search by ETag to see if we have them already in the DB
		
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			Map json = (Map) iterator.next();
			/*
			     "LastModified": "2018-07-24T15:02:14.000Z", 
            "ETag": "\"3fa04f8eb0bb5852d0d24f6b6eb206b2\"", 
            "StorageClass": "STANDARD", 
            "Key": "180208/180208.cosessiondb", 
            "Owner": {
                "DisplayName": "cory", 
                "ID": "515e06f26dc591bc438c596a2618c7f9028137adf01423b285610d59a09b18db"
            }, 
            "Size": 2486272
			 */
			String type = (String)json.get("StorageClass");
			if( type != null && type.equals("STANDARD"))
			{
				String sourcepath = (String)json.get("Key");
				if( !okToAdd(sourcepath))
				{
					continue;
				}
				sourcepath = getFolderPath() + "/" + sourcepath;
				Asset asset = getMediaArchive().getAssetBySourcePath(sourcepath);
				if( asset == null)
				{
					asset = (Asset)assetsearcher.createNewData();
					asset.setSourcePath(sourcepath);
				}
				asset.setValue("filesize", json.get("Size"));
				String lastmod = (String)json.get("LastModified");
				Date edited = DateStorageUtil.getStorageUtil().parse(lastmod, "yyyy-MM-dd'T'HH:mm:ssZ");
				asset.setValue("assetmodificationdate", edited);
				asset.setValue("assetaddeddate", new Date());
				
				asset.setValue("etagid", json.get("ETag"));
				
				asset.setProperty("importstatus", "needsmetadata");//Will possibly cause a download based on size and time?
				asset.setProperty("previewstatus", "0");
				//asset.setProperty("pushstatus", "resend");
				asset.setProperty("editstatus", "1");
				
				String foundprimary = PathUtilities.extractFileName(sourcepath);
				asset.setPrimaryFile(foundprimary);
				asset.setName(foundprimary);
				//getAssetUtilities().readMetadata(asset, found, getMediaArchive());
				//getMediaArchive().getAssetImporter().getAssetUtilities().populateCategory(asset, inInput, getMediaArchive(), null);
				String dir = PathUtilities.extractDirectoryPath(sourcepath);
				Category category = getMediaArchive().createCategoryPath(dir);
				asset.addCategory(category);
				
				tosave.add(asset);
			}
		}
		assetsearcher.saveAllData(tosave, null);
		getMediaArchive().firePathEvent("importing/assetscreated",null,tosave);
		return tosave.size();
		
	}

	@Override
	public void checkForDeleted()
	{
		//TODO: Do a search for versions that have been deleted and make sure they are marked as such
		
	}

	@Override
	protected ContentItem checkLocation(Asset inAsset, ContentItem inUploaded, User inUser)
	{
		File dest = getFile(inAsset);
		if(!inUploaded.getPath().equals(dest.getPath()))//move from tmp location to final location
		{
			Map props = new HashMap();
			props.put("absolutepath", dest.getAbsolutePath());
			getMediaArchive().fireMediaEvent("asset","savingoriginal",inAsset.getSourcePath(),props,inUser);
			getFileUtils().move(new File(inUploaded.getAbsolutePath()), dest, true);
			getMediaArchive().fireMediaEvent("asset","savingoriginalcomplete",inAsset.getSourcePath(),props,inUser);
		}
		return getOriginalContent(inAsset, false);
	}
	
	public void assetUploaded(Asset inAsset)
	{
		//Upload
		File file = getFile(inAsset);
		upload(inAsset, file);
	}
}
