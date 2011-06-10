package org.openedit.users;

import java.util.List;

import org.openedit.data.Searcher;

import com.openedit.hittracker.HitTracker;
import com.openedit.users.Group;
import com.openedit.users.User;

public interface UserSearcher extends Searcher
{

	public abstract User getUser(String inAccount);

	public abstract User getUserByEmail(String inEmail);

	public abstract HitTracker getUsersInGroup(Group inGroup);

	public abstract void saveUsers(List userstosave, User user);

}