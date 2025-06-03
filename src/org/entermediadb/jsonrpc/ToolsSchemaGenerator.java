package org.entermediadb.jsonrpc;

import java.util.Collection;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;

public class ToolsSchemaGenerator {
	private Object id;
	private Collection<Data> modules;
	
	final private String inputSchema = """
		{
			"id": SCHEMA_ID,
			"jsonrpc": "2.0",
			"result": { 
				"tools": [
					{
						"name": "show_hint",
						"description": "Show hints on how to properly use the search function and provide examples. Use this function when people ask questions like: 'What can you do?', 'How can I search using this tool?', 'Give me example of some search queries'."
					},
					{
						"name": "emedia_takeaways",
						"description": "Searches and summarizes the key points from one or more documents",
						"inputSchema": {
							"type": "object", 
							"properties": {
								"keywords": {
									"type": "array",
									"description": "The keywords to search for files",
									"items": {
										"type": "string"
									}
								}
							},
							"required": ["keywords"]
						}
					},
					{
						"name": "search_module",
						"description": "Search with one or more keywords in one or more modules. Keywords can be mutually exclusive or inclusive depending on their phrasing. For example: 'search fils containing any of the words red or blue' is an exclusive search; 'search fils containing all of the words red and blue' is an inclusive search. If not specified, exclusive search will be used. If modules are not specified, all modules will be searched.",
						"inputSchema": {
							"type": "object", 
							"properties": {
								"keywords": {
									"type": "array",
									"description": "The keywords to search for",
									"items": {
										"type": "string"
									}
								},
								"conjunction": {
									"type": "string",
									"description": "The conjunction to use. Inclusive if user asks to search with all of the keywords. Exclusive if user asks to search with any of the keywords. Default value 'exclusive' if conjunction is not specified.",
									"enum": [
										"exclusive",
										"inclusive"
									]
								},
								"modules": {
									"anyOf": [
										{
											"type": "string",
											"enum": ["all"],
											"description": "Search all modules. Default value if modules are not specified."
										}
										USER_ENTITIES
									]
								}
							},
							"required": ["keywords"]
						}
					}
				]
			}
		}
		""";
	
	public ToolsSchemaGenerator(Object id, Collection<Data> modules) {
		this.id = id;
		this.modules = modules;
	}
	
	private String getAnyOf(Data module)
	{
		JSONObject result = new JSONObject();
		JSONArray enumArr = new JSONArray();
		
		enumArr.add(module.getId());
		enumArr.add(module.getName());
		
		result.put("type", "string");
		result.put("enum", enumArr);
		
		String description = "This is "+ module.get("name") +" module.";
		String ai_cont = module.get("aisearch_contains");
		if(ai_cont != null && ai_cont.length() > 0) 
		{
			description = "This module includes " + module.get("aisearch_contains");
		}
		
		result.put("description", description);
		
		return "," + result.toJSONString();
	}

	public String generate() { 

		String toolsSchema = inputSchema.replace("SCHEMA_ID", String.valueOf(id));
		
		String modulesSchema = "";
		for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			modulesSchema += getAnyOf(data);
		}

		toolsSchema = toolsSchema.replace("USER_ENTITIES", modulesSchema);

		return toolsSchema;
    }
}
