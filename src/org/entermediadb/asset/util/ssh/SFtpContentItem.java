package org.entermediadb.asset.util.ssh;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.openedit.repository.InputStreamItem;
import org.openedit.repository.RepositoryException;

import com.jcraft.jsch.ChannelSftp;

class SFtpContentItem extends InputStreamItem
//class SFtpContentItem extends ContentItem
{
	protected Boolean existed;
	ChannelSftp.LsEntry entry;
	private SftpRepository repository;
	public SFtpContentItem(){
		
	}
	
	public SFtpContentItem(ChannelSftp.LsEntry entry){
		this.entry = entry;
		this.fieldLength= this.entry.getAttrs().getSize();
		this.setLastModified(new Date(this.entry.getAttrs().getMTime()));
		this.fieldActualPath = this.entry.getLongname();
		//this.fieldPath = this.entry.getFilename();
		this.fieldPath = this.entry.getFilename();
		//item.setPath(inPath);
		//item.setAbsolutePath(path);
		
	}
	
	@Override
	public long getLength() {
		if(this.entry == null){
			String path = getAbsolutePath().substring(1);
			//return repository.doesExist(path);
			String url = this.repository.getProperty("defaultremotepath") + "/" + path;
			this.entry = repository.getAttribute(url);
		}
		if(this.entry != null)
			this.fieldLength= this.entry.getAttrs().getSize();
		return this.fieldLength;
	}
//	@Override
//	public long getLastModified(){
//		return (new Date()).getTime();
//	}
	//@Override
	public Date lastModified()
	{
		if(this.entry != null)
			this.setLastModified(new Date(this.entry.getAttrs().getMTime()));
		return super.lastModified();
	}
	
	public InputStream getInputStream() throws RepositoryException
	{
		if ( isFolder() )
		{
			return createFileListingStream();
		}
		try
		{
			String path = getAbsolutePath().substring(1);
			return repository.getInputStream(path);
		}
		catch(Exception e )
		{
			throw new RepositoryException( e );
		}
	}

	public boolean exists()
	{
		try
		{
			String path = getAbsolutePath().substring(1);
			//return repository.doesExist(path);
			String url = this.repository.getProperty("defaultremotepath") + "/" + path;
			this.entry = repository.getAttribute(url);
			if(this.entry == null) return false;
			return true;
			
		}
		catch(Exception e )
		{
			return false;
			
		}
	}
	

	public boolean isFolder()
	{
		if (getAbsolutePath().endsWith("/"))
		{
			return true;
		}
		return false;
	}


	public boolean isWritable() {
		// TODO Auto-generated method stub
		return true;
	}

	protected InputStream createFileListingStream()
	{
		SftpRepository sftp = null;
		StringBuffer sb = null;
		try
		{
			List<String> files = repository.getChildrenNames(this.fieldAbsolutePath);
			sb = new StringBuffer();
			for ( int i = 0; i < files.size(); i++ )
			{
				if ( !files.get(i).equals(".versions") )
				{
					sb.append( files.get(i) + "\n" );
				}
			}
		}
		catch(Exception e )
		{
			throw new RepositoryException( e );
		}
		return new ByteArrayInputStream( sb.toString().getBytes() );
	}

	public SftpRepository getRepository() {
		return repository;
	}

	public void setRepository(SftpRepository repository) {
		this.repository = repository;
	}
	
}
