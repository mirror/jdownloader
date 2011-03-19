package jd.plugins.optional.interfaces;

public interface RemoteSupport {
    /**
     * Example contents of this method: t.setCommand(Command);
     * t.setInfo(Description); Note: Addon commands must begin with /addon/
     */
    void initCmdTable();

    Object handleRemoteCmd(String cmd);
}
