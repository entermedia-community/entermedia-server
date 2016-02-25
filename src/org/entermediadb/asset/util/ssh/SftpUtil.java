package org.entermediadb.asset.util.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.repository.RepositoryException;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SftpUtil
{

	private static Log log = LogFactory.getLog(SftpUtil.class);

	protected String fieldKeyFile;
	protected boolean fieldTrust;
	protected String fieldKnownHosts;
	protected String fieldUsername;
	protected String fieldPassword;
	protected String fieldHost;
	protected int fieldPort = 22;
	protected Session fieldSession;
	protected Channel channel;

	public Session getSession()
	{
		return fieldSession;
	}

	public void setSession(Session inSession)
	{
		fieldSession = inSession;
	}

	protected Session openSession() throws JSchException
	{
		JSch jsch = new JSch();

		if (null != getKeyFile())
		{
			jsch.addIdentity(getKeyFile());
		}

		if (!isTrust() && getKnownHosts() != null)
		{
			log.info("Using known hosts: " + getKnownHosts());
			jsch.setKnownHosts(getKnownHosts());
		}

		fieldSession = jsch.getSession(getUsername(), getHost(), getPort());
		//session.setUserInfo(this);
		fieldSession.setPassword(getPassword());
		fieldSession.setConfig("StrictHostKeyChecking", "no"); // 
		fieldSession.setConfig("PreferredAuthentications", "password,gssapi-with-mic,publickey");
		log.info("Connecting to " + getHost() + ":" + getPort());
		fieldSession.connect();
		//log.info("connected");

		return fieldSession;
	}


	public String getPassword()
	{
		return fieldPassword;
	}

	public void sendFileToRemote(File localFile, String remotePath) throws IOException, SftpException, JSchException
	{

		ChannelSftp channel = (ChannelSftp) openSftpChannel();

		long filesize = localFile.length();

		if (remotePath == null)
		{
			remotePath = localFile.getName();
		}

		long startTime = System.currentTimeMillis();
		long totalLength = filesize;

		channel.put(localFile.getAbsolutePath(), remotePath);

	}

	public boolean isFolder(String remotePath) throws IOException, SftpException, JSchException
	{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		SftpATTRS attrs = channel.lstat(remotePath);
		return attrs.isDir();
	}

	public InputStream getFileFromRemote(String remotePath) throws IOException, SftpException, JSchException
	{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		return channel.get(remotePath);
	}

	protected Channel openSftpChannel() throws JSchException
	{
		if (this.channel == null)
		{
			this.channel = openSession().openChannel("sftp");
			channel.connect();
			ChannelSftp c = (ChannelSftp) channel;
		}
		return this.channel;
	}

	public String getUsername()
	{
		return fieldUsername;
	}

	public void setUsername(String inUsername)
	{
		fieldUsername = inUsername;
	}

	public void setPassword(String inPassword)
	{
		fieldPassword = inPassword;
	}

	public String getHost()
	{
		return fieldHost;
	}

	public void setHost(String inHost)
	{
		fieldHost = inHost;
	}

	public int getPort()
	{
		return fieldPort;
	}

	public void setPort(int inPort)
	{
		fieldPort = inPort;
	}

	public String getKeyFile()
	{
		return fieldKeyFile;
	}

	public void setKeyFile(String inKeyFile)
	{
		fieldKeyFile = inKeyFile;
	}

	public boolean isTrust()
	{
		return fieldTrust;
	}

	public void setTrust(boolean inTrust)
	{
		fieldTrust = inTrust;
	}

	public String getKnownHosts()
	{
		return fieldKnownHosts;
	}

	public void setKnownHosts(String inKnownHosts)
	{
		fieldKnownHosts = inKnownHosts;
	}

	public boolean isConnected()
	{
		return getSession().isConnected();
	}

	public InputStream retrieveFileStream(String remotePath) throws SftpException, JSchException
	{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		return channel.get(remotePath);
	}

	public void makeDirs(String inPath) throws SftpException, JSchException
	{
		if (inPath.contains("/"))
		{
			String[] components = inPath.split("/");
			String path = components[0];
			ChannelSftp channel = (ChannelSftp) openSftpChannel();
			for (int i = 1; i < components.length; i++)
			{
				channel.mkdir(path);
				path += "/" + components[i];
			}
		}

	}

	public void remove(ContentItem inPath) throws RepositoryException
	{
		String path = inPath.getAbsolutePath().substring(1);
		try
		{
			ChannelSftp channel = (ChannelSftp) openSftpChannel();
			channel.rm(path);
		}
		catch (Exception e)
		{
			throw new RepositoryException("Couldn't remove file: " + inPath.getPath());
		}
	}

	public boolean doesExist(String path) throws Exception
	{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		InputStream is;
		try
		{
			is = channel.get(path);
			if (is != null)
				return is.available() > 0;
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}

	public List getStrChildNames(String inParent) throws JSchException, SftpException
	{
		List<String> childNames = new ArrayList<String>();
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		Vector v = channel.ls(inParent);
		ChannelSftp.LsEntry entry = null;
		for (int i = 0; i < v.size(); i++)
		{
			entry = (ChannelSftp.LsEntry) v.get(i);
			childNames.add(entry.getFilename());
		}
		return childNames;
	}

	public List getChildNames(String inParent) throws JSchException, SftpException
	{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		List<ChannelSftp.LsEntry> v = channel.ls(inParent);
		return v;
	}

	public static void main(String args[]) throws Exception
	{
		SftpUtil sftp = new SftpUtil();
		sftp.setUsername("tuan");
		sftp.setPassword("entermedia");
		File localFile = new File("f:/test.txt");
		String remotePath = "/home/tuan";
		sftp.sendFileToRemote(localFile, remotePath);

		//InputStream is = sftp.retrieveFileStream("/home/tuan/test.txt");
		sftp.disconnect();
	}

	public void disconnect()
	{
		if (this.channel != null)
			this.channel.disconnect();
		if (this.fieldSession != null)
			this.fieldSession.disconnect();
		this.channel = null;
		this.fieldSession = null;

	}

	public boolean deleteFile(String path) throws JSchException, SftpException
	{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		try
		{
			channel.cd(path);
		}
		catch (SftpException e)
		{
			channel.rm(path);
			return true;
		}
		channel.rmdir(path);
		return true;
	}

	public void sendFileToRemote(InputStream inStream, String remotePath)  
	{
		try
		{
			ChannelSftp channel = (ChannelSftp) openSftpChannel();
			String dir = PathUtilities.extractDirectoryPath(remotePath);
			if( dir.length() > 0 )
			{
				channel.cd(dir);
			}
			
			String name = PathUtilities.extractFileName(remotePath);
			channel.put(inStream, name);
		}
		catch( Exception ex )
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			FileUtils.safeClose(inStream);
		}
	}
}