package org.jdownloader.iconsetter.gui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.appwork.app.gui.BasicGui;
import org.appwork.shutdown.ShutdownController;
import org.appwork.swing.MigPanel;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.iconsetter.IconSetMaker;
import org.jdownloader.iconsetter.ResourceSet;
import org.jdownloader.images.NewTheme;

public class IconSetterGui extends BasicGui {

    private IconSetMaker owner;

    public IconSetterGui(IconSetMaker owner) {
        super("IconSet Creator");
        this.owner = owner;

    }

    @Override
    protected List<? extends Image> getAppIconList() {
        final java.util.List<Image> l = new ArrayList<Image>();
        l.add(NewTheme.I().getImage(IconKey.ICON_LOGO_JD_LOGO_128_128, -1));
        l.add(NewTheme.I().getImage(IconKey.ICON_LOGO_JD_LOGO_128_128, 16));
        l.add(NewTheme.I().getImage(IconKey.ICON_LOGO_JD_LOGO_128_128, 18));
        l.add(NewTheme.I().getImage(IconKey.ICON_LOGO_JD_LOGO_128_128, 32));
        return l;
    }

    @Override
    protected void layoutPanel() {
        MigPanel p = new MigPanel("ins 5", "[grow,fill]", "[grow,fill]");

        JProgressBar bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setString("Scanning Themes");
        bar.setIndeterminate(true);

        p.add(bar);

        getFrame().setContentPane(p);
        getFrame().revalidate();
        // getFrame().pack();

    }

    @Override
    protected void requestExit() {
        ShutdownController.getInstance().requestShutdown();
    }

    public void onThemesScanned() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                MigPanel p = new MigPanel("ins 5,wrap 2,", "[][grow,fill]", "[40!,fill][24!]");

                PseudoCombo<ResourceSet> combo = new PseudoCombo<ResourceSet>(owner.getResourceSets().toArray(new ResourceSet[] {})) {
                    @Override
                    protected String getLabel(ResourceSet v, boolean closed) {
                        return v.getName();
                    }

                    @Override
                    public void onChanged(ResourceSet newValue) {
                        owner.edit(newValue);
                    }

                    @Override
                    protected Icon getIcon(ResourceSet v, boolean closed) {

                        try {
                            File file = Application.getResource("themes/" + v.getName() + "/" + v.getIcons().get(0).getPath());
                            int smallest = v.getIcons().get(0).getPath().length();
                            for (int i = 0; i < v.getIcons().size(); i++) {
                                if (v.getIcons().get(i).getPath().length() < smallest) {
                                    smallest = v.getIcons().get(i).getPath().length();
                                    file = Application.getResource("themes/" + v.getName() + "/" + v.getIcons().get(i).getPath());
                                }
                            }

                            return new ImageIcon(IconIO.getScaledInstance(ImageIO.read(file), 32, 32));
                        } catch (Throwable e) {

                            return null;
                        }
                    }
                };
                p.add(new JLabel("Choose IconSet:"));
                p.add(combo);

                p.add(new ExtButton(new BasicAction() {
                    {
                        setName("Create New");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        InputDialog d = new InputDialog(0, "Choose Name", "Choose Name", null);
                        UIOManager.I().show(null, d);
                        d.getText();
                        File file = Application.getResource("themes/" + d.getText() + "/");
                        if (file.exists()) {
                            UIOManager.I().showErrorMessage(d.getText() + " already exists...");
                            return;
                        }

                        owner.edit(owner.createNewResourceSet(d.getText()));
                    }

                }), "spanx,pushx,growx");
                getFrame().setContentPane(p);
                getFrame().revalidate();
                // getFrame().pack();
            }
        };
    }

    public void onEditTheme(final ResourceSet set) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                MigPanel p = new MigPanel("ins 5,wrap 2,", "[][grow,fill]", "[40!,fill][24!][grow,fill]");

                PseudoCombo<ResourceSet> combo = new PseudoCombo<ResourceSet>(owner.getResourceSets().toArray(new ResourceSet[] {})) {
                    @Override
                    protected String getLabel(ResourceSet v, boolean closed) {
                        return v.getName();
                    }

                    @Override
                    public void onChanged(ResourceSet newValue) {
                        if (newValue != set) {
                            owner.edit(newValue);
                        }
                    }

                    @Override
                    protected Icon getIcon(ResourceSet v, boolean closed) {
                        try {
                            File file = Application.getResource("themes/" + v.getName() + "/" + v.getIcons().get(0).getPath());
                            int smallest = v.getIcons().get(0).getPath().length();
                            for (int i = 0; i < v.getIcons().size(); i++) {
                                if (v.getIcons().get(i).getPath().length() < smallest) {
                                    smallest = v.getIcons().get(i).getPath().length();
                                    file = Application.getResource("themes/" + v.getName() + "/" + v.getIcons().get(i).getPath());
                                }
                            }

                            return new ImageIcon(IconIO.getScaledInstance(ImageIO.read(file), 32, 32));
                        } catch (Throwable e) {

                            return null;
                        }
                    }
                };
                combo.setSelectedItem(set);
                p.add(new JLabel("Choose IconSet:"));
                p.add(combo);

                p.add(new ExtButton(new BasicAction() {
                    {
                        setName("Create New");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        InputDialog d = new InputDialog(0, "Choose Name", "Choose Name", null);
                        UIOManager.I().show(null, d);
                        d.getText();
                        File file = Application.getResource("themes/" + d.getText() + "/");
                        if (file.exists()) {
                            UIOManager.I().showErrorMessage(d.getText() + " already exists...");
                            return;
                        }

                        owner.edit(owner.createNewResourceSet(d.getText()));
                    }

                }), "spanx,pushx,growx");

                SetTable table = new SetTable(owner, new SetTableModel(owner.getStandardSet(), set));
                JScrollPane sp;
                p.add(sp = new JScrollPane(table), "spanx,pushx,growx");
                // sp.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getPreferredSize().height));
                getFrame().setContentPane(p);
                getFrame().revalidate();
                // getFrame().pack();
            }
        };
    }

}
