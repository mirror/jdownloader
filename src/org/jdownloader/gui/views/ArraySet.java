package org.jdownloader.gui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class ArraySet<T> extends ArrayList<T> {
    private HashSet<T> set;

    public ArraySet(Collection<? extends T> rawSelection) {
        this();
        addAll(rawSelection);
    }

    public ArraySet() {
        set = new HashSet<T>();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public boolean add(T e) {
        if (set.add(e)) {
            //
            return super.add(e);
        }
        return false;
    }

    @Override
    public void add(int index, T element) {
        if (set.add(element)) {
            super.add(index, element);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modded = false;
        for (T t : c) {
            if (add(t)) {

                modded = true;
            }
        }
        return modded;

    }

}