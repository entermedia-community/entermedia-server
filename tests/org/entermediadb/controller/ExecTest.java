package org.entermediadb.controller;

import java.util.ArrayList;
import java.util.List;

import org.openedit.BaseTestCase;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.ExecutorManager;
import org.openedit.util.RunningProcess;

public class ExecTest extends BaseTestCase
{	
	
	public void XXtestSpeed()
	{
		Exec exec = (Exec)getBean("exec");
		List comm = new ArrayList();
		comm.add("-S");
		comm.add("-d");
		comm.add("%Y-%m-%d %H:%M:%S");
		comm.add("/media/D603-EA1D/Sample EM/Content Archive/Highlights/2011/HL_12_11/HL_DEC_2011_PRESS_PDFS/HL_12_11_05_VERSE.pdf");
		comm.add("-n");
		long start = System.currentTimeMillis();
		ExecResult done = exec.runExec("exiftool", comm, true);
		assertTrue(done.isRunOk());
		log( done.getStandardOut());
		long end = System.currentTimeMillis();
		log("done in " + (end - start) + " milliseconds" );
		
	}
	private void log(String inString)
	{
		System.out.println(inString);
		
	}
	public void testRunExec() throws Exception
	{
		log("testRunExec\n");
		Exec exec = null;//(Exec)getBean("exec");
		if( exec == null)
		{
			exec = new Exec();
			exec.setExecutorManager(new ExecutorManager());
		}
		
		RunningProcess process = exec.getProcess("faceprofile");
		//RunningProcess process = exec.getProcess("cat");
		
		Thread.sleep(500);
		
		String stuff = "/home/shanti/Downloads/FaceAll.jpg";
		for (int i = 0; i < 10; i++)
		{
			String oneline = process.runExecStream(stuff);
			//log(i + "__GOT Back:" + oneline);
		}
		

		
		//make sure creation went ok, file found all that jazz
//		assertNotNull(exec.fieldCachedCommands);
//		assertNotNull(exec.fieldXmlCommandsFilename);
//		//call ffmpeg, lame, exiftool, imagemagick, ghostscript
//		//for testing runExec will just call a shell script which echos to the screen
//		exec.runExec("ffmpeg", null);
//		assertNotNull(exec.fieldCachedCommands.get("ffmpeg"));
//		exec.runExec("lame", null);
//		assertNotNull(exec.fieldCachedCommands.get("lame"));
//		exec.runExec("convert", null);
//		assertNotNull(exec.fieldCachedCommands.get("convert"));
//		exec.runExec("ghostscript", null);
//		assertNotNull(exec.fieldCachedCommands.get("ghostscript"));
//		exec.runExec("exiftool", null);
//		assertNotNull(exec.fieldCachedCommands.get("exiftool"));
	}
	
	
	
	
}
