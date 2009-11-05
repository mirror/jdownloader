package jd;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.util.BitSet;

public class Tester2 {
	public static boolean[] fromByte(byte b) {
		boolean[] bits = new boolean[8];
		for (int i = 0; i < 8; i++) {
			bits[i] = (b & 1) == 1;
			b >>= 1;
		}
		return bits;
	}

	public static byte countBits3(byte x) {
		byte count = 0;
		while (x != 0) {
			x &= x - 1;
			count++;
		}
		return count;
	}

	public static byte countBits2(byte x) {
		byte result = 0;
		for (int i = 0; i < 8; i++) {
			result += x & 1;
			x >>>= 1;
		}
		return result;
	}

	public static byte countBitsByte(byte x) {
		// collapsing partial parallel sums method
		// collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
		byte b = x;
		x >>>= 1;
		x &= 0x55;
		x += b & 0x55;
		b = x;
		x >>>= 2;
		x &= 0x33;
		x += b & 0x33;
		b = x;
		x >>>= 4;
		x &= 0x0f;
		x += b & 0x0f;
		b = x;
		x >>>= 8;
		x &= 0x00ff;
		x += b & 0x00ff;
		return x;
	}

	public static int countBits(int x) {
		// collapsing partial parallel sums method
		// collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
		x = (x >>> 1 & 0x55555555) + (x & 0x55555555);
		// collapse 16x2 bit counts to 8x4 bit counts, mask 00110011
		x = (x >>> 2 & 0x33333333) + (x & 0x33333333);
		// collapse 8x4 bit counts to 4x8 bit counts, mask 00001111
		x = (x >>> 4 & 0x0f0f0f0f) + (x & 0x0f0f0f0f);
		// collapse 4x8 bit counts to 2x16 bit counts
		x = (x >>> 8 & 0x00ff00ff) + (x & 0x00ff00ff);
		// collapse 2x16 bit counts to 1x32 bit count
		return (x >>> 16) + (x & 0x0000ffff);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int bc = -1;
		int b2 = 7;
		long wh = 200000000l;
		long time = System.currentTimeMillis();
		for (long i = 0; i < wh; i++) {
			bc ^= b2;
			countBits(bc);
			// byte c = 0;
			// for (int j = 0; j < bo1.length; j++) {
			// if(bo1[j]==bo2[j])c++;
			// }
		}

		System.out.println(System.currentTimeMillis() - time);
		boolean[] bo1 = new boolean[] { false, true, false, true, false, true,
				false, true, false, true, false, true, false, true, false,
				true, false, true, false, true, false, true, false, true,
				false, true, false, true, false, true, false, true };
		boolean[] bo2 = new boolean[] { false, true, false, true, false, true,
				false, true, false, true, false, true, false, true, false,
				true, false, true, false, true, false, true, false, true,
				false, true, false, true, false, true, false, true };

		time = System.currentTimeMillis();
		for (long i = 0; i < wh; i++) {

			byte c = 0;
			for (int j = 0; j < bo1.length; j++) {
				if (bo1[j] == bo2[j])
					c++;
			}
		}

		System.out.println(System.currentTimeMillis() - time);
	}

}
