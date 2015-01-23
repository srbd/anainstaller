package org.eclipse.dawnsci.anaconda.installer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IInstaller {

	/**
	 * Abstraction for the method that actually performs the final installation of Anaconda to the given installation
	 * path, updating the monitor as it goes.
	 * 
	 * @param installPath
	 *            Directory to install to
	 * @param monitor
	 *            Progress monitor, should be non-<code>null</code>
	 * @param feedbackHandler
	 *            will be called with any output on stderr/stdout
	 * @param completeHandler
	 *            will be called when installation has successfully completed
	 * @throws CoreException
	 *             if the install did not complete fully as expected
	 */
	void runInstall(String installPath, IProgressMonitor monitor, InstallOutputHandler feedbackHandler,
			IInstallCompleteHandler completeHandler) throws CoreException;

	/**
	 * Run the installer in manual mode.
	 * 
	 * This launches the installer but does not wait for it to complete. Only available on Windows because the installer
	 * is GUI based.
	 */
	public void runManualInstall() throws CoreException;

}