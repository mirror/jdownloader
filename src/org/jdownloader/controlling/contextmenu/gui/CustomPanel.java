package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.GetterSetter;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.gui.views.downloads.action.ByPassDialogSetup;
import org.jdownloader.gui.views.downloads.action.Modifier;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class CustomPanel extends MigPanel {

    private MenuManagerDialog managerFrame;
    private LogSource         logger;

    public CustomPanel(MenuManagerDialog managerFrame) {
        super("ins 0", "[]", "[]");
        this.managerFrame = managerFrame;
        logger = managerFrame.getLogger();
        setOpaque(false);
    }

    public void add(final ActionData actionData, final CustomizableAppAction action, final ActionContext actionClass, final GetterSetter gs) {
        try {

            if (Clazz.isBoolean(gs.getType())) {
                JLabel lbl;
                add(lbl = new JLabel(gs.getAnnotation(Customizer.class).name()), "pushx");
                final JCheckBox jb = new JCheckBox();
                add(jb, "wrap,alignx right");
                try {
                    jb.setSelected((Boolean) actionData.fetchSetup(gs.getKey()));
                } catch (Exception e) {
                    try {
                        jb.setSelected((Boolean) gs.get(actionClass));
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                }
                jb.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        actionData.putSetup(gs.getKey(), jb.isSelected());
                        if (action instanceof CustomizableAppAction) {
                            ((CustomizableAppAction) action).loadContextSetups();
                        }
                        managerFrame.repaint();

                    }
                });
                if (gs.getKey().equals("BypassDialog") && gs.getGetter().getDeclaringClass().isAssignableFrom(ByPassDialogSetup.class) && CFG_GUI.CFG.isBypassAllRlyDeleteDialogsEnabled()) {
                    jb.setSelected(true);
                    jb.setEnabled(false);
                    lbl.setEnabled(false);
                }
            } else if (gs.getType() == Modifier.class) {
                final ExtTextField shortcut = new ExtTextField();
                shortcut.setHelpText(_GUI._.InfoPanel_InfoPanel_shortcuthelp2());
                shortcut.setEditable(false);
                String value = (String) actionData.fetchSetup(gs.getKey());
                Modifier mod = null;

                if (value != null) {
                    mod = Modifier.create(value);
                }
                if (mod == null) {
                    mod = (Modifier) gs.get(actionClass);

                }
                if (mod != null) {

                    shortcut.setText(KeyEvent.getKeyModifiersText(mod.getKeyStroke().getModifiers()));
                }
                shortcut.addKeyListener(new KeyListener() {

                    @Override
                    public void keyTyped(KeyEvent e) {
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                    }

                    @Override
                    public void keyPressed(KeyEvent event) {
                        KeyStroke ks = KeyStroke.getKeyStroke(event.getKeyCode(), event.getModifiersEx());

                        Modifier mod = Modifier.create(ks.toString());
                        System.out.println(ks + " - " + ks.getModifiers());
                        if (mod != null) {
                            shortcut.setText(KeyEvent.getKeyModifiersText(mod.getKeyStroke().getModifiers()));
                            actionData.putSetup(gs.getKey(), mod.toString());
                        } else {
                            shortcut.setText("");
                            actionData.putSetup(gs.getKey(), null);
                        }
                        if (action instanceof CustomizableAppAction) {
                            ((CustomizableAppAction) action).loadContextSetups();
                        }

                        managerFrame.repaint();
                    }

                });
                add(new JLabel(gs.getAnnotation(Customizer.class).name()), "pushx,growx");
                add(shortcut, "spanx,width 20:120:n");
            } else if (Clazz.isEnum(gs.getType())) {

                final Object[] values = ReflectionUtils.getEnumValues((Class<? extends Enum>) gs.getType());
                String value = (String) actionData.fetchSetup(gs.getKey());
                Object myValue = value == null ? null : ReflectionUtils.getEnumValueOf((Class<? extends Enum>) gs.getType(), value);

                if (value == null || myValue == null) {
                    myValue = gs.get(actionClass);

                    value = myValue.toString();
                }

                int index = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i].toString().equals(value)) myValue = values[i];

                    if (myValue == values[i]) index = i;

                }
                final PseudoCombo cb = new PseudoCombo(values) {
                    protected boolean isHideSelf() {
                        return false;
                    }

                    protected javax.swing.Icon getIcon(Object v, boolean closed) {
                        if (closed) return null;
                        if (getSelectedItem() == v) {
                            return CheckBoxIcon.TRUE;
                        } else {
                            return CheckBoxIcon.FALSE;
                        }

                    };

                    public void onChanged(Object newValue) {
                        actionData.putSetup(gs.getKey(), getSelectedItem().toString());
                        if (action instanceof CustomizableAppAction) {
                            ((CustomizableAppAction) action).loadContextSetups();
                        }
                        managerFrame.repaint();
                    };

                    protected javax.swing.Icon getPopIcon(boolean closed) {

                        if (closed) {
                            if (isPopDown()) {
                                return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);
                            } else {
                                return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
                            }
                        } else {
                            if (isPopDown()) {
                                return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
                            } else {
                                return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);

                            }
                        }
                    };

                    protected String getLabel(Object v, boolean closed) {
                        String ret = null;
                        EnumLabel lbl;
                        try {
                            lbl = ((Class) gs.getType()).getDeclaredField(v.toString()).getAnnotation(EnumLabel.class);

                            if (lbl != null) {
                                ret = lbl.value();
                            } else if (v instanceof LabelInterface) {
                                ret = ((LabelInterface) v).getLabel();

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (StringUtils.isEmpty(ret)) {
                            ret = v.toString();
                        }
                        if (closed && ret.length() > 50) {
                            ret = ret.substring(0, 50) + "...";
                        }
                        return ret;

                    };
                };
                cb.setSelectedItem(values[index]);
                add(new JLabel(gs.getAnnotation(Customizer.class).name()), "growx,spanx,wrap");
                add(cb, "growx,spanx,wrap");
            } else if (gs.getType() == String.class) {

                String value = (String) actionData.fetchSetup(gs.getKey());

                if (value == null) {
                    value = (String) gs.get(actionClass);

                }

                final ExtTextField cb = new ExtTextField() {

                    @Override
                    public void onChanged() {
                        actionData.putSetup(gs.getKey(), getText());
                        if (action instanceof CustomizableAppAction) {
                            ((CustomizableAppAction) action).loadContextSetups();
                        }
                        managerFrame.repaint();
                    }

                };

                cb.setText(value);
                add(new JLabel(gs.getAnnotation(Customizer.class).name()), "growx,spanx,wrap");
                add(cb, "growx,spanx,wrap,pushx,growx");
            }
        } catch (Exception e) {
            logger.log(e);
        }
    }
}
