/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.springframework.ide.eclipse.beans.core.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.springframework.ide.eclipse.beans.core.BeansCorePlugin;
import org.springframework.ide.eclipse.beans.core.BeansCoreUtils;
import org.springframework.ide.eclipse.beans.core.model.BeansModelChangedEvent;
import org.springframework.ide.eclipse.beans.core.model.IBean;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfig;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfigSet;
import org.springframework.ide.eclipse.beans.core.model.IBeansModelElement;
import org.springframework.ide.eclipse.beans.core.model.IBeansModel;
import org.springframework.ide.eclipse.beans.core.model.IBeansModelChangedListener;
import org.springframework.ide.eclipse.beans.core.model.IBeansProject;
import org.springframework.ide.eclipse.beans.core.resources.IBeansResourceChangeEvents;
import org.springframework.ide.eclipse.beans.core.resources.BeansResourceChangeListener;

/**
 * The <code>IBeansModel</code> manages instances of <code>IBeansProject</code>s.
 * <code>IBeansModelChangedListener</code>s register with the <code>IBeansModel</code>,
 * and receive <code>BeansModelChangedEvent</code>s for all changes.
 * <p>
 * The single instance of <code>IBeansModel</code> is available from
 * the static method <code>BeansCorePlugin.getModel()</code>.
 */
public class BeansModel extends BeansModelElement implements IBeansModel {

	public static final String DEBUG_OPTION = BeansCorePlugin.PLUGIN_ID +
																 "/model/debug";
	public static boolean DEBUG = BeansCorePlugin.isDebug(DEBUG_OPTION);

	private Map projects;
	private List listeners;
	private IResourceChangeListener listener;

	public BeansModel() {
		super(null, "" /*model has empty name*/);
		this.projects = new HashMap();
		this.listeners = new ArrayList();
		this.listener = new BeansResourceChangeListener(
											  new ResourceChangeEventHandler());
	}

	public int getElementType() {
		return MODEL;
	}

	public IResource getElementResource() {
		return null;
	}

	public void startup() {
		if (DEBUG) {
			System.out.println("Beans Model startup");
		}
		initialize();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(listener,
										 BeansResourceChangeListener.LISTENER_FLAGS);
	}

	public void shutdown() {
		if (DEBUG) {
			System.out.println("Beans Model shutdown");
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(listener);
		projects.clear();
	}

	public boolean hasProject(IProject project) {
		if (project != null  && project.isAccessible() &&
												projects.containsKey(project)) {
			return true;
		}
		return false;
	}

	public IBeansProject getProject(IProject project) {
		if (hasProject(project)) {
			return (IBeansProject) projects.get(project);
		}
		return null;
	}

	public IBeansProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
																		  name);
		return getProject(project);
	}

	/**
	 * Returns a collection of all <code>IBeansProject</code>s defined in this
	 * model.
	 * @see org.springframework.ide.eclipse.beans.core.model.IBeansProject
	 */
	public Collection getProjects() {
		return projects.values();
	}

	public boolean hasConfig(IFile configFile) {
		if (configFile != null) {
			IBeansProject project = getProject(configFile.getProject());
			if (project != null) {
				return project.hasConfig(configFile);
			}
		}
		return false;
	}

	public IBeansConfig getConfig(IFile configFile) {
		if (configFile != null) {
			IBeansProject project = getProject(configFile.getProject());
			if (project != null) {
				return project.getConfig(configFile);
			}
		}
		return null;
	}

	public void addChangeListener(IBeansModelChangedListener listener) {
		listeners.add(listener);
	}

	public void removeChangeListener(IBeansModelChangedListener listener) {
		listeners.remove(listener);
	}

	public String toString() {
		StringBuffer text = new StringBuffer("Beans model:\n");
		Iterator projs = projects.values().iterator();
		while (projs.hasNext()) {
			IBeansProject project = (IBeansProject) projs.next();
			text.append(" Configs of project '");
			text.append(project.getElementName());
			text.append("':\n");
			Iterator configs = project.getConfigs().iterator();
			while (configs.hasNext()) {
				IBeansConfig config = (IBeansConfig) configs.next();
				text.append("  ");
				text.append(config);
				text.append('\n');
				Iterator beans = config.getBeans().iterator();
				while (beans.hasNext()) {
					IBean bean = (IBean) beans.next();
					text.append("   ");
					text.append(bean);
					text.append('\n');
				}
			}
			text.append(" Config sets of project '");
			text.append(project.getElementName());
			text.append("':\n");
			Iterator configSets = project.getConfigSets().iterator();
			while (configSets.hasNext()) {
				IBeansConfigSet configSet = (IBeansConfigSet) configSets.next();
				text.append("  ");
				text.append(configSet);
				text.append('\n');
			}
		}
		return text.toString();
	}

	private void initialize() {
		if (DEBUG) {
			System.out.println("Initializing model - loading all projects");
		}
		this.projects.clear();
		List projects = getBeansProjects();
		if (!projects.isEmpty()) {
			Iterator iter = projects.iterator();
			while (iter.hasNext()) {
				IProject project = (IProject) iter.next();
				this.projects.put(project, new BeansProject(project));
			}
		}
	}

	/**
	 * Returns a list of all <code>IProject</code>s with the Beans project nature.
	 * @return list of all <code>IProject</code>s with the Beans project nature
	 */
	private static List getBeansProjects() {
		List springProjects = new ArrayList();
		IProject[] projects =
						 ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (BeansCoreUtils.isBeansProject(project)) {
				springProjects.add(project);
			}
		}
		return springProjects;
	}

	private void notifyListeners(IBeansModelElement element, int type) {
		BeansModelChangedEvent event = new BeansModelChangedEvent(element,
																  type);
		Iterator iter = listeners.iterator();
		while (iter.hasNext()) {
			IBeansModelChangedListener listener =
									   (IBeansModelChangedListener) iter.next();
			listener.elementChanged(event);
		}
	}

	/**
	 * Internal resource change event handler.
	 */
	private class ResourceChangeEventHandler implements IBeansResourceChangeEvents {

		public boolean isSpringProject(IProject project) {
			return BeansCoreUtils.isBeansProject(project);
		}

		public void springNatureAdded(IProject project) {
			if (DEBUG) {
				System.out.println("Spring beans nature added to project '" +
								   project.getName() + "'");
			}
			BeansProject proj = new BeansProject(project);
			projects.put(project, proj);
			notifyListeners(proj, BeansModelChangedEvent.ADDED);
		}

		public void springNatureRemoved(IProject project) {
			if (DEBUG) {
				System.out.println("Spring beans nature removed from " +
								   "project '" + project.getName() + "'");
			}
			IBeansProject proj = (IBeansProject) projects.remove(project);
			notifyListeners(proj, BeansModelChangedEvent.REMOVED);
		}

		public void projectAdded(IProject project) {
			if (DEBUG) {
				System.out.println("Project '" + project.getName() + "' added");
			}
			BeansProject proj = new BeansProject(project);
			projects.put(project, proj);
			notifyListeners(proj, BeansModelChangedEvent.ADDED);
		}
	
		public void projectOpened(IProject project) {
			if (DEBUG) {
				System.out.println("Project '" + project.getName() +
								   "' opened");
			}
			BeansProject proj = new BeansProject(project);
			projects.put(project, proj);
			notifyListeners(proj, BeansModelChangedEvent.ADDED);
		}
	
		public void projectClosed(IProject project) {
			if (DEBUG) {
				System.out.println("Project '" + project.getName() +
								   "' closed");
			}
			IBeansProject proj = (IBeansProject) projects.remove(project);
			notifyListeners(proj, BeansModelChangedEvent.REMOVED);
		}
	
		public void projectDeleted(IProject project) {
			if (DEBUG) {
				System.out.println("Project '" + project.getName() +
								   "' deleted");
			}
			IBeansProject proj = (IBeansProject) projects.remove(project);
			notifyListeners(proj, BeansModelChangedEvent.REMOVED);
		}
	
		public void projectDescriptionChanged(IFile file) {
			if (DEBUG) {
				System.out.println("Project description '" +
								   file.getFullPath() +
								   "' changed");
			}
			BeansProject project = (BeansProject)
												projects.get(file.getProject());
			project.reset();
			notifyListeners(project, BeansModelChangedEvent.CHANGED);
		}

		public void configAdded(IFile file) {
			if (DEBUG) {
				System.out.println("Config '" + file.getFullPath() +
								   "' added");
			}
			BeansProject project = (BeansProject)
												projects.get(file.getProject());
			project.addConfig(file);
			IBeansConfig config = project.getConfig(file);
			notifyListeners(config, BeansModelChangedEvent.ADDED);
		}
	
		public void configChanged(IFile file) {
			if (DEBUG) {
				System.out.println("Config '" + file.getFullPath() +
								   "' changed");
			}
			IBeansProject project = (IBeansProject)
												projects.get(file.getProject());
			BeansConfig config = (BeansConfig) project.getConfig(file);
			config.reset();
			notifyListeners(config, BeansModelChangedEvent.CHANGED);
		}
	
		public void configRemoved(IFile file) {
			if (DEBUG) {
				System.out.println("Config '" + file.getFullPath() +
								   "' removed");
			}
			BeansProject project = (BeansProject)
												projects.get(file.getProject());
			BeansConfig config = (BeansConfig) project.getConfig(file);
			project.removeConfig(file);
			notifyListeners(config, BeansModelChangedEvent.REMOVED);
		}
	}

}
