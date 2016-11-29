function getOffsetSum(elem) {
	var top = 0, left = 0
	while (elem) {
		top = top + parseInt(elem.offsetTop)
		left = left + parseInt(elem.offsetLeft)
		elem = elem.offsetParent
	}

	return {
		top : top,
		left : left
	}
}

function getOffsetRect(elem) {
	var box = elem.getBoundingClientRect()

	var body = document.body
	var docElem = document.documentElement

	var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop
	var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft

	var clientTop = docElem.clientTop || body.clientTop || 0
	var clientLeft = docElem.clientLeft || body.clientLeft || 0

	var top = box.top + scrollTop - clientTop
	var left = box.left + scrollLeft - clientLeft

	return {
		top : Math.round(top),
		left : Math.round(left)
	}
}

function getOffset(elem) {
	if (elem.getBoundingClientRect) {
		return getOffsetRect(elem)
	} else {
		return getOffsetSum(elem)
	}
}

function init(elem) {

	window.addEventListener("beforeunload", function(e) {
		// close dialog when user closed browser
		unload();
		// https://svn.jdownloader.org/issues/78145
		// http://stackoverflow.com/questions/11793996/onbeforeunload-handler-says-null-in-ie
		if (getInternetExplorerVersion() < 11) {
			return null;
			; // Webkit, Safari, Chrome
		}
	});

	var bounds = null;
	if (elem != null) {
		bounds = elem.getBoundingClientRect();
	}

	var xmlHttp = null;

	xmlHttp = new XMLHttpRequest();
	var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0)
	var h = Math.max(document.documentElement.clientHeight, window.innerHeight || 0)
	/*
	 * If the browser does not support screenX and screen Y, use screenLeft and
	 * screenTop instead (and vice versa)
	 */
	var winLeft = window.screenLeft ? window.screenLeft : window.screenX;
	var winTop = window.screenTop ? window.screenTop : window.screenY;
	var windowWidth = window.outerWidth;
	var windowHeight = window.outerHeight;
	var ie = getInternetExplorerVersion();
	// alert(ie);
	if (ie > 0) {
		if (ie >= 10) {
			// bug in ie 10 and 11
			var zoom = screen.deviceXDPI / screen.logicalXDPI;
			winLeft *= zoom;
			winTop *= zoom;
			windowWidth *= zoom;
			windowHeight *= zoom;

			// alert(zoom);
		}

	}

	if (bounds) {
		xmlHttp.open("GET", window.location.href + "&do=loaded&x=" + winLeft + "&y=" + winTop + "&w=" + windowWidth + "&h=" + windowHeight + "&vw=" + w + "&vh=" + h + "&eleft=" + bounds.left + "&etop=" + bounds.top + "&ew=" + bounds.width + "&eh=" + bounds.height, true);

	} else {
		xmlHttp.open("GET", window.location.href + "&do=loaded&x=" + winLeft + "&y=" + winTop + "&w=" + windowWidth + "&h=" + windowHeight + "&vw=" + w + "&vh=" + h, true);

	}
	xmlHttp.send();

}
function getInternetExplorerVersion() {
	var rv = -1;
	if (navigator.appName == 'Microsoft Internet Explorer') {
		var ua = navigator.userAgent;
		var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
		if (re.exec(ua) != null) rv = parseFloat(RegExp.$1);
	} else if (navigator.appName == 'Netscape') {
		var ua = navigator.userAgent;
		var re = new RegExp("Trident/.*rv:([0-9]{1,}[\.0-9]{0,})");
		if (re.exec(ua) != null) rv = parseFloat(RegExp.$1);
	}
	return rv;
}
function unload() {
	try {
		var xhr = new XMLHttpRequest();

		xhr.open("GET", window.location.href + "&do=unload", true);
		xhr.timeout = 5000;
		xhr.send();

	} catch (err) {

		return;
	}
}
function closeWindowOrTab() {
if(window.location.hash)return;
	console.log("Close browser");
	if (/Edge\/\d+./i.test(navigator.userAgent)) {
		open(location, '_self').close();
		return;
	}
	var ie = getInternetExplorerVersion();

	if (ie > 7) {
		window.open('', '_self', '');
		window.close();
		return;
	} else if (ie == 7) {
		// This method is required to close a window without any prompt for
		// IE7 & greater versions.
		window.open('', '_parent', '');
		window.close();
		return;
	} else if (ie > 0) {
		// This method is required to close a window without any prompt for
		// IE6
		this.focus();
		self.opener = this;
		self.close();
		return;
	}

	// For NON-IE Browsers except Firefox which doesnt support Auto Close
	try {
		this.focus();
		self.opener = this;
		self.close();
	} catch (e) {

	}

	try {
		window.open('', '_self', '');
		window.close();
	} catch (e) {

	}
	

}
/*
 * Close the tab if the captcha is solved or canceled
 */
function refresh() {

	try {
		var xhr = new XMLHttpRequest();

		xhr.onTimeout = function() {
			closeWindowOrTab();

		}

		xhr.onerror = xhr.onTimeout;

		xhr.onLoad = function() {
			if (xhr.status == 0) {
				closeWindowOrTab();
			} else if (xhr.responseText == "true") {
				closeWindowOrTab();

				return;
			} else {
				setTimeout(refresh, 1000);
			}
		}

		xhr.onreadystatechange = function() {
			if (xhr.readyState == 4) {
				xhr.onLoad();
			}
		};

		xhr.open("GET", window.location.href + "&do=canClose", true);
		// set timeout AFTER .open else we would get an invalid state exception
		// in ie 11 (any maybe even 10)
		xhr.timeout = 5000;
		xhr.send();

	} catch (err) {
		closeWindowOrTab();
		return;
	}

}
setTimeout(refresh, 1000);
