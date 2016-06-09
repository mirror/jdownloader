var localPort = null/* %%%localPort%%% */;
var localID = null/* %%%localID%%% */;
var debuggerEnabled = null/* %%%debugger%%% */;
var phantomPort = null/* %%%phantomPort%%% */;
var accessToken = null/* %%%accessToken%%% */;

// kill Phantomjs if jd is not reachable
console.log("Start PhantomJS port:" + localPort + " ID:" + localID);
setInterval(function() {
	if (new Date().getTime() - lastPing > 10000) {
		console.log("JD Ping Missing. Exit!");
		phantom.exit(1);
	}

}, 5000);

var _global = this;

var webserver = require('webserver');
var server = webserver.create();
var lastPing = new Date().getTime();
var httpListener = function(request, response) {
	if (request.method != "POST" || request.post.accessToken != accessToken) {
		// received a request that is not a post request, or did not contain the
		// accesstoken
		console.log(JSON.stringify(request));
		console.log("Forbidden Access.EXIT");
		response.statusCode = 511;
		response.write('FORBIDDEN');
		response.close();
		phantom.exit(1);

	}
	console.log(JSON.stringify(request));
	if ("/screenshot" == request.url) {
	
		try {
		
			(function() {
				var base64 = page.renderBase64('PNG');
				endJob(request.post.jobID, base64);
			})();
		} catch (err) {
			console.log(err);
			response.statusCode = 511;
			response.write('Error ' + JSON.stringify(err));
			response.close();
			return;
		}
		response.statusCode = 200;
		response.write('\r\nOK');
		response.close();
		return;
	} else if ("/exec" == request.url) {
		js = request.post.js;
		try {
			console.log("Eval " + js);
			eval(js);
		} catch (err) {
			console.log(err);
			response.statusCode = 511;
			response.write('Error ' + JSON.stringify(err));
			response.close();
			return;
		}
		response.statusCode = 200;
		response.write('\r\nOK');
		response.close();
		return;
	} else if ("/ping" == request.url) {
		lastPing = new Date().getTime();
		response.statusCode = 200;
		response.write('\r\nOK');
		response.close();
		return;
	}
	response.statusCode = 400;
	response.write('\r\nBAD REQUEST');
	response.close();
};
var service = server.listen('127.0.0.1:' + phantomPort, httpListener);

var endJob = function(jobID, result) {
	console.log(">>>RESULT:" + jobID + ":" + JSON.stringify(result) + "<<<");
};
endJob(-1, service);
console.log("Server " + service + " port " + phantomPort);
var page = require('webpage').create();
var system = require('system');
page.settings.userAgent = 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36';
var requestID = 0;
page.onResourceError = function(resourceError) {
	console.log('Unable to load resource (#' + resourceError.id + 'URL:' + resourceError.url + ')');
	console.log('Error code: ' + resourceError.errorCode + '. Description: ' + resourceError.errorString);
};
page.onResourceReceived = function(response) {
	console.log('Response (#' + response.id + ', stage "' + response.stage + '"): ' + JSON.stringify(response));
};

page.onInitialized = function() {
	// Detect UA Sniffing
	console.log("Page Init");

	// Spoof Plugins:
	page.evaluate(function() {
		var oldNavigator = navigator;
		var oldPlugins = oldNavigator.plugins;
		var plugins = {};
		plugins.length = 1;
		plugins.__proto__ = oldPlugins.__proto__;

		window.navigator = {
			plugins : plugins
		};
		window.navigator.__proto__ = oldNavigator.__proto__;
	});

	// callPhantom

	page.evaluate(function() {
		delete window._phantom;
		delete window.callPhantom;

	});

	page.evaluate(function() {
		console.log("Try to access local file");
		var xhr = new XMLHttpRequest();
		xhr.open('GET', "file:///e:/text.txt", false);
		xhr.onload = function() {
			console.log("XHR FILE RESPONSE: " + xhr.responseText);
			console.log("Exit. PJS Page Context has filesys access!");
			phantom.exit(1);
		};
		xhr.onerror = function(e) {
			console.log('Error: ' + JSON.stringify(e));
		};
		xhr.send();
	});
	page.evaluate(function() {
		console.log("Detect PJS START ");

		if (window['callPhantom']) {
			console.log("PhantomJS environment detected. window['callPhantom'] " + window['callPhantom']);
		}
		if (window.callPhantom) {
			console.log("PhantomJS environment detected. window.callPhantom " + window.callPhantom);
		}
		if (window._phantom) {
			console.log("PhantomJS environment detected.  window._phantom " + window._phantom);
		}

		if (window.__phantomas) {
			console.log("PhantomJS environment detected. window.__phantomas " + window.__phantomas);
		}

		if (window.Buffer) {
			console.log("PhantomJS environment detected. window.Buffer " + window.Buffer);
		}

		if (window.emit) {
			console.log("PhantomJS environment detected. window.emit  " + window.emit);
		}
		if (window.spawn) {
			console.log("PhantomJS environment detected. window.spawn  " + window.spawn);
		}

		if (!(navigator.plugins instanceof PluginArray) || navigator.plugins.length == 0) {
			console.log("PhantomJS environment detected. navigator.plugins= " + navigator.plugins + " Length: " + navigator.plugins.length);
		}
		console.log("Detect PJS DONE");

	});
};
page.onResourceRequested = function(requestData, networkRequest) {
	requestID++;
	var newUrl = "http://127.0.0.1:" + localPort + "/webproxy?id=" + localID + "&url=" + encodeURIComponent(requestData.url) + "&rid=" + requestID;

	console.log("Redirect " + newUrl);

	networkRequest.changeUrl(newUrl);

	networkRequest.setHeader("Host", "127.0.0.1");

};
page.onConsoleMessage = function(msg) {
	system.stderr.writeLine('console: ' + msg);
};

var loadPage = function(jobID, url) {
	console.log("Load Page " + page + " " + url);
	page.open(url, function(status) {
		console.log("Page Loaded");
		endJob(jobID, status);

	});

};