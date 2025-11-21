package org.entermediadb.ai.llm.llama;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.openai.OpenAiConnection;

public class LlamaOpenAiConnection extends OpenAiConnection {
	private static Log log = LogFactory.getLog(LlamaOpenAiConnection.class);
	
	@Override
	public String getLlmProtocol()
	{
		//return "openai"; //This might not work. We might need to tweak the JSON
		return "llama";
	}
}
