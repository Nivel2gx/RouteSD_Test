package com.artech.celeritas.sdpanel8;

import com.artech.android.ContextImpl;
import com.artech.application.MyApplication;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.IGxProcedure;
import com.artech.base.services.Services;
import com.artech.providers.EntityDataProvider;
import com.artech.controls.ads.Ads;
import com.artech.celeritas.sdpanel8.controls.*;
import com.genexus.Application;
import com.genexus.ClientContext;

public class MainApplication extends MyApplication
{
	@Override
	public final void onCreate()
	{
		GenexusApplication application = new GenexusApplication();
		application.setName("celeritas");
		application.setAPIUri("http://apps5.genexus.com/Id7d028be6183d09ec3fa92b6135a69e28/");
		application.setAppEntry("SDPANEL8");
		application.setMajorVersion(1);
		application.setMinorVersion(0);

		// Extensibility Point for Logging
 

		// Security
		application.setIsSecure(false);
		application.setEnableAnonymousUser(false);
		application.setClientId("");
		application.setLoginObject("");
		application.setNotAuthorizedObject("");
		application.setChangePasswordObject("");
		//application.setCompleteUserDataObject("");

		// Dynamic Url		
		application.setUseDynamicUrl(false);
		application.setDynamicUrlAppId("CELERITAS");

		// Ads
		application.setUseAds(false);
		application.setAdMobPublisherId("");
		// Notifications
		application.setUseNotification(false);
		application.setNotificationSenderId("");
		application.setNotificationRegistrationHandler("(none)");

		// Testing
		application.setUseTestMode(false);

		MyApplication.setApp(application);

		registerModule(new com.example.samplemodule.SampleModule());


		UserControls.initializeUserControls();

		super.onCreate();

		
		AndroidContext.ApplicationContext = new ContextImpl(getApplicationContext());
    }

	@Override
	public Class<? extends com.artech.services.EntityService> getEntityServiceClass()
	{
		return AppEntityService.class;
	}

	@Override
	public EntityDataProvider getProvider()
	{
		return new AppEntityDataProvider();
	}

}
