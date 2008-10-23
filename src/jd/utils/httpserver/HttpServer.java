package jd.utils.httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread {
	private ServerSocket ssocket;
	private Socket csocket;
	private Handler handler;
	private boolean running = true;
	
	public HttpServer(int port, Handler handler) {
		try {
			ssocket = new ServerSocket(port);
			this.handler = handler;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void serverstop() {
		running = false;
		suspend();
	}

	public void run() {
		while(running) {
			try {
				csocket = ssocket.accept();
				
				new RequestHandler(csocket, handler).run();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isStarted() {
		return running;
	}
}