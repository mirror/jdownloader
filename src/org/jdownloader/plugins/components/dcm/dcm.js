function hex_sha1(s){return binb2hex(core_sha1(str2binb(s),s.length * chrsz));}

/*
 * Convert an 8-bit or 16-bit string to an array of big-endian words
 * In 8-bit function, characters >255 have their hi-byte silently ignored.
 */
function str2binb(str)
{
	var bin = Array();

	for (var i = 0, n = 1 + ((str.length * chrsz) >> 5); i < n; i++)
		bin[i] = 0;

	var mask = (1 << chrsz) - 1;
	for (var i = 0; i < str.length * chrsz; i += chrsz)
		bin[i >> 5] |= (str.charCodeAt(i / chrsz) & mask) << (24 - i % 32);
	return bin;
}

/*
 * Calculate the SHA-1 of an array of big-endian words, and a bit length
 */
function core_sha1(x, len)
{
	/* append padding */
	x[len >> 5] |= 0x80 << (24 - len % 32);
	x[((len + 64 >> 9) << 4) + 15] = len;

	var w = Array(80);
	var a =  1732584193;
	var b = -271733879;
	var c = -1732584194;
	var d =  271733878;
	var e = -1009589776;

	for (var i = 0; i < x.length; i += 16)
	{
		var olda = a;
		var oldb = b;
		var oldc = c;
		var oldd = d;
		var olde = e;

		for (var j = 0; j < 80; j++)
		{
			if (j < 16) w[j] = x[i + j];
			else w[j] = rol(w[j-3] ^ w[j-8] ^ w[j-14] ^ w[j-16], 1);
			var t = safe_add(safe_add(rol(a, 5), sha1_ft(j, b, c, d)), safe_add(safe_add(e, w[j]), sha1_kt(j)));
			e = d;
			d = c;
			c = rol(b, 30);
			b = a;
			a = t;
		}

		a = safe_add(a, olda);
		b = safe_add(b, oldb);
		c = safe_add(c, oldc);
		d = safe_add(d, oldd);
		e = safe_add(e, olde);
	}
	return Array(a, b, c, d, e);
}

/*
 * Convert an array of big-endian words to a hex string.
 */
function binb2hex(binarray)
{
	var hex_tab = hexcase ? "0123456789ABCDEF" : "0123456789abcdef";
	var str = "";
	for (var i = 0; i < binarray.length * 4; i++)
	{
		str += hex_tab.charAt((binarray[i>>2] >> ((3 - i%4)*8+4)) & 0xF) +
			   hex_tab.charAt((binarray[i>>2] >> ((3 - i%4)*8  )) & 0xF);
	}
	return str;
}

function hashLoginPassword(doForm, cur_session_id)
{
	// Compatibility.
	if (cur_session_id == null)
		cur_session_id = smf_session_id;

	if (typeof(hex_sha1) == 'undefined')
		return;
	// Are they using an email address?
	if (doForm.user.value.indexOf('@') != -1)
		return;

	// Unless the browser is Opera, the password will not save properly.
	if (!('opera' in window))
		doForm.passwrd.autocomplete = 'off';

	doForm.hash_passwrd.value = hex_sha1(hex_sha1(doForm.user.value.php_to8bit().php_strtolower() + doForm.passwrd.value.php_to8bit()) + cur_session_id);

	// It looks nicer to fill it with asterisks, but Firefox will try to save that.
	if (is_ff != -1)
		doForm.passwrd.value = '';
	else
		doForm.passwrd.value = doForm.passwrd.value.replace(/./g, '*');
}

// A property we'll be needing for php_to8bit.
String.prototype.oCharsetConversion = {
	from: '',
	to: ''
};

// Convert a string to an 8 bit representation (like in PHP).
String.prototype.php_to8bit = function ()
{
	if (smf_charset == 'UTF-8')
	{
		var n, sReturn = '';

		for (var i = 0, iTextLen = this.length; i < iTextLen; i++)
		{
			n = this.charCodeAt(i);
			if (n < 128)
				sReturn += String.fromCharCode(n)
			else if (n < 2048)
				sReturn += String.fromCharCode(192 | n >> 6) + String.fromCharCode(128 | n & 63);
			else if (n < 65536)
				sReturn += String.fromCharCode(224 | n >> 12) + String.fromCharCode(128 | n >> 6 & 63) + String.fromCharCode(128 | n & 63);
			else
				sReturn += String.fromCharCode(240 | n >> 18) + String.fromCharCode(128 | n >> 12 & 63) + String.fromCharCode(128 | n >> 6 & 63) + String.fromCharCode(128 | n & 63);
		}

		return sReturn;
	}

	else if (this.oCharsetConversion.from.length == 0)
	{
		switch (smf_charset)
		{
			case 'ISO-8859-1':
				this.oCharsetConversion = {
					from: '\xa0-\xff',
					to: '\xa0-\xff'
				};
			break;

			case 'ISO-8859-2':
				this.oCharsetConversion = {
					from: '\xa0\u0104\u02d8\u0141\xa4\u013d\u015a\xa7\xa8\u0160\u015e\u0164\u0179\xad\u017d\u017b\xb0\u0105\u02db\u0142\xb4\u013e\u015b\u02c7\xb8\u0161\u015f\u0165\u017a\u02dd\u017e\u017c\u0154\xc1\xc2\u0102\xc4\u0139\u0106\xc7\u010c\xc9\u0118\xcb\u011a\xcd\xce\u010e\u0110\u0143\u0147\xd3\xd4\u0150\xd6\xd7\u0158\u016e\xda\u0170\xdc\xdd\u0162\xdf\u0155\xe1\xe2\u0103\xe4\u013a\u0107\xe7\u010d\xe9\u0119\xeb\u011b\xed\xee\u010f\u0111\u0144\u0148\xf3\xf4\u0151\xf6\xf7\u0159\u016f\xfa\u0171\xfc\xfd\u0163\u02d9',
					to: '\xa0-\xff'
				};
			break;

			case 'ISO-8859-5':
				this.oCharsetConversion = {
					from: '\xa0\u0401-\u040c\xad\u040e-\u044f\u2116\u0451-\u045c\xa7\u045e\u045f',
					to: '\xa0-\xff'
				};
			break;

			case 'ISO-8859-9':
				this.oCharsetConversion = {
					from: '\xa0-\xcf\u011e\xd1-\xdc\u0130\u015e\xdf-\xef\u011f\xf1-\xfc\u0131\u015f\xff',
					to: '\xa0-\xff'
				};
			break;

			case 'ISO-8859-15':
				this.oCharsetConversion = {
					from: '\xa0-\xa3\u20ac\xa5\u0160\xa7\u0161\xa9-\xb3\u017d\xb5-\xb7\u017e\xb9-\xbb\u0152\u0153\u0178\xbf-\xff',
					to: '\xa0-\xff'
				};
			break;

			case 'tis-620':
				this.oCharsetConversion = {
					from: '\u20ac\u2026\u2018\u2019\u201c\u201d\u2022\u2013\u2014\xa0\u0e01-\u0e3a\u0e3f-\u0e5b',
					to: '\x80\x85\x91-\x97\xa0-\xda\xdf-\xfb'
				};
			break;

			case 'windows-1251':
				this.oCharsetConversion = {
					from: '\u0402\u0403\u201a\u0453\u201e\u2026\u2020\u2021\u20ac\u2030\u0409\u2039\u040a\u040c\u040b\u040f\u0452\u2018\u2019\u201c\u201d\u2022\u2013\u2014\u2122\u0459\u203a\u045a\u045c\u045b\u045f\xa0\u040e\u045e\u0408\xa4\u0490\xa6\xa7\u0401\xa9\u0404\xab-\xae\u0407\xb0\xb1\u0406\u0456\u0491\xb5-\xb7\u0451\u2116\u0454\xbb\u0458\u0405\u0455\u0457\u0410-\u044f',
					to: '\x80-\x97\x99-\xff'
				};
			break;

			case 'windows-1253':
				this.oCharsetConversion = {
					from: '\u20ac\u201a\u0192\u201e\u2026\u2020\u2021\u2030\u2039\u2018\u2019\u201c\u201d\u2022\u2013\u2014\u2122\u203a\xa0\u0385\u0386\xa3-\xa9\xab-\xae\u2015\xb0-\xb3\u0384\xb5-\xb7\u0388-\u038a\xbb\u038c\xbd\u038e-\u03a1\u03a3-\u03ce',
					to: '\x80\x82-\x87\x89\x8b\x91-\x97\x99\x9b\xa0-\xa9\xab-\xd1\xd3-\xfe'
				};
			break;

			case 'windows-1255':
				this.oCharsetConversion = {
					from: '\u20ac\u201a\u0192\u201e\u2026\u2020\u2021\u02c6\u2030\u2039\u2018\u2019\u201c\u201d\u2022\u2013\u2014\u02dc\u2122\u203a\xa0-\xa3\u20aa\xa5-\xa9\xd7\xab-\xb9\xf7\xbb-\xbf\u05b0-\u05b9\u05bb-\u05c3\u05f0-\u05f4\u05d0-\u05ea\u200e\u200f',
					to: '\x80\x82-\x89\x8b\x91-\x99\x9b\xa0-\xc9\xcb-\xd8\xe0-\xfa\xfd\xfe'
				};
			break;

			case 'windows-1256':
				this.oCharsetConversion = {
					from: '\u20ac\u067e\u201a\u0192\u201e\u2026\u2020\u2021\u02c6\u2030\u0679\u2039\u0152\u0686\u0698\u0688\u06af\u2018\u2019\u201c\u201d\u2022\u2013\u2014\u06a9\u2122\u0691\u203a\u0153\u200c\u200d\u06ba\xa0\u060c\xa2-\xa9\u06be\xab-\xb9\u061b\xbb-\xbe\u061f\u06c1\u0621-\u0636\xd7\u0637-\u063a\u0640-\u0643\xe0\u0644\xe2\u0645-\u0648\xe7-\xeb\u0649\u064a\xee\xef\u064b-\u064e\xf4\u064f\u0650\xf7\u0651\xf9\u0652\xfb\xfc\u200e\u200f\u06d2',
					to: '\x80-\xff'
				};
			break;

			default:
				this.oCharsetConversion = {
					from: '',
					to: ''
				};
			break;
		}
		var funcExpandString = function (sSearch) {
			var sInsert = '';
			for (var i = sSearch.charCodeAt(0), n = sSearch.charCodeAt(2); i <= n; i++)
				sInsert += String.fromCharCode(i);
			return sInsert;
		};
		this.oCharsetConversion.from = this.oCharsetConversion.from.replace(/.\-./g, funcExpandString);
		this.oCharsetConversion.to = this.oCharsetConversion.to.replace(/.\-./g, funcExpandString);
	}

	var sReturn = '', iOffsetFrom = 0;
	for (var i = 0, n = this.length; i < n; i++)
	{
		iOffsetFrom = this.oCharsetConversion.from.indexOf(this.charAt(i));
		sReturn += iOffsetFrom > -1 ? this.oCharsetConversion.to.charAt(iOffsetFrom) : (this.charCodeAt(i) > 127 ? '&#' + this.charCodeAt(i) + ';' : this.charAt(i));
	}

	return sReturn
}

// Character-level replacement function.
String.prototype.php_strtr = function (sFrom, sTo)
{
	return this.replace(new RegExp('[' + sFrom + ']', 'g'), function (sMatch) {
		return sTo.charAt(sFrom.indexOf(sMatch));
	});
}

// Simulate PHP's strtolower (in SOME cases PHP uses ISO-8859-1 case folding).
String.prototype.php_strtolower = function ()
{
	return typeof(smf_iso_case_folding) == 'boolean' && smf_iso_case_folding == true ? this.php_strtr(
		'ABCDEFGHIJKLMNOPQRSTUVWXYZ\x8a\x8c\x8e\x9f\xc0\xc1\xc2\xc3\xc4\xc5\xc6\xc7\xc8\xc9\xca\xcb\xcc\xcd\xce\xcf\xd0\xd1\xd2\xd3\xd4\xd5\xd6\xd7\xd8\xd9\xda\xdb\xdc\xdd\xde',
		'abcdefghijklmnopqrstuvwxyz\x9a\x9c\x9e\xff\xe0\xe1\xe2\xe3\xe4\xe5\xe6\xe7\xe8\xe9\xea\xeb\xec\xed\xee\xef\xf0\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xfb\xfc\xfd\xfe'
	) : this.php_strtr('ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz');
}