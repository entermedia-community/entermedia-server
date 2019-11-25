package org.entermediadb.projects;

import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.asset.MediaArchive;
import org.openedit.MultiValued;
import org.openedit.users.User;

public class TopicLabelPicker 
{
	
	MediaArchive fieldArchive;
	LibraryCollection fieldLibraryCollection;
	public MediaArchive getArchive() {
		return fieldArchive;
	}
	public void setArchive(MediaArchive fieldArchive) {
		this.fieldArchive = fieldArchive;
	}
	public LibraryCollection getLibraryCollection() {
		return fieldLibraryCollection;
	}
	public void setLibraryCollection(LibraryCollection fieldLibraryCollection) {
		this.fieldLibraryCollection = fieldLibraryCollection;
	}
	
	public String showLabel(MultiValued inTopic)
	{
		if( inTopic == null) 
		{
			return null;
		}
		Collection values = inTopic.getValues("parentcollectionid");
		if( values != null)
		{
			for (Iterator iterator = values.iterator(); iterator.hasNext();) {
				String id = (String) iterator.next();
				if( !id.equals(getLibraryCollection().getId()))
				{
					//Look it up?
					org.openedit.Data other = getArchive().getCachedData("librarycollection",id);
					String userid = (String)other.getValue("owner");
					User otherUser = getArchive().getUser(userid);
					return otherUser.getScreenName();
				}
			}
		}
		return "None";
		
	}
	

}
