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
package org.entermedia.websocket;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.websocket.annotation.AnnotationConnection;
import org.entermedia.websocket.annotation.AnnotationServer;

public class AnnotationServerConfig implements ServerApplicationConfig
{
	private static final Log log = LogFactory.getLog(AnnotationServerConfig.class);
	GetHttpSessionConfigurator configurator = new GetHttpSessionConfigurator();
	
	@Override
	public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned)
	{
		log.info("loading endpoints " + scanned.size());
		Set<ServerEndpointConfig> result = new HashSet<ServerEndpointConfig>();
		if (scanned.contains(AnnotationConnection.class))
		{
			ServerEndpointConfig conf = ServerEndpointConfig.Builder.create(
					AnnotationConnection.class, "/entermedia/services/websocket/echoProgrammatic")
					.configurator(configurator).build();
			result.add(conf);
			log.info("configured /entermedia/services/websocket/echoProgrammatic");
		}
		return result;
	}

	@Override
	public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned)
	{
		// Deploy all WebSocket endpoints defined by annotations in the examples
		// web application. Filter out all others to avoid issues when running
		// tests on Gump
		log.info("loading classes " + scanned.size());
		Set<Class<?>> results = new HashSet<Class<?>>();
		for (Class<?> clazz : scanned)
		{
			if (clazz.getPackage().getName().startsWith("org.entermedia.websocket."))
			{
				results.add(clazz);
				log.info("configured " + clazz.getPackage().getName());
			}
		}
		return results;
	}
}
