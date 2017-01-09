package test;

import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


public int listall(String inPath, int counted)
{
	
	File file = new File(inPath);
	String[] all = file.list();
	if( all != null)
	{
		List children = new ArrayList(all.length);
		for (int i = 0; i < all.length; i++)
		{
			counted++;
			File child = new File(file,all[i]);
			//log.info(child.getAbsolutePath());
			if(child.isDirectory())
			{
				counted = listall(child.getAbsolutePath(), counted);
			}
		}
	}
	return counted;	
}

public void listallFast(String inPath)
{
	Path dir = Paths.get(inPath);
	FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
	  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException 
	  {
		  //log.info(file);
		if(attrs.isDirectory())
		{
			//listallFast(file.toString());
		}
		return FileVisitResult.CONTINUE;
	  }
	};
//
	try 
	{
		Files.walkFileTree(dir, fv);
	}
	catch (IOException e) 
	{
	  e.printStackTrace();
	}
}

public void init()
{
	long start = System.currentTimeMillis();
	//echo 3 | sudo tee /proc/sys/vm/drop_caches
	listallFast("/home/shanti/git");
	//int counted = listall("/home/shanti/git", 0);
	long end = System.currentTimeMillis();
	end = end - start;
	//log.info("Listed " + counted + " in " + end);
	log.info("Listed fast in " + end);
}

init();