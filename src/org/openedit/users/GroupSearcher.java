package org.openedit.users;

import org.openedit.data.Searcher;

import com.openedit.users.Group;

public interface GroupSearcher extends Searcher
{

	public abstract Group getGroup(String inGroupId);

}