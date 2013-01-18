package jd.gui.swing.jdgui.views.settings.panels.extensionmanager;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.RightPanel;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules.AntiStandByModule;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules.ExtensionModule;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules.GenericExtensionModule;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules.ShutdownModule;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules.WebinterfaceModule;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.UpdateController;

public class ExtensionManager extends AbstractConfigPanel {

    private HashMap<String, ExtensionModule> modules;

    public ExtensionManager() {
        super();
        this.addHeader(getTitle(), NewTheme.I().getIcon("extensionmanager", 32));
        this.addDescription(_GUI._.ExtensionManager_ExtensionManager_description_());

        add(new JScrollPane(getPanel()));

    }

    private Component getPanel() {
        final MigPanel panel = new RightPanel() {
            private Dimension dim;
            {
                dim = new Dimension(getWidth(), 200000);
            }

            public Dimension getPreferredScrollableViewportSize() {
                return dim;
            }
        };
        panel.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]"));
        modules = new HashMap<String, ExtensionModule>();
        if (UpdateController.getInstance().isHandlerSet()) {
            panel.add(getExperimentalDivider(_GUI._.ExtensionManager_getPanel_extensions_header_()));
            addModule(panel, new WebinterfaceModule());
            addModule(panel, new AntiStandByModule());
            addModule(panel, new ShutdownModule());
        }
        new Thread("App Manager Panel Builder") {
            public void run() {
                try {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {

                            add3rdParty(panel);
                        }

                    };
                    if (UpdateController.getInstance().isHandlerSet()) {
                        String[] lst = UpdateController.getInstance().listExtensionIds();
                        if (lst == null || lst.length == 0) return;
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                panel.add(getExperimentalDivider(_GUI._.ExtensionManager_getPanel_experimental_header_()));
                            }
                        };

                        for (final String s : lst) {
                            if (!modules.containsKey(s)) {
                                new EDTRunner() {

                                    @Override
                                    protected void runInEDT() {
                                        addModule(panel, new GenericExtensionModule(s));
                                    }
                                };
                            }

                        }
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            revalidate();
                        }
                    };
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return panel;
    }

    private void add3rdParty(MigPanel panel) {
        panel.add(getExperimentalDivider(_GUI._.ExtensionManager_getPanel3rdparty_header_()));

        panel.add(new Empty3rdPartyModule());
    }

    private Component getExperimentalDivider(String string) {
        MigPanel ret = new MigPanel("ins 15 0 15 0", "[grow,fill]5[][][]5[grow,fill]", "[]");
        ret.setOpaque(false);
        ret.add(Box.createGlue());
        ret.add(new JLabel(NewTheme.I().getIcon("download", 16)));
        ret.add(new JLabel("<html><u><b>" + string + "</b></u></html>"));
        ret.add(new JLabel(NewTheme.I().getIcon("download", 16)));
        ret.add(Box.createGlue());
        return ret;
    }

    public void addModule(MigPanel panel, ExtensionModule comp) {

        modules.put(comp.getID(), comp);
        panel.add(comp);
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("extensionmanager", 20);
    }

    @Override
    public String getTitle() {
        return _GUI._.ExtensionManager_getTitle_();
    }

    @Override
    protected void onShow() {
        super.onShow();
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

}
