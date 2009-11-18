package jd;

import java.util.HashMap;

public class Tester {
    private static HashMap<Integer, Integer> map;

    public static int countBits(int x) {
        // collapsing partial parallel sums method
        // collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
        x = (x >>> 1 & 0x5555) + (x & 0x5555);
        // collapse 16x2 bit counts to 8x4 bit counts, mask 00110011
        x = (x >>> 2 & 0x3333) + (x & 0x3333);
        // collapse 8x4 bit counts to 4x8 bit counts, mask 00001111
        x = (x >>> 4 & 0x0f0f) + (x & 0x0f0f);
        return x;
        // collapse 4x8 bit counts to 2x16 bit counts
        // x = (x >>> 8 & 0x00ff00ff) + (x & 0x00ff00ff);
        // // collapse 2x16 bit counts to 1x32 bit count
        // return (x >>> 16) + (x & 0x0000ffff);
    }

    public static int getBits(int b) {
        switch (b) {
        case 0:
            return 0;
        case 1:
            return 1;
        case 2:
            return 1;
        case 3:
            return 2;
        case 4:
            return 1;
        case 5:
            return 2;
        case 6:
            return 2;
        case 7:
            return 3;
        case 8:
            return 1;
        case 9:
            return 2;
        case 10:
            return 2;
        case 11:
            return 3;
        case 12:
            return 2;
        case 13:
            return 3;
        case 14:
            return 3;
        case 15:
            return 4;
        case 16:
            return 1;
        case 17:
            return 2;
        case 18:
            return 2;
        case 19:
            return 3;
        case 20:
            return 2;
        case 21:
            return 3;
        case 22:
            return 3;
        case 23:
            return 4;
        case 24:
            return 2;
        case 25:
            return 3;
        case 26:
            return 3;
        case 27:
            return 4;
        case 28:
            return 3;
        case 29:
            return 4;
        case 30:
            return 4;
        case 31:
            return 5;
        case 32:
            return 1;
        case 33:
            return 2;
        case 34:
            return 2;
        case 35:
            return 3;
        case 36:
            return 2;
        case 37:
            return 3;
        case 38:
            return 3;
        case 39:
            return 4;
        case 40:
            return 2;
        case 41:
            return 3;
        case 42:
            return 3;
        case 43:
            return 4;
        case 44:
            return 3;
        case 45:
            return 4;
        case 46:
            return 4;
        case 47:
            return 5;
        case 48:
            return 2;
        case 49:
            return 3;
        case 50:
            return 3;
        case 51:
            return 4;
        case 52:
            return 3;
        case 53:
            return 4;
        case 54:
            return 4;
        case 55:
            return 5;
        case 56:
            return 3;
        case 57:
            return 4;
        case 58:
            return 4;
        case 59:
            return 5;
        case 60:
            return 4;
        case 61:
            return 5;
        case 62:
            return 5;
        case 63:
            return 6;
        case 64:
            return 1;
        case 65:
            return 2;
        case 66:
            return 2;
        case 67:
            return 3;
        case 68:
            return 2;
        case 69:
            return 3;
        case 70:
            return 3;
        case 71:
            return 4;
        case 72:
            return 2;
        case 73:
            return 3;
        case 74:
            return 3;
        case 75:
            return 4;
        case 76:
            return 3;
        case 77:
            return 4;
        case 78:
            return 4;
        case 79:
            return 5;
        case 80:
            return 2;
        case 81:
            return 3;
        case 82:
            return 3;
        case 83:
            return 4;
        case 84:
            return 3;
        case 85:
            return 4;
        case 86:
            return 4;
        case 87:
            return 5;
        case 88:
            return 3;
        case 89:
            return 4;
        case 90:
            return 4;
        case 91:
            return 5;
        case 92:
            return 4;
        case 93:
            return 5;
        case 94:
            return 5;
        case 95:
            return 6;
        case 96:
            return 2;
        case 97:
            return 3;
        case 98:
            return 3;
        case 99:
            return 4;
        case 100:
            return 3;
        case 101:
            return 4;
        case 102:
            return 4;
        case 103:
            return 5;
        case 104:
            return 3;
        case 105:
            return 4;
        case 106:
            return 4;
        case 107:
            return 5;
        case 108:
            return 4;
        case 109:
            return 5;
        case 110:
            return 5;
        case 111:
            return 6;
        case 112:
            return 3;
        case 113:
            return 4;
        case 114:
            return 4;
        case 115:
            return 5;
        case 116:
            return 4;
        case 117:
            return 5;
        case 118:
            return 5;
        case 119:
            return 6;
        case 120:
            return 4;
        case 121:
            return 5;
        case 122:
            return 5;
        case 123:
            return 6;
        case 124:
            return 5;
        case 125:
            return 6;
        case 126:
            return 6;
        case 127:
            return 7;
        case 128:
            return 1;
        case 129:
            return 2;
        case 130:
            return 2;
        case 131:
            return 3;
        case 132:
            return 2;
        case 133:
            return 3;
        case 134:
            return 3;
        case 135:
            return 4;
        case 136:
            return 2;
        case 137:
            return 3;
        case 138:
            return 3;
        case 139:
            return 4;
        case 140:
            return 3;
        case 141:
            return 4;
        case 142:
            return 4;
        case 143:
            return 5;
        case 144:
            return 2;
        case 145:
            return 3;
        case 146:
            return 3;
        case 147:
            return 4;
        case 148:
            return 3;
        case 149:
            return 4;
        case 150:
            return 4;
        case 151:
            return 5;
        case 152:
            return 3;
        case 153:
            return 4;
        case 154:
            return 4;
        case 155:
            return 5;
        case 156:
            return 4;
        case 157:
            return 5;
        case 158:
            return 5;
        case 159:
            return 6;
        case 160:
            return 2;
        case 161:
            return 3;
        case 162:
            return 3;
        case 163:
            return 4;
        case 164:
            return 3;
        case 165:
            return 4;
        case 166:
            return 4;
        case 167:
            return 5;
        case 168:
            return 3;
        case 169:
            return 4;
        case 170:
            return 4;
        case 171:
            return 5;
        case 172:
            return 4;
        case 173:
            return 5;
        case 174:
            return 5;
        case 175:
            return 6;
        case 176:
            return 3;
        case 177:
            return 4;
        case 178:
            return 4;
        case 179:
            return 5;
        case 180:
            return 4;
        case 181:
            return 5;
        case 182:
            return 5;
        case 183:
            return 6;
        case 184:
            return 4;
        case 185:
            return 5;
        case 186:
            return 5;
        case 187:
            return 6;
        case 188:
            return 5;
        case 189:
            return 6;
        case 190:
            return 6;
        case 191:
            return 7;
        case 192:
            return 2;
        case 193:
            return 3;
        case 194:
            return 3;
        case 195:
            return 4;
        case 196:
            return 3;
        case 197:
            return 4;
        case 198:
            return 4;
        case 199:
            return 5;
        case 200:
            return 3;
        case 201:
            return 4;
        case 202:
            return 4;
        case 203:
            return 5;
        case 204:
            return 4;
        case 205:
            return 5;
        case 206:
            return 5;
        case 207:
            return 6;
        case 208:
            return 3;
        case 209:
            return 4;
        case 210:
            return 4;
        case 211:
            return 5;
        case 212:
            return 4;
        case 213:
            return 5;
        case 214:
            return 5;
        case 215:
            return 6;
        case 216:
            return 4;
        case 217:
            return 5;
        case 218:
            return 5;
        case 219:
            return 6;
        case 220:
            return 5;
        case 221:
            return 6;
        case 222:
            return 6;
        case 223:
            return 7;
        case 224:
            return 3;
        case 225:
            return 4;
        case 226:
            return 4;
        case 227:
            return 5;
        case 228:
            return 4;
        case 229:
            return 5;
        case 230:
            return 5;
        case 231:
            return 6;
        case 232:
            return 4;
        case 233:
            return 5;
        case 234:
            return 5;
        case 235:
            return 6;
        case 236:
            return 5;
        case 237:
            return 6;
        case 238:
            return 6;
        case 239:
            return 7;
        case 240:
            return 4;
        case 241:
            return 5;
        case 242:
            return 5;
        case 243:
            return 6;
        case 244:
            return 5;
        case 245:
            return 6;
        case 246:
            return 6;
        case 247:
            return 7;
        case 248:
            return 5;
        case 249:
            return 6;
        case 250:
            return 6;
        case 251:
            return 7;
        case 252:
            return 6;
        case 253:
            return 7;
        case 254:
            return 7;
        case 255:
            return 8;
        }
        return -1;

    }

    public static void main(String ss[]) throws Exception {
        map= new HashMap<Integer,Integer>();
        map.put(0,0);
        map.put(1,1);
        map.put(2,1);
        map.put(3,2);
        map.put(4,1);
        map.put(5,2);
        map.put(6,2);
        map.put(7,3);
        map.put(8,1);
        map.put(9,2);
        map.put(10,2);
        map.put(11,3);
        map.put(12,2);
        map.put(13,3);
        map.put(14,3);
        map.put(15,4);
        map.put(16,1);
        map.put(17,2);
        map.put(18,2);
        map.put(19,3);
        map.put(20,2);
        map.put(21,3);
        map.put(22,3);
        map.put(23,4);
        map.put(24,2);
        map.put(25,3);
        map.put(26,3);
        map.put(27,4);
        map.put(28,3);
        map.put(29,4);
        map.put(30,4);
        map.put(31,5);
        map.put(32,1);
        map.put(33,2);
        map.put(34,2);
        map.put(35,3);
        map.put(36,2);
        map.put(37,3);
        map.put(38,3);
        map.put(39,4);
        map.put(40,2);
        map.put(41,3);
        map.put(42,3);
        map.put(43,4);
        map.put(44,3);
        map.put(45,4);
        map.put(46,4);
        map.put(47,5);
        map.put(48,2);
        map.put(49,3);
        map.put(50,3);
        map.put(51,4);
        map.put(52,3);
        map.put(53,4);
        map.put(54,4);
        map.put(55,5);
        map.put(56,3);
        map.put(57,4);
        map.put(58,4);
        map.put(59,5);
        map.put(60,4);
        map.put(61,5);
        map.put(62,5);
        map.put(63,6);
        map.put(64,1);
        map.put(65,2);
        map.put(66,2);
        map.put(67,3);
        map.put(68,2);
        map.put(69,3);
        map.put(70,3);
        map.put(71,4);
        map.put(72,2);
        map.put(73,3);
        map.put(74,3);
        map.put(75,4);
        map.put(76,3);
        map.put(77,4);
        map.put(78,4);
        map.put(79,5);
        map.put(80,2);
        map.put(81,3);
        map.put(82,3);
        map.put(83,4);
        map.put(84,3);
        map.put(85,4);
        map.put(86,4);
        map.put(87,5);
        map.put(88,3);
        map.put(89,4);
        map.put(90,4);
        map.put(91,5);
        map.put(92,4);
        map.put(93,5);
        map.put(94,5);
        map.put(95,6);
        map.put(96,2);
        map.put(97,3);
        map.put(98,3);
        map.put(99,4);
        map.put(100,3);
        map.put(101,4);
        map.put(102,4);
        map.put(103,5);
        map.put(104,3);
        map.put(105,4);
        map.put(106,4);
        map.put(107,5);
        map.put(108,4);
        map.put(109,5);
        map.put(110,5);
        map.put(111,6);
        map.put(112,3);
        map.put(113,4);
        map.put(114,4);
        map.put(115,5);
        map.put(116,4);
        map.put(117,5);
        map.put(118,5);
        map.put(119,6);
        map.put(120,4);
        map.put(121,5);
        map.put(122,5);
        map.put(123,6);
        map.put(124,5);
        map.put(125,6);
        map.put(126,6);
        map.put(127,7);
        map.put(128,1);
        map.put(129,2);
        map.put(130,2);
        map.put(131,3);
        map.put(132,2);
        map.put(133,3);
        map.put(134,3);
        map.put(135,4);
        map.put(136,2);
        map.put(137,3);
        map.put(138,3);
        map.put(139,4);
        map.put(140,3);
        map.put(141,4);
        map.put(142,4);
        map.put(143,5);
        map.put(144,2);
        map.put(145,3);
        map.put(146,3);
        map.put(147,4);
        map.put(148,3);
        map.put(149,4);
        map.put(150,4);
        map.put(151,5);
        map.put(152,3);
        map.put(153,4);
        map.put(154,4);
        map.put(155,5);
        map.put(156,4);
        map.put(157,5);
        map.put(158,5);
        map.put(159,6);
        map.put(160,2);
        map.put(161,3);
        map.put(162,3);
        map.put(163,4);
        map.put(164,3);
        map.put(165,4);
        map.put(166,4);
        map.put(167,5);
        map.put(168,3);
        map.put(169,4);
        map.put(170,4);
        map.put(171,5);
        map.put(172,4);
        map.put(173,5);
        map.put(174,5);
        map.put(175,6);
        map.put(176,3);
        map.put(177,4);
        map.put(178,4);
        map.put(179,5);
        map.put(180,4);
        map.put(181,5);
        map.put(182,5);
        map.put(183,6);
        map.put(184,4);
        map.put(185,5);
        map.put(186,5);
        map.put(187,6);
        map.put(188,5);
        map.put(189,6);
        map.put(190,6);
        map.put(191,7);
        map.put(192,2);
        map.put(193,3);
        map.put(194,3);
        map.put(195,4);
        map.put(196,3);
        map.put(197,4);
        map.put(198,4);
        map.put(199,5);
        map.put(200,3);
        map.put(201,4);
        map.put(202,4);
        map.put(203,5);
        map.put(204,4);
        map.put(205,5);
        map.put(206,5);
        map.put(207,6);
        map.put(208,3);
        map.put(209,4);
        map.put(210,4);
        map.put(211,5);
        map.put(212,4);
        map.put(213,5);
        map.put(214,5);
        map.put(215,6);
        map.put(216,4);
        map.put(217,5);
        map.put(218,5);
        map.put(219,6);
        map.put(220,5);
        map.put(221,6);
        map.put(222,6);
        map.put(223,7);
        map.put(224,3);
        map.put(225,4);
        map.put(226,4);
        map.put(227,5);
        map.put(228,4);
        map.put(229,5);
        map.put(230,5);
        map.put(231,6);
        map.put(232,4);
        map.put(233,5);
        map.put(234,5);
        map.put(235,6);
       // map.put(236,5);
        map.put(237,6);
        map.put(238,6);
        map.put(239,7);
        map.put(240,4);
        map.put(241,5);
        map.put(242,5);
        map.put(243,6);
        map.put(244,5);
        map.put(245,6);
        map.put(246,6);
        map.put(247,7);
        map.put(248,5);
        map.put(249,6);
        map.put(250,6);
        map.put(251,7);
        map.put(252,6);
        map.put(253,7);
        map.put(254,7);
        map.put(255,8);
        
        //erstele testwerte
        int[] values = new int[256];        
        for(int i=0; i<256;i++)values[i]=(int)(Math.random()*256);
        long time = System.currentTimeMillis();
        for (long i = 0; i < 100000000; i++) {
         
            countBits(values[(int)i%255]);
        }
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        for (long i = 0; i < 100000000; i++) {
           getBits(values[(int)i%255]);    
        }
        System.out.println(System.currentTimeMillis() - time);
        
        
        time = System.currentTimeMillis();
        for (long i = 0; i < 100000000; i++) {
           map.get(values[(int)i%255]);            
        }
        System.out.println(System.currentTimeMillis() - time);

    }

}