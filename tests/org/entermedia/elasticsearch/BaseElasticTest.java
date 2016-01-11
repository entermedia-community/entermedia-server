package org.entermedia.elasticsearch;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.TestFixture;

public class BaseElasticTest extends BaseEnterMediaTest
{

	public TestFixture getFixture()
	{
		return super.getStaticFixture();
	}
}
