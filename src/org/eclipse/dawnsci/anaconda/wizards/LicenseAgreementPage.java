package org.eclipse.dawnsci.anaconda.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Path;
import org.eclipse.dawnsci.anaconda.Activator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class LicenseAgreementPage extends WizardPage {

	/**
	 * Fall back text in case we fail to load license file.
	 */
	private static final String FAILED_TO_GET_LICENSE_LOCALLY_ALT_TEXT = "Failed to get License locally, please review http://docs.continuum.io/anaconda/eula.html";
	private static final String REVIEW_LICENSE_DESCRIPTION = "To continue the installation, you must review and approve the license term agreement.";

	private Button licenseApproved;

	protected LicenseAgreementPage() {
		super("License Agreement");
		setTitle("Anaconda Installer");
		setDescription(REVIEW_LICENSE_DESCRIPTION);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		Text licenseText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		gd.widthHint = 600;
		gd.heightHint = 400;
		licenseText.setLayoutData(gd);

		String licenseAsString = getLicenseAsString();
		licenseText.setText(licenseAsString);
		
		
		licenseApproved = new Button(container, SWT.CHECK);
		licenseApproved.setText("I accept the terms of the license agreement");
		licenseApproved.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});
		

		setPageComplete(false);
		setControl(container);
	}

	private void dialogChanged() {
		if (!licenseApproved.getSelection()) {
			setErrorMessage(REVIEW_LICENSE_DESCRIPTION);
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	private String getLicenseAsString() {
		URL licenseUrl = Activator.getBundleURL(new Path("installer/Anaconda_License.txt"));
		InputStream stream = null;
		String licenseAsString;
		if (licenseUrl == null) {
			licenseAsString = FAILED_TO_GET_LICENSE_LOCALLY_ALT_TEXT;
		} else {
			try {
				stream = licenseUrl.openStream();
				licenseAsString = IOUtils.toString(stream, "UTF-8");
			} catch (IOException e) {
				licenseAsString = FAILED_TO_GET_LICENSE_LOCALLY_ALT_TEXT;
			} finally {
				try {
					if (stream != null)
						stream.close();
				} catch (IOException e) {
					// Ignore exception on stream close
				}
			}
		}
		return licenseAsString;
	}

	public void setReadOnly() {
		licenseApproved.setEnabled(false);
		setErrorMessage(null);
		setMessage("Installation Complete. Please press Finish again to close the wizard.");
	}
	
}

