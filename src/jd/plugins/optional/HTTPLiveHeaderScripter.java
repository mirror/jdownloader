//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.JDFileFilter;
import jd.config.MenuItem;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.parser.SimpleMatches;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HTTPLiveHeaderScripter extends PluginOptional {
    public static int getAddonInterfaceVersion() {
        return 0;
    }



    private JFrame frame;

    private JTextArea textArea;

    private JMenuItem menImportHTTPLive;

    private JMenuItem menImportJDLH;

    private JMenuItem menImportFile;

    private JMenuItem menSave;

    private JMenuItem menExit;

    private JMenuItem menEditValidate;

//    private JMenuItem menEditTest;

    private JMenuItem menEditAddRequest;

    private JMenuItem menEditAddDefine;

    private JMenuItem menEditAddWait;

    private JMenuItem menEditAddVariable;

    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getPluginID() {
        return getPluginName() + " " + getVersion();
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.httpliveheaderscripter.name", "HTTPLiveHeaderScripter");
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public boolean initAddon() {

        return true;
    }

    private void initGUI() {
        this.frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.title", "HTTP Live Header Script creater"));
        frame.setIconImage(JDTheme.I("gui.images.config.reconnect"));
        frame.setPreferredSize(new Dimension(600, 600));
        frame.setName("HTTPLIVEHEADERCREATER");
        LocationListener list = new LocationListener();
        frame.addComponentListener(list);
        frame.addWindowListener(list);
        frame.setLayout(new BorderLayout());
        initMenu();

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        // Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
textArea.setText("[[[HSRC]]]\r\n\r\n\r\n[[[/HSRC]]]");
        frame.setResizable(true);
        textArea.setEditable(true);
        textArea.requestFocusInWindow();

        frame.add(scrollPane, BorderLayout.CENTER);

        frame.pack();
        SimpleGUI.restoreWindow(null, null, frame);
        frame.setVisible(true);
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menFile = new JMenu(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.file", "File"));
        JMenu menEdit = new JMenu(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit", "Edit"));
        menuBar.add(menFile);
        menuBar.add(menEdit);

        // Filemenu
        this.menImportHTTPLive = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importhhtplive", "Import Firefox LiveHeader Script"));
        this.menImportJDLH = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importjdlh", "Import JD-LiveHeader Script"));
        this.menImportFile = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.file.importfile", "Open file"));
        this.menSave = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.file.save", "Save file"));
        this.menExit = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.file.exit", "Exit"));
        menImportHTTPLive.addActionListener(this);
        menImportJDLH.addActionListener(this);
        menImportFile.addActionListener(this);
        menSave.addActionListener(this);
        menExit.addActionListener(this);

        menFile.add(menImportFile);

        menFile.add(menSave);
        menFile.add(new JSeparator());
        menFile.add(menImportHTTPLive);
        menFile.add(menImportJDLH);
        menFile.add(new JSeparator());
        menFile.add(menExit);

        // EditMenu
        this.menEditValidate = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.validate", "Validate current script"));
        // this.menEditTest = new
        // JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.test",
        // "Test current script"));
        this.menEditAddRequest = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addrequest", "Add request tag"));
        this.menEditAddDefine = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.adddefine", "Add define tag"));
        this.menEditAddWait = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addwait", "Add wait tag"));
        this.menEditAddVariable = new JMenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.menu.edit.addvariable", "Add variable"));

        menEditValidate.addActionListener(this);
        //menEditTest.addActionListener(this);
        menEditAddRequest.addActionListener(this);
        menEditAddDefine.addActionListener(this);
        menEditAddWait.addActionListener(this);
        menEditAddVariable.addActionListener(this);

        menEdit.add(menEditValidate);

        // menEdit.add(menEditTest);
        menEdit.add(new JSeparator());
        menEdit.add(menEditAddRequest);
        menEdit.add(menEditAddDefine);

        menEdit.add(menEditAddWait);
       // menEdit.add(menEditAddVariable);

    }

    @SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.menImportHTTPLive) {
            this.importFF();
            return;
        }
        if (e.getSource() == this.menImportJDLH) {
            this.importLHScript();
            return;
        }
        if (e.getSource() == this.menImportFile) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.openfile", "Open"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new JDFileFilter(null, ".xml", true));
            fc.showSaveDialog(frame);
            File ret = fc.getSelectedFile();
            if (ret == null || !ret.exists()) return;
            try {
                Vector<String> save = (Vector<String>) JDUtilities.loadObject(frame, ret, true);
                this.textArea.setText(save.get(2));

            } catch (Exception e2) {
                this.textArea.setText(JDUtilities.getLocalFile(ret));
            }

            return;
        }
        if (e.getSource() == this.menSave) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.avefile", "Save"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new JDFileFilter(null, ".xml", true));
            fc.showSaveDialog(frame);
            File ret = fc.getSelectedFile();
            if (ret == null) return;
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("xml")) {

                ret = new File(ret.getAbsolutePath() + ".xml");
            }
            Vector<String> save = new Vector<String>();
            String manu = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.save.manufactur", "Manufactur? (e.g. Siemens)"));
            String model = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.save.model", "Model? (e.g. Gigaset 555 (fw 3.01.05)"));
            if (manu == null || model == null) return;
            save.add(manu);
            save.add(model);
            save.add(textArea.getText());
            save.add("?s).*" + manu.toLowerCase() + ".*");

            JDUtilities.saveObject(frame, save, ret, null, null, true);
            return;
        }
        if (e.getSource() == this.menExit) {
            frame.dispose();
            return;
        }
        if (e.getSource() == this.menEditValidate) {
            if (validate()) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.validate.ok", "Script is Valid."));

            }
            return;
        }

        if (e.getSource() == this.menEditAddRequest) {

            if (!validate()) return;
String script=this.textArea.getText();
int id=Math.max(script.lastIndexOf("[[[/STEP]]]")+11,script.lastIndexOf("[[[HSRC]]]")+9);
StringBuffer sb= new StringBuffer();
sb.append(script.substring(0,id).trim());
sb.append("\r\n");
sb.append("     [[[STEP]]]" + "\r\n");
sb.append("          [[[REQUEST]]]" + "\r\n\r\n");
sb.append("          [[[/REQUEST]]]" + "\r\n");
sb.append("     [[[/STEP]]]" + "\r\n");

sb.append(script.substring(id).trim());
textArea.setText(sb+"");
            return;
        }
        if (e.getSource() == this.menEditAddDefine) {
            if (!validate()) return;
            String script=this.textArea.getText();
            int id=Math.max(script.indexOf("[[[STEP]]]"),script.indexOf("[[[HSRC]]]")+9);
            StringBuffer sb= new StringBuffer();
            sb.append(script.substring(0,id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[DEFINE variablename=\"VALUE\"/]]]" + "\r\n");
            sb.append("     [[[/STEP]]]" + "\r\n");

            sb.append(script.substring(id).trim());
            textArea.setText(sb+"");
            return;
        }
        if (e.getSource() == this.menEditAddWait) {
            if (!validate()) return;
            String script=this.textArea.getText();
            int id=Math.max(script.lastIndexOf("[[[/STEP]]]")+11,script.lastIndexOf("[[[HSRC]]]")+9);
            StringBuffer sb= new StringBuffer();
            sb.append(script.substring(0,id).trim());
            sb.append("\r\n");
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[WAIT seconds=\"3\"/]]]" + "\r\n");
            sb.append("     [[[/STEP]]]" + "\r\n");

            sb.append(script.substring(id).trim());
            textArea.setText(sb+"");
            return;
        }
  

        if (frame == null || !frame.isVisible()) {
            initGUI();
        } else {
            frame.dispose();
        }
    }

    public static Document parseXmlString(String xmlString, boolean validating) throws SAXException, IOException, ParserConfigurationException {

        // Create a builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);

        InputSource inSource = new InputSource(new StringReader(xmlString));

        // Create the builder and parse the file
        Document doc = factory.newDocumentBuilder().parse(inSource);

        return doc;

    }

    private boolean validate() {
        String script = this.textArea.getText();

        if (script == null) {
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid"));
            return false;
        }

        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST>", "<REQUEST><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE>", "<RESPONSE><![CDATA[");
        script = script.replaceAll("</RESPONSE>", "]]></RESPONSE>");
        Document xmlScript;

        try {
            xmlScript = parseXmlString(script, false);
            Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                String error = "Root Node must be [[[HSRC]]]*[/HSRC]";
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid:") + error);
                return false;
            }
//            RequestInfo requestInfo = null;
            NodeList steps = root.getChildNodes();

            for (int step = 0; step < steps.getLength(); step++) {

                Node current = steps.item(step);

                if (current.getNodeType() == 3) {

                    continue;
                }
                if (!current.getNodeName().equalsIgnoreCase("STEP")) {

                    String error = "Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName();
                    JDUtilities.getGUI().showMessageDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid:") + error);
                    return false;
                }
//                NodeList toDos = current.getChildNodes();

            }

        } catch (Exception e) {
            String error = e.getMessage();
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.validate.error", "Script not valid:") + error);
            return false;

        }

        return true;

    }

    private void importFF() {
        String script = JDUtilities.getGUI().showTextAreaDialog(JDLocale.L("plugins.optional.httpliveheaderscripter.gui.importff.title", "Import Firefox Live header Log"), JDLocale.L("plugins.optional.httpliveheaderscripter.gui.importff.message", "Insert your firefox Liveaheader Log here"), null);

        script = convertFF(script);

        this.textArea.setText(script);
    }

    private String convertFF(String script) {
        String[] forbidden = new String[] { "jpg", "gif", "ico", "png", "mid" };
        String[] forbiddenHeaders = new String[] { "accept", "user-agent", "If-Modified-Since", "Cache-Control" };

        ArrayList<String> ips = new ArrayList<String>();
//        ArrayList<String> vars = new ArrayList<String>();
        String[] requests = script.split("----------------------------------------------------------");
        StringBuffer sb = new StringBuffer();
         for (String request : requests) {

            String[] lines = SimpleMatches.getLines(request.trim());
            if (lines.length < 3) continue;
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
            if (f) continue;
            sb.append("     [[[STEP]]]" + "\r\n");
            sb.append("          [[[REQUEST]]]" + "\r\n");
            boolean post = lines[2].trim().toLowerCase().startsWith("post");
            lines: for (int i = 2; i < lines.length; i++) {
                lines[i] = lines[i].trim();
                if (lines[i].toLowerCase().startsWith("http/")) break;

                for (String forb : forbiddenHeaders)
                    if (lines[i].toLowerCase().startsWith(forb.toLowerCase())) continue lines;
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
        sb = new StringBuffer();
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

        StringBuffer ret = new StringBuffer();
        ret.append("[[[HSRC]]]" + "\r\n");
        ret.append(sb);
        ret.append(sc);

        ret.append("[[[/HSRC]]]");
        return ret + "";
    }

    private void importLHScript() {
        Vector<String[]> scripts = new HTTPLiveHeader().getLHScripts();

        Collections.sort(scripts, new Comparator<Object>() {
            public int compare(Object a, Object b) {
                String[] aa = (String[]) a;
                String[] bb = (String[]) b;

                if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) > 0) {
                    return 1;
                } else if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) < 0) {
                    return -1;
                } else {
                    return 0;
                }

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
            d[i] = i + ". " + JDUtilities.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
        }

        // String selected = (String) JOptionPane.showInputDialog(this,
        // JDLocale.L("gui.config.liveHeader.dialog.selectRouter", "Bitte
        // wähle deinen Router aus"),
        // JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router
        // importieren"), JOptionPane.INFORMATION_MESSAGE, null, d, null);
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

            public void removeUpdate(DocumentEvent e) {
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
        });
        searchField.addFocusListener(new FocusAdapter() {
            boolean onInit = true;

            public void focusGained(FocusEvent e) {
                if (onInit) {
                    onInit = !onInit;
                    return;
                }
                searchField.setForeground(Color.black);
                if (searchField.getText().equals(text)) searchField.setText("");
            }
        });

        // !!! Eclipse Clear Console Icon
        ImageIcon imageIcon = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.exit")));
        imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(16, -1, Image.SCALE_SMOOTH));
        JButton reset = new JButton(imageIcon);
        reset.setBorder(null);
        reset.setOpaque(false);
        reset.setContentAreaFilled(false);
        reset.setBorderPainted(false);
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchField.setForeground(Color.lightGray);
                searchField.setText(text);
                for (int i = 0; i < d.length; i++) {
                    defaultListModel.addElement(d[i]);
                }
            }
        });
        searchField.setText(text);
        // !!! Lupen-Icon
        Icon icon = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.update_manager")));
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(searchField, BorderLayout.CENTER);
        p.add(reset, BorderLayout.EAST);
        JLabel example = new JLabel("Example: 3Com ADSL");
        example.setForeground(Color.gray);
        p.add(example, BorderLayout.SOUTH);
        for (int i = 0; i < d.length; i++) {
            defaultListModel.addElement(d[i]);
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
        JDialog dialog = op.createDialog(frame, JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"));
        dialog.add(op);
        dialog.setModal(true);
        dialog.setPreferredSize(new Dimension(400, 500));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        int answer = ((Integer) op.getValue()).intValue(); // JOptionPane.showConfirmDialog(this,
        // panel,
        // JDLocale.L("gui.config.liveHeader.dialog.importRouter",
        // "Router
        // importieren"),
        // JOptionPane.OK_CANCEL_OPTION,
        // JOptionPane.INFORMATION_MESSAGE);
        if (answer != JOptionPane.CANCEL_OPTION && list.getSelectedValue() != null) {
            String selected = (String) list.getSelectedValue();
            int id = Integer.parseInt(selected.split("\\.")[0]);
            String[] data = scripts.get(id);
            if (data[2].toLowerCase().indexOf("curl") >= 0) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
            }

            this.textArea.setText(data[2]);
            // routerScript.setData(data[2]);
            // String username = (String) user.getText();
            // if (username == null || username.matches("[\\s]*"))
            // user.setData(data[4]);
            // String pw = (String) pass.getText();
            // if (pw == null || pw.matches("[\\s]*")) pass.setData(data[5]);

        }

    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        if (frame == null || !frame.isVisible()) {
            menu.add(new MenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.action.start", "Start Scripter"), 0).setActionListener(this));
        } else {
            menu.add(new MenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.action.end", "Exit Scripter"), 0).setActionListener(this));

        }
        return menu;
    }

    @Override
    public void onExit() {

    }

}
