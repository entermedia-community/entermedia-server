package org.openedit.entermedia.util.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;


public class SftpUtil  {

	private static Log log = LogFactory.getLog(SftpUtil.class);

	protected String fieldKeyFile;
	protected boolean fieldTrust;
	protected String fieldKnownHosts;
	protected String fieldUsername = "root";	
	protected String fieldPassword = "spw8j3nn6";
	protected String fieldHost = "dev.ijsolutions.ca";
	protected int fieldPort =22;
	protected Session fieldSession;
	
	
	
	public Session getSession() {
		return fieldSession;
	}

	public void setSession(Session inSession) {
		fieldSession = inSession;
	}

	protected Session openSession() throws JSchException {
		JSch jsch = new JSch();

		if (null != getKeyFile()) {
			jsch.addIdentity(getKeyFile());
		}

		if (!isTrust() && getKnownHosts() != null) {
			log.info("Using known hosts: " + getKnownHosts());
			jsch.setKnownHosts(getKnownHosts());
		} 
		
		

		Session session = jsch.getSession(getUsername(), getHost(), getPort());
		//session.setUserInfo(this);
		session.setPassword(getPassword());
		session.setConfig("StrictHostKeyChecking", "no");  // 
		session.setConfig("PreferredAuthentications",
        "password,gssapi-with-mic,publickey");
		log.info("Connecting to " + getHost() + ":" + getPort());
		session.connect();
		log.info("connected");
		
		return session;
	}

	public String getPassword() {
		return fieldPassword;
	}

	public void sendFileToRemote(File localFile,
			String remotePath) throws IOException, SftpException, JSchException {
	
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		
		
		long filesize = localFile.length();

		if (remotePath == null) {
			remotePath = localFile.getName();
		}

		long startTime = System.currentTimeMillis();
		long totalLength = filesize;

			
					channel.put(localFile.getAbsolutePath(), remotePath);
		
	}
	

	
	
	
	
	
	 protected Channel openSftpChannel() throws JSchException {
		 Channel channel=openSession().openChannel("sftp");
	      channel.connect();
	      ChannelSftp c=(ChannelSftp)channel;
	        return channel;
	    }
	
	
	
	public String getUsername() {
		return fieldUsername;
	}

	public void setUsername(String inUsername) {
		fieldUsername = inUsername;
	}

	public void setPassword(String inPassword) {
		fieldPassword = inPassword;
	}

	public String getHost() {
		return fieldHost;
	}

	public void setHost(String inHost) {
		fieldHost = inHost;
	}

	public int getPort() {
		return fieldPort;
	}

	public void setPort(int inPort) {
		fieldPort = inPort;
	}

	public String getKeyFile() {
		return fieldKeyFile;
	}

	public void setKeyFile(String inKeyFile) {
		fieldKeyFile = inKeyFile;
	}

	public boolean isTrust() {
		return fieldTrust;
	}

	public void setTrust(boolean inTrust) {
		fieldTrust = inTrust;
	}

	public String getKnownHosts() {
		return fieldKnownHosts;
	}

	public void setKnownHosts(String inKnownHosts) {
		fieldKnownHosts = inKnownHosts;
	}

	public boolean isConnected(){
		return getSession().isConnected();
	}

	public InputStream retrieveFileStream(String remotePath) throws SftpException, JSchException {
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		return	channel.get(remotePath);
	}

	public FTPFile[] listFiles(String inPath) {
		// TODO Auto-generated method stub
		return null;
	}
	public boolean doesExist(String path) throws Exception{
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
		
		return channel.get(path).available() >0;
	}

	public List getChildNames(String inParent) throws JSchException, SftpException {
		ChannelSftp channel = (ChannelSftp) openSftpChannel();
	    return channel.ls(inParent);
		

	}

	public boolean deleteFile(String inPath) {
		// TODO Auto-generated method stub
		return false;
	}

	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

}
