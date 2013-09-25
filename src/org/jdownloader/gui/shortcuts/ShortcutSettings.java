package org.jdownloader.gui.shortcuts;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;

public interface ShortcutSettings extends ConfigInterface {

    class DefaultTextFieldCopy extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toString();
        }
    }

    class DefaultTextFieldCut extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toString();
        }
    }

    class DefaultTextFieldDelete extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0).toString();
        }
    }

    class DefaultTextFieldPaste extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toString();
        }
    }

    class DefaultTextFieldSelect extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toString();
        }
    }

    class KeyStrokeValidator extends AbstractValidator<String> {

        @Override
        public void validate(String keystroke) throws ValidationException {
            if (KeyStroke.getKeyStroke(keystroke) == null) { throw new ValidationException("Invalid KeyStroke: " + keystroke); }
        }

    }

    @AboutConfig
    @DefaultFactory(DefaultTextFieldCopy.class)
    @ValidatorFactory(KeyStrokeValidator.class)
    String getTextFieldCopy();

    @AboutConfig
    @DefaultFactory(DefaultTextFieldCut.class)
    @ValidatorFactory(KeyStrokeValidator.class)
    String getTextFieldCut();

    @AboutConfig
    @DefaultFactory(DefaultTextFieldDelete.class)
    @ValidatorFactory(KeyStrokeValidator.class)
    String getTextFieldDelete();

    @AboutConfig
    @DefaultFactory(DefaultTextFieldPaste.class)
    @ValidatorFactory(KeyStrokeValidator.class)
    String getTextFieldPaste();

    @AboutConfig
    @DefaultFactory(DefaultTextFieldSelect.class)
    @ValidatorFactory(KeyStrokeValidator.class)
    String getTextFieldSelect();

    void setTextFieldCopy(String keystroke);

    void setTextFieldCut(String keystroke);

    void setTextFieldDelete(String keystroke);

    void setTextFieldPaste(String keystroke);

    void setTextFieldSelect(String keystroke);
}
