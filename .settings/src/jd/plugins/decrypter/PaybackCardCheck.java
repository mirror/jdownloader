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

package jd.plugins.decrypter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.PluginForDecrypt;

// http://www.payback.de/pb/vorhandene_karte/id/12972/?start=8953485834&anzahl=200
@DecrypterPlugin(revision = "$Revision: 15481 $", interfaceVersion = 2, names = { "payback.de" }, urls = { "http://(www\\.)?payback\\.de/pb/vorhandene_karte/id/12972/\\?start=\\d+\\&anzahl=\\d+(\\&pin=\\d{4})?" }, flags = { 0 })
public class PaybackCardCheck extends PluginForDecrypt {

    FileWriter     writer;
    private String aBrowser = "";
    String         pin      = "1234";

    public PaybackCardCheck(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final String[] values = parameter.split("\\?");
        if (values == null || values.length != 2) { return decryptedLinks; }
        parameter = values[0];

        final String startId = new Regex(values[1], "start=(\\d+)").getMatch(0);
        String anzahl = new Regex(values[1], "anzahl=(\\d+)").getMatch(0);
        final String pPin = new Regex(values[1], "pin=(\\d+)").getMatch(0);
        pin = pPin != null && pPin.length() == 4 ? pPin : pin;

        br.setFollowRedirects(false);

        Form checkCard = null;
        if (startId == null || anzahl == null || startId != null && startId.length() > 10) { return decryptedLinks; }
        final File file = new File("payback_" + getDateTime("yyyyMMdd") + ".txt");
        final File error = new File("payback_error.txt");

        final long sId = Long.parseLong(startId);
        if (anzahl.length() > 5) {
            anzahl = "9999";
        }
        int a = Integer.parseInt(anzahl);
        // zuviele Serveranfragen sind nie gut
        a = a > 1000 ? 1000 : a;

        String treffer;
        int hit = 0, errorHit = 0;
        for (int i = 0; i < a; i++) {
            br.getPage(parameter);
            checkCard = br.getFormbyProperty("id", "cardNumberForm");
            if (checkCard == null) {
                continue;
            }
            treffer = String.valueOf((sId + i));
            checkCard.put("model.cardNumber", treffer);
            checkCard.put("x", "" + (int) (55 * Math.random() + 1));
            checkCard.put("y", "" + (int) (23 * Math.random() + 1));
            br.submitForm(checkCard);
            if (br.containsHTML("<h2>Anmeldung für PAYBACK\\-Neukunden</h2>")) {
                final DownloadLink dl = createDownloadlink("http://payback.de/?id=" + System.currentTimeMillis() + ".txt");
                if (register(treffer, param)) {
                    schreiben(treffer + " pin: " + pin, file);
                    hit++;
                    dl.setFinalFileName("PayBack Karten ID: " + treffer + " erfolgreich angemeldet am " + getDateTime("dd.MM.yy") + " um " + getDateTime("HH-mm-ss") + " Uhr");
                    dl.setAvailable(true);
                } else {
                    schreiben("http://www.payback.de/pb/vorhandene_karte/id/12972/?start=" + treffer + "&anzahl=1", error);
                    errorHit++;
                    dl.setFinalFileName("PayBack Karten ID: " + treffer + "  nicht erfolgreich angemeldet");
                    dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                }
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dl);
            }
            sleep(3 * 1000l, param);
        }
        if (hit > 0 || errorHit > 0) {
            String m = "";
            if (errorHit > 0) {
                m = "\r\n\r\n" + errorHit + " mal konnte die Anmeldung nicht abgeschlossen werden!";
            }
            UserIO.getInstance().requestMessageDialog("PAYBACK - Suche nach unbenutzen Kartennummern", (hit > 0 ? "Es wurde(n) " + hit + " Kartennummer(n) gefunden und erfolgreich registriert.\r\nGespeichert in: " + file.getAbsolutePath() : "") + m);
        } else {
            UserIO.getInstance().requestMessageDialog("PAYBACK - Suche nach unbenutzen Kartennummern", "Leider keine Treffer!");
        }
        return decryptedLinks;
    }

    private String getDateTime(final String s) {
        final Date date = new Date();
        final SimpleDateFormat format = new SimpleDateFormat(s);
        return format.format(date);
    }

    private HashMap<String, String> getFakeData() throws Exception {
        final HashMap<String, String> ret = new HashMap<String, String>();
        final Browser f = new Browser();
        f.getPage("http://www.fake-it.biz/");
        String t = f.toString();
        t = t.replaceAll("\n|\r", "");
        f.getRequest().setHtmlCode(t);
        haveFun(f);
        t = new Regex(aBrowser, "(Name:.*?)Name:").getMatch(0);
        t = t.replaceAll("\\s\t\\s\\s\t\\s+\t\\s+", "|");
        t = t.replaceAll(":\\|", ":");
        for (final String[] s : new Regex(t, "(.*?):(.*?)\\|").getMatches()) {
            ret.put(s[0].trim(), s[1].trim());
        }

        return ret;
    }

    public void haveFun(final Browser f) throws Exception {
        final ArrayList<String> someStuff = new ArrayList<String>();
        final ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("(<.*?>)");
        for (final String aRegex : regexStuff) {
            aBrowser = f.toString();
            final String replaces[] = f.getRegex(aRegex).getColumn(0);
            if (replaces != null && replaces.length != 0) {
                for (final String dingdang : replaces) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (final String gaMing : someStuff) {
            aBrowser = aBrowser.replaceAll(gaMing, "");
        }
    }

    private boolean register(final String id, final CryptedLink param) throws Exception {
        final Browser r = br.cloneBrowser();
        Form personDetailsForm = r.getFormbyProperty("id", "personDetailsForm");
        if (personDetailsForm == null) { return false; }
        final HashMap<String, String> fakeData = new HashMap<String, String>(getFakeData());
        if (fakeData == null || fakeData.size() == 0) { return false; }
        personDetailsForm.remove("model.salutationId");
        personDetailsForm.put("model.salutationId", "1");
        // personDetailsForm.put("model.titleId", "0");
        personDetailsForm.put("model.firstName", fakeData.get("Name") != null ? fakeData.get("Name").split("\\s")[0] : "");
        personDetailsForm.put("model.lastName", fakeData.get("Name") != null ? fakeData.get("Name").split("\\s")[1] : "");
        personDetailsForm.put("model.street", fakeData.get("Adresse") != null ? fakeData.get("Adresse").replace(" ", "+") : "");
        final String zip = fakeData.get("Stadt") != null ? fakeData.get("Stadt").split("\\s")[0] : "";
        personDetailsForm.put("model.zip", zip.length() % 5 > 1 ? "0" + zip : zip);
        personDetailsForm.put("model.city", fakeData.get("Stadt") != null ? fakeData.get("Stadt").split("\\s")[1] : "");
        personDetailsForm.put("model.dobDay", fakeData.get("Geboren") != null ? fakeData.get("Geboren").split("/")[0] : "");
        personDetailsForm.put("model.dobMonth", fakeData.get("Geboren") != null ? fakeData.get("Geboren").split("/")[1] : "");
        personDetailsForm.put("model.dobYear", fakeData.get("Geboren") != null ? fakeData.get("Geboren").split("/")[2] : "");
        String email = fakeData.get("Email") != null ? fakeData.get("Email") : "";
        email = Encoding.htmlDecode(email);
        personDetailsForm.put("model.email", email);
        personDetailsForm.put("model.emailRepeat", email);
        personDetailsForm.put("model.pin", pin);
        personDetailsForm.put("model.pinretype", pin);
        personDetailsForm.put("x", "" + (int) (55 * Math.random() + 1));
        personDetailsForm.put("y", "" + (int) (23 * Math.random() + 1));
        try {
            r.submitForm(personDetailsForm);
        } catch (final Throwable e) {
            if (r.getHttpConnection().getResponseCode() == 500) {
                logger.warning("Error: Server Response 500");
                logger.info(r.getHttpConnection().getRequest().toString());
                // if (Reconnecter.waitForNewIP(15000, false)) { return
                // register(id, param); }
                return false;
            }
        }
        final String error = "(<div class=\"error(\\-global)?\">|Sie haben seit mehr als \\d+ Minuten keine Aktion)";
        // payback korregiert falscheingaben selbstständig
        if (r.containsHTML(error)) {
            for (int i = 0; i < 5; i++) {
                personDetailsForm = r.getFormbyProperty("id", "personDetailsForm");
                if (personDetailsForm == null) { return false; }
                personDetailsForm.remove("model.salutationId");
                personDetailsForm.put("model.salutationId", "1");
                personDetailsForm.put("model.pin", pin);
                personDetailsForm.put("model.pinretype", pin);
                personDetailsForm.put("x", "" + (int) (55 * Math.random() + 1));
                personDetailsForm.put("y", "" + (int) (23 * Math.random() + 1));
                try {
                    r.submitForm(personDetailsForm);
                } catch (final Throwable e) {
                    if (r.getHttpConnection().getResponseCode() == 500) {
                        logger.warning("Error: Server Response 500");
                        logger.info(r.getHttpConnection().getRequest().toString());
                        // if (Reconnecter.waitForNewIP(15000, false)) { return
                        // register(id, param); }
                        return false;
                    }
                }
                if (!r.containsHTML(error)) {
                    break;
                }
            }
        }
        if (r.containsHTML(error)) {
            logger.warning("Error: Es ist ein Problem beim der Anmeldung aufgetreten! KartenId:" + id);
            return false;
        }
        final Form servicesForm = r.getFormbyProperty("id", "servicesForm");
        if (servicesForm == null) { return false; }
        servicesForm.remove("model.information");
        servicesForm.remove("model.newsletter");
        servicesForm.remove("model.newsletterType");
        servicesForm.remove("model.email");
        servicesForm.remove("model.emailRepeat");
        servicesForm.remove("model.sms");
        servicesForm.remove("model.mobilePhone");
        // Captcha
        for (int i = 0; i < 5; i++) {
            final String captchaLink = r.getRegex("src=\"/pb/jcaptcha\\?(captcha_form_ident=[\\w\\-]+)\"").getMatch(0);
            final String postDataCode = r.getRegex("name=\"(captcha\\-[\\w\\-]+)\"").getMatch(0);
            if (captchaLink == null || postDataCode == null) {
                logger.warning("Error: Konnte CaptchaUrl nicht finden. Benutzte KartenId=" + id);
                r.clearCookies(r.getHost());
                return register(id, param);
            }
            final String[] cl = captchaLink.split("=");
            final Browser brc = r.cloneBrowser();
            final File captchaFile = getLocalCaptchaFile();
            brc.getDownload(captchaFile, "https://www.payback.de/pb/jcaptcha?" + captchaLink);
            final String code = getCaptchaCode(captchaFile, param);
            servicesForm.put(cl[0], cl[1]);
            servicesForm.put(postDataCode, code);
            servicesForm.put("x", "" + (int) (55 * Math.random() + 1));
            servicesForm.put("y", "" + (int) (23 * Math.random() + 1));
            r.submitForm(servicesForm);
            if (r.containsHTML("<h2>Ihre Anmeldung war erfolgreich</h2>")) {
                break;
            }
            if (r.containsHTML(error)) {
                logger.info("Formular inklusive Captchaabfrage verarbeitet. Es wurde trotzdem ein fehler festgestellt!");
                final String e = r.getRegex("<div class=\"error(\\-global)?\">([^<]+)").getMatch(1);
                if (e != null) {
                    logger.warning("Error: " + e);
                } else {
                    logger.info("payback gibt keinen Fehler aus!");
                }
            }
            r.clearCookies(r.getHost());
        }
        if (!r.containsHTML("<h2>Ihre Anmeldung war erfolgreich</h2>")) { return false; }
        return true;
    }

    private void schreiben(final String s, final File f) {
        try {
            writer = new FileWriter(f, true);
            writer.write(s);
            writer.write(System.getProperty("line.separator"));
            writer.flush();
            writer.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
