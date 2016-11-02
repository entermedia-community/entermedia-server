package org.entermediadb.asset.importer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openedit.OpenEditException;
import org.openedit.cache.CacheManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.util.ExecutorManager;

public class FolderMonitor implements Runnable, WebEventListener
{
	protected WatchService watcher = null;
	protected Map<WatchKey, Path> keys = null;
	protected ExecutorManager fieldExecutorManager;
	protected Map fieldPathChangedListeners = new HashMap();
	protected CacheManager fieldTimedCache;
	
	public CacheManager getTimedCache()
	{
		return fieldTimedCache;
	}

	public void setTimedCache(CacheManager inTimedCache)
	{
		fieldTimedCache = inTimedCache;
	}

	public void addPathChangedListener(String inPrefix, PathChangedListener inPathChangedListener)
	{
		fieldPathChangedListeners.put(inPrefix, inPathChangedListener);
		addFolderTree(inPrefix);
	}

	public ExecutorManager getExecutorManager()
	{
		return fieldExecutorManager;
	}

	public void setExecutorManager(ExecutorManager inExecutorManager)
	{
		fieldExecutorManager = inExecutorManager;
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event)
	{
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException
	{
		WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the
	 * WatchService.
	 */
	private void registerAll(final Path start) throws IOException
	{
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Process all events for keys queued to the watcher
	 */
	public void run()
	{
		for (;;)
		{

			// wait for key to be signalled
			WatchKey key;
			try
			{
				key = watcher.take();
			}
			catch (InterruptedException x)
			{
				return;
			}

			Path dir = keys.get(key);
			if (dir == null)
			{
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents())
			{
				WatchEvent.Kind kind = event.kind();

				// TBD - provide example of how OVERFLOW event is handled
				if (kind == StandardWatchEventKinds.OVERFLOW)
				{
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);

				// print out event
				//System.out.format("%s: %s\n", event.kind().name(), child);
				String absolutepath = child.toFile().getAbsolutePath();
				if( getTimedCache().get("FolderMonitor", absolutepath) == null )
				{
					for (Iterator iterator = fieldPathChangedListeners.keySet().iterator(); iterator.hasNext();)
					{
						String path = (String) iterator.next();
						if( absolutepath.startsWith(path))
						{
							PathChangedListener listener = (PathChangedListener)fieldPathChangedListeners.get(path);
							listener.pathChanged(event.kind().name(), absolutepath);
						}
					}
				}
				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if (kind == StandardWatchEventKinds.ENTRY_CREATE)
				{
					try
					{
						if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS))
						{
							registerAll(child);
						}
					}
					catch (IOException x)
					{
						// ignore to keep sample readbale
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid)
			{
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty())
				{
					break;
				}
			}
		}
	}

	public void addFolderTree(String inPath)
	{
		Path dir = Paths.get(inPath);
		try
		{
			if (this.watcher == null)
			{
				this.watcher = FileSystems.getDefault().newWatchService();
				this.keys = new HashMap<WatchKey, Path>();
				registerAll(dir);
				getExecutorManager().execute(this);
			}
			else
			{
				registerAll(dir);
			}
		}
		catch (IOException x)
		{
			// ignore to keep sample readbale
			throw new OpenEditException(x);
		}

		// enable trace after initial registration

	}

	public boolean hasFolderTree(String inAbsolutePath)
	{
		return fieldPathChangedListeners.containsKey(inAbsolutePath);
	}

	@Override
	public void eventFired(WebEvent inEvent)
	{
		if( "savingoriginal".equals( inEvent.getOperation() ))
		{
			getTimedCache().put("FolderMonitor", inEvent.get("absolutepath"),true);
		}
//		else if( "savingoriginalcomplete".equals( inEvent.getOperation() ))
//		{
//			fieldIgnoreList.remove(inEvent.get("absolutepath"));
//		}
		
	}

	//    public static void main(String[] args) throws IOException {
	//        // parse arguments
	//        if (args.length == 0 || args.length > 2)
	//            usage();
	//        boolean recursive = false;
	//        int dirArg = 0;
	//        if (args[0].equals("-r")) {
	//            if (args.length < 2)
	//                usage();
	//            recursive = true;
	//            dirArg++;
	//        }
	// 
	//        // register directory and process its events
	//        Path dir = Paths.get(args[dirArg]);
	//        new WatchDir(dir, recursive).processEvents();
	//    }
}
