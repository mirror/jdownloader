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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.controlling.reconnect.HTTPLiveHeader;
import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Encoding;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@OptionalPlugin(rev = "$Revision$", id = "livescripter", interfaceversion = 4)
public class HTTPLiveHeaderScripter extends PluginOptional {
    public HTTPLiveHeaderScripter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private SwitchPanel tabbedPanel;

    private JMenuItem menHelpWiki, menEditAddDefine, menEditAddRequest, menEditAddWait, menEditValidate;

    private JMenuItem menImportFile, menImportHTTPLive, menImportJDLH, menSave;

    private JTextArea textArea;

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
            initGUI();
            SwingGui.getInstance().setContent(tabbedPanel);
        } else if (e.getSource() == menImportHTTPLive) {
            importFF();
        } else if (e.getSource() == menHelpWiki) {
            try {
                JLink.openURL(JDL.L("plugins.optional.httpliveheaderscripter.gui.wikilink", "http://wiki.jdownloader.org/index.php?title=HTTPLiveHeader_reconnect_Script_erstellen"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource() == menImportJDLH) {
            importLHScript();
        } else if (e.getSource() == menImportFile) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDL.L("plugins.optional.httpliveheaderscripter.gui.openfile", "Open"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new JDFileFilter(JDL.L("plugins.optional.httpliveheaderscripter.gui.desc", "XML-File"), ".xml", true));
            fc.showSaveDialog(tabbedPanel);
            File ret = fc.getSelectedFile();
            if (ret == null || !ret.exists()) { return; }
            try {
                Vector<String> save = (Vector<String>) JDIO.loadObject(tabbedPanel, ret, true);
                textArea.setText(save.get(2));
            } catch (Exception e2) {
                textArea.setText(JDIO.getLocalFile(ret));
            }

        } else if (e.getSource() == menSave) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDL.L("gui.btn_save", "Save"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new JDFileFilter(JDL.L("plugins.optional.httpliveheaderscripter.gui.desc", "XML-File"), ".xml", true));
            fc.showSaveDialog(tabbedPanel);
            File ret = fc.getSelectedFile();
            if (ret == null) { return; }
            if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("xml")) {
                ret = new File(ret.getAbsolutePath() + ".xml");
            }
            ArrayList<String> save = new ArrayList<String>();
            String manu = UserIO.getInstance().requestInputDialog(0, JDL.L("plugins.optional.httpliveheaderscripter.gui.save.manufactur", "Manufactur? (e.g. Siemens)"), null);
            String model = UserIO.getInstance().requestInputDialog(0, JDL.L("plugins.optional.httpliveheaderscripter.gui.save.model", "Model? (e.g. Gigaset 555 (fw 3.01.05)"), null);
            if (manu == null || model == null) { return; }
            save.add(manu);
            save.add(model);
            save.add(textArea.getText());
            save.add("?s).*" + manu.toLowerCase() + ".*");

            JDIO.saveObject(tabbedPanel, save, ret, null, null, true);
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
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[REQUEST]]]" + "\r\n\r\n");
            sb.append("          [[[/REQUEST]]]" + "\r\n");
            sb.append("     [[[/STEP]]]" + "\r\n");
            sb.append(script.substring(id).trim());
            textArea.setText(sb + "");
        } else if (e.getSource() == menEditAddDefine) {
            if (!validate()) { return; }
            String script = textArea.getText();
            int id = Math.max(script.indexOf("[[[STEP]]]"), script.indexOf("[[[HSRC]]]") + 9);
            StringBuilder sb = new StringBuilder();
            sb.append(script.substring(0, id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[DEFINE variablename=\"VALUE\"/]]]" + "\r\n");
            sb.append("     [[[/STEP]]]" + "\r\n");
            sb.append(script.substring(id).trim());
            textArea.setText(sb + "");
        } else if (e.getSource() == menEditAddWait) {
            if (!validate()) { return; }
            String script = textArea.getText();
            int id = Math.max(script.lastIndexOf("[[[/STEP]]]") + 11, script.lastIndexOf("[[[HSRC]]]") + 9);
            StringBuilder sb = new StringBuilder();
            sb.append(script.substring(0, id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[WAIT seconds=\"3\"/]]]" + "\r\n");
            sb.append("     [[[/STEP]]]" + "\r\n");
            sb.append(script.substring(id).trim());
            textArea.setText(sb + "");
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
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[REQUEST]]]" + "\r\n");
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
                    sb.append("Host: %%%IP" + ips.indexOf(ip) + "%%%" + "\r\n");

                } else {
                    if (post) {
                        if (lines.length > i + 1 && lines[i + 1].toLowerCase().startsWith("http/")) {
                            sb.append("\r\n");
                        }
                    }
                    sb.append(lines[i].trim() + "\r\n");
                }
            }
            sb.append("          [[[/REQUEST]]]" + "\r\n");
            sb.append("     [[[/STEP]]]" + "\r\n");
        }

        int i = 0;

        String sc = sb.toString();
        sb = new StringBuilder();
        sb.append("     [[[STEP]]]" + "\r\n");
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
        sb.append("/]]]" + "\r\n");
        sb.append("     [[[/STEP]]]" + "\r\n");
        i++;

        StringBuilder ret = new StringBuilder();
        ret.append("[[[HSRC]]]" + "\r\n");
        ret.append(sb);
        ret.append(sc);

        ret.append("[[[/HSRC]]]");
        return ret + "";
    }

    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(getHost(), 0).setActionListener(this));

        return menu;
    }

    public boolean initAddon() {
        return true;
    }

    private void initGUI() {
        if (tabbedPanel != null) return;

        tabbedPanel = new SwitchPanel() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onShow() {
            }

            @Override
            public void onHide() {
            }

        };

        textArea = new JTextArea();
        textArea.setText("[[[HSRC]]]\r\n\r\n\r\n[[[/HSRC]]]");
        textArea.setEditable(true);
        textArea.requestFocusInWindow();

        tabbedPanel.setLayout(new BorderLayout());
        tabbedPanel.add(buildMenu(), BorderLayout.PAGE_START);
        tabbedPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    private JMenuBar buildMenu() {
        // Filemenu
        JMenu menFile = new JMenu(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file", "File"));

        menImportFile = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importfile", "Open file"));
        menSave = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.save", "Save file"));
        menImportHTTPLive = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importhhtplive", "Import Firefox LiveHeader Script"));
        menImportJDLH = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importjdlh", "Import JD-LiveHeader Script"));

        menImportFile.addActionListener(this);
        menSave.addActionListener(this);
        menImportHTTPLive.addActionListener(this);
        menImportJDLH.addActionListener(this);

        menFile.add(menImportFile);
        menFile.add(menSave);
        menFile.add(new JSeparator());
        menFile.add(menImportHTTPLive);
        menFile.add(menImportJDLH);

        // EditMenu
        JMenu menEdit = new JMenu(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit", "Edit"));

        menEditValidate = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.validate", "Validate current script"));
        menEditAddRequest = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addrequest", "Add request tag"));
        menEditAddDefine = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.adddefine", "Add define tag"));
        menEditAddWait = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addwait", "Add wait tag"));

        menEditValidate.addActionListener(this);
        menEditAddRequest.addActionListener(this);
        menEditAddDefine.addActionListener(this);
        menEditAddWait.addActionListener(this);

        menEdit.add(menEditValidate);
        menEdit.add(new JSeparator());
        menEdit.add(menEditAddRequest);
        menEdit.add(menEditAddDefine);
        menEdit.add(menEditAddWait);

        // HelpMenu
        JMenu menHelp = new JMenu(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.help", "Help"));

        menHelpWiki = new JMenuItem(JDL.L("plugins.optional.httpliveheaderscripter.gui.menu.help.wiki", "Howto"));
        menHelpWiki.addActionListener(this);

        menHelp.add(menHelpWiki);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menFile);
        menuBar.add(menEdit);
        menuBar.add(menHelp);
        return menuBar;
    }

    public void onExit() {
    }

    private void importFF() {
        String script = UserIO.getInstance().requestTextAreaDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.importff.title", "Import Firefox Live header Log"), JDL.L("plugins.optional.httpliveheaderscripter.gui.importff.message", "Insert your firefox Liveaheader Log here"), null);

        script = convertFF(script);

        textArea.setText(script);
    }

    private void importLHScript() {
        ArrayList<String[]> scripts = new HTTPLiveHeader().getLHScripts();

        Collections.sort(scripts, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
            }
        });

        HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
        for (int i = scripts.size() - 1; i >= 0; i--) {
            if (ch.containsKey(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2])) {
                scripts.remove(i);
            } else {

                ch.put(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2], true);
            }
        }
        ch.clear();
        final String[] d = new String[scripts.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = i + ". " + Encoding.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
        }

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        final DefaultListModel defaultListModel = new DefaultListModel();
        final String text = "Search Router Model";
        final JTextField searchField = new JTextField();
        searchField.setForeground(Color.lightGray);
        final JList list = new JList(defaultListModel);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
            }

            public void insertUpdate(DocumentEvent e) {
                refreshList();
            }

            private void refreshList() {
                String search = searchField.getText().toLowerCase();
                String[] hits = search.split(" ");
                defaultListModel.removeAllElements();
                for (int i = 0; i < d.length; i++) {
                    for (int j = 0; j < hits.length; j++) {
                        if (!d[i].toLowerCase().contains(hits[j])) {
                            break;
                        }
                        if (j == hits.length - 1) {
                            defaultListModel.addElement(d[i]);
                        }
                    }
                }
                list.setModel(defaultListModel);
            }

            public void removeUpdate(DocumentEvent e) {
                refreshList();
            }
        });
        searchField.addFocusListener(new FocusAdapter() {
            boolean onInit = true;

            public void focusGained(FocusEvent e) {
                if (onInit) {
                    onInit = !onInit;
                    return;
                }
                searchField.setForeground(Color.black);
                if (searchField.getText().equals(text)) {
                    searchField.setText("");
                }
            }
        });

        JButton reset = new JButton(JDTheme.II("gui.images.exit", 16, 16));
        reset.setBorder(null);
        reset.setOpaque(false);
        reset.setContentAreaFilled(false);
        reset.setBorderPainted(false);
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchField.setForeground(Color.lightGray);
                searchField.setText(text);
                for (String element : d) {
                    defaultListModel.addElement(element);
                }
            }
        });
        searchField.setText(text);
        // !!! Lupen-Icon
        Icon icon = JDTheme.II("gui.images.update_manager", 16, 16);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(searchField, BorderLayout.CENTER);
        p.add(reset, BorderLayout.EAST);
        JLabel example = new JLabel("Example: 3Com ADSL");
        example.setForeground(Color.gray);
        p.add(example, BorderLayout.SOUTH);
        for (String element : d) {
            defaultListModel.addElement(element);
        }
        // list.setPreferredSize(new Dimension(400, 500));
        JScrollPane scrollPane = new JScrollPane(list);
        panel.add(p, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(400, 500));
        int n = 10;
        panel.setBorder(new EmptyBorder(n, n, n, n));
        JOptionPane op = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
        // JDialog dialog = new
        // JDialog(SwingUtilities.getWindowAncestor(btnSelectRouter), );
        JDialog dialog = op.createDialog(tabbedPanel, JDL.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"));
        dialog.add(op);
        dialog.setModal(true);
        dialog.setPreferredSize(new Dimension(400, 500));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        int answer = ((Integer) op.getValue()).intValue();
        if (answer != JOptionPane.CANCEL_OPTION && list.getSelectedValue() != null) {
            String selected = (String) list.getSelectedValue();
            int id = Integer.parseInt(selected.split("\\.")[0]);
            String[] data = scripts.get(id);
            if (data[2].toLowerCase().indexOf("curl") >= 0) {
                UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
            }
            textArea.setText(data[2]);
        }
    }

    private boolean validate() {
        String script = textArea.getText();

        if (script == null) {
            UserIO.getInstance().requestMessageDialog(JDL.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid"));
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