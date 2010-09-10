package jd.controlling.reconnect.plugins.liveheader;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jd.controlling.FavIconController;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterUtils;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Hash;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

public class RouterSender {
    private static final RouterSender INSTANCE           = new RouterSender();
    private static final String       ROUTER_COL_SERVICE = "http://localhost:8081";

    public static RouterSender getInstance() {
        return RouterSender.INSTANCE;
    }

    private static String getManufactor(String mc) {
        if (mc == null) { return null; }
        // do not use IO.readFile to save mem
        mc = mc.substring(0, 6);
        BufferedReader f = null;
        InputStreamReader isr = null;
        FileInputStream fis = null;
        try {
            f = new BufferedReader(isr = new InputStreamReader(fis = new FileInputStream(JDUtilities.getResourceFile("jd/router/manlist.txt")), "UTF8"));
            String line;

            while ((line = f.readLine()) != null) {
                if (line.startsWith(mc)) { return line.substring(7); }
            }

        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                f.close();
            } catch (final Throwable e) {
            }
            try {
                isr.close();
            } catch (final Throwable e) {
            }
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    public static void main(final String[] args) throws IOException, InterruptedException {

        final String mc = RouterUtils.getMacAddress("192.168.178.1");
        System.out.println(RouterSender.getManufactor(mc));
        // final Browser br = new Browser();
        // br.getPage("http://standards.ieee.org/regauth/oui/oui.txt");
        // for (final String[] match :
        // br.getRegex("([0-9A-F]+)\\s+\\(base 16\\)\\s+([^\n]+)").getMatches())
        // {
        // System.out.println(match[0] + "=" + match[1]);
        // }

    }

    private String                  routerIP;
    private String                  script;
    private String                  routerName;

    private String                  mac;
    private String                  manufactor;
    private int                     responseCode;
    private HashMap<String, String> responseHeaders;

    private String                  title;

    private int                     pTagsCount;

    private int                     frameTagCount;

    private String                  favIconHash;

    private RouterSender() {

    }

    private void collectData() {
        this.routerName = this.trim(this.getPlugin().getRouterName());
        this.routerIP = this.trim(this.getPlugin().getRouterIP());
        this.script = this.trim(this.getPlugin().getScript());
        final String userName = this.trim(this.getPlugin().getUser());
        final String password = this.trim(this.getPlugin().getPassword());
        if (userName != null && userName.length() > 2) {
            this.script = Pattern.compile(Pattern.quote(userName), Pattern.CASE_INSENSITIVE).matcher(this.script).replaceAll("%%%user%%%");
        }
        if (password != null && password.length() > 2) {
            this.script = Pattern.compile(Pattern.quote(password), Pattern.CASE_INSENSITIVE).matcher(this.script).replaceAll("%%%pass%%%");
        }

        try {
            this.mac = RouterUtils.getMacAddress(this.routerIP);
            this.manufactor = RouterSender.getManufactor(this.mac);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final Browser br = new Browser();
        try {
            br.getPage("http://" + this.routerIP);
            final URLConnectionAdapter con = br.getHttpConnection();
            this.responseHeaders = new HashMap<String, String>();
            Entry<String, List<String>> next;
            for (final Iterator<Entry<String, List<String>>> it = con.getHeaderFields().entrySet().iterator(); it.hasNext();) {
                next = it.next();

                for (final String value : next.getValue()) {

                    this.responseHeaders.put(next.getKey().toLowerCase(), value);
                }
            }
            this.title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            this.pTagsCount = br.toString().split("<p>").length;
            this.frameTagCount = br.toString().split("<frame").length;
            // get favicon and build hash
            final BufferedImage image = FavIconController.getInstance().downloadFavIcon(this.routerIP);
            final File imageFile = JDUtilities.getResourceFile("tmp/routerfav.png", true);
            imageFile.delete();
            imageFile.deleteOnExit();
            ImageIO.write(image, "png", imageFile);
            this.favIconHash = Hash.getMD5(imageFile);

            if (image != null) {
                this.responseCode = con.getResponseCode();
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public String getFavIconHash() {
        return this.favIconHash;
    }

    public int getFrameTagCount() {
        return this.frameTagCount;
    }

    public String getMac() {
        return this.mac;
    }

    public String getManufactor() {
        return this.manufactor;
    }

    private LiveHeaderReconnect getPlugin() {
        return (LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID);
    }

    public int getpTagsCount() {
        return this.pTagsCount;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public HashMap<String, String> getResponseHeaders() {
        return this.responseHeaders;
    }

    public String getRouterIP() {
        return this.routerIP;
    }

    public String getRouterName() {
        return this.routerName;
    }

    public String getScript() {
        return this.script;
    }

    public String getTitle() {
        return this.title;
    }

    public void run() throws JsonGenerationException, JsonMappingException, IOException {
        this.collectData();

        final String dataString = JSonStorage.toString(this);

        final Browser br = new Browser();
        br.forceDebug(true);
        final String data = URLEncoder.encode(dataString, "UTF-8");
        URLDecoder.decode(data.trim(), "UTF-8");
        br.postPage(RouterSender.ROUTER_COL_SERVICE, "action=add&data=" + data);

    }

    public void setFavIconHash(final String favIconHash) {
        this.favIconHash = favIconHash;
    }

    public void setFrameTagCount(final int frameTagCount) {
        this.frameTagCount = frameTagCount;
    }

    public void setMac(final String mac) {
        this.mac = mac;
    }

    public void setManufactor(final String manufactor) {
        this.manufactor = manufactor;
    }

    public void setpTagsCount(final int pTagsCount) {
        this.pTagsCount = pTagsCount;
    }

    public void setResponseCode(final int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseHeaders(final HashMap<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setRouterIP(final String routerIP) {
        this.routerIP = routerIP;
    }

    public void setRouterName(final String routerName) {
        this.routerName = routerName;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    private String trim(final String stringToTrim) {

        return stringToTrim == null ? null : stringToTrim.trim();
    }

}
