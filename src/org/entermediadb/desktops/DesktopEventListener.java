package org.entermediadb.desktops;

import org.entermediadb.projects.LibraryCollection;

public interface DesktopEventListener
{
	public void checkoutCollection(LibraryCollection inCollection);
	public void uploadCollection(LibraryCollection inCollection);	
}
