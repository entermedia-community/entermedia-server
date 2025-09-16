/**
 *       Copyright 2010 Newcastle University
 *
 *          http://research.ncl.ac.uk/smart/
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openedit.entermedia.util;

import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.token.BasicOAuthToken;
import org.apache.oltu.oauth2.common.token.OAuthToken;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;
import org.openedit.OpenEditException;

/**
 *
 *
 *
 */
public class EmTokenResponse extends OAuthAccessTokenResponse
{

	protected JSONObject fieldData;
	
	
	public JSONObject getData()
	{
		return fieldData;
	}

	public void setData(JSONObject inData)
	{
		fieldData = inData;
	}

	public String getAccessToken()
	{
		return (String) getData().get("access_token");
	}

	public Long getExpiresIn()
	{
		Long value = (Long) getData().get("expires_in");
		return value;
	}

	public String getRefreshToken()
	{
		return (String) getData().get("refresh_token");
	}

	public String getScope()
	{
		return (String) getData().get("scope");
	}

	public OAuthToken getOAuthToken()
	{
		return new BasicOAuthToken(getAccessToken(), getExpiresIn(), getRefreshToken(), getScope());
	}

	protected void setBody(String body)
	{
		this.body = body;
		JSONParser parser = new JSONParser();
		try{
		this.fieldData = (JSONObject) parser.parse(body);
		} catch (Exception e){
			throw new OpenEditException(e);
		}

	}

	protected void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	protected void setResponseCode(int code)
	{
		this.responseCode = code;
	}
	
	
	@Override
	public String getParam(String inParam)
	{
		
		return  (String) getData().get(inParam);
	}
	

}