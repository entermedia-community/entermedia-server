package org.openedit.data.lucene;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFieldVisitor;
import org.openedit.Data;

/**
 * A {@link StoredFieldVisitor} that creates a {@link Document} containing all
 * stored fields, or only specific requested fields provided to
 * {@link #DocumentStoredFieldVisitor(Set)}.
 * <p>
 * This is used by {@link IndexReader#document(int)} to load a document.
 * 
 */
public class SearchResultStoredFieldVisitor extends StoredFieldVisitor
{
	/** Load all stored fields. */
	public SearchResultStoredFieldVisitor(Map inKeys)
	{
		fieldKeys = inKeys;
		int size = inKeys.size();
		if( size == 0 )
		{
			size = 3;//at least id name sourcepath
		}
		fieldValues = new String[size];
	}
	protected Map<String, Integer> fieldKeys;
	public Map<String, Integer> getKeys()
	{
		return fieldKeys;
	}

	public void setKeys(Map<String, Integer> inKeys)
	{
		fieldKeys = inKeys;
	}

	public String[] getValues()
	{
		return fieldValues;
	}

	public void setValues(String[] inValues)
	{
		fieldValues = inValues;
	}

	protected String[] fieldValues;


	protected void putValue(String inKey, String inValue)
	{
		Integer index = fieldKeys.get(inKey);
		if (index == null)
		{
			index = fieldKeys.size();
			fieldKeys.put(inKey, index);
		}
		growTo(index);
		fieldValues[index] = inValue;
	}

	protected void growTo(Integer inIndex)
	{
		if (fieldValues.length <= inIndex)
		{
			String[] newsize = new String[inIndex + 1]; //grow only a little, this should not happen often
			System.arraycopy(fieldValues, 0, newsize, 0, fieldValues.length);
			fieldValues = newsize;
		}

	}

	@Override
	public void binaryField(FieldInfo fieldInfo, byte[] value) throws IOException
	{
		if (value.length > 0)
		{
			StoredField stored = new StoredField(fieldInfo.name, value);
			putValue(fieldInfo.name, stored.stringValue());
		}
	}

	@Override
	public void stringField(FieldInfo fieldInfo, String value) throws IOException
	{
		final FieldType ft = new FieldType(TextField.TYPE_STORED);
		ft.setStoreTermVectors(fieldInfo.hasVectors());
		ft.setIndexed(fieldInfo.isIndexed());
		ft.setOmitNorms(fieldInfo.omitsNorms());
		ft.setIndexOptions(fieldInfo.getIndexOptions());
		String val = new Field(fieldInfo.name, value, ft).stringValue();
		putValue(fieldInfo.name,val);
	}

	@Override
	public void intField(FieldInfo fieldInfo, int value)
	{
		StoredField stored = new StoredField(fieldInfo.name, value);
		putValue(fieldInfo.name, stored.stringValue());
	}

	@Override
	public void longField(FieldInfo fieldInfo, long value)
	{
		StoredField stored = new StoredField(fieldInfo.name, value);
		putValue(fieldInfo.name, stored.stringValue());
	}

	@Override
	public void floatField(FieldInfo fieldInfo, float value)
	{
		StoredField stored = new StoredField(fieldInfo.name, value);
		putValue(fieldInfo.name, stored.stringValue());
	}

	@Override
	public void doubleField(FieldInfo fieldInfo, double value)
	{
		StoredField stored = new StoredField(fieldInfo.name, value);
		putValue(fieldInfo.name, stored.stringValue());
	}

	@Override
	public Status needsField(FieldInfo fieldInfo) throws IOException
	{
		return Status.YES;
	}
	
	public Data createSearchResult()
	{
		SearchResultData data = new SearchResultData(fieldKeys, fieldValues);
		return data;
	}
}
