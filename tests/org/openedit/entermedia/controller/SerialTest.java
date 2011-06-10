package org.openedit.entermedia.controller;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.apache.bsf.debug.serverImpl.ObjectServer;
import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;

public class SerialTest extends BaseEnterMediaTest
{

	public void testSerialization() throws Exception
	{
		User admin = getFixture().getUserManager().getUser("admin");
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		ObjectOutputStream objects = new ObjectOutputStream(out);
		objects.writeObject(admin);

		HitTracker tracker = getMediaArchive().getAssetSearcher().getAllHits();
		objects.writeObject(tracker);
		
	}
}
