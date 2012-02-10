//    JD RemoteControl Demo
//    Copyright (C) 2011  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

(function JDwebUI_Initialization($) {
	var JDwebUI = {
		_defaults: {
			serverAddress: 	'http://localhost:3128',
			refreshTime: 	5000
		},
		_cookieCache: {},
		_logHandlers: 	[],
		_sendRequest: function(url, callback) {
			$.ajax({
				url: JDwebUI.buildUrl(url),
				success: callback
				/*
				dataType: 'jsonp',
				url: JDwebUI.buildUrl(url)+ "?getjson=true",
				jsonp: "jsonp_callback",
				success: callback
				*/
			});
			
			// Return requst object
			return {
					'follow': function() {
						JDwebUI.follow(url, callback);
						return this;
					},
					'unfollow': function() {
						JDwebUI.unfollow(url);
						return this;
					}
				};
		},
		_commands: [],
		_intervalCallback: function() {
			$.each(JDwebUI._commands, function(ignore, value) {
				JDwebUI._sendRequest(value.url, value.callback);
			});
		},
		init: function() {
			$.jd.setOptions({
								apiServer: 'http://localhost:3128',
								user: 'jd',
								pass: 'jd',
								debug: true
							});
			$.jd.startSession(function(e) {
				console.dir(arguments)
				if (e.status == $.jd.e.sessionStatus.REGISTERED)
					$(JDwebUI).trigger('sessionStart');
					
				$.jd.startPolling();
			});
			
			window.setInterval(JDwebUI._intervalCallback, JDwebUI.option('refreshTime'));
		},
		buildUrl: function(command) {
			return JDwebUI.option('serverAddress') + command;
		},
		follow: function(url, callback) {
			JDwebUI._sendRequest(url, callback);
			JDwebUI._commands.push({'url': url, 'callback': callback});
		},
		unfollow: function(url) {
			$.each(JDwebUI._commands, function(i, value) {
				if(value.url == url) {
					JDwebUI._commands.splice(i, 1);
					return false;
				}
			});
		},
		format: {
			array: function(a, separator) {
				// Defaults to slash
				separator = separator || '/';
				if(!$.isArray(a))
					return '';
				return escape(a.join(separator));
			},
			bool: function(b) {
				if(!!b)
					return 'true';
				return 'false';
			},
			string: function(s) {
				return escape(s);
			},
			type: function(s) {
				if(s != 'offline' && s != 'available')
					return 'offline';
				return 'available';
			},
			encoding: function() {
				return 'base64';
			}
		},
		option: function(key, value) {
			if(typeof value == 'undefined') {	// Get option
				// Check cache
				if(key in JDwebUI._cookieCache)
					return JDwebUI._cookieCache[key];

				var value = $.cookie('options_'+key);
				
				if(value === null) {
					value = JDwebUI._defaults[key];
					JDwebUI.option(key, value);
				} else {
					JDwebUI._cookieCache[key] = value;
				}

				return value;
			}

			$.cookie('options_'+key, value, {expires: 365});
			JDwebUI._cookieCache[key] = value;
		}
	};

	// Web interface wrapper
	// get
	JDwebUI.get = {
		// Get main information/configuration
		'rcversion': function(callback) {
			return JDwebUI._sendRequest('/get/rcversion', callback);
		},
		'version': function(callback) {
			return JDwebUI._sendRequest('/get/version', callback);
		},
		'config': function(callback) {
			return JDwebUI._sendRequest('/get/config', callback);
		},
		'ip': function(callback) {
			return JDwebUI._sendRequest('/get/ip', callback);
		},
		'randomip': function(callback) {
			return JDwebUI._sendRequest('/get/randomip', callback);
		},
		'speed': function(callback) {
			return JDwebUI._sendRequest('/get/speed', callback);
		},
		'speedlimit': function(callback) {
			return JDwebUI._sendRequest('/get/speedlimit', callback);
		},
		'downloadstatus': function(callback) {
			return JDwebUI._sendRequest('/get/downloadstatus', callback);
		},
		'isreconnect': function(callback) {
			return JDwebUI._sendRequest('/get/isreconnect', callback);
		},
		'passwordlist': function(callback) {
			return JDwebUI._sendRequest('/get/passwordlist', callback);
		},
		
		// Get linkgrabber information
		'grabber': {
			'list': function(callback) {
				return JDwebUI._sendRequest('/get/grabber/list', callback);
			},
			'count': function() {
				
			},
			'isbusy': function() {
				
			},
			'isset': {
				'startafteradding': function(callback) {
					return JDwebUI._sendRequest('/get/grabber/isset/startafteradding', callback);
				},
				'autoadding': function(callback) {
					return JDwebUI._sendRequest('/get/grabber/isset/autoadding', callback);
				}
			}
		},
		
		// Get download list information
		'downloads': {
			'count': function(cat, callback) {
				if(cat != 'current' || cat != 'finished')
					cat = 'all';

				return JDwebUI._sendRequest('/get/downloads/'+cat+'/count', callback);
			},
			'list': function(cat, callback) {
				if(cat != 'current' || cat != 'finished')
					cat = 'all';

				return JDwebUI._sendRequest('/get/downloads/'+cat+'/list', callback);
			}
		}
	}

	// set
	JDwebUI.set = {
		// Set (download-/grabber-)configuration
		'passwordlist': function(listArray, callback) {
			return JDwebUI._sendRequest('/set/passwordlist/'+JDwebUI.format.array(listArray, '\n'), callback)
		},
		'reconnect': function(yesno, callback) {
			
		},
		'premium': function(yesno, callback) {
			
		},
		'downloaddir': function(dir, callback) {
			
		},
		'download': {
			'limit': function(value) {
				
			},
			'max': function(value) {
				
			}
		},
		'grabber': {
			'startafteradding': function(yesno, callback) {
				return JDwebUI._sendRequest('/set/grabber/startafteradding/'+JDwebUI.format.bool(yesno), callback);
			},
			'autoadding': function(yesno, callback) {
				return JDwebUI._sendRequest('/set/grabber/autoadding/'+JDwebUI.format.bool(yesno), callback);
			}
		}
	}

	// /action
	JDwebUI.action = {
		// Control downloads
		'stop': function(callback) {
			return JDwebUI._sendRequest('/downloads/start', callback);
		},
		'start': function(callback) {
			return JDwebUI._sendRequest('/downloads/stop', callback);
		},
		'captcha': {
			'getcurrent': function(e, callback) {
				return JDwebUI._sendRequest('/action/captcha/getcurrent/'+JDwebUI.format.encoding(e), callback);
			},
			'solve': function(string) {
				return JDwebUI._sendRequest('/action/captcha/solve/'+JDwebUI.format.string(string));
			}
		},

		// Client actions
		'update': function(callback) {
			return JDwebUI._sendRequest('/action/update', callback);
		},
		'restart': function(callback) {
			return JDwebUI._sendRequest('/action/restart', callback);
		},
		'shutdown': function(callback) {
			return JDwebUI._sendRequest('/action/shutdown', callback);
		},

		// Add downloads
		'add': {
			'links': function(links, callback) {
				return JDwebUI._sendRequest('/action/add/links/'+JDwebUI.format.array(links, "\n"), callback);
			},
			'container': function(location) {
				
			}
		},

		// Export download packages
		'save': {
			'container': function(location, grabber) {
				
			}
		},

		// Edit linkgrabber packages
		'grabber': {
			'set': {
				'archivepassword': function(packageName, pass) {
					
				},
				'downloaddir': function(packageName, location) {
					
				},
				'comment': function(packageName, value) {
					
				}
			},
			'rename': function(packageName, value) {
				
			},
			'join': function(packageName, packages) {
				
			},
			'confirmall': function(callback) {
				return JDwebUI._sendRequest('/action/downloads/confirmall', callback);
			},
			'confirm': function(packages, callback) {
				//console.log('/action/grabber/confirm/'+JDwebUI.format.array(packages));
				return JDwebUI._sendRequest('/action/grabber/confirm/'+JDwebUI.format.array(packages), callback);
			},
			'removetype': function(type, callback) {
				return JDwebUI._sendRequest('/action/grabber/removetype/'+JDwebUI.format.type(type), callback)
			},
			'removeall': function(callback) {
				return JDwebUI._sendRequest('/action/grabber/removeall', callback);
			},
			'remove': function(packages, callback) {
				return JDwebUI._sendRequest('/action/grabber/remove/'+JDwebUI.format.array(packages), callback);
			},
			'move': function(packageName, links, callback) {
				return JDwebUI._sendRequest('/action/grabber/move/'+JDwebUI.format.string(packageName)
																+'/'+JDwebUI.format.array(links, '\n')
																,callback)
			}
		},
		'downloads': {
			'removeall': function(callback) {
				return JDwebUI._sendRequest('/action/downloads/removeall', callback);
			},
			'remove': function(packages, callback) {
				return JDwebUI._sendRequest('/action/downloads/remove/'+JDwebUI.format.array(packages), callback);
			},
			'speed': function(packages, callback) {
				return JDwebUI._sendRequest('/action/downloads/remove/'+JDwebUI.format.array(packages), callback);
			}
		}
	}
	
	// special
	JDwebUI.special = {
		'check': function(links, callback) {
			return JDwebUI._sendRequest('/special/check/'+JDwebUI.format.array(links, '\n'), callback);
		}
	}
	
	window.JDwebUI = JDwebUI;
})(jQuery);