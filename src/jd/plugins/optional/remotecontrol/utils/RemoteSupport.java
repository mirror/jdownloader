package jd.plugins.optional.remotecontrol.utils;

public interface RemoteSupport {
    /**
     * Example contents of this method: t.setCommand(Command);
     * t.setInfo(Description); Note: Addon commands must begin with /addon/
     */
    void initCmdTable();

    Object handleRemoteCmd(String cmd);
}
