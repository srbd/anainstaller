package org.eclipse.dawnsci.anaconda.installer;

public interface IInstallCompleteHandler {

	/**
	 * Set the path where Anaconda was installed to as a result of running the wizard
	 * 
	 * @param installPath
	 *            root of Anaconda installation
	 */
	public void setInstallPath(String installPath);

}
