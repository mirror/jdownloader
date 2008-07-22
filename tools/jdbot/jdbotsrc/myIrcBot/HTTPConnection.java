package myIrcBot;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
/**
 * GNU GPL Lizenz Den offiziellen englischen Originaltext finden Sie unter http://www.gnu.org/licenses/gpl.html.
 * 
 *
 */
public class HTTPConnection {

    public static final int HTTP_NOT_IMPLEMENTED = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
    private HttpURLConnection connection;
    private HashMap<String, List<String>> requestProperties=null;
 
    private String postData;

    public HTTPConnection(URLConnection openConnection) {
        this.connection=(HttpURLConnection)openConnection;
        requestProperties=new HashMap<String, List<String>>();
        
        Map<String, List<String>> tmp = connection.getRequestProperties();
        Iterator<Entry<String, List<String>>> set = tmp.entrySet().iterator();
        while(set.hasNext()){
            Entry<String, List<String>> next = set.next();
            requestProperties.put(next.getKey(), next.getValue());
        }
        
    }

    public void setReadTimeout(int timeout) {
        connection.setReadTimeout(timeout);
        
    }

    public void setConnectTimeout(int timeout) {
        connection.setConnectTimeout(timeout);
        
    }

    public void setInstanceFollowRedirects(boolean redirect) {
        connection.setInstanceFollowRedirects(redirect);
        
    }

    public void setRequestProperty(String key, String value) {
        LinkedList<String> l = new LinkedList<String>();

        l.add(value);
        requestProperties.put(key, l);
        connection.setRequestProperty(key,value);
       
    }
    
    public String getHeaderField(String string) {
        return connection.getHeaderField(string);
    }

    public Map<String, List<String>> getHeaderFields() {
        return connection.getHeaderFields();
    }

    public void setDoOutput(boolean b) {
        connection.setDoOutput(b);
        
    }

    public void connect() throws IOException {
        
        connection.connect();
        
    }

    public OutputStream getOutputStream() throws IOException {       
        return connection.getOutputStream();
    }

    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public URL getURL() {
       return connection.getURL();
    }

    public String getContentType() {
        return connection.getContentType();
    }

    public HttpURLConnection getHTTPURLConnection() {
        return connection;
      
    }

    public int getContentLength() {
        return connection.getContentLength();
    }

    public Map<String, List<String>> getRequestProperties() {
       return requestProperties;
     
    }

    public String getRequestProperty(String string) {
       return connection.getRequestProperty(string);
    }

    public void post(String parameter) throws IOException {
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        if (parameter != null) wr.write(parameter);
        this.postData=parameter;
        wr.flush();
        wr.close();
        
    }
    public void postGzip(String parameter) throws IOException {
        
 
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        if (parameter != null) wr.write(parameter);
        this.postData=parameter;
        wr.flush();
        wr.close();
        
    }
    public String getPostData() {
        return postData;
    }

    public void setRequestMethod(String string) throws ProtocolException {
        connection.setRequestMethod(string);
        
    }
    public String getRequestMethod(){
        return connection.getRequestMethod();
    }

}
