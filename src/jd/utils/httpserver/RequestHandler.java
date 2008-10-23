package jd.utils.httpserver;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class RequestHandler extends Thread {
	private Socket socket;
	private Handler handler;
	
	public RequestHandler(Socket socket, Handler handler) {
		this.socket = socket;
		this.handler = handler;
	}
	
	public void run() {
		try {
			BufferedInputStream reader = new BufferedInputStream(socket.getInputStream());
			String line = "";
			
			Request req = new Request();
			
			while ((line = readline(reader)) != null && line.trim().length() > 0) {
                String key = null;
                String value = null;
                if(line.startsWith("GET ") || line.startsWith("POST ")) {
                	String[] help = line.split(" ");
                	req.setRequestType(help[0]);
                	req.setHttpType(help[2]);
                	
                	if(help[1].indexOf("?") > 0) {
                		req.setRequestUrl(help[1].substring(0, help[1].indexOf("?")));
                		parseParameter(req, help[1].substring(help[1].indexOf("?") + 1));
                	}
                	else
                		req.setRequestUrl(help[1]);
                }
                else if(line.indexOf(": ") > 0) {
                    key = line.substring(0, line.indexOf(": ")).toLowerCase();
                    value = line.substring(line.indexOf(": ") + 2);
                }
                req.addHeader(key, value);
            }
			
			Response res = new Response();
			handler.handle(req, res);
			
			OutputStream out = socket.getOutputStream();
			res.writeToStream(out);
			out.close();
			
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void parseParameter(Request req, String parameter) {
		String[] help = parameter.split("\\&");
		
		for(String entry : help) {
			entry = entry.trim();
			int index = entry.indexOf("=");
			
			if (index > 0)
                req.addParameter(entry.substring(0, index), entry.substring(index + 1));
			else
				req.addParameter(entry, "");
		}
	}
	
	private String readline(BufferedInputStream reader) {
        int max_buf = 1024;
        byte[] buffer = new byte[max_buf];
        int index = 0;
        int byteread = 0;
        try {

            while ((byteread = reader.read()) != -1) {
                if (byteread == 10 || byteread == 13) {
                    reader.mark(0);
                    if ((byteread = reader.read()) != -1) {
                        if (byteread == 13 || byteread == 10) {
                            break;
                        } else {
                            reader.reset();
                            break;
                        }
                    }
                }
                if (index > max_buf) { return null; }
                buffer[index] = (byte) byteread;
                index++;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return new String(buffer).substring(0, index);
    }
}