/*
 * $Id: BeanInfoSupport.java,v 1.12 2007/12/09 17:42:29 stolis Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 * Useful baseclass for BeanInfos. With this class, normal introspection occurs
 * and then you are given the opportunity to reconfigure portions of the
 * bean info in the <code>initialize</code> method.
 *
 * @author rbair, Jan Stola
 */
public abstract class BeanInfoSupport extends SimpleBeanInfo {
    private static Logger LOG = Logger.getLogger(BeanInfoSupport.class.getName());
    
    /**
     * Indicates whether I am introspecting state for the give class. This
     * helps prevent infinite loops
     */
    private static Map<Class, Boolean> introspectingState = new HashMap<Class, Boolean>();
    /**
     * The class of the bean that this BeanInfoSupport is for
     */
    private Class beanClass;
    
    /**
     * @see BeanInfo
     */
    private int defaultPropertyIndex = -1;
    /**
     * @see BeanInfo
     */
    private int defaultEventIndex = -1;
    /**
     * The 16x16 color icon
     */
    private Image iconColor16 = null;
    /**
     * The 32x32 color icon
     */
    private Image iconColor32 = null;
    /**
     * The 16x16 monochrome icon
     */
    private Image iconMono16 = null;
    /**
     * The 32x32 monochrome icon
     */
    private Image iconMono32 = null;
    /**
     * A reference to the icon. This String must be of a form that
     * ImageIO can use to locate and load the icon image
     */
    private String iconNameC16 = null;
    /**
     * A reference to the icon. This String must be of a form that
     * ImageIO can use to locate and load the icon image
     */
    private String iconNameC32 = null;
    /**
     * A reference to the icon. This String must be of a form that
     * ImageIO can use to locate and load the icon image
     */
    private String iconNameM16 = null;
    /**
     * A reference to the icon. This String must be of a form that
     * ImageIO can use to locate and load the icon image
     */
    private String iconNameM32 = null;
    
    private BeanDescriptor beanDescriptor;
    
    private Map<String, PropertyDescriptor> properties = new TreeMap<String, PropertyDescriptor>();
    private Map<String, EventSetDescriptor> events = new TreeMap<String, EventSetDescriptor>();
    private Map<String, MethodDescriptor> methods = new TreeMap<String, MethodDescriptor>();

    /**
     * Creates a new instance of BeanInfoSupport.
     * 
     * @param beanClass class of the bean.
     */
    public BeanInfoSupport(Class beanClass) {
        this.beanClass = beanClass;
        if (!isIntrospecting()) {
            introspectingState.put(beanClass, Boolean.TRUE);
            try {
                Class superClass = beanClass.getSuperclass();
                while (superClass != null) {
                    Introspector.flushFromCaches(superClass);
                    superClass = superClass.getSuperclass();
                }
                BeanInfo info = Introspector.getBeanInfo(beanClass);
                beanDescriptor = info.getBeanDescriptor();
                if (beanDescriptor != null) {
                    Class customizerClass = getCustomizerClass();
                    beanDescriptor = new BeanDescriptor(beanDescriptor.getBeanClass(),
                            customizerClass == null ? beanDescriptor.getCustomizerClass()
                            : customizerClass);
                } else {
                    beanDescriptor = new BeanDescriptor(beanClass, getCustomizerClass());
                }
                for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                    properties.put(pd.getName(), pd);
                }
                for (EventSetDescriptor esd : info.getEventSetDescriptors()) {
                    events.put(esd.getName(), esd);
                }
                for (MethodDescriptor md : info.getMethodDescriptors()) {
                    methods.put(md.getName(), md);
                }
                
                defaultPropertyIndex = info.getDefaultPropertyIndex();
                defaultEventIndex = info.getDefaultEventIndex();
                                
                iconColor16 = loadStandardImage(info, BeanInfo.ICON_COLOR_16x16);
                iconColor32 = loadStandardImage(info, BeanInfo.ICON_COLOR_32x32);
                iconMono16 = loadStandardImage(info, BeanInfo.ICON_MONO_16x16);
                iconMono32 = loadStandardImage(info, BeanInfo.ICON_MONO_32x32);
            } catch (Exception e) {
                e.printStackTrace();
            }
            introspectingState.put(beanClass, Boolean.FALSE);
            initialize();
        }
    }
    
    private boolean isIntrospecting() {
        Boolean b = introspectingState.get(beanClass);
        return b == null ? false : b.booleanValue();
    }

    /**
     * attempts to load a png icon from the
     * resource directory beneath beaninfo, named like:
     *   JXTaskPaneContainer16.png
     *   JXTaskPaneContainer16-mono.png
     *   JXTaskPaneContainer32.png
     *   JXTaskPaneContainer32-mono.png
     * 
     * if any of the icons is missing, an attempt is made to
     * get an icon via introspection. If that fails, the icon
     * will be set to placeholder16.png or one of the derivitives
     */
    private Image loadStandardImage(BeanInfo info, int size) {
        String s = "";
        switch (size) {
            case BeanInfo.ICON_COLOR_16x16: s = "16"; break;
            case BeanInfo.ICON_COLOR_32x32: s = "32"; break;
            case BeanInfo.ICON_MONO_16x16: s = "16-mono"; break;
            case BeanInfo.ICON_MONO_32x32: s = "32-mono"; break;
        }
        String iconName = beanClass.getSimpleName() + s + ".png";
        
        Image image = null;
        try {
            image = loadImage("resources/" + iconName);
        } catch (Exception e) {
            LOG.info("No icon named " + iconName + " was found");
        }
        
        return image;
    }
    
    @Override
    public Image loadImage(final String resourceName) {
        URL url = getClass().getResource(resourceName);
        return (url == null) ? null : new ImageIcon(url).getImage();
    }

    /**
     * Called by the constructor during the proper time so that subclasses
     * can override the settings/values for the various beaninfo properties.
     * For example, you could call setDisplayName("Foo Name", "foo") to change
     * the foo properties display name
     */
    protected abstract void initialize();

    /**
     * Override this method if you want to return a custom customizer class
     * for the bean
     * 
     * @return <code>null</code>.
     */
    protected Class getCustomizerClass() {
        return null;
    }
    
    //------------------------------------ Methods for mutating the BeanInfo    
    /**
     * Specify the name/url/path to the small 16x16 color icon
     * 
     * @param name name of the icon.
     */
    protected void setSmallColorIconName(String name) {
        iconNameC16 = name;
    }
    
    /**
     * Specify the name/url/path to the 32x32 color icon
     * 
     * @param name name of the icon.
     */
    protected void setColorIconName(String name) {
        iconNameC32 = name;
    }

    /**
     * Specify the name/url/path to the small 16x16 monochrome icon
     * 
     * @param name name of the icon.
     */
    protected void setSmallMonoIconName(String name) {
        iconNameM16 = name;
    }

    /**
     * Specify the name/url/path to the 32x32 monochrome icon
     * 
     * @param name name of the icon.
     */
    protected void setMonoIconName(String name) {
        iconNameM32 = name;
    }
    
    /**
     * Changes the display name of the given named property. Property names
     * are always listed last to allow for varargs
     * 
     * @param displayName display name of the property.
     * @param propertyName name of the property.
     */
    protected void setDisplayName(String displayName, String propertyName) {
        PropertyDescriptor pd = properties.get(propertyName);
        if (pd != null) {
            pd.setDisplayName(displayName);
        } else {
            LOG.log(Level.WARNING, "Failed to set display name for property '" +
                    propertyName + "'. No such property was found");
        }
    }
    
    /**
     * Sets the given named properties to be "hidden".
     * 
     * @param hidden determines whether the properties should be marked as hidden or not.
     * @param propertyNames name of properties.
     * @see PropertyDescriptor
     */
    protected void setHidden(boolean hidden, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setHidden(hidden);
            } else {
                LOG.log(Level.WARNING, "Failed to set hidden attribute for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setExpert(boolean expert, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setExpert(expert);
            } else {
                LOG.log(Level.WARNING, "Failed to set expert attribute for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setPreferred(boolean preferred, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setPreferred(preferred);
            } else {
                LOG.log(Level.WARNING, "Failed to set preferred attribute for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setBound(boolean bound, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setBound(bound);
            } else {
                LOG.log(Level.WARNING, "Failed to set bound attribute for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setConstrained(boolean constrained, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setConstrained(constrained);
            } else {
                LOG.log(Level.WARNING, "Failed to set constrained attribute for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setCategory(String categoryName, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setValue("category", categoryName);
            } else {
                LOG.log(Level.WARNING, "Failed to set category for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setPropertyEditor(Class editorClass, String... propertyNames) {
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setPropertyEditorClass(editorClass);
            } else {
                LOG.log(Level.WARNING, "Failed to set property editor for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    protected void setEnumerationValues(EnumerationValue[] values, String... propertyNames) {
        if (values == null) {
            return;
        }
        
        Object[] enumValues = new Object[values.length * 3];
        int index = 0;
        for (EnumerationValue ev : values) {
            enumValues[index++] = ev.getName();
            enumValues[index++] = ev.getValue();
            enumValues[index++] = ev.getJavaInitializationString();
        }
        
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = properties.get(propertyName);
            if (pd != null) {
                pd.setValue("enumerationValues", enumValues);
            } else {
                LOG.log(Level.WARNING, "Failed to set enumeration values for property '" +
                        propertyName + "'. No such property was found");
            }
        }
    }
    
    //----------------------------------------------------- BeanInfo methods
    /**
     * Gets the bean's <code>BeanDescriptor</code>s.
     *
     * @return BeanDescriptor describing the editable
     * properties of this bean.  May return null if the
     * information should be obtained by automatic analysis.
     */
    @Override
    public BeanDescriptor getBeanDescriptor() {
        return isIntrospecting() ? null : beanDescriptor;
    }
    
    /**
     * Gets the bean's <code>PropertyDescriptor</code>s.
     *
     * @return An array of PropertyDescriptors describing the editable
     * properties supported by this bean.  May return null if the
     * information should be obtained by automatic analysis.
     * <p>
     * If a property is indexed, then its entry in the result array will
     * belong to the IndexedPropertyDescriptor subclass of PropertyDescriptor.
     * A client of getPropertyDescriptors can use "instanceof" to check
     * if a given PropertyDescriptor is an IndexedPropertyDescriptor.
     */
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return isIntrospecting() 
            ? null
            : properties.values().toArray(new PropertyDescriptor[0]);
    }
    
    /**
     * Gets the bean's <code>EventSetDescriptor</code>s.
     *
     * @return  An array of EventSetDescriptors describing the kinds of
     * events fired by this bean.  May return null if the information
     * should be obtained by automatic analysis.
     */
    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        return isIntrospecting()
            ? null
            : events.values().toArray(new EventSetDescriptor[0]);
    }
    
    /**
     * Gets the bean's <code>MethodDescriptor</code>s.
     *
     * @return  An array of MethodDescriptors describing the methods
     * implemented by this bean.  May return null if the information
     * should be obtained by automatic analysis.
     */
    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        return isIntrospecting()
            ? null
            : methods.values().toArray(new MethodDescriptor[0]);
    }
    
    /**
     * A bean may have a "default" property that is the property that will
     * mostly commonly be initially chosen for update by human's who are
     * customizing the bean.
     * @return  Index of default property in the PropertyDescriptor array
     * 		returned by getPropertyDescriptors.
     * <P>	Returns -1 if there is no default property.
     */
    @Override
    public int getDefaultPropertyIndex() {
        return isIntrospecting() ? -1 : defaultPropertyIndex;
    }
    
    /**
     * A bean may have a "default" event that is the event that will
     * mostly commonly be used by human's when using the bean.
     * @return Index of default event in the EventSetDescriptor array
     *		returned by getEventSetDescriptors.
     * <P>	Returns -1 if there is no default event.
     */
    @Override
    public int getDefaultEventIndex() {
        return isIntrospecting() ? -1 : defaultEventIndex;
    }
    
    /**
     * This method returns an image object that can be used to
     * represent the bean in toolboxes, toolbars, etc.   Icon images
     * will typically be GIFs, but may in future include other formats.
     * <p>
     * Beans aren't required to provide icons and may return null from
     * this method.
     * <p>
     * There are four possible flavors of icons (16x16 color,
     * 32x32 color, 16x16 mono, 32x32 mono).  If a bean choses to only
     * support a single icon we recommend supporting 16x16 color.
     * <p>
     * We recommend that icons have a "transparent" background
     * so they can be rendered onto an existing background.
     *
     * @param  iconKind  The kind of icon requested.  This should be
     *    one of the constant values ICON_COLOR_16x16, ICON_COLOR_32x32,
     *    ICON_MONO_16x16, or ICON_MONO_32x32.
     * @return  An image object representing the requested icon.  May
     *    return null if no suitable icon is available.
     */
    @Override
    public java.awt.Image getIcon(int iconKind) {
        switch ( iconKind ) {
            case ICON_COLOR_16x16:
                return getImage(iconNameC16, iconColor16);
            case ICON_COLOR_32x32:
                return getImage(iconNameC32, iconColor32);
            case ICON_MONO_16x16:
                return getImage(iconNameM16, iconMono16);
            case ICON_MONO_32x32:
                return getImage(iconNameM32, iconMono32);
            default:
                return null;
        }
    }
    
    private java.awt.Image getImage(String name, java.awt.Image img) {
        if (img == null) {
            if (name != null) {
                img = loadImage(name);
            }
        }
        return img;
    }
}
