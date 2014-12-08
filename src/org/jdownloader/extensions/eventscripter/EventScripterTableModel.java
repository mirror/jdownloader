package org.jdownloader.extensions.eventscripter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jsyntaxpane.syntaxkits.JavaScriptSyntaxKit;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public class EventScripterTableModel extends ExtTableModel<ScriptEntry> implements GenericConfigEventListener<Object> {

    private EventScripterExtension extension;

    public EventScripterTableModel(EventScripterExtension extension) {
        super("EventCallerTableModel");
        this.extension = extension;
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().addListener(this);
        update();
    }

    private void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // make sure that this class is loaded. it contains the logic to restore old settings.

                ArrayList<ScriptEntry> scripts = CFG_EVENT_CALLER.CFG.getScripts();
                if (scripts == null || scripts.size() == 0) {
                    scripts = getDefaultScriptList();

                }
                _fireTableStructureChanged(scripts, true);

            };

        };
    }

    protected ArrayList<ScriptEntry> getDefaultScriptList() {
        ArrayList<ScriptEntry> ret = new ArrayList<ScriptEntry>();
        ScriptEntry dfScript = new ScriptEntry();
        dfScript.setName(T._.example_script_name());
        dfScript.setEventTrigger(EventTrigger.ON_DOWNLOAD_CONTROLLER_STOPPED);
        dfScript.setScript(null);
        dfScript.setEnabled(false);
        ret.add(dfScript);
        return ret;
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<ScriptEntry>(_GUI._.lit_enabled()) {

            private static final long serialVersionUID = 1515656228974789237L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3224931991570756349L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isEditable(ScriptEntry obj) {
                return true;
            }

            @Override
            protected boolean getBooleanValue(ScriptEntry value) {
                return value.isEnabled();
            }

            @Override
            protected void setBooleanValue(boolean value, ScriptEntry object) {
                object.setEnabled(value);
                extension.save(getTableData());

            }
        });

        this.addColumn(new ExtTextColumn<ScriptEntry>(_GUI._.lit_name()) {

            @Override
            public String getStringValue(ScriptEntry value) {
                return value.getName();
            }

            @Override
            protected void setStringValue(String value, ScriptEntry object) {
                object.setName(value);
                extension.save(getTableData());
            }

            @Override
            public boolean isEditable(ScriptEntry obj) {
                return true;
            }
        });
        this.addColumn(new ExtComboColumn<ScriptEntry, EventTrigger>(T._.event_trigger(), new DefaultComboBoxModel<EventTrigger>(EventTrigger.values())) {
            @Override
            protected String modelItemToString(EventTrigger selectedItem) {
                return selectedItem.getLabel();
            }

            @Override
            protected EventTrigger getSelectedItem(org.jdownloader.extensions.eventscripter.ScriptEntry object) {
                return object.getEventTrigger();
            }

            @Override
            protected void setSelectedItem(org.jdownloader.extensions.eventscripter.ScriptEntry object, EventTrigger value) {
                object.setEventTrigger(value);
                extension.save(getTableData());
            }
        });

        this.addColumn(new ExtComponentColumn<ScriptEntry>(T._.edit_script()) {
            private JButton            editorBtn;
            private JButton            rendererBtn;
            private ScriptEntry        editing;
            protected MigPanel         editor;
            protected RendererMigPanel renderer;
            private RenderLabel        label;

            {
                editorBtn = new JButton("");

                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {

                            ProgressDialog p = new ProgressDialog(new ProgressGetter() {

                                @Override
                                public void run() throws Exception {
                                    JavaScriptSyntaxKit.initKit();

                                }

                                @Override
                                public String getString() {
                                    return null;
                                }

                                @Override
                                public int getProgress() {
                                    return -1;
                                }

                                @Override
                                public String getLabelString() {
                                    return null;
                                }
                            }, 0, T._.loading_editor_title(), "", null);

                            UIOManager.I().show(null, p);
                            JavaScriptEditorDialog d = new JavaScriptEditorDialog(extension, editing);
                            UIOManager.I().show(null, d);

                            if (d.getCloseReason() == CloseReason.OK) {
                                String script = d.getScript();
                                if (script != null) {
                                    editing.setScript(script);
                                    extension.save(getTableData());
                                }
                            }

                        }
                    }
                });
                label = new RenderLabel();
                rendererBtn = new JButton("");
                this.editor = new MigPanel("ins 1", "[grow,fill]", "[18!]") {

                    @Override
                    public void requestFocus() {
                    }

                };
                editor.add(editorBtn);
                this.renderer = new RendererMigPanel("ins 1", "[grow,fill]", "[18!]");
                renderer.add(rendererBtn);
                setClickcount(1);
            }

            @Override
            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public boolean isSortable(final ScriptEntry obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(ScriptEntry value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, ScriptEntry obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(ScriptEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public boolean isEnabled(ScriptEntry obj) {
                return true;
            }

            @Override
            public void configureRendererComponent(ScriptEntry value, boolean isSelected, boolean hasFocus, int row, int column) {

                rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
                rendererBtn.setText(_GUI._.lit_edit());
                // }
            }

            @Override
            public void configureEditorComponent(ScriptEntry value, boolean isSelected, int row, int column) {
                editing = value;
                editorBtn.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
                editorBtn.setText(_GUI._.lit_edit());

            }

            @Override
            public void resetEditor() {
            }

            @Override
            public void resetRenderer() {
            }

        });
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        update();
    }

}
