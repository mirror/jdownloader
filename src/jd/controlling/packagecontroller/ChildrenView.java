package jd.controlling.packagecontroller;

import java.util.ArrayList;

import org.jdownloader.DomainInfo;

public abstract class ChildrenView<T> extends ArrayList<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5324936310983343571L;

	/**
	 * Update the whole view.
	 */
	public void update() {
	}

	public void replace(ArrayList<T> items) {
		this.clear();
		this.addAll(items);
	}

	abstract public DomainInfo[] getDomainInfos();

}
