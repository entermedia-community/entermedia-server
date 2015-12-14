package org.entermediadb.asset.generators;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;

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
