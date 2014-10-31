//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import jd.PluginWrapper;
import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.GifDecoder;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.Colors;
import jd.nutils.JDHash;
import jd.nutils.Screen;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.seamless.util.io.IO;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+" }, flags = { 0 })
public class LnkCrptWs extends PluginForDecrypt {

    private static ReentrantLock LOCKDIALOG = new ReentrantLock();

    public static class AdsCaptcha {
        private final Browser br;
        public Browser        acBr;
        private String        challenge;
        private String        publicKey;
        private String        captchaAddress;
        private String        captchaId;
        private String        result;
        private int           count = -1;

        public AdsCaptcha(final Browser br) {
            this.br = br;
        }

        public Form getResult() throws Exception {
            try {
                load();
            } catch (final Throwable e) {
                e.printStackTrace();
                throw new PluginException(LinkStatus.ERROR_FATAL, e.getMessage());
            } finally {
                try {
                    acBr.getHttpConnection().disconnect();
                } catch (final Throwable e) {
                }
            }
            Form ret = new Form();
            if (result == null) {
                return null;
            }
            ret.put("aera", result);
            ret.put("adscaptcha_challenge_field", challenge);
            ret.put("adscaptcha_response_field", result);
            return ret;
        }

        private boolean isStableEnviroment() {
            String prev = JDUtilities.getRevision();
            if (prev == null || prev.length() < 3) {
                prev = "0";
            } else {
                prev = prev.replaceAll(",|\\.", "");
            }
            final int rev = Integer.parseInt(prev);
            if (rev < 10000) {
                return true;
            }
            return false;
        }

        private void load() throws Exception {
            acBr = br.cloneBrowser();
            if (!checkIfSupported()) {
                throw new Exception("AdsCaptcha: Captcha type not supported!");
            }
            acBr.getPage(captchaAddress);
            getChallenge();
            getPublicKey();
            getImageCount();
            if (challenge == null || publicKey == null) {
                throw new Exception("AdsCaptcha: challenge and/or publickey equal null!");
            }

            if (!isStableEnviroment()) {
                final URL[] images = imageUrls();
                if (count <= 0 && images.length == 1) {
                    throw new Exception("AdsCaptcha modul broken!");
                }
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        SliderCaptchaDialog sc = new SliderCaptchaDialog(0, "AdsCaptcha - " + br.getHost(), images, count);
                        sc.displayDialog();
                        result = sc.getReturnValue();
                    }
                });

            } else {
                throw new Exception("AdsCaptcha: currently not available in JD1!");
            }
        }

        private void getChallenge() {
            challenge = acBr.getRegex("\"challenge\":\"([0-9a-f\\-]+)\"").getMatch(0);
        }

        private void getPublicKey() {
            publicKey = acBr.getRegex("\"publicKey\":\"([0-9a-f\\-]+)\"").getMatch(0);
            if (publicKey == null) {
                publicKey = new Regex(captchaAddress, "PublicKey=([0-9a-f\\-]+)\\&").getMatch(0);
            }
        }

        private void getImageCount() {
            String c = acBr.getRegex("\"count\":\"?(\\d+)\"?").getMatch(0);
            if (c != null) {
                count = Integer.parseInt(c);
            }
        }

        private boolean checkIfSupported() throws Exception {
            captchaAddress = acBr.getRegex("src=\'(http://api\\.adscaptcha\\.com/Get\\.aspx\\?CaptchaId=\\d+\\&PublicKey=[^\'<>]+)").getMatch(0);
            captchaId = new Regex(captchaAddress, "CaptchaId=(\\d+)\\&").getMatch(0);
            if (captchaAddress == null || captchaId == null) {
                throw new Exception("AdsCaptcha: Captcha address not found!");
            }
            if (!"3671".equals(captchaId)) {
                return false;
            }
            return true;
        }

        private URL[] imageUrls() throws Exception {
            acBr.getPage("http://api.minteye.com/Slider/SliderData.ashx?cid=" + challenge + "&CaptchaId=" + captchaId + "&PublicKey=" + publicKey + "&w=180&h=150");
            String urls[] = acBr.getRegex("\\{\'src\':\\s\'(https?://[^\']+)\'\\}").getColumn(0);
            if (urls == null || urls.length == 0) {
                urls = acBr.getRegex("\\{\'src\':\\s\'(//[^\']+)\'\\}").getColumn(0);
            }
            if (urls == null || urls.length == 0) {
                urls = acBr.getRegex("(\'|\")spriteUrl(\'|\"):\\s*(\'|\")(.*?)(\'|\")").getColumn(3);
            }
            if (urls == null || urls.length == 0) {
                throw new Exception("AdsCaptcha: Image urls not found!");
            }
            URL out[] = new URL[urls.length];
            int i = 0;
            for (String u : urls) {
                if (u.startsWith("//")) {
                    u = "http:" + u;
                }
                out[i++] = new URL(u);
            }
            return out;
        }

        public String getChallengeId() {
            return challenge;
        }

        public String getCaptchaUrl() {
            return captchaAddress;
        }

        public String getResultValue() {
            return result;
        }

    }

    private static class SliderCaptchaDialog extends AbstractDialog<String> {
        private JSlider       slider;
        private JPanel        p;
        private int           images          = -1;
        private URL           imageUrls[];
        private int           pos             = 0;
        private JPanel        picture;
        private BufferedImage image[];
        private Image         finalImage;
        private Thread        download;
        private JLabel        dLabel;
        private JLabel        cLabel;
        private JProgressBar  bar;
        private final JButton dynamicOkButton = new JButton(_AWU.T.ABSTRACTDIALOG_BUTTON_OK());

        public SliderCaptchaDialog(int flag, String title, URL[] imageUrls, int count) {
            super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, title, null, null, null);
            setCountdownTime(120);
            this.images = imageUrls.length - 1;
            this.imageUrls = imageUrls;
            if (images == 0) {
                images = count--;
            }
        }

        @Override
        public JComponent layoutDialogContent() {
            bar = new JProgressBar(0, images);
            dLabel = new JLabel("Please wait while downloading captchas: " + bar.getValue() + "/" + images);
            cLabel = new JLabel("Slide to fit");

            download = new Thread("Captcha download") {
                public void run() {
                    InputStream stream = null;
                    try {
                        if (images < 0) {
                            image = new BufferedImage[imageUrls.length];
                            for (int i = 0; i < image.length; i++) {
                                sleep(50);
                                try {
                                    image[i] = ImageIO.read(stream = imageUrls[i].openStream());
                                    bar.setValue(i + 1);
                                } finally {
                                    try {
                                        stream.close();
                                    } catch (final Throwable e) {
                                    }
                                }
                            }
                        } else {
                            image = new BufferedImage[images];
                            try {
                                BufferedImage tmpImage = ImageIO.read(stream = imageUrls[0].openStream());
                                int w = tmpImage.getWidth();
                                int h = tmpImage.getHeight() / images;
                                for (int i = 0; i < image.length; i++) {
                                    image[i] = tmpImage.getSubimage(0, i * h, w, h);
                                    bar.setValue(i + 1);
                                }
                            } finally {
                                try {
                                    stream.close();
                                } catch (final Throwable e) {
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.exception(e);
                    } catch (InterruptedException e) {
                        Log.exception(e);
                    }
                }
            };
            download.start();

            slider = new JSlider(0, images, 0);
            slider.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    pos = ((JSlider) e.getSource()).getValue();
                    resizeImage();
                    p.repaint();
                }
            });

            /* setup panels */
            p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
            picture = new JPanel() {
                /**
                 *
                 */
                private static final long serialVersionUID = 1L;

                @Override
                public Dimension getPreferredSize() {
                    if (finalImage != null) {
                        return new Dimension(finalImage.getWidth(dialog), finalImage.getHeight(dialog));
                    } else {
                        return super.getPreferredSize();
                    }
                }

                @Override
                protected void paintComponent(final Graphics g) {
                    super.paintComponent(g);
                    if (finalImage != null) {
                        g.setColor(Color.WHITE);
                        g.drawImage(finalImage, 0, 0, this);
                    }
                }
            };

            bar.setStringPainted(true);
            bar.addChangeListener(new ChangeListener() {
                private int pos = 0;

                @Override
                public void stateChanged(ChangeEvent e) {
                    pos = ((JProgressBar) e.getSource()).getValue();
                    if (pos < images) {
                        dLabel.setText("Please wait while downloading captchas: " + bar.getValue() + "/" + images);
                        dLabel.paintImmediately(dLabel.getVisibleRect());
                    } else {
                        resizeImage();
                        dialog.setSize(315, 350);
                        if (JDGui.getInstance() == null) {
                            dialog.setLocation(Screen.getCenterOfComponent(null, dialog));
                        } else if (JDGui.getInstance().getMainFrame().getExtendedState() == 1 || !JDGui.getInstance().getMainFrame().isVisible()) {
                            dialog.setLocation(Screen.getDockBottomRight(dialog));
                        } else {
                            dialog.setLocation(Screen.getCenterOfComponent(JDGui.getInstance().getMainFrame(), dialog));
                        }
                        setupCaptchaDialog();
                    }
                }

            });

            p.add(dLabel);
            p.add(bar);

            return p;
        }

        private void setupCaptchaDialog() {
            p.remove(dLabel);
            p.remove(bar);
            p.add(cLabel);
            p.add(picture);
            p.add(slider);
            p.repaint();
        }

        private void resizeImage() {
            if (image == null || image.length == 0) {
                finalImage = null;
            } else {
                finalImage = image[pos].getScaledInstance(300, 250, Image.SCALE_SMOOTH);
            }
        }

        @Override
        protected void addButtons(final JPanel buttonBar) {
            dynamicOkButton.addActionListener(this);
            p.addContainerListener(new ContainerListener() {

                @Override
                public void componentAdded(ContainerEvent e) {
                }

                @Override
                public void componentRemoved(ContainerEvent e) {
                    if (e.getChild().getClass().getName().endsWith("JProgressBar")) {
                        buttonBar.add(dynamicOkButton, "cell 0 0,tag ok,sizegroup confirms");
                    }
                }
            });

        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == dynamicOkButton) {
                Log.L.fine("Answer: Button<OK:" + dynamicOkButton.getText() + ">");
                setReturnmask(true);
            } else if (e.getActionCommand().equals("enterPushed")) {
                return;
            }
            super.actionPerformed(e);
        }

        @Override
        protected String createReturnValue() {
            if (!download.isInterrupted()) {
                download.interrupt();
            }
            if (Dialog.isOK(getReturnmask())) {
                return String.valueOf(pos);
            }
            return null;
        }

    }

    public static class SolveMedia {
        private final Browser      br;
        private String             challenge;
        private String             chId;
        private String             captchaAddress;
        private String             server;
        private String             path;
        public static final String FAIL_CAUSE_CKEY_MISSING = "SolveMedia Module fails --> Probably a host side bug/wrong key";
        private Form               verify;
        private boolean            secure                  = false;
        private boolean            noscript                = true;
        private boolean            clearReferer            = true;
        public Browser             smBr;

        public SolveMedia(final Browser br) {
            this.br = br;
        }

        public File downloadCaptcha(final File captchaFile) throws Exception {
            load();
            URLConnectionAdapter con = null;
            try {
                captchaAddress = captchaAddress.replaceAll("%0D%0A", "").trim();
                Browser.download(captchaFile, con = smBr.openGetConnection(server + captchaAddress));
            } catch (IOException e) {
                captchaFile.delete();
                throw e;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            return captchaFile;
        }

        private void load() throws Exception {
            smBr = br.cloneBrowser();
            // solvemedia works off API key, and javascript. The imported browser session isn't actually needed.
            /*
             * Randomise user-agent to prevent tracking by solvemedia, each time we load(). Without this they could make the captchas images
             * harder read, the more a user requests captcha'. Also algos could track captcha requests based on user-agent globally, which
             * means JD default user-agent been very old (firefox 3.x) negatively biased to JD clients! Tracking takes place on based on IP
             * address, User-Agent, and APIKey of request (site of APIKey), cookies session submitted, and combinations of those.
             * Effectively this can all be done with a new browser, with regex tasks from source browser (ids|keys|submitting forms).
             */
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            smBr.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());

            // this prevents solvemedia group from seeing referrer
            try {
                if (clearReferer) {
                    smBr.setCurrentURL(null);
                }
            } catch (final Throwable e) {
                /* 09581 will break here */
            }
            // end of privacy protection

            if (this.challenge == null) {
                getChallengeKey();
            }
            setServer();
            setPath();
            if (!smBr.getURL().contains("solvemedia.com/")) {
                // when we retry solving a solvemedia session, we reuse smBr, browser already contains the info we need!
                smBr.getPage(server + path + challenge);
            }
            if (smBr.containsHTML(">error: domain / ckey mismatch")) {
                throw new Exception(FAIL_CAUSE_CKEY_MISSING);
            }
            if (noscript) {
                verify = smBr.getForm(0);
                captchaAddress = smBr.getRegex("<img src=\"(/papi/media\\?c=[^\"]+)").getMatch(0);
                if (captchaAddress == null) {
                    captchaAddress = smBr.getRegex("src=\"(/papi/media\\?c=[^\"]+)").getMatch(0);
                }
                if (verify == null) {
                    throw new Exception("SolveMedia Module fails");
                }
            } else {
                chId = smBr.getRegex("\"chid\"\\s+?:\\s+?\"(.*?)\",").getMatch(0);
                captchaAddress = chId != null ? server + "/papi/media?c=" + chId : null;
            }
            if (captchaAddress == null) {
                throw new Exception("SolveMedia Module fails");
            }
        }

        private void getChallengeKey() {
            challenge = br.getRegex("http://api\\.solvemedia\\.com/papi/_?challenge\\.script\\?k=(.{32})").getMatch(0);
            if (challenge == null) {
                // when we retry solving a solvemedia session.
                challenge = smBr.getRegex("<input type=hidden name=\"k\" value=\"([^\"]+)\">").getMatch(0);
            }
            if (challenge == null) {
                secure = true;
                challenge = br.getRegex("ckey:\'([\\w\\-\\.]+)\'").getMatch(0);
                if (challenge == null) {
                    challenge = br.getRegex("https://api\\-secure\\.solvemedia\\.com/papi/_?challenge\\.script\\?k=(.{32})").getMatch(0);
                }
                if (challenge == null) {
                    secure = false;
                }
            }
        }

        public String getChallenge() {
            if (captchaAddress == null) {
                return null;
            }
            return new Regex(captchaAddress, "/papi/media\\?c=(.*?)$").getMatch(0);
        }

        public String getChallenge(final String code) throws Exception {
            if (!noscript) {
                return chId;
            }

            /** FIXME stable Browser Bug --> Form action handling */
            String url = smBr.getURL();
            url = url.substring(0, url.indexOf("media?c="));
            verify.setAction((url == null ? "" : url) + verify.getAction());

            verify.put("adcopy_response", Encoding.urlEncode(code));
            smBr.submitForm(verify);
            String verifyUrl = smBr.getRegex("URL=(http[^\"]+)").getMatch(0);
            if (verifyUrl == null) {
                return null;
            }
            if (secure) {
                verifyUrl = verifyUrl.replaceAll("http://", "https://");
            }
            try {
                smBr.getPage(verifyUrl);
            } catch (Throwable e) {
                throw new Exception("SolveMedia Module fails");
            }
            return smBr.getRegex("id=gibberish>([^<]+)").getMatch(0);
        }

        public Browser getBr() {
            return smBr;
        }

        /**
         * @default false
         * @parameter if true uses "https://api-secure.solvemedia.com" instead of "http://api.solvemedia.com"
         */
        public void setSecure(boolean secure) {
            if (secure) {
                secure = true;
            }
        }

        /**
         * @default true
         * @parameter if false uses "_challenge.js" instead of "challenge.noscript" as url path
         */
        public void setNoscript(boolean noscript) {
            if (!noscript) {
                noscript = false;
            }
        }

        private void setServer() {
            server = "http://api.solvemedia.com";
            if (secure) {
                server = "https://api-secure.solvemedia.com";
            }
        }

        private void setPath() {
            path = "/papi/challenge.noscript?k=";
            if (!noscript) {
                path = "/papi/_challenge.js?k=";
            }
        }

        public void setChallengeKey(final String challengeKey) {
            this.challenge = challengeKey;
        }

        public String getChallengeUrl() {
            return server + path;
        }

        public String getChallengeId() {
            return challenge;
        }

        public String getCaptchaUrl() {
            return captchaAddress;
        }

        public void setClearReferer(final boolean clearReferer) {
            this.clearReferer = clearReferer;
        }

    }

    public static class KeyCaptcha {
        private static Object LOCK = new Object();

        public static void prepareBrowser(final Browser kc, final String a) {
            kc.getHeaders().put("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
            kc.getHeaders().put("Referer", DLURL);
            kc.getHeaders().put("Pragma", null);
            kc.getHeaders().put("Cache-Control", null);
            kc.getHeaders().put("Accept", a);
            kc.getHeaders().put("Accept-Charset", null);
            kc.getHeaders().put("Accept-Language", "en-EN");
            kc.getHeaders().put("Cache-Control", null);
        }

        private final Browser                br;
        private Form                         FORM;
        private HashMap<String, String>      PARAMS;
        private Browser                      rcBr;
        private String                       SERVERSTRING;
        private static String                DLURL;
        private String[]                     stImgs;
        private String[]                     sscStc;
        private LinkedHashMap<String, int[]> fmsImg;

        public KeyCaptcha(final Browser br) {
            this.br = br;
        }

        private String getGjsParameter() {
            final String[] pars = { "s_s_c_user_id", "src", "s_s_c_session_id", "s_s_c_captcha_field_id", "s_s_c_submit_button_id", "s_s_c_web_server_sign", "s_s_c_web_server_sign2", "s_s_c_web_server_sign3", "s_s_c_web_server_sign4" };
            String result = "";
            for (final String key : pars) {
                result = result != "" ? result + "|" : result;
                if (PARAMS.containsKey(key)) {
                    result += PARAMS.get(key);
                }
            }
            return result;
        }

        private boolean isStableEnviroment() {
            String prev = JDUtilities.getRevision();
            if (prev == null || prev.length() < 3) {
                prev = "0";
            } else {
                prev = prev.replaceAll(",|\\.", "");
            }
            final int rev = Integer.parseInt(prev);
            if (rev < 10000) {
                return true;
            }
            return false;
        }

        private String getAdditionalQuery(String query) {
            ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
            ScriptEngine engine = manager.getEngineByName("javascript");
            String js = rcBr.toString();
            String doc = new Regex(query, "(document.*?)$").getMatch(0);
            js = js.replaceAll(doc + "=s_s_c_web_server_sign4;", "");
            query = query.replaceAll("\\+" + doc, "");
            try {
                engine.eval("var document = new Object();");
                engine.eval(js);
            } catch (final Throwable e) {
                /* ignore rhino Exceptions */
                try {
                    // query = engine.eval(query).toString() + new Regex(js, doc + "=\"([^\"]+)").getMatch(0);
                    query = new Regex(js, doc + "=\"([^\"]+)").getMatch(0);
                } catch (Throwable e1) {
                }
            }
            return "|1|0|" + query;
        }

        private void makeFirstRequest() {
            ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
            ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                /* creating pseudo functions: document.location */
                engine.eval("var document = { loc : function() { return \"" + DLURL + "\";}}");
                engine.eval("document.location = document.loc();");
                engine.put("s_s_c_user_id", PARAMS.get("s_s_c_user_id"));
                engine.eval(SERVERSTRING);
                SERVERSTRING = engine.get("_13").toString();
            } catch (final Throwable e) {
                SERVERSTRING = null;
            }
        }

        private void load() throws Exception {
            rcBr = br.cloneBrowser();
            rcBr.setFollowRedirects(true);
            String additionalQuery = null;
            prepareBrowser(rcBr, "application/javascript, */*;q=0.8");

            if (PARAMS.containsKey("src")) {
                rcBr.getPage(PARAMS.get("src"));
                PARAMS.put("src", DLURL);
                SERVERSTRING = rcBr.getRegex("(var _13=[^;]+;)").getMatch(0);
                makeFirstRequest();
            }
            if (SERVERSTRING != null && SERVERSTRING.startsWith("https")) {
                rcBr.getPage(SERVERSTRING);
                SERVERSTRING = null;
                PARAMS.put("s_s_c_web_server_sign4", rcBr.getRegex("s_s_c_web_server_sign4=\"(.*?)\"").getMatch(0));
                String[] next = rcBr.getRegex("\\.setAttribute\\(\"src\",\"(.*?)\"\\+(.*?)\\+").getRow(0);
                if (next == null) {
                    throw new Exception("KeyCaptcha Module fails");
                }
                SERVERSTRING = next[0];

                /* a little bit js processing */
                additionalQuery = getAdditionalQuery(rcBr.getRegex("var " + next[1] + "=.*?s_s_c_web_server_sign4\\+(.*?);").getMatch(0));

            }
            if (SERVERSTRING != null && SERVERSTRING.startsWith("https")) {
                rcBr.clearCookies(rcBr.getHost());
                SERVERSTRING = SERVERSTRING + Encoding.urlEncode(getGjsParameter() + additionalQuery) + "&r=" + Math.random();
                rcBr.getPage(SERVERSTRING);
                additionalQuery = additionalQuery.substring(0, additionalQuery.lastIndexOf("|"));
                PARAMS.put("s_s_c_web_server_sign3", rcBr.getRegex("s_s_c_setnewws\\(\"(.*?)\",").getMatch(0));
                stImgs = rcBr.getRegex("\\(\'([0-9a-f]+)\',\'(http.*?\\.png)\',(.*?),(true|false)\\)").getRow(0);
                sscStc = rcBr.getRegex("\\(\'([0-9a-f]+)\',\'(http.*?\\.png)\',(.*?),(true|false)\\)").getRow(1);

                String signFour = PARAMS.get("s_s_c_web_server_sign4");
                if (signFour.length() < 33) {
                    signFour = signFour.substring(0, 10) + "378" + signFour.substring(10);
                    PARAMS.put("s_s_c_web_server_sign4", signFour);
                }
                SERVERSTRING = rcBr.getRegex("\\.s_s_c_resurl=\'([^\']+)\'\\+").getMatch(0);
                if (stImgs == null || sscStc == null || SERVERSTRING == null) {
                    throw new Exception("KeyCaptcha Module fails");
                }
                SERVERSTRING += Encoding.urlEncode(getGjsParameter() + additionalQuery);
            } else {
                throw new Exception("KeyCaptcha Module fails");
            }
        }

        private void parse() throws Exception {
            FORM = null;
            if (br.containsHTML("(KeyCAPTCHA|www\\.keycaptcha\\.com)")) {
                for (final Form f : br.getForms()) {
                    if (f.containsHTML("var s_s_c_user_id = ('\\d+'|\"\\d+\")")) {
                        FORM = f;
                        break;
                    }
                }
                if (FORM == null) {
                    String st = br.getRegex("(<script type=\'text/javascript\'>var s_s_c_.*?\'></script>)").getMatch(0);
                    if (st != null) {
                        Browser f = br.cloneBrowser();
                        FORM = new Form();
                        f.getRequest().setHtmlCode("<form>" + st + "</form>");
                        FORM = f.getForm(0);
                    }
                }
                if (FORM == null) {
                    throw new Exception("KeyCaptcha form couldn't be found");
                } else {
                    PARAMS = new HashMap<String, String>();
                    String[][] parameter = FORM.getRegex("(s_s_c_\\w+) = \'(.*?)\'").getMatches();
                    if (parameter == null || parameter.length == 0) {
                        parameter = FORM.getRegex("(s_s_c_\\w+) = \"(.*?)\"").getMatches();
                    }
                    for (final String[] para : parameter) {
                        if (para.length != 2) {
                            continue;
                        }
                        PARAMS.put(para[0], para[1]);
                    }
                    if (PARAMS == null || PARAMS.size() == 0) {
                        throw new Exception("KeyCaptcha values couldn't be found");
                    } else {
                        String src = FORM.getRegex("src=\'([^']+keycaptcha\\.com[^']+)\'").getMatch(0);
                        if (src == null) {
                            src = FORM.getRegex("src=\"([^\"]+keycaptcha\\.com[^\"]+)\"").getMatch(0);
                            if (src == null) {
                                throw new Exception("KeyCaptcha Module fails");
                            }
                        }
                        PARAMS.put("src", src);
                    }
                }
            } else {
                throw new Exception("KeyCaptcha handling couldn't be found");
            }
        }

        /**
         * Handles a KeyCaptcha by trying to autosolve it first and use a dialog as fallback
         * 
         * @param parameter
         *            the keycaptcha parameter as already used for showDialog
         * @param downloadLink
         *            downloadlink (for counting attempts)
         * @return
         * @throws Exception
         */
        public String handleKeyCaptcha(final String parameter, DownloadLink downloadLink) throws Exception {
            int attempt = downloadLink.getIntegerProperty("KEYCAPTCHA_ATTEMPT", 0);
            downloadLink.setProperty("KEYCAPTCHA_ATTEMPT", attempt + 1);
            if (attempt < 2) {
                // less than x attempts -> try autosolve
                return autoSolve(parameter);
            } else {
                // shows the dialog
                return showDialog(parameter);
            }
        }

        /**
         * This methods just displays a dialog. You can use {@link #handleKeyCaptcha(String, DownloadLink) handleKeyCaptcha} instead, which
         * tries to autosolve it first. Or you can use {@link #autoSolve(String) autosolve}, which tries to solve the captcha directly.
         */
        public synchronized String showDialog(final String parameter) throws Exception {
            LOCKDIALOG.lock();
            try {
                DLURL = parameter;
                try {
                    parse();
                    load();
                } catch (final Throwable e) {
                    e.printStackTrace();
                    throw new Exception(e.getMessage());
                } finally {
                    try {
                        rcBr.getHttpConnection().disconnect();
                    } catch (final Throwable e) {
                    }
                }

                /* Bilderdownload und Verarbeitung */
                sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));// fragmentierte Puzzleteile
                sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));// fragmentiertes Hintergrundbild

                if (sscStc == null || sscStc.length == 0 || stImgs == null || stImgs.length == 0 || fmsImg == null || fmsImg.size() == 0) {
                    return "CANCEL";
                }

                String out = null;
                ArrayList<Integer> marray = new ArrayList<Integer>();

                final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + Encoding.Base64Decode("S2hkMjFNNDc="));
                String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
                mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();

                if (!isStableEnviroment()) {
                    final KeyCaptchaDialog vC = new KeyCaptchaDialog(0, "KeyCaptcha - " + br.getHost(), new String[] { stImgs[1], sscStc[1] }, fmsImg, null, rcBr, mmUrlReq);

                    // avoid imports here
                    jd.gui.swing.dialog.AbstractCaptchaDialog.playCaptchaSound();
                    try {
                        out = org.appwork.utils.swing.dialog.Dialog.getInstance().showDialog(vC);
                    } catch (final Throwable e) {
                        out = null;
                    }
                    if (out == null) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    if (vC.getReturnmask() == 4) {
                        out = "CANCEL";
                    }
                    marray.addAll(vC.mouseArray);

                } else {
                    final KeyCaptchaDialogForStable vC = new KeyCaptchaDialogForStable("KeyCaptcha - " + br.getHost(), new String[] { stImgs[1], sscStc[1] }, fmsImg, rcBr, mmUrlReq);
                    // Warten bis der KeyCaptcha-Dialog geschlossen ist
                    synchronized (LOCK) {
                        try {
                            LOCK.wait();
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    out = vC.POSITION;
                }
                if (out == null) {
                    return null;
                }
                if ("CANCEL".equals(out)) {
                    System.out.println("KeyCaptcha: User aborted captcha dialog.");
                    return out;
                }

                String key = rcBr.getRegex("\\|([0-9a-zA-Z]+)\'\\.split").getMatch(0);
                if (key == null) {
                    key = Encoding.Base64Decode("OTNodk9FZmhNZGU=");
                }

                String cOut = "";
                for (Integer i : marray) {
                    if (cOut.length() > 1) {
                        cOut += ".";
                    }
                    cOut += String.valueOf(i);
                }

                SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
                rcBr.clearCookies(rcBr.getHost());
                out = rcBr.getPage(SERVERSTRING.substring(0, SERVERSTRING.lastIndexOf("%7C")));
                out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
                // validate final response
                if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) {
                    return null;
                }
                return out;
            } finally {
                LOCKDIALOG.unlock();
            }
        }

        private String sscFsmCheckFour(String arg0, final String arg1) {
            try {
                if (arg0 == null || arg0.length() < 8 || arg1 == null) {
                    return null;
                }
                String prand = "";
                for (int i = 0; i < arg1.length(); i++) {
                    prand += arg1.codePointAt(i);
                }
                final int sPos = (int) Math.floor(prand.length() / 5);
                final int mult = Integer.parseInt(String.valueOf(prand.charAt(sPos) + "" + prand.charAt(sPos * 2) + "" + prand.charAt(sPos * 3) + "" + prand.charAt(sPos * 4) + "" + prand.charAt(sPos * 5 - 1)));
                final int incr = Math.round(arg1.length() / 3);
                final long modu = (int) Math.pow(2, 31);
                final int salt = Integer.parseInt(arg0.substring(arg0.length() - 8, arg0.length()), 16);
                arg0 = arg0.substring(0, arg0.length() - 8);
                prand += salt;
                while (prand.length() > 9) {
                    prand = String.valueOf(Integer.parseInt(prand.substring(0, 9), 10) + Integer.parseInt(prand.substring(9, Math.min(prand.length(), 14)), 10)) + prand.substring(Math.min(prand.length(), 14), prand.length());
                }
                final String[] sburl = "https://back2.keycaptcha.com".split("\\.");
                if (sburl != null && sburl.length == 3 && "keycaptcha".equalsIgnoreCase(sburl[1]) && "com".equalsIgnoreCase(sburl[2])) {
                    prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
                } else {
                    prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu + 294710);
                }
                int enc_chr = 0;
                String enc_str = "";
                for (int i = 0; i < arg0.length(); i += 2) {
                    enc_chr = Integer.parseInt(arg0.substring(i, i + 2), 16) ^ (int) Math.floor(Double.parseDouble(prand) / modu * 255);
                    enc_str += String.valueOf((char) enc_chr);
                    prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
                }
                return enc_str;
            } catch (final Throwable e) {
                return null;
            }
        }

        private String sscFsmCheckTwo(final String arg0, final String arg1) {
            try {
                if (arg1 == null) {
                    return null;
                }
                String prand = "";
                for (int i = 0; i < arg1.length(); i++) {
                    prand += arg1.codePointAt(i);
                }
                final int sPos = (int) Math.floor(prand.length() / 5);
                final int mult = Integer.parseInt(String.valueOf(prand.charAt(sPos) + "" + prand.charAt(sPos * 2) + "" + prand.charAt(sPos * 3) + "" + prand.charAt(sPos * 4) + "" + prand.charAt(sPos * 5 - 1)));
                final int incr = (int) Math.ceil(arg1.length() / 3d);
                final long modu = (int) Math.pow(2, 31);
                if (mult < 2) {
                    return null;
                }
                int salt = (int) Math.round(Math.random() * 1000000000) % 100000000;
                prand += salt;
                while (prand.length() > 9) {
                    prand = String.valueOf(Integer.parseInt(prand.substring(0, 9), 10) + Integer.parseInt(prand.substring(9, Math.min(prand.length(), 14)), 10)) + prand.substring(Math.min(prand.length(), 14), prand.length());
                }
                final String[] sburl = "https://back2.keycaptcha.com".split("\\.");
                if (sburl != null && sburl.length == 3 && "keycaptcha".equalsIgnoreCase(sburl[1]) && "com".equalsIgnoreCase(sburl[2])) {
                    prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
                } else {
                    prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu + 540);
                }
                int enc_chr = 0;
                String enc_str = "";
                for (int i = 0; i < arg0.length(); i++) {
                    enc_chr = arg0.codePointAt(i) ^ (int) Math.floor(Double.parseDouble(prand) / modu * 255);
                    if (enc_chr < 16) {
                        enc_str += "0" + String.valueOf(Integer.toHexString(enc_chr));
                    } else {
                        enc_str += String.valueOf(Integer.toHexString(enc_chr));
                    }
                    prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
                }
                String saltStr = String.valueOf(Integer.toHexString(salt));
                while (saltStr.length() < 8) {
                    saltStr = "0" + saltStr;
                }
                return enc_str + saltStr;
            } catch (final Throwable e) {
                return null;
            }
        }

        private void sscGetImagest(final String arg0, final String arg1, final String arg2, final boolean arg4) {
            final String outst = sscFsmCheckFour(arg0, arg1.substring(arg1.length() - 33, arg1.length() - 6));
            String[] parseOutst;
            int[] pOut = null;
            if (arg4) {
                parseOutst = outst.split(";");
            } else {
                parseOutst = outst.split(",");
                pOut = new int[parseOutst.length];
                for (int i = 0; i < pOut.length; i++) {
                    pOut[i] = Integer.parseInt(parseOutst[i]);
                }
            }
            if (parseOutst != null && parseOutst.length > 0) {
                if (fmsImg == null || fmsImg.size() == 0) {
                    fmsImg = new LinkedHashMap<String, int[]>();
                }
                if (arg4) {
                    for (final String pO : parseOutst) {
                        final String[] tmp = pO.split(":");
                        if (tmp == null || tmp.length == 0) {
                            continue;
                        }
                        final String[] tmpOut = tmp[1].split(",");
                        pOut = new int[tmpOut.length];
                        for (int i = 0; i < pOut.length; i++) {
                            pOut[i] = Integer.parseInt(tmpOut[i]);
                        }
                        fmsImg.put(tmp[0], pOut);
                    }
                } else {
                    fmsImg.put("backGroundImage", pOut);
                }
            }
        }

        // ===== BEGIN autosolve stuff
        public String autoSolve(final String parameter) throws Exception {

            DLURL = parameter;
            try {
                parse();
                load();
            } catch (final Throwable e) {
                e.printStackTrace();
                throw new Exception(e.getMessage());
            } finally {
                try {
                    rcBr.getHttpConnection().disconnect();
                } catch (final Throwable e) {
                }
            }

            /* Bilderdownload und Verarbeitung */
            sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));// fragmentierte Puzzleteile
            sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));// fragmentiertes Hintergrundbild

            if (sscStc == null || sscStc.length == 0 || stImgs == null || stImgs.length == 0 || fmsImg == null || fmsImg.size() == 0) {
                return "CANCEL";
            }

            String out = null;
            ArrayList<Integer> marray = new ArrayList<Integer>();

            final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + Encoding.Base64Decode("S2hkMjFNNDc="));
            String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
            mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();

            KeyCaptchaImageGetter imgGetter = new KeyCaptchaImageGetter(new String[] { stImgs[1], sscStc[1] }, fmsImg, rcBr, mmUrlReq);

            KeyCaptchaSolver kcSolver = new KeyCaptchaSolver();

            rcBr.cloneBrowser().getPage(mmUrlReq);

            out = kcSolver.solve(imgGetter.getKeyCaptchaImage());
            marray.addAll(kcSolver.getMouseArray());
            if (out == null) {
                return null;
            }
            if ("CANCEL".equals(out)) {
                System.out.println("KeyCaptcha: User aborted captcha dialog.");
                return out;
            }

            String key = rcBr.getRegex("\\|([0-9a-zA-Z]+)\'\\.split").getMatch(0);
            if (key == null) {
                key = Encoding.Base64Decode("OTNodk9FZmhNZGU=");
            }

            String cOut = "";
            for (Integer i : marray) {
                if (cOut.length() > 1) {
                    cOut += ".";
                }
                cOut += String.valueOf(i);
            }

            SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
            rcBr.clearCookies(rcBr.getHost());
            out = rcBr.getPage(SERVERSTRING.substring(0, SERVERSTRING.lastIndexOf("%7C")));
            out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
            // validate final response
            if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) {
                return null;
            }
            return out;
        }
    }

    private static class KeyCaptchaDialog extends AbstractDialog<String> implements ActionListener {
        private JLayeredPane                       drawPanel;
        private final LinkedHashMap<String, int[]> coordinates;
        private final String[]                     imageUrl;
        private BufferedImage[]                    kcImages;
        private int                                kcSampleImg;
        private Image[]                            IMAGE;
        private Graphics                           go;
        private JPanel                             p;
        private final Dimension                    dimensions;
        private ArrayList<Integer>                 mouseArray;
        private Browser                            kc;
        private String                             url;

        public KeyCaptchaDialog(final int flag, final String title, final String[] imageUrl, final LinkedHashMap<String, int[]> coordinates, final String cancelOption, Browser br, String url) {
            super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.LOGIC_COUNTDOWN, title, null, null, null);
            setCountdownTime(120);
            this.imageUrl = imageUrl;
            this.coordinates = coordinates;
            dimensions = new Dimension(465, 250);
            this.kc = br.cloneBrowser();
            this.url = url;
        }

        @Override
        protected String createReturnValue() {
            if (Dialog.isOK(getReturnmask())) {
                return getPosition(drawPanel);
            }
            return null;
        }

        private String getPosition(final JLayeredPane drawPanel) {
            int i = 0;
            String positions = "";
            final Component[] comp = drawPanel.getComponents();
            for (int c = comp.length - 1; c >= 0; c--) {
                if (comp[c].getMouseListeners().length == 0) {
                    continue;
                }
                final Point p = comp[c].getLocation();
                positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
                i++;
            }
            return positions;
        }

        @Override
        public Dimension getPreferredSize() {
            return dimensions;
        }

        public void handleCoordinates(final LinkedHashMap<String, int[]> arg0) {
            kcImages = new BufferedImage[coordinates.size()];
        }

        @Override
        public JComponent layoutDialogContent() {
            loadImage(imageUrl);
            // use a container
            p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));

            handleCoordinates(coordinates);
            drawPanel = new JLayeredPane();

            makePieces();
            makeBackground();
            int offset = 4;
            boolean sampleImg = false;
            drawPanel.add(new KeyCaptchaDrawBackgroundPanel(kcImages[0]), new Integer(JLayeredPane.DEFAULT_LAYER), new Integer(JLayeredPane.DEFAULT_LAYER));

            mouseArray = new ArrayList<Integer>();

            for (int i = 1; i < kcImages.length; i++) {
                if (kcImages[i] == null) {
                    continue;
                } else if (i == kcSampleImg) {
                    sampleImg = true;
                } else {
                    sampleImg = false;
                }
                drawPanel.add(new KeyCaptchaDragPieces(kcImages[i], offset, sampleImg, mouseArray, kc, url), new Integer(JLayeredPane.DEFAULT_LAYER) + i, new Integer(JLayeredPane.DEFAULT_LAYER) + i);

                offset += 4;
            }

            p.add(new JLabel("Assemble the image as you see at the upper right corner"));

            p.add(drawPanel);
            return p;
        }

        public void loadImage(final String[] imagesUrl) {
            int i = 0;
            IMAGE = new Image[imagesUrl.length];
            File fragmentedPic;
            final Browser dlpic = new Browser();
            KeyCaptcha.prepareBrowser(dlpic, "image/png,image/*;q=0.8,*/*;q=0.5");
            final MediaTracker mt = new MediaTracker(dialog);
            for (final String imgUrl : imagesUrl) {
                try {
                    fragmentedPic = Application.getResource("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic.deleteOnExit();
                    Browser.download(fragmentedPic, dlpic.openGetConnection(imgUrl));
                    /* TODO: replace with ImageProvider.read in future */
                    IMAGE[i] = ImageIO.read(fragmentedPic);
                    // IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new URL(imgUrl));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                mt.addImage(IMAGE[i], i);
                i++;
            }
            try {
                mt.waitForAll();
            } catch (final InterruptedException ex) {
            }
        }

        private void makeBackground() {
            int curx = 0;
            int cik = 0;
            kcImages[0] = new BufferedImage(450, 160, BufferedImage.TYPE_INT_RGB);
            go = kcImages[0].getGraphics();
            go.setColor(Color.WHITE);
            go.fillRect(0, 0, 450, 160);
            final int[] bgCoord = coordinates.get("backGroundImage");
            while (cik < bgCoord.length) {
                go.drawImage(IMAGE[1], bgCoord[cik], bgCoord[cik + 1], bgCoord[cik] + bgCoord[cik + 2], bgCoord[cik + 1] + bgCoord[cik + 3], curx, 0, curx + bgCoord[cik + 2], bgCoord[cik + 3], dialog);
                curx = curx + bgCoord[cik + 2];
                cik = cik + 4;
            }
        }

        private void makePieces() {
            final Object[] key = coordinates.keySet().toArray();
            int pieces = 1;
            for (final Object element : key) {
                if (element.equals("backGroundImage")) {
                    continue;
                }
                final int[] imgcs = coordinates.get(element);
                if (imgcs == null | imgcs.length == 0) {
                    break;
                }
                final int w = imgcs[1] + imgcs[5] + imgcs[9];
                final int h = imgcs[3] + imgcs[15] + imgcs[27];
                int dX = 0;
                int dY = 0;
                kcImages[pieces] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                go = kcImages[pieces].getGraphics();
                if (element.equals("kc_sample_image")) {
                    kcSampleImg = pieces;
                }
                int sX = 0, sY = 0, sW = 0, sH = 0;
                dX = 0;
                dY = 0;
                for (int cik2 = 0; cik2 < 36; cik2 += 4) {
                    sX = imgcs[cik2];
                    sY = imgcs[cik2 + 2];
                    sW = imgcs[cik2 + 1];
                    sH = imgcs[cik2 + 3];
                    if (sX + sW > IMAGE[0].getWidth(dialog) || sY + sH > IMAGE[0].getHeight(dialog)) {
                        continue;
                    }
                    if (dX + sW > w || dY + sH > h) {
                        continue;
                    }
                    if (sW == 0 || sH == 0) {
                        continue;
                    }
                    // Puzzlebild erstellen
                    go.drawImage(IMAGE[0], dX, dY, dX + sW, dY + sH, sX, sY, sX + sW, sY + sH, dialog);
                    dX = dX + sW;
                    if (dX >= w) {
                        dY = dY + sH;
                        dX = 0;
                    }
                }
                pieces += 1;
            }
        }

    }

    private static class KeyCaptchaDragPieces extends JPanel {
        private static final long   serialVersionUID = 1L;
        private final BufferedImage IMAGE;
        private final MouseAdapter  MOUSEADAPTER;
        private int                 k;

        public KeyCaptchaDragPieces(final BufferedImage image, final int offset, final boolean sampleImg, final ArrayList<Integer> mouseArray, final Browser br, final String url) {
            IMAGE = image;

            MOUSEADAPTER = new MouseInputAdapter() {
                private Point p1;
                private Point loc;

                private Timer mArrayTimer = new Timer(1000, new ActionListener() {
                                              public void actionPerformed(ActionEvent e) {
                                                  marray(loc);
                                              }
                                          });

                @Override
                public void mouseDragged(final MouseEvent e) {
                    Point p2 = e.getPoint();
                    loc = getLocation();
                    loc.translate(p2.x - p1.x, p2.y - p1.y);
                    mArrayTimer.setRepeats(false);
                    mArrayTimer.start();
                    setLocation(loc);
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    p1 = e.getPoint();
                    setBorder(BorderFactory.createLineBorder(Color.black));
                    if (!br.getURL().equals(url)) {
                        new Thread() {
                            public void run() {
                                try {
                                    br.getPage(url);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    p1 = e.getPoint();
                    setBorder(BorderFactory.createEmptyBorder());
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    mArrayTimer.start();
                }

                @Override
                public void mouseMoved(final MouseEvent e) {
                    p1 = e.getPoint();
                    loc = getLocation();
                    loc.translate(p1.x, p1.y);
                    mArrayTimer.setRepeats(false);
                    mArrayTimer.start();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    mArrayTimer.stop();
                }

                private void marray(Point loc) {
                    if (loc != null) {
                        if (mouseArray.size() == 0) {
                            mouseArray.add(loc.x + 465);
                            mouseArray.add(loc.y + 264);
                        }
                        if (mouseArray.get(mouseArray.size() - 2) != loc.x + 465 || mouseArray.get(mouseArray.size() - 1) != loc.y + 264) {
                            mouseArray.add(loc.x + 465);
                            mouseArray.add(loc.y + 264);
                        }
                        if (mouseArray.size() > 40) {
                            ArrayList<Integer> tmpMouseArray = new ArrayList<Integer>();
                            tmpMouseArray.addAll(mouseArray.subList(2, 40));
                            mouseArray.clear();
                            mouseArray.addAll(tmpMouseArray);
                        }
                    }
                }
            };

            k = 0;
            setOpaque(false);
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            if (!sampleImg) {
                setBounds(offset, offset, image.getWidth(), image.getHeight());
                setLocation(offset, offset);
                setBorder(BorderFactory.createLineBorder(Color.black));
                addMouseListener(MOUSEADAPTER);
                addMouseMotionListener(MOUSEADAPTER);
                enableEvents(MouseEvent.MOUSE_EVENT_MASK | MouseEvent.MOUSE_MOTION_EVENT_MASK);
            } else {
                setLayout(null);
                setBounds(449 - image.getWidth() - 10, 0, image.getWidth() + 10, image.getHeight() + 10);
                setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
                setBackground(Color.white);
                k = 5;
            }
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (IMAGE != null) {
                g.drawImage(IMAGE, k, k, this);
            }
        }
    }

    private static class KeyCaptchaDrawBackgroundPanel extends JPanel {
        private static final long   serialVersionUID = 1L;
        private final BufferedImage image;

        public KeyCaptchaDrawBackgroundPanel(final BufferedImage image) {
            this.image = image;
            setOpaque(true);
            setBounds(0, 0, image.getWidth(), image.getHeight());
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        }

        @Override
        public Dimension getPreferredSize() {
            if (image != null) {
                return new Dimension(image.getWidth(), image.getHeight());
            } else {
                return super.getPreferredSize();
            }
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, this);
            }
        }
    }

    private static class KeyCaptchaDialogForStable extends JFrame {
        private static final long                  serialVersionUID = 1L;
        private Image[]                            IMAGE;
        private BufferedImage[]                    kcImages;
        private final LinkedHashMap<String, int[]> coordinates;
        private Graphics                           go;
        private int                                kcSampleImg;
        private final ActionListener               AL;
        public String                              POSITION;
        private final JFrame                       FRAME            = this;
        private final JPanel                       p;
        private ArrayList<Integer>                 mouseArray;

        public KeyCaptchaDialogForStable(final String title, final String[] arg0, final LinkedHashMap<String, int[]> arg1, Browser br, String url) throws Exception {
            super(title);
            coordinates = arg1;
            loadImage(arg0);
            handleCoordinates();
            p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
            final JLayeredPane drawPanel = new JLayeredPane();
            setSize(new Dimension(455, 235));
            makePieces();
            makeBackground();
            int offset = 4;
            boolean sampleImg = false;

            mouseArray = new ArrayList<Integer>();

            for (int i = 1; i < kcImages.length; i++) {
                if (kcImages[i] == null) {
                    continue;
                } else if (i == kcSampleImg) {
                    sampleImg = true;
                } else {
                    sampleImg = false;
                }
                drawPanel.add(new KeyCaptchaDragPieces(kcImages[i], offset, sampleImg, mouseArray, br, url), new Integer(JLayeredPane.DEFAULT_LAYER + i), new Integer(JLayeredPane.DEFAULT_LAYER + i));
                offset += 4;
            }
            drawPanel.add(new KeyCaptchaDrawBackgroundPanel(kcImages[0]), new Integer(JLayeredPane.DEFAULT_LAYER), new Integer(JLayeredPane.DEFAULT_LAYER));

            final JButton btnOk = new JButton("OK");
            final JButton btnCancel = new JButton("CANCEL");

            AL = new ActionListener() {

                public void actionPerformed(final ActionEvent e) {
                    if (e.getActionCommand().equals("OK")) {
                        POSITION = getPosition(drawPanel);
                    } else {
                        POSITION = "CANCEL";
                    }
                    try {
                        FRAME.dispose();
                    } finally {
                        synchronized (KeyCaptcha.LOCK) {
                            KeyCaptcha.LOCK.notify();
                        }
                    }
                }
            };
            btnOk.addActionListener(AL);
            btnCancel.addActionListener(AL);

            final JPanel buttons = new JPanel();
            buttons.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
            buttons.add(Box.createHorizontalGlue());

            btnOk.addActionListener(AL);
            btnCancel.addActionListener(AL);

            buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
            buttons.add(btnOk);
            buttons.add(Box.createRigidArea(new Dimension(5, 0)));
            buttons.add(btnCancel);

            p.setLayout(new BorderLayout());
            p.add(new JLabel("Assemble the image as you see at the upper right corner"), BorderLayout.NORTH);
            p.add(drawPanel, BorderLayout.CENTER);
            p.add(buttons, BorderLayout.SOUTH);

            getContentPane().add(p);

            toFront();

            if (JDGui.getInstance() == null) {
                setLocation(Screen.getCenterOfComponent(null, this));
            } else if (JDGui.getInstance().getMainFrame().getExtendedState() == 1 || !JDGui.getInstance().getMainFrame().isVisible()) {
                setLocation(Screen.getDockBottomRight(this));
            } else {
                setLocation(Screen.getCenterOfComponent(JDGui.getInstance().getMainFrame(), this));
            }

            setResizable(false);
            setVisible(true);
        }

        private String getPosition(final JLayeredPane drawPanel) {
            int i = 0;
            String positions = "";
            final Component[] comp = drawPanel.getComponents();
            for (int c = comp.length - 1; c >= 0; c--) {
                if (comp[c].getMouseListeners().length == 0) {
                    continue;
                }
                final Point p = comp[c].getLocation();
                positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
                i++;
            }
            return positions;
        }

        public void handleCoordinates() {
            kcImages = new BufferedImage[coordinates.size()];
        }

        public void loadImage(final String[] imagesUrl) {
            int i = 0;
            IMAGE = new Image[imagesUrl.length];
            File fragmentedPic;
            final Browser dlpic = new Browser();
            KeyCaptcha.prepareBrowser(dlpic, "image/png,image/*;q=0.8,*/*;q=0.5");
            final MediaTracker mt = new MediaTracker(this);
            for (final String imgUrl : imagesUrl) {
                try {
                    // fragmentedPic = Application.getRessource("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic = JDUtilities.getResourceFile("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic.deleteOnExit();
                    Browser.download(fragmentedPic, dlpic.openGetConnection(imgUrl));
                    /* TODO: replace with ImageProvider.read in future */
                    IMAGE[i] = ImageIO.read(fragmentedPic);
                    // IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new URL(imgUrl));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                mt.addImage(IMAGE[i], i);
                i++;
            }
            try {
                mt.waitForAll();
            } catch (final InterruptedException ex) {
            }
        }

        private void makeBackground() {
            int curx = 0;
            int cik = 0;
            kcImages[0] = new BufferedImage(450, 160, BufferedImage.TYPE_INT_RGB);
            go = kcImages[0].getGraphics();
            go.setColor(Color.WHITE);
            go.fillRect(0, 0, 450, 160);
            final int[] bgCoord = coordinates.get("backGroundImage");
            while (cik < bgCoord.length) {
                go.drawImage(IMAGE[1], bgCoord[cik], bgCoord[cik + 1], bgCoord[cik] + bgCoord[cik + 2], bgCoord[cik + 1] + bgCoord[cik + 3], curx, 0, curx + bgCoord[cik + 2], bgCoord[cik + 3], this);
                curx = curx + bgCoord[cik + 2];
                cik = cik + 4;
            }
        }

        private void makePieces() {
            final Object[] key = coordinates.keySet().toArray();
            int pieces = 1;
            for (final Object element : key) {
                if (element.equals("backGroundImage")) {
                    continue;
                }
                final int[] imgcs = coordinates.get(element);
                if (imgcs == null | imgcs.length == 0) {
                    break;
                }
                final int w = imgcs[1] + imgcs[5] + imgcs[9];
                final int h = imgcs[3] + imgcs[15] + imgcs[27];
                int dX = 0;
                int dY = 0;
                kcImages[pieces] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                go = kcImages[pieces].getGraphics();
                if (element.equals("kc_sample_image")) {
                    kcSampleImg = pieces;
                }
                int sX = 0, sY = 0, sW = 0, sH = 0;
                dX = 0;
                dY = 0;
                for (int cik2 = 0; cik2 < 36; cik2 += 4) {
                    sX = imgcs[cik2];
                    sY = imgcs[cik2 + 2];
                    sW = imgcs[cik2 + 1];
                    sH = imgcs[cik2 + 3];
                    if (sX + sW > IMAGE[0].getWidth(this) || sY + sH > IMAGE[0].getHeight(this)) {
                        continue;
                    }
                    if (dX + sW > w || dY + sH > h) {
                        continue;
                    }
                    if (sW == 0 || sH == 0) {
                        continue;
                    }
                    // Puzzlebild erstellen
                    go.drawImage(IMAGE[0], dX, dY, dX + sW, dY + sH, sX, sY, sX + sW, sY + sH, this);
                    dX = dX + sW;
                    if (dX >= w) {
                        dY = dY + sH;
                        dX = 0;
                    }
                }
                pieces += 1;
            }
        }

    }

    public static class KeyCaptchaShowDialogTwo {
        int    x;
        int    y;
        byte[] z = new byte[256];

        final int A() {
            int x;
            int y;
            int zx, zy;

            x = this.x + 1 & 0xff;
            zx = z[x];
            y = zx + this.y & 0xff;
            zy = z[y];
            this.x = x;
            this.y = y;
            z[y] = (byte) (zx & 0xff);
            z[x] = (byte) (zy & 0xff);
            return z[(zx + zy & 0xff)];
        }

        private void B(final byte[] b) {
            int p, o;
            int w;
            int m;
            int n;

            for (n = 0; n < 256; n++) {
                z[n] = (byte) n;
            }
            w = 0;
            m = 0;
            for (n = 0; n < 256; n++) {
                p = z[n];
                m = m + b[w] + p & 0xff;
                o = z[m];
                z[m] = (byte) (p & 0xff);
                z[n] = (byte) (o & 0xff);
                if (++w >= b.length) {
                    w = 0;
                }
            }
        }

        public synchronized void C(final byte[] a, final int b, final byte[] c, final int d, final int e) {
            final int end = b + e;
            for (int si = b, di = d; si < end; si++, di++) {
                c[di] = (byte) ((a[si] ^ A()) & 0xff);
            }
        }

        public byte[] D(final byte[] a, final byte[] b) {
            B(a);
            final byte[] dest = new byte[b.length];
            C(b, 0, dest, 0, b.length);
            return dest;
        }

    }

    /**
     * if packed js contain 'soft hyphen' encoding as \u00ad(unicode) or %C2%AD(uft-8) then result is broken in rhino
     * decodeURIComponent('\u00ad') --> is empty.
     */
    public static class JavaScriptUnpacker {

        public static String decode(String packedJavaScript) {
            /* packed with extended ascii */
            if (new Regex(packedJavaScript, "c%a\\+161").matches()) {
                String packed[] = new Regex(packedJavaScript, ("\\}\\(\'(.*?)\',(\\d+),(\\d+),\'(.*?)\'\\.split\\(\'\\|\\'\\),(\\d+)")).getRow(0);
                if (packed == null) {
                    return null;
                }
                return nativeDecode(packed[0], Integer.parseInt(packed[1]), Integer.parseInt(packed[2]), packed[3].split("\\|"), Integer.parseInt(packed[4]));
            }
            return rhinoDecode(packedJavaScript);
        }

        private static String nativeDecode(String p, int a, int c, String k[], int e) {
            LinkedHashMap<String, String> lm = new LinkedHashMap<String, String>();
            while (c > 0) {
                c--;
                lm.put(e(c, a), k[c]);
            }
            for (Entry<String, String> next : lm.entrySet()) {
                p = p.replace(next.getKey(), next.getValue());
            }
            return p;
        }

        private static String rhinoDecode(String eval) {
            Object result = new Object();
            final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(eval);
            } catch (final Throwable e) {
            }
            return result != null ? String.valueOf(result) : null;
        }

        private static String e(int c, int a) {
            return (c < a ? "" : e(c / a, a)) + String.valueOf((char) (c % a + 161));
        }

    }

    public static String IMAGEREGEX(final String b) {
        final KeyCaptchaShowDialogTwo v = new KeyCaptchaShowDialogTwo();
        /*
         * CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset!
         */
        final byte[] o = JDHash.getMD5(Encoding.Base64Decode("Yzg0MDdhMDhiM2M3MWVhNDE4ZWM5ZGM2NjJmMmE1NmU0MGNiZDZkNWExMTRhYTUwZmIxZTEwNzllMTdmMmI4Mw==") + JDHash.getMD5("V2UgZG8gbm90IGVuZG9yc2UgdGhlIHVzZSBvZiBKRG93bmxvYWRlci4=")).getBytes();
        /*
         * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
         */
        if (b != null) {
            return new String(v.D(o, JDHexUtils.getByteArray(b)));
        }
        return new String(v.D(o, JDHexUtils.getByteArray("E3CEACB19040D08244C9E5C29D115AE220F83AB417")));
    }

    private final HashMap<String, String> map = new HashMap<String, String>();

    public LnkCrptWs(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        setBrowserExclusive();
        prepareBrowser("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
        final String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);
        parameter = "http://linkcrypt.ws/dir/" + containerId;
        URLConnectionAdapter con;
        loadAndSolveCaptcha(param, progress, decryptedLinks, parameter, containerId);
        // check for a password. Store latest password in DB
        Form password = br.getForm(0);
        if (password != null && password.hasInputFieldByName("password")) {
            String latestPassword = getPluginConfig().getStringProperty("PASSWORD");
            if (latestPassword != null) {
                password.put("password", latestPassword);
                br.submitForm(password);
                //
            }
            // no defaultpassword, or defaultpassword is wrong
            for (int i = 0; i <= 3; i++) {
                password = br.getForm(0);
                if (password != null && password.hasInputFieldByName("password")) {
                    latestPassword = Plugin.getUserInput(null, param);
                    password.put("password", latestPassword);
                    br.submitForm(password);
                    password = br.getForm(0);
                    if (password != null && password.hasInputFieldByName("password")) {
                        continue;
                    }
                    getPluginConfig().setProperty("PASSWORD", latestPassword);
                    getPluginConfig().save();
                    break;
                }
                break;
            }
        }
        if (password != null && password.hasInputFieldByName("password")) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        // Look for containers
        String[] containers = br.getRegex("eval(.*?)[\r\n]+").getColumn(0);
        final String tmpc = br.getRegex("<div id=\"containerfiles\"(.*?)</script>").getMatch(0);
        if (tmpc != null) {
            containers = new Regex(tmpc, "eval(.*?)[\r\n]+").getColumn(0);
        }
        String decryptedJS = null;
        for (String c : containers) {
            decryptedJS = JavaScriptUnpacker.decode(c);
            String[] row = new Regex(decryptedJS, "href=\"(http.*?)\".*?(dlc|ccf|rsdf)").getRow(0);// all
            // container
            if (row == null) {
                row = new Regex(decryptedJS, "href=\"([^\"]+)\"[^>]*>.*?<img.*?image/(.*?)\\.").getRow(0); // cnl
            }
            if (row == null) {
                row = new Regex(decryptedJS, "(https?://linkcrypt\\.ws/container/[^\"]+)\".*?https?://linkcrypt\\.ws/image/([a-z]+)\\.").getRow(0); // fallback
            }
            if (row != null) {
                if ("cnl".equalsIgnoreCase(row[1])) {
                    row[1] = "cnl";
                    row[0] = decryptedJS;
                }
                if (!map.containsKey(row[1])) {
                    map.put(row[1], row[0]);
                }
            }
        }

        final Form preRequest = br.getForm(0);
        if (preRequest != null) {
            final String url = preRequest.getRegex("https?://.*/captcha\\.php\\?id=\\d+").getMatch(-1);
            if (url != null) {
                con = null;
                try {
                    con = br.cloneBrowser().openGetConnection(url);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }

        /* CNL --> Container --> Webdecryption */
        boolean webDecryption = br.containsHTML("BlueHeadLine.*?>Weblinks<");
        boolean isCnlAvailable = map.containsKey("cnl");

        // CNL
        if (isCnlAvailable) {
            final Browser cnlbr = br.cloneBrowser();
            decryptedJS = map.get("cnl").replaceAll("\\\\", "");

            /* Workaround for the stable and parseInputFields method */
            String jk = new Regex(decryptedJS, "(?i)NAME=\"jk\" VALUE=\"([^\"]+)\">").getMatch(0);
            decryptedJS = decryptedJS.replaceAll("(?i)NAME=\"jk\" VALUE=\"[^\"]+\">", "NAME=\"jk\" VALUE=\"" + Encoding.urlEncode(jk) + "\">");

            cnlbr.getRequest().setHtmlCode(decryptedJS);
            Form cnlForm = cnlbr.getForm(0);

            if (cnlForm != null) {
                if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
                    HashMap<String, String> infos = new HashMap<String, String>();
                    infos.put("crypted", Encoding.urlDecode(cnlForm.getInputField("crypted").getValue(), false));
                    infos.put("jk", Encoding.urlDecode(cnlForm.getInputField("jk").getValue(), false));
                    String source = cnlForm.getInputField("source").getValue();
                    if (StringUtils.isEmpty(source)) {
                        source = parameter.toString();
                    } else {
                        source = Encoding.urlDecode(source, true);
                    }
                    infos.put("source", source);
                    String json = JSonStorage.toString(infos);
                    final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                } else {
                    try {
                        cnlbr.submitForm(cnlForm);
                        if (cnlbr.containsHTML("success")) {
                            return decryptedLinks;
                        }
                        if (cnlbr.containsHTML("^failed")) {
                            logger.warning("linkcrypt.ws: CNL2 Postrequest was failed! Please upload now a logfile, contact our support and add this loglink to your bugreport!");
                            logger.warning("linkcrypt.ws: CNL2 Message: " + cnlbr.toString());
                        }
                    } catch (Throwable e) {
                        logger.info("linkcrypt.ws: ExternInterface(CNL2) is disabled!");
                    }
                }
            }
            map.remove("cnl");
        }

        // Container
        for (Entry<String, String> next : map.entrySet()) {
            if (!next.getKey().equalsIgnoreCase("cnl")) {
                final File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + next.getKey(), true);
                if (!container.exists()) {
                    container.createNewFile();
                }
                try {
                    br.cloneBrowser().getDownload(container, next.getValue());
                    if (container != null) {
                        logger.info("Container found: " + container);
                        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                        container.delete();
                        if (!decryptedLinks.isEmpty()) {
                            return decryptedLinks;
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Webdecryption
        if (webDecryption) {
            // shouldn't we already be at this url?
            br.getPage("http://linkcrypt.ws/dir/" + containerId);
            logger.info("Trying webdecryption...");
            final Form[] forms = br.getForms();
            progress.setRange(forms.length - 8);
            for (final Form form : forms) {
                Browser clone;
                if (form.getInputField("file") != null && form.getInputField("file").getValue() != null && form.getInputField("file").getValue().length() > 0) {
                    progress.increase(1);
                    clone = br.cloneBrowser();
                    clone.submitForm(form);
                    final String[] srcs = clone.getRegex("<frame scrolling.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getColumn(0);
                    for (String col : srcs) {
                        if (col.contains("out.pl=head")) {
                            continue;
                        }
                        col = Encoding.htmlDecode(col);
                        if (col.contains("out.pl")) {
                            clone.getPage(col);
                            // Thread.sleep(600);
                            if (clone.containsHTML("eval")) {
                                final String[] evals = clone.getRegex("eval(.*?)[\r\n]+").getColumn(0);
                                for (final String c : evals) {
                                    String code = JavaScriptUnpacker.decode(c);
                                    if (code == null) {
                                        continue;
                                    }
                                    if (code.contains("ba2se") || code.contains("premfree")) {
                                        code = code.replaceAll("\\\\", "");
                                        String versch = new Regex(code, "ba2se=\'(.*?)\'").getMatch(0);
                                        if (versch == null) {
                                            versch = new Regex(code, ".*?='([^']*)'").getMatch(0);
                                            versch = Encoding.Base64Decode(versch);
                                            versch = new Regex(versch, "<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getMatch(0);
                                        }
                                        versch = Encoding.Base64Decode(versch);
                                        versch = Encoding.htmlDecode(new Regex(versch, "100.*?src=\"(.*?)\"></iframe>").getMatch(0));
                                        if (versch != null) {
                                            final DownloadLink dl = createDownloadlink(versch);
                                            try {
                                                distribute(dl);
                                            } catch (final Throwable e) {
                                                /* does not exist in 09581 */
                                            }
                                            decryptedLinks.add(dl);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 23.7.14
                    String link = clone.getRegex("'window.location = \"([^\"]+)").getMatch(0);
                    if (link != null) {
                        final DownloadLink dl = createDownloadlink(link);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            /* does not exist in 09581 */
                        }
                        decryptedLinks.add(dl);
                    }
                    try {
                        if (this.isAbort()) {
                            return decryptedLinks;
                        }
                    } catch (Throwable e) {
                        /* does not exist in 09581 */
                    }
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.info("No links found, let's see if CNL2 is available!");
            if (isCnlAvailable) {
                LocalBrowser.openDefaultURL(new URL(parameter));
                throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
            }
            logger.warning("Decrypter out of date for link: " + parameter);
            try {
                invalidateLastChallengeResponse();
            } catch (final Throwable e) {
            }
            return null;
        }
        try {
            validateLastChallengeResponse();
        } catch (final Throwable e) {
        }
        return decryptedLinks;
    }

    public void loadAndSolveCaptcha(final CryptedLink param, final ProgressController progress, final ArrayList<DownloadLink> decryptedLinks, String parameter, final String containerId) throws IOException, InterruptedException, Exception, DecrypterException {
        br.clearCookies(parameter);
        br.getPage(parameter);
        for (int i = 0; i < 5; i++) {
            if (br.containsHTML("TextX")) {
                // since we are currently not able to auto solve TextX Captcha, we try to get another one
                Thread.sleep(500);
                br.clearCookies(parameter);
                br.getPage(parameter);
            }
        }
        System.out.println("TextX " + br.containsHTML("TextX"));
        System.out.println("CaptX " + br.containsHTML("CaptX"));
        System.out.println("KeyCAPTCHA " + br.containsHTML("KeyCAPTCHA"));
        if (br.containsHTML("<title>Linkcrypt\\.ws // Error 404</title>")) {
            logger.info("This link might be offline: " + parameter);
            final String additional = br.getRegex("<h2>\r?\n?(.*?)<").getMatch(0);
            if (additional != null) {
                logger.info(additional);
            }
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            offline.setFinalFileName(new Regex(parameter, "([\\w]+)$").getMatch(0));
            decryptedLinks.add(offline);
            throw new Exception("Cancel");
        }

        final String important[] = { "/js/jquery.js", "/dir/image/Warning.png" };
        URLConnectionAdapter con = null;
        for (final String template : important) {
            final Browser br2 = br.cloneBrowser();
            try {
                con = br2.openGetConnection(template);
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }

        // Different captcha types
        boolean valid = true;
        boolean done = false;
        if (br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) {
            KeyCaptcha kc;

            // START solve keycaptcha automatically
            for (int i = 0; i < 3; i++) {
                kc = new KeyCaptcha(br);
                final String result = kc.autoSolve(parameter);

                if ("CANCEL".equals(result)) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }

                br.postPage(parameter, "capcode=" + Encoding.urlEncode(result));
                if (!br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) {
                    done = true;
                    break;
                }
            }
            // START solve keycaptcha automatically
            if (!done) {
                for (int i = 0; i <= 3; i++) {
                    kc = new KeyCaptcha(br);
                    final String result = kc.showDialog(parameter);
                    if (result == null) {
                        continue;
                    }
                    if ("CANCEL".equals(result)) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    br.postPage(parameter, "capcode=" + Encoding.urlEncode(result));
                    if (!br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) {
                        break;
                    }
                }
            }
        }
        if (br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        if (br.containsHTML("CaptX|TextX")) {
            final int max_attempts = 4;
            for (int attempts = 0; attempts < max_attempts; attempts++) {
                if (valid && attempts > 0) {
                    break;
                }
                final Form[] captchas = br.getForms();
                String url = null;
                for (final Form captcha : captchas) {
                    if (captcha != null && br.containsHTML("CaptX|TextX")) {
                        url = captcha.getRegex("src=\"(.*?secid.*?)\"").getMatch(0);
                        if (url != null) {
                            valid = false;
                            final String capDescription = captcha.getRegex("<b>(.*?)</b>").getMatch(0);
                            final File file = this.getLocalCaptchaFile();
                            br.cloneBrowser().getDownload(file, url);
                            // remove black bars
                            Point p = null;
                            final byte[] bytes = IO.readBytes(file);
                            if (br.containsHTML("CaptX") && attempts < 2) {
                                // try autosolve
                                p = CaptXSolver.solveCaptXCaptcha(bytes);
                            }
                            if (p == null) {

                                // solve by user
                                BufferedImage image = toBufferedImage(new ByteArrayInputStream(bytes));
                                ImageIO.write(image, "png", file);
                                p = UserIO.getInstance().requestClickPositionDialog(file, "LinkCrypt.ws | " + String.valueOf(max_attempts - attempts), capDescription);
                            }
                            if (p == null) {
                                throw new DecrypterException(DecrypterException.CAPTCHA);
                            }
                            captcha.put("x", p.x + "");
                            captcha.put("y", p.y + "");
                            br.submitForm(captcha);
                            if (!br.containsHTML("(Our system could not identify you as human beings\\!|Your choice was wrong\\! Please wait some seconds and try it again\\.)")) {
                                valid = true;
                            } else {
                                br.getPage("/dir/" + containerId);
                            }
                        }
                    }
                }
            }
        }
        if (!valid) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);
        try {
            ret.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
        } catch (Throwable e) {

        }
        return ret;
    }

    // stuff for cleaning black animations
    private static void cleanBlack(int x, int y, int[][] grid) {
        for (int x1 = Math.max(x - 2, 0); x1 < Math.min(x + 2, grid.length); x1++) {
            for (int y1 = Math.max(y - 2, 0); y1 < Math.min(y + 2, grid[0].length); y1++) {
                if (grid[x1][y1] == 0x000000) {
                    grid[x1][y1] = 0xffffff;
                    cleanBlack(x1, y1, grid);
                }
            }
        }
    }

    private static BufferedImage toBufferedImage(InputStream is) throws InterruptedException {

        try {
            JAntiCaptcha jac = new JAntiCaptcha("easycaptcha");
            jac.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(is);
            int n = d.getFrameCount();
            Captcha[] frames = new Captcha[d.getFrameCount()];
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                frames[i] = jac.createCaptcha(frame);

            }
            int[][] grid = new int[frames[0].getWidth()][frames[0].getHeight()];

            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[0].length; y++) {
                    int max = 0;
                    HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
                    for (int i = 0; i < frames.length; i++) {
                        float[] hsb = Colors.rgb2hsb(frames[i].getGrid()[x][y]);
                        int distance = Colors.getRGBDistance(frames[i].getGrid()[x][y]);
                        if (!colors.containsKey(frames[i].getGrid()[x][y])) {
                            colors.put(frames[i].getGrid()[x][y], 1);
                        } else {
                            colors.put(frames[i].getGrid()[x][y], colors.get(frames[i].getGrid()[x][y]) + 1);
                        }
                        if (hsb[2] < 0.2 && distance < 100) {
                            continue;
                        }

                        max = Math.max(max, frames[i].getGrid()[x][y]);
                    }
                    int mainColor = 0;
                    int mainCount = 0;
                    for (Entry<Integer, Integer> col : colors.entrySet()) {
                        if (col.getValue() > mainCount && col.getKey() > 10) {
                            mainCount = col.getValue();
                            mainColor = col.getKey();
                        }
                    }
                    grid[x][y] = mainColor;
                }
            }
            int gl1 = grid[0].length - 1;
            for (int x = 0; x < grid.length; x++) {
                int bl1 = 0;
                int bl2 = 0;
                for (int i = Math.max(0, x - 6); i < Math.min(grid.length, x + 6); i++) {
                    if (grid[i][0] == 0x000000) {
                        bl1++;
                    }
                    if (grid[i][gl1] == 0x000000) {
                        bl2++;
                    }
                }
                if (bl1 == 12) {
                    cleanBlack(x, 0, grid);
                }
                if (bl2 == 12) {
                    cleanBlack(x, gl1, grid);
                }
            }
            gl1 = grid.length - 1;

            for (int y = 0; y < grid.length; y++) {
                int bl1 = 0;
                int bl2 = 0;
                for (int i = Math.max(0, y - 6); i < Math.min(grid[0].length, y + 6); i++) {
                    if (grid[0][i] == 0x000000) {
                        bl1++;
                    }
                    if (grid[gl1][i] == 0x000000) {
                        bl2++;
                    }
                }
                if (bl1 == 12) {
                    cleanBlack(0, y, grid);
                }
                if (bl2 == 12) {
                    cleanBlack(gl1, y, grid);
                }
            }
            frames[0].setGrid(grid);

            return frames[0].getImage(1);

        } finally {

        }
    }

    /**
     * TODO: can be removed with next major update cause of recaptcha change
     */

    public AdsCaptcha getAdsCaptcha(final Browser br) {
        return new AdsCaptcha(br);
    }

    public SolveMedia getSolveMedia(final Browser br) {
        return new SolveMedia(br);
    }

    public KeyCaptcha getKeyCaptcha(final Browser br) {
        return new KeyCaptcha(br);
    }

    private void prepareBrowser(final String userAgent) {
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Language", "en-EN");
        br.getHeaders().put("User-Agent", userAgent);
        try {
            br.setCookie("linkcrypt.ws", "language", "en");
        } catch (final Throwable e) {
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    // KeyCaptcha stuff
    /**
     * Solves KeyCaptcha for us
     * 
     * @author flubshi
     * 
     */
    private static class KeyCaptchaSolver {
        // min line length for border detection
        private final int           borderPixelMin = 15;
        // threshold for detection similar pixels
        private final Double        threshold      = 7.0d;

        private LinkedList<Integer> mouseArray     = new LinkedList<Integer>();

        private void marray(Point loc) {
            if (loc != null) {
                if (mouseArray.size() == 0) {
                    mouseArray.add(loc.x + 465);
                    mouseArray.add(loc.y + 264);
                }
                if (mouseArray.get(mouseArray.size() - 2) != loc.x + 465 || mouseArray.get(mouseArray.size() - 1) != loc.y + 264) {
                    mouseArray.add(loc.x + 465);
                    mouseArray.add(loc.y + 264);
                }
                if (mouseArray.size() > 40) {
                    ArrayList<Integer> tmpMouseArray = new ArrayList<Integer>();
                    tmpMouseArray.addAll(mouseArray.subList(2, 40));
                    mouseArray.clear();
                    mouseArray.addAll(tmpMouseArray);
                }
            }
        }

        public LinkedList<Integer> getMouseArray() {
            return mouseArray;
        }

        public String solve(KeyCaptchaImages images) {
            HashMap<BufferedImage, Point> imgPosition = new HashMap<BufferedImage, Point>();
            int limit = images.pieces.size();

            LinkedList<BufferedImage> piecesOld = new LinkedList<BufferedImage>(images.pieces);

            for (int i = 0; i < limit; i++) {
                List<DirectedBorder> borders = getBreakingBordersInImage(images.backgroundImage, borderPixelMin);
                ImageAndPosition imagePos = getBestPosition(images, borders);
                marray(new Point((int) (Math.random() * imagePos.position.x), (int) (Math.random() * imagePos.position.y)));
                marray(imagePos.position);

                imgPosition.put(imagePos.image, imagePos.position);
                images.integratePiece(imagePos.image, imagePos.position);
            }

            String positions = "";
            int i = 0;
            for (int c = 0; c < piecesOld.size(); c++) {
                BufferedImage image = piecesOld.get(c);
                final Point p = imgPosition.get(image);
                positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
                i++;
            }
            return positions;
        }

        /**
         * Find vertical & horizontal borders within an image
         * 
         * @param img
         *            the image to search in
         * @param min
         *            line length for border detection
         * @return a set of directed borders
         */
        private List<DirectedBorder> getBreakingBordersInImage(BufferedImage img, int minPixels) {
            List<DirectedBorder> triples = new LinkedList<DirectedBorder>();
            // horizontal
            for (int y = 0; y < img.getHeight() - 1; y++) {
                int c = -1;
                boolean whiteToColor = true;
                for (int x = 0; x < img.getWidth(); x++) {
                    if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) < threshold && Colors.getCMYKColorDifference1(img.getRGB(x, y + 1), Color.WHITE.getRGB()) > threshold) {
                        if (!whiteToColor) {
                            whiteToColor = true;
                            c = -1;
                        }
                        c++;
                        if (c >= minPixels) {
                            triples.add(new DirectedBorder(new Point(x - c, y + 1), new Point(x, y + 1), Direction.TOPDOWN));
                            if (c > minPixels) {
                                triples.remove(triples.size() - 2);
                            }
                        }
                    } else if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) > threshold && Colors.getCMYKColorDifference1(img.getRGB(x, y + 1), Color.WHITE.getRGB()) < threshold) {
                        if (whiteToColor) {
                            whiteToColor = false;
                            c = -1;
                        }
                        c++;
                        if (c >= minPixels) {
                            triples.add(new DirectedBorder(new Point(x - c, y), new Point(x, y), Direction.BOTTOMUP));
                            if (c > minPixels) {
                                triples.remove(triples.size() - 2);
                            }
                        }
                    } else {
                        c = -1;
                    }
                }
            }
            // vertical
            for (int x = 0; x < img.getWidth() - 1; x++) {
                int c = -1;
                boolean whiteToColor = true;
                for (int y = 0; y < img.getHeight(); y++) {

                    if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) < threshold && Colors.getCMYKColorDifference1(img.getRGB(x + 1, y), Color.WHITE.getRGB()) > threshold) {
                        if (!whiteToColor) {
                            whiteToColor = true;
                            c = -1;
                        }
                        c++;
                        if (c >= minPixels) {
                            triples.add(new DirectedBorder(new Point(x + 1, y - c), new Point(x + 1, y), Direction.LEFTRIGHT));
                            if (c > minPixels) {
                                triples.remove(triples.size() - 2);
                            }
                        }
                    } else if (Colors.getCMYKColorDifference1(img.getRGB(x, y), Color.WHITE.getRGB()) > threshold && Colors.getCMYKColorDifference1(img.getRGB(x + 1, y), Color.WHITE.getRGB()) < threshold) {
                        if (whiteToColor) {
                            whiteToColor = false;
                            c = -1;
                        }
                        c++;
                        if (c >= minPixels) {
                            triples.add(new DirectedBorder(new Point(x, y - c), new Point(x, y), Direction.RIGHTLEFT));
                            if (c > minPixels) {
                                triples.remove(triples.size() - 2);
                            }
                        }
                    } else {
                        c = -1;
                    }

                }
            }
            return triples;
        }

        /**
         * Gets the image and its position with highest possible probability to be correct for this puzzle piece
         * 
         * @param keyCaptchaImages
         *            all keycaptcha images (background, sample, pieces)
         * @param borders
         *            a collection of all borders within the background image
         * @return one puzzle piece and its position within the puzzle
         */
        private ImageAndPosition getBestPosition(KeyCaptchaImages keyCaptchaImages, List<DirectedBorder> borders) {
            int bestMin = Integer.MAX_VALUE;
            Point bestPos = new Point();
            BufferedImage bestPiece = null;

            for (BufferedImage piece : keyCaptchaImages.pieces) {
                for (DirectedBorder border : borders) {
                    if (border.direction == Direction.TOPDOWN || border.direction == Direction.BOTTOMUP) {
                        // horizontal
                        if ((border.direction == Direction.TOPDOWN && border.p1.y - piece.getHeight() < 0) || (border.direction == Direction.BOTTOMUP && (border.p1.y + piece.getHeight() > keyCaptchaImages.backgroundImage.getHeight()))) {
                            continue;
                        }

                        for (int x = Math.max(border.p1.x - piece.getWidth(), 0); x <= Math.min(border.p2.x, keyCaptchaImages.backgroundImage.getWidth() - 1); x++) {
                            int tmp = rateHorizontalLine(keyCaptchaImages.backgroundImage, piece, new Point(x, border.p1.y), border.direction == Direction.TOPDOWN ? piece.getHeight() - 1 : 0);
                            tmp /= piece.getWidth();
                            if (tmp < bestMin) {
                                bestMin = tmp;
                                bestPos = new Point(x, border.p1.y + (border.direction == Direction.TOPDOWN ? -piece.getHeight() : 1));
                                bestPiece = piece;
                            }
                        }
                    } else {
                        // vertical
                        if ((border.direction == Direction.LEFTRIGHT && border.p1.x - piece.getWidth() < 0) || (border.direction == Direction.RIGHTLEFT && border.p1.x + piece.getWidth() > keyCaptchaImages.backgroundImage.getWidth())) {
                            continue;
                        }

                        for (int y = Math.max(border.p1.y - piece.getHeight(), 0); y <= Math.min(border.p2.y, keyCaptchaImages.backgroundImage.getHeight() - 1); y++) {
                            int tmp = rateVerticalLine(keyCaptchaImages.backgroundImage, piece, new Point(border.p1.x, y), border.direction == Direction.LEFTRIGHT ? piece.getWidth() - 1 : 0);
                            tmp /= piece.getHeight();
                            if (tmp < bestMin) {
                                bestMin = tmp;
                                bestPos = new Point(border.p1.x + (border.direction == Direction.LEFTRIGHT ? -piece.getWidth() : 1), y);
                                bestPiece = piece;
                            }
                        }
                    }
                }
            }
            return new ImageAndPosition(bestPiece, bestPos);
        }

        /**
         * Rates probaility the puzzle piece fits horizontal to this position
         * 
         * @param background
         *            the background image
         * @param piece
         *            puzzle piece image
         * @param backgroundPosition
         *            the position to rate (within background image)
         * @param pieceY
         *            the y offset within puzzle to comapre
         * @return a rating (smaller is better)
         */
        private int rateHorizontalLine(BufferedImage background, BufferedImage piece, Point backgroundPosition, int pieceY) {
            int diff = 0;
            for (int x = 0; x < piece.getWidth(); x++) {
                if (backgroundPosition.x + x >= background.getWidth()) {
                    diff += (backgroundPosition.x + piece.getWidth() - background.getWidth()) * 150;
                    break;
                }
                int bgColor = background.getRGB(backgroundPosition.x + x, backgroundPosition.y);
                int pColor = piece.getRGB(x, pieceY);
                diff += Colors.getColorDifference(bgColor, pColor);
                if (Colors.getCMYKColorDifference1(pColor, Color.WHITE.getRGB()) < threshold) {
                    diff += 30;
                }
            }
            return diff;
        }

        /**
         * Rates probaility the puzzle piece fits vertical to this position
         * 
         * @param background
         *            the background image
         * @param piece
         *            puzzle piece image
         * @param backgroundPosition
         *            the position to rate (within background image)
         * @param pieceX
         *            the x offset within puzzle to comapre
         * @return a rating (smaller is better)
         */
        private int rateVerticalLine(BufferedImage background, BufferedImage piece, Point backgroundPosition, int pieceX) {
            int diff = 0;
            for (int y = 0; y < piece.getHeight(); y++) {
                if (backgroundPosition.y + y >= background.getHeight()) {
                    diff += (backgroundPosition.y + piece.getHeight() - background.getHeight()) * 150;
                    break;
                }
                int bgColor = background.getRGB(backgroundPosition.x, backgroundPosition.y + y);
                int pColor = piece.getRGB(pieceX, y);
                diff += Colors.getColorDifference(bgColor, pColor);
                if (Colors.getCMYKColorDifference1(pColor, Color.WHITE.getRGB()) < threshold) {
                    diff += 30;
                }
            }
            return diff;
        }
    }

    /**
     * Represnts a border an the direction of white to 'color'
     * 
     * @author flubshi
     * 
     */
    private static class DirectedBorder {
        public final Point     p1;
        public final Point     p2;
        public final Direction direction;

        /**
         * @param p1
         *            start of bordering line
         * @param p2
         *            end of bordering line
         * @param direction
         *            the direction (e.g. TOPDOWN if a horizontal line with white above and color below)
         */
        public DirectedBorder(Point p1, Point p2, Direction direction) {
            this.p1 = p1;
            this.p2 = p2;
            this.direction = direction;
        }
    }

    /**
     * represents a direction
     * 
     * @author flubshi
     * 
     */
    private enum Direction {
        TOPDOWN,
        RIGHTLEFT,
        BOTTOMUP,
        LEFTRIGHT
    }

    /**
     * Datastructure to assign a position to an image
     * 
     * @author flubshi
     * 
     */
    private static class ImageAndPosition {
        public final BufferedImage image;
        public final Point         position;

        /**
         * Assign a position to an image
         * 
         * @param image
         *            the image (usually a puzzle piece for KeyCaptcha)
         * @param position
         *            the position to assign to the image
         */
        public ImageAndPosition(BufferedImage image, Point position) {
            this.image = image;
            this.position = position;
        }
    }

    /**
     * Represents a KeyCaptcha, which consists of a background image, sample image and a few puzzle pieces
     * 
     * @author flubshi
     * 
     */
    private static class KeyCaptchaImages {
        public BufferedImage             backgroundImage;
        public BufferedImage             sampleImage;
        public LinkedList<BufferedImage> pieces;

        /**
         * Creates an object
         * 
         * @param backgroundImage
         *            The background image of the KeyCaptcha
         * @param sampleImage
         *            a smaller, monochrome version of the assembled image
         * @param pieces
         *            a collection of puzzle pieces
         */
        public KeyCaptchaImages(BufferedImage backgroundImage, BufferedImage sampleImage, LinkedList<BufferedImage> pieces) {
            this.backgroundImage = backgroundImage;
            this.sampleImage = sampleImage;
            this.pieces = pieces;
        }

        /**
         * Integrates a puzzle piece into the puzzle: removes piece from list of puzzle and draws the piece on background
         * 
         * @param image
         *            the puzzle piece image
         * @param position
         *            the puzzle piece's position
         */
        public void integratePiece(BufferedImage image, Point position) {
            if (pieces.remove(image)) {
                Graphics2D g2d = backgroundImage.createGraphics();
                g2d.drawImage(image, position.x, position.y, null);
                g2d.dispose();
            }
        }
    }

    private static class KeyCaptchaImageGetter {
        private Image[]                            IMAGE;
        private BufferedImage[]                    kcImages;
        private final LinkedHashMap<String, int[]> coordinates;
        private Graphics                           go;
        private int                                kcSampleImg;

        private KeyCaptchaImages                   keyCaptchaImage;

        public KeyCaptchaImageGetter(final String[] imageUrl, final LinkedHashMap<String, int[]> coordinates, Browser br, String url) throws Exception {
            this.coordinates = coordinates;
            loadImage(imageUrl);
            handleCoordinates();

            makePieces();
            makeBackground();

            LinkedList<BufferedImage> pieces = new LinkedList<BufferedImage>();
            BufferedImage sampleImg = null;

            for (int i = 1; i < kcImages.length; i++) {
                if (kcImages[i] == null) {
                    continue;
                } else if (i == kcSampleImg) {
                    sampleImg = kcImages[i];
                } else {
                    pieces.add(kcImages[i]);
                }
            }
            keyCaptchaImage = new KeyCaptchaImages(kcImages[0], sampleImg, pieces);
        }

        public KeyCaptchaImages getKeyCaptchaImage() {
            return this.keyCaptchaImage;
        }

        public void handleCoordinates() {
            kcImages = new BufferedImage[coordinates.size()];
        }

        public void loadImage(final String[] imagesUrl) {
            int i = 0;
            IMAGE = new Image[imagesUrl.length];
            File fragmentedPic;
            final Browser dlpic = new Browser();
            KeyCaptcha.prepareBrowser(dlpic, "image/png,image/*;q=0.8,*/*;q=0.5");
            final MediaTracker mt = new MediaTracker(new JLabel());
            for (final String imgUrl : imagesUrl) {
                try {
                    // fragmentedPic = Application.getRessource("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic = JDUtilities.getResourceFile("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic.deleteOnExit();
                    Browser.download(fragmentedPic, dlpic.openGetConnection(imgUrl));
                    /* TODO: replace with ImageProvider.read in future */
                    IMAGE[i] = ImageIO.read(fragmentedPic);
                    // IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new URL(imgUrl));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                mt.addImage(IMAGE[i], i);
                i++;
            }
            try {
                mt.waitForAll();
            } catch (final InterruptedException ex) {
            }
        }

        private void makeBackground() {
            int curx = 0;
            int cik = 0;
            kcImages[0] = new BufferedImage(450, 160, BufferedImage.TYPE_INT_RGB);
            go = kcImages[0].getGraphics();
            go.setColor(Color.WHITE);
            go.fillRect(0, 0, 450, 160);
            final int[] bgCoord = coordinates.get("backGroundImage");
            while (cik < bgCoord.length) {
                go.drawImage(IMAGE[1], bgCoord[cik], bgCoord[cik + 1], bgCoord[cik] + bgCoord[cik + 2], bgCoord[cik + 1] + bgCoord[cik + 3], curx, 0, curx + bgCoord[cik + 2], bgCoord[cik + 3], null);
                curx = curx + bgCoord[cik + 2];
                cik = cik + 4;
            }
        }

        private void makePieces() {
            final Object[] key = coordinates.keySet().toArray();
            int pieces = 1;
            for (final Object element : key) {
                if (element.equals("backGroundImage")) {
                    continue;
                }
                final int[] imgcs = coordinates.get(element);
                if (imgcs == null | imgcs.length == 0) {
                    break;
                }
                final int w = imgcs[1] + imgcs[5] + imgcs[9];
                final int h = imgcs[3] + imgcs[15] + imgcs[27];
                int dX = 0;
                int dY = 0;
                kcImages[pieces] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                go = kcImages[pieces].getGraphics();
                if (element.equals("kc_sample_image")) {
                    kcSampleImg = pieces;
                }
                int sX = 0, sY = 0, sW = 0, sH = 0;
                dX = 0;
                dY = 0;
                for (int cik2 = 0; cik2 < 36; cik2 += 4) {
                    sX = imgcs[cik2];
                    sY = imgcs[cik2 + 2];
                    sW = imgcs[cik2 + 1];
                    sH = imgcs[cik2 + 3];
                    if (sX + sW > IMAGE[0].getWidth(null) || sY + sH > IMAGE[0].getHeight(null)) {
                        continue;
                    }
                    if (dX + sW > w || dY + sH > h) {
                        continue;
                    }
                    if (sW == 0 || sH == 0) {
                        continue;
                    }
                    // create puzzle piece
                    go.drawImage(IMAGE[0], dX, dY, dX + sW, dY + sH, sX, sY, sX + sW, sY + sH, null);
                    dX = dX + sW;
                    if (dX >= w) {
                        dY = dY + sH;
                        dX = 0;
                    }
                }
                pieces += 1;
            }
        }
    }

    // CaptX autosolve stuff
    private static class Circle {
        int                       inBorder        = 4;
        int                       outBorder       = 2;

        int                       minArea         = 170;
        private Captcha           captcha;
        int                       backgroundColor = 0xffffff;
        private List<PixelObject> objectArray;
        private Letter            openCircle;

        public Circle(Captcha captcha, List<PixelObject> objectArray) {
            this.captcha = captcha;
            this.objectArray = objectArray;
        }

        /**
         * is the color a color of the element 1=true 0=false
         */
        Comparator<Integer> isElementColor = new Comparator<Integer>() {

                                               public int compare(Integer o1, Integer o2) {
                                                   int c = o1;
                                                   int c2 = o2;
                                                   if (isBackground(o1) || isBackground(o2)) {
                                                       return 0;
                                                   }
                                                   if (c == 0x000000 || c2 == 0x000000) {
                                                       return c == c2 ? 1 : 0;
                                                   }
                                                   int[] hsvC = Colors.rgb2hsv(c);
                                                   int[] hsvC2 = Colors.rgb2hsv(c2);
                                                   // TODO The "hsvC[1] / hsvC2[2] == 1" is repeated twice
                                                   // Is it a typo? Was a different comparison meant in the second place?
                                                   return ((hsvC[0] == hsvC2[0] && (hsvC[1] == hsvC2[1] || hsvC[2] == hsvC2[2] || hsvC[1] / hsvC2[2] == 1 || hsvC[1] / hsvC2[2] == 1)) && Colors.getRGBColorDifference2(c, c2) < 80) ? 1 : 0;
                                               }

                                           };

        private boolean equalElements(int c, int c2) {
            return isElementColor.compare(c, c2) == 1;
        }

        private boolean isBackground(int c) {
            return c < 0 || c == backgroundColor;
        }

        private PixelObject getCircle(int x, int y, int r) {
            PixelObject b = new PixelObject(captcha);
            int ret = 0;
            for (int i = -inBorder; i < outBorder / 2; i++) {
                PixelObject n = new PixelObject(captcha);
                ret += circle(x, y, r + i, n);
                if (n.getSize() > 0) {
                    b.add(n);
                }
            }
            if (b.getSize() > 10 && b.getArea() > 30) {
                return b;
            }
            return null;
        }

        private int checkBackground(int x, int y, PixelObject n) {
            int c = captcha.getPixelValue(x, y);
            boolean b = isBackground(c);
            if (!b) {
                n.add(x, y, c);
            }
            return b ? 1 : 0;
        }

        // Midpoint circle algorithm
        private int circle(int cx, int cy, int radius, PixelObject n) {
            int error = -radius;
            int x = radius;
            int y = 0;
            int ret = 0;
            while (x >= y) {
                ret += plot8points(cx, cy, x, y, n);
                ret += plot8points(cx - 1, cy, x, y, n);
                ret += plot8points(cx, cy - 1, x, y, n);

                error += y;
                ++y;
                error += y;

                if (error >= 0) {
                    --x;
                    error -= x;
                    error -= x;
                }
            }
            return ret;
        }

        // Midpoint circle algorithm
        private int plot8points(int cx, int cy, int x, int y, PixelObject n) {
            int ret = 0;
            ret += plot4points(cx, cy, x, y, n);
            if (x != y) {
                ret += plot4points(cx, cy, y, x, n);
            }
            return ret;
        }

        // Midpoint circle algorithm
        private int plot4points(int cx, int cy, int x, int y, PixelObject n) {
            int ret = 0;
            ret += checkBackground(cx + x, cy + y, n);
            if (x != 0) {
                ret += checkBackground(cx - x, cy + y, n);
            }
            if (y != 0) {
                ret += checkBackground(cx + x, cy - y, n);
            }
            if (x != 0 && y != 0) {
                ret += checkBackground(cx - x, cy - y, n);
            }
            return ret;
        }

        /**
         * returns the Circles Bounds on the Captcha TODO geht nur bei x entlang sollte noch bei y gemacht werden um bessere ergebnisse zu
         * bekommen
         * 
         * @param pixelObject
         * @param captcha
         * @return
         */
        private int[] getBounds(PixelObject pixelObject) {
            if (pixelObject.getSize() < 5 || pixelObject.getArea() < minArea) {
                return null;
            }
            Letter let = pixelObject.toColoredLetter();
            int r = let.getWidth() / 2;
            try {
                int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
                if ((ratio > 95 && ratio < 105) || equalElements(let.getGrid()[r][0], let.getGrid()[0][r]) || equalElements(let.getGrid()[r][let.getWidth() - 1], let.getGrid()[0][r]) || equalElements(let.getGrid()[r][0], let.getGrid()[let.getWidth() - 1][r]) || equalElements(let.getGrid()[r][let.getWidth() - 1], let.getGrid()[let.getWidth() - 1][r])) {
                    return new int[] { let.getLocation()[0] + r, let.getLocation()[1] + let.getWidth() };
                }

            } catch (Exception e) {
            }
            java.util.List<int[]> best = new ArrayList<int[]>();
            int h = let.getLocation()[1] + let.getHeight();

            for (int x = let.getLocation()[0]; x < let.getLocation()[0] + let.getWidth(); x++) {
                int y = let.getLocation()[1];
                int c = captcha.grid[x][y];

                if (!isBackground(c)) {

                    y++;

                    for (; y < h; y++) {

                        if (isBackground(captcha.grid[x][y])) {
                            break;
                        }
                    }

                    // if (oldy == y || h < y) continue;
                    int oldy = y;

                    for (; y < h; y++) {
                        if (!isBackground(captcha.grid[x][y]) && equalElements(c, captcha.grid[x][y])) {
                            break;
                        }
                    }
                    if (oldy == y || h < y) {
                        continue;
                    }
                    oldy = y;

                    for (; y < h; y++) {
                        if (isBackground(captcha.grid[x][y])) {
                            break;
                        }
                    }

                    if (oldy == y) {
                        continue;
                    }
                    if (y == let.getHeight() && Math.abs(let.getHeight() - let.getWidth()) > 15) {
                        continue;
                    }
                    if (best.size() > 0) {
                        if (y > best.get(0)[0]) {
                            best = new ArrayList<int[]>();
                            best.add(new int[] { x, y });
                        } else if (y == best.get(0)[1]) {
                            best.add(new int[] { x, y });
                        }
                    } else {
                        best.add(new int[] { x, y });
                    }
                }
            }
            if (best.size() == 0) {
                return null;
            } else {
                int x = 0;
                for (int[] is : best) {
                    x += is[0];
                }
                return new int[] { x / best.size(), best.get(0)[1] };
            }
        }

        private void addCircles(PixelObject pixelObject, java.util.List<PixelObject> obnew) {
            if (pixelObject.getArea() < minArea) {
                return;
            }
            int[] bounds = getBounds(pixelObject);
            int r = 0;
            if (bounds != null) {
                r = (bounds[1] - pixelObject.getLocation()[1]) / 2;

                PixelObject object = getCircle(bounds[0], bounds[1] - r, r);

                if (object != null) {
                    int ratio = object.getHeight() * 100 / object.getWidth();
                    if (ratio > 90 && ratio < 110) {
                        obnew.add(object);
                        pixelObject.del(object);
                    }
                }
            }

        }

        private static BufferedImage copyImage(BufferedImage image) {
            ColorModel colorModel = image.getColorModel();
            WritableRaster raster = image.copyData(null);
            return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
        }

        /**
         * returns true if coordinates are out of the bounds of the image
         * 
         * @param img
         * @param x
         * @param y
         * @return
         */
        private boolean outOfBounds(BufferedImage img, int x, int y) {
            if (x >= img.getWidth()) {
                return true;
            }
            if (x < 0) {
                return true;
            }
            if (y >= img.getHeight()) {
                return true;
            }
            if (y < 0) {
                return true;
            }
            return false;
        }

        /**
         * expand the circle by expanding colored pixels to their neighbors
         * 
         * @param src
         *            the image
         * @return a copy of the image
         */
        private BufferedImage expandImage(BufferedImage src) {
            BufferedImage tgt = copyImage(src);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int c = src.getRGB(x, y);
                    if (Colors.getCMYKColorDifference1(c, Color.WHITE.getRGB()) > 4) {
                        tgt.setRGB(x, y, c);
                        if (x < src.getWidth() - 1) {
                            tgt.setRGB(x, y, c);
                            if (!outOfBounds(src, x, y - 1)) {
                                tgt.setRGB(x, y - 1, c);
                            }
                            if (!outOfBounds(src, x + 1, y)) {
                                tgt.setRGB(x + 1, y, c);
                            }
                            if (!outOfBounds(src, x, y + 1)) {
                                tgt.setRGB(x, y + 1, c);
                            }
                            if (!outOfBounds(src, x - 1, y)) {
                                tgt.setRGB(x - 1, y, c);
                            }
                        }
                    }
                }
            }
            return tgt;
        }

        /**
         * returns the detected circles
         * 
         * @return
         */
        public java.util.List<PixelObject> getCircles() {
            java.util.List<PixelObject> obnew = new ArrayList<PixelObject>();
            for (PixelObject pixelObject : objectArray) {
                if (pixelObject.getArea() > minArea) {
                    addCircles(pixelObject, obnew);
                }
            }
            return obnew;
        }

        /**
         * Gets the longest part of a circle which is missing
         * 
         * @param img
         *            image containing a circle (with center in center)
         * @param r
         *            radius of circle to compare
         * @return longest missing part
         */
        private int getLongestOff(BufferedImage img, int r) {
            int xc = (int) (new Double(img.getWidth()) / 2.0d);
            int yc = (int) (new Double(img.getHeight()) / 2.0d);

            int longestOff = 0;
            int tmp = 0;

            for (Double theta = 0.0d; theta < 3 * Math.PI; theta += 0.1d) {
                double x = r * Math.cos(theta) + xc;
                double y = r * Math.sin(theta) + yc;

                int color = img.getRGB((int) x, (int) y);
                if (Colors.getCMYKColorDifference1(Color.white.getRGB(), color) > 5) {
                    tmp = 0;
                } else {
                    // TODO: limit to 1/3*2pi (radian)
                    tmp++;
                    if (tmp > longestOff) {
                        longestOff = tmp;
                    }
                }

            }
            return longestOff;

        }

        /**
         * returns the open circle
         * 
         * @return
         */
        private Letter getOpenCircle() {
            if (openCircle != null) {
                return openCircle;
            }
            objectArray = getCircles();
            Letter best = null;
            int maxLongestOff = Integer.MIN_VALUE;
            for (PixelObject pixelObject : objectArray) {
                Letter let = pixelObject.toColoredLetter();

                int tmp = getLongestOff(expandImage(let.getImage()), (int) (new Double(Math.min(let.getHeight(), let.getWidth())) / 2.0d) - 2);
                if (tmp > maxLongestOff) {
                    maxLongestOff = tmp;
                    best = let;
                }
            }
            openCircle = best;
            return best;
        }
    }

    /**
     * Solves a CaptX captcha (linkcrypt). That are the captcha where you have to click into the open circle
     * 
     * @author flubshi
     * 
     */
    public static class CaptXSolver {

        /**
         * Calculates coordinates of the open circle within the image
         * 
         * @param captchaFile
         *            image with open circle drawn on it
         * @return coordinates of the circle
         * @throws Exception
         */
        public static Point solveCaptXCaptcha(byte[] bytes) throws Exception {
            final String method = "lnkcrptwsCircles";
            if (JACMethod.hasMethod(method)) {
                final JAntiCaptcha jac = new JAntiCaptcha(method);
                final BufferedImage image = toBufferedImage(new ByteArrayInputStream(bytes));
                final Captcha captcha = Captcha.getCaptcha(image, jac);
                final Point point = getOpenCircleCentrePoint(captcha);
                return point;
            }
            return null;
        }

        private static boolean equalElements(int c, int c2) {
            return c == c2;
        }

        private static boolean isWhite(int c) {
            return c < 0 || c == 0xffffff;
        }

        /**
         * get objects with different color
         * 
         * @param grid
         * @return
         */
        public static java.util.List<PixelObject> getObjects(Captcha grid) {
            java.util.List<PixelObject> ret = new ArrayList<PixelObject>();
            java.util.List<PixelObject> merge;
            for (int x = 0; x < grid.getWidth(); x++) {
                for (int y = 0; y < grid.getHeight(); y++) {
                    int c = grid.getGrid()[x][y];
                    if (isWhite(c)) {
                        continue;
                    }
                    PixelObject n = new PixelObject(grid);
                    n.add(x, y, c);
                    merge = new ArrayList<PixelObject>();
                    for (PixelObject o : ret) {
                        if (o.isTouching(x, y, true, 5, 5) && equalElements(c, o.getMostcolor())) {
                            merge.add(o);
                        }
                    }
                    if (merge.size() == 0) {
                        ret.add(n);
                    } else if (merge.size() == 1) {
                        merge.get(0).add(n);
                    } else {
                        for (PixelObject po : merge) {
                            ret.remove(po);
                            n.add(po);
                        }
                        ret.add(n);
                    }
                }
            }
            return ret;
        }

        public static Point getOpenCircleCentrePoint(Captcha captcha) throws InterruptedException {
            java.util.List<PixelObject> ob = getObjects(captcha);
            // delete the lines
            for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
                PixelObject pixelObject = iterator.next();
                int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
                if (ratio > 110 || ratio < 90) {
                    iterator.remove();
                }
            }

            Circle circle = new Circle(captcha, ob);
            circle.inBorder = 3;
            circle.outBorder = 2;
            circle.isElementColor = new Comparator<Integer>() {

                public int compare(Integer o1, Integer o2) {
                    return o1.equals(o2) ? 1 : 0;
                }
            };
            Letter openCircle = circle.getOpenCircle();
            int x = openCircle.getLocation()[0] + (openCircle.getWidth() / 2);
            int y = openCircle.getLocation()[1] + (openCircle.getHeight() / 2);
            return new Point(x, y);
        }
    }

}