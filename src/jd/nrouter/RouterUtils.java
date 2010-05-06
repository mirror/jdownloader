//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nrouter;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nutils.Executer;
import jd.nutils.OSDetector;
import jd.nutils.Threader;
import jd.nutils.Threader.WorkerListener;
import jd.nutils.jobber.JDRunnable;
import jd.parser.Regex;
import jd.utils.locale.JDL;

public class RouterUtils {

    public final static ArrayList<String> HOST_NAMES = new ArrayList<String>();
    private static InetAddress ADDRESS_CACHE;

    static {

        HOST_NAMES.add("fritz.fonwlan.box");
        HOST_NAMES.add("speedport.ip");
        HOST_NAMES.add("fritz.box");
        HOST_NAMES.add("dsldevice.lan");
        HOST_NAMES.add("speedtouch.lan");
        HOST_NAMES.add("mygateway1.ar7");
        HOST_NAMES.add("fritz.fon.box");
        HOST_NAMES.add("home");
        HOST_NAMES.add("arcor.easybox");
        HOST_NAMES.add("fritz.slwlan.box");
        HOST_NAMES.add("eumex.ip");
        HOST_NAMES.add("easy.box");
        HOST_NAMES.add("my.router");
        HOST_NAMES.add("fritz.fon");
        HOST_NAMES.add("router");
        HOST_NAMES.add("mygateway.ar7");
        HOST_NAMES.add("login.router");
        HOST_NAMES.add("SX541");
        HOST_NAMES.add("SE515.home");
        HOST_NAMES.add("sinus.ip");
        HOST_NAMES.add("fritz.wlan.box");
        HOST_NAMES.add("my.siemens");
        HOST_NAMES.add("local.gateway");
        HOST_NAMES.add("congstar.box");
        HOST_NAMES.add("login.modem");
        HOST_NAMES.add("homegate.homenet.telecomitalia.it");
        HOST_NAMES.add("SE551");
        HOST_NAMES.add("home.gateway");
        HOST_NAMES.add("alice.box");
        HOST_NAMES.add("www.brntech.com.tw");
        HOST_NAMES.add("buffalo.setup");
        HOST_NAMES.add("vood.lan");
        HOST_NAMES.add("DD-WRT");
        HOST_NAMES.add("versatel.modem");
        HOST_NAMES.add("myrouter.home");
        HOST_NAMES.add("MyDslModem.local.lan");
        HOST_NAMES.add("alicebox");
        HOST_NAMES.add("HSIB.home");
        HOST_NAMES.add("AolynkDslRouter.local.lan");
        HOST_NAMES.add("SL2141I.home");
        HOST_NAMES.add("e.home");
        HOST_NAMES.add("dsldevice.domain.name");
        HOST_NAMES.add("192.168.1.1");
        HOST_NAMES.add("192.168.2.1");
        HOST_NAMES.add("192.168.0.1");
        HOST_NAMES.add("192.168.178.1");
        HOST_NAMES.add("192.168.1.254");
        HOST_NAMES.add("10.0.0.138");
        HOST_NAMES.add("10.0.0.2");
        HOST_NAMES.add("192.168.123.254");
        HOST_NAMES.add("192.168.1.2");
        HOST_NAMES.add("192.168.0.254");
        HOST_NAMES.add("10.0.0.1");
        HOST_NAMES.add("192.168.10.1");
        HOST_NAMES.add("192.168.220.1");
        HOST_NAMES.add("192.168.254.254");
        HOST_NAMES.add("192.168.0.2");
        HOST_NAMES.add("192.168.100.1");
        HOST_NAMES.add("192.168.1.100");
        HOST_NAMES.add("10.1.1.1");
        HOST_NAMES.add("192.168.0.100");
        HOST_NAMES.add("192.168.3.1");
        HOST_NAMES.add("192.168.5.1");
        HOST_NAMES.add("192.168.2.2");
        HOST_NAMES.add("192.168.11.1");
        HOST_NAMES.add("192.168.1.10");
        HOST_NAMES.add("192.168.0.10");
        HOST_NAMES.add("192.168.0.253");
        HOST_NAMES.add("192.168.7.1");
        HOST_NAMES.add("192.168.182.1");
        HOST_NAMES.add("192.168.2.254");
        HOST_NAMES.add("192.168.178.2");
        HOST_NAMES.add("192.168.15.1");
        HOST_NAMES.add("192.168.1.5");
        HOST_NAMES.add("192.168.0.3");
        HOST_NAMES.add("192.168.123.1");
        HOST_NAMES.add("192.168.1.253");
        HOST_NAMES.add("192.168.0.99");
        HOST_NAMES.add("172.16.0.1");
        HOST_NAMES.add("192.168.4.1");

    }

    public static String findIP(final GUIConfigEntry ip) {
        return new GuiRunnable<String>() {

            @Override
            public String runSave() {
                final ProgressController progress = new ProgressController(JDL.L("gui.config.routeripfinder.featchIP", "Search for routers hostname..."), 100, null);

                ip.setData(JDL.L("gui.config.routeripfinder.featchIP", "Search for routers hostname..."));

                progress.setStatus(80);
                InetAddress ia = RouterUtils.getAddress(false);
                if (ia != null) ip.setData(ia.getHostAddress());
                progress.setStatus(100);
                if (ia != null) {
                    progress.setStatusText(JDL.LF("gui.config.routeripfinder.ready", "Hostname found: %s", ia.getHostAddress()));
                    progress.doFinalize(3000);
                    return ia.getHostAddress();
                } else {
                    progress.setStatusText(JDL.L("gui.config.routeripfinder.notfound", "Can't find your routers hostname"));
                    progress.doFinalize(3000);
                    progress.setColor(Color.RED);
                    return null;
                }
            }

        }.getReturnValue();
    }

    /**
     * Validates the givvei ip. a) checks if it is a valid IP adress (regex) b)
     * checks if it is available within a timeout of 1500 ms
     * 
     * @param iPaddress
     * @return
     */
    public static boolean validateIP(String iPaddress) {
        final Pattern IP_PATTERN = Pattern.compile("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        if (IP_PATTERN.matcher(iPaddress).matches()) {
            return true;
        } else {
            try {
                if (InetAddress.getByName(iPaddress).isReachable(1500)) return true;
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
        return false;
    }

    /**
     * Returns all InetAddresses of the local Network devices.
     * 
     * @return
     */
    public static ArrayList<InetAddress> getNetworkDeviceAdresses() {
        ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();

            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();

                Enumeration<InetAddress> e2 = ni.getInetAddresses();

                while (e2.hasMoreElements()) {
                    InetAddress ip = e2.nextElement();
                    if (ip.isLoopbackAddress()) break;
                    if (ip.getHostAddress().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) ret.add(ip);
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return ret;
    }

    /**
     * checks if there is a open port at host. e.gh. test if there is a
     * webserverr unning on this port
     * 
     * @param host
     * @param port
     * @return
     */
    public static boolean checkPort(String host, int port) {
        Socket sock;
        try {
            sock = new Socket(host, port);
            sock.setSoTimeout(200);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Tries to find the router's ip adress and returns it.
     * 
     * @param force
     *            if false, jd uses a cached value if available
     * 
     * @return
     */
    public synchronized static InetAddress getAddress(boolean force) {
        if (!force && ADDRESS_CACHE != null) return ADDRESS_CACHE;
        InetAddress address = null;
        try {
            address = getIPFormNetStat();
            if (address != null) return address;
            address = getIPFromRouteCommand();
            if (address != null) return address;
            address = getIpFormHostTable();
            return address;
        } finally {
            ADDRESS_CACHE = address;
        }
    }

    /**
     * Updates the host table and adds the full ip range (0-255) of the local
     * devices to the table.
     */
    private static void updateHostTable() {
        String ip;

        for (InetAddress ia : RouterUtils.getNetworkDeviceAdresses()) {
            try {

                if (RouterUtils.validateIP(ia.getHostAddress() + "")) {
                    ip = ia.getHostAddress();

                    if (ip != null && ip.lastIndexOf(".") != -1) {
                        String host = ip.substring(0, ip.lastIndexOf(".")) + ".";
                        for (int i = 0; i < 255; i++) {
                            String lhost = host + i;
                            if (!lhost.equals(ip) && !HOST_NAMES.contains(lhost)) {
                                HOST_NAMES.add(lhost);
                            }

                        }
                    }
                }
                HOST_NAMES.remove(ia.getHostName());
                HOST_NAMES.remove(ia.getAddress());
            } catch (Exception exc) {
                JDLogger.exception(exc);
            }
        }
    }

    /**
     * Runs throw a predefined Host Table (multithreaded) and checks if there is
     * a service on port 80. returns the ip if there is a webservice on any
     * adress. See {@link #updateHostTable()}
     * 
     * @return
     */
    private static InetAddress ASYNCH_RETURN;

    public static InetAddress getIpFormHostTable() {
        updateHostTable();
        ASYNCH_RETURN = null;
        final int size = HOST_NAMES.size();
        final Threader threader = new Threader();
        for (int i = 0; i < size; i++) {
            threader.add(new WebServerChecker(HOST_NAMES.get(i)));
        }

        threader.getBroadcaster().addListener(new WorkerListener() {

            public void onThreadException(Threader th, JDRunnable job, Throwable e) {
            }

            public void onThreadFinished(Threader th, JDRunnable runnable) {
                if (((WebServerChecker) runnable).getAddress() != null) {
                    th.interrupt();
                    ASYNCH_RETURN = ((WebServerChecker) runnable).getAddress();
                }

            }

            public void onThreadStarts(Threader threader, JDRunnable runnable) {
            }

        });
        threader.startAndWait();
        return ASYNCH_RETURN;
    }

    /**
     * Uses the /sbin/route command to determine the router's ip. works on linux
     * and mac.
     * 
     * @return
     */
    public static InetAddress getIPFromRouteCommand() {

        if (new File("/sbin/route").exists()) {
            try {

                if (OSDetector.isMac()) {

                    Executer exec = new Executer("/sbin/route");
                    exec.addParameters(new String[] { "-n", "get", "default" });
                    exec.setRunin("/");
                    exec.setWaitTimeout(1000);
                    exec.start();
                    exec.waitTimeout();
                    String routingt = exec.getOutputStream() + " \r\n " + exec.getErrorStream();
                    Pattern pattern = Pattern.compile("gateway: (\\S*)", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(routingt);
                    while (matcher.find()) {
                        String hostname = matcher.group(1).trim();
                        if (!hostname.matches("[\\s]*\\*[\\s]*")) {
                            try {
                                InetAddress ia = InetAddress.getByName(hostname);
                                if (ia.isReachable(1500)) {
                                    if (RouterUtils.checkPort(hostname, 80)) return ia;
                                }
                            } catch (Exception e) {
                                JDLogger.exception(e);
                            }
                        }

                    }
                } else {
                    Executer exec = new Executer("/sbin/route");
                    exec.addParameters(new String[] { "-n", "get", "default" });
                    exec.setRunin("/");
                    exec.setWaitTimeout(1000);
                    exec.start();
                    exec.waitTimeout();
                    String routingt = exec.getOutputStream() + " \r\n " + exec.getErrorStream();
                    routingt = routingt.replaceFirst(".*\n.*", "");

                    Pattern pattern = Pattern.compile(".{16}(.{16}).*", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(routingt);
                    while (matcher.find()) {
                        String hostname = matcher.group(1).trim();
                        if (!hostname.matches("[\\s]*\\*[\\s]*")) {
                            try {
                                InetAddress ia = InetAddress.getByName(hostname);
                                if (ia.isReachable(1500)) {
                                    if (RouterUtils.checkPort(hostname, 80)) return ia;
                                }
                            } catch (Exception e) {
                                JDLogger.exception(e);
                            }
                        }

                    }
                }
            } catch (Exception e) {
            }

        }
        return null;
    }

    /**
     * Calls netstat -nt to find the router's ip. returns null if nothing found
     * and the ip if found something;
     * http://jdownloader.net:8081/pastebin/1ab4eabb60df171d0d442f0c7fb875a0
     */
    public static InetAddress getIPFormNetStat() {
        try {
            Pattern pat = Pattern.compile("^\\s*(?:0\\.0\\.0\\.0\\s*){1,2}((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)).*");
            Executer exec = new Executer("netstat");
            exec.addParameter("-rn");
            exec.setWaitTimeout(5000);
            exec.start();
            exec.waitTimeout();

            String[] out = Regex.getLines(exec.getOutputStream());
            for (String string : out) {
                String m = new Regex(string, pat).getMatch(0);
                if (m != null) {
                    InetAddress ia = InetAddress.getByName(m);
                    if (ia.isReachable(1500)) return ia;
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    private static class WebServerChecker implements JDRunnable {

        private String host;
        private InetAddress address = null;

        public InetAddress getAddress() {
            return address;
        }

        public WebServerChecker(String host) {
            this.host = host;
        }

        public void go() throws Exception {
            try {
                InetAddress ia = InetAddress.getByName(host);
                if (ia.isReachable(1500)) {
                    if (RouterUtils.checkPort(host, 80)) {
                        address = ia;
                    }
                }
            } catch (IOException e) {
            }
        }
    }

}
