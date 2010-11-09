package jd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import jd.http.Browser;
import jd.http.URLConnectionAdapter.METHOD;
import jd.nutils.encoding.Encoding;

public class Tester {

    public static void main(String[] args) throws Throwable {
        Browser br = new Browser();
        br.setDebug(true);
        System.out.println(Encoding.htmlDecode("14300921004cb82f694cbed%0D%0A"));

        int i = 1;
    }

    private static String runCommand(final String serviceType, final String controlUrl, final String command) throws IOException {
        String data = "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:" + command + " xmlns:u='" + serviceType + "' /> </s:Body> </s:Envelope>";
        // this works for fritz box.
        // old code did NOT work:

        // data =
        // "<?xml version=\"1.0\"?>\n<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n <s:Body>\n<m:"
        // + command + " xmlns:m=\"" + serviceType + "\"></m:" + command + ">\n"
        // + " </s:Body>\n" + "</s:Envelope>";

        final URL url = new URL(controlUrl);
        final URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        conn.addRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        conn.addRequestProperty("SOAPAction", serviceType + "#" + command);
        // conn.addRequestProperty("SOAPAction", serviceType + "#" + command +
        // "\"");
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String xmlstr = "";
            String nextln;
            while ((nextln = rd.readLine()) != null) {
                xmlstr += nextln.trim();
            }
            return xmlstr;

        } finally {
            if (wr != null) {
                wr.close();
            }
            if (rd != null) {
                rd.close();
            }
        }

    }

}