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

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
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

import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;
import jd.plugins.Regexp;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;


import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class JDRemoteControl extends PluginOptional implements ControlListener {

    private Server server;
    @SuppressWarnings("unused")
    private AbstractHandler serverHandler;

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginID() {
        return "0.3.0.0";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.RemoteControl.name", "RemoteControl");
    }

    @Override
    public String getVersion() {
        return "0.3.0.0";
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if (JDUtilities.getJavaVersion() >= 1.5) {
            if (enable) {
                logger.info("RemoteControl OK");
                initRemoteControl();
               JDUtilities.getController().addControlListener(this);
            }
        } else {
            logger.severe("Error initializing RemoteControl");
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

                Vector<String> command= new Vector<String>();
                Vector<String> desc= new Vector<String>();
                
                command.add("/get/ip");
                command.add("/get/config");
                command.add("/get/version");
                command.add("/get/downloads/currentcount");
                command.add("/get/downloads/currentlist");
                command.add("/get/downloads/allcount");
                command.add("/get/downloads/alllist");
                command.add("/get/downloads/finishedcount");
                command.add("/get/downloads/finishedlist");
                command.add("/get/speed");
                command.add("/get/isreconnect");
                command.add("/get/isclipboard");
                command.add("/action/start");
                command.add("/action/pause");
                command.add("/action/stop");
                command.add("/action/toggle");
                command.add("/action/update");
                command.add("/action/reconnect");
                command.add("/action/restart");
                command.add("/action/shutdown");
                command.add("/action/set/download/limit/%X%");
                command.add("/action/set/download/max/%X%");
                command.add("/action/open/add");
                command.add("/action/open/config");
                command.add("/action/open/log");
                command.add("/action/open/containerdialog");
                command.add("/action/add/links/grabber(0|1)/start(0|1)/%X%");
                command.add("/action/add/container/%X%");
                command.add("/action/save/container/%X%");
                command.add("/action/set/clipboard/(true|false)");
                command.add("/action/set/reconnectenabled/(true|false)");
                        
                desc.add("Get IP");
                desc.add("Get Config");
                desc.add("Get Version");
                desc.add("Get Current Downloads Count");
                desc.add("Get Current Downloads List");
                desc.add("Get max. sim. Downloads Count");
                desc.add("Get max. sim. Downloads List");
                desc.add("Get finished Downloads Count");
                desc.add("Get finished Downloads List");
                desc.add("Get current Speed");
                desc.add("Get If Reconnect");
                desc.add("Get If ClipBoard enabled");
                desc.add("Start DLs");
                desc.add("Pause DLs");
                desc.add("Stop DLs");
                desc.add("Toggle DLs (BETA - Does NOT refresh GUI!)");
                desc.add("Do Webupdate");
                desc.add("Do Reconnect");
                desc.add("Restart JD");
                desc.add("Shutdown JD");
                desc.add("Set Downloadspeedlimit %X%");
                desc.add("Set max sim. Downloads %X%");
                desc.add("Open Add Links Dialog");
                desc.add("Open Config Dialog");
                desc.add("Open Log Dialog");
                desc.add("Open Container Dialog");
                desc.add("Add Links %X% to Grabber<br />Options:<br />grabber(0|1): Show/Hide LinkGrabber<br />start(0|1): Start DLs after insert<br />Sample<br />/action/add/links/grabber0/start1/http://tinyurl.com/6o73eq http://tinyurl.com/4khvhn<br />Don't forget Space between Links!");
                desc.add("Add Container %X%");
                desc.add("Save DLC-Container with all Links to %X%");
                desc.add("Set ClipBoard Control");
                desc.add("Set Reconnect Enabled");

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
                		"JDRemoteControl 0.3<br />" +
                		"<br />" +
                		"Usage:<br />" +
                		"<br />" +
                		"<table border=\"0\" cellspacing=\"5\">");
                for (int commandcount = 0; commandcount < command.size(); commandcount++)
                {
                    
                        response.getWriter().println( "<tr><td valign=\"top\"><a href=\"http://127.0.0.1:10025" + 
                        command.get(commandcount) + "\">" + command.get(commandcount) + "</a></td><td valign=\"top\">" +
                        desc.get(commandcount) +
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
            if (request.getRequestURI().equals("/get/ip")) {
                response.getWriter().println(JDUtilities.getIPAddress());
            }
            //Get Config
            if (request.getRequestURI().equals("/get/config")) {
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
            if (request.getRequestURI().equals("/get/version")) {
                response.getWriter().println(JDUtilities.JD_VERSION + JDUtilities.getRevision());
            }
            
            //Get Current DLs COUNT
            if (request.getRequestURI().equals("/get/downloads/currentcount")) {
                response.getWriter().println(JDUtilities.getController().getRunningDownloadNum());
            }
            
            //Get Current DLs
            if (request.getRequestURI().equals("/get/downloads/currentlist")) {

                for (int i = 0; i < JDUtilities.getController().getDownloadLinks().size(); i++) {
                    if (JDUtilities.getController().getDownloadLinks().get(i).getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS)
                    {
                        response.getWriter().println(
                                "<- name=\""+JDUtilities.getController().getDownloadLinks().get(i).getName()+
                                "\" hoster=\"" + JDUtilities.getController().getDownloadLinks().get(i).getHost() + 
                                "\" speed=\"" + JDUtilities.getController().getDownloadLinks().get(i).getDownloadSpeed() + 
                                "\" ->");             
                    }             
                }   
            }
            
            //Get DLList COUNT
            if (request.getRequestURI().equals("/get/downloads/allcount")) {
                response.getWriter().println(JDUtilities.getController().getDownloadLinks().size());
            }
            
            //Get DLList
            if (request.getRequestURI().equals("/get/downloads/alllist")) {

                for (int i = 0; i < JDUtilities.getController().getDownloadLinks().size(); i++) {
                        response.getWriter().println(
                                "<- name=\""+JDUtilities.getController().getDownloadLinks().get(i).getName()+
                                "\" hoster=\"" + JDUtilities.getController().getDownloadLinks().get(i).getHost() + 
                                "\" speed=\"" + JDUtilities.getController().getDownloadLinks().get(i).getDownloadSpeed() + 
                                "\" statusint=\"" + JDUtilities.getController().getDownloadLinks().get(i).getStatus() + 
                                "\" statusstr=\"" + JDUtilities.getController().getDownloadLinks().get(i).getStatusText() + 
                                "\" container=\"" + JDUtilities.getController().getDownloadLinks().get(i).getContainerFile() + 
                                "\" waittime=\"" + JDUtilities.getController().getDownloadLinks().get(i).getWaitTime() + 
                                "\" ->");                          
                }   
            }
            
            //Get finished DLs COUNT
            if (request.getRequestURI().equals("/get/downloads/finishedcount")) {
                int counter = 0;
                Vector<DownloadLink> k = JDUtilities.getController().getDownloadLinks();

                for (int i = 0; i < k.size(); i++) {
                    if (k.get(i).getStatus() == DownloadLink.STATUS_DONE) counter++;
                }
                response.getWriter().println(counter);
            }
            
            //Get finished DLs
            if (request.getRequestURI().equals("/get/downloads/finishedlist")) {

                for (int i = 0; i < JDUtilities.getController().getDownloadLinks().size(); i++) {
                    if (JDUtilities.getController().getDownloadLinks().get(i).getStatus() == DownloadLink.STATUS_DONE)
                    {
                        response.getWriter().println(
                                "<- name=\""+JDUtilities.getController().getDownloadLinks().get(i).getName()+
                                "\" hoster=\"" + JDUtilities.getController().getDownloadLinks().get(i).getHost() + 
                                "\" crc=\"" + JDUtilities.getController().getDownloadLinks().get(i).getCrcStatus() + 
                                "\" ->");             
                    }             
                }   
            }
            
            //Get current Speed
            if (request.getRequestURI().equals("/get/speed")) {
                response.getWriter().println(JDUtilities.getController().getSpeedMeter() / 1000);
            }
            
            //Get IsReconnect
            if (request.getRequestURI().equals("/get/isreconnect")) {
                response.getWriter().println(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
            }
            //Get IsClipBoard
            if (request.getRequestURI().equals("/get/isclipboard")) {
                response.getWriter().println(JDUtilities.getController().getClipboard().isEnabled());
            }
            
            //---------------------------------------
            //Control
            //---------------------------------------
            
            //Do Start Download
            if (request.getRequestURI().equals("/action/start")) {
                JDUtilities.getController().startDownloads();
                response.getWriter().println("Downloads started");
            }
            
            //Do Pause Download
            if (request.getRequestURI().equals("/action/pause")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_PAUSE_DOWNLOADS, null));
                response.getWriter().println("Downloads paused");
            }
            
            //Do Stop Download
            if (request.getRequestURI().equals("/action/stop")) {
                JDUtilities.getController().stopDownloads();
                response.getWriter().println("Downloads stopped");
            }
            
            //Do Toggle Download
            if (request.getRequestURI().equals("/action/toggle")) {
                JDUtilities.getController().toggleStartStop();
                response.getWriter().println("Downloads toggled");
            }

            //Do Make Webupdate
            if (request.getRequestURI().equals("/action/update")) {
                //JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)
                
                boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);

                response.getWriter().println("Do Webupdate...");
            }
            
            //Do Reconnect
            if (request.getRequestURI().equals("/action/reconnect")) {
                response.getWriter().println("Do Reconnect...");
                
                boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                
                JDUtilities.getController().uiEvent(new UIEvent(JDUtilities.getGUI(), UIEvent.UI_INTERACT_RECONNECT));
                
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
                
                
            }
            
            //Do Restart JD
            if (request.getRequestURI().equals("/action/restart")) {
                //TODO: Ausgabe der Meldung. z.Z. nur keine Verbindung
                response.getWriter().println("Restarting...");
                JDUtilities.restartJD();
            }
            
            //Do Shutdown JD
            if (request.getRequestURI().equals("/action/shutdown")) {
                //TODO: Ausgabe der Meldung. z.Z. nur keine Verbindung
                response.getWriter().println("Shutting down...");
                JDUtilities.getController().exit();
            }
            
            //Set Downloadlimit
            if (request.getRequestURI().matches("(?is).*/action/set/download/limit/[0-9]+.*")) {
                Integer newdllimit = Integer.parseInt(new Regexp(request.getRequestURI(),
                        "[\\s\\S]*/action/set/download/limit/([0-9]+).*")
                        .getFirstMatch());
                logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString() );
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
                response.getWriter().println("newlimit=" + newdllimit);
            }
            
            //Set max. sim. Downloads
            if (request.getRequestURI().matches("(?is).*/action/set/download/max/[0-9]+.*")) {
                Integer newsimdl = Integer.parseInt(new Regexp(request.getRequestURI(),
                        "[\\s\\S]*/action/set/download/max/([0-9]+).*")
                        .getFirstMatch());
                logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString() );
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
                response.getWriter().println("newmax=" + newsimdl);
            }
            
            //OpenDialog Add-Links
            if (request.getRequestURI().equals("/action/open/add")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.ITEMS_ADD, null));
                response.getWriter().println("Add-Links Dialog opened");
            }
            
            //OpenDialog Config
            if (request.getRequestURI().equals("/action/open/config")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CONFIGURATION, null));
                response.getWriter().println("Config Dialog opened");
            }
            
            //OpenDialog Log
            if (request.getRequestURI().equals("/action/open/log")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOG, null));
                response.getWriter().println("Log Dialog opened");
            }
            
            //OpenDialog Container
            if (request.getRequestURI().equals("/action/open/containerdialog")) {
                simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOAD_DLC, null));
                response.getWriter().println("Container Dialog opened");
            }
            
            //Add Link(s) 
            if (request.getRequestURI().matches("(?is).*/action/add/links/grabber[01]{1}/start[01]{1}/[\\s\\S]+")) {
                String link = (new Regexp(request.getRequestURI(),
                    "[\\s\\S]*?/action/add/links/grabber[01]{1}/start[01]{1}/([\\s\\S]+)")
                    .getFirstMatch());
                //response.getWriter().println(link);
                Integer showgrab = Integer.parseInt((new Regexp(request.getRequestURI(),
                    "[\\s\\S]*?/action/add/links/grabber([01]{1})/start[01]{1}/[\\s\\S]+")
                    .getFirstMatch()));
                Boolean hidegrabber = false;
                if(showgrab == 0){hidegrabber = true;};
                //response.getWriter().println(hidegrabber.toString());
                Integer stdl = Integer.parseInt((new Regexp(request.getRequestURI(),
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
            if (request.getRequestURI().matches("(?is).*/action/add/container/[\\s\\S]+")) {
                String dlcfilestr = (new Regexp(request.getRequestURI(),
                "[\\s\\S]*/action/add/container/([\\s\\S]+)")
                .getFirstMatch());
                dlcfilestr = JDUtilities.htmlDecode(dlcfilestr);
                //wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().loadContainerFile(new File(dlcfilestr));
                response.getWriter().println("Container opened. (" + dlcfilestr + ")");
            }
            
            //Save Linklist as DLC Container
            if (request.getRequestURI().matches("(?is).*/action/save/container/[\\s\\S]+")) {
                String dlcfilestr = (new Regexp(request.getRequestURI(),
                "[\\s\\S]*/action/save/container/([\\s\\S]+)")
                .getFirstMatch());
                dlcfilestr = JDUtilities.htmlDecode(dlcfilestr);
                //wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().saveDLC(new File(dlcfilestr));
                response.getWriter().println("Container saved. (" + dlcfilestr + ")");
            }
            
            //Set ClipBoard
            if (request.getRequestURI().matches("(?is).*/action/set/clipboard/.*")) {
                boolean newclip = Boolean.parseBoolean(new Regexp(request.getRequestURI(),
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
            
            //Set ReconnectEnabled
            if (request.getRequestURI().matches("(?is).*/action/set/reconnectenabled/.*")) {
                boolean newrc = Boolean.parseBoolean(new Regexp(request.getRequestURI(),
                     "[\\s\\S]*/action/set/reconnectenabled/(.*)")
                     .getFirstMatch());
                logger.fine("RemoteControl - Set ReConnect: " + newrc );
                if((!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) ^ (newrc)) /*C++ User:^ is equuvalent to XOR*/
                {
                    simplegui.toggleReconnect(false);
                    response.getWriter().println("reconnect=" + newrc + " (CHANGED=true)");
                }
                else
                {
                    response.getWriter().println("reconnect=" + newrc + " (CHANGED=false)");
                } 
            }
            
  //        //Set use premium 
  //            if (request.getRequestURI().matches("(?is).*/action/download/premium/.*")) {
  //              boolean newuseprem = Boolean.parseBoolean(new Regexp(request.getRequestURI(),
  //                      "[\\s\\S]*/action/download/premium/(.*)")
  //                      .getFirstMatch());
  //              logger.fine("RemoteControl - Set Premium: " + newuseprem );
  //              JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
  //            response.getWriter().println("newprem=" + newuseprem);
  //        }
  //        

            
            ((Request) request).setHandled(true);
        }
    };

}
