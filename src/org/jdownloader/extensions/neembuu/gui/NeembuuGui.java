package org.jdownloader.extensions.neembuu.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.DownloadSession;
import org.jdownloader.extensions.neembuu.NeembuuExtension;
import org.jdownloader.extensions.neembuu.translate._NT;

public class NeembuuGui extends AddonPanel<NeembuuExtension> {

    private static final String ID         = "NEEMBUUGUI";
    private SwitchPanel         panel;
    private final JTabbedPane   tabbedPane = new JTabbedPane();

    public NeembuuGui(NeembuuExtension plg) {
        super(plg);
        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]")) {

            @Override
            protected void onShow() {

            }

            @Override
            protected void onHide() {
            }
        };
        // layout all contents in panel
        this.setContent(panel);

        layoutPanel();
    }

    public void addSession(DownloadSession jdds) {
        synchronized (tabbedPane) {
            if (tabbedPane.indexOfComponent(jdds.getWatchAsYouDownloadSession().getFilePanel()) == -1) {
                tabbedPane.add(jdds.getDownloadLink().getFilePackage().getName(), jdds.getWatchAsYouDownloadSession().getFilePanel());
                tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(jdds.getWatchAsYouDownloadSession().getFilePanel()), new ButtonTabComponent(tabbedPane, jdds, this));
            }
        }
    }

    public void removeSession(JComponent jc) {
        synchronized (tabbedPane) {
            tabbedPane.removeTabAt(tabbedPane.indexOfComponent(jc));
        }
    }

    private void layoutPanel() {
        panel.add(tabbedPane);
        JPanel settingsPanel = new JPanel(new MigLayout());
        tabbedPane.addTab(_NT._.settingsPanelTitle(), settingsPanel);

        final JLabel jl = new JLabel(_NT._.vfsUnchecked());
        settingsPanel.add(jl, "wrap");
        JButton testJpfm = new JButton(_NT._.checkVFSButton());
        settingsPanel.add(testJpfm, "wrap");
        JLabel basicMntLoc = new JLabel(_NT._.basicMountLocation());
        settingsPanel.add(basicMntLoc, "split 2");
        final JTextField bml = new JTextField();
        bml.setEditable(false);
        bml.setText(getExtension().getBasicMountLocation());
        settingsPanel.add(bml, "span 2");
        JButton bml_b = new JButton(_NT._.browse());
        settingsPanel.add(bml_b, "wrap");
        JButton openBml = new JButton(_NT._.openBasicMountLocation());
        settingsPanel.add(openBml, "wrap");
        JButton pismowebsite = new JButton("<html><U><FONT COLOR=\"0000ff\">" + _NT._.poweredByPismo() + "</FONT></U></html>", new ImageIcon(NeembuuGui.class.getResource("pfm.png")));
        pismowebsite.setBorderPainted(false);
        settingsPanel.add(pismowebsite, "span");
        JLabel vlc = new JLabel(_NT._.worksBestWithVlc(), new ImageIcon(NeembuuGui.class.getResource("vlc.png")), JLabel.LEFT);
        settingsPanel.add(vlc, "span");
        JLabel vlc_option = new JLabel(_NT._.vlcPathOption());
        settingsPanel.add(vlc_option, "span");
        final JTextField vlcPath = new JTextField();

        vlcPath.setEditable(false);
        JButton browseVlc = new JButton(_NT._.browse());
        settingsPanel.add(vlcPath, "width 200::");
        settingsPanel.add(browseVlc, "wrap");
        testJpfm.addActionListener(new ActionListener() {

            // @Override
            public void actionPerformed(ActionEvent e) {
                boolean usable = getExtension().isUsable();
                if (usable) {
                    jl.setText(_NT._.vfsWorking());
                } else {
                    jl.setText(_NT._.vfsNotWorking());
                }
            }
        });

        bml_b.addActionListener(new ActionListener() {
            // @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int ret = jfc.showOpenDialog(panel);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    try {
                        getExtension().setBasicMountLocation(jfc.getSelectedFile().getAbsolutePath());
                        bml.setText(getExtension().getBasicMountLocation());
                    } catch (Exception a) {
                        JOptionPane.showMessageDialog(panel, jfc.getSelectedFile() + "\nCannot be used. Reason :" + a.getMessage(), "Cannot set basic mount location", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        browseVlc.addActionListener(new ActionListener() {

            // @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                jfc.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        String n = f.getName();
                        if (f.isFile()) {
                            if (n.equalsIgnoreCase("vlc.exe") || n.equalsIgnoreCase("vlc") || n.equalsIgnoreCase("vlc.app")) return true;
                        } else
                            return true;// else
                                        // if(n.equalsIgnoreCase("vlc.app"))return
                                        // true;//mac
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "VLC (vlc.exe or vlc)";
                    }
                });
                int ret = jfc.showOpenDialog(panel);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    try {
                        String vlcLoc = jfc.getSelectedFile().getAbsolutePath();
                        if (vlcLoc.toLowerCase().endsWith(".app")) {
                            if (!vlcLoc.endsWith("/")) vlcLoc += '/';
                            vlcLoc += "Contents/MacOS/VLC";
                        }
                        getExtension().setVlcLocation(vlcLoc);
                        vlcPath.setText(getExtension().getVlcLocation());
                    } catch (Exception a) {
                        JOptionPane.showMessageDialog(panel, jfc.getSelectedFile() + "\nCannot be used. Reason :" + a.getMessage(), "Cannot set basic mount location", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    getExtension().setVlcLocation(null);
                    vlcPath.setText(getExtension().getVlcLocation());
                }
            }
        });
        openBml.addActionListener(new ActionListener() {
            // @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(getExtension().getBasicMountLocation()));
                } catch (Exception a) {
                    // ignore
                }
            }
        });
        pismowebsite.addActionListener(new ActionListener() {

            // @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://www.pismotechnic.com/pfm/"));
                } catch (Exception a) {
                    // ignore
                }
            }
        });
    }

    /**
     * is called if, and only if! the view has been closed
     */
    @Override
    protected void onDeactivated() {
        Log.L.finer("onDeactivated " + getClass().getSimpleName());
    }

    /**
     * is called, if the gui has been opened.
     */
    @Override
    protected void onActivated() {
        Log.L.finer("onActivated " + getClass().getSimpleName());
    }

    @Override
    public Icon getIcon() {
        return this.getExtension().getIcon(16);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getTitle() {
        return _NT._.gui_title();
    }

    @Override
    public String getTooltip() {
        return _NT._.gui_tooltip();
    }

    /**
     * Is called if gui is visible now, and has not been visible before. For
     * example, user starte the extension, opened the view, or switched form a
     * different tab to this one
     */
    @Override
    protected void onShow() {
        Log.L.finer("Shown " + getClass().getSimpleName());
    }

    /**
     * gets called of the extensiongui is not visible any more. for example
     * because it has been closed or user switched to a different tab/view
     */
    @Override
    protected void onHide() {
        Log.L.finer("hidden " + getClass().getSimpleName());
    }

}
