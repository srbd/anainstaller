package org.eclipse.dawnsci.anaconda.wizards;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.dawnsci.anaconda.installer.InstallOutputHandler;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class DestinationPage extends WizardPage {

	private static final String SELECT_DESTINATION_DESCRIPTION = "Select destination directory for Anaconda";
	private Text installLocationText;
	private InstallOutputHandler outputHandler;
	private Text installOutputText;
	private Button closeWizardOnSuccess;
	private Button installBrowseButton;

	protected DestinationPage() {
		super("Install Destination");
		setTitle("Anaconda Installer");
		setDescription(SELECT_DESTINATION_DESCRIPTION);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createInstallLocationGroup(container);
		
		Label installOutputLabel = new Label(container, SWT.NONE);
		installOutputLabel.setText("Installation Output:");
		
		installOutputText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		GC gc = new GC(installOutputText);
		gc.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		FontMetrics fontMetrics2 = gc.getFontMetrics();
		gd.widthHint = fontMetrics2.getAverageCharWidth() * 80;
		gc.dispose();
		gd.heightHint = 300;
		installOutputText.setLayoutData(gd);
		installOutputText.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		
		closeWizardOnSuccess = new Button(container, SWT.CHECK);
		closeWizardOnSuccess.setSelection(false);
		closeWizardOnSuccess.setText("Close wizard automatically on successful installation.");

		setControl(container);
		
		dialogChanged();
	}

	private void createInstallLocationGroup(Composite parent) {
		Composite installLocGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		installLocGroup.setLayout(layout);
		installLocGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		Label groupLabel = new Label(installLocGroup, SWT.NONE);
		groupLabel.setText("Install to:");

		installLocationText = new Text(installLocGroup, SWT.BORDER);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint = 250;
		installLocationText.setLayoutData(data);
		installLocationText.setText(getDefaultInstallLocation());

		installLocationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		installBrowseButton = new Button(installLocGroup, SWT.PUSH);
		installBrowseButton.setText("Browse...");
		installBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleInstallBrowseButtonPressed();
			}
		});
		installBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setButtonLayoutData(installBrowseButton);
	}

	private void dialogChanged() {
		String installLoc = installLocationText.getText();
		if (installLoc.length() == 0) {
			setErrorMessage(SELECT_DESTINATION_DESCRIPTION);
			setPageComplete(false);
			return;
		} 

		if (!Path.ROOT.isValidPath(installLoc)) {
			setErrorMessage("Selected destination is not a valid path");
			setPageComplete(false);
			return;
		}
		
		Path installLocPath = new Path(installLoc);
		if (!installLocPath.isAbsolute()) {
			setErrorMessage("Selected destination is not a absolute path");
			setPageComplete(false);
			return;
		}
		
		File installLocFile = installLocPath.toFile();
		if (installLocFile.exists() && !installLocFile.isDirectory()) {
			setErrorMessage("Selected destination already exists, but is not a directory");
			setPageComplete(false);
			return;
		}
	
		if (installLocFile.exists() && installLocFile.isDirectory()) {
			setErrorMessage(null);
			setMessage("Selected destination already exists. Installing into this directory might overwrite existing files.", WARNING);
			setPageComplete(true);
			return;
		} 
	
		setMessage(null);
		setErrorMessage(null);
		setPageComplete(true);
	}

	private void handleInstallBrowseButtonPressed() {

		String currentInstallLocation = this.installLocationText.getText();
		DirectoryDialog dialog = new DirectoryDialog(installLocationText.getShell(), SWT.SAVE | SWT.SHEET);
		dialog.setText("Install to directory");
		dialog.setMessage("Select a directory to install to");
		dialog.setFilterPath(currentInstallLocation);

		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			installLocationText.setText(selectedDirectory);
		}
	}

	public String getInstallPath() {
		return installLocationText.getText();
	}
	
	public boolean getCloseWizardOnSuccess() {
		return closeWizardOnSuccess.getSelection();
	}

	public InstallOutputHandler getOutputHandler() {
		if (outputHandler == null) {
			outputHandler = new InstallOutputHandler() {
				
				@Override
				public void output(final String string) {
					Display.getDefault().asyncExec(new Runnable() {
						
						@Override
						public void run() {
							installOutputText.append(string);
							installOutputText.setSelection(installOutputText.getCharCount());
						}
					});
				}

				@Override
				public void starting() {
					Display.getDefault().asyncExec(new Runnable() {
						
						@Override
						public void run() {
							installOutputText.setText("");
						}
					});
					
				}
			};
		}
		return outputHandler;
	}

	public void setReadOnly() {
		installLocationText.setEditable(false);
		closeWizardOnSuccess.setEnabled(false);
		installBrowseButton.setEnabled(false);
		setErrorMessage(null);
		setMessage("Installation Complete. Please press Finish again to close the wizard.");
	}


	protected String getDefaultInstallLocation() {
		if (PlatformUtils.isWindowsPlatform()) {
			// default installation path on Windows
			return "C:\\Python27";
		}
		// install to home directory on non-Windows
		try {
			String home = System.getenv("HOME");
			return home + File.separatorChar + "anacondaWizardInstall";
		} catch (Exception e) {
			// can't get home directory, default to empty directory
			return "";
		}
	}


}

