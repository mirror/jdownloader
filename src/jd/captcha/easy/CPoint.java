package jd.captcha.easy;

import java.awt.Point;
import java.io.Serializable;

import jd.captcha.pixelgrid.Captcha;

public class CPoint extends Point implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 333616481245029882L;
	private int color, distance;
	private boolean foreground = true;

	public boolean isForeground() {
		return foreground;
	}

	public void setForeground(boolean foreground) {
		this.foreground = foreground;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}
	public CPoint() {
	}
	public CPoint(int x, int y, int distance, Captcha captcha) {
		this(x, y, distance, captcha.getPixelValue(x, y));
	}

	public CPoint(int x, int y, int distance, int color) {
		super(x, y);
		this.color = color;
		this.distance = distance;
	}
	@Override
	public Object clone() {
		return new CPoint(x,y,distance,color);
	}
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj) || ((CPoint) obj).color == color;
	}
}
