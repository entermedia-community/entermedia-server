package org.entermediadb.ai.llm.llama;

import org.entermediadb.ai.llm.openai.OpenAiConnection;

public class LlamaConnection extends OpenAiConnection {
	@Override
	public String getModelIdentifier()
	{
		String modelname = getModelData().getId();
		return "/root/.cache/llama.cpp/unsloth_"+modelname+".gguf";
	}
	
}
