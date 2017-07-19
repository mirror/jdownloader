package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterPanel;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test.TestWaitDialog;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.test.PackagizerSingleTestTableModel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.pathchooser.PathChooser;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.controlling.packagizer.PackagizerRuleWrapper;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public class PackagizerFilterRuleDialog extends ConditionDialog<PackagizerRule> {
    private class PriorityAction extends AbstractAction {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final Priority    priority;

        public PriorityAction(Priority priority) {
            this.priority = priority;
        }

        public void actionPerformed(ActionEvent e) {
            prio = priority;
        }

        public Priority getPriority() {
            return priority;
        }

        public String getTooltipText() {
            return priority.T();
        }
    }

    private class RadioButton extends JRadioButton {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public RadioButton(PriorityAction pa_1) {
            super(pa_1);
            setToolTipText(pa_1.getTooltipText());
        }
    }

    public static class RuleMatcher {
        private final HashSet<CrawledLink> matches = new HashSet<CrawledLink>();
        private final PackagizerRule       rule;

        public RuleMatcher(final PackagizerRule rule) {
            this.rule = rule;
        }

        /**
         * @return the matches
         */
        public boolean matches(final CrawledLink link) {
            synchronized (matches) {
                return matches.contains(link);
            }
        }

        public PackagizerRule getRule() {
            return rule;
        }

        public void setMatches(final CrawledLink link, final PackagizerRuleWrapper lgr) {
            synchronized (matches) {
                matches.add(link);
            }
        }
    }

    private static final HashMap<PackagizerRule, PackagizerFilterRuleDialog> ACTIVE_DIALOGS = new HashMap<PackagizerRule, PackagizerFilterRuleDialog>();

    public static void showDialog(final PackagizerRule rule, final Runnable doAfterShow) {
        new Thread("ShowRuleDialogThread:" + rule) {
            public void run() {
                PackagizerFilterRuleDialog d = null;
                synchronized (ACTIVE_DIALOGS) {
                    d = ACTIVE_DIALOGS.get(rule);
                    if (d == null || !d.isVisible()) {
                        d = new PackagizerFilterRuleDialog(rule);
                        ACTIVE_DIALOGS.put(rule, d);
                    }
                }
                if (d.isVisible()) {
                    WindowManager.getInstance().setZState(d.getDialog(), FrameState.TO_FRONT);
                } else {
                    try {
                        CloseReason closeReason = UIOManager.I().show(null, d).getCloseReason();
                        if (CloseReason.OK.equals(closeReason) && doAfterShow != null) {
                            doAfterShow.run();
                        }
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private ExtCheckBox    cbAdd;
    private JToggleButton  cbAlways;
    private ExtCheckBox    cbChunks;
    private ExtCheckBox    cbDest;
    private ExtCheckBox    cbEnable;
    private ExtCheckBox    cbExtract;
    private ExtCheckBox    cbComment;
    private ExtCheckBox    cbForce;
    private ExtCheckBox    cbMove;
    private ExtCheckBox    cbName;
    private ExtCheckBox    cbPackagename;
    private ExtCheckBox    cbPriority;
    private ExtCheckBox    cbRename;
    private ExtCheckBox    cbStart;
    private JComboBox      cobAutoAdd;
    private JComboBox      cobAutostart;
    private JComboBox      cobEnable;
    private JComboBox      cobExtract;
    private JComboBox      cobForce;
    private PathChooser    fpDest;
    private PathChooser    fpMove;
    private JLabel         lblautoadd;
    private JLabel         lblAutostart;
    private JLabel         lblChunks;
    private JLabel         lblDest;
    private JLabel         lblEnable;
    private JLabel         lblExtract;
    private JLabel         lblFilename;
    private JLabel         lblComment;
    private JLabel         lblForce;
    private JLabel         lblMove;
    private JLabel         lblPackagename;
    private JLabel         lblPriority;
    private JLabel         lblRename;
    private RuleMatcher    matcher = null;
    protected Priority     prio    = Priority.DEFAULT;
    private PackagizerRule rule;
    private ExtSpinner     spChunks;
    private ExtTextField   txtNewFilename;
    private ExtTextField   txtPackagename;
    private ExtTextField   txtComment;
    private ExtTextField   txtRename;
    private ButtonGroup    group;

    private PackagizerFilterRuleDialog(PackagizerRule filterRule) {
        super();
        this.rule = filterRule;
        setLocator(new RememberAbsoluteDialogLocator(getClass().getSimpleName()));
        setDimensor(new RememberLastDialogDimension(getClass().getSimpleName()));
    }

    public void addConditionGui(final JComponent panel) {
        cbAlways = new ExtCheckBox();
        panel.add(cbAlways);
        panel.add(new JLabel(_GUI.T.FilterRuleDialog_layoutDialogContent_lbl_always()), "spanx");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private JComboBox createEnabledBox() {
        final JComboBox ret = new JComboBox(new String[] { _GUI.T.PackagizerFilterRuleDialog_updateGUI_enabled_(), _GUI.T.PackagizerFilterRuleDialog_updateGUI_disabled_() });
        final ListCellRenderer org = ret.getRenderer();
        ret.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel r = (JLabel) org.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index < 0) {
                    r.setIcon(ret.getSelectedIndex() == 1 ? CheckBoxIcon.FALSE : CheckBoxIcon.TRUE);
                } else {
                    r.setIcon(index == 1 ? CheckBoxIcon.FALSE : CheckBoxIcon.TRUE);
                }
                if (!ret.isEnabled()) {
                    r.setIcon(NewTheme.I().getDisabledIcon(r.getIcon()));
                }
                return r;
            }
        });
        return ret;
    }

    @Override
    protected MigPanel createHeader(String string) {
        MigPanel ret = new MigPanel("ins 0", "[21,fill][][grow,fill]", "[]");
        ret.add(new JSeparator());
        JLabel label;
        ret.add(SwingUtils.toBold(label = new JLabel(string)));
        label.setIcon(new AbstractIcon(IconKey.ICON_PACKAGIZER, 14));
        ret.add(new JSeparator());
        return ret;
    }

    private JLabel createLbl(String packagizerFilterRuleDialog_layoutDialogContent_dest) {
        JLabel ret = new JLabel(packagizerFilterRuleDialog_layoutDialogContent_dest);
        return ret;
    }

    @Override
    protected PackagizerRule createReturnValue() {
        return rule;
    }

    protected JMenu createVariablesMenu(JTextComponent txtPackagename2) {
        JMenu ret = new JMenu(_GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_menu());
        // ret.add(new VariableAction(txtPackagename2,
        // _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_hoster(),
        // "<jd:hoster>"));
        // ret.add(new VariableAction(txtPackagename2,
        // _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_source(),
        // "<jd:source>"));
        ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
        ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_filename_org(), "<jd:" + PackagizerController.ORGFILENAME + ">"));
        ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_filetype_org(), "<jd:" + PackagizerController.ORGFILETYPE + ">"));
        if (getFilenameFilter().isEnabled()) {
            for (int i = 0; i < getFilenameFilter().calcPlaceholderCount(); i++) {
                ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_filename((i + 1)), "<jd:" + PackagizerController.ORGFILENAME + ":" + (i + 1) + ">"));
            }
        }
        ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.ORGPACKAGENAME + ">"));
        if (getPackagenameFilter().isEnabled()) {
            for (int i = 0; i < getPackagenameFilter().calcPlaceholderCount(); i++) {
                ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_package((i + 1)), "<jd:" + PackagizerController.ORGPACKAGENAME + ":" + (i + 1) + ">"));
            }
        }
        if (getHosterFilter().isEnabled()) {
            for (int i = 0; i < getHosterFilter().calcPlaceholderCount(); i++) {
                ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_hoster((i + 1)), "<jd:" + PackagizerController.HOSTER + ":" + (i + 1) + ">"));
            }
        }
        if (getSourceFilter().isEnabled()) {
            for (int i = 0; i < getSourceFilter().calcPlaceholderCount(); i++) {
                ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_source((i + 1)), "<jd:" + PackagizerController.SOURCE + ":" + (i + 1) + ">"));
            }
        }
        if (txtPackagename2 != txtPackagename && txtPackagename2 != txtFilename) {
            ret.add(new VariableAction(txtPackagename2, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));
        }
        return ret;
    }

    private void disable(JComponent ret) {
        ret.setEnabled(false);
        for (Component c : ret.getComponents()) {
            if (c instanceof JComponent) {
                disable((JComponent) c);
            }
        }
    }

    private void focusHelp(final ExtTextField comp, final String help) {
        comp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JDGui.help(comp.getHelpText(), help, NewTheme.I().getIcon(IconKey.ICON_INFO, 32));
            }
        });
    }

    /**
     * Returns a Linkgrabberfilter representing current settings. does NOT save the original one
     *
     * @return
     */
    private PackagizerRule getCurrentCopy() {
        PackagizerRule ret = this.rule.duplicate();
        save(ret);
        return ret;
    }

    private JLabel getLbl(PriorityAction pa) {
        final JLabel ret = new JLabel();
        ret.setIcon(NewTheme.I().getIcon("prio_" + pa.getPriority().getId(), 18));
        ret.setToolTipText(pa.getTooltipText());
        return ret;
    }

    private BooleanFilter getMatchAlwaysFilter() {
        return new BooleanFilter(cbAlways.isSelected());
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel ret = (MigPanel) super.layoutDialogContent();
        /* THEN SET */
        ret.add(createHeader(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_then()), "gaptop 10, spanx,growx,pushx");
        lblDest = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_dest());
        lblPriority = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_priority());
        lblPackagename = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_packagename());
        lblFilename = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_filename());
        lblComment = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_comment());
        lblExtract = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_extract());
        lblAutostart = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_autostart2());
        lblautoadd = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_autoadd2());
        lblChunks = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_chunks());
        lblEnable = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_enable());
        lblForce = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_force());
        cobExtract = createEnabledBox();
        cobAutostart = createEnabledBox();
        cobAutoAdd = createEnabledBox();
        cobEnable = createEnabledBox();
        cobForce = createEnabledBox();
        fpDest = new PathChooser("PackagizerDest", true) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public File doFileChooser() {
                try {
                    return DownloadFolderChooserDialog.open(getFile(), true, getDialogTitle());
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                JMenu sub = createVariablesMenu(txt);
                menu.add(sub);
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }
        };
        fpDest.setHelpText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_dest_help());
        final FilterPanel fpPriority = new FilterPanel("ins 0", "[]0[]8[]0[]8[]0[]8[]0[]8[]0[]8[]0[]8[]0[]", "[]");
        group = new ButtonGroup();
        RadioButton rbDefault = null;
        for (Priority priority : Priority.values()) {
            final PriorityAction pa = new PriorityAction(priority);
            final RadioButton rb = new RadioButton(pa);
            if (priority == Priority.DEFAULT) {
                rbDefault = rb;
            }
            group.add(rb);
            fpPriority.add(getLbl(pa));
            fpPriority.add(rb);
        }
        rbDefault.setSelected(true);
        txtPackagename = new ExtTextField() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public JPopupMenu getPopupMenu(AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(createVariablesMenu(txtPackagename));
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }
        };
        txtPackagename.setHelpText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_packagename_help_());
        txtNewFilename = new ExtTextField() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public JPopupMenu getPopupMenu(AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(createVariablesMenu(txtNewFilename));
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }
        };
        txtNewFilename.setHelpText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_filename_help_());
        txtComment = new ExtTextField() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public JPopupMenu getPopupMenu(AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(createVariablesMenu(txtComment));
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }
        };
        txtComment.setHelpText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_comment_help_());
        spChunks = new ExtSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        cbDest = new ExtCheckBox(fpDest);
        cbPriority = new ExtCheckBox(fpPriority);
        cbPackagename = new ExtCheckBox(txtPackagename);
        cbExtract = new ExtCheckBox(cobExtract);
        cbStart = new ExtCheckBox(cobAutostart);
        cbForce = new ExtCheckBox(cobForce);
        cbAdd = new ExtCheckBox(cobAutoAdd);
        cbChunks = new ExtCheckBox(spChunks);
        cbName = new ExtCheckBox(txtNewFilename);
        cbEnable = new ExtCheckBox(cobEnable);
        cbComment = new ExtCheckBox(txtComment);
        ret.add(cbDest);
        ret.add(lblDest, "spanx 2");
        ret.add(fpDest, "spanx,pushx,growx");
        link(cbDest, lblDest, fpDest);
        ret.add(cbPriority);
        ret.add(lblPriority, "spanx 2");
        ret.add(fpPriority, "spanx");
        link(cbPriority, lblPriority, fpPriority);
        ret.add(cbPackagename);
        ret.add(lblPackagename, "spanx 2");
        ret.add(txtPackagename, "spanx,pushx,growx");
        link(cbPackagename, lblPackagename, txtPackagename);
        ret.add(cbName);
        ret.add(lblFilename, "spanx 2");
        ret.add(txtNewFilename, "spanx,pushx,growx");
        link(cbName, lblFilename, txtNewFilename);
        ret.add(cbComment);
        ret.add(lblComment, "spanx 2");
        ret.add(txtComment, "spanx,pushx,growx");
        link(cbComment, lblComment, txtComment);
        ret.add(cbChunks);
        ret.add(lblChunks, "spanx 2");
        ret.add(spChunks, "spanx,pushx,growx");
        link(cbChunks, lblChunks, spChunks);
        ret.add(cbExtract);
        ret.add(lblExtract, "spanx 2");
        ret.add(cobExtract, "spanx,growx,pushx");
        link(cbExtract, lblExtract, cobExtract);
        ret.add(cbAdd);
        ret.add(lblautoadd, "spanx 2");
        ret.add(cobAutoAdd, "spanx,growx,pushx");
        link(cbAdd, lblautoadd, cobAutoAdd);
        ret.add(cbStart);
        ret.add(lblAutostart, "spanx 2");
        ret.add(cobAutostart, "spanx,growx,pushx");
        link(cbStart, lblAutostart, cobAutostart);
        ret.add(cbForce);
        ret.add(lblForce, "spanx 2");
        ret.add(cobForce, "spanx,growx,pushx");
        link(cbForce, lblForce, cobForce);
        ret.add(cbEnable);
        ret.add(lblEnable, "spanx 2");
        ret.add(cobEnable, "spanx,growx,pushx");
        link(cbEnable, lblEnable, cobEnable);
        /* THEN DO */
        ret.add(createHeader(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_do2()), "gaptop 10, spanx,growx,pushx");
        lblMove = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_move());
        fpMove = new PathChooser("PackagizerMove", true) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public File doFileChooser() {
                try {
                    return DownloadFolderChooserDialog.open(getFile(), true, getDialogTitle());
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                JMenu sub = createVariablesMenu(txt);
                menu.add(sub);
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }
        };
        fpMove.setHelpText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_dest_help());
        cbMove = new ExtCheckBox(fpMove);
        ret.add(cbMove);
        ret.add(lblMove, "spanx 2");
        ret.add(fpMove, "spanx,pushx,growx");
        link(cbMove, lblMove, fpMove);
        lblRename = createLbl(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_rename());
        txtRename = new ExtTextField() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public JPopupMenu getPopupMenu(AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(createVariablesMenu(txtRename));
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }
        };
        txtRename.setHelpText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_filename_help_());
        focusHelp(txtRename, _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables());
        focusHelp(txtPackagename, _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables());
        focusHelp(txtComment, _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables());
        focusHelp(txtNewFilename, _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables());
        focusHelp(fpDest.getTxt(), _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables());
        focusHelp(fpMove.getTxt(), _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables());
        cbRename = new ExtCheckBox(txtRename);
        ChangeListener al = new ChangeListener() {
            private boolean wasSelectedCrawlerSource = false;
            private boolean wasSelectedSource        = false;

            @Override
            public void stateChanged(ChangeEvent e) {
                if (lblSource.isEnabled() && (cbRename.isSelected() || cbMove.isSelected())) {
                    wasSelectedSource = cbSource.isSelected();
                    cbSource.setSelected(false);
                    wasSelectedCrawlerSource = cbCrawlerSource.isSelected();
                    if (wasSelectedCrawlerSource || wasSelectedSource) {
                        JDGui.help(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_title(), _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_help_msg(), NewTheme.I().getIcon(IconKey.ICON_RAR, 32));
                    }
                    cbCrawlerSource.setSelected(false);
                    cbSource.setEnabled(false);
                    cobSource.setEnabled(false);
                    txtSource.setEnabled(false);
                    lblSource.setEnabled(false);
                    cbCrawlerSource.setEnabled(false);
                    cobCrawlerSource.setEnabled(false);
                    cobCrawlerSourceOptions.setEnabled(false);
                    lblCrawlerSource.setEnabled(false);
                    cbSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    cobSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    txtSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    lblSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    cbCrawlerSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    cobCrawlerSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    cobCrawlerSourceOptions.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                    lblCrawlerSource.setToolTipText(_GUI.T.PackagizerFilterRuleDialog_stateChanged_tt_disabled_archive());
                } else if (!lblSource.isEnabled() && !cbRename.isSelected() && !cbMove.isSelected()) {
                    cbSource.setSelected(wasSelectedSource);
                    cbCrawlerSource.setSelected(wasSelectedCrawlerSource);
                    cbSource.setEnabled(true);
                    cobSource.setEnabled(cbSource.isSelected());
                    txtSource.setEnabled(cbSource.isSelected());
                    lblSource.setEnabled(true);
                    cbCrawlerSource.setEnabled(true);
                    cobCrawlerSource.setEnabled(cbCrawlerSource.isSelected());
                    cobCrawlerSourceOptions.setEnabled(cbCrawlerSource.isSelected());
                    lblCrawlerSource.setEnabled(true);
                    cbSource.setToolTipText(null);
                    cobSource.setToolTipText(null);
                    txtSource.setToolTipText(null);
                    lblSource.setToolTipText(null);
                    cbCrawlerSource.setToolTipText(null);
                    cobCrawlerSource.setToolTipText(null);
                    cobCrawlerSourceOptions.setToolTipText(null);
                    lblCrawlerSource.setToolTipText(null);
                }
            }
        };
        cbRename.addChangeListener(al);
        cbMove.addChangeListener(al);
        ret.add(cbRename);
        ret.add(lblRename, "spanx 2");
        ret.add(txtRename, "spanx,pushx,growx");
        link(cbRename, lblRename, txtRename);
        updateGUI();
        if (rule.isStaticRule()) {
            okButton.setEnabled(false);
            okButton.setText(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_cannot_modify_());
            disable(ret);
        }
        JScrollPane sp = new JScrollPane(ret);
        sp.setBorder(null);
        return sp;
    }

    private void link(final ExtCheckBox cb, JComponent... components) {
        MouseListener ml = new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                cb.setSelected(true);
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        };
        for (JComponent c : components) {
            c.addMouseListener(ml);
        }
    }

    @Override
    protected void packed() {
        super.packed();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JDGui.help(_GUI.T.PackagizerFilterRuleDialog_run_help_title(), _GUI.T.PackagizerFilterRuleDialog_run_help_msg(), new AbstractIcon(IconKey.ICON_PACKAGIZER, 32));
            }
        });
    }

    protected void runTest(String text) {
        TestWaitDialog d;
        try {
            final PackagizerRule rule = getCurrentCopy();
            matcher = new RuleMatcher(rule);
            PackagizerController packagizer = new PackagizerController(true) {
                @Override
                protected void set(CrawledLink link, PackagizerRuleWrapper lgr) {
                    final String name;
                    if (link.isNameSet()) {
                        name = link.getName();
                    } else {
                        name = null;
                    }
                    matcher.setMatches(link, lgr);
                    super.set(link, lgr);
                    // restore name to provide before/after filename
                    link.setName(name);
                }
            };
            rule.setEnabled(true);
            packagizer.add(rule);
            d = new TestWaitDialog(text, _GUI.T.PackagizerRuleDialog_runTest_title_(rule.toString()), null) {
                @Override
                protected ExtTableModel<CrawledLink> createTableModel() {
                    return new PackagizerSingleTestTableModel(matcher);
                }
            };
            d.setPackagizer(packagizer);
            java.util.List<CrawledLink> ret = Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    private void save(PackagizerRule rule) {
        rule.setFilenameFilter(getFilenameFilter());
        rule.setPackagenameFilter(getPackagenameFilter());
        rule.setHosterURLFilter(getHosterFilter());
        rule.setName(getName());
        rule.setFilesizeFilter(getFilersizeFilter());
        rule.setSourceURLFilter(getSourceFilter());
        rule.setOriginFilter(getOriginFilter());
        rule.setConditionFilter(getConditionFilter());
        rule.setFiletypeFilter(getFiletypeFilter());
        rule.setMatchAlwaysFilter(getMatchAlwaysFilter());
        if (cbDest.isSelected()) {
            final String path = fpDest.getPath();
            DownloadPathHistoryManager.getInstance().add(path);
            rule.setDownloadDestination(path);
        } else {
            rule.setDownloadDestination(null);
        }
        if (cbMove.isSelected()) {
            final String path = fpMove.getPath();
            DownloadPathHistoryManager.getInstance().add(path);
            rule.setMoveto(path);
        } else {
            rule.setMoveto(null);
        }
        rule.setRename(cbRename.isSelected() ? txtRename.getText() : null);
        rule.setLinkEnabled(cbEnable.isSelected() ? cobEnable.getSelectedIndex() == 0 : null);
        rule.setChunks(cbChunks.isSelected() ? ((Number) spChunks.getValue()).intValue() : -1);
        rule.setPriority(cbPriority.isSelected() ? prio : null);
        rule.setPackageName(cbPackagename.isSelected() ? txtPackagename.getText() : null);
        rule.setFilename(cbName.isSelected() ? txtNewFilename.getText() : null);
        rule.setComment(cbComment.isSelected() ? txtComment.getText() : null);
        rule.setAutoExtractionEnabled(cbExtract.isSelected() ? cobExtract.getSelectedIndex() == 0 : null);
        rule.setAutoAddEnabled(cbAdd.isSelected() ? cobAutoAdd.getSelectedIndex() == 0 : null);
        rule.setAutoStartEnabled(cbStart.isSelected() ? cobAutostart.getSelectedIndex() == 0 : null);
        rule.setAutoForcedStartEnabled(cbForce.isSelected() ? cobForce.getSelectedIndex() == 0 : null);
        rule.setIconKey(getIconKey());
        rule.setTestUrl(getTxtTestUrl());
        rule.setOnlineStatusFilter(getOnlineStatusFilter());
        rule.setPluginStatusFilter(getPluginStatusFilter());
    }

    @Override
    protected void setDisposed(boolean b) {
        super.setDisposed(b);
        if (b) {
            synchronized (ACTIVE_DIALOGS) {
                ACTIVE_DIALOGS.remove(rule);
            }
        }
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            save(rule);
        }
    }

    private void updateGUI() {
        setIconKey(rule.getIconKey());
        setFilenameFilter(rule.getFilenameFilter());
        setPackagenameFilter(rule.getPackagenameFilter());
        setHosterFilter(rule.getHosterURLFilter());
        setName(rule.getName());
        setFilesizeFilter(rule.getFilesizeFilter());
        setSourceFilter(rule.getSourceURLFilter());
        setFiletypeFilter(rule.getFiletypeFilter());
        setOnlineStatusFilter(rule.getOnlineStatusFilter());
        setPluginStatusFilter(rule.getPluginStatusFilter());
        setOriginFilter(rule.getOriginFilter());
        setConditionFilter(rule.getConditionFilter());
        txtPackagename.setText(rule.getPackageName());
        txtNewFilename.setText(rule.getFilename());
        txtComment.setText(rule.getComment());
        txtRename.setText(rule.getRename());
        txtTestUrl.setText(rule.getTestUrl());
        fpDest.setQuickSelectionList(DownloadPathHistoryManager.getInstance().listPaths(rule.getDownloadDestination()));
        fpDest.setPath(rule.getDownloadDestination());
        fpMove.setQuickSelectionList(DownloadPathHistoryManager.getInstance().listPaths(rule.getMoveto()));
        fpMove.setPath(rule.getMoveto());
        cbExtract.setSelected(rule.isAutoExtractionEnabled() != null);
        cbEnable.setSelected(rule.getLinkEnabled() != null);
        cobEnable.setSelectedIndex((rule.getLinkEnabled() == null || rule.getLinkEnabled()) ? 0 : 1);
        if (rule.getChunks() > 0) {
            spChunks.setValue(rule.getChunks());
        }
        cbStart.setSelected(rule.isAutoStartEnabled() != null);
        cbForce.setSelected(rule.isAutoForcedStartEnabled() != null);
        cobForce.setSelectedIndex((rule.isAutoForcedStartEnabled() == null || rule.isAutoForcedStartEnabled()) ? 0 : 1);
        cbAdd.setSelected(rule.isAutoAddEnabled() != null);
        cbAlways.setSelected(rule.getMatchAlwaysFilter() != null && rule.getMatchAlwaysFilter().isEnabled());
        cobAutoAdd.setSelectedIndex((rule.isAutoAddEnabled() == null || rule.isAutoAddEnabled()) ? 0 : 1);
        cobAutostart.setSelectedIndex((rule.isAutoStartEnabled() == null || rule.isAutoStartEnabled()) ? 0 : 1);
        cobExtract.setSelectedIndex((rule.isAutoExtractionEnabled() == null || rule.isAutoExtractionEnabled()) ? 0 : 1);
        cbChunks.setSelected(rule.getChunks() > 0);
        cbComment.setSelected(!StringUtils.isEmpty(rule.getComment()));
        cbName.setSelected(!StringUtils.isEmpty(rule.getFilename()));
        cbDest.setSelected(!StringUtils.isEmpty(rule.getDownloadDestination()));
        cbMove.setSelected(!StringUtils.isEmpty(rule.getMoveto()));
        cbRename.setSelected(!StringUtils.isEmpty(rule.getRename()));
        cbPackagename.setSelected(!StringUtils.isEmpty(rule.getPackageName()));
        cbPriority.setSelected(rule.getPriority() != null);
        prio = rule.getPriority();
        if (prio == null) {
            prio = Priority.DEFAULT;
        }
        final Enumeration<AbstractButton> priorityButtons = group.getElements();
        while (priorityButtons.hasMoreElements()) {
            final AbstractButton priorityButton = priorityButtons.nextElement();
            final Action action = ((RadioButton) priorityButton).getAction();
            if (((PriorityAction) action).getPriority().equals(prio)) {
                priorityButton.setSelected(true);
                break;
            }
        }
    }
}
