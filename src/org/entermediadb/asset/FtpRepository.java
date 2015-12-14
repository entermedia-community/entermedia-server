package org.entermediadb.asset;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.openedit.data.SearcherManager;
import org.openedit.repository.BaseRepository;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class FtpRepository extends BaseRepository
{
	protected SearcherManager fieldSearcherManager;
	protected UserManager fieldUserManager; // for getting the FTP user

	private static final Log log = LogFactory.getLog(FtpRepository.class);

	protected FTPClient fieldFTPClient;

	public FTPClient getFTPClient()
	{
		if (fieldFTPClient == null)
		{
			fieldFTPClient = new FTPClient();

		}

		return fieldFTPClient;
	}

	public void setFTPClient(FTPClient inFTPClient)
	{
		fieldFTPClient = inFTPClient;
	}

	public ContentItem get(String inPath) throws RepositoryException
	{
		String path = inPath.substring(getPath().length());
		if (path.length() == 0)
		{
			path = "/";
		}
		String url = getExternalPath() + path;
		FtpContentItem item = new FtpContentItem();
		item.setPath(inPath);
		item.setAbsolutePath(path);

		return item;
	}
	
	protected String makeRemotePath(String inPath)
	{
		String path = inPath.toString();
		if (path.startsWith(getPath()))
		{
			//chop off the repo path. add 1 b/c repo path doesn't include trailing slash
			path = path.substring(getPath().length() + 1);				
		}
		return path;
	}
	protected void checkConnection() throws Exception
	{
		if (!isConnected())
		{
			boolean connect = connect();

			if (!connect)
			{
				throw new RepositoryException("Cannot connect to server: " + getExternalPath());
			}
		}
	}

	public void copy(ContentItem inSource, ContentItem inDestination)
			throws RepositoryException
	{
		try
		{
			checkConnection();			
			String path = inDestination.getAbsolutePath().substring(1);
			makeDirs(path);
			boolean success = getFTPClient().storeFile(path, inSource.getInputStream());
			if (!success)
				log.info("Failed to store " + path);
		} catch (Exception e)
		{
			throw new RepositoryException(e);
		}

	}

	public void deleteOldVersions(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public boolean doesExist(String inPath) throws RepositoryException
	{
		return false;
		/*
		try
		{
			checkConnection();
			String path = makeRemotePath(inPath);
			FTPFile[] files = getFTPClient().listFiles(path);
			int num = files.length;
			
			if (num == 0)
				return false;
			else if (num == 1)
				return true;
			else
			{
				throw new Exception("Individual file list returned multiple matches:" + inPath);
			}
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}	
		*/	
	}

	public List getChildrenNames(String inParent) throws RepositoryException
	{
		log.info("trying to get filenames");
		try
		{
			if (!isConnected())
			{
				boolean connect = connect();

				if (!connect)
				{
					log.info("unable to connect to server: "
							+ getExternalPath());
					return null;
				}
			}

			getFTPClient().changeWorkingDirectory(inParent);
			FTPFile[] files = getFTPClient().listFiles();
			List filenames = new ArrayList();
			for (int i = 0; i < files.length; i++)
			{

				FTPFile file = files[i];
				String filename = file.getName();
				filenames.add(filename);
			}
			return filenames;
		} catch (Exception e)
		{
			throw new RepositoryException(e);
		}

	}

	public ContentItem getLastVersion(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public ContentItem getStub(String inPath) throws RepositoryException
	{
		return get(inPath);
	}

	public List getVersions(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void move(ContentItem inSource, ContentItem inDestination)
			throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public void move(ContentItem inSource, Repository inSourceRepository,
			ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}
	
	/* Given a path to a file, make all the intermediate directories
	 * 
	 */
	protected void makeDirs(String inPath) throws IOException
	{
		if (inPath.contains("/"))
		{
			String[] components = inPath.split("/");
			String path = components[0];
			
			for (int i = 1; i < components.length; i++)
			{
				getFTPClient().makeDirectory(path);
				path += "/" + components[i];
			}	
		}
		
	}

	public void remove(ContentItem inPath) throws RepositoryException
	{
		try
		{
			checkConnection();
			String path = inPath.getAbsolutePath().substring(1);
			boolean success = getFTPClient().deleteFile(path);
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

	class FtpContentItem extends InputStreamItem
	{
		protected Boolean existed;

		public InputStream getInputStream() throws RepositoryException
		{

			try
			{
				return getFTPClient().retrieveFileStream(getAbsolutePath().substring(1));
			} catch (IOException e)
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
				FTPFile[] files = getFTPClient().listFiles(path);
	
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

	public boolean isConnected()
	{
		return getFTPClient().isConnected();
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	/*
	public Data getConfiguration()
	{
		Searcher configsearcher = getSearcherManager().getSearcher("system",
				"ftp");
		Data config = (Data) configsearcher.searchById(getPath());
		if (config == null)
		{
			config = configsearcher.createNewData();
			config.setId(getPath());
			configsearcher.saveData(config, null);
		}
		return config;
	}
	*/
	
	public void disconnect() throws IOException
	{
		if (isConnected())
		{
			getFTPClient().disconnect();
		}
	}

	public boolean connect() throws Exception
	{
		try
		{
			if (isConnected())
			{
				return true;
			}
			boolean connected = false;
			
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

			getFTPClient().connect(serverName);
			log.info("trying to connect to : " + serverName);
			
			
			String ftpuser = getUserName();
			
			User user = getUserManager().getUser(ftpuser);

			if (user == null)
			{
				log.info("No user found -trying anonymous login");
				connected = getFTPClient().login("anonymous", "anonymous");

			}
			else
			{
				connected = getFTPClient().login(ftpuser,
						getUserManager().decryptPassword(user));	
			}
			if (connected)
			{
				//getFTPClient().enterLocalPassiveMode();
				log.info("Connected to " + serverName);
				if (subdir != null)
				{
					log.info("Changing working directory to " + subdir);
					getFTPClient().changeWorkingDirectory(subdir);
				}
				getFTPClient().setFileType(FTP.BINARY_FILE_TYPE);
			}
				
			
			log.info(getFTPClient().getReplyString());
			return connected;
		}

		catch (IOException e)
		{
			return false;
		}
	}

	public String getUserName()
	{
		return getProperty("username");
	}

	public void setUserName(String userName)
	{
		setProperty("username", userName);
	}

	@Override
	public void put(ContentItem inContent) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}
}
