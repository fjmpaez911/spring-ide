/*******************************************************************************
 *  Copyright (c) 2013 VMware, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.gettingstarted.dashboard;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.springframework.ide.eclipse.gettingstarted.GettingStartedActivator;
import org.springsource.ide.eclipse.dashboard.ui.AbstractDashboardPage;
import org.springsource.ide.eclipse.dashboard.ui.AbstractDashboardPart;

/**
 * A DashBoard page that displays the contents of a webpage.
 * 
 * @author Kris De Volder
 */
public class WebDashboardPage extends AbstractDashboardPage implements IExecutableExtension {

	public static final String DASHBOARD_SLAVE_BROWSER_ID = null;
	/**
	 * Helper method to open urls in a regular web browser. Should be used to open 
	 * urls from dashboard pages that would otherwise navigate the embedded browser
	 * away from the intended landing page that should always be shown in the 
	 * dashboard page.
	 */
	public static void openUrl(String url) {
		try {
			IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(DASHBOARD_SLAVE_BROWSER_ID);
			browser.openURL(new URL(url));
		} catch (Exception e) {
			GettingStartedActivator.log(e);
		}
	}

	public static class HelloPart extends AbstractDashboardPart {

		@Override
		public Control createPartContent(Composite parent) {
			FormToolkit toolkit = getToolkit();
			Label label = toolkit.createLabel(parent, "Hello World!");
			return label;
		}

	}

	private static int idCounter = 0;

	/**
	 * The URL that will be displayed in this Dashboard webpage.
	 */
	String url;

	public WebDashboardPage() {
		//It seems we are forced to pass an id and a title although looks like this
		// stuff isn't used. Morever we don't really know what it will be yet... that
		// info is passed in later by 'setInitializationData'
		super(generateId(), "Generic Web Page");
	}
	
	private static synchronized String generateId() {
		return WebDashboardPage.class.getName() + (idCounter++);
	}

	@Override
	public void setInitializationData(IConfigurationElement cfig,
			String propertyName, Object data) {
		super.setInitializationData(cfig, propertyName, data);
		if (data!=null) {
			if (data instanceof String) {
				this.url = (String) data;
			} else if (data instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, String> map = (Map<String, String>) data;
				this.url = map.get("url");
			}
		}
	}
	
	@Override
	protected void createFormContent(IManagedForm managedForm) {
//		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		
		//toolkit.decorateFormHeading(form.getForm()); TODO: what's this for? 

		//IPreferenceStore prefStore = IdeUiPlugin.getDefault().getPreferenceStore();
		
		FillLayout layout = new FillLayout();

		Composite body = form.getBody();
		body.setLayout(layout);
		
		Browser browser = new Browser(body, SWT.NONE);
		if (url!=null) {
			browser.setUrl(url);
		} else {
			browser.setText("<h1>URL not set</h1>" +
					"<p>Url should be provided via the setInitializationData method</p>"
			);
		}
		addBrowserHooks(browser);
//		searchBox.setFocus();
	}

	/**
	 * Subclasses may override this if they want to customize the browser (e.g. add listeners to
	 * handle certain urls specially.
	 */
	protected void addBrowserHooks(Browser browser) {
	}

	/**
	 * The url of the landing page this dashboard page will show when it is opened.
	 */
	public String getUrl() {
		return url;
	}
	
	@Override
	protected List<AbstractDashboardPart> contributeParts(Composite parent, String path) {
		List<AbstractDashboardPart> parts = new ArrayList<AbstractDashboardPart>();
		parts.add(new HelloPart());
		return parts;
	}

}
