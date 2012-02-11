/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu.newconnectionprovider;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import neembuu.vfs.connection.NewConnectionParams;
import neembuu.vfs.connection.NewConnectionProvider;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.JDDownloadSession;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class JD_HTTP_Download_Manager implements NewConnectionProvider {
	final JDDownloadSession jdds;

	private static final Logger LOGGER = Logger
			.getLogger(JD_HTTP_Download_Manager.class.getName());

	private final ConcurrentLinkedQueue<JD_HTTP_Connection> connection_list = new ConcurrentLinkedQueue<JD_HTTP_Connection>();

	public JD_HTTP_Download_Manager(JDDownloadSession jdds) {
		this.jdds = jdds;
	}

	private void connectionsRequested() {
		String message = "++++++++++ConnectionsRequested+++++++++\n";
		for (JD_HTTP_Connection e : connection_list) {
			message = message + e.getConnectionParams().toString() + "\n";
		}
		message = message + "---------ConnectionsRequested--------\n";

		LOGGER.info(message);
	}

	// @Override
	public final String getSourceDescription() {
		return "JD_DownloadManager{" + jdds.getDownloadLink().getDownloadURL()
				+ "}";
	}

	// @Override
	public final void provideNewConnection(
			final NewConnectionParams connectionParams) {
		class StartNewJDBrowserConnectionThread extends Thread {

			StartNewJDBrowserConnectionThread() {
				// always name thread, otherwise it can be extremely difficult
				// to debug
				super("StartNew[JD_Download_Manager]{" + connectionParams + "}");
			}

			@Override
			public final void run() {
				try {
					JD_HTTP_Connection c = new JD_HTTP_Connection(
							JD_HTTP_Download_Manager.this, connectionParams);
					connection_list.add(c);
					connectionsRequested();
					c.connectAndSupply();
				} catch (Exception e) {
					Log.L.log(Level.INFO, "Problem in new connection ", e);
				}
			}
		}

		new StartNewJDBrowserConnectionThread().start();
	}

	// @Override
	public final long estimateCreationTime(long offset) {
		return averageConnectionCreationTime();
	}

	private long averageConnectionCreationTime() {
		int i = 0;
		long totalTime = 0;
		for (JD_HTTP_Connection connection : connection_list) {
			if (connection.succeededInCreation()) {
				totalTime += connection.timeTakenForCreation();
				i++;
			}
		}
		if (i == 0) {
			return 0;// creation time is unknown
		}
		return ((totalTime) / i);
	}

	long[] totalProgress = { 0 };
}
