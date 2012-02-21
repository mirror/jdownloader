/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu;

import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jpfm.operations.readwrite.ReadRequest;
import neembuu.vfs.connection.NewConnectionParams;
import neembuu.vfs.file.TroubleHandler;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class NBTroubleHandler implements TroubleHandler {

	private final DownloadSession jdds;

	public NBTroubleHandler(DownloadSession jdds) {
		this.jdds = jdds;
	}

	// @Override
	public final void cannotCreateANewConnection(final NewConnectionParams ncp,
			final int numberOfRetries) {
		final String mess = "Cannot create a new connection, unmounting as we already retried "
				+ numberOfRetries + " times.";
		final String mess2 = ncp.toString();
		SwingUtilities.invokeLater(new Runnable() {
			// @Override
			public void run() {
				JOptionPane.showMessageDialog(null, mess + "\n" + mess2,
						"Unmount initiated on " + jdds, JOptionPane.ERROR);
			}
		});

		jdds.getDownloadInterface().logger
				.log(Level.SEVERE, mess + " " + mess2);

		try {
			jdds.getWatchAsYouDownloadSession().unMount();
		} catch (Exception a) {
			jdds.getDownloadInterface().logger.log(Level.SEVERE,
					"unmounting problem", a);
		}
	}

	// @Override
	public final void readRequestsPendingSinceALongTime(
			List<ReadRequest> pendingReadRequest, long atleastMillisec) {
		long maxMillisecWait = atleastMillisec;
		for (ReadRequest rr : pendingReadRequest) {
			if (!rr.isCompleted()) {
				long pendingSince = rr.getCreationTime()
						- System.currentTimeMillis();
				maxMillisecWait = Math.max(maxMillisecWait, pendingSince);
			}
		}

		final String mess = "Some read request pending since past "
				+ (maxMillisecWait / 60000d) + "minute(s)";
		final String mess2 = "Try watching the file after completely downloading it.\n"
				+ "\"Watch as you download\" is difficult on this file.\n"
				+ "Unmounting to prevent Not Responding state of the application used to open this file.";

		jdds.getDownloadInterface().logger.log(Level.SEVERE, mess);

		if (maxMillisecWait > 3 * 60 * 1000) { // if a request is pending since
												// last 3 mintues ... we better
												// quit
			SwingUtilities.invokeLater(new Runnable() {
				// @Override
				public void run() {
					JOptionPane.showMessageDialog(null, mess + "\n" + mess2,
							"Unmount initiated on " + jdds, JOptionPane.ERROR);
				}
			});

			jdds.getDownloadInterface().logger.log(Level.SEVERE, mess2);

			try {
				jdds.getWatchAsYouDownloadSession().unMount();
			} catch (Exception a) {
				jdds.getDownloadInterface().logger.log(Level.SEVERE,
						"unmounting problem", a);
			}
		}
	}

}
