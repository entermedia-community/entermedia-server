package org.openedit.entermedia.util.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.FtpRepository;
import org.openedit.repository.BaseRepository;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.FileRepository;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.openedit.OpenEditException;
import com.openedit.users.User;
import com.openedit.users.UserManager;

//public class SftpRepository extends BaseRepository {
  public class SftpRepository extends FileRepository {

	protected SftpUtil fieldSftpUtil;

	protected SearcherManager fieldSearcherManager;
	protected UserManager fieldUserManager; // for getting the FTP user

	private static final Log log = LogFactory.getLog(FtpRepository.class);

	public SftpUtil getSftpUtil() {
		if (fieldSftpUtil == null) {
			fieldSftpUtil = new SftpUtil();

		}

		return fieldSftpUtil;
	}

	protected void checkConnection() throws Exception {
		if (!isConnected()) {
			boolean connect = connect();

			if (!connect) {
				throw new RepositoryException("Cannot connect to server: "
						+ getExternalPath());
			}
		}
	}

	public void setSftpUtil(SftpUtil fieldSftpUtil) {
		this.fieldSftpUtil = fieldSftpUtil;
	}

	public ContentItem get(String inPath) throws RepositoryException {
		String path = inPath.substring(getPath().length());
		if (path.length() == 0)
		{
			path = "/";
		}
		String url = getExternalPath() + path;
		//String path =this.getDefaultRemoteDirectory() +  inContent.getName();
		SFtpContentItem item = new SFtpContentItem();
		item.setPath(inPath);
		item.setAbsolutePath(path);

		return item;
	}

	@Override
	public ContentItem getStub(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean doesExist(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	
	public void put(ContentItem inContent) throws RepositoryException {
		//need to write the file to the webserver folder first
		
		String path =getProperty("defaultremotepath") +"/"+  inContent.getName();
		//File file = new File(inContent.getAbsolutePath());
		
		try {
			getSftpUtil().sendFileToRemote(inContent.getInputStream(), path);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}
	
	
	
	public List<ContentItem> listFiles(String inPath) throws RepositoryException  {
		List<String> childNames;
		try {
			childNames = getSftpUtil().getChildNames(inPath);
		} catch (Exception e) {
			throw new RepositoryException("Couldn't list file in: " + inPath);
		} 
		String path = inPath.substring(getPath().length());
		if (path.length() == 0)
		{
			path = "/";
		}
		List<ContentItem> contentItems = new ArrayList<ContentItem>();
	    for(int i=0; i < childNames.size(); i++){
			String url = getExternalPath() + childNames.get(i);
			SFtpContentItem item = new SFtpContentItem();
			item.setPath(inPath);
			item.setAbsolutePath(path);
			contentItems.add((ContentItem)item);
	    }
	    return contentItems;
	}
	

	@Override
	public void copy(ContentItem inSource, ContentItem inDestination)
			throws RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(ContentItem inSource, ContentItem inDestination)
			throws RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(ContentItem inSource, Repository inSourceRepository,
			ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(ContentItem inPath) throws RepositoryException {
		try
		{
			checkConnection();
			String path = inPath.getAbsolutePath().substring(1);
			boolean success = getSftpUtil().deleteFile(path);
			if (!success)
			{
				throw new RepositoryException("Couldn't put file: " + inPath.getPath());
			}
		} 
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}

	}

	@Override
	public List getVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentItem getLastVersion(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	
	public List getChildrenNames(String inParent) throws RepositoryException {
		try {
			String path =getProperty("defaultremotepath")+  inParent.substring(getPath().length(), inParent.length());
			
			return getSftpUtil().getChildNames(path);
		} catch (JSchException e) {
			throw new RepositoryException(e);
		} catch (SftpException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void deleteOldVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub

	}

	public boolean isConnected() {
		return getSftpUtil().isConnected();
	}
 
	public boolean connect() throws Exception
	{
		
			if (isConnected())
			{
				return true;
			}
			
			String serverName = getExternalPath();
			String subdir = null;
			if (serverName.indexOf(':') > -1)
			{
				String[] parts = serverName.split(":");
				if (parts.length > 0)
				{
					serverName = parts[0];
				}
				if (parts.length > 1)
				{
					
					subdir = parts[1];
				}
			}
			
			String ftpuser = getUserName();
			User user = getUserManager().getUser(ftpuser);

			String password = getUserManager().decryptPassword(user);

			log.info("trying to connect to : " + serverName);
			
			getSftpUtil().openSession();
			return getSftpUtil().isConnected();
	
	}
	public void disconnect() throws IOException
	{
		if (isConnected())
		{
			getSftpUtil().disconnect();
		}
	}

	

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public String getUserName() {
		return getProperty("username");
	}

	public void setUserName(String userName) {
		setProperty("username", userName);
	}
	class SFtpContentItem extends InputStreamItem
	{
		protected Boolean existed;

		public InputStream getInputStream() throws RepositoryException
		{

			try
			{
				String path = getAbsolutePath().substring(1);
				path =getProperty("defaultremotepath")+ "/" + path;
				return getSftpUtil().retrieveFileStream(path);
			} catch (Exception e)
			{
				throw new RepositoryException(e);
			}
		}

		public boolean exists()
		{
			try
			{
				checkConnection();
				String path = getAbsolutePath().substring(1);
				path =getProperty("defaultremotepath")+ "/" + path;

				return   getSftpUtil().doesExist(path);
	
			} catch (Exception e)
			{
				throw new RepositoryException(e);
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

	}

}