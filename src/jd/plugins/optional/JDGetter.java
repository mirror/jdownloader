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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.mortbay.jetty.handler.*;
import org.mortbay.jetty.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

public class JDGetter extends PluginOptional {
    
    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getPluginID() {
        return "0.0.0.1";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.getter.name","Getter");
    }

    @Override
    public String getVersion() {
        return "0.0.0.1";
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if(JDUtilities.getJavaVersion() >= 1.5) {
            if (enable) {
                logger.info("Getter OK");
                initgetter();
            }
        }
        else {
            logger.severe("Error initializing Getter");
        }
    }
    
    public void initgetter() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), "SPEED1", JDLocale.L("plugins.optional.getter.port","Port:"), 10000, 65500));
        cfg.setDefaultValue(10000);
        
        Handler handler = new AbstractHandler()
        {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
            {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                
                if(request.getRequestURI().equals("/ip")) {
                    response.getWriter().println(JDUtilities.getIPAddress());
                }
                if(request.getRequestURI().equals("/version")) {
                    response.getWriter().println(JDUtilities.JD_VERSION + JDUtilities.getRevision());
                }
                if(request.getRequestURI().equals("/downloads/current")) {
                    response.getWriter().println(JDUtilities.getController().getRunningDownloadNum());
                }
                if(request.getRequestURI().equals("/downloads/max")) {
                    response.getWriter().println(JDUtilities.getController().getDownloadLinks().size());
                }
                if(request.getRequestURI().equals("/downloads/finished")) {
                    int counter = 0;
                    Vector<DownloadLink> k = JDUtilities.getController().getDownloadLinks();
                    
                    for(int i=0; i<k.size(); i++) {
                        if(k.get(i).getStatus() == DownloadLink.STATUS_DONE)
                            counter++;
                    }
                    response.getWriter().println(counter);
                }
                if(request.getRequestURI().equals("/speed")) {
                    response.getWriter().println(JDUtilities.getController().getSpeedMeter() / 1000);
                }
                if(request.getRequestURI().equals("/isreconnect")) {
                    response.getWriter().println(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
                }
                if(request.getRequestURI().equals("/isclipboard")) {
                    response.getWriter().println(JDUtilities.getController().getClipboard().isEnabled());
                }
                
                ((Request)request).setHandled(true);
            }
        };
        
        try {
            Server server = new Server(8080);
            server.setHandler(handler);
            server.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean isExecutable() {
        return false;
    }
    @Override
    public boolean execute() {
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public ArrayList<String> createMenuitems() {
        return null;
    }
}
