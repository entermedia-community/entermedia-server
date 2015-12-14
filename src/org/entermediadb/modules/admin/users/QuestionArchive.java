package org.entermediadb.modules.admin.users;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.openedit.OpenEditException;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

public class QuestionArchive
{
	XmlArchive fieldXmlArchive;
	List fieldQuestions;
	long lastModDate;
	
	public Question getRandomQuestion() throws OpenEditException
	{
		if( getQuestions().size() == 0)
		{
			return null;
		}
		double d = Math.random();
		d = d * ((double)getQuestions().size()-1D);
		int hit = (int)Math.round(d);
		
		return (Question)getQuestions().get(hit);
	}
	public List getQuestions() throws OpenEditException
	{
		String xmlpath = "/system/questions.xml";
		long now = getXmlArchive().getLastModified(xmlpath);
		boolean reload = false;
		if( now != lastModDate )
		{
			reload = true;
		}
		
		if (reload || fieldQuestions == null)
		{
			XmlFile file = getXmlArchive().getXml(xmlpath);
			fieldQuestions = new ArrayList();
			if( file.isExist() )
			{
				for (Iterator iterator = file.getElements().iterator(); iterator.hasNext();)
				{
					Element elem = (Element) iterator.next();
					Question q = new Question();
					q.setId(elem.attributeValue("id"));
					q.setDescription(elem.attributeValue("text"));
					q.setAnswer(elem.attributeValue("answer"));
					fieldQuestions.add(q);
				}
			}
			lastModDate = file.getLastModified();
		}
		return fieldQuestions;
	}
	public Question getQuestion(String inId) throws OpenEditException
	{
		for (Iterator iter = getQuestions().iterator(); iter.hasNext();)
		{
			Question q = (Question) iter.next();
			if ( q.getId().equals(inId) )
			{
				return q;
			}
		}
		return null;
	}
	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}
	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}
}
