package jd.controlling.reconnect.plugins.upnp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.plugins.upnp.translate.T;

import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;

public class UPNPReconnectInvoker extends ReconnectInvoker {

    private String serviceType;

    public UPNPReconnectInvoker(UPNPRouterPlugin upnpRouterPlugin, String serviceType2, String controlURL2) {
        super(upnpRouterPlugin);
        this.serviceType = serviceType2;
        this.controlURL = controlURL2;
    }

    public static String runCommand(final String serviceType, final String controlUrl, final String command) throws IOException {
        final String data = "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:" + command + " xmlns:u='" + serviceType + "' /> </s:Body> </s:Envelope>";
        // this works for fritz box.
        // old code did NOT work:

        /*
         * 
         * final String data = "<?xml version=\"1.0\"?>\n" +
         * "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
         * + " <s:Body>\n  <m:" + command + " xmlns:m=\"" + serviceType +
         * "\"></m:" + command + ">\n </s:Body>\n</s:Envelope>"; try { final URL
         * url = new URL(controlUrl); final URLConnection conn =
         * url.openConnection(); conn.setDoOutput(true);
         * conn.addRequestProperty("Content-Type",
         * "text/xml; charset=\"utf-8\""); conn.addRequestProperty("SOAPAction",
         * serviceType + "#" + command + "\"");
         */
        final URL url = new URL(controlUrl);
        HTTPConnection con = new HTTPConnectionImpl(url);
        con.setRequestMethod(RequestMethod.POST);
        con.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        con.setRequestProperty("SOAPAction", serviceType + "#" + command);
        byte datas[] = data.getBytes("UTF-8");
        con.setRequestProperty("Content-Length", datas.length + "");
        BufferedReader rd = null;
        try {
            con.getOutputStream().write(datas);
            con.getOutputStream().flush();
            rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String xmlstr = "";
            String nextln;
            while ((nextln = rd.readLine()) != null) {
                xmlstr += nextln.trim();
            }
            return xmlstr;
        } finally {
            try {
                rd.close();
            } catch (final Throwable e) {
            }
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }

    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setControlURL(String controlURL) {
        this.controlURL = controlURL;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getControlURL() {
        return controlURL;
    }

    private String controlURL;

    public String getName() {
        return T._.UPNPReconnectInvoker_getName_();
    }

    @Override
    public void run() throws ReconnectException, InterruptedException {
        try {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            runCommand(serviceType, controlURL, "ForceTermination");
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Thread.sleep(2000);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            runCommand(serviceType, controlURL, "RequestConnection");
        } catch (InterruptedException e) {
            throw e;
        } catch (final Throwable e) {
            throw new ReconnectException(e);
        }
    }

    @Override
    protected ReconnectResult createReconnectResult() {
        return new UPNPReconnectResult();
    }

    @Override
    protected void testRun() throws ReconnectException, InterruptedException {
        run();
    }

}
