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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CaptchaException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.utils.EditDistance;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "serienjunkies.org", "dokujunkies.org" }, urls = { "http://[\\w\\.]*?serienjunkies\\.org/([a-z]{1,2}[_-][a-f0-9]{16}.*|go\\-[a-f0-9]{128}/)", "http://[\\w\\.]*?dokujunkies\\.org/[\\w\\-/]+.*\\.html" })
public class Srnnks extends antiDDoSForDecrypt {
    class DecryptRunnable implements Runnable {
        private final Form                    downloadForm;
        private final Browser                 br;
        private final ArrayList<DownloadLink> results;

        public DecryptRunnable(final Form downloadForm, final Browser br, final ArrayList<DownloadLink> results) {
            this.downloadForm = downloadForm;
            this.br = br;
            this.results = results;
        }

        public void run() {
            // sj heuristic detection. this makes the jobber useless... but we have to use the 300 ms to work around sj's firewall
            try {
                Thread.sleep(FW_WAIT);
                if (isAbort()) {
                    return;
                }
                br.submitForm(downloadForm);
                if (br.getRedirectLocation() != null) {
                    this.results.add(Srnnks.this.createDownloadlink(this.br.getRedirectLocation()));
                } else {
                    // not sure if there are still pages that use this old system
                    final String link = br.getRegex("SRC=\"(http://download\\.serienjunkies\\.org.*?)\"").getMatch(0);
                    if (link != null) {
                        Thread.sleep(FW_WAIT);
                        if (isAbort()) {
                            return;
                        }
                        br.getPage(link);
                        final String loc = br.getRedirectLocation();
                        if (loc != null) {
                            this.results.add(Srnnks.this.createDownloadlink(loc));
                            return;
                        } else {
                            throw new Exception("no Redirect found");
                        }
                    } else {
                        throw new Exception("no Frame found");
                    }
                }
            } catch (final Throwable e) {
                getLogger().log(e);
            }
        }
    }

    private static ArrayList<String> PASSWORDS          = new ArrayList<String>(Arrays.asList(new String[] { "serienjunkies.dl.am", "serienjunkies.org", "dokujunkies.org" }));
    private final int                CAPTCHA_MAXRETRIES = 8;

    private synchronized boolean limitsReached(final Browser br) throws Exception {
        final long timeStamp = System.currentTimeMillis();
        final long maxTimeout = 2 * 60 * 1000l;
        int loopIndex = 0;
        while (!isAbort() || System.currentTimeMillis() - timeStamp > maxTimeout) {
            loopIndex++;
            if (br.containsHTML("Error 503")) {
                logger.info("Limit reached!");
                return true;
            } else if (br.containsHTML("Du hast zu oft das Captcha falsch")) {
                Thread.sleep(15000 + loopIndex * 500);
                br.loadConnection(br.openRequestConnection(null));
            } else if (br.containsHTML("Download-Limit")) {
                Thread.sleep(30000);
                br.loadConnection(br.openRequestConnection(null));
            } else {
                return false;
            }
        }
        if (isAbort()) {
            throw abortException;
        } else {
            logger.info("Limit reached!");
            return true;
        }
    }

    private volatile String crawlStatus = null;

    public Srnnks(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected DownloadLink createDownloadlink(final String link) {
        final DownloadLink dlink = super.createDownloadlink(link);
        dlink.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
        dlink.setSourcePluginPasswordList(PASSWORDS);
        this.distribute(dlink);
        return dlink;
    }

    public String getCrawlerStatusString() {
        return crawlStatus;
    }

    private static ReentrantLock GLOBAL_LOCK = new ReentrantLock();
    private final long           FW_WAIT     = 300;

    public void getPage(final String url) throws Exception {
        GLOBAL_LOCK.lock();
        try {
            Thread.sleep(FW_WAIT);
            if (isAbort()) {
                throw abortException;
            } else {
                super.getPage(url);
            }
        } finally {
            GLOBAL_LOCK.unlock();
        }
    }

    @Override
    protected void submitForm(Form form) throws Exception {
        GLOBAL_LOCK.lock();
        try {
            Thread.sleep(FW_WAIT);
            if (isAbort()) {
                throw abortException;
            } else {
                super.submitForm(form);
            }
        } finally {
            GLOBAL_LOCK.unlock();
        }
    }

    private final Exception abortException = new Exception();

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, final ProgressController progress) throws Exception {
        parameter.setDecrypterPassword("serienjunkies.org");
        // crude importation method from doju -> serien
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (parameter.getCryptedUrl().contains("dokujunkies.org")) {
            br.setFollowRedirects(false);
            br.setCookiesExclusive(true);
            getPage(parameter.toString());
            if (br.containsHTML("<h2>Error 404 \\- Page not found\\!</h2>")) {
                logger.warning("Invalid URL: " + parameter);
            } else {
                final String grab = br.getRegex("<p><strong>[\\w\\-\\.]+</strong><br />(.*?)<div class=\"post\\_details\">").getMatch(0);
                final String[] links = new Regex(grab, "href=\"(http://[\\w\\.]*?serienjunkies\\.org/.*?)\" target").getColumn(0);
                if (links != null && links.length != 0) {
                    for (final String link : links) {
                        ret.add(createDownloadlink(link));
                    }
                }
            }
            return ret;
        } else {
            if (parameter.getCryptedUrl().matches(".+/go\\-[a-f0-9]{128}/")) {
                // redirect support if the user adds copies these temp hash
                br.setFollowRedirects(false);
                getPage(parameter.getCryptedUrl());
                final String link = br.getRedirectLocation();
                if (link != null) {
                    ret.add(createDownloadlink(link));
                } else {
                    logger.warning("Failed to find temp redirect link location!");
                }
                return ret;
            } else {
                try {
                    captchaLoop: for (int i = 0; i < CAPTCHA_MAXRETRIES; i++) {
                        if (isAbort()) {
                            return ret;
                        }
                        getPage(parameter.getCryptedUrl());
                        if (limitsReached(br)) {
                            return ret;
                        }
                        if (br.containsHTML("<FRAME SRC")) {
                            final String src = br.getRegex("<FRAME SRC=\"(.*?)\"").getMatch(0);
                            if (src != null) {
                                getPage(src);
                                if (limitsReached(this.br)) {
                                    return ret;
                                }
                            } else {
                                logger.info("frame src is null!");
                            }
                        }
                        // linkendung kommt auch im action der form vor
                        final String sublink = parameter.getCryptedUrl().substring(parameter.getCryptedUrl().indexOf("org/") + 3);
                        // try captcha max 5 times
                        // suche wahrscheinlichste form
                        Form captchaForm = null;
                        final Form[] searchCaptchaForm = br.getForms();
                        int bestdist = Integer.MAX_VALUE;
                        if (searchCaptchaForm != null) {
                            searchLoop: for (final Form form : searchCaptchaForm) {
                                if (form.getAction() == null) {
                                    continue searchLoop;
                                } else {
                                    final int dist = EditDistance.damerauLevenshteinDistance(sublink, form.getAction());
                                    if (dist < bestdist) {
                                        captchaForm = form;
                                        bestdist = dist;
                                    }
                                }
                            }
                        }
                        if (captchaForm == null || bestdist > 100) {
                            throw new Exception("Serienjunkies Captcha Form konnte nicht gefunden werden!");
                        }
                        try {
                            final String crawlStatus = br.getRegex("<TITLE>.* \\- (.*?)</TITLE>").getMatch(0);
                            if (StringUtils.isEmpty(crawlStatus)) {
                                this.crawlStatus = "";
                            } else {
                                this.crawlStatus = crawlStatus;
                            }
                            if (captchaForm.containsHTML("=\"g-recaptcha\"")) {
                                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                                submitForm(captchaForm);
                                if (br.containsHTML("class=\"g-recaptcha\"")) {
                                    invalidateLastChallengeResponse();
                                    continue captchaLoop;
                                }
                            } else if (captchaForm.getRegex("img.*?src=\"([^\"]*?secure)").matches()) {
                                final String captchaLink = new Regex(captchaForm.getHtmlCode(), "<ig src=\"(.*?)\"").getMatch(0);
                                // only each 5 link needs captchas
                                final File captcha = this.getLocalCaptchaFile(".png");
                                final String captchaCode;
                                final long timeBefore;
                                try {
                                    // captcha laden
                                    GLOBAL_LOCK.lock();
                                    try {
                                        Thread.sleep(FW_WAIT);
                                        if (isAbort()) {
                                            return ret;
                                        } else {
                                            final URLConnectionAdapter con = openAntiDDoSRequestConnection(br.cloneBrowser(), br.createGetRequest(captchaLink));
                                            try {
                                                Browser.download(captcha, con);
                                            } finally {
                                                con.disconnect();
                                            }
                                        }
                                    } finally {
                                        GLOBAL_LOCK.unlock();
                                    }
                                    if ("7ebca510a6a18c1e8f6e8d98c3118874".equals(JDHash.getMD5(captcha))) {
                                        // dummy captcha without content.. wait before reloading
                                        logger.warning("Dummy Captcha. wait 3 seconds");
                                        Thread.sleep(3000);
                                        continue captchaLoop;
                                    }
                                    // wenn es ein Einzellink ist soll die Captchaerkennung benutzt werden
                                    timeBefore = System.currentTimeMillis();
                                    if (captchaLink.contains(".gif")) {
                                        captchaCode = this.getCaptchaCode("einzellinks.serienjunkies.org", captcha, parameter);
                                    } else {
                                        captchaCode = this.getCaptchaCode(captcha, parameter);
                                    }
                                } finally {
                                    captcha.delete();
                                }
                                if (captchaCode == null) {
                                    return ret;
                                } else if (captchaIsExpired(timeBefore)) {
                                    logger.info("Captcha expired!?");
                                    Thread.sleep(1100);
                                    continue;
                                } else if (captchaCode.length() != 3) {
                                    Thread.sleep(1100);
                                    continue;
                                }
                                captchaForm.getInputFieldByType("text").setValue(captchaCode);
                                submitForm(captchaForm);
                            } else {
                                logger.info("Captcha?!");
                            }
                        } catch (CaptchaException e) {
                            getLogger().log(e);
                            e.throwMeIfNoRefresh();
                            continue;
                        } catch (DecrypterException e) {
                            getLogger().log(e);
                            continue;
                        }
                        if (limitsReached(this.br)) {
                            return ret;
                        }
                        if (br.getRedirectLocation() != null) {
                            validateLastChallengeResponse();
                            ret.add(this.createDownloadlink(br.getRedirectLocation()));
                            return ret;
                        } else {
                            final ArrayList<Form> downloadForms = new ArrayList<Form>();
                            Form cnlForm = null;
                            final Form[] forms = br.getForms();
                            for (final Form frm : forms) {
                                if (frm.getAction().contains("download.serienjunkies.org") && !frm.getAction().contains("firstload") && !frm.getAction().equals("http://mirror.serienjunkies.org")) {
                                    downloadForms.add(frm);
                                }
                                if (frm.getAction().contains("addcrypted2")) {
                                    cnlForm = frm;
                                }
                            }
                            if (cnlForm != null && cnlForm.hasInputFieldByName("crypted") && cnlForm.hasInputFieldByName("jk")) {
                                validateLastChallengeResponse();
                                final String source = cnlForm.getInputField("source").getValue();
                                final DownloadLink cnl;
                                if (StringUtils.isEmpty(source)) {
                                    cnl = DummyCNL.createDummyCNL(Encoding.urlDecode(cnlForm.getInputField("crypted").getValue(), false), Encoding.urlDecode(cnlForm.getInputField("jk").getValue(), false), null, parameter.toString());
                                } else {
                                    cnl = DummyCNL.createDummyCNL(Encoding.urlDecode(cnlForm.getInputField("crypted").getValue(), false), Encoding.urlDecode(cnlForm.getInputField("jk").getValue(), false), null, Encoding.urlDecode(source, false));
                                }
                                cnl.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
                                cnl.setSourcePluginPasswordList(PASSWORDS);
                                this.distribute(cnl);
                                ret.add(cnl);
                            } else if (downloadForms.size() == 0) {
                                logger.info("No download forms!");
                                invalidateLastChallengeResponse();
                                continue captchaLoop;
                            } else {
                                validateLastChallengeResponse();
                                GLOBAL_LOCK.lock();
                                try {
                                    for (final Form downloadForm : downloadForms) {
                                        if (isAbort()) {
                                            return ret;
                                        } else {
                                            this.new DecryptRunnable(downloadForm, this.br.cloneBrowser(), ret).run();
                                        }
                                    }
                                } finally {
                                    GLOBAL_LOCK.unlock();
                                }
                            }
                            if (ret.size() != 0) {
                                return ret;
                            }
                        }
                    }
                logger.info("Max retries reached!");
                return ret;
                } catch (final Exception e) {
                    logger.log(e);
                    if (e == abortException) {
                        return ret;
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private final int CAPTCHA_EXPIRE_SECONDS = 58;

    private boolean captchaIsExpired(final long timeBefore) {
        final long timeDifference = System.currentTimeMillis() - timeBefore;
        if (timeDifference > (CAPTCHA_EXPIRE_SECONDS * 1000l)) {
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}