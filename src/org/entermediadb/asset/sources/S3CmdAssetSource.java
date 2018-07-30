package org.entermediadb.asset.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class S3CmdAssetSource extends BaseAssetSource
{
	protected Exec fieldExec;
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
		File file = getFile(inAsset);
		if( !file.exists() )
		{
			//Fire command line to go download it
			file = download(inAsset,file);
			//Consider streaming this?
		}
		if( !file.exists() )
		{
			return null;
		}
		try
		{
			FileInputStream stream = new FileInputStream(file);
			return stream;
		} catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}
	}

	protected File download(Asset inAsset, File file)
	{
		List cmd = new ArrayList();
		//aws s3 cp file.txt s3://
		cmd.add("s3");
		cmd.add("cp");
		cmd.add("s3://" + getBucket() + "/" + inAsset.getSourcePath() );
		cmd.add(file.getAbsolutePath());
		ExecResult res = getExec().runExec("aws", cmd);
		if( !res.isRunOk() )
		{
			throw new OpenEditException("Could not download " + res.getStandardOut() + " " + cmd + " " );
		}
		//How do we set the timestamp? From the asset?
		return file;
	}

	protected File getFile(Asset inAsset)
	{
		String sp = getSourcePath(inAsset);
		String abpath = getExternalPath() + "/" + sp;
		return new File(abpath);
	}

	@Override
	public ContentItem getOriginalContent(Asset inAsset)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean handles(Asset inAsset)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeOriginal(Asset inAsset)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Asset addNewAsset(Asset inAsset, List<ContentItem> inTemppages)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Asset replaceOriginal(Asset inAsset, List<ContentItem> inTemppages)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Asset assetAdded(Asset inAsset, ContentItem inContentItem)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void detach()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveConfig()
	{
		// TODO Auto-generated method stub
		
	}

	/**
	 * Loop over all the results since last time we ran it
	 * aws s3api list-objects --bucket "bucket-name" --query 'Contents[?LastModified>=2016-05-20][].{Key: Key}'
	 * aws s3api list-objects --bucket text-content --query 'Contents[].{Key: Key, Size: Size}'
	 * aws s3api list-objects --bucket <YOURBUCKETNAME> --query 'Contents?LastModified>=`2015-07-30`][.{Key: Key, LastModified: LastModified}' --output text
	 * aws s3api list-objects --bucket <bucket name> --query "Contents[?LastModified > '2017-08-03T23' && LastModified < '2017-08-03T23:15']"
	 */
	@Override
	public List<String> importAssets(String inBasepath)
	{
		List cmd = new ArrayList();
		//aws s3 cp file.txt s3://
		cmd.add("s3api");
		cmd.add("list-objects");
		cmd.add("--max-items");
		cmd.add("500");
		cmd.add("--page-size"); //1000 by default
		cmd.add("500");		
		cmd.add("--bucket");
		cmd.add(getBucket());
		cmd.add("--query");
		//2017-08-03T23
		String since = getConfig().get("lastscanstart");
		if( since != null)
		{
			//2013-09-17T00:55:03.000Z //Amazon
			//"yyyy-MM-dd'T'HH:mm:ssZ"  https://developers.google.com/gmail/markup/reference/datetime-formatting
			since = DateStorageUtil.getStorageUtil().formatDate(since, "yyyy-MM-dd'T'HH:mm:ssZ");
			cmd.add("Contents[?LastModified > '" + since + "'");
		}
		ExecResult res = getExec().runExec("aws", cmd);
		if( !res.isRunOk() )
		{
			throw new OpenEditException("Could not download " + res.getStandardOut() + " " + cmd + " " );
		}
		String out = res.getStandardOut();
		if( !out.startsWith("{"))
		{
			throw new OpenEditException("Could not parse returned ");	
		}
		try
		{
			Map parsed = (Map)new JSONParser().parse(out);
			//save assets
			
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void checkForDeleted()
	{
		// TODO Auto-generated method stub
		
	}

}
