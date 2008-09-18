package jd.router;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;


public class test extends ControlPoint implements SearchResponseListener{

    public void deviceSearchResponseReceived(SSDPPacket packet) {
//        String url = packet.getLocation();
//        System.out.println(url);
//            URLConnection con = new URL(url).openConnection();
//            System.out.println(url);
            System.out.println(packet.toString());
//           Node in = new kXML2Parser().parse(con.getInputStream());
//           System.out.println(in.getNode("device").getNode("deviceList").getNode("device").getNode("deviceList").getNode("device").getNode("serviceList").getNode(1));

       
//        System.out.println("device search res : uuid = " + uuid + ", ST = " + st + ", location = " + url); 
    }
    public static void main(String[] args) {
         test t = new test();
         t.start();
    
         t.addSearchResponseListener(t);
         try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
         t.stop();
         System.exit(0);
    }
}
