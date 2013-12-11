package org.jdownloader.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.Storage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.utils.IO;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.SessionInfo;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;
import org.jdownloader.myjdownloader.client.bindings.AccountStorable;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable;
import org.jdownloader.myjdownloader.client.bindings.events.EventDistributor;
import org.jdownloader.myjdownloader.client.bindings.events.EventsDistributorListener;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AccountAPIV2;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigManagerAPI;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ContentAPIV2;
import org.jdownloader.myjdownloader.client.exceptions.APIException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.ExceptionResponse;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.json.CaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;
import org.jdownloader.myjdownloader.client.json.JSonHandler;
import org.jdownloader.myjdownloader.client.json.JsonFactoryInterface;
import org.jdownloader.myjdownloader.client.json.MyJDJsonMapper;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage.TYPE;

public class TestClient {
    private static final String CALL_ACTION_WITHOUT_PARAMETERS = "Call Action Without parameters";
    private static final String GET_CAPTCHA_LIST               = "Get Captcha List";
    private static final String LIST_DEVICES                   = "List Devices";
    private static final String RESTORE_SESSION                = "Restore Session";
    private static final String REGISTER                       = "Register";
    private static final String LOGIN                          = "Login";
    private static final String WRITE_SESSION                  = "Write Session & Exit";
    private static final String CALL_UPTIME                    = "Call Uptime";
    private static final String REGAIN_TOKEN                   = "Regain Token";
    private static final String CAPTCHA_CHALLENGE              = "Captcha Challenge";
    private static final String DISCONNECT                     = "Disconnect";
    private static final String CHANGE_PASSWORD                = "Change Password";
    private static final String FEEDBACK                       = "Give Feedback";
    private static final String KEEPALIVE                      = "Keep Alive";
    private static final String CANCEL                         = "Cancel Registration";
    private static final String EVENTS_TEST                    = "Test Events";
    private static final String TEST_ACCOUNTAPI                = "Test AccountAPI";
    private static final String TEST_CONFIGAPI                 = "Test ConfigAPI";
    private static final String TEST_CONTENTAPI                = "Test ContentAPI";

    private static ImageIcon createImage(final CaptchaChallenge challenge) throws IOException {
        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.decode(challenge.getImage().substring(challenge.getImage().lastIndexOf(",")))));

        return new ImageIcon(image);
    }

    /**
     * @param args
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws APIException
     * @throws MyJDownloaderException
     * @throws DialogCanceledException
     * @throws DialogClosedException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws APIException, MyJDownloaderException, DialogClosedException, DialogCanceledException, IOException, InterruptedException {
        final BasicHTTP br = new BasicHTTP();

        JacksonMapper jm;
        JSonStorage.setMapper(jm = new JacksonMapper());

        MyJDJsonMapper.HANDLER = new JSonHandler() {

            @Override
            public String objectToJSon(final Object payload) {
                return JSonStorage.serializeToJson(payload);
            }

            @Override
            public <T> T jsonToObject(final String dec, final Type clazz) {
                return (T) JSonStorage.restoreFromString(dec, new TypeRef(clazz) {

                });
            }
        };

        jm.addSerializer(JsonFactoryInterface.class, new JsonSerializer<JsonFactoryInterface>() {

            @Override
            public String toJSonString(final JsonFactoryInterface list) {
                return list.toJsonString();

            }

            @Override
            public boolean canSerialize(final Object arg0) {
                return arg0 instanceof JsonFactoryInterface;
            }

        });
        AbstractDialog.setDefaultLocator(new RememberAbsoluteDialogLocator("MYJDTest"));
        br.putRequestHeader("Content-Type", "application/json; charset=utf-8");
        final int[] codes = new int[999];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = i;
        }
        br.setAllowedResponseCodes(codes);

        final AbstractMyJDClient api = new AbstractMyJDClient("Java Test Application") {

            @Override
            protected byte[] base64decode(final String base64encodedString) {
                return Base64.decode(base64encodedString);
            }

            @Override
            protected String base64Encode(final byte[] encryptedBytes) {
                return Base64.encodeToString(encryptedBytes, false);
            }

            @Override
            protected byte[] post(final String query, final String object, final byte[] keyAndIV) throws ExceptionResponse {
                HTTPConnection con = null;
                byte[] ret = null;
                try {
                    if (keyAndIV != null) {
                        br.putRequestHeader("Accept-Encoding", "gzip_aes");
                        final byte[] sendBytes = (object == null ? "" : object).getBytes("UTF-8");
                        final HashMap<String, String> header = new HashMap<String, String>();
                        header.put(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, "" + sendBytes.length);
                        final String url = getServerRoot() + query;
                        con = br.openPostConnection(new URL(url), null, new ByteArrayInputStream(sendBytes), header);
                        final String content_Encoding = con.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING);
                        if (con.getResponseCode() == 200) {
                            if ("gzip_aes".equals(content_Encoding)) {
                                final byte[] aes = IO.readStream(-1, con.getInputStream());
                                final byte[] decrypted = decrypt(aes, keyAndIV);
                                ret = IO.readStream(-1, new GZIPInputStream(new ByteArrayInputStream(decrypted)));
                            } else if (content_Encoding == null) {
                                // not encrypted
                                ret = IO.readStream(-1, con.getInputStream());
                            } else {
                                final byte[] aes = IO.readStream(-1, new Base64InputStream(con.getInputStream()));
                                final byte[] decrypted = decrypt(aes, keyAndIV);
                                ret = decrypted;
                            }
                        } else {
                            ret = IO.readStream(-1, con.getInputStream());
                        }
                    } else {
                        br.putRequestHeader("Accept-Encoding", null);
                        ret = br.postPage(new URL(getServerRoot() + query), object == null ? "" : object).getBytes("UTF-8");
                        con = br.getConnection();
                    }
                    System.out.println(con);
                    System.out.println(ret);

                    if (con != null && con.getResponseCode() > 0 && con.getResponseCode() != 200) {
                        //
                        throw new ExceptionResponse(new String(ret, "UTF-8"), con.getResponseCode());
                    }
                    return ret;
                } catch (final ExceptionResponse e) {
                    throw e;
                } catch (final Exception e) {
                    throw new ExceptionResponse(e);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }

            }

            @Override
            public String urlencode(final String text) {
                try {
                    return URLEncoder.encode(text, "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    throw new WTFException(e);

                }
            }

        };
        final Storage config = JSonStorage.getPlainStorage("APiClientTest");

        api.setServerRoot("http://api.jdownloader.org");

        // api.setServerRoot("http://localhost:10101");

        if (false) {
            api.connect(config.get("email", ""), config.get("password", ""));
            try {
                api.reconnect();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            try {
                api.reconnect();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            System.out.println(api.listDevices());
            api.disconnect();
            return;
        }
        if (false) {
            api.connect(config.get("email", ""), config.get("password", ""));
            try {
                api.reconnect();
            } catch (final Throwable e) {
            }
            final ArrayList<DeviceData> devices = api.listDevices().getList();
            api.registerNotification("blabla", devices.get(0), TYPE.CAPTCHA);
            final NotificationRequestMessage message = new NotificationRequestMessage();
            message.setType(TYPE.CAPTCHA);
            message.setRequested(false);
            boolean onoff = true;
            for (int i = 1; i < 100; i++) {
                message.setTimestamp(System.currentTimeMillis());
                message.setRequested(onoff);
                onoff = !onoff;
                // System.out.println("Send okay: " + api.pushNotification(message));
                api.registerNotification("blabla", devices.get(0), TYPE.CAPTCHA);
                // Thread.sleep(20);
            }
            // api.unregisterNotification("blabla", devices.get(0));
            api.disconnect();
            return;
        }

        try {

            String lastOption = TestClient.LOGIN;
            while (true) {
                try {
                    lastOption = (String) Dialog.getInstance().showComboDialog(0, "Please Choose", "Choose Test", new String[] { TestClient.LOGIN, TestClient.REGISTER, TestClient.TEST_CONTENTAPI, TestClient.TEST_ACCOUNTAPI, TestClient.TEST_CONFIGAPI, TestClient.CANCEL, TestClient.LIST_DEVICES, TestClient.EVENTS_TEST, TestClient.RESTORE_SESSION, TestClient.CHANGE_PASSWORD, TestClient.DISCONNECT, TestClient.CAPTCHA_CHALLENGE, TestClient.REGAIN_TOKEN, TestClient.CALL_UPTIME, TestClient.CALL_ACTION_WITHOUT_PARAMETERS, TestClient.GET_CAPTCHA_LIST, TestClient.WRITE_SESSION, TestClient.FEEDBACK, TestClient.KEEPALIVE }, lastOption, null, null, null, null);
                    if (TEST_CONFIGAPI == lastOption) {
                        final AdvancedConfigManagerAPI link = api.link(AdvancedConfigManagerAPI.class, chooseDevice(api));
                        ArrayList<AdvancedConfigEntryDataStorable> fullList = link.list(".*", true, true, true);
                        ArrayList<AdvancedConfigEntryDataStorable> captchacondif = link.list(".*captcha.*", false, true, false);
                        System.out.println(fullList);
                        System.out.println("maxforceddownloads = " + link.get("org.jdownloader.settings.GeneralSettings", null, "maxforceddownloads"));
                    } else if (TestClient.TEST_CONTENTAPI == lastOption) {
                        final ContentAPIV2 link = api.link(ContentAPIV2.class, chooseDevice(api));

                        Dialog.getInstance().showConfirmDialog(0, "Host icon", "", new ImageIcon(link.getFavIcon("jdownloader.org")), null, null);
                        Dialog.getInstance().showConfirmDialog(0, "File icon", "", new ImageIcon(link.getFileIcon("text.doc")), null, null);

                    } else if (TestClient.TEST_ACCOUNTAPI == lastOption) {

                        final AccountAPIV2 accounts = api.link(AccountAPIV2.class, chooseDevice(api));
                        final ArrayList<String> premiumhoster = accounts.listPremiumHoster();
                        final ArrayList<AccountStorable> list = accounts.listAccounts(new AccountQuery(0, -1, true, false, false, false, false, false));
                        System.out.println(list);
                    } else if (TestClient.EVENTS_TEST == lastOption) {
                        final DeviceList list = api.listDevices();
                        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
                        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);

                        final EventDistributor ed = new EventDistributor(api, list.getList().get(device).getId());

                        ed.subscribe(new String[] { ".*" }, null);
                        ed.getEventSender().addListener(new EventsDistributorListener() {

                            @Override
                            public void onNewMyJDEvent(final String publisher, final String eventid, final Object eventData) {
                                try {
                                    Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult", publisher + "." + eventid + ":" + JSonStorage.toString(eventData));
                                } catch (final DialogNoAnswerException e) {
                                    throw new RuntimeException(e);
                                }

                            }
                        });
                        new Thread(ed).start();

                    } else if (TestClient.KEEPALIVE == lastOption) {
                        api.keepalive();
                    } else if (TestClient.CHANGE_PASSWORD == lastOption) {
                        final String email = Dialog.getInstance().showInputDialog(0, "Enter Email", "Enter", null, null, null, null);
                        final CaptchaChallenge challenge = api.getChallenge();
                        final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                        challenge.setCaptchaResponse(response);
                        api.requestPasswordResetEmail(challenge, email);

                        api.finishPasswordReset(email, Dialog.getInstance().showInputDialog(0, "Confirmal Key", "Enter", null, null, null, null), Dialog.getInstance().showInputDialog(0, "New Password", "Enter", null, null, null, null));
                    } else if (TestClient.LIST_DEVICES == lastOption) {

                        final DeviceList list = api.listDevices();
                        Dialog.getInstance().showMessageDialog(JSonStorage.serializeToJson(list));
                    } else if (TestClient.DISCONNECT == lastOption) {
                        api.disconnect();

                        Dialog.getInstance().showMessageDialog("Disconnect OK");
                        System.exit(1);

                    } else if (TestClient.FEEDBACK == lastOption) {
                        final String message = Dialog.getInstance().showInputDialog(0, "Enter Feedback message", "Enter", null, null, null, null);
                        api.feedback(message);
                    } else if (TestClient.CAPTCHA_CHALLENGE == lastOption) {

                        final CaptchaChallenge challenge = api.getChallenge();
                        final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                        challenge.setCaptchaResponse(response);

                    } else if (TestClient.REGAIN_TOKEN == lastOption) {
                        api.reconnect();

                        Dialog.getInstance().showMessageDialog("Done. New SessionToken: " + JSonStorage.serializeToJson(api.getSessionInfo()));
                    } else if (TestClient.CALL_UPTIME == lastOption) {
                        final String dev = chooseDevice(api);
                        final Long uptime = api.callAction(dev, "/jd/uptime", long.class);
                        Dialog.getInstance().showMessageDialog("Uptime: " + uptime);
                    } else if (TestClient.CALL_ACTION_WITHOUT_PARAMETERS == lastOption) {
                        final DeviceList list = api.listDevices();
                        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
                        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
                        final String cmd = Dialog.getInstance().showInputDialog(0, "Enter Command", "Enter", config.get("cmd", "/jd/doSomethingCool"), null, null, null);
                        config.put("cmd", cmd);
                        final Object uptime = api.callAction(list.getList().get(device).getId(), cmd, Object.class);
                        Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult", "" + uptime);

                    } else if (TestClient.GET_CAPTCHA_LIST == lastOption) {
                        final DeviceList list = api.listDevices();
                        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
                        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
                        final ArrayList uptime = api.callAction(list.getList().get(device).getId(), "/captcha/list", ArrayList.class);
                        Dialog.getInstance().showMessageDialog("Uptime: " + uptime);
                    } else if (TestClient.WRITE_SESSION == lastOption) {

                        config.put("session", JSonStorage.serializeToJson(api.getSessionInfo()));
                        ShutdownController.getInstance().requestShutdown();

                    } else if (TestClient.LOGIN == lastOption) {
                        final LoginDialog login = new LoginDialog(0);
                        login.setMessage("MyJDownloader Account Logins");
                        login.setRememberDefault(true);
                        login.setUsernameDefault(config.get("email", ""));
                        login.setPasswordDefault(config.get("password", ""));
                        final LoginData li = Dialog.getInstance().showDialog(login);
                        if (li.isSave()) {
                            config.put("email", li.getUsername());

                            config.put("password", li.getPassword());
                        }
                        try {
                            api.connect(li.getUsername(), li.getPassword());
                        } catch (final EmailNotValidatedException e) {
                            final CaptchaChallenge challenge = api.getChallenge();

                            final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                            challenge.setCaptchaResponse(response);

                            api.requestRegistrationEmail(challenge, li.getUsername(), null);
                            api.finishRegistration(Dialog.getInstance().showInputDialog(0, "Email Confirmal Key", "Enter", null, null, null, null), li.getUsername(), li.getPassword());
                            api.connect(li.getUsername(), li.getPassword());

                        }

                    } else if (TestClient.REGISTER == lastOption) {
                        final LoginDialog login = new LoginDialog(0);
                        login.setMessage("MyJDownloader Account Register");
                        login.setRememberDefault(true);
                        login.setUsernameDefault(config.get("email", ""));
                        login.setPasswordDefault(config.get("password", ""));

                        final LoginData li = Dialog.getInstance().showDialog(login);
                        if (li.isSave()) {
                            config.put("email", li.getUsername());

                            config.put("password", li.getPassword());
                        }
                        try {
                            final CaptchaChallenge challenge = api.getChallenge();
                            try {
                                final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                                challenge.setCaptchaResponse(response);

                                api.requestRegistrationEmail(challenge, li.getUsername(), null);
                            } catch (final ExceptionResponse e) {
                                e.printStackTrace();

                            }
                            api.finishRegistration(Dialog.getInstance().showInputDialog(0, "Email Confirmal Key", "Enter", null, null, null, null), li.getUsername(), li.getPassword());

                        } catch (final EmailNotValidatedException e) {
                            final CaptchaChallenge challenge = api.getChallenge();
                            try {
                                final String response = Dialog.getInstance().showInputDialog(0, "Enter Captcha to register", "Enter", null, TestClient.createImage(challenge), null, null);
                                challenge.setCaptchaResponse(response);

                                api.requestRegistrationEmail(challenge, li.getUsername(), null);
                            } catch (final ExceptionResponse e1) {
                                e1.printStackTrace();

                            }
                            api.finishRegistration(Dialog.getInstance().showInputDialog(0, "Email Confirmal Key", "Enter", null, null, null, null), li.getUsername(), li.getPassword());
                            api.connect(li.getUsername(), li.getPassword());

                        }

                    } else if (TestClient.RESTORE_SESSION == lastOption) {
                        final String session = config.get("session", "");
                        final SessionInfo sessioninfo = JSonStorage.restoreFromString(session, SessionInfo.class);
                        api.setSessionInfo(sessioninfo);

                        api.reconnect();
                        Dialog.getInstance().showMessageDialog("Done. New SessionToken: " + JSonStorage.serializeToJson(api.getSessionInfo()));
                    } else if (TestClient.CANCEL == lastOption) {
                        final String email = Dialog.getInstance().showInputDialog(0, "Email", "Enter", config.get("email", ""), null, null, null);
                        final String key = Dialog.getInstance().showInputDialog(0, "Email Cancel Key", "Enter", null, null, null, null);
                        api.cancelRegistrationEmail(email, key);
                    }
                } catch (final DialogNoAnswerException e) {
                    System.exit(1);
                } catch (final Exception e) {

                    Dialog.getInstance().showExceptionDialog("Error!!", e.getClass().getSimpleName() + ": " + e, e);
                }
            }

        } catch (final Exception e) {
            e.printStackTrace();
            Dialog.getInstance().showExceptionDialog("Error!!", e.getClass().getSimpleName() + ": " + e, e);
        }
        // List<FilePackageAPIStorable> ret = api.link(DownloadsAPI.class, "downloads").queryPackages(new APIQuery());
        // List<CaptchaJob> list = api.link(CaptchaAPI.class, "captcha").list();
        // System.out.println(list);
        //
        // Long uptime = api.callAction("/jd/uptime", long.class);
        // System.out.println(uptime);
        // System.out.println(ret);
        // api.disconnect();
        // System.out.println(jdapi.uptime());
    }

    protected static String chooseDevice(final AbstractMyJDClient api) throws MyJDownloaderException, DialogClosedException, DialogCanceledException {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
        if (list.getList().size() == 1) { return list.getList().get(0).getId(); }
        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
        final String dev = list.getList().get(device).getId();
        return dev;
    }
}
