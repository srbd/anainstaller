package org.eclipse.dawnsci.anaconda;

import org.eclipse.dawnsci.anaconda.installer.IInstallCompleteHandler;
import org.eclipse.dawnsci.anaconda.wizards.InstallWizard;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.ui.pythonpathconf.IInterpreterProvider;

/**
 * A provider that can install anaconda
 */
public class InterpreterProvider implements IInterpreterProvider, IInstallCompleteHandler {

	private String installPath = null;
	
	@Override
	public String getExecutableOrJar() {
		if (isInstalled()) {
			return installPath;
		}
		return getName() + " - select to install";
	}

	@Override
	public String getName() {
		return "Anaconda";
	}

	@Override
	public boolean isInstalled() {
		//return false;
		return installPath != null;
	}

	@Override
	public void runInstall() {
		IWizard installWizard = InstallWizard.createInstallWizard(this);
		WizardDialog wizardDialog = new WizardDialog(Display.getDefault().getActiveShell(), installWizard);
		wizardDialog.open();
	}

	@Override
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}
}