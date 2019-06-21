package org.openedit.entermedia.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ManyThreads
{
	protected static void log(String info)
	{
		System.out.println(info);
	}

	public static void main(String[] args)
	{
		testThreads();
		testRam();
		testFiles();
	}
	private static void testRam()
	{
		//2000
		log("testing ram");
		int i = 0;
		Collection hold = new ArrayList();
		String copythis = "This is some text that we want to copy and bunch of times";
		
		while (i < 50000)
		{
			hold.add(copythis + i++);
		}
		log("Added a bunch of ram (chars): " + (1 + copythis.length())  * hold.size());
	}	
	private static void testFiles()
	{
		//2000
		log("testing files");

		int i = 0;
		Collection hold = new ArrayList();
		new File("/tmp/cburkey").mkdirs();
		while (i < 2000)
		{
			log("Open Files[{}]" + i++);
			try
			{
				
				File somefile = new File("/tmp/cburkey/junkem333" + i);
				FileOutputStream saving = new FileOutputStream(somefile);
				saving.write(1);
				hold.add(saving);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
		}
		log("Sleeping 30s with 2000 open files open");
		try
		{
			Thread.currentThread().sleep(30000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}	
		log("Exiting sleepo");
		
	}

	private static void testThreads()
	{
		int i = 0;
		while (i < 5000)
		{
			log("Starting thread [{}]" + i++);
			Thread t = new Thread(new Idler());
			t.start();
		}
	}

	private static class Idler implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				Thread.sleep(2L * 60L * 1000L); //2Minutes
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}
}
