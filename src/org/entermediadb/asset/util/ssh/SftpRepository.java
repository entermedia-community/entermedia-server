package org.entermediadb.asset.util.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.FtpRepository;
import org.openedit.OpenEditException;
import org.openedit.data.SearcherManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.users.User;
import org.openedit.users.UserManager;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

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
		String url = getProperty("defaultremotepath") + path;
//		if(path.startsWith("/"))
//			url = getProperty("defaultremotepath") + path;
//		else
//			url = getProperty("defaultremotepath") + "/" + path;
	
		SFtpContentItem item = new SFtpContentItem();
		item.setRepository(this);
		item.setPath(inPath);
		item.setAbsolutePath(path);

		return item;
	}

	public InputStream getInputStream(String inPath) throws RepositoryException {
		String path = inPath.substring(getPath().length());
		if (path.length() == 0)
		{
			//path = "/";
			return null;
		}
		String url = getProperty("defaultremotepath") + "/" + path;
		//String path =this.getDefaultRemoteDirectory() +  inContent.getName();
		SFtpContentItem item = new SFtpContentItem();
		InputStream is = null;
		try {
			is =getSftpUtil().getFileFromRemote(path);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
		return is;
	}
	@Override
	public ContentItem getStub(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean doesExist(String inPath) throws RepositoryException {
		try {
			return getSftpUtil().doesExist(inPath);
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
	public ChannelSftp.LsEntry getAttribute(String inPath) throws RepositoryException {
		try {
			List l = getSftpUtil().getChildNames(inPath);
			if (l== null) return null;
			return (ChannelSftp.LsEntry)l.get(0);
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
	
	
	public boolean isFolder(String inPath) throws RepositoryException {
		try {
			return getSftpUtil().isFolder(inPath);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}

	
	public void put(ContentItem inContent) throws RepositoryException {
		//need to write the file to the webserver folder first
		super.put(inContent);
		String path =getProperty("defaultremotepath") +"/"+  inContent.getName();
		//File file = new File(inContent.getAbsolutePath());
		File file = new File(inContent.getAbsolutePath());
		try {
			getSftpUtil().sendFileToRemote(file, path);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}
	
	public List<ContentItem> listFiles(String inPath) throws RepositoryException  {
		List<ChannelSftp.LsEntry> childNames;
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
			SFtpContentItem item = new SFtpContentItem(childNames.get(i));
			item.setPath(inPath);
			item.setAbsolutePath(path);
			item.setRepository(this);
			contentItems.add((ContentItem)item);
	    }
	    return contentItems;
	}
	
//	public List<ContentItem> listFiles(String inPath) throws RepositoryException  {
//		List<String> childNames;
//		try {
//			childNames = getSftpUtil().getChildNames(inPath);
//		} catch (Exception e) {
//			throw new RepositoryException("Couldn't list file in: " + inPath);
//		} 
//		String path = inPath.substring(getPath().length());
//		if (path.length() == 0)
//		{
//			path = "/";
//		}
//		List<ContentItem> contentItems = new ArrayList<ContentItem>();
//	    for(int i=0; i < childNames.size(); i++){
//			String url = getExternalPath() + childNames.get(i);
//			SFtpContentItem item = new SFtpContentItem();
//			item.setPath(inPath);
//			item.setAbsolutePath(path);
//			contentItems.add((ContentItem)item);
//	    }
//	    return contentItems;
//	}
	

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
			
			String path = inParent.substring(getPath().length());
			if (path.length() == 0)
			{
					path =getProperty("defaultremotepath");
			}else{
					path =getProperty("defaultremotepath")+  inParent.substring(getPath().length(), inParent.length());	
			}
			return getSftpUtil().getStrChildNames(path);
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
			if(serverName== null || serverName.trim().equals(""))
				serverName = getProperty("externalpath");
			if(serverName == null) return false;
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
			getSftpUtil().setUsername(user.getName());
			getSftpUtil().setPassword(password);
			getSftpUtil().setHost(serverName);
			
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	class SFtpContentItem extends InputStreamItem
//	{
//		protected Boolean existed;
//
//		public InputStream getInputStream() throws RepositoryException
//		{
//
//			try
//			{
//				return getSftpUtil().retrieveFileStream(getAbsolutePath().substring(1));
//			} catch (Exception e)
//			{
//				throw new RepositoryException(e);
//			}
//		}
//
//		public boolean exists()
//		{
//			try
//			{
//				checkConnection();
//				String path = getAbsolutePath().substring(1);
//				return   getSftpUtil().doesExist(path);
//	
//			} catch (Exception e)
//			{
//				throw new RepositoryException(e);
//			}
//		}
//
//		public boolean isFolder()
//		{
//			if (getAbsolutePath().endsWith("/"))
//			{
//				return true;
//			}
//			return false;
//		}
//
//	}

}