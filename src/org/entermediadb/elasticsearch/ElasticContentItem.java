package org.entermediadb.elasticsearch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.openedit.Data;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.util.ReaderInputStream;

class ElasticContentItem extends StringItem implements Data{
	
	
	protected Boolean existed;
	public Data getElasticData() {
		return fieldElasticData;
	}

	public void setElasticData(Data inElasticData) {
		fieldElasticData = inElasticData;
	}

	protected Data fieldElasticData;	

	public InputStream getInputStream() throws RepositoryException {
		if (getOutputEncoding() == null) {
			return new ByteArrayInputStream(getContent().getBytes());
		}
		try {
			// BufferedReader reader = new BufferedReader ( new
			// InputStreamReader ( in ) );
			return new ReaderInputStream(new StringReader(getContent()), getOutputEncoding());
		} catch (Exception ex) {
			throw new RepositoryException(ex);
		}
	}
	public Collection getObjects(String inField)
	{
		Collection values = (Collection)getValue(inField);
		return values;
	}
	public boolean exists() {
		return true;
	}


	public void setId(String inNewid) {
		 getElasticData().setId(inNewid);

		
	}

	@Override
	public String getName(String inLocale) {
		return getElasticData().getName();
	}

	@Override
	public void setName(String inName) {
		getElasticData().setName(inName);
		
	}

	@Override
	public void setSourcePath(String inSourcepath) {
		getElasticData().setSourcePath(inSourcepath);
		
	}

	@Override
	public String getSourcePath() {
	 return getElasticData().getSourcePath();
	}

	@Override
	public void setProperty(String inId, String inValue) {
		getElasticData().setProperty(inId, inValue);
		
	}

	@Override
	public String get(String inId) {
		return getElasticData().get(inId);
	}

	@Override
	public Object getValue(String inKey) {
	return getElasticData().getValue(inKey);
	}

	@Override
	public void setValue(String inKey, Object inValue) {
		getElasticData().setValue(inKey, inValue);
		
	}

	@Override
	public Map getProperties() {
		return getElasticData().getProperties();
	}

	@Override
	public void setProperties(Map inProperties) {
		 getElasticData().setProperties(inProperties);
	}

	@Override
	public Set keySet()
	{
		return getElasticData().keySet();
	}

}