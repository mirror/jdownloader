package jd.plugins.webinterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

public class JDSimpleStatusPage {
	
	private Logger logger = JDUtilities.getLogger();
	private JDSimpleWebserverResponse response=new JDSimpleWebserverResponse();
    private JDController controller= JDUtilities.getController();
	
	 public JDSimpleStatusPage(JDSimpleWebserverResponse response) {
         this.response=response;	              
	    }
	 
     public void status(){
         Vector<DownloadLink> links=controller.getDownloadLinks();
         
         response.addContent("<html><body>");
         response.addContent("<P ALIGN=CENTER STYLE=\"margin-bottom: 0in\"><B>JDownloader </B></P>");
         response.addContent("<P ALIGN=CENTER STYLE=\"margin-bottom: 0in\"><B>Simple Status Page</B></P>");
         response.addContent("<TABLE WIDTH=100% BORDER=1 BORDERCOLOR=\"#000000\" CELLPADDING=4 CELLSPACING=0><COL WIDTH=43*><COL WIDTH=43*><COL WIDTH=43*><COL WIDTH=64*><COL WIDTH=64*>");
         for(int i=0; i<links.size();i++){ 
             DownloadLink link= links.get(i);
             response.addContent("<TR VALIGN=TOP>");
             response.addContent("<TD WIDTH=25%><P ALIGN=CENTER>"+i+"</P></TD>");
             response.addContent("<TD WIDTH=25%><P ALIGN=CENTER>"+link.getFileOutput()+"</P></TD>");
             response.addContent("<TD WIDTH=25%><P ALIGN=CENTER>"+link.getHost()+"</P></TD>");
             response.addContent("<TD WIDTH=25%><P ALIGN=CENTER>"+link.getStatusText()+"</P></TD>");             
         };
         response.addContent("</TABLE></html></body>");         
         response.setOk();
     }
	 
	 
}
