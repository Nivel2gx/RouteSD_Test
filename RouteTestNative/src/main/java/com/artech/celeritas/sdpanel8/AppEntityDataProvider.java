package com.artech.celeritas.sdpanel8;

import com.artech.providers.EntityDataProvider;

public class AppEntityDataProvider extends EntityDataProvider
{
	public AppEntityDataProvider()
	{
		EntityDataProvider.AUTHORITY = "com.artech.celeritas.sdpanel8.appentityprovider";
		EntityDataProvider.URI_MATCHER = buildUriMatcher();
	}
}
