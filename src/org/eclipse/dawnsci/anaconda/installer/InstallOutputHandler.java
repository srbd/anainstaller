package org.eclipse.dawnsci.anaconda.installer;

/**
 * Callbacks that the installer can make to the process launching the installer.
 */
public interface InstallOutputHandler {

	/**
	 * Called once at the beginning of the install process
	 */
	void starting();
	
	/**
	 * Called with any output that should be displayed in a console like way
	 * @param string text to display
	 */
	void output(String string);

}