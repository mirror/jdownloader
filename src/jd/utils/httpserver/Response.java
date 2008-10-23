package jd.utils.httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

public class Response {
	private StringBuilder data = new StringBuilder();
	
	private HashMap<String, String> headers = new HashMap<String, String>();
	
	private String returnStatus = "200 OK";
	
	private String returnType = "text/html";
	
	public final static String OK = "200 OK";
	
	public final static String ERROR = "404 ERROR";
	
	public Response() {
		
	}
	
	public void addHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public void setReturnStatus(String returnStatus) {
		this.returnStatus = returnStatus;
	}
	
	public void addContent(Object content) {
        data.append(content.toString());
    }
	
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
	
	
	
	public void writeToStream(OutputStream out) throws IOException {
		StringBuilder help = new StringBuilder();
		help.append("HTTP/1.1 " + returnStatus + "\r\n");
		help.append("Connection: close\r\n");
		help.append("Server: jDownloader HTTP Server\r\n");
		help.append("Content-Type: " + returnType + "\r\n");
		help.append("Content-Length: " + data.toString().getBytes("iso-8859-1").length + "\r\n");
		
		Iterator<String> it = headers.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			help.append(key + ": " + headers.get(it) + "\r\n");
		}
		
		help.append("\r\n");
		
		out.write(help.toString().getBytes("iso-8859-1"));
		out.write(data.toString().getBytes("iso-8859-1"));
	}
}
