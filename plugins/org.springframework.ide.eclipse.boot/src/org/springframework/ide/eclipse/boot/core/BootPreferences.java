/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.core;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.springframework.ide.eclipse.boot.util.StringUtil;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;

/**
 * @author Kris De Volder
 */
public class BootPreferences implements IPreferenceChangeListener {

	public static final String PREF_BOOT_PROJECT_EXCLUDE = "org.springframework.ide.eclipse.boot.project.exclude";
	public static final Pattern DEFAULT_BOOT_PROJECT_EXCLUDE = Pattern.compile("^$");

	private static BootPreferences INSTANCE = null;
	private IEclipsePreferences prefs;
	private final LiveExpression<Pattern> projectExclude = new LiveExpression<Pattern>(DEFAULT_BOOT_PROJECT_EXCLUDE) {
		@Override
		protected Pattern compute() {
			return compileProjectExclude();
		}
	};

	private BootPreferences() {
		this.prefs = getPrefs();
		this.prefs.addPreferenceChangeListener(this);
		this.projectExclude.refresh();
	}

	public synchronized static BootPreferences getInstance() {
		if (INSTANCE==null) {
			INSTANCE = new BootPreferences();
		}
		return INSTANCE;
	}

	protected IEclipsePreferences getPrefs() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(BootActivator.PLUGIN_ID);
		return prefs;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(PREF_BOOT_PROJECT_EXCLUDE)) {
			projectExclude.refresh();
		}
	}

	public Pattern getProjectExclusion() {
		return projectExclude.getValue();
	}

	private Pattern compileProjectExclude() {
		try {
			if (prefs!=null) {
				String patternString = prefs.get(PREF_BOOT_PROJECT_EXCLUDE, null);
				if (StringUtil.hasText(patternString)) {
					return Pattern.compile(patternString);
				}
			}
		} catch (Exception e) {
			BootActivator.log(e);
		}
		//Ensure there's always some default pattern returned, no matter what!
		return Pattern.compile("^$");
	}

	public LiveExpression<Pattern> getProjectExclusionExp() {
		return projectExclude;
	}

}
