package jd.plugins.optional.remotecontrol.helppage;

import java.util.ArrayList;

public class HelpPage {

    private static final long serialVersionUID = -7554703678938558791L;

    private static ArrayList<Table> tables = new ArrayList<Table>();

    private static Table create(Table table) {
        tables.add(table);
        return table;
    }

    private static void createTables() {
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

        t.setCommand("/action/add(/auto)/links/%X%");
        t.setInfo("Add links %X% to grabber<br/>" + "e.g. /action/add/links/http://tinyurl.com/6o73eq" + "<p>auto parameter: Downloads will be automatically added to download queue and will start afterwards</p>" + "Note: Links must be URLEncoded. Use NEWLINE between links!");

        t.setCommand("/action/add(/auto)/container/%X%");
        t.setInfo("Add (remote or local) container %X%<br/>" + "e.g. /action/add/container/C:\\container.dlc" + "<p>auto parameter: Downloads will be automatically added to download queue and will start afterwards</p>" + "Note: Address (remote or local) must be URLEncoded!");

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

        // TODO:
        // JDScriptLaucher table - will accessing JDScriptLaucher addon's
        // functionality
        //
        // t = create(new Table("[addon] JDScriptLauncher"));
        //
        // t.setCommand("/addon/scriptlauncher/launch/%X%");
        // t.setInfo("Launches a script on the remote machine via JDScriptLauncher addon");
    }

    public static String getHTML() {
        createTables();

        String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"><head><title>JDRemoteControl Help</title><style type=\"text/css\">" + "a {text-decoration:none; color:#5f5f5f}" + "a:hover {text-decoration:underline; color_#5f5f5f;}" + "body {margin:0% 10% 20% 10%; font-size:12px; color:#5f5f5f; background-color:#ffffff; font-family:Verdana, Arial, Helvetica, sans-serif;}" + "table {width:100%; padding:none; border:1px solid #5f5f5f; border-collapse:collapse; background-color:#ffffff;}" + "td {width:50%; padding:5px; border-top:1px solid #5f5f5f; border-bottom:1px solid #5f5f5f; vertical-align:top;}" + "th {color:#ffffff; background-color:#5f5f5f; font-size:13px; font-weight:normal; text-align:left; padding:5px;}"
                + "tr:hover {background-color:#E3F3B8;}" + "h1 {font-size:25px; font-weight:normal; color:#5f5f5f;}" + "</style></head><body><h1>JDRemoteControl " + "&lt;Versions-Nr.&gt;" + "</h1><p>&nbsp;</p>" + "<p>Replace %X% and %Y% with specific values e.g. /action/save/container/C:\\backup.dlc<br/>Replace (true|false) with true or false<br/>Replace (value) with value => optional parameter<p/>";

        for (Table table : tables) {
            html += "<table>";
            html += "<tr><th colspan=\"2\">" + table.getName() + "</th></tr>";

            for (Entry entry : table.getEntries()) {
                html += "<tr><td><a href=\"" + entry.getCommand() + "\" target=\"_blank\">" + entry.getCommand() + "</a></td><td>" + entry.getInfo() + "</td></tr>";
            }

            html += "</table><br/><br/>";
        }

        html += "</html>";

        return html;
    }
}