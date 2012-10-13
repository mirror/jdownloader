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
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
import javax.swing.event.MouseInputAdapter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+" }, flags = { 0 })
public class LnkCrptWs extends PluginForDecrypt {

    public static class SolveMedia {
        private final Browser br;
        private String        challenge;
        private String        chId;
        private String        captchaAddress;
        private String        server;
        private String        path;
        private Browser       smBr;
        private Form          verify;
        private boolean       secure   = false;
        private boolean       noscript = true;

        public SolveMedia(final Browser br) {
            this.br = br;
        }

        public File downloadCaptcha(final File captchaFile) throws Exception {
            this.load();
            URLConnectionAdapter con = null;
            try {
                captchaAddress = captchaAddress.replaceAll("%0D%0A", "").trim();
                Browser.download(captchaFile, con = this.smBr.openGetConnection(this.server + this.captchaAddress));
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
            this.smBr = this.br.cloneBrowser();
            this.getChallenge();
            this.setServer();
            this.setPath(noscript);
            this.smBr.getPage(this.server + this.path + this.challenge);
            if (this.noscript) {
                this.verify = smBr.getForm(0);
                this.captchaAddress = this.smBr.getRegex("<img src=\"(/papi/media\\?c=[^\"]+)").getMatch(0);
                if (this.verify == null) throw new Exception("SolveMedia Module fails");
            } else {
                this.chId = this.smBr.getRegex("\"chid\"\\s+?:\\s+?\"(.*?)\",").getMatch(0);
                this.captchaAddress = this.chId != null ? this.server + "/papi/media?c=" + this.chId : null;
            }
            if (this.captchaAddress == null) throw new Exception("SolveMedia Module fails");
        }

        private void getChallenge() {
            this.challenge = this.br.getRegex("http://api\\.solvemedia\\.com/papi/_?challenge\\.script\\?k=(.{32})").getMatch(0);
            if (secure) this.challenge = this.br.getRegex("ckey:\'([\\w\\-\\.]+)\'").getMatch(0);
        }

        public String verify(String code) throws Exception {
            if (!this.noscript) return this.chId;

            /** FIXME stable Browser Bug --> Form action handling */
            String url = this.smBr.getURL();
            url = url.substring(0, url.indexOf("media?c="));
            this.verify.setAction((url == null ? "" : url) + this.verify.getAction());

            this.verify.put("adcopy_response", code);
            this.smBr.submitForm(verify);
            String verifyUrl = this.smBr.getRegex("URL=(http[^\"]+)").getMatch(0);
            if (verifyUrl == null) return null;
            if (secure) verifyUrl = verifyUrl.replaceAll("http://", "https://");
            try {
                this.smBr.getPage(verifyUrl);
            } catch (Throwable e) {
                throw new Exception("SolveMedia Module fails");
            }
            return this.smBr.getRegex("id=gibberish>([^<]+)").getMatch(0);
        }

        /**
         * @default false
         * @parameter if true uses "https://api-secure.solvemedia.com" instead of "http://api.solvemedia.com"
         */
        public void setSecure(boolean secure) {
            if (secure) this.secure = true;
        }

        /**
         * @default true
         * @parameter if false uses "_challenge.js" instead of "challenge.noscript" as url path
         */
        public void setNoscript(boolean noscript) {
            if (!noscript) this.noscript = false;
        }

        private void setServer() {
            this.server = "http://api.solvemedia.com";
            if (secure) this.server = "https://api-secure.solvemedia.com";
        }

        private void setPath(boolean b) {
            this.path = "/papi/challenge.noscript?k=";
            if (!noscript) this.path = "/papi/_challenge.js?k=";
        }

        public String getChallengeUrl() {
            return this.server + this.path;
        }

        public String getChallengeId() {
            return this.challenge;
        }

        public String getCaptchaUrl() {
            return this.captchaAddress;
        }

    }

    public static class KeyCaptcha {
        private static Object LOCK = new Object();

        public static void prepareBrowser(final Browser kc, final String a) {
            kc.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
            kc.getHeaders().put("Referer", DLURL);
            kc.getHeaders().put("Pragma", null);
            kc.getHeaders().put("Cache-Control", null);
            kc.getHeaders().put("Accept", a);
            kc.getHeaders().put("Accept-Charset", "UTF-8,*");
            kc.getHeaders().put("Accept-Language", "en-us");
            kc.getHeaders().put("DNT", "1");
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
            if (rev < 10000) { return true; }
            return false;
        }

        private void load() throws Exception {
            rcBr = br.cloneBrowser();
            rcBr.setFollowRedirects(true);
            prepareBrowser(rcBr, "*/*");
            if (PARAMS.containsKey("src")) {
                rcBr.getPage(PARAMS.get("src"));
                PARAMS.put("src", DLURL);
                SERVERSTRING = rcBr.getRegex("var _13=\"(.*?)\"").getMatch(0);
            }
            if (SERVERSTRING != null && SERVERSTRING.startsWith("https")) {
                SERVERSTRING = SERVERSTRING + Encoding.urlEncode(DLURL) + "&r=" + Math.random();
                rcBr.getPage(SERVERSTRING);

                SERVERSTRING = null;
                PARAMS.put("s_s_c_web_server_sign4", rcBr.getRegex("s_s_c_web_server_sign4=\"(.*?)\"").getMatch(0));
                SERVERSTRING = rcBr.getRegex("\\.setAttribute\\(\"src\",\"(.*?)\"").getMatch(0);
            }
            if (SERVERSTRING != null && SERVERSTRING.startsWith("https")) {
                SERVERSTRING = SERVERSTRING + Encoding.urlEncode(getGjsParameter()) + "&r=" + Math.random();
                rcBr.getPage(SERVERSTRING);

                PARAMS.put("s_s_c_web_server_sign3", rcBr.getRegex("s_s_c_setnewws\\(\"(.*?)\",").getMatch(0));
                stImgs = rcBr.getRegex("\\(\'([0-9a-f]+)\',\'(http.*?\\.png)\',(.*?),(true|false)\\)").getRow(0);
                sscStc = rcBr.getRegex("\\(\'([0-9a-f]+)\',\'(http.*?\\.png)\',(.*?),(true|false)\\)").getRow(1);
                SERVERSTRING = rcBr.getRegex("s_s_c_resscript\\.setAttribute\\(\'src\',\'(.*?)\'").getMatch(0) + Encoding.urlEncode(getGjsParameter());
                if (stImgs == null || sscStc == null || SERVERSTRING == null) throw new Exception("KeyCaptcha Module fails");
            } else {
                throw new Exception("KeyCaptcha Module fails");
            }
        }

        private void parse() throws Exception {
            FORM = null;
            if (br.containsHTML("(KeyCAPTCHA|www\\.keycaptcha\\.com)")) {
                for (final Form f : br.getForms()) {
                    if (f.containsHTML("var s_s_c_user_id = \'\\d+\'")) {
                        FORM = f;
                        break;
                    }
                }
                if (FORM == null) {
                    throw new Exception("KeyCaptcha form couldn't be found");
                } else {
                    PARAMS = new HashMap<String, String>();
                    final String[][] parameter = FORM.getRegex("(s_s_c_\\w+) = \'(.*?)\'").getMatches();
                    for (final String[] para : parameter) {
                        if (para.length != 2) {
                            continue;
                        }
                        PARAMS.put(para[0], para[1]);
                    }
                    if (PARAMS == null || PARAMS.size() == 0) {
                        throw new Exception("KeyCaptcha values couldn't be found");
                    } else {
                        PARAMS.put("src", FORM.getRegex("src=\'(.*?)\'").getMatch(0));
                    }
                }
            } else {
                throw new Exception("KeyCaptcha handling couldn't be found");
            }
        }

        public synchronized String showDialog(final String parameter) throws Exception {
            DLURL = parameter;
            try {
                parse();
                load();
            } catch (final Throwable e) {
                System.out.println(rcBr.getHttpConnection() + "\n" + e.getMessage());
                throw new Exception(e.getMessage());
            } finally {
                try {
                    rcBr.getHttpConnection().disconnect();
                } catch (final Throwable e) {
                }
            }

            /* Bilderdownload und Verarbeitung */
            // fragmentierte Puzzleteile
            sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));
            // fragmentiertes Hintergrundbild
            sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));

            if (sscStc == null || sscStc.length == 0 || stImgs == null || stImgs.length == 0 || fmsImg == null || fmsImg.size() == 0) { return "CANCEL"; }

            String out;

            if (!isStableEnviroment()) {

                final KeyCaptchaDialog vC = new KeyCaptchaDialog(0, "KeyCaptcha - " + br.getHost(), new String[] { stImgs[1], sscStc[1] }, fmsImg, null);
                vC.displayDialog();
                out = vC.getReturnValue();
                if (vC.getReturnmask() == 4) {
                    out = "CANCEL";
                }

            } else {
                final KeyCaptchaShowDialog vC = new KeyCaptchaShowDialog("KeyCaptcha - " + br.getHost(), new String[] { stImgs[1], sscStc[1] }, fmsImg);
                // Warten bis der KeyCaptcha-Dialog geschlossen ist
                synchronized (LOCK) {
                    try {
                        LOCK.wait();
                    } catch (final InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                out = vC.POSITION;
            }
            if (out == null) { return null; }
            if (out.equals("CANCEL")) {
                System.out.println("KeyCaptcha: User aborted captcha dialog.");
                return out;
            }

            final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + "02njhd8322");
            String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
            mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();
            rcBr.getPage(mmUrlReq);

            SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + out + "&cP=");
            out = rcBr.getPage(SERVERSTRING);
            out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
            // validate final response
            if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) { return null; }
            return out;
        }

        private String sscFsmCheckFour(String arg0, final String arg1) {
            try {
                if (arg0 == null || arg0.length() < 8 || arg1 == null) { return null; }
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
                if (arg1 == null) { return null; }
                String prand = "";
                for (int i = 0; i < arg1.length(); i++) {
                    prand += arg1.codePointAt(i);
                }
                final int sPos = (int) Math.floor(prand.length() / 5);
                final int mult = Integer.parseInt(String.valueOf(prand.charAt(sPos) + "" + prand.charAt(sPos * 2) + "" + prand.charAt(sPos * 3) + "" + prand.charAt(sPos * 4) + "" + prand.charAt(sPos * 5 - 1)));
                final int incr = Math.round(arg1.length() / 3);
                final long modu = (int) Math.pow(2, 31);
                if (mult < 2) { return null; }
                final int salt = (int) Math.round(Math.random() * 1000000000) % 100000000;
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

        public KeyCaptchaDialog(final int flag, final String title, final String[] imageUrl, final LinkedHashMap<String, int[]> coordinates, final String cancelOption) {
            super(flag | Dialog.STYLE_HIDE_ICON | Dialog.LOGIC_COUNTDOWN, title, null, null, null);
            setCountdownTime(120);
            this.imageUrl = imageUrl;
            this.coordinates = coordinates;
            dimensions = new Dimension(465, 250);
        }

        @Override
        protected String createReturnValue() {
            if (Dialog.isOK(getReturnmask())) { return getPosition(drawPanel); }
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

            for (int i = 1; i < kcImages.length; i++) {
                if (kcImages[i] == null) {
                    continue;
                } else if (i == kcSampleImg) {
                    sampleImg = true;
                } else {
                    sampleImg = false;
                }
                drawPanel.add(new KeyCaptchaDragPieces(kcImages[i], offset, sampleImg), new Integer(JLayeredPane.DEFAULT_LAYER) + i, new Integer(JLayeredPane.DEFAULT_LAYER) + i);

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
                    fragmentedPic = JDUtilities.getResourceFile("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic.deleteOnExit();
                    Browser.download(fragmentedPic, dlpic.openGetConnection(imgUrl));
                    /* TODO: replace with ImageProvider.read in future */
                    IMAGE[i] = ImageIO.read(fragmentedPic);
                    // IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new
                    // URL(imgUrl));
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
                kcImages[pieces] = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
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

        public KeyCaptchaDragPieces(final BufferedImage image, final int offset, final boolean sampleImg) {
            IMAGE = image;
            MOUSEADAPTER = new MouseInputAdapter() {
                private Point p1;

                @Override
                public void mouseDragged(final MouseEvent e) {
                    final Point p2 = e.getPoint();
                    final Point loc = getLocation();
                    loc.translate(p2.x - p1.x, p2.y - p1.y);
                    setLocation(loc);
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    p1 = e.getPoint();
                    setBorder(BorderFactory.createLineBorder(Color.black));
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    p1 = e.getPoint();
                    setBorder(BorderFactory.createEmptyBorder());
                }
            };
            k = 0;
            setOpaque(true);
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

    private static class KeyCaptchaShowDialog extends JFrame {
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

        public KeyCaptchaShowDialog(final String title, final String[] arg0, final LinkedHashMap<String, int[]> arg1) throws Exception {
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
            for (int i = 1; i < kcImages.length; i++) {
                if (kcImages[i] == null) {
                    continue;
                } else if (i == kcSampleImg) {
                    sampleImg = true;
                } else {
                    sampleImg = false;
                }
                drawPanel.add(new KeyCaptchaDragPieces(kcImages[i], offset, sampleImg), new Integer(JLayeredPane.DEFAULT_LAYER + i), new Integer(JLayeredPane.DEFAULT_LAYER + i));
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

            if (SwingGui.getInstance() == null) {
                setLocation(Screen.getCenterOfComponent(null, this));
            } else if (SwingGui.getInstance().getMainFrame().getExtendedState() == 1 || !SwingGui.getInstance().getMainFrame().isVisible()) {
                setLocation(Screen.getDockBottomRight(this));
            } else {
                setLocation(Screen.getCenterOfComponent(SwingGui.getInstance().getMainFrame(), this));
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
                    fragmentedPic = JDUtilities.getResourceFile("captchas/" + imgUrl.substring(imgUrl.lastIndexOf("/") + 1));
                    fragmentedPic.deleteOnExit();
                    Browser.download(fragmentedPic, dlpic.openGetConnection(imgUrl));
                    /* TODO: replace with ImageProvider.read in future */
                    IMAGE[i] = ImageIO.read(fragmentedPic);
                    // IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new
                    // URL(imgUrl));
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
                kcImages[pieces] = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
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
            zx = (int) z[x];
            y = zx + this.y & 0xff;
            zy = (int) z[y];
            this.x = x;
            this.y = y;
            z[y] = (byte) (zx & 0xff);
            z[x] = (byte) (zy & 0xff);
            return (int) z[(zx + zy & 0xff)];
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
                p = (int) z[n];
                m = m + b[w] + p & 0xff;
                o = (int) z[m];
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
                c[di] = (byte) (((int) a[si] ^ A()) & 0xff);
            }
        }

        public byte[] D(final byte[] a, final byte[] b) {
            B(a);
            final byte[] dest = new byte[b.length];
            C(b, 0, dest, 0, b.length);
            return dest;
        }

    }

    public static String IMAGEREGEX(final String b) {
        final KeyCaptchaShowDialogTwo v = new KeyCaptchaShowDialogTwo();
        final byte[] o = JDHash.getMD5(Encoding.Base64Decode("Yzg0MDdhMDhiM2M3MWVhNDE4ZWM5ZGM2NjJmMmE1NmU0MGNiZDZkNWExMTRhYTUwZmIxZTEwNzllMTdmMmI4Mw==") + JDHash.getMD5("V2UgZG8gbm90IGVuZG9yc2UgdGhlIHVzZSBvZiBKRG93bmxvYWRlci4=")).getBytes();
        if (b != null) { return new String(v.D(o, JDHexUtils.getByteArray(b))); }
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
        prepareBrowser(RandomUserAgent.generate());
        final String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);
        parameter = "http://linkcrypt.ws/dir/" + containerId;
        br.getPage(parameter);
        if (br.containsHTML("<title>Linkcrypt\\.ws // Error 404</title>")) {
            logger.info("This link might be offline: " + parameter);
            final String additional = br.getRegex("<h2>\r?\n?(.*?)<").getMatch(0);
            if (additional != null) {
                logger.info(additional);
            }
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        URLConnectionAdapter con = null;
        try {
            con = br.cloneBrowser().openGetConnection("http://linkcrypt.ws/js/jquery.js");
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        // Different captcha types
        boolean valid = true;

        if (br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) {
            KeyCaptcha kc;
            for (int i = 0; i <= 3; i++) {
                kc = new KeyCaptcha(br);
                final String result = kc.showDialog(parameter);
                if (result == null) {
                    continue;
                }
                if (result == "CANCEL") { throw new DecrypterException(DecrypterException.CAPTCHA); }
                final Form kcform = br.getForm(0);
                kcform.put("capcode", result);
                br.submitForm(kcform);
                br.postPage(parameter, "capcode=" + Encoding.urlEncode(result) + "&=Enter+Folder");
                if (!br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) {
                    break;
                }
            }
        }
        if (br.containsHTML("<\\!\\-\\- KeyCAPTCHA code")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        if (br.containsHTML("CaptX|TextX")) {
            final int max_attempts = 3;
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
                            file.deleteOnExit();
                            br.cloneBrowser().getDownload(file, url);
                            final Point p = UserIO.getInstance().requestClickPositionDialog(file, "LinkCrypt.ws | " + String.valueOf(max_attempts - attempts), capDescription);
                            if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                            captcha.put("x", p.x + "");
                            captcha.put("y", p.y + "");
                            br.submitForm(captcha);
                            if (!br.containsHTML("(Our system could not identify you as human beings\\!|Your choice was wrong\\! Please wait some seconds and try it again\\.)")) {
                                valid = true;
                            } else {
                                br.getPage("http://linkcrypt.ws/dir/" + containerId);
                            }
                        }
                    }
                }
            }
        }
        if (!valid) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        // check for a password. Store latest password in DB
        Form password = br.getForm(0);
        if (password != null && password.hasInputFieldByName("password")) {
            String latestPassword = getPluginConfig().getStringProperty("PASSWORD");
            if (latestPassword != null) {
                password.put("password", latestPassword);
                br.submitForm(password);
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
        if (password != null && password.hasInputFieldByName("password")) { throw new DecrypterException(DecrypterException.PASSWORD); }
        // Look for containers
        String[] containers = br.getRegex("eval(.*?)\n").getColumn(0);
        final String tmpc = br.getRegex("<div id=\"containerfiles\"(.*?)</script>").getMatch(0);
        if (tmpc != null) {
            containers = new Regex(tmpc, "eval(.*?)\n").getColumn(0);
        }
        for (final String c : containers) {
            Object result = new Object();
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(c);
            } catch (final Throwable e) {
            }
            final String code = result.toString();
            String[] row = new Regex(code, "href=\"(http.*?)\".*?(dlc|ccf|rsdf)").getRow(0);
            if (row == null) {
                row = new Regex(code, "href=\"([^\"]+)\"[^>]*>.*?<img.*?image/(.*?)\\.").getRow(0);
            }
            if (row != null) {
                map.put(row[1], row[0]);
            }
        }

        final Form preRequest = br.getForm(0);
        if (preRequest != null) {
            final String url = preRequest.getRegex("http://.*/captcha\\.php\\?id=\\d+").getMatch(-1);
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

        File container = null;
        if (map.containsKey("dlc")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc", true);
            if (!container.exists()) {
                container.createNewFile();
            }
            br.cloneBrowser().getDownload(container, map.get("dlc"));
        } else if (map.containsKey("ccf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf", true);
            if (!container.exists()) {
                container.createNewFile();
            }
            br.cloneBrowser().getDownload(container, map.get("ccf"));
        } else if (map.containsKey("rsdf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf", true);
            if (!container.exists()) {
                container.createNewFile();
            }
            br.cloneBrowser().getDownload(container, map.get("rsdf"));
        }
        if (container != null) {
            // container available
            logger.info("Container found: " + container);
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            if (decryptedLinks.size() > 0) { return decryptedLinks; }
        }
        /* we have to open the normal page for weblinks */
        if (br.containsHTML("BlueHeadLine.*?>(Weblinks)<")) {
            br.getPage("http://linkcrypt.ws/dir/" + containerId);
            logger.info("ContainerID is null, trying webdecryption...");
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
                                final String[] evals = clone.getRegex("eval(.*?)\n").getColumn(0);
                                for (final String c : evals) {
                                    Object result = new Object();
                                    final ScriptEngineManager manager = new ScriptEngineManager();
                                    final ScriptEngine engine = manager.getEngineByName("javascript");
                                    try {
                                        result = engine.eval(c);
                                    } catch (final Throwable e) {
                                    }
                                    final String code = result.toString();
                                    if (code.contains("ba2se") || code.contains("premfree")) {
                                        String versch;
                                        versch = new Regex(code, "ba2se='(.*?)'").getMatch(0);
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
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.info("No links found, let's see if CNL2 is available!");
            if (map.containsKey("cnl")) {
                LocalBrowser.openDefaultURL(new URL(parameter));
                throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
            }
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /**
     * TODO: can be removed with next major update cause of recaptcha change
     */

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

}