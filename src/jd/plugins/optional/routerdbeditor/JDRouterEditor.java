//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.routerdbeditor;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.io.JDIO;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

@OptionalPlugin(rev = "$Revision$", id = "routereditor", hasGui = true, interfaceversion = 5)
public class JDRouterEditor extends PluginOptional {

    private static final String JDL_PREFIX = "jd.plugins.optional.JDRouterEditor.";
    private Boolean readonly = false;
    private Vector<String> shipped = null;
    private MenuAction activateAction = null;
    private JDRouterEditorView view;
    private SwitchPanel frame;
    private JComboBox fileselector;
    private Vector<String> files = new Vector<String>();
    private JButton newbutton;
    private JButton selbutton;
    private JButton newrouterbutton;
    private JButton delrouterbutton;
    private JButton saverouterbutton;
    private RouterTable routertable;
    private RouterList router = new RouterList();
    private JTextField routerhersteller;
    private JTextField routername;
    private JTextField routerpass;
    private JTextField routeruser;
    private JTextField routerregex;
    private JTextArea routerscript;
    private JScrollPane routerscriptScrollPane;
    private Router currentrouter;
    private String currentfile;

    public JDRouterEditor(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        if (activateAction == null) {
            activateAction = new MenuAction("routereditor", 0);
            activateAction.setActionListener(this);
            activateAction.setTitle(getHost());
            activateAction.setIcon(this.getIconKey());
            activateAction.setSelected(false);
        }
        return true;
    }

    @Override
    public void onExit() {

    }

    @Override
    public String getIconKey() {
        return "gui.images.reconnect_settings";
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    private void initGUI() {
        frame = new SwitchPanel() {
            private static final long serialVersionUID = -2980165161222567714L;

            @Override
            public void onShow() {
            }

            @Override
            public void onHide() {
            }
        };

        // File Select/add/Del
        loadShippedFileList(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/router/lock");
        getFiles();
        fileselector = new JComboBox(files);
        newbutton = new JButton(JDL.L(JDL_PREFIX + "newbutton", "+"));
        newbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                String filename = UserIO.getInstance().requestInputDialog(JDL.L(JDL_PREFIX + "newfileinput", "Enter Filename:"));
                if (filename == null) return;
                if (!filename.endsWith(".xml")) filename = filename.concat(".xml").toString();
                currentfile = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/router/" + filename;
                if (new File(currentfile).exists()) return;
                router.cleanRouter();
                saveToFile();
                getFiles();
                loadFile(currentfile);
                setGuiRouterData(null);
                currentrouter = null;
                fileselector.revalidate();
                fileselector.setSelectedItem(filename);
                delrouterbutton.setEnabled(true);
                newrouterbutton.setEnabled(true);
                readonly = false;
            }

        });
        selbutton = new JButton(JDL.L(JDL_PREFIX + "selbutton", "Select"));
        selbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                currentfile = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/router/" + fileselector.getSelectedItem().toString();
                setGuiRouterData(null);
                loadFile(currentfile);
                if (shipped.contains(fileselector.getSelectedItem().toString()))
                    readonly = true;
                else
                    readonly = false;
                if (readonly) {
                    delrouterbutton.setEnabled(false);
                    newrouterbutton.setEnabled(false);
                } else {
                    delrouterbutton.setEnabled(true);
                    newrouterbutton.setEnabled(true);
                }

            }
        });

        JPanel filepanel = new JPanel(new MigLayout("ins 0", "[][grow,fill]10[]5[]"));
        filepanel.add(new JLabel(JDL.L(JDL_PREFIX + "dbselect", "DB-File:")));
        filepanel.add(fileselector, "growx");
        filepanel.add(selbutton, "sizegroup file");
        filepanel.add(newbutton, "sizegroup file");

        // EditPanel
        newrouterbutton = new JButton(JDL.L(JDL_PREFIX + "routeredit.routernew", "New"));
        newrouterbutton.setEnabled(false);
        newrouterbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                currentrouter = null;
                setGuiRouterData(null);
                saverouterbutton.setEnabled(true);
            }

        });
        delrouterbutton = new JButton(JDL.L(JDL_PREFIX + "routeredit.routerdel", "Delete"));
        delrouterbutton.setEnabled(false);
        delrouterbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (currentrouter != null) {
                    router.delRouter(currentrouter);
                    currentrouter = null;
                    updateTable();
                    setGuiRouterData(null);
                    saveToFile();
                }
            }

        });
        saverouterbutton = new JButton(JDL.L(JDL_PREFIX + "routeredit.routersave", "Save"));
        saverouterbutton.setEnabled(false);
        saverouterbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (currentrouter != null) {
                    router.delRouter(currentrouter);
                    currentrouter = new Router(routerhersteller.getText(), routername.getText(), routeruser.getText(), routerpass.getText(), routerregex.getText(), routerscript.getText());
                    router.addRouter(currentrouter);
                    updateTable();
                } else {
                    currentrouter = new Router(routerhersteller.getText(), routername.getText(), routeruser.getText(), routerpass.getText(), routerregex.getText(), routerscript.getText());
                    router.addRouter(currentrouter);
                    updateTable();
                }
                saveToFile();
            }

        });
        routerhersteller = new JTextField();
        routername = new JTextField();
        routername.setColumns(40);
        routerregex = new JTextField();
        routeruser = new JTextField();
        routerpass = new JTextField();
        routerscript = new JTextArea();
        routerscript.setRows(30);
        routerscript.setColumns(50);
        routerscriptScrollPane = new JScrollPane();
        routerscriptScrollPane.setViewportView(routerscript);
        JPanel editPanel = new JPanel(new MigLayout("ins 1, wrap 2", "[fill, grow][fill, grow]"));
        editPanel.add(newrouterbutton, "span 2, split 3, sizegroup edit");
        editPanel.add(saverouterbutton, "sizegroup edit");
        editPanel.add(delrouterbutton, "sizegroup edit");
        editPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routermanufacturer", "Router manufacturer:")));
        editPanel.add(routerhersteller);
        editPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routername", "Router Name:")));
        editPanel.add(routername);
        editPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routerregex", "Router Regex:")));
        editPanel.add(routerregex);
        editPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routeruser", "Router Username:")));
        editPanel.add(routeruser);
        editPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routerpass", "Router Password:")));
        editPanel.add(routerpass);
        editPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routerscript", "Router Script:")));
        editPanel.add(routerscriptScrollPane);

        // Main Gui(Router List + Editpanel)
        routertable = new RouterTable(router);
        routertable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                RouterTable routertable = (RouterTable) e.getSource();
                Point p = e.getPoint();
                int row = routertable.rowAtPoint(p);
                currentrouter = router.getRouterdata(row);
                setGuiRouterData(currentrouter);
                if (readonly) {
                    saverouterbutton.setEnabled(false);

                } else {
                    saverouterbutton.setEnabled(true);
                }

            }
        });

        frame.setLayout(new MigLayout("ins 5, wrap 2", "[fill, grow][fill, grow]"));
        frame.add(filepanel, "spanx");
        frame.add(new JSeparator(), "spanx");
        frame.add(new JScrollPane(routertable), "grow");
        frame.add(editPanel, "grow");
        frame.setVisible(true);
        updateTable();
    }

    private void setGuiRouterData(Router routerdata) {
        if (routerdata != null) {
            this.routerhersteller.setText(routerdata.getHersteller());
            this.routername.setText(routerdata.getName());
            this.routerscript.setText(routerdata.getScript());
            this.routerscript.setCaretPosition(0);
            this.routerregex.setText(routerdata.getRegex());
            this.routeruser.setText(routerdata.getUsername());
            this.routerpass.setText(routerdata.getPass());
        } else {
            this.routerhersteller.setText("");
            this.routername.setText("");
            this.routerscript.setText("");
            this.routerscript.setCaretPosition(0);
            this.routerregex.setText("");
            this.routeruser.setText("");
            this.routerpass.setText("");
        }
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            if (view == null) {
                initGUI();
                view = new JDRouterEditorView();
                view.getBroadcaster().addListener(new SwitchPanelListener() {

                    @Override
                    public void onPanelEvent(SwitchPanelEvent event) {
                        if (event.getID() == SwitchPanelEvent.ON_REMOVE) {
                            activateAction.setSelected(false);
                        }
                    }

                });
                view.setContent(frame);
            }
            SwingGui.getInstance().setContent(view);
        } else {
            if (view != null) view.close();
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    public void updateTable() {
        routertable.getModel().refreshModel();
        routertable.getModel().fireTableDataChanged();
    }

    private void getFiles() {
        String routerpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/router/";
        File routerfolder = new File(routerpath);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        File[] list = routerfolder.listFiles(filter);
        for (File temp : list) {
            if (temp.isFile()) files.add(temp.getName());
        }
    }

    private void loadFile(String filepath) {
        File file = new File(filepath);
        Vector<?> temp = (Vector<?>) JDIO.loadObject(null, file, true);
        if (temp != null) router.loadRouter(temp);
        updateTable();
    }

    private void loadShippedFileList(String filepath) {
        File file = new File(filepath);
        shipped = new Vector<String>();
        if (file.exists()) {
            try {
                FileInputStream fs = new FileInputStream(file);
                DataInputStream in = new DataInputStream(fs);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String nextline;
                while ((nextline = br.readLine()) != null) {
                    shipped.add(nextline);
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    protected void saveToFile() {
        File file = new File(currentfile);
        JDIO.saveObject(null, router.prepareToSave(), file, null, null, true);

    }

}
