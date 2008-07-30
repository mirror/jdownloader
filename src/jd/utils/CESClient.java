package jd.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.captcha.CES;
import jd.config.SubConfiguration;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPPost;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;

public class CESClient implements Serializable {
    private static final String BANNED = "bann";
    private static final long CHECK_INTERVAL = 20 * 1000;
    public static final String DO_WARNING = "CES_DO_WARNING";
    private static final String ERROR_MESSAGE_NOT_SENT = "Message not sent";
    private static final int FLAG_ABORT_RECEIVING = 0;
    public static final String LASTEST_INSTANCE = "CES_LASTEST_INSTANCE";
    private static final long MAX_TIME = 5 * 60 * 1000;

    public static final String MESSAGES = "CES_MESSAGES";
    public static final String PARAM_PASS = "CES_PASS";
    public static final String PARAM_USER = "CES_USER";
    private static final String PATTERN_CAPTCHA_ID = "<input type=\"hidden\" name=\"Captcha\" value=\"°\"/>";
    private static final String PATTERN_CAPTCHA_STATE = "<input type=\"hidden\" name=\"State\" value=\"°\"/>";
    private static final String PATTERN_IMAGE = "<p>Captcha image here:<br><img src=\"°\" border=\"2\" />";

    private static final String PATTERN_MESSAGE = "official C.E.S forum</a>!<br><p>°[ reload ]";
    private static final String PATTERN_MESSAGES = "<font color=\"green\"> &nbsp;-&nbsp; <i>°<b>SMS from <b>°</b>:°</b></i></font> &nbsp; <a href=\"°&DeleteMessage=°\">[ delete ]</a><br>";
    private static final String PATTERN_QUEUE = "<p>Your Username is <b>\"°\"</b>, your balance is <b>°</b> points. You have received <b>°</b> captchas, recognized <b>°</b>, errors <b>° (°%)</b>.<br>You have sent on a server of <b>°</b> captchas, from them has been recognized <b>°</b>, errors <b>° (°%)</b>.<br></p>°<p><br><div id=\"ta\">You are staying in queue to captcha recognition for <b>°</b> seconds.</div><br>There are <b>°</b> users in queue and <b>°</b> before you.°<br>Please, wait a little more... (estimated waiting time: °)<a href=°>[ reload ]";
    private static final String PATTERN_STATS_QUEUE = "There are <b>°</b> registered users now in queue on this server.";
    private static final String PATTERN_STATS_RECEIVE = "Your balance is <b>°</b> points. You have received <b>°</b> captchas, recognized <b>°</b>, errors <b>° (°%)</b>";
    private static final String PATTERN_STATS_SENT = "You have sent on a server of <b>°</b> captchas, from them has been recognized <b>°</b>, errors <b>° (°%)</b>.";
    private static final String PATTERN_SYSTEMMESSAGE = "<i>° ° <b>°</b></i><br>";
    private static final String PATTERN_TITLE = "<title>CaptchaExchangeServer - v° (°) by DVK</title>";
    private static final long RECEIVE_INTERVAL = 20 * 1000;
    /**
     * 
     */
    private static final long serialVersionUID = 1448063149305109311L;
    public static final String UPDATE = "CES_QUEUE_LENGTH";

    public static void main(String[] args) {

        // File file = new
        // File("C:/Users/coalado/.jd_home/captchas/rapidshare.com/21.04.2008_17.19.51.jpg");
        //
        // CESClient ces = new CESClient();

        // ces.enterQueueAndWait();
        // if (ces.sendCaptcha()) {
        // JDUtilities.getLogger().info("code: "+ces.waitForAnswer());
        // }
        //
        // // ces.register("coalado");
        //
        // // ces.login();

    }

    private int abortFlag = -1;
    private String balance;
    private boolean banned;

    private String boundary = "---------------------------19777693011381";
    private String captchaCode;
    private String captchaID;
    private String cesCode;
    private String cesDate;
    private boolean cesStatus;
    private String cesVersion;
    private SubConfiguration config;
    private DownloadLink downloadLink;
    private String eta;
    private File file;
    private String k = JDUtilities.Base64Decode("akRvV25Mb2FEZXIxMDY=");
    transient private Logger logger;
    private String md5;
    private String pass;
    private boolean penaltyWarned = false;
    private Plugin plugin;
    private String price;
    private String queueLength;
    private String queuePosition;
    private String receivedCaptchas;
    private String receiveErrors;
    private String receiveErrorsPercent;
    private String recognizedCaptchas;
    private String recTime;
    private String recUser;
    private int save = 0;
    private Object sendErrorsPercent;
    private String sendRecognized;
    private String sentCaptchasNum;
    private String sentErrors;
    private String server = "http://dvk.com.ua/rapid/";// "http://dvk.org.ua/sokol/rapid/";//
    private String specs = "";
    private long startTime;
    private String statusText;
    private int type = 1;
    private String user;

    public CESClient() {
        config = JDUtilities.getSubConfig("JAC");
        config.setProperty(LASTEST_INSTANCE, this);
        logger = JDUtilities.getLogger();

    }

    public CESClient(File captcha) {
        file = captcha;
        md5 = JDUtilities.getLocalHash(file);
        logger = JDUtilities.getLogger();
        config = JDUtilities.getSubConfig("JAC");
        config.setProperty(LASTEST_INSTANCE, this);

    }

    /**
     * Brihct das Warten auf einen Captcha ab
     */
    public void abortReceiving() {

        abortFlag = FLAG_ABORT_RECEIVING;

        try {
            printMessage(HTTP.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "logout=Stop+captcha+recognition&State=4&Nick=" + user + "&Pass=" + pass).getHtmlCode());
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        config.setProperty(LASTEST_INSTANCE, this);
    }

    private void askUserForCode(String adr, String id, String state) {

        int index = Math.max(adr.lastIndexOf("/"), adr.lastIndexOf("\\"));
        String ext = adr.substring(index + 1);
        Calendar calendar = Calendar.getInstance();
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS", calendar);
        File dest = JDUtilities.getResourceFile("captchas/CES/" + date + "." + ext);
        JDUtilities.downloadBinary(dest.getAbsolutePath(), adr);
        String code = JDUtilities.getGUI().getCaptchaCodeFromUser(plugin, dest, "");
        if (code != null && code.length() > 0) {
            try {
                penaltyWarned = false;
                RequestInfo ri = HTTP.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "Code=" + code + "&enter=OK&Captcha=" + id + "&State=" + state + "&Nick=" + user + "&Pass=" + pass);
                if (checkBanned(ri)) { return; }
                config.setProperty(LASTEST_INSTANCE, this);
                printMessage(ri.getHtmlCode());
            } catch (MalformedURLException e) {

                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();
            }
        } else {
            CES.setEnabled(false);
            config.setProperty(LASTEST_INSTANCE, this);
            config.setProperty(UPDATE, (int) System.currentTimeMillis());

        }
    }

    private boolean checkBanned(RequestInfo ri) {
        if (ri.containsHTML(BANNED)) {
            banned = true;
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("captcha.ces.banned", "Du bist gebannt. Bitte neu registrieren."));

            return true;
        } else {
            banned = false;
            return false;
        }
    }

    /**
     * Betritt die Warteschleife TODO!!!
     */
    @SuppressWarnings("unchecked")
    public void enterQueueAndWait() {
        while (true) {
            try {

                // http://dvk.com.ua/rapid/index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1
                if (abortFlag == FLAG_ABORT_RECEIVING) { return; }
                RequestInfo ri = HTTP.getRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"));
                if (checkBanned(ri)) { return; }
                printMessage(ri.getHtmlCode());
                config.setProperty(LASTEST_INSTANCE, this);
                // PATTERN_IMAGE
                String img = SimpleMatches.getSimpleMatch(ri.getHtmlCode(), PATTERN_IMAGE, 0);
                if (img != null) {
                    String id = SimpleMatches.getSimpleMatch(ri.getHtmlCode(), PATTERN_CAPTCHA_ID, 0);
                    String state = SimpleMatches.getSimpleMatch(ri.getHtmlCode(), PATTERN_CAPTCHA_STATE, 0);

                    printMessage(ri.getHtmlCode());
                    askUserForCode(server + img, id, state);

                }
                Object oldMessages = config.getProperty(MESSAGES);
                HashMap<Integer, ArrayList<String>> savedMessages = null;
                if (oldMessages != null) {
                    savedMessages = (HashMap<Integer, ArrayList<String>>) oldMessages;
                }
                // [[01.05.08 19:27, coalado, test,
                // index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1, 67618],
                // [01.05.08 22:48, coalado, bla und so,
                // index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1, 67887]]
                ArrayList<ArrayList<String>> messages = SimpleMatches.getAllSimpleMatches(ri.getHtmlCode(), PATTERN_MESSAGES);
                for (ArrayList<String> message : messages) {
                    int id = Integer.parseInt(message.get(4));
                    if (savedMessages == null) {
                        savedMessages = new HashMap<Integer, ArrayList<String>>();
                    }
                    if (!savedMessages.containsKey(id)) {
                        onNewMessage(message);
                        savedMessages.put(id, message);
                    }

                }
                if (true) {
                    config.setProperty(MESSAGES, savedMessages);
                    config.save();
                }
                messages = SimpleMatches.getAllSimpleMatches(ri.getHtmlCode(), PATTERN_SYSTEMMESSAGE);
                for (ArrayList<String> tmp : messages) {
                    ArrayList<String> message = new ArrayList<String>();
                    message.add(tmp.get(0) + " " + tmp.get(1));
                    message.add("C.E.S. System");
                    message.add(tmp.get(2));
                    message.add(null);
                    message.add(null);

                    int id = (int) System.currentTimeMillis();
                    if (savedMessages == null) {
                        savedMessages = new HashMap<Integer, ArrayList<String>>();
                    }
                    if (!savedMessages.containsKey(id)) {
                        onNewMessage(message);
                        savedMessages.put(id, message);
                    }

                }
                if (true) {
                    config.setProperty(MESSAGES, savedMessages);
                    config.save();
                }

                String[] answer;

                answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), PATTERN_QUEUE);

                if (answer != null) {
                    balance = answer[1];
                    receivedCaptchas = answer[2];
                    recognizedCaptchas = answer[3];
                    receiveErrors = answer[4];
                    receiveErrorsPercent = answer[5];
                    sentCaptchasNum = answer[6];
                    sendRecognized = answer[7];
                    sentErrors = answer[8];
                    sendErrorsPercent = answer[9];
                    queueLength = answer[12];
                    queuePosition = answer[13];
                    eta = answer[15];

                    config.setProperty(UPDATE, (int) System.currentTimeMillis());
                    if (eta.contains("sec")) {
                        onWarning();
                    }
                    if (eta.contains("min") && JDUtilities.filterInt(eta) <= 3) {
                        onWarning();
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            long i = RECEIVE_INTERVAL;

            while (i > 0) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }

                i -= 500;

                if (abortFlag == FLAG_ABORT_RECEIVING) {

                return; }
            }
        }

    }

    public String getAccountInfoString() {
        String l = JDLocale.L("captcha.ces.infostring", "C.E.S |Punkte: %s |Benutzer: %s |Position: %s |Wartezeit bis Captcha: %s");
        return String.format(l, getBalance(), getQueueLength(), getQueuePosition(), getEta());
    }

    public String getBalance() {
        return balance;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public String getCaptchaID() {
        return captchaID;
    }

    public String getCesCode() {
        return cesCode;
    }

    public String getCesDate() {
        return cesDate;
    }

    public String getCesVersion() {
        return cesVersion;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public String getEta() {
        return eta;
    }

    public File getFile() {
        return file;
    }

    /**
     * Erstellt den Auth key für den Server
     * 
     * @return
     */
    private String getKey() {
        return JDUtilities.getMD5(k + md5);

    }

    public String getPass() {
        return pass;
    }

    public String getPrice() {
        return price;
    }

    public String getQueueLength() {
        return queueLength;
    }

    public String getQueuePosition() {
        return queuePosition;
    }

    public String getReceivedCaptchas() {
        return receivedCaptchas;
    }

    public String getReceiveErrors() {
        return receiveErrors;
    }

    public String getReceiveErrorsPercent() {
        return receiveErrorsPercent;
    }

    public String getRecognizedCaptchas() {
        return recognizedCaptchas;
    }

    public String getRecTime() {
        return recTime;
    }

    public String getRecUser() {
        return recUser;
    }

    public int getSave() {
        return save;
    }

    public Object getSendErrorsPercent() {
        return sendErrorsPercent;
    }

    public String getSendRecognized() {
        return sendRecognized;
    }

    public String getSentCaptchasNum() {
        return sentCaptchasNum;
    }

    public String getSentErrors() {
        return sentErrors;
    }

    /**
     * Getter für alles mögliche
     * 
     * @return
     */
    public String getServer() {
        return server;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getStatusText() {
        return statusText;
    }

    public int getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    public boolean isBanned() {
        return banned;
    }

    public boolean isCesStatus() {
        return cesStatus;
    }

    /**
     * login. Statistische Informationen über den account werden abgerfen. Ist
     * zur funktionweise nicht zwingend nötig
     * 
     * @return
     */
    public boolean login() {
        RequestInfo ri;
        try {

            ri = HTTP.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=" + pass, true);
            if (checkBanned(ri)) { return false; }
            String error = SimpleMatches.getSimpleMatch(ri.getHtmlCode(), "<font color=\"red\"><h2>°</h2></font>", 0);
            printMessage(ri.getHtmlCode());
            statusText = error;
            if (error != null) {

            return false; }

            String[] answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), PATTERN_TITLE);
            cesVersion = answer[0];
            cesDate = answer[1];
            answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_RECEIVE);
            balance = answer[0];
            receivedCaptchas = answer[1];
            recognizedCaptchas = answer[2];
            receiveErrors = answer[3];
            receiveErrorsPercent = answer[4];
            answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_SENT);
            sentCaptchasNum = answer[0];
            sendRecognized = answer[1];
            sentErrors = answer[2];
            sendErrorsPercent = answer[3];
            answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_QUEUE);

            queueLength = answer[0];

        } catch (Exception e) {

        }
        return true;
    }

    private void onNewMessage(ArrayList<String> message) {
        // [[01.05.08 19:27, coalado, test,
        // index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1, 67618],
        // [01.05.08 22:48, coalado, bla und so,
        // index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1, 67887]]
        String title = String.format(JDLocale.L("captcha.ces.message.title", "C.E.S. Neue Nachricht erhalten von %s am %s"), message.get(1), message.get(0));
        String html = String.format(JDLocale.L("captcha.ces.message.body", "<link href=\"http://jdownloader.org/jdccs.css\" rel=\"stylesheet\" type=\"text/css\" /><div><p>%s Nachricht von %s<hr>%s</p></div>"), JDUtilities.htmlDecode(message.get(0)), JDUtilities.htmlDecode(message.get(1)), JDUtilities.htmlDecode(message.get(2)));
        JDUtilities.getGUI().showHTMLDialog(title, html);

    }

    private void onWarning() {
        if (config.getBooleanProperty(DO_WARNING, true) && !penaltyWarned) {
            JDSounds.PT("sound.CES.penaltyAvoidance");
            penaltyWarned = true;
            boolean res = JDUtilities.getGUI().showCountdownConfirmDialog(String.format(JDLocale.L("captcha.ces.penaltyavoidance", "<font color='RED'><b>C.E.S. ACHTUNG! Captchaeingabe in Kürze.(%s)</b></font>"), eta), 60);
            if (!res) {
                CES.setEnabled(false);
                config.setProperty(LASTEST_INSTANCE, this);
                config.setProperty(UPDATE, (int) System.currentTimeMillis());
            }

        }

    }

    private void printMessage(String htmlCode) {
        String msg = SimpleMatches.getSimpleMatch(htmlCode, PATTERN_MESSAGE, 0);
        if (msg == null) {
            msg = htmlCode;
        }
        msg = msg.replaceAll(user, "*****");
        msg = msg.replaceAll(pass, "*****");
        logger.info("CES: " + msg);

    }

    /**
     * Registriert einen user und gibt sein Passwort zurück null wird im
     * fehlerfall zurückgegeben. mit getStatusText() kann in diesem Fall der
     * fehler abgerufen werden
     * 
     * @param user
     * @return
     */
    public String register(String user) {

        RequestInfo ri;
        try {
            ri = HTTP.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=RapidByeByeBye", true);
            String pass = SimpleMatches.getSimpleMatch(ri.getHtmlCode(), "You password is <b>°<b></h2>", 0);
            if (pass == null) {

                String error = SimpleMatches.getSimpleMatch(ri.getHtmlCode(), "<font color=\"red\"><h2>°</h2></font>", 0);
                statusText = error;
            }
            return pass;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Schickt den übergebenen Captcha an den Server
     * 
     * @return
     */
    public boolean sendCaptcha() {
        try {
            HTTPPost up = new HTTPPost(server + "post.php", true);
            up.setBoundary(boundary);
            up.doUpload();
            up.connect();
            up.setForm("Captcha");
            up.sendFile(file.getAbsolutePath(), file.getName());
            up.sendVariable("Nick", user);
            up.sendVariable("Pass", pass);
            up.sendVariable("Save", save + "");
            up.sendVariable("Link", "No link due to security reasons");
            up.sendVariable("Type", type + "");
            up.sendVariable("Key", getKey());
            up.sendVariable("Spec", specs);

            RequestInfo ri = up.getRequestInfo();
            if (checkBanned(ri)) { return false; }
            // <html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08
            // 02:00) by DVK<br>dvk_ok001; Captcha is loaded with ID(580281),
            // your
            // balance (96), please wait recognition...<br></body></html>
            up.close();
            printMessage(ri.getHtmlCode());
            if (ri.containsHTML("dvk_ok001")) {
                String[] answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v° (°) by DVK<br>dvk_ok°; Captcha is loaded with ID(°), your balance (°), please wait recognition...<br></body></html>");
                cesVersion = answer[0];
                cesDate = answer[1];
                cesStatus = true;
                cesCode = answer[2];
                captchaID = answer[3];
                balance = answer[4];
                return true;
            } else {
                cesStatus = false;
                String[] answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08 02:00) by DVK<br>dvk_°;°<br></body></html>");
                cesVersion = answer[0];
                cesDate = answer[1];

                cesCode = answer[2];
                statusText = answer[3];

                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Informiert den Server darüber dass der letzte Captcha falsch erkannt
     * wurde
     */
    public void sendCaptchaWrong() {
        try {

            RequestInfo ri = HTTP.postRequest(new URL(server + "test.php"), null, null, null, "Nick=" + user + "&Pass=" + pass + "&Wrong=" + captchaID, true);
            if (checkBanned(ri)) { return; }
            config.setProperty(LASTEST_INSTANCE, this);
            printMessage(ri.getHtmlCode());
        } catch (Exception e) {

        }
    }

    public boolean sendMessage(String nick, String message) {
        // POST /rapid/index.php?Nick=coalado&Pass=aCvtSmZwNCqm1 HTTP/1.1
        // Host: dvk.com.ua
        // User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.14)
        // Gecko/20080404 Firefox/2.0.0.14
        // Accept:
        // text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
        // Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
        // Accept-Encoding: gzip,deflate
        // Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
        // Keep-Alive: 300
        // Connection: keep-alive
        // Referer:
        // http://dvk.com.ua/rapid/index.php?Nick=coalado&Pass=aCvtSmZwNCqm1
        // Content-Type: application/x-www-form-urlencoded
        // Content-Length: 65
        //
        // State=&Message=bla&ToNick=coalado&Nick=coalado&Pass=aCvtSmZwNCqm1
        message = JDUtilities.urlEncode(HTMLEntities.htmlentities(JDUtilities.htmlDecode(message)));
        nick = JDUtilities.urlEncode(HTMLEntities.htmlentities(JDUtilities.htmlDecode(nick)));

        try {
            RequestInfo ri = HTTP.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass), "State=&Message=" + message + "&ToNick=" + nick + "&Nick=" + user + "&Pass=" + pass);
            if (checkBanned(ri)) { return false; }
            if (ri.containsHTML(ERROR_MESSAGE_NOT_SENT)) { return false;

            }
            return true;
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return false;

    }

    public void setDownloadLink(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;

    }

    public void setFile(File file) {
        this.file = file;
        md5 = JDUtilities.getLocalHash(file);
    }

    /**
     * Setzt die Account Logins
     * 
     * @param user
     * @param pass
     */
    public void setLogins(String user, String pass) {
        this.user = user;
        this.pass = pass;

    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public void setPlugin(Plugin plg) {
        plugin = plg;

    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setSpecs(String specs) {
        this.specs = specs;

    }

    public void setType(int type) {
        this.type = type;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Nach dem mit sendCaptcha ein captcha verschickt wurde, muss diese
     * Funktion aufgerufen werden. Sie wartet solange bis der captcha
     * zurückgegeben wird und gibt den code zurück
     * 
     * @return
     */
    public String waitForAnswer() {
        // <html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08
        // 02:00) by DVK<br>dvk_ok001; Captcha is loaded with ID(510), your
        // balance (0), please wait recognition...<br></body></html>
        startTime = System.currentTimeMillis();
        try {
            logger.info("WAIT FOR REC");
            while (true) {
                config.setProperty(LASTEST_INSTANCE, this);

                long wait = CHECK_INTERVAL;
                while (wait > 0) {
                    if (getDownloadLink() != null && getDownloadLink().getDownloadLinkController().isAborted()) { return null; }
                    Thread.sleep(1000);
                    wait -= 1000;

                }
                if (System.currentTimeMillis() - startTime > MAX_TIME) {
                    cesStatus = false;
                    statusText = "Timeout of " + MAX_TIME;
                    return null;
                }
                RequestInfo ri = HTTP.postRequest(new URL(server + "test.php"), null, null, null, "Nick=" + user + "&Pass=" + pass + "&Test=" + captchaID, true);
                if (checkBanned(ri)) { return null; }
                printMessage(ri.getHtmlCode());
                if (ri.containsHTML("dvk_ok002")) {

                    String[] answer = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v° (°) by DVK<br>dvk_ok002; AccessCode(°), price ° point, \"°\" recognized in ° seconds early, balance (°).<br></body></html>");
                    cesVersion = answer[0];
                    cesDate = answer[1];
                    cesStatus = true;
                    captchaCode = answer[2];
                    price = answer[3];
                    recUser = answer[4];
                    recTime = answer[5];

                    balance = answer[5];
                    return captchaCode;

                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}