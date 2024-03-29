/*
 *
 * Copyright (c) 2002 NoMagic, Inc. All Rights Reserved.
 */

package com.incquerylabs.magicdraw.plugin.metamodelreport;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.MDActionsCategory;

public class MainMenuConfigurator implements AMConfigurator {

	String REPORTING = "Report";

	/**
	 * Action will be added to manager.
	 */
	private NMAction[] actions;

	/**
	 * Creates configurator.
	 * 
	 * @param action
	 *            action to be added to main menu.
	 */
	public MainMenuConfigurator(NMAction... actions) {
		this.actions = actions;
	}

	/**
	 * @see com.nomagic.actions.AMConfigurator#configure(ActionsManager) Methods
	 *      adds action to given manager Examples category.
	 */
	@Override
	public void configure(ActionsManager manager) {
		// searching for Examples action category
		ActionsCategory category = (ActionsCategory) manager.getActionFor(REPORTING);

		if (category == null) {
			// creating new category
			category = new MDActionsCategory(REPORTING, REPORTING);
			category.setNested(true);
			manager.addCategory(category);
		}
		for (NMAction action : actions) {
			category.addAction(action);
		}
	}

	@Override
	public int getPriority() {
		return AMConfigurator.MEDIUM_PRIORITY;
	}

}