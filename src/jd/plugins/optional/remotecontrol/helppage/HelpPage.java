//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.remotecontrol.helppage;

import java.util.ArrayList;

import jd.OptionalPluginWrapper;
import jd.utils.JDUtilities;

public class HelpPage {

    private static final long serialVersionUID = -7554703678938558791L;

    private static ArrayList<Table> tables = new ArrayList<Table>();
    private static OptionalPluginWrapper rc = JDUtilities.getOptionalPlugin("remotecontrol");

    private static Table create(Table table) {
        tables.add(table);
        return table;
    }

    private static void initTables() {
        Table t = null;

        // Values table
        t = create(new Table("Get values"));

        t.setCommand("/get/rcversion");
        t.setInfo("Get RemoteControl version");

        t.setCommand("/get/version");
        t.setInfo("Get version");

        t.setCommand("/get/config");
        t.setInfo("Get config");

        t.setCommand("/get/ip");
        t.setInfo("Get IP");

        t.setCommand("/get/randomip");
        t.setInfo("Get random IP as replacement for real IP-check");

        t.setCommand("/get/speed");
        t.setInfo("Get current speed");

        t.setCommand("/get/speedlimit");
        t.setInfo("Get current speed limit");

        t.setCommand("/get/downloadstatus");
        t.setInfo("Get download status => RUNNING, NOT_RUNNING or STOPPING");

        t.setCommand("/get/isreconnect");
        t.setInfo("Get whether reconnect is enabled or not");

        t.setCommand("/get/grabber/list");
        t.setInfo("Get all links that are currently held by the link grabber (XML)");

        t.setCommand("/get/grabber/count");
        t.setInfo("Get amount of all links in linkgrabber");

        t.setCommand("/get/grabber/isbusy");
        t.setInfo("Get whether linkgrabber is busy or not");

        t.setCommand("/get/grabber/isset/startafteradding");
        t.setInfo("Get whether downloads should start or not start after they were added to the download queue");

        t.setCommand("/get/downloads/all/count");
        t.setInfo("Get amount of all downloads");

        t.setCommand("/get/downloads/current/count");
        t.setInfo("Get amount of current downloads");

        t.setCommand("/get/downloads/finished/count");
        t.setInfo("Get amount of finished downloads");

        t.setCommand("/get/downloads/all/list");
        t.setInfo("Get list of all downloads (XML)");

        t.setCommand("/get/downloads/current/list");
        t.setInfo("Get list of current downloads (XML)");

        t.setCommand("/get/downloads/finished/list");
        t.setInfo("Get list of finished downloads (XML)");

        // Actions table
        t = create(new Table("Actions"));

        t.setCommand("/action/start");
        t.setInfo("Start downloads");

        t.setCommand("/action/pause");
        t.setInfo("Pause downloads");

        t.setCommand("/action/stop");
        t.setInfo("Stop downloads");

        t.setCommand("/action/toggle");
        t.setInfo("Toggle downloads");

        t.setCommand("/action/reconnect");
        t.setInfo("Reconnect");

        t.setCommand("/action/(force)update");
        t.setInfo("Do a webupdate - /action/forceupdate will activate auto-restart if update is possible");

        t.setCommand("/action/restart");
        t.setInfo("Restart JDownloader");

        t.setCommand("/action/shutdown");
        t.setInfo("Shutdown JDownloader");

        t.setCommand("/action/set/download/limit/%X%");
        t.setInfo("Set download speedlimit %X%");

        t.setCommand("/action/set/download/max/%X%");
        t.setInfo("Set max. sim. Downloads %X%");

        t.setCommand("/action/set/reconnect/(true|false)");
        t.setInfo("Set reconnect enabled or not");

        t.setCommand("/action/set/premium/(true|false)");
        t.setInfo("Set premium usage enabled or not");

        t.setCommand("/action/set/grabber/startafteradding/(true|false)");
        t.setInfo("Set whether downloads should start or not start after they were added to the download queue");

        t.setCommand("/action/add/archivepassword/%X%/%Y%");
        t.setInfo("Add an archive password %Y% to one or more packages with packagename %X% hold by the linkgrabber, each packagename seperated by a slash)");

        t.setCommand("/action/add(/auto)/links/%X%");
        t.setInfo("Add links %X% to grabber<br/>" + "e.g. /action/add/links/http://tinyurl.com/6o73eq" + "<p>auto parameter: Downloads will be automatically added to download queue after the linkcheck is done</p>" + "Note: Links must be URLEncoded. Use NEWLINE between links!");

        t.setCommand("/action/add(/auto)/container/%X%");
        t.setInfo("Add (remote or local) container %X%<br/>" + "e.g. /action/add/container/C:\\container.dlc" + "<p>auto parameter: Downloads will be automatically added to download queue after the linkcheck is done</p>" + "Note: Address (remote or local) must be URLEncoded!");

        t.setCommand("/action/save/container(/fromgrabber)/%X%");
        t.setInfo("Save DLC-container with all links to %X%<br/>" + "e.g. /action/add/container/%X%" + "<p>fromgrabber: save DLC-container from grabber list instead from download list</p>");

        t.setCommand("/action/grabber/join/%X%/%Y%");
        t.setInfo("Join all denoted linkgrabber packages %Y%, each separated by a slash, to the package %X%");

        t.setCommand("/action/grabber/rename/%X%/%Y%");
        t.setInfo("Rename link grabber package from %X% to %Y%");

        t.setCommand("/action/grabber/confirmall");
        t.setInfo("Schedule all packages as download that are located in the link grabber");

        t.setCommand("/action/grabber/confirm/%X%");
        t.setInfo("Schedule all denoted grabber packages %X%, each seperated by a slash, as download");

        t.setCommand("/action/grabber/removetype/%X%");
        t.setInfo("Remove links from grabber that match the denoted type(s) %X% - the types must be seperated by a slash. Possible type values: 'offline' for offline links and 'available' for links that are already scheduled as download");

        t.setCommand("/action/grabber/removeall");
        t.setInfo("Remove all links from linkgrabber");

        t.setCommand("/action/grabber/remove/%X%");
        t.setInfo("Remove packages %X% from linkgrabber, each packagename seperated by a slash");

        t.setCommand("/action/grabber/move/%X%/%Y%");
        t.setInfo("Move %Y% (single link or list of links, each separated by NEWLINE char) to package %X%. In case the package given is not available, it will be newly created. Please note that if there are multiple packages named equally, the links will be put into the first one that is found. The term 'link' equals the 'browser url' you've provided previously, not the final download url. Package will be searched by case insensitive search.");

        t.setCommand("/action/downloads/removeall");
        t.setInfo("Remove all scheduled downloads");

        t.setCommand("/action/downloads/remove/%X%");
        t.setInfo("Remove packages %X% from download list, each packagename seperated by a slash");

        // special table
        t = create(new Table("Specials"));

        t.setCommand("/special/check/%X%");
        t.setInfo("Check links in %X% without adding them to the linkgrabber or the download list. %X% may be a list of urls. Note: Links must be URLEncoded. Use NEWLINE between links!");

        // JDScriptLaucher table
        OptionalPluginWrapper sl = JDUtilities.getOptionalPlugin("scriptlauncher");

        if (sl != null && sl.isLoaded() && sl.isEnabled()) {
            t = create(new Table("[addon] JDScriptLauncher"));

            t.setCommand("/addon/scriptlauncher/getlist");
            t.setInfo("Get list of all available scripts");

            t.setCommand("/addon/scriptlauncher/launch/%X%");
            t.setInfo("Launches a script on the remote machine via JDScriptLauncher addon");
        }
    }

    public static String getHTML() {
        initTables();

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");

        html.append("<head>");
        html.append("<title>JDRemoteControl Help</title>");

        // stylesheet
        html.append("<style type=\"text/css\">");
        html.append("a {text-decoration:none; color:#5f5f5f}");
        html.append("a:hover {text-decoration:underline; color_#5f5f5f;}");
        html.append("body {margin:0% 10% 20% 10%; font-size:12px; color:#5f5f5f; background-color:#ffffff; font-family:Verdana, Arial, Helvetica, sans-serif;}");
        html.append("table {width:100%; padding:none; border:1px solid #5f5f5f; border-collapse:collapse; background-color:#ffffff;}");
        html.append("td {width:50%; padding:5px; border-top:1px solid #5f5f5f; border-bottom:1px solid #5f5f5f; vertical-align:top;}");
        html.append("th {color:#ffffff; background-color:#5f5f5f; font-size:13px; font-weight:normal; text-align:left; padding:5px;}");
        html.append("tr:hover {background-color:#E3F3B8;}");
        html.append("h1 {font-size:25px; font-weight:normal; color:#5f5f5f;}");
        html.append("</style>");

        html.append("</head>");

        // begin of body
        html.append("<body>");
        html.append("<h1>JDRemoteControl " + rc.getVersion() + "</h1>");
        html.append("<p>&nbsp;</p>");
        html.append("<p>Replace %X% and %Y% with specific values e.g. /action/save/container/C:\\backup.dlc<br/>Replace (true|false) with true or false<br/>Replace (value) with value => optional parameter<p/>");

        for (Table table : tables) {
            html.append("<table>");
            html.append("<tr><th colspan=\"2\">" + table.getName() + "</th></tr>");

            for (Entry entry : table.getEntries()) {
                html.append("<tr>");
                html.append("<td><a href=\"" + entry.getCommand() + "\" target=\"_blank\">" + entry.getCommand() + "</a></td>");
                html.append("<td>" + entry.getInfo() + "</td>");
                html.append("</tr>");
            }

            html.append("</table>");
            html.append("<p>&nbsp;</p>");
        }

        html.append("</html>");

        return html.toString();
    }
}