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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.Property;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class JDGetter extends PluginOptional implements ControlListener {

    private Server server;
    private AbstractHandler serverHandler;

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
        return JDLocale.L("plugins.optional.getter.name", "Getter");
    }

    @Override
    public String getVersion() {
        return "0.0.0.1";
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if (JDUtilities.getJavaVersion() >= 1.5) {
            if (enable) {
                logger.info("Getter OK");
                initgetter();
               JDUtilities.getController().addControlListener(this);
            }
        } else {
            logger.severe("Error initializing Getter");
        }
    }

    public void initgetter() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), "PORT", JDLocale.L("plugins.optional.getter.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10000);
    
        try {
            server = new Server(this.getProperties().getIntegerProperty("PORT", 10000));
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
                server = new Server(this.getProperties().getIntegerProperty("PORT", 10000));
                server.setHandler(new Serverhandler());
                server.start();
                JDUtilities.getGUI().showMessageDialog(this.getPluginName() + " started on port "+this.getProperties().getIntegerProperty("PORT", 10000));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ArrayList<String> createMenuitems() {
        ArrayList<String> menu = new ArrayList<String>();
        menu.add("Toggle Start/Stop");
        return menu;
    }



    class Serverhandler extends AbstractHandler {

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);

            if (request.getRequestURI().equals("/ip")) {
                response.getWriter().println(JDUtilities.getIPAddress());
            }
            if (request.getRequestURI().equals("/config")) {
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
            if (request.getRequestURI().equals("/version")) {
                response.getWriter().println(JDUtilities.JD_VERSION + JDUtilities.getRevision());
            }
            if (request.getRequestURI().equals("/downloads/current")) {
                response.getWriter().println(JDUtilities.getController().getRunningDownloadNum());
            }
            if (request.getRequestURI().equals("/downloads/max")) {
                response.getWriter().println(JDUtilities.getController().getDownloadLinks().size());
            }
            if (request.getRequestURI().equals("/downloads/finished")) {
                int counter = 0;
                Vector<DownloadLink> k = JDUtilities.getController().getDownloadLinks();

                for (int i = 0; i < k.size(); i++) {
                    if (k.get(i).getStatus() == DownloadLink.STATUS_DONE) counter++;
                }
                response.getWriter().println(counter);
            }
            if (request.getRequestURI().equals("/speed")) {
                response.getWriter().println(JDUtilities.getController().getSpeedMeter() / 1000);
            }
            if (request.getRequestURI().equals("/isreconnect")) {
                response.getWriter().println(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
            }
            if (request.getRequestURI().equals("/isclipboard")) {
                response.getWriter().println(JDUtilities.getController().getClipboard().isEnabled());
            }

            ((Request) request).setHandled(true);
        }
    };

}
