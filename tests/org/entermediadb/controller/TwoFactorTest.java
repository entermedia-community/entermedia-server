package org.entermediadb.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.BaseEnterMediaTest;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class TwoFactorTest extends BaseEnterMediaTest {

	private static final Log log = LogFactory.getLog(TwoFactorTest.class);



	
	
	public void testGoogle() {
		GoogleAuthenticator gAuth = new GoogleAuthenticator();
		final GoogleAuthenticatorKey key = gAuth.createCredentials();
		String keystring =  "KNCCQIP3VYUCDCEN";
		log.info(keystring);
		//https://github.com/wstrange/GoogleAuth
		boolean isCodeValid = gAuth.authorize(keystring, 199426);
		assertTrue(isCodeValid);
		
	

	}



}
