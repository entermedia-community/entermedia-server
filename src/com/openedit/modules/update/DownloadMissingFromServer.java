package com.openedit.modules.update;

import java.util.Map;

import com.openedit.OpenEditException;

public class DownloadMissingFromServer extends SyncToServer
{

	protected void deleteExtraFiles( Map inLocal ) throws OpenEditException
	{
		//Do nothing... this is a download only, not a sync
	}
}
