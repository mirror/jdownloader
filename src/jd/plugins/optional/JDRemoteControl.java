//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

//
//    Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität zu erhöhen.
//

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jd.JDInit;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.controlling.DistributeData;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.PluginOptional;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;


import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class JDRemoteControl extends PluginOptional implements ControlListener {
    public static int getAddonInterfaceVersion(){
        return 0;
    }
    private String version = "0.5.0.3";
    private DecimalFormat f = new DecimalFormat("#0.00"); 
    
    private Server server;
    @SuppressWarnings("unused")
    private AbstractHandler serverHandler;

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginID() {
        return version;
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.RemoteControl.name", "RemoteControl");
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean initAddon()  {
        if (JDUtilities.getJavaVersion() >= 1.5) {
          
                logger.info("RemoteControl OK");
                initRemoteControl();
               JDUtilities.getController().addControlListener(this);
            return true;
        } else {
            logger.severe("Error initializing RemoteControl");
            return false;
        }
    }

    public void initRemoteControl() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), "PORT", JDLocale.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10025);
    
        try {
            server = new Server(this.getProperties().getIntegerProperty("PORT", 10025));
            server.setHandler(new Serverhandler());
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }



    public void actionPerformed(ActionEvent e) {
        if (server == null) return;
        try {
            if (this.server.isStarted() || this.server.isStarting()) {
                server.stop();
                JDUtilities.getGUI().showMessageDialog(this.getPluginName() + " stopped");
            } else {
                server = new Server(this.getProperties().getIntegerProperty("PORT", 10025));
                server.setHandler(new Serverhandler());
                server.start();
                JDUtilities.getGUI().showMessageDialog(this.getPluginName() + " started on port "+this.getProperties().getIntegerProperty("PORT", 10025) + "\n http://127.0.0.1:" +this.getProperties().getIntegerProperty("PORT", 10025) + "/help for Developer Information.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem("Toggle Start/Stop",0).setActionListener(this));
        return menu;
    }



    class Serverhandler extends AbstractHandler {

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            SimpleGUI simplegui = SimpleGUI.CURRENTGUI;

            //---------------------------------------
            //Help
            //---------------------------------------
            
            if (request.getRequestURI().equals("/help")) {
                //alt:
                //final String[] command2={"/get/ip","/get/config","/get/version","/get/downloads/current","/get/downloads/max","/get/downloads/finished","/get/speed","/get/isreconnect","/get/isclipboard","/action/start","/action/pause","/action/stop","/action/toggle","/action/update","/action/reconnect","/action/restart","/action/shutdown","/action/set/download/limit/%X%","/action/set/download/max/%X%","/action/open/add","/action/open/config","/action/open/log","/action/open/containerdialog","/action/add/links/grabber(0|1)/start(0|1)/%X%","/action/add/container/%X%","/action/set/clipboard/(true|false)","/action/set/reconnectenabled/(true|false)"};
                //final String[] desc2={"Get IP","Get Config","Get Version","Get Current Downloads","Get max. sim. Downloads","Get finished Downloads","Get current Speed","Get If Reconnect","Get If ClipBoard enabled","Start DLs","Pause DLs","Stop DLs","Toggle DLs","Do Webupdate","Do Reconnect","Restart JD","Shutdown JD","Set Downloadspeedlimit %X%","Set max sim. Downloads %X%","Open Add Links Dialog","Open Config Dialog","Open Log Dialog","Open Container Dialog","Add Links %X% to Grabber<br />Options:<br />grabber(0|1): Show/Hide LinkGrabber<br />start(0|1): Start DLs after insert<br />Sample<br />/action/add/links/grabber0/start1/http://tinyurl.com/6o73eq http://tinyurl.com/4khvhn<br />Don't forget Space between Links!","Add Container %X%","Set ClipBoard Control","Set Reconnect Enabled"};

                Vector<String> commandvec= new Vector<String>();
                Vector<String> infovector= new Vector<String>();
                
            commandvec.add(" ");
            infovector.add("<br /><b>Get Values:</b><br />&nbsp;");

                commandvec.add("/get/speed");
                infovector.add("Get current Speed");
                
                commandvec.add("/get/ip");
                infovector.add("Get IP");
            
                commandvec.add("/get/config");
                infovector.add("Get Config");
                
                commandvec.add("/get/happyhour/gui(0|1)/");
                infovector.add("Get Happy Hour Status (1: Grafisch aufbereitet /0: Besser auslesbar)");
                
                commandvec.add("/get/version");
                infovector.add("Get Version");
                
                commandvec.add("/get/rcversion");
                infovector.add("Get RemoteControl Version");
                
                commandvec.add("/get/speedlimit");
                infovector.add("Get current Speedlimit");

                commandvec.add("/get/isreconnect");
                infovector.add("Get If Reconnect");
                
                commandvec.add("/get/isclipboard");
                infovector.add("Get whether clipboard is enabled");

                commandvec.add("/get/downloads/currentcount");
                infovector.add("Get amount of current downloads");
                commandvec.add("/get/downloads/currentlist");
                infovector.add("Get Current Downloads List");
                
                commandvec.add("/get/downloads/allcount");
                infovector.add("Get amount of downloads in list");
                commandvec.add("/get/downloads/alllist");
                infovector.add("Get list of downloads in list");
                
                commandvec.add("/get/downloads/finishedcount");
                infovector.add("Get amount of finished Downloads");
                commandvec.add("/get/downloads/finishedlist");      
                infovector.add("Get finished Downloads List");
                
            commandvec.add(" ");
            infovector.add("<br /><b>Actions:</b><br />&nbsp;");
                
                commandvec.add("/action/start");
                infovector.add("Start DLs");
                
                commandvec.add("/action/pause");
                infovector.add("Pause DLs");
                
                commandvec.add("/action/stop");
                infovector.add("Stop DLs");
                
                commandvec.add("/action/toggle");
                infovector.add("Toggle DLs");
                
                commandvec.add("/action/update/force(0|1)/");
                infovector.add("Do Webupdate <br />" +
                		       "force1 activates auto-restart if update is possible");
                
                commandvec.add("/action/reconnect");
                infovector.add("Do Reconnect");
                
                commandvec.add("/action/restart");
                infovector.add("Restart JD");
                
                commandvec.add("/action/shutdown");
                infovector.add("Shutdown JD");
                
                commandvec.add("/action/set/download/limit/%X%");
                infovector.add("Set Downloadspeedlimit %X%");
                
                commandvec.add("/action/set/download/max/%X%");
                infovector.add("Set max sim. Downloads %X%");
                
                commandvec.add("/action/add/links/grabber(0|1)/start(0|1)/%X%");
                infovector.add("Add Links %X% to Grabber<br />" +
                		"Options:<br />" +
                		"grabber(0|1): Show/Hide LinkGrabber<br />" +
                		"start(0|1): Start DLs after insert<br />" +
                		"Sample:<br />" +
                		"/action/add/links/grabber0/start1/http://tinyurl.com/6o73eq http://tinyurl.com/4khvhn<br />" +
                		"Don't forget Space between Links!");
                
                commandvec.add("/action/add/container/%X%");
                infovector.add("Add Container %X%<br />" +
                		"Sample:<br />" +
                		"/action/add/container/C:\\container.dlc");
                
                commandvec.add("/action/save/container/%X%");
                infovector.add("Save DLC-Container with all Links to %X%<br /> " +
                		"Sample see /action/add/container/%X%");
                
                commandvec.add("/action/set/beta/(true|false)");
                infovector.add("Set use beta enabled or not (doesn't restart itself!)");
                
                commandvec.add("/action/set/clipboard/(true|false)");
                infovector.add("Set ClipBoard Control enabled or not");
                
                commandvec.add("/action/set/reconnectenabled/(true|false)");
                infovector.add("Set Reconnect enabled or not");
                
                commandvec.add("/action/set/premiumenabled/(true|false)");
                infovector.add("Set Use Premium enabled or not");
                
                commandvec.add("/action/open/add");
                infovector.add("Open Add Links Dialog");
                
                commandvec.add("/action/open/config");
                infovector.add("Open Config Dialog");
                
                commandvec.add("/action/open/log");
                infovector.add("Open Log Dialog");
                
                commandvec.add("/action/open/containerdialog");
                infovector.add("Open OpenContainer Dialog");
                


                response.getWriter().println(
                        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"" +
                        "\"http://www.w3.org/TR/html4/strict.dtd\">"+
                        "<html>" +
                        "<head>" +
                        "<title>JDRemoteControl Help</title>" +
                        "<style type=\"text/css\">" +
                        "a {" +
                        "    font-size: 14px;" +
                        "    text-decoration: none;" +
                        "    background: none;" +
                        "    color: #599ad6;" +
                        "}" +
                        "a:hover {" +
                        "    text-decoration: underline;" +
                        "    color:#333333;" +
                        "}" +
                        "body {" +
                        "    color: #333333;" +
                        "    background:#f0f0f0;" +
                        "    font-family: Verdana, Arial, Helvetica, sans-serif;" +
                        "    font-size: 14px;" +
                        "    vertical-align: top;" +
                        "  }" +
                        "</style>" +
                        "</head>" +
                        "<body><p><br />" +
                		"<b>JDRemoteControl " + version + "<br />" +
                		"<br />" +
                		"Usage:</b><br />&nbsp;<br />" +
                		"1)Replace %X% with your value<br />" +
                		"Sample: /action/save/container/C:\\backup.dlc <br />" +
                		"2)Replace (true|false) with true or false<br />" +
                		"Sample: /action/set/clipboard/true" +
                		"<br />" +
                		"<table border=\"0\" cellspacing=\"5\">");
                for (int commandcount = 0; commandcount < commandvec.size(); commandcount++)
                {
                    
                        response.getWriter().println( "<tr><td valign=\"top\"><a href=\"http://127.0.0.1:" + getProperties().getIntegerProperty("PORT", 10025) + 
                        commandvec.get(commandcount) + "\">" + commandvec.get(commandcount) + "</a></td><td valign=\"top\">" +
                        infovector.get(commandcount) +
                        "</td></tr>");
                }
                response.getWriter().println("</table>" +
                		"<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;" +
                		"</p>" +
                		"</body>" +
                		"</html>");
            }
            
            //---------------------------------------
            //Get
            //---------------------------------------
           
            //Get IP
            else if (request.getRequestURI().equals("/get/ip")) {
                response.getWriter().println(JDUtilities.getIPAddress());
            }
            
            //Get HappyHour
            else if (request.getRequestURI().matches("[\\s\\S]*?/get/happyhour/gui[01]{1}/[\\s\\S]*")) {
                    
                Integer happyguiint = Integer.parseInt((new Regex(request.getRequestURI(),
                    "[\\s\\S]*?/get/happyhour/gui([01]{1})/[\\s\\S]*")
                    .getFirstMatch()));
                Boolean happygui = false;
                if(happyguiint == 1){happygui = true;};
                    
                RequestInfo ri = HTTP.getRequest(new URL("http://jdownloader.org/hh.php?txt=1"));
                
                int sec = 300 - JDUtilities.filterInt(JDUtilities.splitByNewline(ri.getHtmlCode())[3]);
    
                int lastStart = JDUtilities.filterInt(JDUtilities.splitByNewline(ri.getHtmlCode())[4]);
                int lastEnd = JDUtilities.filterInt(JDUtilities.splitByNewline(ri.getHtmlCode())[5]);
                Date lastStartDate = new Date(lastStart * 1000L);
                lastStartDate.setTime(lastStart * 1000L);
    
                Date lastEndDate = new Date(lastEnd * 1000L);
                lastEndDate.setTime(lastEnd * 1000L);
                if (ri.containsHTML("Hour")) {//HappyHour aktiv
                    int activ = JDUtilities.filterInt(JDUtilities.splitByNewline(ri.getHtmlCode())[1]);
                    Date d = new Date(activ * 1000L);
                    d.setTime(activ * 1000L);
    
                    SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    String html="";
                    if(!happygui)
                    {
                        html = String.format("<- active=\"1\" active_since=\"%s\" last_check_ago=\"%s\" last_happyhour_from=\"%s\" last_happyhour_until=\"%s\" ->", df.format(d), JDUtilities.formatSeconds(sec), df.format(lastStartDate), df.format(lastEndDate));    
                    }
                    else
                    {
                        html = String.format(JDLocale.L("plugins.hoster.rapidshare.com.hhactive.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><body><div><p style='text-align:center'><img src='http://jdownloader.org/img/hh.jpg' /><br>Aktiv seit %s<br>Zuletzt überprüft vor %s<br>Letzte Happy Hour von %s bis %s</p></div></body>"), df.format(d), JDUtilities.formatSeconds(sec), df.format(lastStartDate), df.format(lastEndDate));    
                    }
    
                    
                    response.getWriter().println(html);
                    //JDUtilities.getGUI().showHTMLDialog(JDLocale.L("plugins.hoster.rapidshare.com.happyHours", "Happy Hour Check"), html);
                } else {
                    int activ = JDUtilities.filterInt(JDUtilities.splitByNewline(ri.getHtmlCode())[1]);
                    Date d = new Date(activ * 1000L);
                    d.setTime(activ * 1000L);
    
                    SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    
                    String html="";
                    if(!happygui)
                    {
                        html = String.format("<- active=\"0\" last_check_ago=\"%s\" last_happyhour_from=\"%s\" last_happyhour_until=\"%s\" ->", JDUtilities.formatSeconds(sec), df.format(lastStartDate), df.format(lastEndDate));
                    }
                    else
                    {
                        html = String.format(JDLocale.L("plugins.hoster.rapidshare.com.hhinactive.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><body><div><p style='text-align:center'><img src='http://jdownloader.org/img/nhh.jpg' /><br>Die letzte Happy Hour Phase endete am %s<br>Zuletzt überprüft vor %s<br>Letzte Happy Hour von %s bis %s</p></div></body>"), df.format(d), JDUtilities.formatSeconds(sec), df.format(lastStartDate), df.format(lastEndDate));
                    }
                    
                    response.getWriter().println(html);
                    //JDUtilities.getGUI().showHTMLDialog(JDLocale.L("plugins.hoster.rapidshare.com.happyHours", "Happy Hour Check"), html);
                }
            }
            
            //Get Config
            else if (request.getRequestURI().equals("/get/config")) {
                Property config = JDUtilities.getConfiguration();
                response.getWriter().println("<pre>");
                if (request.getParameterMap().containsKey("sub")) {
                
                    config = JDUtilities.getSubConfig(((String[]) request.getParameterMap().get("sub"))[0].toUpperCase());

                }
                Entry<String, Object> next;
                for (Iterator<Entry<String, Object>> it = config.getProperties().entrySet().iterator(); it.hasNext();) {
                    next = it.next();
                    response.getWriter().println(next.getKey() + " = " + next.getValue() + "\r\n");
                } response.getWriter().println("</pre>");
            }
            
            //Get Version
            else if (request.getRequestURI().equals("/get/version")) {
                response.getWriter().println(JDUtilities.JD_VERSION + JDUtilities.getRevision());
            }
            
            //Get RemoteControlVersion
            else if (request.getRequestURI().equals("/get/rcversion")) {
                response.getWriter().println(version);
            }
            
            //Get SpeedLimit
            else if (request.getRequestURI().equals("/get/speedlimit")) {
                response.getWriter().println(JDUtilities.getSubConfig("DOWNLOAD").getProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, "0"));
            }
            
            //Get Current DLs COUNT
            else if (request.getRequestURI().equals("/get/downloads/currentcount")) {
                int counter = 0;                
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;
 
                for (Package_ID=0; Package_ID<JDUtilities.getController().getPackages().size();Package_ID++){
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);
                    for (Download_ID=0; Download_ID <filePackage.getDownloadLinks().size();Download_ID++){

                        dLink = filePackage.getDownloadLinks().get(Download_ID);
                        if(dLink.isInProgress())
                        {
                            counter++;
                        }
                    }
                } 
                response.getWriter().println(counter);
            } 
            
            //Get Current DLs
            else if (request.getRequestURI().equals("/get/downloads/currentlist")) {
                String output = "";
                
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;
 
                for (Package_ID=0; Package_ID<JDUtilities.getController().getPackages().size();Package_ID++){
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    /* Paket Infos */
                    output = output + "<package";//Open Package
                    output = output + " package_name=\"" + filePackage.getName() + "\"";
                    output = output + " package_id=\"" + Package_ID.toString() + "\"";                    
                    output = output + " package_percent=\"" + f.format(filePackage.getPercent()) +  "\"";
                    output = output + " package_linksinprogress=\"" + filePackage.getLinksInProgress() + "\"";
                    output = output + " package_linkstotal=\"" + filePackage.size() + "\"";
                    output = output + " package_ETA=\"" + JDUtilities.formatSeconds(filePackage.getETA()) + "\"";
                    output = output + " package_speed=\"" + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s\"";
                    output = output + " package_loaded=\"" + JDUtilities.formatKbReadable(filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " package_size=\"" + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize()) + "\"";
                    output = output + " package_todo=\"" + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize() - filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " >";//Close Package
                   
                    for (Download_ID=0; Download_ID <filePackage.getDownloadLinks().size();Download_ID++){
                        

                        dLink = filePackage.getDownloadLinks().get(Download_ID);
                        if(dLink.isInProgress())
                        {
                            /* Download Infos */
                            output = output + "<file";//Open File
                            output = output + " file_name=\"" + dLink.getName() + "\"";
                            output = output + " file_id=\"" + Download_ID.toString() + "\"";
                            output = output + " file_package=\"" + Package_ID.toString() + "\"";
                            output = output + " file_percent=\"" + f.format(dLink.getDownloadCurrent() * 100.0 / Math.max(1,dLink.getDownloadMax())) + "\"";
                            output = output + " file_hoster=\"" + dLink.getHost() + "\"";
                            output = output + " file_status=\"" + dLink.getStatusText().toString() + "\"";
                            output = output + " file_speed=\"" + dLink.getDownloadSpeed() + "\"";
                            output = output + " > ";//Close File
                        }
                    }

                    output = output + "</package> ";// Close Package
                }
                
                response.getWriter().println(output);
            }
                
                /*String output = "";
                for (int i = 0; i < JDUtilities.getController().getDownloadLinks().size(); i++) {
                    if (JDUtilities.getController().getDownloadLinks().get(i).getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS)
                    {
                        output = output + 
                                "<- name=\""+JDUtilities.getController().getDownloadLinks().get(i).getName()+
                                "\" hoster=\"" + JDUtilities.getController().getDownloadLinks().get(i).getHost() + 
                                "\" speed=\"" + JDUtilities.getController().getDownloadLinks().get(i).getDownloadSpeed() + 
                                "\" ->";             
                    }             
                } 
                response.getWriter().println(output);
            }*/
            
            //Get DLList COUNT
            else if (request.getRequestURI().equals("/get/downloads/allcount")) {
                int counter = 0;                
                FilePackage filePackage;
                Integer Package_ID;
                Integer Download_ID;
 
                for (Package_ID=0; Package_ID<JDUtilities.getController().getPackages().size();Package_ID++){
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    for (Download_ID=0; Download_ID <filePackage.getDownloadLinks().size();Download_ID++){
                            counter++;
                    }
                } 
                response.getWriter().println(counter);
            } 
            
            //Get DLList
            
            else if (request.getRequestURI().equals("/get/downloads/alllist")) {
                String output = "";
                
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;
 
                for (Package_ID=0; Package_ID<JDUtilities.getController().getPackages().size();Package_ID++){
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    /* Paket Infos */
                    output = output + "<package";//Open Package
                    output = output + " package_name=\"" + filePackage.getName() + "\"";
                    output = output + " package_id=\"" + Package_ID.toString() + "\"";                    
                    output = output + " package_percent=\"" + f.format(filePackage.getPercent()) +  "\"";
                    output = output + " package_linksinprogress=\"" + filePackage.getLinksInProgress() + "\"";
                    output = output + " package_linkstotal=\"" + filePackage.size() + "\"";
                    output = output + " package_ETA=\"" + JDUtilities.formatSeconds(filePackage.getETA()) + "\"";
                    output = output + " package_speed=\"" + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s\"";
                    output = output + " package_loaded=\"" + JDUtilities.formatKbReadable(filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " package_size=\"" + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize()) + "\"";
                    output = output + " package_todo=\"" + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize() - filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " >";//Close Package
                   
                    for (Download_ID=0; Download_ID <filePackage.getDownloadLinks().size();Download_ID++){
                        
                        dLink = filePackage.getDownloadLinks().get(Download_ID);
                        /* Download Infos */
                        output = output + "<file";//Open File
                        output = output + " file_name=\"" + dLink.getName() + "\"";
                        output = output + " file_id=\"" + Download_ID.toString() + "\"";
                        output = output + " file_package=\"" + Package_ID.toString() + "\"";
                        output = output + " file_percent=\"" + f.format(dLink.getDownloadCurrent() * 100.0 / Math.max(1,dLink.getDownloadMax())) + "\"";
                        output = output + " file_hoster=\"" + dLink.getHost() + "\"";
                        output = output + " file_status=\"" + dLink.getStatusText().toString() + "\"";
                        output = output + " file_speed=\"" + dLink.getDownloadSpeed() + "\"";
                        output = output + " > ";//Close File
                    }

                    output = output + "</package> ";// Close Package
                }
                
                response.getWriter().println(output);
            }
            
            //Get finished DLs COUNT
            else if (request.getRequestURI().equals("/get/downloads/finishedcount")) {
                int counter = 0;                
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;
 
                for (Package_ID=0; Package_ID<JDUtilities.getController().getPackages().size();Package_ID++){
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    for (Download_ID=0; Download_ID <filePackage.getDownloadLinks().size();Download_ID++){

                        dLink = filePackage.getDownloadLinks().get(Download_ID);
                        if(dLink.getStatus() == DownloadLink.STATUS_DONE)
                        {
                            counter++;
                        }
                    }
                } 
                response.getWriter().println(counter);
            }   
            
            //Get finished DLs
            else if (request.getRequestURI().equals("/get/downloads/finishedlist")) {
                String output = "";
                
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;
 
                for (Package_ID=0; Package_ID<JDUtilities.getController().getPackages().size();Package_ID++){
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    /* Paket Infos */
                    output = output + "<package";//Open Package
                    output = output + " package_name=\"" + filePackage.getName() + "\"";
                    output = output + " package_id=\"" + Package_ID.toString() + "\"";                    
                    output = output + " package_percent=\"" + f.format(filePackage.getPercent()) +  "\"";
                    output = output + " package_linksinprogress=\"" + filePackage.getLinksInProgress() + "\"";
                    output = output + " package_linkstotal=\"" + filePackage.size() + "\"";
                    output = output + " package_ETA=\"" + JDUtilities.formatSeconds(filePackage.getETA()) + "\"";
                    output = output + " package_speed=\"" + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s\"";
                    output = output + " package_loaded=\"" + JDUtilities.formatKbReadable(filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " package_size=\"" + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize()) + "\"";
                    output = output + " package_todo=\"" + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize() - filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " >";//Close Package
                   
                    for (Download_ID=0; Download_ID <filePackage.getDownloadLinks().size();Download_ID++){
                        

                        dLink = filePackage.getDownloadLinks().get(Download_ID);
                        if(dLink.getStatus() == DownloadLink.STATUS_DONE)
                        {
                            /* Download Infos */
                            output = output + "<file";//Open File
                            output = output + " file_name=\"" + dLink.getName() + "\"";
                            output = output + " file_id=\"" + Download_ID.toString() + "\"";
                            output = output + " file_package=\"" + Package_ID.toString() + "\"";
                            output = output + " file_percent=\"" + f.format(dLink.getDownloadCurrent() * 100.0 / Math.max(1,dLink.getDownloadMax())) + "\"";
                            output = output + " file_hoster=\"" + dLink.getHost() + "\"";
                            output = output + " file_status=\"" + dLink.getStatusText().toString() + "\"";
                            output = output + " file_speed=\"" + dLink.getDownloadSpeed() + "\"";
                            output = output + " > ";//Close File
                        }
                    }

                    output = output + "</package> ";// Close Package
                }
                
                response.getWriter().println(output);
            }
            
            //Get current Speed
            else if (request.getRequestURI().equals("/get/speed")) {
                response.getWriter().println(JDUtilities.getController().getSpeedMeter() / 1000);
            }
            
            //Get IsReconnect
            else if (request.getRequestURI().equals("/get/isreconnect")) {
                response.getWriter().println(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
            }
            //Get IsClipBoard
            else if (request.getRequestURI().equals("/get/isclipboard")) {
                response.getWriter().println(JDUtilities.getController().getClipboard().isEnabled());
            }
            
            //---------------------------------------
            //Control
            //---------------------------------------
            
            //Do Start Download
            else if (request.getRequestURI().equals("/action/start")) {
                JDUtilities.getController().startDownloads();
                response.getWriter().println("Downloads started");
            }
            
            //Do Pause Download
            else if (request.getRequestURI().equals("/action/pause")) {
                JDUtilities.getController().pauseDownloads(true);
                response.getWriter().println("Downloads paused");
            }
            
            //Do Stop Download
            else if (request.getRequestURI().equals("/action/stop")) {
                JDUtilities.getController().stopDownloads();
                response.getWriter().println("Downloads stopped");
            }
            
            //Do Toggle Download
            else if (request.getRequestURI().equals("/action/toggle")) {
                JDUtilities.getController().toggleStartStop();
                response.getWriter().println("Downloads toggled");
            }

            //Do Make Webupdate
            
            else if (request.getRequestURI().matches("[\\s\\S]*?/action/update/force[01]{1}/[\\s\\S]*")) {
                    
                Integer force = Integer.parseInt((new Regex(request.getRequestURI(),
                    "[\\s\\S]*?/action/update/force([01]{1})/[\\s\\S]*")
                    .getFirstMatch()));
                if(force == 1){
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
                };
            
                new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);
   
                response.getWriter().println("Do Webupdate...");
            }
            
            //Do Reconnect
            else if (request.getRequestURI().equals("/action/reconnect")) {
                response.getWriter().println("Do Reconnect...");
                
                boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                
                JDUtilities.getController().uiEvent(new UIEvent(JDUtilities.getGUI(), UIEvent.UI_INTERACT_RECONNECT));
                
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
                
                
            }
            
            //Do Restart JD
            else if (request.getRequestURI().equals("/action/restart")) {
                response.getWriter().println("Restarting...");
                
                class JDClose implements Runnable { /* zeitverzögertes beenden  - thx jiaz */
                    JDClose() {
                        new Thread(this).start();
                    }
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            
                            e.printStackTrace();
                        }
                        JDUtilities.restartJD();
                    }
                }
                @SuppressWarnings("unused")
                JDClose jdshutdown = new JDClose(); 
            }
            
            //Do Shutdown JD
            else if (request.getRequestURI().equals("/action/shutdown")) {
                
                response.getWriter().println("Shutting down...");
                
                class JDClose implements Runnable { /* zeitverzögertes beenden  - thx jiaz */
                    JDClose() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            
                            e.printStackTrace();
                        }
                        JDUtilities.getController().exit();
                    }
                }
                @SuppressWarnings("unused")
                JDClose jdshutdown = new JDClose();
            }
            
            //Set Downloadlimit
            else if (request.getRequestURI().matches("(?is).*/action/set/download/limit/[0-9]+.*")) {
                Integer newdllimit = Integer.parseInt(new Regex(request.getRequestURI(),
                        "[\\s\\S]*/action/set/download/limit/([0-9]+).*")
                        .getFirstMatch());
                logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString() );
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
                JDUtilities.getSubConfig("DOWNLOAD").save();
                response.getWriter().println("newlimit=" + newdllimit);
            }
            
            //Set max. sim. Downloads
            else if (request.getRequestURI().matches("(?is).*/action/set/download/max/[0-9]+.*")) {
                Integer newsimdl = Integer.parseInt(new Regex(request.getRequestURI(),
                        "[\\s\\S]*/action/set/download/max/([0-9]+).*")
                        .getFirstMatch());
                logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString() );
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
                JDUtilities.getSubConfig("DOWNLOAD").save();
                response.getWriter().println("newmax=" + newsimdl);
            }
            
            //OpenDialog Add-Links
            else if (request.getRequestURI().equals("/action/open/add")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.ITEMS_ADD, null));
                response.getWriter().println("Add-Links Dialog opened");
            }
            
            //OpenDialog Config
            else if (request.getRequestURI().equals("/action/open/config")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CONFIGURATION, null));
                response.getWriter().println("Config Dialog opened");
            }
            
            //OpenDialog Log
            else if (request.getRequestURI().equals("/action/open/log")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOG, null));
                response.getWriter().println("Log Dialog opened");
            }
            
            //OpenDialog Container
            else if (request.getRequestURI().equals("/action/open/containerdialog")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOAD_DLC, null));
                response.getWriter().println("Container Dialog opened");
            }
            
            //Add Link(s) 
            else if (request.getRequestURI().matches("(?is).*/action/add/links/grabber[01]{1}/start[01]{1}/[\\s\\S]+")) {
                String link = (new Regex(request.getRequestURI(),
                    "[\\s\\S]*?/action/add/links/grabber[01]{1}/start[01]{1}/([\\s\\S]+)")
                    .getFirstMatch());
                //response.getWriter().println(link);
                Integer showgrab = Integer.parseInt((new Regex(request.getRequestURI(),
                    "[\\s\\S]*?/action/add/links/grabber([01]{1})/start[01]{1}/[\\s\\S]+")
                    .getFirstMatch()));
                Boolean hidegrabber = false;
                if(showgrab == 0){hidegrabber = true;};
                //response.getWriter().println(hidegrabber.toString());
                Integer stdl = Integer.parseInt((new Regex(request.getRequestURI(),
                    "[\\s\\S]*?/action/add/links/grabber[01]{1}/start([01]{1})/[\\s\\S]+")
                    .getFirstMatch()));
                Boolean startdl = false;
                if(stdl == 1){startdl = true;};
                //response.getWriter().println(startdl.toString());
                link = JDUtilities.htmlDecode(link);
                //wegen leerzeichen etc, die ja in urls verändert werden...
                
                DistributeData distributeData = new DistributeData(link, hidegrabber, startdl);
                distributeData.addControlListener(JDUtilities.getController());
                distributeData.start();
                response.getWriter().println("Link(s) added. (" + link + ")");
            }
            
            //Open DLC Container
            else if (request.getRequestURI().matches("(?is).*/action/add/container/[\\s\\S]+")) {
                String dlcfilestr = (new Regex(request.getRequestURI(),
                "[\\s\\S]*/action/add/container/([\\s\\S]+)")
                .getFirstMatch());
                dlcfilestr = JDUtilities.htmlDecode(dlcfilestr);
                //wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().loadContainerFile(new File(dlcfilestr));
                response.getWriter().println("Container opened. (" + dlcfilestr + ")");
            }
            
            //Save Linklist as DLC Container
            else if (request.getRequestURI().matches("(?is).*/action/save/container/[\\s\\S]+")) {
                String dlcfilestr = (new Regex(request.getRequestURI(),
                "[\\s\\S]*/action/save/container/([\\s\\S]+)")
                .getFirstMatch());
                dlcfilestr = JDUtilities.htmlDecode(dlcfilestr);
                //wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().saveDLC(new File(dlcfilestr));
                response.getWriter().println("Container saved. (" + dlcfilestr + ")");
            }
            
            //Set ClipBoard
            else if (request.getRequestURI().matches("(?is).*/action/set/clipboard/.*")) {
                boolean newclip = Boolean.parseBoolean(new Regex(request.getRequestURI(),
                     "[\\s\\S]*/action/set/clipboard/(.*)")
                     .getFirstMatch());
                logger.fine("RemoteControl - Set ClipBoard: " + newclip );
                if((JDUtilities.getController().getClipboard().isEnabled()) ^ (newclip)) /*C++ User:^ is equuvalent to XOR*/
                {
                    simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CLIPBOARD, null));
                    response.getWriter().println("clip=" + newclip + " (CHANGED=true)");
                }
                else
                {
                    response.getWriter().println("clip=" + newclip + " (CHANGED=false)");
                } 
            }
            
            //Set Beta Enabled
            else if (request.getRequestURI().matches("(?is).*/action/set/beta/.*")) {
                boolean newbeta = Boolean.parseBoolean(new Regex(request.getRequestURI(),
                     "[\\s\\S]*/action/set/beta/(.*)")
                     .getFirstMatch());
                logger.fine("RemoteControl - Set Beta: " + newbeta );
                if((JDUtilities.getController().getClipboard().isEnabled()) ^ (newbeta)) /*C++ User:^ is equuvalent to XOR*/
                {
                    JDUtilities.getSubConfig("WEBUPDATE").setProperty("WEBUPDATE_BETA",newbeta);
                    JDUtilities.getSubConfig("WEBUPDATE").save();
                    response.getWriter().println("beta=" + newbeta + " (CHANGED=true)");
                }
                else
                {
                    response.getWriter().println("beta=" + newbeta + " (CHANGED=false)");
                } 
            }
            
            //Set ReconnectEnabled
            else if (request.getRequestURI().matches("(?is).*/action/set/reconnectenabled/.*")) {
                boolean newrc = Boolean.parseBoolean(new Regex(request.getRequestURI(),
                     "[\\s\\S]*/action/set/reconnectenabled/(.*)")
                     .getFirstMatch());
                logger.fine("RemoteControl - Set ReConnect: " + newrc );
                if((!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) ^ (newrc)) /*C++ User:^ is equivalent to XOR*/
                {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);

                    JDUtilities.saveConfig();
                    
                    response.getWriter().println("reconnect=" + newrc + " (CHANGED=true)");
                }
                else
                {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, true);

                    JDUtilities.saveConfig();
                    response.getWriter().println("reconnect=" + newrc + " (CHANGED=false)");
                } 
            }
            
            //Set use premium 
            else if (request.getRequestURI().matches("(?is).*/action/set/premiumenabled/.*")) {
                  boolean newuseprem = Boolean.parseBoolean(new Regex(request.getRequestURI(),
                          "[\\s\\S]*/action/set/premiumenabled/(.*)")
                          .getFirstMatch());
                  logger.fine("RemoteControl - Set Premium: " + newuseprem );
                  if((newuseprem) ^ (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem))) /*C++ User:^ is equivalent to XOR*/
                  {
                      JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
                      JDUtilities.saveConfig();
                      response.getWriter().println("newprem=" + newuseprem + " (CHANGED=true)");
                  }
                  else
                  {
                      response.getWriter().println("newprem=" + newuseprem + " (CHANGED=false)");
                  }                      
                
                
            }
            else
            {
                response.getWriter().println("JDRemoteControl - Malformed Request. use /help");
            }
            

            
            ((Request) request).setHandled(true);
        }
    }



    @Override
    public void onExit() {
        // TODO Auto-generated method stub
        
    };

}
