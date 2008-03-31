package jd.plugins.webinterface;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.Vector;

import jd.utils.JDUtilities;

public class JDSimpleStatusPage extends Thread {
	private Socket Current_Socket;
	
	 public JDSimpleStatusPage(Socket Client_Socket) {
	       this.Current_Socket = Client_Socket;	        
	    }
	 
	 private static void sendHeader(BufferedOutputStream out, int code, String contentType, long contentLength, long lastModified) throws IOException {
	        out.write(("HTTP/1.0 " + code + " OK\r\n" + 
	                   "Date: " + new Date().toString() + "\r\n" +
	                   "Server: JibbleWebServer/1.0\r\n" +
	                   "Content-Type: " + contentType + "\r\n" +              
	                  "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" +
	                   ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") +
	                   "Last-modified: " + new Date(lastModified).toString() + "\r\n" +
	                   "\r\n").getBytes());
	    }
	 
	 public void run() {
	        InputStream reader = null;
	        try {
	        	Current_Socket.setSoTimeout(30000);
	            BufferedReader in = new BufferedReader(new InputStreamReader(Current_Socket.getInputStream()));
	            BufferedOutputStream out = new BufferedOutputStream(Current_Socket.getOutputStream());
	            Vector<String> requestHeaders= new Vector<String>();
	            String line;
	            while((line=in.readLine())!=null&&line.length()>3){JDUtilities.getLogger().info(line);requestHeaders.add(line);}
	            
	                  
	            sendHeader(out, 200, "text/html", -1, System.currentTimeMillis());
	                
	            out.write(("<html><meta http-equiv='refresh' content='1'/><head><title>test</title></head><body><p>"+JDUtilities.getController().getSpeedMeter()+"<br/>"+JDUtilities.getController().getDownloadLinks().get(0).getStatusText()+"<br/><hr/>"+requestHeaders+"</p></body></html>\n").getBytes());
	            out.flush();
	            out.close();
	        }
	        catch (IOException e) {
	            if (reader != null) {
	                try {
	                    reader.close();
	                }
	                catch (Exception anye) {
	                    // Do nothing.
	                }
	            }
	        }
	    }
}
