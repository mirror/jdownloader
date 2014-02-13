package org.jdownloader.api.myjdownloader;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultIntArrayValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.InitHook;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;

//org.jdownloader.extensions.myjdownloader.MyJDownloaderExtension has been the old path of the settingsfile
@InitHook(MyJDownloaderSettingsInitHook.class)
public interface MyJDownloaderSettings extends ConfigInterface {

    @DefaultStringValue("api.jdownloader.org")
    @AboutConfig
    public String getConnectIP();

    public void setConnectIP(String url);

    @DefaultIntArrayValue({ 80, 10101 })
    @AboutConfig
    public int[] getDeviceConnectPorts();

    public void setDeviceConnectPorts(int port[]);

    @DefaultIntValue(80)
    @AboutConfig
    public int getClientConnectPort();

    public void setClientConnectPort(int port);

    public String getEmail();

    public void setEmail(String email);

    public String getPassword();

    public void setPassword(String s);

    public String getUniqueDeviceID();

    public void setUniqueDeviceID(String id);

    public static class DeviceNameFactory extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return "JDownloader@" + System.getProperty("user.name", "User");
        }
    }

    @AboutConfig
    @DefaultFactory(DeviceNameFactory.class)
    public String getDeviceName();

    public void setDeviceName(String name);

    @AboutConfig
    @DefaultBooleanValue(true)
    public void setAutoConnectEnabledV2(boolean b);

    public boolean isAutoConnectEnabledV2();

    public static enum MyJDownloaderError {
        @EnumLabel("Outdated, please update your JDownloader")
        OUTDATED,
        @EnumLabel("No Error -  everything is fine")
        NONE,
        @EnumLabel("Username/email is unknown")
        EMAIL_INVALID,
        @EnumLabel("Please confirm your account(Click the link in the Confirmal Email)")
        ACCOUNT_UNCONFIRMED,
        @EnumLabel("Wrong Username or Password")
        BAD_LOGINS,
        @EnumLabel("Service is down for Maintenance")
        SERVER_DOWN,
        @EnumLabel("Connection problem to the MyJDownloader Service")
        IO,
        @EnumLabel("Unknown error")
        UNKNOWN,
        @EnumLabel("No Internet Connection")
        NO_INTERNET_CONNECTION,

    }

    @AboutConfig
    @DefaultEnumValue("NONE")
    public void setLatestError(MyJDownloaderError error);

    public MyJDownloaderError getLatestError();

}
