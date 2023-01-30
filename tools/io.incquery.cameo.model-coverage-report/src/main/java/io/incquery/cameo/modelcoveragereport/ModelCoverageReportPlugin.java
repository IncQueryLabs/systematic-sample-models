package io.incquery.cameo.modelcoveragereport;

import io.incquery.cameo.modelcoveragereport.actions.MetaModelCoverageAction;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.plugins.Plugin;

public class ModelCoverageReportPlugin extends Plugin {

	@Override
	public boolean close() {
		return true;
	}

	@Override
	public void init() {

		ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
		manager.addMainMenuConfigurator(new MainMenuConfigurator(new MetaModelCoverageAction()));
		manager.addContainmentBrowserContextConfigurator(new ProfileCoverageActionConfigurator());

	}

	@Override
	public boolean isSupported() {
		return true;
	}

}
