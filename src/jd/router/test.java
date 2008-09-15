package jd.router;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.cybergarage.xml.Node;
import org.cybergarage.xml.ParserException;
import org.cybergarage.xml.parser.kXML2Parser;


public class test extends ControlPoint implements SearchResponseListener{

    public void deviceSearchResponseReceived(SSDPPacket packet) {
        String url = packet.getLocation();
        System.out.println(url);
        try {
            URLConnection con = new URL(url).openConnection();
           Node in = new kXML2Parser().parse(con.getInputStream());
           System.out.println(in.getNode("device").getNode("deviceList").getNode("device").getNode("deviceList").getNode("device").getNode("serviceList").getNode(1));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.stop();
//        System.out.println("device search res : uuid = " + uuid + ", ST = " + st + ", location = " + url); 
    }
    public static void main(String[] args) {
         test t = new test();
         t.start();
    
         t.addSearchResponseListener(t);
    }
}
