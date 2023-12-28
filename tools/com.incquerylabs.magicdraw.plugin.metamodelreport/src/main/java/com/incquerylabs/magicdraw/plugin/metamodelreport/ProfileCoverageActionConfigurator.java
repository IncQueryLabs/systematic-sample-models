package com.incquerylabs.magicdraw.plugin.metamodelreport;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.incquerylabs.magicdraw.plugin.metamodelreport.actions.ProfileCoverageAction;
import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsStateUpdater;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;

public class ProfileCoverageActionConfigurator implements BrowserContextAMConfigurator {

	@Override
	public void configure(ActionsManager manager, Tree tree) {
		ActionsCategory cat = manager.getCategory("REPORT");
		if(cat == null) {
			cat = new MDActionsCategory("REPORT", "Generate Report...");
			manager.addCategory(cat);
		}
		Set<Profile> selectedObjects = Arrays.stream(tree.getSelectedNodes()).map(Node::getUserObject)
				.filter(Profile.class::isInstance).map(Profile.class::cast).collect(Collectors.toSet());
		if(!selectedObjects.isEmpty()) {
			cat.addAction(new ProfileCoverageAction(selectedObjects.iterator().next()));
			ActionsStateUpdater.updateActionsState();
		}
	}

	@Override
	public int getPriority() {
		return AMConfigurator.MEDIUM_PRIORITY;
	}
}
