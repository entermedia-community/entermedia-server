/*
 * Created on Jul 19, 2006
 */
package org.entermediadb.elasticsearch;

import java.util.Collection;
import java.util.Date;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.util.DateStorageUtil;

public class ElasticSearchQuery extends SearchQuery
{
	public ElasticSearchQuery()
	{
		// TODO Auto-generated constructor stub
	}

//	protected transient NumberUtils fieldNumberUtils;
//
//	public NumberUtils getNumberUtils()
//	{
//		if (fieldNumberUtils == null)
//		{
//			fieldNumberUtils = new NumberUtils();
//
//		}
//
//		return fieldNumberUtils;
//	}
//
//	public void setNumberUtils(NumberUtils inNumberUtils)
//	{
//		fieldNumberUtils = inNumberUtils;
//	}

	public Term addAfter(String inString, Date inSearchDate)
	{
		PropertyDetail detail = getPropertyDetails().getDetail(inString);
		if(detail == null)
		{
			detail = new PropertyDetail();
			detail.setId(inString);
			detail.setDataType("date");
		}
		return addAfter(detail, inSearchDate);
	}
	public void addJoinFilter(SearchQuery filterQuery, String inFilterColumn, boolean inFilterHasMultiValues, String filterSearchType, String inResultsColumn)
	{
		//TODO: Upgrade to ElasticSearch and create a smart object that will perform the 
		//https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-has-parent-query.html
	}
	public Term addAfter(PropertyDetail inFieldId,final Date inDate)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				String date = DateTools.dateToString(inDate, Resolution.SECOND);
				String fin = getDetail().getId() + ":[" + date + " TO 99999999999999]";
				return fin;
			}
		};
		term.setDetail(inFieldId);
		String valueof= DateStorageUtil.getStorageUtil().formatForStorage(inDate);

		term.setValue(valueof);
		term.setOperation("afterdate");
		getTerms().add(term);
		return term;
	}

	public Term addBetween(PropertyDetail inFieldId, final Date inAfter, final Date inBefore)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				String lowDate = DateTools.dateToString(inAfter, Resolution.SECOND);
				String highDate = DateTools.dateToString(inBefore, Resolution.SECOND);
				String fin = getDetail().getId() + ":[" + lowDate + " TO " + highDate + "]";
				return fin;
			}
		};
		String lowDate = getDateFormat().format(inAfter);
		String highDate = getDateFormat().format(inBefore);
		term.setValue(lowDate + " - " + highDate);
		term.setDetail(inFieldId);
		term.addParameter("afterDate", lowDate);
		term.addParameter("beforeDate", highDate);
		term.setOperation("betweendates");
		getTerms().add(term);
		return term;
	}

	public Term addBefore(PropertyDetail inField,final  Date inDate)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				String date = DateTools.dateToString(inDate, Resolution.SECOND);
				String fin = getDetail().getId() + ":[00000000000000 TO " +date + "]";
				return fin;
			}
		};
		term.setOperation("beforedate");
		term.setDetail(inField);

		String valueof= DateStorageUtil.getStorageUtil().formatForStorage(inDate);
		term.setValue(valueof);

		getTerms().add(term);
		return term;
	}
	
	@Override
	public Term addOrsGroup(String inString, Collection<String> inValues)
	{
		return super.addOrsGroup(inString, inValues); //this calls the Array one
	}
	
	@Override
	public Term addOrsGroup(PropertyDetail inField, String[] inValues)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				StringBuffer orString = new StringBuffer();
				Object[] values = getValues();
				if (values.length > 0)
				{
					orString.append("(");
					for (int i = 0; i < values.length - 1; i++)
					{
						if(values[i].toString().length() > 0)
						{
							orString.append(values[i]);
							orString.append(" OR ");
						}
					}
					orString.append(values[values.length - 1]);
					orString.append(")");
				}
				return getDetail().getId() + ":" + orString.toString();
			}
		};
		term.setDetail(inField);
		term.setId(inField.getId());
		//term.setValue(inValue);
		//String[] orwords = inValue.split("\\s+");
		term.setValues(inValues);
		term.setOperation("orsGroup");
		getTerms().add(term);
		return term;
	}
	
	public Term addOrsGroup(PropertyDetail inField, String inValue)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				StringBuffer orString = new StringBuffer();
				Object[] values = getValues();
				if (values.length > 0)
				{
					orString.append("(");
					for (int i = 0; i < values.length - 1; i++)
					{
						if(values[i].toString().length() > 0)
						{
							orString.append(values[i]);
							orString.append(" OR ");
						}
					}
					orString.append(values[values.length - 1]);
					orString.append(")");
				}
				return getDetail().getId() + ":" + orString.toString();
			}
		};
		term.setDetail(inField);
		term.setId(inField.getId());
		term.setValue(inValue);
		String[] orwords = inValue.split("\\s+");
		term.setValues(orwords);
		term.setOperation("orsGroup");
		getTerms().add(term);
		return term;
	}
	
	public Term addMatches(PropertyDetail inField, String inValue)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				String inVal = getValue();
				if( inVal != null && inVal.startsWith("'") && inVal.endsWith("'"))
				{
					inVal = inVal.replace('\'', '\"');
				}

				if (getDetail().getId() != null)
				{
					return getDetail().getId() + ":(" + inVal + ")";
				}
				else
				{
					return inVal;
				}
			}
		};
		term.setOperation("matches");
		term.setDetail(inField);
		term.setValue(inValue);
		addTerm(term);
		return term;
	}

	public Term addStartsWith(PropertyDetail inField, String inVal)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				StringBuffer q = new StringBuffer();
				q.append(getDetail().getId());
				q.append(":(");

				if (getValue().startsWith("\""))
				{
					q.append(getValue());
				}
				else
				{
					String[] spaces = getValue().split("\\s+");
					for (int i = 0; i < spaces.length; i++)
					{
						String chunk = spaces[i];
						q.append(chunk);
						if (chunk.indexOf('*') == -1)
						{
							q.append('*');
						}
						if (i + 1 < spaces.length)
						{
							q.append(' ');
						}
					}
				}
				q.append(")");
				return q.toString();
			}
		};
		term.setOperation("startswith");
		term.setDetail(inField);
		term.setValue(inVal);
		addTerm(term);
		return term;
	}

	public Term addNots(PropertyDetail inField, String inNots)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				StringBuffer orString = new StringBuffer();

				String[] notwords = getValue().split("\\s");
				if (notwords.length > 0)
				{
					for (int i = 0; i < notwords.length; i++)
					{
						orString.append(" NOT " + notwords[i]);
					}
				}
				return orString.toString();
			}
		};
		term.setOperation("notgroup");
		term.setDetail(inField);
		term.setValue(inNots);
		getTerms().add(term);
		return term;
	}

	public Term addExact(PropertyDetail inField, String inValue)
	{
		if( inValue == null)
		{
			return null;
		}
		Term term = new Term()
		{
			public String toQuery()
			{
				String val = getValue();
				if(val.startsWith("\""))
				{
					val = val.substring(1);
				}
				if(val.endsWith("\""))
				{
					val = val.substring(0,val.length()-2);
				}
				val = val.replace("\"", "\\\"");
				return getDetail().getId() + ":\"" + val + "\"";
			}
		};
		term.setOperation("exact");
		term.setDetail(inField);
		term.setValue(inValue);
		addTerm(term);
		return term;
	}

	public void addExact(String inValue)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				return "\"" + getValue() + "\"";
			}
		};

		term.setOperation("exact");
		term.setValue(inValue);
		addTerm(term);

	}

	public Term addNot(PropertyDetail inField, String inVal)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				return "-" + getDetail().getId() + ":" + getValue();
			}
		};
		term.setDetail(inField);
		term.setValue(inVal);
		term.setOperation("not");
		getTerms().add(term);
		return term;
	}
/* is this used anyplace?
	public void addCategoryFilter(List inRemaining, String inFriendly)
	{
		final List categories = inRemaining;
		Term term = new Term()
		{
			public String toQuery()
			{
				return "-" + getId() + ":" + getValue() + "";
			}

			public Element toXml()
			{
				Element term = DocumentHelper.createElement("term");
				term.addAttribute("id", getId());
				term.addAttribute("val", getValue());
				term.addAttribute("op", "categoryfilter");

				for (Iterator iterator = categories.iterator(); iterator.hasNext();)
				{
					String category = (String) iterator.next();
					Element cat = term.addElement("category");
					cat.addAttribute("categoryid", category);
				}

				return term;
			}
		};
		term.setId("category");
		StringBuffer all = new StringBuffer();
		all.append("(");
		for (Iterator iter = inRemaining.iterator(); iter.hasNext();)
		{
			String cat = (String) iter.next();
			all.append(cat);
			all.append(" ");
		}
		all.append(")");
		term.setValue(all.toString());
		addTerm(term);
	}
*/
	
	public Term addLessThan(PropertyDetail inFieldId, long val)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				return getValue();
			}
		};
		term.setOperation("lessthannumber");
		term.setDetail(inFieldId);
		term.setValue(String.valueOf( val ) );
		addTerm(term);
		return term;
	}


	
	public Term addGreaterThan(PropertyDetail inFieldId,final long high)
	{
		Term term = new Term()
		{
			public String toQuery()
			{
				return getValue();
			}

		};
		term.setOperation("greaterthannumber");
		term.setDetail(inFieldId);
		term.setValue(String.valueOf( high) );
		addTerm(term);
		return term;
	}
	public Term addExact(PropertyDetail inField, long inParseInt)
	{

		Term term = new Term()
		{
			public String toQuery()
			{
				return getValue();
			}

		};
		term.setOperation("greaterthannumber");
		term.setDetail(inField);
		term.setValue(String.valueOf(inParseInt));
		addTerm(term);
		return term;

	}
	public Term addBetween(PropertyDetail inField, long lowval, long highval)
	{
		// lowval = pad(lowval);
		// highval = pad(highval);
		Term term = new Term()

		{
			public String toQuery()
			{
				return getValue();
			}

		};
		term.setDetail(inField);
		term.setOperation("betweennumbers");
		term.addParameter("lowval", String.valueOf(  lowval ) );
		term.addParameter("highval", String.valueOf(highval));
		term.setValue(lowval  + " to "  + highval);
		addTerm(term);
		return term;
	}
	
	
	public Term addBetween(PropertyDetail inField, double lowval, double highval)
	{
		// lowval = pad(lowval);
		// highval = pad(highval);
		Term term = new Term()

		{
			public String toQuery()
			{
				return getValue();
			}

		};
		term.setDetail(inField);
		term.setOperation("betweennumbers");
		term.addParameter("lowval", String.valueOf(  lowval ) );
		term.addParameter("highval", String.valueOf(highval));
		term.setValue(lowval  + " to "  + highval);
		addTerm(term);
		return term;
	}
	
	
	public String toQuery()
	{
		StringBuffer done = new StringBuffer();
		String op = null;
		if (isAndTogether())
		{
			op = "+";
		}
		else
		{
			op = " OR ";
		}
		if( getTerms().size() > 0)
		{
			
			for (int i = 0; i < fieldTerms.size(); i++)
			{
				Term field = (Term) fieldTerms.get(i);
				String q = field.toQuery();
				if (i > 0 && !q.startsWith("+") && !q.startsWith("-"))
				{
					done.append(op);
				}
				done.append(q);
				if (i + 1 < fieldTerms.size())
				{
					done.append(" ");
				}
			}

//			if (!isAndTogether())
//			{
//				done.append(")");
//			}
		}
		if( fieldChildren != null && fieldChildren.size() > 0)
		{
			if( getTerms().size() > 0 )
			{
				
			}
			for (int j = 0; j < getChildren().size(); j++)
			{
				SearchQuery child = (SearchQuery) getChildren().get(j);
				String query = child.toQuery();
				boolean enclose = true;
				if (query.startsWith("+") || query.startsWith("-"))
					enclose = false;
					
				//&& !query.startsWith("+") && !query.startsWith("-")
				if (j > 0 )
				{
					done.append(" ");
					if( isAndTogether())
					{
						if (enclose)
						{
							done.append("+(");	
						}
					}
					else
					{
						done.append("OR ");
						if (enclose)
						{
							done.append("(");	
						}
					}
				}
				else if (enclose)
				{
					done.append("(");
				}
				done.append(query);
				
				if (enclose)
				{
					done.append(")");	
				}
			}
		}
		return done.toString();
	}

}
