package jd.captcha;



import java.io.File;

import jd.router.RouterData;

/**
 * JAC Tester

 * 
 * @author coalado
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTest main = new JACTest();
        main.go();
    }
    private void go(){
      

       // JAntiCaptcha jac= new JAntiCaptcha("rapidshare.com");
//
        //jac.showPreparedCaptcha(new File("captcha\\methods\\rapidshare.com\\captchas\\captcha_rapidshare.com_code8BXS.jpg"));
        RouterData routerdata = new RouterData();
        routerdata.setConnectionDisconnect("POST /upnp/control/WANIPConn1 HTTP/1.1\r\nHOST: blah:49000\r\nSOAPACTION: \"urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination\"\r\nCONTENT-TYPE: text/xml ;\r\ncharset=\"utf-8\\r\nContent-Length: 293\r\n\r\n<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"\r\nxmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n   <s:Body>\r\n      <u:ForceTermination xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\" />\r\n   </s:Body>\r\n</s:Envelope>");
        routerdata.setIpAddressPre("<br> IP-Adresse ");
        routerdata.setIpAddressPost("</td>");
        routerdata.setIpAddressSite("fritz.box");
      UTILITIES.getLogger().info(  routerdata.getIPAdress(null));
        JAntiCaptcha jac= new JAntiCaptcha(null,"secured.in");
     //sharegullicom47210807182105.gif
       jac.showPreparedCaptcha(new File("..\\..\\methods\\secured.in\\captchas\\captcha_secured.in_code2uyd.jpg"));
       //jac.removeBadLetters();
      //jac.addLetterMap();
      //jac.saveMTHFile();
    
   
    }
}