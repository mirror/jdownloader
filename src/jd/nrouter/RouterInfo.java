package jd.nrouter;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.nutils.Executer;
import jd.nutils.OSDetector;
import jd.nutils.Threader;
import jd.nutils.jobber.JDRunnable;
import jd.parser.Regex;

public class RouterInfo {
    private InetAddress address;
    public final static ArrayList<String> HOST_NAMES = new ArrayList<String>();
    private static RouterInfo INSTANCE = null;
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

    /**
     * Singleton. use RouterInfo.getInstance
     */
    private RouterInfo() {

    }

    /**
     * Returns a shared instance of RouterInfo
     * 
     * @return
     */
    public synchronized static RouterInfo getInstance() {
       
            if (INSTANCE == null) INSTANCE = new RouterInfo();
            return INSTANCE;
        
    }

    /**
     * Tries to find the ourter's ip adress and returns it.
     * 
     * @return
     */
    public synchronized InetAddress getAddress() {
        if (address != null) {

        return address; }
        address = getIPFormNetStat();
        if (address != null) return address;
        address = getIPFromRouteCommand();
        if (address != null) return address;
        address = getIpFormHostTable();

        return address;

    }

    /**
     * UPdates the host table and adds the full ip range (0-255) of the local
     * devices to the table.
     */
    private void updateHostTable() {
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
 * RUns throw a predefined  HOst Table (multithreaded) and checks if there is a service on port 80.
 * returns the ip if there is a webservice on any adress. See updateHostTable() 
 * @return
 */
    public InetAddress getIpFormHostTable() {
        updateHostTable();
        final int size = HOST_NAMES.size();
        final Threader threader = new Threader();
        for (int i = 0; i < size; i++) {
            final int d = i;
            threader.add(new JDRunnable() {

                public void go() throws Exception {

                    final String hostname = HOST_NAMES.get(d);
                    try {
                        InetAddress ia = InetAddress.getByName(hostname);
                        if (ia.isReachable(1500)) {
                            if (RouterUtils.checkport(hostname, 80)) {
                                address = ia;

                                threader.interrupt();
                            }
                        }

                    } catch (IOException e) {
                    }

                }
            });

        }
        threader.startAndWait();
        return address;
    }

    /**
     * USes the /sbin/route command to determine therouter's ip. works on linux
     * and mac.
     * 
     * @return
     */
    public InetAddress getIPFromRouteCommand() {

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
                                    if (RouterUtils.checkport(hostname, 80)) { return ia; }
                                }
                            } catch (UnknownHostException e) {

                                JDLogger.exception(e);
                            } catch (IOException e) {

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
                                    if (RouterUtils.checkport(hostname, 80)) { return ia; }
                                }
                            } catch (UnknownHostException e) {

                                JDLogger.exception(e);
                            } catch (IOException e) {

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
     * 
     */
    public InetAddress getIPFormNetStat() {
        try {
            Pattern pat = Pattern.compile("^\\s*(?:0\\.0\\.0\\.0\\s*){1,2}((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)).*");
            Executer exec = new Executer("netstat");
            exec.addParameter("-rn");
            exec.setWaitTimeout(5000);
            exec.start();
            exec.waitTimeout();
            String[] out = exec.getOutputStream().split("[\r\n]+");
            for (String string : out) {
                String m = new Regex(string, pat).getMatch(0);
                if(m!=null)
                {
                    InetAddress ia = InetAddress.getByName(m);
                    if (ia.isReachable(1500)) {

                    return ia; }
                }
            }


        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

}
