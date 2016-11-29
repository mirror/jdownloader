package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.captcha.SkipRequest;
import jd.gui.swing.jdgui.JDGui;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.swing.MigPanel;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.statistics.StatsManager;

public abstract class RecaptchaV1Handler {

    public static String load(Browser rcBr, final String siteKey) throws IOException, InterruptedException {
        if (Application.isHeadless()) {
            return null;
        }
        if (!BrowserSolverService.getInstance().getConfig().isBrowserLoopEnabled()) {
            return null;
        }
        if (!BrowserSolverService.getInstance().getConfig().isBrowserLoopDuringSilentModeEnabled() && JDGui.getInstance().isSilentModeActive()) {
            return null;
        }
        if (StringUtils.isNotEmpty(rcBr.getCookie("google.com", "SID")) && StringUtils.isNotEmpty(rcBr.getCookie("google.com", "HSID"))) {
            return null;
        }
        final String[] browserCommandLine = BrowserSolverService.getInstance().getConfig().getBrowserCommandline();
        if (!CrossSystem.isOpenBrowserSupported() && (browserCommandLine == null || browserCommandLine.length == 0)) {
            return null;
        }
        final AtomicReference<String> url = new AtomicReference<String>();
        final AbstractBrowserChallenge dummyChallenge = new AbstractBrowserChallenge("recaptcha", null) {

            @Override
            public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                return false;
            }

            @Override
            public String getHTML(String id) {
                try {
                    final URL url = RecaptchaV1Handler.class.getResource("recaptchaGetChallenge.html");
                    String html = IO.readURLToString(url);
                    html = html.replace("%%%sitekey%%%", siteKey);
                    return html;
                } catch (IOException e) {
                    throw new WTFException(e);
                }
            }

            @Override
            public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException, RemoteAPIException {
                String pDo = request.getParameterbyKey("do");
                if (pDo.equals("setChallenge")) {
                    url.set(request.getParameterbyKey("url"));

                    response.getOutputStream(true).write("true".getBytes("UTF-8"));
                    synchronized (url) {
                        url.notifyAll();
                    }
                    return true;
                }
                return super.onGetRequest(browserReference, request, response);
            }

            @Override
            public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                return null;
            }
        };
        final BrowserReference ref = new BrowserReference(dummyChallenge) {

            @Override
            public void onResponse(String request) {
            }

        };
        ref.open();
        try {
            synchronized (url) {
                url.wait(30000);
            }
        } finally {
            ref.dispose();
        }
        if (!BrowserSolverService.getInstance().getConfig().isBrowserLoopUserConfirmed()) {
            ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, _GUI.T.RecaptchaV1Handler_load_help__title(), _GUI.T.RecaptchaV1Handler_load_help_msg(), new AbstractIcon(IconKey.ICON_OCR, 32), _GUI.T.RecaptchaV1Handler_ok(), _GUI.T.RecaptchaV1Handler_disable()) {
                {

                    setLeftActions(new AppAction() {
                        {
                            setName(_GUI.T.RecaptchaV1Handler_load_help_());
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            CrossSystem.openURL("https://support.jdownloader.org/index.php?/Knowledgebase/Article/View/42/0/JD-opens-my-browser-to-display-captchas");
                        }

                    });
                }

                @Override
                protected JComponent getIconComponent() {
                    URLConnectionAdapter con = null;
                    try {
                        con = new Browser().openGetConnection(url.get());
                        BufferedImage niceImage = IconIO.toBufferedImage(ImageIO.read(con.getInputStream()));
                        Browser br = new Browser();
                        br.getPage("https://www.google.com/recaptcha/api/challenge?k=" + siteKey);

                        String challenge = br.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
                        String server = br.getRegex("server.*?:.*?'(.*?)',").getMatch(0);

                        BufferedImage badImage = IconIO.toBufferedImage(ImageIO.read(br.openGetConnection(server + "image?c=" + challenge).getInputStream()));

                        Graphics2D niceGraphics = (Graphics2D) niceImage.getGraphics();
                        Graphics2D badGraphics = (Graphics2D) badImage.getGraphics();
                        Font font = new Font(ImageProvider.getDrawFontName(), Font.BOLD, 18);

                        niceGraphics.setColor(Color.GREEN);
                        niceGraphics.setFont(font);
                        niceGraphics.drawString("With Browser Loop", 4, niceImage.getHeight() - 4);

                        badGraphics.setColor(Color.RED);
                        badGraphics.setFont(font);
                        badGraphics.drawString("Without Browser Loop", 4, badImage.getHeight() - 4);
                        MigPanel ret = new MigPanel("ins 0,wrap 1", "[]", "[][]");
                        ret.add(new JLabel(new ImageIcon(niceImage)));
                        ret.add(new JLabel(new ImageIcon(badImage)));
                        return ret;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                    return super.getIconComponent();
                }

                protected int getPreferredWidth() {
                    return 700;
                };
            };
            d.setTimeout(120000);
            final ConfirmDialogInterface impl = UIOManager.I().show(ConfirmDialogInterface.class, d);
            try {
                impl.throwCloseExceptions();
                StatsManager.I().track("browserloop/enabled");
                BrowserSolverService.getInstance().getConfig().setBrowserLoopUserConfirmed(true);
            } catch (DialogCanceledException e) {
                if (!e.isCausedByTimeout()) {
                    BrowserSolverService.getInstance().getConfig().setBrowserLoopUserConfirmed(true);
                    BrowserSolverService.getInstance().getConfig().setBrowserLoopEnabled(false);
                    StatsManager.I().track("browserloop/disabled");
                }
            } catch (DialogClosedException e) {
            }
        }
        final String urlString = url.get();
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }
        return urlString.substring(urlString.indexOf("c=") + 2);
    }
}
