package com.devxtend.sdmapadvanced;

import android.content.Context;
import android.util.Log;

import com.artech.framework.GenexusModule;
import com.artech.usercontrols.UcFactory;
import com.artech.usercontrols.UserControlDefinition;

/**
 *
 */
public class SDMapAdvancedModule implements GenexusModule {

	@Override
	public void initialize(Context context) {

		UserControlDefinition basicUserControl = new UserControlDefinition(
				SDMapAdvanced.NAME,
				SDMapAdvanced.class
		);

		UcFactory.addControl(basicUserControl);
	}
}
