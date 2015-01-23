
package org.eclipse.dawnsci.anaconda.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.dawnsci.anaconda.Activator;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.utils.PlatformUtils;

public final class Installer implements IInstaller {
	/**
	 * For a successful installation, how many characters come on stdout. Nothing goes wrong if too low or too high,
	 * just makes progress bar more accurate (and allocations slightly more efficient)
	 */
	private static final int APPROX_NUM_OF_CHARS_ON_STDOUT = 5500;

	@Override
	public void runManualInstall() throws CoreException {
		IPath installer = getInstallerLocation();
		try {
			SimpleRunner.createProcess(new String[] { "cmd.exe", "/c", installer.toOSString() }, null, null);
		} catch (IOException e) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Anaconda Installer failed to launch.",
					e);
			throw new CoreException(status);
		}
	}

	@Override
	public void runInstall(String installPath, final IProgressMonitor monitor,
			final InstallOutputHandler feedbackHandler, final IInstallCompleteHandler completeHandler)
			throws CoreException {
		monitor.beginTask("Installing", APPROX_NUM_OF_CHARS_ON_STDOUT);
		feedbackHandler.starting();

		Process process = launchProcess(installPath, feedbackHandler);

		// Process StdErr
		ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream(), false);

		// Process StdOut
		final FastStringBuffer stdoutcontents = new FastStringBuffer(APPROX_NUM_OF_CHARS_ON_STDOUT);
		Thread std = createStdoutThread(monitor, feedbackHandler, process, stdoutcontents);
		std.start();

		// Process StdIn
		Thread stdin = createStdinThread(process, installPath);
		stdin.start();

		waitUntilProcessDone(monitor, process, std, stdin, err);

		String errcontents = err.getContents();
		if (errcontents.length() > 0) {
			feedbackHandler.output("\nError output:\n");
			feedbackHandler.output(errcontents);
		}

		if (PlatformUtils.isWindowsPlatform()
				|| (process.exitValue() == 0 )) {
			String exe = installPath;
			if (PlatformUtils.isWindowsPlatform()) {
				if (!exe.endsWith("\\")) {
					exe = exe + "\\";
				}
				exe = exe + "python.exe";
			} else {
				if (!exe.endsWith("/")) {
					exe = exe + "/";
				}
				exe = exe + "bin/python";
			}
			if (new File(exe).exists()) {
				completeHandler.setInstallPath(exe);
			} else {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"Installation failed. Expected to find python here: '" + exe + "' but it was missing.");
				throw new CoreException(status);
			}
		} else if (monitor.isCanceled()) {
			Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID,
					"Installation canceled. You may need to manually clean up partial install. Please review output.");
			throw new CoreException(status);
		} else {
			String string = stdoutcontents.toString();
			int errorIndex = string.indexOf("ERROR: ");
			if (errorIndex < 0) {
				string = "Unexpected/unknown error";
			} else {
				string = string.substring("ERROR: ".length() + errorIndex);
			}
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, string);
			throw new CoreException(status);
		}

		monitor.done();

	}

	private void waitUntilProcessDone(final IProgressMonitor monitor, final Process process, Thread std, Thread stdin,
			ThreadStreamReader err) {
		Thread thread = new Thread("Wait For Anaconda Installer To Finish") {
			@Override
			public void run() {
				boolean interrupted = true;
				while (interrupted) {
					if (monitor.isCanceled()) {
						process.destroy();
					}

					interrupted = false;
					try {
						process.waitFor(); // wait until the process completion.
					} catch (InterruptedException e1) {
						interrupted = true;
					}
				}
			}
		};
		thread.start();

		while (thread.isAlive()) {
			if (monitor.isCanceled()) {
				thread.interrupt();
			}
			try {
				thread.join(100);
			} catch (InterruptedException e) {
				// ignore and try again
			}
		}

		// wait for all process readers/writers to complete
		try {
			std.join();
			stdin.join();
			err.join();
		} catch (InterruptedException e) {
			// ignored, treat like they completed
		}

	}

	private Thread createStdoutThread(final IProgressMonitor monitor, final InstallOutputHandler handler,
			Process process, final FastStringBuffer stdoutcontents) {
		Thread std;
		final InputStream is = process.getInputStream();
		std = new Thread("Anaconda Installer Input Reader") {
			@Override
			public void run() {
				try {
					InputStreamReader in = new InputStreamReader(is);
					int c;

					while ((c = in.read()) != -1) {
						handler.output("" + (char) c);
						stdoutcontents.append((char) c);
						monitor.worked(1);
					}
				} catch (Exception e) {
					// that's ok, process has completed, thread can close
				}
			}
		};
		return std;
	}

	private Thread createStdinThread(final Process process, final String installPath) {
		Thread stdin;
		final OutputStream os = process.getOutputStream();
		stdin = new Thread("Anaconda Installer Output Writer") {
			@Override
			public void run() {
				Path installLocPath = new Path(installPath);
				File installLocFile = installLocPath.toFile();
				if (installLocFile.exists() && installLocFile.isDirectory()) {
					PrintStream printStream = new PrintStream(os);
					printStream.println("yes");
					printStream.close();
				} else {
					// Finished with stdin
					try {
						os.close();
					} catch (IOException e) {
						// Ignore close error
					}
				}
			}
		};

		return stdin;
	}

	private Process launchProcess(String installPath, InstallOutputHandler handler) throws CoreException {
		IPath installer = getInstallerLocation();
		
		handler.output("Installer location: ");

		final String[] cmdarray;
		if (PlatformUtils.isWindowsPlatform()) {
			cmdarray = new String[] { "cmd.exe", "/c", installer.toOSString(), "TARGETDIR=" + installPath, "/qr", "/norestart" };
		} else {
			
			// eg installPath -b -p && installPath/bin/conda install --yes anaconda
			// TODO: get this to work rather than chaining two bash commands; simplerunner doesn't seem to respect these - two processes? 
			String condaPath = installPath + "/bin/conda";
			cmdarray = new String[] { installer.toOSString(), "-b", "-p", installPath, "&&", condaPath, "install", "--yes", "anaconda" };
		}
		

		handler.output("Running: ");
		for (int i = 0; i < cmdarray.length; i++) {
			handler.output(cmdarray[i]);
			handler.output(" ");
		}
		handler.output("\n");

		try {
			Process process = SimpleRunner.createProcess(cmdarray, null, null);
			return process;
		} catch (IOException e) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to launch installer", e);
			throw new CoreException(status);
		}
	}

	/**
	 * Get the installer location for the current platform.
	 * 
	 * @return path to the installer, never returns <code>null</code>
	 * @throws CoreException
	 *             if installer could not be found
	 */
	public static IPath getInstallerLocation() throws CoreException {
		IPath installer = null;
		try {
			URL bundleURL = Activator.getBundleURL(new Path("installer/anaconda_installer.txt"));
			if (bundleURL != null) {
				InputStream openStream = bundleURL.openStream();
				BufferedReader bufferedInputStream = new BufferedReader(new InputStreamReader(openStream));
				String filePath = bufferedInputStream.readLine();
				bufferedInputStream.close();

				URL installerUrl = Activator.getBundleURL(new Path("installer/" + filePath));
				if (installerUrl != null) {
					installer = new Path(FileLocator.toFileURL(installerUrl).getFile());
				}
			}
		} catch (IOException e) {
			Status status = new Status(
					IStatus.ERROR,
					Activator.PLUGIN_ID,
					"Anaconda Installer not available locally. Please visit [].",
					e);
			throw new CoreException(status);
		}

		if (installer == null) {
			Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID,
					"Anaconda Installer not available. Please visit [].");
			throw new CoreException(status);
		}

		return installer;
	}

}