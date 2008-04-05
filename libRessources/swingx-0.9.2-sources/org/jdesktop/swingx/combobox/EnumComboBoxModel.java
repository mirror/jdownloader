/*
 * $Id: EnumComboBoxModel.java,v 1.7 2007/07/16 13:52:20 kschaefe Exp $
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
package org.jdesktop.swingx.combobox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * <p>
 * A ComboBoxModel implementation that safely wraps an Enum. It allows the
 * developer to directly use an enum as their model for a combobox without any
 * extra work, though the display can can be further customized.
 * </p>
 * 
 * <h4>Simple Usage</h4>
 * 
 * <p>
 * The simplest usage is to wrap an <code>enum</code> inside the
 * <code>EnumComboBoxModel</code> and then set it as the model on the combo
 * box. The combo box will then appear on screen with each value in the
 * <code>enum</code> as a value in the combobox.
 * </p>
 * <p>
 * ex:
 * </p>
 * 
 * <pre><code>
 *  enum MyEnum { GoodStuff, BadStuff };
 *  ...
 *  JComboBox combo = new JComboBox();
 *  combo.setModel(new EnumComboBoxModel(MyEnum.class));
 * </code></pre>
 * 
 * <h4>Type safe access</h4>
 * <p>
 * By using generics and co-variant types you can make accessing elements from
 * the model be completely typesafe. ex:
 * </p>
 * 
 * <pre><code>
 * EnumComboBoxModel&lt;MyEnum&gt; enumModel = new EnumComboBoxModel&lt;MyEnum1&gt;(
 *         MyEnum1.class);
 * 
 * MyEnum first = enumModel.getElement(0);
 * 
 * MyEnum selected = enumModel.getSelectedItem();
 * </code></pre>
 * 
 * <h4>Advanced Usage</h4>
 * <p>
 * Since the exact <code>toString()</code> value of each enum constant may not
 * be exactly what you want on screen (the values won't have spaces, for
 * example) you can override to toString() method on the values when you declare
 * your enum. Thus the display value is localized to the enum and not in your
 * GUI code. ex:
 * 
 * <pre><code>
 *    private enum MyEnum {GoodStuff, BadStuff;
 *        public String toString() {
 *           switch(this) {
 *               case GoodStuff: return &quot;Some Good Stuff&quot;;
 *               case BadStuff: return &quot;Some Bad Stuff&quot;;
 *           }
 *           return &quot;ERROR&quot;;
 *        }
 *    };
 * </code></pre>
 * 
 * Note: if more than one enum constant returns the same {@code String} via
 * {@code toString()}, this model will throw an exception on creation.
 * 
 * @author joshy
 * @author Karl Schaefer
 */
public class EnumComboBoxModel<E extends Enum<E>> 
        extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 2176566393195371004L;
    
    private final Map<String, E> valueMap;
    private final Class<E> enumClass;
    private final List<E> quickList;
    private E selected = null;

    /**
     * Creates an {@code EnumComboBoxModel} for the enum represent by the
     * {@code Class} {@code en}.
     * 
     * @param en
     *            the enum class type
     * @throws IllegalArgumentException
     *             if the {@code Enum.toString} returns the same value for more
     *             than one constant
     */
    public EnumComboBoxModel(Class<E> en) {
        //we could size these, probably not worth it; enums are usually small 
        valueMap = new HashMap<String, E>();
        quickList = new ArrayList<E>();
        enumClass = en;
        
        EnumSet<E> ens = EnumSet.allOf(en);
        Iterator<E> iter = ens.iterator();
        
        while (iter.hasNext()) {
            E element = iter.next();
            String s = element.toString();
            
            if (valueMap.containsKey(s)) {
                throw new IllegalArgumentException(
                        "multiple constants map to one string value");
            }
            
            valueMap.put(s, element);
            quickList.add(element);
        }
        
        selected = quickList.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return quickList.size();
    }

    /**
     * {@inheritDoc}
     */
    public E getElementAt(int index) {
        return quickList.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object anItem) {
        E input = null;
        
        if (enumClass.isInstance(anItem)) {
            input = (E) anItem;
        } else {
            input = valueMap.get(anItem);
        }
        
        if (input != null || anItem == null) {
            selected = input;
        }
        
        this.fireContentsChanged(this, 0, getSize());
    }
    
    /**
     * {@inheritDoc}
     */
    public E getSelectedItem() {
	return selected;
    }
    
    /*
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));
        
        
        JComboBox combo1 = new JComboBox();
        combo1.setModel(new EnumComboBoxModel(MyEnum1.class));
        frame.add(combo1);
        
        JComboBox combo2 = new JComboBox();
        combo2.setModel(new EnumComboBoxModel(MyEnum2.class));
        frame.add(combo2);
        
        EnumComboBoxModel<MyEnum1> enumModel = new EnumComboBoxModel<MyEnum1>(MyEnum1.class);
        JComboBox combo3 = new JComboBox();
        combo3.setModel(enumModel);
        frame.add(combo3);
        
        MyEnum1 selected = enumModel.getSelectedItem();
        
        //uncomment to see the ClassCastException
//        enumModel.setSelectedItem("Die clown");
        
        frame.pack();
        frame.setVisible(true);
    }
    
    private enum MyEnum1 {GoodStuff, BadStuff};
    private enum MyEnum2 {GoodStuff, BadStuff;
    public String toString() {
        switch(this) {
            case GoodStuff: return "Some Good Stuff";
            case BadStuff: return "Some Bad Stuff";
        }
        return "ERROR";
    }
    };
    */

}