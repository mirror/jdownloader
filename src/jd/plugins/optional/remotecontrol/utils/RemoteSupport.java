package jd.plugins.optional.remotecontrol.utils;

import jd.plugins.optional.remotecontrol.helppage.HelpPage;
import jd.plugins.optional.remotecontrol.helppage.Table;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;

public interface RemoteSupport {

    Table t = HelpPage.createAddonTable(new Table(""));

    Document xmlDocument = JDUtilities.parseXmlString("<jdownloader></jdownloader>", false);

    /**
     * Example contents of this method: t.setName(AddonName);
     */
    void setCmdTableName();

    /**
     * Example contents of this method: t.setCommand(Command);
     * t.setInfo(Description); Note: Addon commands must begin with /addon/
     */
    void initCmdTable();

    Object handleRemoteCmd(String cmd);
}
