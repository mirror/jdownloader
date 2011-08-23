//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.http.RandomUserAgent;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+" }, flags = { 0 })
public class LnkCrptWs extends PluginForDecrypt {

    public static class KeyCaptcha {
        private static Object                LOCK = new Object();
        private final Browser                br;
        private Form                         FORM;
        private HashMap<String, String>      PARAMS;
        private Browser                      rcBr;
        private String                       SERVERSTRING;
        private String                       DLURL;
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

        public synchronized String handleAuto(final String parameter) throws Exception {
            DLURL = parameter;
            parse();
            load();
            // Sobald ein Mausklickevent im Fenster stattfindet:
            // Der Bereich der in A geparst wird, ist im JS obfuscated!
            // final String[] A =
            // rcBr.getRegex("\"(.*?)\".*?sscl\\.s_s_c_fsmcheck2\\((.*?),(.*?).*?\"(.*)\"").getRow(0);
            final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + "02njhd8322");
            String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
            mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();
            rcBr.getPage(mmUrlReq);
            /* Bilderdownload und Verarbeitung */
            // fragmentierte Puzzleteile
            sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));
            // fragmentiertes Hintergrundbild
            sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));

            final boolean stable = true;
            String out;

            if (!stable) {
                final KeyCaptchaDialog vC = new KeyCaptchaDialog(0, "KeyCaptcha", new String[] { sscStc[1], stImgs[1] }, fmsImg, null);
                vC.displayDialog();
                out = vC.getReturnValue();
            } else {
                final KeyCaptchaShowDialog vC = new KeyCaptchaShowDialog(new String[] { sscStc[1], stImgs[1] }, fmsImg);
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
            SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + out + "&cP=");
            out = rcBr.getPage(SERVERSTRING);
            out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
            if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) { return null; }
            return out;
        }

        private void load() throws IOException, PluginException {
            rcBr = br.cloneBrowser();
            rcBr.setFollowRedirects(true);
            rcBr.getHeaders().put("Referer", DLURL);
            rcBr.getHeaders().put("Pragma", null);
            rcBr.getHeaders().put("Cache-Control", null);
            if (PARAMS.containsKey("src")) {
                rcBr.getPage(PARAMS.get("src"));
                PARAMS.put("src", DLURL);
                SERVERSTRING = rcBr.getRegex("var _13=\"(.*?)\"").getMatch(0);
            } else {
                JDLogger.getLogger().severe("Keycaptcha Module fails: " + rcBr.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (SERVERSTRING != null && SERVERSTRING.startsWith("https")) {
                SERVERSTRING = SERVERSTRING + Encoding.urlEncode(DLURL) + "&r=" + Math.random();
                rcBr.getPage(SERVERSTRING);
                SERVERSTRING = null;

                final String picString = rcBr.getRegex("_3\\.innerHTML=(.*?)var _5").getMatch(0);
                if (picString != null) {
                    final String pictures[] = new Regex(picString, "(/js/[a-z-]+\\.(gif|png))").getColumn(-1);
                    for (final String template : pictures) {
                        rcBr.cloneBrowser().openGetConnection(template);
                    }
                }

                PARAMS.put("s_s_c_web_server_sign4", rcBr.getRegex("s_s_c_web_server_sign4=\"(.*?)\"").getMatch(0));
                SERVERSTRING = rcBr.getRegex("_5\\.setAttribute\\(\"src\",\"(.*?)\"").getMatch(0);
            }
            if (SERVERSTRING != null && SERVERSTRING.startsWith("https")) {
                SERVERSTRING = SERVERSTRING + Encoding.urlEncode(getGjsParameter()) + "&r=" + Math.random();
                rcBr.getPage(SERVERSTRING);

                final String[] bla = { "/js/i.png", "/js/rightsolution.png", "/js/q.png" };
                for (final String template : bla) {
                    rcBr.cloneBrowser().openGetConnection(template);
                }

                PARAMS.put("s_s_c_web_server_sign3", rcBr.getRegex("s_s_c_setnewws\\(\"(.*?)\",").getMatch(0));
                stImgs = rcBr.getRegex("st_imgs=s_s_c_loader\\.s_s_c_getimagest\\(\'(.*?)\',\'(.*?)\',(.*?),(.*?)\\)").getRow(0);
                sscStc = rcBr.getRegex("s_s_c_stc=s_s_c_loader\\.s_s_c_getimagest\\(\'(.*?)\',\'(.*?)\',(.*?),(.*?)\\)").getRow(0);
                SERVERSTRING = rcBr.getRegex("s_s_c_resscript\\.setAttribute\\(\'src\',\'(.*?)\'").getMatch(0) + Encoding.urlEncode(getGjsParameter());
            } else {
                JDLogger.getLogger().severe("Keycaptcha Module fails: " + rcBr.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

        }

        private void parse() throws IOException, PluginException {
            FORM = null;
            if (br.containsHTML("(KeyCAPTCHA|www\\.keycaptcha\\.com)")) {
                for (final Form f : br.getForms()) {
                    if (f.containsHTML("var s_s_c_user_id = \'\\d+\'")) {
                        FORM = f;
                        break;
                    }
                }
                if (FORM == null) {
                    JDLogger.getLogger().warning("KeyCAPTCHA form couldn't be found...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                        JDLogger.getLogger().warning("KeyCaptcha values couldn't be found...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        PARAMS.put("src", FORM.getRegex("src=\'(.*?)\'").getMatch(0));
                    }
                }
            } else {
                JDLogger.getLogger().warning("KeyCAPTCHA handling couldn't be found...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                final int incr = Math.round(arg1.length() / 2);
                final long modu = (int) Math.pow(2, 31);
                final int salt = Integer.parseInt(arg0.substring(arg0.length() - 8, arg0.length()), 16);
                arg0 = arg0.substring(0, arg0.length() - 8);
                prand += salt;
                while (prand.length() > 9) {
                    prand = String.valueOf(Integer.parseInt(prand.substring(0, 9), 10) + Integer.parseInt(prand.substring(9, Math.min(prand.length(), 14)), 10)) + prand.substring(Math.min(prand.length(), 14), prand.length());
                }
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
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
                final int incr = Math.round(arg1.length() / 2);
                final long modu = (int) Math.pow(2, 31);
                if (mult < 2) { return null; }
                final int salt = (int) Math.round(Math.random() * 1000000000) % 100000000;
                prand += salt;
                while (prand.length() > 9) {
                    prand = String.valueOf(Integer.parseInt(prand.substring(0, 9), 10) + Integer.parseInt(prand.substring(9, Math.min(prand.length(), 14)), 10)) + prand.substring(Math.min(prand.length(), 14), prand.length());
                }
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
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
            final String outst = sscFsmCheckFour(arg0, arg1.substring(arg1.length() - 30, arg1.length()));
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

        public KeyCaptchaDialog(final int flag, final String title, final String[] imageUrl, final LinkedHashMap<String, int[]> coordinates, final String cancelOption) {
            super(flag | Dialog.STYLE_HIDE_ICON, title, null, null, null);
            this.imageUrl = imageUrl;
            this.coordinates = coordinates;
        }

        @Override
        protected String createReturnValue() {
            if (Dialog.isOK(getReturnmask())) { return getPosition(drawPanel); }
            return null;
        }

        private String getPosition(final JLayeredPane drawPanel) {
            int i = 0;
            String positions = "";
            for (final Component comp : drawPanel.getComponents()) {
                if (comp.getMouseListeners().length == 0) {
                    continue;
                }
                final Point p = comp.getLocation();
                positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
                i++;
            }
            return positions;
        }

        public void handleCoordinates(final LinkedHashMap<String, int[]> arg0) {
            kcImages = new BufferedImage[coordinates.size()];
        }

        @Override
        public JComponent layoutDialogContent() {
            loadImage(imageUrl);
            handleCoordinates(coordinates);
            drawPanel = new JLayeredPane();
            drawPanel.setLayout(new BorderLayout());
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
                drawPanel.add(new KeyCaptchaDragPieces(kcImages[i], offset, sampleImg), null, new Integer(JLayeredPane.DEFAULT_LAYER + i));
                offset += 4;
            }
            drawPanel.add(new KeyCaptchaDrawBackgroundPanel(kcImages[0]), null, new Integer(JLayeredPane.DEFAULT_LAYER + kcImages.length));
            dialog.add(drawPanel);
            return drawPanel;
        }

        public void loadImage(final String[] imagesUrl) {
            int i = 0;
            IMAGE = new Image[imagesUrl.length];
            final MediaTracker mt = new MediaTracker(dialog);
            for (final String imgUrl : imagesUrl) {
                try {
                    IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new URL(imgUrl));
                } catch (final MalformedURLException e) {
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
            final int[] bgCoord = coordinates.get("backGroundImage");
            while (cik < bgCoord.length) {
                go.drawImage(IMAGE[0], bgCoord[cik], bgCoord[cik + 1], bgCoord[cik] + bgCoord[cik + 2], bgCoord[cik + 1] + bgCoord[cik + 3], curx, 0, curx + bgCoord[cik + 2], bgCoord[cik + 3], dialog);
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
                    if (sX + sW > IMAGE[1].getWidth(dialog) || sY + sH > IMAGE[1].getHeight(dialog)) {
                        continue;
                    }
                    if (dX + sW > w || dY + sH > h) {
                        continue;
                    }
                    if (sW == 0 || sH == 0) {
                        continue;
                    }
                    // Puzzlebild erstellen
                    go.drawImage(IMAGE[1], dX, dY, dX + sW, dY + sH, sX, sY, sX + sW, sY + sH, dialog);
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
                setBounds(0, 0, image.getWidth() + 10, image.getHeight() + 10);
                setBorder(BorderFactory.createLineBorder(Color.black));
                setLocation(450 - image.getWidth() - 10, 0);
                setBackground(Color.white);
            }
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (IMAGE != null) {
                g.drawImage(IMAGE, 0, 0, this);
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
        private static final long            serialVersionUID = 1L;
        private Image[]                      IMAGE;
        private BufferedImage[]              kcImages;
        private LinkedHashMap<String, int[]> rectanglecCoordinates;
        private Graphics                     go;
        private int                          kcSampleImg;
        private final ActionListener         AL;
        public String                        POSITION;
        private final JFrame                 FRAME            = this;

        public KeyCaptchaShowDialog(final String[] arg0, final LinkedHashMap<String, int[]> arg1) throws Exception {
            super("KeyCaptcha");
            loadImage(arg0);
            handleCoordinates(arg1);
            final JLayeredPane drawPanel = new JLayeredPane();
            drawPanel.setLayout(new BorderLayout());
            FRAME.setSize(new Dimension(450, 180));
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
                drawPanel.add(new KeyCaptchaDragPieces(kcImages[i], offset, sampleImg), null, new Integer(JLayeredPane.DEFAULT_LAYER + i));
                offset += 4;
            }
            drawPanel.add(new KeyCaptchaDrawBackgroundPanel(kcImages[0]), null, new Integer(JLayeredPane.DEFAULT_LAYER + kcImages.length));
            AL = new ActionListener() {

                public void actionPerformed(final ActionEvent e) {
                    POSITION = getPosition(drawPanel);
                    try {
                        FRAME.dispose();
                    } finally {
                        synchronized (KeyCaptcha.LOCK) {
                            KeyCaptcha.LOCK.notify();
                        }
                    }
                }
            };

            final JButton button = new JButton("OK");
            final JPanel p = new JPanel();
            button.addActionListener(AL);
            p.add(button);

            getContentPane().add(p, BorderLayout.PAGE_END);
            getContentPane().add(drawPanel, BorderLayout.CENTER);

            toFront();
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
            pack();
            // setResizable(false);
            setVisible(true);
        }

        private String getPosition(final JLayeredPane drawPanel) {
            int i = 0;
            String positions = "";
            for (final Component comp : drawPanel.getComponents()) {
                if (comp.getMouseListeners().length == 0) {
                    continue;
                }
                final Point p = comp.getLocation();
                positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
                i++;
            }
            return positions;
        }

        public void handleCoordinates(final LinkedHashMap<String, int[]> arg0) {
            rectanglecCoordinates = new LinkedHashMap<String, int[]>();
            for (final Map.Entry<String, int[]> entry : arg0.entrySet()) {
                rectanglecCoordinates.put(entry.getKey(), entry.getValue());
            }
            kcImages = new BufferedImage[rectanglecCoordinates.size()];
        }

        public void loadImage(final String[] imagesUrl) throws MalformedURLException {
            int i = 0;
            IMAGE = new Image[imagesUrl.length];
            final MediaTracker mt = new MediaTracker(this);
            for (final String imgUrl : imagesUrl) {
                IMAGE[i] = Toolkit.getDefaultToolkit().getImage(new URL(imgUrl));
                if (IMAGE[i] != null) {
                    repaint();
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
            final int[] bgCoord = rectanglecCoordinates.get("backGroundImage");
            while (cik < bgCoord.length) {
                go.drawImage(IMAGE[0], bgCoord[cik], bgCoord[cik + 1], bgCoord[cik] + bgCoord[cik + 2], bgCoord[cik + 1] + bgCoord[cik + 3], curx, 0, curx + bgCoord[cik + 2], bgCoord[cik + 3], this);
                curx = curx + bgCoord[cik + 2];
                cik = cik + 4;
            }
        }

        private void makePieces() {
            final Object[] key = rectanglecCoordinates.keySet().toArray();
            int pieces = 1;
            for (final Object element : key) {
                if (element.equals("backGroundImage")) {
                    continue;
                }
                final int[] imgcs = rectanglecCoordinates.get(element);
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
                    if (sX + sW > IMAGE[1].getWidth(this) || sY + sH > IMAGE[1].getHeight(this)) {
                        continue;
                    }
                    if (dX + sW > w || dY + sH > h) {
                        continue;
                    }
                    if (sW == 0 || sH == 0) {
                        continue;
                    }
                    // Puzzlebild erstellen
                    go.drawImage(IMAGE[1], dX, dY, dX + sW, dY + sH, sX, sY, sX + sW, sY + sH, this);
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

    private final HashMap<String, String> map = new HashMap<String, String>();

    public LnkCrptWs(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        setBrowserExclusive();
        prepareBrowser(RandomUserAgent.generate());
        final String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);
        br.getPage("http://linkcrypt.ws/dir/" + containerId);
        if (br.containsHTML("Error 404 - Ordner nicht gefunden")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        br.cloneBrowser().openGetConnection("http://linkcrypt.ws/js/jquery.js");
        // Different captcha types
        boolean valid = true;

        if (br.containsHTML("<\\!-- KeyCAPTCHA code")) {
            final KeyCaptcha kc = new KeyCaptcha(br);
            for (int i = 0; i <= 3; i++) {
                final String result = kc.handleAuto(parameter);
                if (result == null) {
                    continue;
                }
                final Form kcform = br.getForm(0);
                kcform.put("capcode", result);
                br.submitForm(kcform);
                if (!br.containsHTML("<\\!-- KeyCAPTCHA code")) {
                    break;
                }
            }
        }
        if (br.containsHTML("<\\!-- KeyCAPTCHA code")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
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
                            br.cloneBrowser().getDownload(file, url);
                            final Point p = UserIO.getInstance().requestClickPositionDialog(file, "LinkCrypt.ws | " + String.valueOf(max_attempts - attempts), capDescription);
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
        final String[] containers = br.getRegex("eval(.*?)\n").getColumn(0);
        for (final String c : containers) {
            Object result = new Object();
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(c);
            } catch (final Throwable e) {
            }
            final String code = result.toString();
            String[] row = new Regex(code, "href=\"([^\"]+)\"[^>]*>.*?<img.*?image/(.*?)\\.").getRow(0);
            if (row == null && br.containsHTML("dlc.png")) {
                row = new Regex(code, "href=\"(http.*?)\".*?(dlc)").getRow(0);
            }
            if (row != null) {
                map.put(row[1], row[0]);
            }
        }

        final Form preRequest = br.getForm(0);
        if (preRequest != null) {
            final String url = preRequest.getRegex("http://.*/captcha\\.php\\?id=\\d+").getMatch(-1);
            if (url != null) {
                br.cloneBrowser().openGetConnection(url);
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
                                            decryptedLinks.add(createDownloadlink(versch));
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
                Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
            }
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    @Override
    protected String getInitials() {
        return "LC";
    }

    private void prepareBrowser(final String userAgent) {
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Language", "en-EN");
        br.getHeaders().put("User-Agent", userAgent);
        br.getHeaders().put("Connection", null);
    }
}
