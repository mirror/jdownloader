package org.jdownloader.api.myjdownloader;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultIntArrayValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.InitHook;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;

//org.jdownloader.extensions.myjdownloader.MyJDownloaderExtension has been the old path of the settingsfile
@InitHook(MyJDownloaderSettingsInitHook.class)
public interface MyJDownloaderSettings extends ConfigInterface {
    
    public static enum DIRECTMODE {
        @EnumLabel("Disable direct connections")
        NONE,
        @EnumLabel("Only allow direct connections from lan")
        LAN,
        @EnumLabel("Allow lan/wan connections with manual port forwarding")
        LAN_WAN_MANUAL,
        @EnumLabel("Allow lan/wan connections with automatic port forwarding via upnp")
        LAN_WAN_UPNP
    }
    
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
    
    public String getUniqueDeviceIDSalt();
    
    public void setUniqueDeviceIDSalt(String salt);
    
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
    
    @AboutConfig
    @DefaultEnumValue("LAN")
    @RequiresRestart("A JDownloader Restart is Required")
    public DIRECTMODE getDirectConnectMode();
    
    public void setDirectConnectMode(DIRECTMODE mode);
    
    @AboutConfig
    @DefaultIntValue(3129)
    @SpinnerValidator(min = 1025, max = 65000)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Try to use given local port for DirectConnectMode=LAN_WAN_MANUAL")
    public int getManualLocalPort();
    
    public void setManualLocalPort(int port);
    
    @AboutConfig
    @DefaultIntValue(0)
    public int getLastLocalPort();
    
    public void setLastLocalPort(int port);
    
    @AboutConfig
    @DefaultIntValue(0)
    public int getLastUpnpPort();
    
    public void setLastUpnpPort(int port);
    
    @AboutConfig
    @DefaultIntValue(3129)
    @RequiresRestart("A JDownloader Restart is Required")
    @SpinnerValidator(min = 80, max = 65000)
    @DescriptionForConfigEntry("Try to use given remove port for DirectConnectMode=LAN_WAN_MANUAL")
    public int getManualRemotePort();
    
    public void setManualRemotePort(int port);
    
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
