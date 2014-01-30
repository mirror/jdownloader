package org.jdownloader.api.test;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.Storage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.utils.IO;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogInterface;
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
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable;
import org.jdownloader.myjdownloader.client.bindings.EnumOptionStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigInterface;
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

    private static TestLogin LOGIN;

    static ImageIcon createImage(final CaptchaChallenge challenge) throws IOException {
        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.decode(challenge.getImage().substring(challenge.getImage().lastIndexOf(",")))));

        return new ImageIcon(image);
    }

    public static abstract class Test {
        private static DeviceData lastDevice;

        public abstract void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception;

        public String getName() {
            return getClass().getSimpleName();
        }

        protected static String chooseDevice(final AbstractMyJDClientForDesktopJVM api) throws MyJDownloaderException, DialogClosedException, DialogCanceledException {
            final DeviceList list = api.listDevices();
            if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
            if (list.getList().size() == 1) { return list.getList().get(0).getId(); }

            int defIndex = 0;
            ArrayList<DeviceData> devList = list.getList();
            if (lastDevice != null) defIndex = devList.indexOf(lastDevice);
            if (defIndex < 0) defIndex = 0;
            final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", devList.toArray(new DeviceData[] {}), defIndex, null, null, null, null);
            lastDevice = list.getList().get(device);
            final String dev = lastDevice.getId();

            return dev;
        }
    }

    public static class TestLogin extends Test {

        @Override
        public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
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
        }

        @Override
        public String getName() {
            return "Login";
        }

    }

    public static class ConfigTest extends Test {

        @Override
        public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
            final AdvancedConfigInterface link = api.link(AdvancedConfigInterface.class, chooseDevice(api));
            ArrayList<AdvancedConfigEntryDataStorable> fullList = link.list(".*", true, true, true);

            ArrayList<AdvancedConfigEntryDataStorable> captchacondif = link.list(".*captcha.*", false, true, false);
            System.out.println(fullList);
            System.out.println("maxforceddownloads = " + link.get("org.jdownloader.settings.GeneralSettings", null, "maxforceddownloads"));

            ArrayList<AdvancedConfigEntryDataStorable> trigger = link.list("org.jdownloader.settings.GeneralSettings.CreateFolderTrigger", false, true, false);
            ArrayList<EnumOptionStorable> enumValues = link.listEnum(trigger.get(0).getType());
            System.out.println(enumValues);
        }

        @Override
        public String getName() {
            return "Test Config";
        }
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

        register(LOGIN = new TestLogin());
        register(new ConfigTest());
        register(new AccountTest());
        register(new EventsTest());
        register(new DownloadListTest());
        register(new LinkgrabberTest());
        register(new KeepAliveTest());

        register(new DialogTest());
        register(new ChangePasswordTest());

        register(new ListDevicesTest());
        register(new DisconnectTest());

        register(new FeedbackTest());

        register(new ChallengeTest());

        register(new RegainTest());

        register(new GetUptimeTest());

        register(new GenericCallTest());

        register(new CaptchaListTest());

        register(new WriteSessionTest());

        register(new RegisterTest());

        register(new RestoreSessionTest());
        register(new TerminateAccountTest());

        register(new CancelRegistrationTest());
        JacksonMapper jm;
        JSonStorage.setMapper(jm = new JacksonMapper());

        MyJDJsonMapper.HANDLER = new JSonHandler<Type>() {

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

        final AbstractMyJDClientForDesktopJVM api = new AbstractMyJDClientForDesktopJVM("Java Test Application") {

            @Override
            protected byte[] base64decode(final String base64encodedString) {
                return Base64.decode(base64encodedString);
            }

            @Override
            protected void log(String json) {
                System.out.println(json);
            }

            @Override
            protected String base64Encode(final byte[] encryptedBytes) {
                return Base64.encodeToString(encryptedBytes, false);
            }

            @Override
            protected byte[] post(final String query, final String object, final byte[] keyAndIV) throws ExceptionResponse {
                HTTPConnection con = null;
                byte[] ret = null;
                final BasicHTTP br = new BasicHTTP();
                br.setLogger(new LogInterface() {

                    @Override
                    public void log(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void info(String msg) {
                        System.out.println(msg);
                    }

                    @Override
                    public void fine(String string) {
                        System.out.println(string);
                    }
                });
                br.putRequestHeader("Content-Type", "application/json; charset=utf-8");
                final int[] codes = new int[999];
                for (int i = 0; i < codes.length; i++) {
                    codes[i] = i;
                }
                br.setAllowedResponseCodes(codes);

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

        };
        final Storage config = JSonStorage.getPlainStorage("APiClientTest");

        api.setServerRoot("http://api.jdownloader.org");

        // api.setServerRoot("http://192.168.2.110:10101");

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

            Test lastOption = LOGIN;
            while (true) {
                try {
                    lastOption = (Test) Dialog.getInstance().showComboDialog(0, "Please Choose", "Choose Test", TESTS.toArray(new Test[] {}), lastOption, null, null, null, new ListCellRenderer() {
                        private ListCellRenderer org;
                        {
                            org = new JComboBox().getRenderer();
                        }

                        @Override
                        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                            return org.getListCellRendererComponent(list, ((Test) value).getName(), index, isSelected, cellHasFocus);

                        }

                    });

                    lastOption.run(config, api);

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

    private static List<Test> TESTS = new ArrayList<Test>();

    private static void register(Test testLogin) {
        TESTS.add(testLogin);
    }

}
