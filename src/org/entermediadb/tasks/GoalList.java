package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class GoalList
{
	protected List sortedIds;
	protected List fieldSorted;
	
	public List getSorted()
	{
		return fieldSorted;
	}

	public void setSorted(List inSortedTasks)
	{
		fieldSorted = inSortedTasks;
	}

	public List getSortedIds()
	{
		return sortedIds;
	}

	public void setSortedIds(List inSortedIds)
	{
		sortedIds = inSortedIds;
	}

	public GoalList(MultiValued inParent, Collection inChildren)
	{
		Collection sorted = inParent.getValues("countdata");
		if( sorted == null)
		{
			sorted = new ArrayList();
		}
		setSortedIds((List)sorted);
		sort(inChildren);
	}

	public void sort(Collection goals)
	{
		ArrayList sorted = new ArrayList(goals);
		sorted.sort(new Comparator<MultiValued>()
		{
			@Override
			public int compare(MultiValued inO1, MultiValued inO2)
			{
				Integer index1 = getSortedIds().indexOf(inO1.getId());
				Integer index2 = getSortedIds().indexOf(inO2.getId());
				if(index1 == -1 && index2 == -1)
				{
					//sort by date
					Date date1 = (Date)inO1.getDate("creationdate");
					Date date2 = (Date)inO2.getDate("creationdate");
					if( date1 != null && date2 != null)
					{
						return date1.compareTo(date2);
					}
					return 0;
				}
				else if(index2 == -1)
				{
					/*
					 if s1 > s2, it returns positive number  
					 if s1 < s2, it returns negative number  
					 if s1 == s2, it returns 0  
					 */
					return 1;
				}
				else if(index1 == -1)
				{
					return -1;
				}
				return index1.compareTo(index2);
			}
		});
		setSorted(sorted);
	}
}
