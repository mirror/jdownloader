package jd.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Vector;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class GetRouterInfo {
    public String password = null;
    public String username = null;
    public String adress = null;
    private Vector<String[]> routerDatas = null;
    private boolean checkport80(String host) {
        Socket sock;
        try {
            sock = new Socket(host, 80);
            sock.setSoTimeout(20);
            return true;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        }
        return false;

    }
    public String getAdress() {
        if (adress != null)
            return adress;
        String[] hosts = new String[]{"192.168.2.1", "192.168.1.1", "192.168.0.1", "fritz.box"};
        for (int i = 0; i < hosts.length; i++) {
            try {
                if (InetAddress.getByName(hosts[i]).isReachable(2)) {
                    if (checkport80(hosts[i])) {
                        adress = hosts[i];
                        return hosts[i];
                    }
                }

            } catch (IOException e) {
            }
        }
        String ip = null;
        try {
            InetAddress myAddr = InetAddress.getLocalHost();
            ip = myAddr.getHostAddress();
        } catch (UnknownHostException exc) {
        }
        if (ip != null) {
            String host = ip.replace("\\.[\\d]+$", ".");
            for (int i = 1; i < 255; i++) {
                try {
                    String lhost = host + i;
                    if (!lhost.equals(ip) && InetAddress.getByName(host).isReachable(2)) {
                        if (checkport80(host)) {
                            adress = host;
                            return host;
                        }
                    }

                } catch (IOException e) {
                }
            }
        }
        return null;

    }
    public Vector<String[]> getRouterDatas() {
        if (routerDatas != null)
            return routerDatas;
        if (getAdress() == null)
            return null;
        try {
            RequestInfo request = Plugin.getRequest(new URL("http://" + adress));
            String html = request.getHtmlCode().toLowerCase();
            Vector<String[]> routerData = new HTTPLiveHeader().getLHScripts();
            Vector<String[]> retRouterData = new Vector<String[]>();
            for (int i = 0; i < routerData.size(); i++) {
                String[] dat = routerData.get(i);
                if (html.matches(dat[3]))
                    retRouterData.add(dat);
            }
            routerDatas = retRouterData;
            return retRouterData;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }
    private boolean isEmpty(String arg)
    {
        if(arg==null || arg.matches("[\\s]*"))
            return true;
        return false;
        
    }
    public String[] getRouterData() {
        if(getRouterDatas()==null)
        return null;
        int retries = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_HTTPSEND_RETRIES, 5);
        int wipchange = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 20);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 10);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_USER, username);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_PASS, password);
        for (int i = 0; i < routerDatas.size(); i++) {
            String[] data = routerDatas.get(i);
            if(isEmpty(username))
            {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_USER, data[4]);
            }
            else
            {
                data[4]=username;
            }
            if(isEmpty(password))
            {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_PASS, data[5]);
            }
            else
            {
                data[5]=password;
            }
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, data[2]);
            JDUtilities.saveConfig();
            if (JDUtilities.getController().reconnect()) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, retries);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, wipchange);
                JDUtilities.saveConfig();
                return data;
            }
        }
        return null;
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        GetRouterInfo router = new GetRouterInfo();
        System.out.println(router.getAdress());
        String[] retRouterDatas = router.getRouterData();
        if(retRouterDatas!=null)
        {
           for (int i = 0; i < retRouterDatas.length; i++) {
            System.out.println(retRouterDatas[i]);
        }
        }
    }

}
