package jd;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import org.appwork.app.gui.copycutpaste.CopyPasteSupport;
import org.jdownloader.gui.shortcuts.CFG_SHORTCUT;

public class CustomCopyPasteSupport extends CopyPasteSupport {

    public CustomCopyPasteSupport() {
        super();
        // KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

        //
        // im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);

        // UIDefaults defaults = UIManager.getDefaults();
        // Enumeration newKeys = defaults.keys();
        //
        // while (newKeys.hasMoreElements()) {
        // Object obj = newKeys.nextElement();
        // System.out.printf("%50s : %s\n", obj, UIManager.get(obj));
        // }
        //
        // UIManager.put("TextField.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
        //
        // }));
        //
        // InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
        // ActionMap am = (ActionMap) UIManager.get("TextField.actionMap");
        //
        // im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK), DefaultEditorKit.copyAction);
        // am.put(DefaultEditorKit.copyAction, new AppAction() {
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // System.out.println("Paste!");
        // }
        // });
        // System.out.println(1);
    }

    @Override
    protected AbstractAction createCutAction(JTextComponent t) {
        AbstractAction ret = super.createCutAction(t);
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(CFG_SHORTCUT.CFG.getTextFieldCut()));
        return ret;
    }

    @Override
    protected AbstractAction createSelectAction(JTextComponent t) {
        AbstractAction ret = super.createSelectAction(t);
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(CFG_SHORTCUT.CFG.getTextFieldSelect()));
        return ret;
    }

    @Override
    protected AbstractAction createDeleteAction(JTextComponent t) {
        AbstractAction ret = super.createDeleteAction(t);
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(CFG_SHORTCUT.CFG.getTextFieldDelete()));
        return ret;
    }

    @Override
    protected AbstractAction createPasteAction(JTextComponent t) {
        AbstractAction ret = super.createPasteAction(t);
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(CFG_SHORTCUT.CFG.getTextFieldPaste()));
        return ret;
    }

    @Override
    protected AbstractAction createCopyAction(JTextComponent t) {
        AbstractAction ret = super.createCopyAction(t);
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(CFG_SHORTCUT.CFG.getTextFieldCopy()));
        return ret;
    }

}
