package org.openedit.entermedia.util.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

import com.openedit.OpenEditException;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class SftpRepository extends BaseRepository {

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
		String path = inContent.getPath();
		File file = new File(inContent.getAbsolutePath());
		try {
			getSftpUtil().sendFileToRemote(file, path);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}

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
		// TODO Auto-generated method stub

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

	@Override
	public List getChildrenNames(String inParent) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
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
				return getSftpUtil().retrieveFileStream(getAbsolutePath().substring(1));
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
				FTPFile[] files = getSftpUtil().listFiles(path);
	
				if (files == null || files.length == 0)
				{
					return false;
				}
				return true;
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
