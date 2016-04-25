package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.swing.MigPanel;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;

public class YoutubeVariantSelectionDialog extends AbstractDialog<Object> {
    private YoutubeClipData        clip;
    private CustomVariantsMapTable table;
    private AbstractVariantWrapper current;

    @Override
    protected int getPreferredHeight() {
        return 600;
    }

    @Override
    protected int getPreferredWidth() {
        return 800;
    }

    protected AbstractVariantWrapper selectedVariant;
    private List<VariantInfo>        variants;
    private AbstractVariant          selected;
    private CrawledLink              link;

    public YoutubeVariantSelectionDialog(CrawledLink link, AbstractVariant selected, YoutubeClipData clipData, List<VariantInfo> variants) {
        super(Dialog.STYLE_HIDE_ICON, _GUI.T.youtube_variant_selection_dialog_title(clipData.title), null, _GUI.T.lit_choose(), null);
        boolean hasVideo = false;

        setDimensor(new RememberLastDialogDimension("YoutubeChooseVariantDialogDimension"));
        setLocator(new RememberRelativeDialogLocator("YoutubeChooseVariantDialogLocation8", JDGui.getInstance().getMainFrame()));
        this.clip = clipData;
        this.selected = selected;
        this.variants = variants;
        this.link = link;
    }

    public CustomVariantsMapTable getTable() {
        return table;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel ret = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");

        List<String> dupe = new ArrayList<String>();
        Collections.sort(variants, new Comparator<VariantInfo>() {

            @Override
            public int compare(VariantInfo o1, VariantInfo o2) {
                return CompareUtils.compare(o2.getVariant().getQualityRating(), o1.getVariant().getQualityRating());
            }
        });
        ArrayList<AbstractVariantWrapper> variantStorables = new ArrayList<AbstractVariantWrapper>();
        String selectID = new VariantIDStorable(selected).createUniqueID();
        if (variants != null) {
            for (VariantInfo vi : variants) {
                if (selected.getGroup() == vi.getVariant().getGroup()) {
                    VariantIDStorable stor = new VariantIDStorable(vi.getVariant());
                    if (dupe.add(stor.createUniqueID())) {
                        AbstractVariantWrapper avw;
                        variantStorables.add(avw = new AbstractVariantWrapper(vi.getVariant()));
                        if (current == null && StringUtils.equals(selectID, stor.createUniqueID())) {
                            current = avw;
                        }
                    }
                }
            }
        }

        table = new CustomVariantsMapTable(variantStorables);
        JScrollPane sp;
        ret.add(sp = new JScrollPane(table));

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                List<AbstractVariantWrapper> selectedObject = table.getModel().getSelectedObjects(1);
                if (selectedObject == null || selectedObject.size() == 0) {
                    selectedVariant = null;
                    okButton.setEnabled(false);
                } else {
                    selectedVariant = selectedObject.get(0);
                    okButton.setEnabled(true);
                }

            }
        });
        table.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (selectedVariant != null) {
                        okButton.doClick();
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        table.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (selectedVariant != null) {
                        okButton.doClick();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });
        getDialog().setMinimumSize(new Dimension(200, 200));
        if (current != null) {
            table.getModel().setSelectedObject(current);
            table.scrollToSelection(0);
        }
        return ret;
    }

    @Override
    protected void packed() {
        super.packed();

    }

    public LinkVariant getVariant() {
        if (selectedVariant == null) {
            return null;
        }
        return selectedVariant.variant;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

}
