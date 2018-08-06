/*
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
package org.entermediadb.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AutomaticWebsocketScanner implements ServerApplicationConfig
{
	private static final Log log = LogFactory.getLog(AutomaticWebsocketScanner.class);
	ModuleManagerLoader configurator = new ModuleManagerLoader();
	//This file is in jasper.jar META-INF/services/javax.servlet.ServletContainerInitializer
	public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned)
	{
		log.info("loading endpoints " + scanned.size());
		Set<ServerEndpointConfig> result = new HashSet<ServerEndpointConfig>();
		
		for (Iterator iterator = scanned.iterator(); iterator.hasNext();)
		{
			Class endpoint = (Class) iterator.next();
			String path = endpoint.getTypeName();
			if (path.startsWith("org.entermediadb"))
			{
				path = path.replace(".", "/");
				ServerEndpointConfig conf = ServerEndpointConfig.Builder.create(
						endpoint, "/entermedia/services/websocket/" + path)  //FYI: Path Configured in OpenEditFilter
						.configurator(configurator).build();
				result.add(conf);
				log.info("configured /" + path);
			}	
		}
		return result;
	}

	public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned)
	{
		/*
		log.info("loading classes " + scanned.size());
		Set<Class<?>> results = new HashSet<Class<?>>();
		for (Class<?> clazz : scanned)
		{
			if (clazz.getPackage().getName().startsWith("org.entermediadb"))
			{
				if(  javax.websocket.Endpoint.class.isAssignableFrom(clazz) )
				{
					results.add(clazz);
					log.info("configured " + clazz.getPackage().getName());
				}
			}
		}
		return results;
		*/
		return Collections.emptySet();
	}
}
