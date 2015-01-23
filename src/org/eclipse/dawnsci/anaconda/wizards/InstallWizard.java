package org.eclipse.dawnsci.anaconda.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dawnsci.anaconda.Activator;
import org.eclipse.dawnsci.anaconda.InterpreterProvider;
import org.eclipse.dawnsci.anaconda.installer.InstallOutputHandler;
import org.eclipse.dawnsci.anaconda.installer.Installer;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;

public class InstallWizard extends Wizard {

	private LicenseAgreementPage licenseAgreementPage;
	private DestinationPage destinationPage;

	private boolean sucessfulRun;
	private final InterpreterProvider interpreterProvider;

	public static IWizard createInstallWizard(InterpreterProvider interpreterProvider) {

		// The normal wizard doesn't work if there may be a Windows based install on the machine already. In this case,
		// create a wizard that launches the installer in fully user interactive mode.
		/*if (PlatformUtils.isWindowsPlatform() && ManualMSIInstallWizard.getInstalledPythons().length > 0) {
			// >= 1 python found in the registry, fall back to doing it manually
			return new ManualMSIInstallWizard(interpreterProvider);
		}*/

		return new InstallWizard(interpreterProvider);
	}

	private InstallWizard(InterpreterProvider interpreterProvider) {
		super();
		this.interpreterProvider = interpreterProvider;
		setNeedsProgressMonitor(true);
		// No help beyond what is shown in the dialog
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		licenseAgreementPage = new LicenseAgreementPage();
		addPage(licenseAgreementPage);
		destinationPage = new DestinationPage();
		addPage(destinationPage);
	}

	@Override
	public boolean performFinish() {
		if (sucessfulRun) {
			return true;
		}

		final String installPath = destinationPage.getInstallPath();
		final InstallOutputHandler handler = destinationPage.getOutputHandler();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					new Installer().runInstall(installPath, monitor, handler, interpreterProvider);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			if (realException instanceof CoreException) {
				CoreException coreException = (CoreException) realException;
				ErrorDialog.openError(getShell(), "Error when running Install",
						"An unexpected error occurred when running the install", coreException.getStatus());
			} else {
				ErrorDialog.openError(getShell(), "Error when running Install",
						"An unexpected error occurred when running the install", new Status(IStatus.ERROR,
								Activator.PLUGIN_ID, "Unexpected error", realException));
			}
			return false;
		}

		if (destinationPage.getCloseWizardOnSuccess()) {
			return true;
		}
		licenseAgreementPage.setReadOnly();
		destinationPage.setReadOnly();
		sucessfulRun = true;
		return false;
	}

}

