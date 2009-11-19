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
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import jd.PluginWrapper;
import jd.event.ControlListener;
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

@OptionalPlugin(rev = "$Revision$", id = "RouterEditor", hasGui = true, interfaceversion = 5)
public class JDRouterEditor extends PluginOptional implements ControlListener {

    private static final String JDL_PREFIX = "jd.plugins.optional.JDRouterEditor.";
    private MenuAction m = null;
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
    private JScrollPane routertableScrollPane;
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
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean initAddon() {
        if (m == null) {
            m = new MenuAction(getWrapper().getID(), 0);
            m.setActionListener(this);
            m.setTitle(getHost());
            m.setIcon(this.getIconKey());
            m.setSelected(false);
        }
        setGuiEnable(true);
        return true;
    }

    @Override
    public void onExit() {

    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(m);
        if (view == null || !view.isVisible()) {
            m.setSelected(false);
        } else {
            m.setSelected(true);
        }
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
        getfiles();
        fileselector = new JComboBox(files);
        newbutton = new JButton(JDL.L(JDL_PREFIX + "newbutton", "+"));
        newbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String filename = UserIO.getInstance().requestInputDialog(JDL.L(JDL_PREFIX + "newfileinput", "Enter Filename:"));
                if (!filename.endsWith(".xml")) filename = filename.concat(".xml").toString();
                currentfile = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/router/" + filename;
                router.cleanRouter();
                savetoFile();
                getfiles();
                loadfile(currentfile);
                setGuiRouterData(null);
                currentrouter = null;
                fileselector.revalidate();
                fileselector.setSelectedItem(filename);
                delrouterbutton.setEnabled(true);
                newrouterbutton.setEnabled(true);
            }
        });
        selbutton = new JButton(JDL.L(JDL_PREFIX + "selbutton", "Select"));
        selbutton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                currentfile = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/router/" + fileselector.getSelectedItem().toString();
                loadfile(currentfile);
                delrouterbutton.setEnabled(true);
                newrouterbutton.setEnabled(true);
            }
        });

        JPanel filepanel = new JPanel(new MigLayout("ins 5, wrap 2", "[fill, grow][fill, grow]"));

        filepanel.add(new JLabel(JDL.L(JDL_PREFIX + "dbselect", "DB-File:")), "Split 2,");
        filepanel.add(fileselector, "width 400:400:400");
        filepanel.add(selbutton, "Split 3");
        filepanel.add(newbutton, "width ::40");

        // EditPanel
        newrouterbutton = new JButton(JDL.L(JDL_PREFIX + "routeredit.routernew", "New"));
        newrouterbutton.setEnabled(false);
        newrouterbutton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                currentrouter = null;
                setGuiRouterData(null);
                saverouterbutton.setEnabled(true);
            }

        });
        delrouterbutton = new JButton(JDL.L(JDL_PREFIX + "routeredit.routerdel", "Delete"));
        delrouterbutton.setEnabled(false);
        delrouterbutton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (currentrouter != null) {
                    router.delRouter(currentrouter);
                    currentrouter = null;
                    updateTable();
                    setGuiRouterData(null);
                    savetoFile();
                }
            }

        });
        saverouterbutton = new JButton(JDL.L(JDL_PREFIX + "routeredit.routersave", "Save"));
        saverouterbutton.setEnabled(false);
        saverouterbutton.addActionListener(new ActionListener() {

            @Override
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
                savetoFile();
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
        JPanel EditPanel = new JPanel(new MigLayout("ins 1, wrap 2", "[fill, grow][fill, grow]"));
        EditPanel.add(newrouterbutton, "span 2, Split 3");
        EditPanel.add(saverouterbutton);
        EditPanel.add(delrouterbutton);
        EditPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routermanufactor", "Router Manufactor:")));
        EditPanel.add(routerhersteller);
        EditPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routername", "Router Name:")));
        EditPanel.add(routername);
        EditPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routerregex", "Router Regex:")));
        EditPanel.add(routerregex);
        EditPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routeruser", "Router Username:")));
        EditPanel.add(routeruser);
        EditPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routerpass", "Router Password:")));
        EditPanel.add(routerpass);
        EditPanel.add(new JLabel(JDL.L(JDL_PREFIX + "routeredit.routerscript", "Router Script:")));
        EditPanel.add(routerscriptScrollPane);

        // Main Gui(Router List + Editpanel)
        routertable = new RouterTable(router);
        routertable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                RouterTable routertable = (RouterTable) e.getSource();
                Point p = e.getPoint();
                int row = routertable.rowAtPoint(p);
                currentrouter = router.getRouterdata(row);
                setGuiRouterData(currentrouter);
                saverouterbutton.setEnabled(true);
            }
        });

        routertableScrollPane = new JScrollPane();
        routertableScrollPane.setViewportView(routertable);

        frame.setLayout(new MigLayout("ins 5, wrap 1", "[fill, grow][fill, grow]"));
        frame.add(filepanel);
        frame.add(routertableScrollPane, "grow,Split 2, width ::200");
        frame.add(EditPanel, "grow");
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
                view = new JDRouterEditorView() {
                    private static final long serialVersionUID = 8256663083855006721L;

                };
                view.getBroadcaster().addListener(new SwitchPanelListener() {

                    @Override
                    public void onPanelEvent(SwitchPanelEvent event) {
                        if (event.getID() == SwitchPanelEvent.ON_REMOVE) {
                            onExit();
                        }
                    }

                });
                view.setContent(frame);
            }
            SwingGui.getInstance().setContent(view);
        } else {
            if (view != null) view.close();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == m) {
            setGuiEnable(true);
        } else {
            setGuiEnable(false);
        }
    }

    public void updateTable() {
        routertable.getModel().refreshModel();
        routertable.getModel().fireTableDataChanged();
    }

    private void getfiles() {
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

    private void loadfile(String filepath) {
        File file = new File(filepath);
        Vector<?> temp = (Vector<?>) JDIO.loadObject(null, file, true);
        if (temp != null) router.loadrouter(temp);
        updateTable();
    }

    protected void savetoFile() {
        File file = new File(currentfile);
        JDIO.saveObject(null, router.prepareToSave(), file, null, null, true);

    }

}
