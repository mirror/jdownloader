package org.jdownloader.extensions.eventscripter;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Script;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;

public class JavaScriptEditorDialog extends AbstractDialog<Object> {
    private static final String                   CLEANUP = "[^\\w\\d\\(\\)\\+\\-\\[\\]\\;\\,/\\\\]";
    private ScriptEntry                           entry;
    private JEditorPane                           editor;
    private org.appwork.scheduler.DelayedRunnable delayer;
    private JToolBar                              toolbar;
    private Global                                scope;
    private MigPanel                              p;
    private JScrollPane                           apiScrollbar;
    private JScrollPane                           scrollpane;
    private EventScripterExtension                extension;
    private TriggerSetupPanel                     settingsPanel;
    private Map<String, Object>                   settingsMap;

    public JavaScriptEditorDialog(EventScripterExtension extension, ScriptEntry entry) {
        super(Dialog.STYLE_HIDE_ICON, T.T.script_editor_title(entry.getName()), null, _GUI.T.lit_save(), null);
        this.entry = entry;
        this.extension = extension;
        setLocator(new RememberAbsoluteDialogLocator(getClass().getSimpleName()));
        setDimensor(new RememberLastDialogDimension(getClass().getSimpleName()));
    }

    @Override
    protected ScriptEntry createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredHeight() {
        return 300;
    }

    @Override
    protected int getPreferredWidth() {
        return 1024;
    }

    @Override
    public JComponent layoutDialogContent() {
        // dummy. @see #relayout
        p = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[][][grow,fill][grow,fill]");
        toolbar = new JToolBar();
        toolbar.setRollover(true);
        toolbar.setFloatable(false);
        toolbar.setPreferredSize(new Dimension(-1, 22));
        p.add(toolbar);
        settingsMap = entry.getEventTriggerSettings();
        settingsPanel = entry.getEventTrigger().createSettingsPanel(settingsMap);
        if (settingsPanel == null) {
            final Checkbox checkBox = new Checkbox(entry.getEventTrigger().isSynchronous(settingsMap));
            settingsPanel = new TriggerSetupPanel(0) {
                @Override
                public void save() {
                    entry.getEventTrigger().setSynchronous(settingsMap, checkBox.isSelected());
                }
            };
            settingsPanel.addDescriptionPlain(T.T.synchronous_desc());
            settingsPanel.addPair(T.T.synchronous(), null, checkBox);
        }
        final JEditorPane defaults = new JEditorPane();
        // defaults.setFocusable(false);
        p.add(apiScrollbar = new JScrollPane(defaults) {
            @Override
            public Dimension getPreferredSize() {
                Dimension ret = defaults.getPreferredSize();
                ret.width += 10;
                ret.height += 10;
                return super.getPreferredSize();
            }
        });
        defaults.setEditable(false);
        defaults.setContentType("text/javascript; charset=UTF-8");
        defaults.setText(ScriptEnvironment.getAPIDescription(entry.getEventTrigger().getAPIClasses()) + "\r\n" + entry.getEventTrigger().getAPIDescription());
        editor = new JEditorPane();
        // editor.setContentType("text/html");
        p.add(scrollpane = new JScrollPane(editor) {
            @Override
            public Dimension getPreferredSize() {
                Dimension ret = editor.getPreferredSize();
                ret.width += 10;
                ret.height += 10;
                ret.height = Math.max(ret.height, 200);
                return super.getPreferredSize();
            }
        });
        delayer = new org.appwork.scheduler.DelayedRunnable(1000, 5000) {
            @Override
            public void delayedrun() {
                updateHighlighter();
            }
        };
        editor.setContentType("text/javascript; charset=UTF-8");
        // editor.setFont(Font.getFont("Dialog"));
        String txt = entry.getScript();
        if (StringUtils.isEmpty(txt)) {
            txt = T.T.emptyScript();
        }
        editor.setText(txt);
        delayer.resetAndStart();
        // toolbar
        toolbar.add(new ExtButton(new AppAction() {
            {
                // setIconKey(IconKey.ICON_TEXT);
                setSelected(CFG_EVENT_CALLER.CFG.isAPIPanelVisible());
                setName(T.T.editor_showhelp());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_EVENT_CALLER.CFG.setAPIPanelVisible(!CFG_EVENT_CALLER.CFG.isAPIPanelVisible());
                relayout();
            }
        }));
        toolbar.add(new ExtButton(new AppAction() {
            {
                setName(T.T.editor_autoformat());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                updateHighlighter();
            }
        }));
        toolbar.add(new ExtButton(new AppAction() {
            {
                setName(T.T.editor_testcompile());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                extension.runTestCompile(entry.getEventTrigger(), editor.getText());
            }
        }));
        toolbar.add(new ExtButton(new AppAction() {
            {
                setName(T.T.editor_testrun());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                extension.runTest(entry.getEventTrigger(), entry.getName(), editor.getText());
            }
        }));
        relayout();
        return p;
    }

    protected void relayout() {
        p.removeAll();
        if (CFG_EVENT_CALLER.CFG.isAPIPanelVisible()) {
            p.setLayout("ins 0,wrap 1", "[grow,fill]", "[][][][grow,fill]");
        } else {
            p.setLayout("ins 0,wrap 1", "[grow,fill]", "[][][grow,fill]");
        }
        p.add(toolbar);
        if (settingsPanel != null) {
            p.add(settingsPanel);
        } else {
            p.add(Box.createGlue());
        }
        if (CFG_EVENT_CALLER.CFG.isAPIPanelVisible()) {
            p.add(apiScrollbar, "height 200:n:n");
        }
        p.add(scrollpane, "height 200:n:n");
        p.revalidate();
        p.repaint();
    }

    protected void updateHighlighter() {
        final AtomicInteger caretPosition = new AtomicInteger();
        String text = new EDTHelper<String>() {
            @Override
            public String edtRun() {
                caretPosition.set(editor.getCaretPosition());
                return editor.getText();
            }
        }.getReturnValue();
        String before = text.substring(0, caretPosition.get()).replaceAll(CLEANUP, "");
        final String formatedText = format(text);
        if (!formatedText.equals(text)) {
            for (int i = 0; i < formatedText.length(); i++) {
                String sb = formatedText.substring(0, i).replaceAll(CLEANUP, "");
                if (sb.length() == before.length()) {
                    final int caret = i;
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            editor.setText(formatedText);
                            editor.setCaretPosition(caret);
                        }
                    }.waitForEDT();
                    return;
                }
            }
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    editor.setText(formatedText);
                }
            }.waitForEDT();
        }
    }

    private synchronized String format(String script) {
        try {
            Context cx = Context.enter();
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_1_5);
            if (scope == null) {
                scope = new Global();
                scope.init(cx);
                String lib;
                lib = IO.readURLToString(ScriptEntry.class.getResource("js_beautifier.js"));
                Script compiledLibrary = JSHtmlUnitPermissionRestricter.compileTrustedString(cx, scope, lib, "", 1, null);
                JSHtmlUnitPermissionRestricter.evaluateTrustedString(cx, scope, "global=this;", "", 1, null);
                compiledLibrary.exec(cx, scope);
            }
            //
            // Class[] classes = new Class[] { Boolean.class, Integer.class, Long.class, String.class, Double.class, Float.class,
            // net.sourceforge.htmlunit.corejs.javascript.EcmaError.class, ProcessRunner.class, DownloadLinkAPIStorableV2.class };
            // String preloadClasses = "";
            // for (Class c : classes) {
            // preloadClasses += "load=" + c.getName() + ";\r\n";
            // }
            // preloadClasses += "var call=" + ProcessRunner.class.getName() + ".call;var alert=" + ProcessRunner.class.getName() +
            // ".alert;delete load;";
            ScriptableObject.putProperty(scope, "text", script);
            String formated = (String) JSHtmlUnitPermissionRestricter.evaluateTrustedString(cx, scope, "js_beautify(text, {   });", "", 1, null);
            return formated;
            // ProcessBuilderFactory.runCommand(commandline);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
        return script;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public void pack() {
        this.getDialog().pack();
    }

    public String getScript() {
        return new EDTHelper<String>() {
            @Override
            public String edtRun() {
                return editor.getText();
            }
        }.getReturnValue();
    }

    public Map<String, Object> getEventTriggerSetup() {
        if (settingsPanel != null) {
            settingsPanel.save();
            return settingsMap;
        }
        return null;
    }
}
