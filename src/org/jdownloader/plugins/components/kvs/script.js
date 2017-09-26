function result(b){
 	var c = "",
 	d = 0;
 	b = b.replace(/[^\u0410\u0412\u0421\u0415\u041cA-Za-z0-9\.\,\~]/g, "");
 	do {
 		var f = "\u0410\u0412\u0421D\u0415FGHIJKL\u041cNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,~".indexOf(b.charAt(d++)),
 		e = "\u0410\u0412\u0421D\u0415FGHIJKL\u041cNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,~".indexOf(b.charAt(d++)),
 		g = "\u0410\u0412\u0421D\u0415FGHIJKL\u041cNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,~".indexOf(b.charAt(d++)),
 		h = "\u0410\u0412\u0421D\u0415FGHIJKL\u041cNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,~".indexOf(b.charAt(d++)),
 		f = f << 2 | e >> 4,
 		e = (e & 15) << 4 | g >> 2,
 		k = (g & 3) << 6 | h,
 		c = c + String.fromCharCode(f);
 		64 != g && (c += String.fromCharCode(e));
 		64 != h && (c += String.fromCharCode(k))
 	} while (d < b.length);
 	return unescape(c)
};