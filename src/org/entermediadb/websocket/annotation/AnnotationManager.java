/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.entermediadb.websocket.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.DateStorageUtil;

public class AnnotationManager  {

	private static final Log log = LogFactory.getLog(AnnotationManager.class);

	 private static final Set<AnnotationConnection> connections =
	            new CopyOnWriteArraySet<AnnotationConnection>();
	 
	 private static final String CACHENAME = "AnnotationServer";
	 
	 protected CacheManager fieldCacheManager;
	 protected ModuleManager fieldModuleManager;
	 protected SearcherManager fieldSearcherManager;
	 protected JSONParser fieldJSONParser;

	 public JSONParser getJSONParser()
	 {
		if (fieldJSONParser == null) {
			fieldJSONParser = new JSONParser();
		}
		return fieldJSONParser;
	 } 

	 public SearcherManager getSearcherManager()
	{
		if (fieldSearcherManager == null)
		{
			fieldSearcherManager =  (SearcherManager)getModuleManager().getBean("searcherManager");
		}
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	public CacheManager getCacheManager()
	{
		if (fieldCacheManager == null)
		{
			fieldCacheManager = (CacheManager)getModuleManager().getBean("cacheManager");//new CacheManager();
		}

		return fieldCacheManager;
	}
	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

    public void annotationModified(AnnotationConnection annotationConnection, JSONObject command, String message, String catalogid,  String inAssetId)
	{
    	//TODO: update our map
    	JSONObject obj = loadAnnotatedAsset(catalogid,inAssetId);
		List annotations = (List)obj.get("annotations");
		JSONObject annotation = (JSONObject)command.get("annotationdata");
		String id = (String)annotation.get("id");
		for (Iterator iterator = annotations.iterator(); iterator.hasNext();)
		{
			JSONObject existing = (JSONObject) iterator.next();
			if( id.equals( existing.get("id") ) )
			{
				int loc = annotations.indexOf(existing);
				annotations.remove(existing);
				if( loc > -1)
				{
					annotations.add(loc, annotation);
				}
				else
				{
					annotations.add(annotation);
				}
				saveAnnotationData(catalogid,  inAssetId, annotation);
				break;
			}
		}
    	//getCacheManager().put(CACHENAME, catalogid + inCollectionId + inAssetId, command.get("annotat"));
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			AnnotationConnection annotationConnection2 = (AnnotationConnection) iterator.next();
			annotationConnection2.sendMessage(command);
		}
	}
    
	public void annotationAdded(AnnotationConnection annotationConnection, JSONObject command, String message, String catalogid,  String inAssetId)
	{
		JSONObject obj = loadAnnotatedAsset(catalogid,inAssetId);
		Collection annotations = (Collection)obj.get("annotations");
		
		JSONObject annotation = (JSONObject)command.get("annotationdata");
		annotations.add(annotation);
		
		saveAnnotationData(catalogid,inAssetId, annotation);
		
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			AnnotationConnection annotationConnection2 = (AnnotationConnection) iterator.next();
			annotationConnection2.sendMessage(command);
		}
	}
    
	protected void saveAnnotationData(String inCatalogid,  String inAssetId, JSONObject inAnnotation)
	{
		String annotationid = (String)inAnnotation.get("id");
		
		Searcher searcher = getSearcherManager().getSearcher(inCatalogid, "annotation");
		Data data = (Data)searcher.searchById(annotationid);
		if( data == null)
		{
			data = searcher.createNewData();
		}
		
		data.setProperty("id", annotationid);
		data.setProperty("assetid", inAssetId);
		data.setProperty("date", (String)inAnnotation.get("date"));
		data.setProperty("user", (String)inAnnotation.get("user"));
		data.setProperty("comment", (String)inAnnotation.get("comment"));
		data.setProperty("color", (String)inAnnotation.get("color"));
		
		JSONArray array = (JSONArray)inAnnotation.get("fabricObjects");
		if( array != null)
		{
			data.setProperty("fabricObjects", array.toJSONString());
		}
		//user: null,
//		comment: "",
//		date : [],
//		fabricObjects: [], 
//		assetid: null
		searcher.saveData(data, null);
		
	}
	public void annotationRemoved(AnnotationConnection annotationConnection, JSONObject command, String message, String catalogid,  String inAssetId)
	{
		JSONObject obj = loadAnnotatedAsset(catalogid,inAssetId);
		Collection annotations = (Collection)obj.get("annotations");
		
		String removed = (String)command.get("annotationid");
		for (Iterator iterator = annotations.iterator(); iterator.hasNext();)
		{
			JSONObject existing = (JSONObject) iterator.next();
			String annotationid = (String)existing.get("id");
			if( removed.equals( annotationid ) )
			{
				annotations.remove(existing);
				break;
			}
		}
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "annotation");
		Data data = (Data)searcher.searchById(removed);
		searcher.delete(data, null);
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			AnnotationConnection annotationConnection2 = (AnnotationConnection) iterator.next();
			annotationConnection2.sendMessage(command);
		}
	}
	
	
	public void loadAnnotatedAsset(AnnotationConnection annotationConnection, String catalogid, String inAssetId)
	{
		JSONObject newcommand = new JSONObject(); //Get this from our map of annotatedAssets
		newcommand.put("command", "asset.loaded");
		
		JSONObject asset = loadAnnotatedAsset(catalogid, inAssetId);
		newcommand.put("annotatedAssetJson", asset);
		
		annotationConnection.sendMessage(newcommand);
	}
	public JSONObject loadAnnotatedAsset(String inCatalogId,  String inAssetId)
	{
    	JSONObject obj = (JSONObject)getCacheManager().get(CACHENAME, inCatalogId + inAssetId);
		if( obj == null)
		{
			//Goto database and load it?
			obj = new JSONObject();
			JSONObject assetData = new JSONObject();
			assetData.put("id",inAssetId);
			Data asset = getSearcherManager().getData(inCatalogId, "asset", inAssetId);
			if( asset == null)
			{
				return null;
			}
			assetData.put("sourcepath",asset.getSourcePath());
			assetData.put("name", asset.getName());
			obj.put("assetData",assetData);
			List annotations = loadAnnotations(inCatalogId,inAssetId);
			obj.put("annotations", annotations);
			
			obj.put("users", new ArrayList());
			obj.put("annotationIndex", new Integer(1));
			getCacheManager().put(CACHENAME, inCatalogId + inAssetId, obj);
		}
		return obj;
	}
	protected List loadAnnotations(String inCatalogId,  String inAssetId)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "annotation");
		HitTracker hits = searcher.query().match("assetid", inAssetId).search();
		
		List list = new ArrayList();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data annotation = (Data) iterator.next();
			JSONObject json = new JSONObject();
			//json.put("id", annotation.getId() );
			for (Iterator iterator2 = annotation.keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				String value = annotation.get(key);
				if( value != null)
				{
					if( "date".equals(key) )
					{
						value = DateStorageUtil.getStorageUtil().checkFormat(value);
					}
				}
				json.put(key,value);
			}
			String obj = (String)annotation.get("fabricObjects");
			if( obj != null)
			{
				try
				{
					Collection parsed = (List)getJSONParser().parse(obj);
					json.put("fabricObjects", parsed);
				}
				catch (ParseException e)
				{
					// TODO Auto-generated catch block
					log.error(e);
				}
			}
//			json.put("comment", annotation.get("comment") );
//			//indexCount: null,
//			//user: null,
//			comment: "",
//			date : [],
//			fabricObjects: [], 
//			assetid: null
			list.add(json);
		}
		
		return list;
	}
	public void removeConnection(AnnotationConnection inAnnotationConnection)
	{
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			AnnotationConnection annotationConnection2 = (AnnotationConnection) iterator.next();
			
			if (inAnnotationConnection == annotationConnection2)
			{
				connections.remove(annotationConnection2);
				break;
			}
		}
	}
	public void addConnection(AnnotationConnection inConnection)
	{
		// TODO Auto-generated method stub
		connections.add(inConnection);
	}
	
}    
//    private static class EchoMessageHandlerBinary
//            implements MessageHandler.Partial<ByteBuffer> {
//
//        private final RemoteEndpoint.Basic remoteEndpointBasic;
//
//        private EchoMessageHandlerBinary(RemoteEndpoint.Basic remoteEndpointBasic) {
//            this.remoteEndpointBasic = remoteEndpointBasic;
//        }
//
//        @Override
//        public void onMessage(ByteBuffer message, boolean last) {
//            try {
//                if (remoteEndpointBasic != null) {
//                    remoteEndpointBasic.sendBinary(message, last);
//                }
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

