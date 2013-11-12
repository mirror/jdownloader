package org.jdownloader.controlling.contextmenu;

import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;

import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.utils.GetterSetter;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.actions.AppAction;
import org.jdownloader.images.NewTheme;

public abstract class CustomizableAppAction extends AppAction {
    private MenuItemData           menuItemData;
    private HashSet<ActionContext> setupObjects;

    public List<ActionContext> getSetupObjects() {
        if (setupObjects == null) return null;
        return new ArrayList<ActionContext>(setupObjects);
    }

    protected ImageIcon getCheckBoxedIcon(String string, boolean selected, boolean enabled) {
        return new ImageIcon(ImageProvider.merge(NewTheme.I().getIcon(string, 18), new CheckBoxIcon(selected, enabled), -2, -2, 6, 6, null, AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f)));
    }

    private long lastRequestUpdate;

    public void removeContextSetup(ActionContext contextSetup) {
        this.setupObjects.remove(contextSetup);
    }

    public void addContextSetup(ActionContext contextSetup) {
        if (this.setupObjects == null) this.setupObjects = new HashSet<ActionContext>();
        this.setupObjects.add(contextSetup);

    }

    public void loadContextSetups() {
        if (setupObjects != null) {
            fill(setupObjects);
        }
    }

    /**
     * @param setupObjects2
     */
    private void fill(HashSet<ActionContext> setupObjects2) {
        if (setupObjects2 != null && menuItemData != null) {
            for (ActionContext setupObject : setupObjects2) {
                for (GetterSetter f : ReflectionUtils.getGettersSetteres(setupObject.getClass())) {

                    try {
                        if (f.getAnnotation(Customizer.class) != null) {
                            Object v = menuItemData.getActionData().fetchSetup(f.getKey());
                            if (v == null) continue;
                            if (Clazz.isEnum(f.getType())) {

                                v = ReflectionUtils.getEnumValueOf((Class<? extends Enum>) f.getType(), v.toString());
                                if (v == null) continue;
                            }
                            f.set(setupObject, v);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void requestUpdate(Object requestor) {
        lastRequestUpdate = System.currentTimeMillis();
        fill(setupObjects);

    }

    @Override
    public void setName(String name) {
        if (menuItemData != null) {
            ActionData actionData = menuItemData.getActionData();

            if (StringUtils.isNotEmpty(actionData.getName())) {

                name = actionData.getName();

            }
            if (StringUtils.isNotEmpty(menuItemData.getName())) {

                name = menuItemData.getName();

            }
        }
        if (StringUtils.equals(MenuItemData.EMPTY_NAME, name)) {
            name = "";
        }
        if (StringUtils.isEmpty(getTooltipText()) && StringUtils.isEmpty(name)) {
            if (StringUtils.isNotEmpty(getName())) {
                setTooltipText(getName());
            }
        }
        super.setName(name);
    }

    public CustomizableAppAction() {
        super();
        if (this instanceof ActionContext) {
            addContextSetup((ActionContext) this);

        }
    }

    protected void initContextDefaults() {

    }

    public CustomizableAppAction(MenuItemData data) {
        this();

        setMenuItemData(data);

    }

    @Override
    public Object getValue(String key) {
        if (Action.MNEMONIC_KEY == key) {
            if (System.currentTimeMillis() - lastRequestUpdate > 1000) {
                System.out.println("Bad Action Usage!");
                new Exception().printStackTrace();
                requestUpdate(null);
            }

        }
        return super.getValue(key);
    }

    public void setMenuItemData(MenuItemData data) {
        this.menuItemData = data;
        fill(setupObjects);
        applyMenuItemData();

    }

    @Override
    public void setIconKey(String iconKey) {

        if (menuItemData != null) {
            ActionData actionData = menuItemData.getActionData();
            if (StringUtils.isNotEmpty(actionData.getIconKey())) {
                iconKey = actionData.getIconKey();
            }
            if (StringUtils.isNotEmpty(menuItemData.getIconKey())) {
                iconKey = menuItemData.getIconKey();

            }
        }
        super.setIconKey(iconKey);
    }

    /**
     * 
     */
    public void applyMenuItemData() {
        if (menuItemData == null) return;
        setName(getName());
        setIconKey(getIconKey());

    }

}
