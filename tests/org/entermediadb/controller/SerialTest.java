package org.entermediadb.controller;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;

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
