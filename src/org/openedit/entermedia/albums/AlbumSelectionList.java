/*
 * Created on Jul 1, 2006
 */
package org.openedit.entermedia.albums;

import java.util.ArrayList;
import java.util.List;

import com.openedit.page.manage.PageManager;
import com.openedit.users.User;

public class AlbumSelectionList
{
	protected User fieldUser;
	protected PageManager fieldPageManager;
	protected List<Album> fieldNamedAlbums;
	protected List<Album> fieldSelectionAlbums;

	protected Album fieldSelectedAlbum;

	/**
	 * @deprecated use albumSearcher for doing this
	 */
	public List<Album> getNamedAlbums()
	{
		if (fieldNamedAlbums == null)
		{
			fieldNamedAlbums = new ArrayList<Album>();
		}
		return fieldNamedAlbums;
	}

	public Album getSelectedAlbum()
	{
		return fieldSelectedAlbum;
	}

	public void setSelectedAlbum(Album inSelectedAlbum)
	{
		fieldSelectedAlbum = inSelectedAlbum;
	}

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public void reload()
	{
	}

	public boolean isCurrent()
	{
		return false;
	}

	public List<Album> getSelectionAlbums()
	{
		if (fieldSelectionAlbums == null)
		{
			fieldSelectionAlbums = new ArrayList<Album>();
		}
		return fieldSelectionAlbums;
	}

	public Album getAlbum(String inId)
	{
		Album col = getSelectionAlbum(inId);
		if (col != null)
		{
			return col;
		}
		return getNamedAlbum(inId);
	}

	/**
	 * @deprecated use albumSearcher for doing this
	 */
	public Album getNamedAlbum(String inId)
	{
		for (Album album: getNamedAlbums())
		{
			if (album.getId().equals(inId))
			{
				return album;
			}
		}
		return null;
	}

	public Album getSelectionAlbum(String inId)
	{
		for (Album album : getSelectionAlbums())
		{
			if (album.getId().equals(inId))
			{
				return album;
			}
		}
		return null;
	}

	public void removeAlbum(Album inCol)
	{
		Album found = null;
		for( Album album: getNamedAlbums() )
		{
			if (album.getId().equals(inCol.getId()))
			{
				found = album;
				break;
			}
		}
		getNamedAlbums().remove(found);
	}

	public boolean hasAlbums()
	{
		if (fieldNamedAlbums == null || fieldNamedAlbums.size() == 0)
		{
			return false;
		}
		return true;
	}

	public void addAlbum(Album inAlbum)
	{
		if (getNamedAlbum(inAlbum.getId()) == null)
		{
			if(inAlbum.isSelection())
			{
				getSelectionAlbums().add(inAlbum);
			}
			else
			{
				getNamedAlbums().add(inAlbum);
			}
		}
	}

	public void setSelectionAlbums(List<Album> selectionAlbums) {
		fieldSelectionAlbums = selectionAlbums;
	}
	
	public void sortSelections()
	{
		List<Album> selections = new ArrayList<Album>();
		for(int i = 0; i < getSelectionAlbums().size(); i++)
		{
			Album album = getSelectionAlbum(String.valueOf(i+1));
			if (album != null)
			{
				selections.add(album);
			}
		}
		setSelectionAlbums(selections);
	}
}
