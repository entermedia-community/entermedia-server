package org.entermediadb.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.video.VTT.Cue;
import org.entermediadb.video.VTT.webvtt.WebvttParser;
import org.entermediadb.video.VTT.webvtt.WebvttSubtitle;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.modules.translations.LanguageMap;

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
