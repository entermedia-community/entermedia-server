package org.entermediadb.ai.llama;

import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.openedit.CatalogEnabled;

public class LlamaConnection extends OpenAiConnection implements CatalogEnabled, LlmConnection
{
	@Override
	public String getModelIdentifier()
	{
		String modelname = getModelData().getId();
		return "/root/.cache/llama.cpp/unsloth_"+modelname+".gguf";
	}
	
}
