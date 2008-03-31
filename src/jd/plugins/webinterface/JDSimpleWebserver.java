package jd.plugins.webinterface;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import jd.config.SubConfiguration;
import jd.utils.JDUtilities;

public class JDSimpleWebserver extends Thread {

	private ServerSocket Server_Socket;

	private boolean Server_Running = true;

	private Logger logger = JDUtilities.getLogger();

	public int clientCounter = 0;

	public JDSimpleWebserver() throws IOException {

		SubConfiguration subConfig = JDUtilities.getSubConfig("WEBINTERFACE");
		Server_Socket = new ServerSocket(subConfig.getIntegerProperty(
				JDWebinterface.PROPERTY_PORT, 1024));
		logger.info("WebInterface start");
		start();
	}

	
	public void run() {
		while (Server_Running) {
			try {
				logger.info("WebInterface client");
				Socket Client_Socket = Server_Socket.accept();
				logger.info("WebInterface client ist da");
				

				JDSimpleStatusPage requestThread = new JDSimpleStatusPage(
						Client_Socket);
				requestThread.start();
			} catch (IOException e) {
				logger.info("WebInterface fehler");
			}
		}
	}

}
