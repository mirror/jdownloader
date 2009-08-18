package jd.nrouter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;

public class RouterUtils {

    /**
     * Validates the givvei ip. a) checks if it is a valid IP adress (regex) b)
     * checks if it is available within a timeout of 1500 ms
     * 
     * @param iPaddress
     * @return
     */
    public static boolean validateIP(String iPaddress) {
        final Pattern IP_PATTERN = Pattern.compile("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        if (IP_PATTERN.matcher(iPaddress).matches())
            return true;
        else {
            try {
                if (InetAddress.getByName(iPaddress).isReachable(1500)) { return true; }
            } catch (UnknownHostException e) {

                JDLogger.exception(e);
            } catch (IOException e) {

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
                NetworkInterface ni = (NetworkInterface) e.nextElement();

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
    public static boolean checkport(String host, int port) {
        Socket sock;
        try {
            sock = new Socket(host, port);
            sock.setSoTimeout(200);
            return true;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        }
        return false;

    }
}
