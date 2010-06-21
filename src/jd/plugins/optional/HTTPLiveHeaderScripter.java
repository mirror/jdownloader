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

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.ImportRouterDialog;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@OptionalPlugin(rev = "$Revision$", id = "livescripter", hasGui = true, interfaceversion = 5)
public class HTTPLiveHeaderScripter extends PluginOptional {
    public HTTPLiveHeaderScripter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ClosableView tabbedPanel;

    private JMenuItem menHelpWiki, menEditAddDefine, menEditAddRequest, menEditAddWait, menEditValidate;

    private JMenuItem menImportFile, menImportHTTPLive, menImportJDLH, menSave;

    private JTextArea textArea;

    private MenuAction action;

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            initGUI();
            SwingGui.getInstance().setContent(tabbedPanel);
        } else {
            if (tabbedPanel != null) tabbedPanel.close();
        }
        if (action != null && action.isSelected() != b) action.setSelected(b);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == action) {
            setGuiEnable(action.isSelected());
        } else if (e.getSource() == menImportHTTPLive) {
            importFF();
        } else if (e.getSource() == menHelpWiki) {
            try {
                JLink.openURL(JDL.L("jd.plugins.optional.HTTPLiveHeaderScripter.actionPerformed.wikilink", "http://jdownloader.org/knowledge/wiki/reconnect/live-header-script"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource() == menImportJDLH) {
            importLHScript();
        } else if (e.getSource() == menImportFile) {
            File[] files = UserIO.getInstance().requestFileChooser(null, null, null, new JDFileFilter(JDL.L("plugins.optional.httpliveheaderscripter.gui.desc", "XML-File"), ".xml", true), null);
            if (files == null) return;

            try {
                Vector<String> save = (Vector<String>) JDIO.loadObject(files[0], true);
                textArea.setText(save.get(2));
            } catch (Exception e2) {
                textArea.setText(JDIO.readFileToString(files[0]));
            }
        } else if (e.getSource() == menSave) {
            File[] files = UserIO.getInstance().requestFileChooser(null, null, null, new JDFileFilter(JDL.L("plugins.optional.httpliveheaderscripter.gui.desc", "XML-File"), ".xml", true), null, null, UserIO.SAVE_DIALOG);
            if (files == null) return;

            String manu = UserIO.getInstance().requestInputDialog(0, JDL.L("plugins.optional.httpliveheaderscripter.gui.save.manufacturer", "Manufacturer? (e.g. Siemens)"), null);
            if (manu == null) return;
            String model = UserIO.getInstance().requestInputDialog(0, JDL.L("plugins.optional.httpliveheaderscripter.gui.save.model", "Model? (e.g. Gigaset 555 (fw 3.01.05)"), null);
            if (model == null) return;

            ArrayList<String> save = new ArrayList<String>();
            save.add(manu);
            save.add(model);
            save.add(textArea.getText());
            save.add("?s).*" + manu.toLowerCase() + ".*");

            JDIO.saveObject(save, files[0], true);
        } else if (e.getSource() == menEditValidate) {
            if (validate()) {
                UserIO.getInstance().requestMessageDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.validate.ok", "Script is Valid."));
            }
        } else if (e.getSource() == menEditAddRequest) {
            if (!validate()) return;
            String script = textArea.getText();
            int id = Math.max(script.lastIndexOf("[[[/STEP]]]") + 11, script.lastIndexOf("[[[HSRC]]]") + 9);
            StringBuilder sb = new StringBuilder();
            sb.append(script.substring(0, id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]\r\n");
            sb.append("          [[[REQUEST]]]\r\n\r\n");
            sb.append("          [[[/REQUEST]]]\r\n");
            sb.append("     [[[/STEP]]]\r\n");
            sb.append(script.substring(id).trim());
            textArea.setText(sb.toString());
        } else if (e.getSource() == menEditAddDefine) {
            if (!validate()) { return; }
            String script = textArea.getText();
            int id = Math.max(script.indexOf("[[[STEP]]]"), script.indexOf("[[[HSRC]]]") + 9);
            StringBuilder sb = new StringBuilder();
            sb.append(script.substring(0, id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]\r\n");
            sb.append("          [[[DEFINE variablename=\"VALUE\"/]]]\r\n");
            sb.append("     [[[/STEP]]]\r\n");
            sb.append(script.substring(id).trim());
            textArea.setText(sb.toString());
        } else if (e.getSource() == menEditAddWait) {
            if (!validate()) { return; }
            String script = textArea.getText();
            int id = Math.max(script.lastIndexOf("[[[/STEP]]]") + 11, script.lastIndexOf("[[[HSRC]]]") + 9);
            StringBuilder sb = new StringBuilder();
            sb.append(script.substring(0, id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]\r\n");
            sb.append("          [[[WAIT seconds=\"3\"/]]]\r\n");
            sb.append("     [[[/STEP]]]\r\n");
            sb.append(script.substring(id).trim());
            textArea.setText(sb.toString());
        }
    }

    private String convertFF(String script) {
        String[] forbidden = new String[] { "jpg", "gif", "ico", "png", "mid", "css", "js", "xml" };
        String[] forbiddenHeaders = new String[] { "accept", "user-agent", "If-Modified-Since", "Cache-Control" };

        ArrayList<String> ips = new ArrayList<String>();
        String[] requests = script.split("----------------------------------------------------------");
        StringBuilder sb = new StringBuilder();
        for (String request : requests) {

            String[] lines = Regex.getLines(request.trim());
            if (lines.length < 3) {
                continue;
            }
            String url = lines[0];
            if (url.indexOf("?") > 0) {
                url = url.substring(0, url.indexOf("?"));
            }
            boolean f = false;
            for (String end : forbidden) {
                if (url.toLowerCase().endsWith(end.toLowerCase())) {
                    f = true;
                    break;
                }
            }
            if (f) {
                continue;
            }
            sb.append("     [[[STEP]]]\r\n");
            sb.append("          [[[REQUEST]]]\r\n");
            boolean post = lines[2].trim().toLowerCase().startsWith("post");
            lines: for (int i = 2; i < lines.length; i++) {
                lines[i] = lines[i].trim();
                if (lines[i].toLowerCase().startsWith("http/")) {
                    break;
                }

                for (String forb : forbiddenHeaders) {
                    if (lines[i].toLowerCase().startsWith(forb.toLowerCase())) {
                        continue lines;
                    }
                }
                if (lines[i].toLowerCase().startsWith("host:")) {
                    String ip = lines[i].substring(6).trim();
                    if (!ips.contains(ip)) {
                        ips.add(ip);

                    }
                    sb.append("Host: %%%IP" + ips.indexOf(ip) + "%%%\r\n");

                } else {
                    if (post) {
                        if (lines.length > i + 1 && lines[i + 1].toLowerCase().startsWith("http/")) {
                            sb.append("\r\n");
                        }
                    }
                    sb.append(lines[i].trim() + "\r\n");
                }
            }
            sb.append("          [[[/REQUEST]]]\r\n");
            sb.append("     [[[/STEP]]]\r\n");
        }

        int i = 0;

        String sc = sb.toString();
        sb = new StringBuilder();
        sb.append("     [[[STEP]]]\r\n");
        sb.append("          [[[DEFINE");
        for (String ip : ips) {

            sb.append(" ip");
            sb.append(i);
            sb.append("=");
            sb.append("\"");
            sb.append(ip);
            sb.append("\"");

            sc = sc.replaceAll(ip, "%%%ip" + i + "%%%");
            i++;
        }
        sb.append("/]]]\r\n");
        sb.append("     [[[/STEP]]]\r\n");
        i++;

        StringBuilder ret = new StringBuilder();
        ret.append("[[[HSRC]]]\r\n");
        ret.append(sb);
        ret.append(sc);
        ret.append("[[[/HSRC]]]");
        return ret.toString();
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(action);

        return menu;
    }

    @Override
    public boolean initAddon() {
        action = new MenuAction("livescripter", 0);
        action.setActionListener(this);
        action.setIcon(this.getIconKey());
        action.setSelected(false);
        return true;
    }

    @Override
    public String getIconKey() {
        return "gui.images.reconnect_settings";
    }

    private void initGUI() {
        if (tabbedPanel != null) return;

        tabbedPanel = new ClosableView() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onShow() {
            }

            @Override
            public Icon getIcon() {
                return JDTheme.II(getIconKey(), 16, 16);
            }

            @Override
            public void onHide() {
            }

            @Override
            public String getTitle() {
                return JDL.L("jd.plugins.optional.HTTPLiveHeaderScripter.title", "HLH Scripter");
            }

            @Override
            public String getTooltip() {
                return JDL.L("jd.plugins.optional.HTTPLiveHeaderScripter.tooltip", "HTTP-Live-Header Scripter: Create Reconnect scripts easily");
            }

            @Override
            protected void initMenu(JMenuBar menuBar) {

                // Filemenu
                JMenu menFile = new JMenu(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file", "File"));

                menImportFile = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importfile", "Open file"));
                menSave = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.save", "Save file"));
                menImportHTTPLive = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importhhtplive", "Import Firefox LiveHeader Script"));
                menImportJDLH = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importjdlh", "Import JD-LiveHeader Script"));

                menImportFile.addActionListener(HTTPLiveHeaderScripter.this);
                menSave.addActionListener(HTTPLiveHeaderScripter.this);
                menImportHTTPLive.addActionListener(HTTPLiveHeaderScripter.this);
                menImportJDLH.addActionListener(HTTPLiveHeaderScripter.this);

                menFile.add(menImportFile);
                menFile.add(menSave);
                menFile.addSeparator();
                menFile.add(menImportHTTPLive);
                menFile.add(menImportJDLH);

                // EditMenu
                JMenu menEdit = new JMenu(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit", "Edit"));

                menEditValidate = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.validate", "Validate current script"));
                menEditAddRequest = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addrequest", "Add request tag"));
                menEditAddDefine = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.adddefine", "Add define tag"));
                menEditAddWait = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addwait", "Add wait tag"));

                menEditValidate.addActionListener(HTTPLiveHeaderScripter.this);
                menEditAddRequest.addActionListener(HTTPLiveHeaderScripter.this);
                menEditAddDefine.addActionListener(HTTPLiveHeaderScripter.this);
                menEditAddWait.addActionListener(HTTPLiveHeaderScripter.this);

                menEdit.add(menEditValidate);
                menEdit.addSeparator();
                menEdit.add(menEditAddRequest);
                menEdit.add(menEditAddDefine);
                menEdit.add(menEditAddWait);

                // HelpMenu
                JMenu menHelp = new JMenu(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.help", "Help"));

                menHelpWiki = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.help.wiki", "Howto"));
                menHelpWiki.addActionListener(HTTPLiveHeaderScripter.this);

                menHelp.add(menHelpWiki);

                menuBar.add(menFile);
                menuBar.add(menEdit);
                menuBar.add(menHelp);

            }

            @Override
            public String getID() {
                return "livescripter";
            }

        };

        tabbedPanel.getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                switch (event.getID()) {
                case SwitchPanelEvent.ON_REMOVE:
                    action.setSelected(false);
                    break;
                }

            }

        });
        tabbedPanel.init();
        textArea = new JTextArea();
        textArea.setText("[[[HSRC]]]\r\n\r\n\r\n[[[/HSRC]]]");
        textArea.setEditable(true);
        textArea.requestFocusInWindow();

        SwitchPanel sp = new SwitchPanel() {

            private static final long serialVersionUID = -258117300311991599L;

            @Override
            protected void onHide() {
            }

            @Override
            protected void onShow() {
            }

        };
        sp.setLayout(new MigLayout("ins 0", "[grow, fill]", "[grow, fill]"));
        sp.add(new JScrollPane(textArea));
        tabbedPanel.setContent(sp);

    }

    @Override
    public void onExit() {
    }

    private void importFF() {
        String script = UserIO.getInstance().requestTextAreaDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.importff.title", "Import Firefox Live header Log"), JDL.L("plugins.optional.httpliveheaderscripter.gui.importff.message", "Insert your firefox Liveaheader Log here"), null);

        script = convertFF(script);

        textArea.setText(script);
    }

    private void importLHScript() {
        String[] data = ImportRouterDialog.showDialog();
        if (data != null) {
            if (data[2].toLowerCase().indexOf("curl") >= 0) {
                UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
            }
            textArea.setText(data[2]);
        }
    }

    private boolean validate() {
        String script = textArea.getText();

        if (script == null) {
            UserIO.getInstance().requestMessageDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.validate.error2", "Script not valid"));
            return false;
        }
        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST>", "<REQUEST><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
        script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        Document xmlScript;

        try {
            xmlScript = JDUtilities.parseXmlString(script, false);
            Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                String error = "Root Node must be [[[HSRC]]]*[/HSRC]";
                UserIO.getInstance().requestMessageDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid:") + error);
                return false;
            }
            // RequestInfo requestInfo = null;
            NodeList steps = root.getChildNodes();

            for (int step = 0; step < steps.getLength(); step++) {

                Node current = steps.item(step);

                if (current.getNodeType() == 3) {

                    continue;
                }
                if (!current.getNodeName().equalsIgnoreCase("STEP")) {

                    String error = "Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName();
                    UserIO.getInstance().requestMessageDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid:") + error);
                    return false;
                }
                // NodeList toDos = current.getChildNodes();
            }

        } catch (Exception e) {
            String error = e.getMessage();
            UserIO.getInstance().requestMessageDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid:") + error);
            return false;
        }

        return true;
    }
}
