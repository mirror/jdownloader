package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.FilersizeFilter;
import org.jdownloader.controlling.filter.FiletypeFilter;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.filter.RegexFilter;
import org.jdownloader.controlling.filter.RegexFilter.MatchType;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConditionDialog extends AbstractDialog<Object> {

    protected ExtTextField txtName;

    public String getName() {
        return txtName.getText();
    }

    public RegexFilter getFilenameFilter() {
        return new RegexFilter(cbFilename.isSelected(), MatchType.values()[cobFilename.getSelectedIndex()], txtFilename.getText());
    }

    public FilersizeFilter getFilersizeFilter() {
        return new FilersizeFilter(fromSize.getBytes(), toSize.getBytes(), cbSize.isSelected());
    }

    public FiletypeFilter getFiletypeFilter() {
        return new FiletypeFilter(cbType.isSelected(), cbAudio.isSelected(), cbVideo.isSelected(), cbArchive.isSelected(), cbImage.isSelected(), cbCustom.isSelected() ? txtCustumMime.getText().trim().split(",") : null);
    }

    public RegexFilter getSourceFilter() {
        return new RegexFilter(cbSource.isSelected(), MatchType.values()[cobSource.getSelectedIndex()], txtSource.getText());
    }

    public RegexFilter getHosterFilter() {
        return new RegexFilter(cbHoster.isSelected(), MatchType.values()[cobHoster.getSelectedIndex()], txtHoster.getText());
    }

    protected ExtCheckBox       cbFilename;

    protected JComboBox<String> cobFilename;
    protected ExtTextField      txtFilename;
    private JComponent          filename;

    private JComponent          size;
    protected ExtCheckBox       cbSize;

    protected SizeSpinner       fromSize;
    protected SizeSpinner       toSize;
    private SpinnerNumberModel  minSizeModel;
    private SpinnerNumberModel  maxSizeModel;
    private FilterPanel         type;

    protected ExtCheckBox       cbType;
    protected ExtCheckBox       cbAudio;
    protected ExtCheckBox       cbVideo;
    protected ExtCheckBox       cbArchive;
    protected ExtCheckBox       cbImage;
    protected ExtTextField      txtCustumMime;
    protected ExtCheckBox       cbCustom;
    private FilterPanel         hoster;
    protected ExtCheckBox       cbHoster;
    protected ExtTextField      txtHoster;
    protected JComboBox<String> cobHoster;
    private FilterPanel         source;
    protected ExtCheckBox       cbSource;
    protected JComboBox<String> cobSource;
    protected ExtTextField      txtSource;

    public ConditionDialog() {
        super(0, _GUI._.FilterRuleDialog_FilterRuleDialog_(""), null, _GUI._.literally_save(), null);

    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    public static void main(String[] args) {
        try {
            LookAndFeelController.getInstance().setUIManager();
            Dialog.getInstance().showDialog(new FilterRuleDialog(new FilterRule()));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        panel = new MigPanel("ins 5,wrap 3", "[][]15[grow,fill][]", "[]");
        panel.add(createHeader(_GUI._.FilterRuleDialog_layoutDialogContent_name()), "spanx,growx,pushx");
        txtName = new ExtTextField() {

            /**
             * 
             */
            private static final long serialVersionUID = 9217479913947520012L;

            @Override
            protected void onChanged() {
                getDialog().setTitle(_GUI._.FilterRuleDialog_FilterRuleDialog_(txtName.getText()));
            }

        };
        txtName.setHelpText(_GUI._.FilterRuleDialog_layoutDialogContent_ht_name());

        panel.add(txtName, "spanx,growx,pushx,gapleft 21");

        panel.add(createHeader(_GUI._.FilterRuleDialog_layoutDialogContent_if()), "gaptop 10,spanx,growx,pushx");
        filename = createFileNameFilter();

        JLabel lblFilename = getLabel(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_filename());
        cbFilename = new ExtCheckBox(filename);

        filename.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                cbFilename.setSelected(true);

            }

        });
        panel.add(cbFilename);
        panel.add(lblFilename);
        panel.add(filename);

        size = createSizeFilter();
        JLabel lblSize = getLabel(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_size());
        cbSize = new ExtCheckBox(size);

        size.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                cbSize.setSelected(true);

            }

        });
        panel.add(cbSize);
        panel.add(lblSize);
        panel.add(size);

        type = createTypeFilter();
        JLabel lblType = getLabel(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_type());
        cbType = new ExtCheckBox(type);
        type.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                cbType.setSelected(true);

            }

        });
        panel.add(cbType, "aligny top");
        panel.add(lblType, "aligny top,gaptop 3");
        panel.add(type);
        // hoster
        hoster = createHosterFilter();
        cbHoster = new ExtCheckBox(hoster);
        hoster.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                cbHoster.setSelected(true);

            }

        });
        panel.add(cbHoster);
        panel.add(new JLabel(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_hoster()));
        panel.add(hoster);
        // crawler
        source = createSourceFilter();
        cbSource = new ExtCheckBox(source);
        source.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                cbSource.setSelected(true);

            }

        });
        panel.add(cbSource);
        panel.add(new JLabel(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_source()));
        panel.add(source);

        return panel;
    }

    protected MigPanel createHeader(String string) {
        MigPanel ret = new MigPanel("ins 0", "[21,fill][][grow,fill]", "[]");
        ret.add(new JSeparator());
        ret.add(SwingUtils.toBold(new JLabel(string)));
        ret.add(new JSeparator());
        return ret;
    }

    private FilterPanel createSourceFilter() {
        final FilterPanel ret = new FilterPanel("[][grow,fill]", "[]");
        cobSource = new JComboBox<String>(new String[] { _GUI._.FilterRuleDialog_layoutDialogContent_equals(), _GUI._.FilterRuleDialog_layoutDialogContent_contains() });
        txtSource = new ExtTextField();
        txtSource.setHelpText(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_source_help());
        ret.add(cobSource);
        ret.add(txtSource);

        return ret;
    }

    private FilterPanel createHosterFilter() {
        final FilterPanel ret = new FilterPanel("[][grow,fill]", "[]");
        cobHoster = new JComboBox<String>(new String[] { _GUI._.FilterRuleDialog_layoutDialogContent_equals(), _GUI._.FilterRuleDialog_layoutDialogContent_contains() });
        txtHoster = new ExtTextField();
        txtHoster.setHelpText(_GUI._.FilterRuleDialog_layoutDialogContent_lbl_hoster_help());
        ret.add(cobHoster);
        ret.add(txtHoster);

        return ret;
    }

    private FilterPanel createTypeFilter() {
        final FilterPanel ret = new FilterPanel("ins 0,wrap 3", "[][]10[grow,fill]", "[]0[]0[]0[]0[]0[]") {
            public void setEnabled(boolean enabled) {
                if (!enabled) {
                    super.setEnabled(enabled);
                } else {
                    for (Component c : getComponents()) {
                        if (c instanceof ExtCheckBox) {
                            c.setEnabled(enabled);
                            ((ExtCheckBox) c).updateDependencies();
                        }
                        if (c instanceof ExtTextField) {
                            c.setEnabled(enabled);
                        }
                    }
                }

            }
        };
        JLabel lbl, ico;
        lbl = new JLabel(_GUI._.FilterRuleDialog_createTypeFilter_mime_audio());
        ico = new JLabel(NewTheme.I().getIcon("audio", 18));
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!cbAudio.isSelected() && !cbVideo.isSelected() && !cbArchive.isSelected() && !cbImage.isSelected() && !cbCustom.isSelected()) {
                    cbType.setSelected(false);
                }
            }
        };
        cbAudio = new ExtCheckBox(lbl, ico);
        lbl.addMouseListener(new ClickDelegater(cbAudio));
        ico.addMouseListener(new ClickDelegater(cbAudio));
        ret.add(ico);
        ret.add(cbAudio);
        cbAudio.addActionListener(al);
        ret.add(lbl);
        // video
        lbl = new JLabel(_GUI._.FilterRuleDialog_createTypeFilter_mime_video());
        ico = new JLabel(NewTheme.I().getIcon("video", 18));
        cbVideo = new ExtCheckBox(lbl, ico);
        cbVideo.addActionListener(al);
        lbl.addMouseListener(new ClickDelegater(cbVideo));
        ico.addMouseListener(new ClickDelegater(cbVideo));
        ret.add(ico);
        ret.add(cbVideo);
        ret.add(lbl);
        // archives
        lbl = new JLabel(_GUI._.FilterRuleDialog_createTypeFilter_mime_archives());
        ico = new JLabel(NewTheme.I().getIcon("archive", 18));
        cbArchive = new ExtCheckBox(lbl, ico);
        cbArchive.addActionListener(al);
        lbl.addMouseListener(new ClickDelegater(cbArchive));
        ico.addMouseListener(new ClickDelegater(cbArchive));
        ret.add(ico);
        ret.add(cbArchive);
        ret.add(lbl);
        // images
        lbl = new JLabel(_GUI._.FilterRuleDialog_createTypeFilter_mime_images());
        ico = new JLabel(NewTheme.I().getIcon("image", 18));
        cbImage = new ExtCheckBox(lbl, ico);
        cbImage.addActionListener(al);

        lbl.addMouseListener(new ClickDelegater(cbImage));
        ico.addMouseListener(new ClickDelegater(cbImage));
        ret.add(ico);
        ret.add(cbImage);
        ret.add(lbl);
        // various

        ico = new JLabel(NewTheme.I().getIcon("help", 18));
        txtCustumMime = new ExtTextField();
        txtCustumMime.setHelpText(_GUI._.FilterRuleDialog_createTypeFilter_mime_custom_help());
        txtCustumMime.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
            }

            public void focusGained(FocusEvent e) {
                cbCustom.setSelected(true);
                cbCustom.updateDependencies();
            }
        });
        cbCustom = new ExtCheckBox(ico, txtCustumMime);
        cbCustom.addActionListener(al);
        ico.addMouseListener(new ClickDelegater(cbCustom));
        ret.add(ico);
        ret.add(cbCustom);
        ret.add(txtCustumMime);
        return ret;
    }

    private FilterPanel createSizeFilter() {
        final JLabel to = new JLabel(NewTheme.I().getIcon("right", 14));

        minSizeModel = new SpinnerNumberModel(50000, 0l, Long.MAX_VALUE, 1) {

            @Override
            public Comparable getMaximum() {
                return (Comparable) maxSizeModel.getValue();
            }

            @Override
            public Comparable getMinimum() {
                return super.getMinimum();
            }
        };

        maxSizeModel = new SpinnerNumberModel(100 * 1024l, 0l, Long.MAX_VALUE, 1) {

            @Override
            public Comparable getMinimum() {
                return (Comparable) minSizeModel.getValue();
            }

        };
        fromSize = new SizeSpinner(minSizeModel);

        toSize = new SizeSpinner(maxSizeModel);

        toSize.setValue(10 * 1024 * 1024l * 1024l);
        final FilterPanel ret = new FilterPanel("[grow,fill][][grow,fill]", "[]");

        ret.add(fromSize, "sg 1");
        ret.add(to);
        ret.add(toSize, "sg 1");

        return ret;
    }

    private FilterPanel createFileNameFilter() {
        final FilterPanel ret = new FilterPanel("[][grow,fill]", "[]");
        cobFilename = new JComboBox<String>(new String[] { _GUI._.FilterRuleDialog_layoutDialogContent_equals(), _GUI._.FilterRuleDialog_layoutDialogContent_contains() });
        txtFilename = new ExtTextField();
        txtFilename.setHelpText(_GUI._.FilterRuleDialog_layoutDialogContent_ht_filename());

        ret.add(cobFilename);
        ret.add(txtFilename);

        return ret;
    }

    private JLabel getLabel(String filterRuleDialog_layoutDialogContent_lbl_name) {
        JLabel lbl = new JLabel(filterRuleDialog_layoutDialogContent_lbl_name);
        // lbl.setEnabled(false);
        return lbl;
    }

}
