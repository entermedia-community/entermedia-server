package org.openedit.entermedia.generators;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.BaseGenerator;
import com.openedit.generators.Output;
import com.openedit.page.Page;

public class NullGenerator extends BaseGenerator
{

	public void generate(WebPageRequest inContext, Page inPage, Output inOut) throws OpenEditException
	{
		//do nothing

	}
	public boolean canGenerate(WebPageRequest inReq)
	{
		return true;
	}

}
