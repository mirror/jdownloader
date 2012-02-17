/**
 * @preserve 
 * ##JDownloader API Bindings for jQuery
 * - - -
 * **namespace** jQuery.jd<br/>
 * @version 07/2011
 * @author mhils
 * 
 * The JDownloader API provides both a polling and an event streaming interface. 
 * This wrapper is the client side reference implementation containing all features of the API.
 * 
 * 
 */

(function jdapi($) {
	$.jd = {
		/**
		 * ##setOptions()
		 * Set options
		 * 
		 * @param {Object.<string, *>} options to set
		 */
		setOptions : function setOptions(options) {
			if (options) {
				$.extend($.jd._settings, options);
			}
			if (!$.jd._settings.apiServer.match(/\/$/))
				$.jd._settings.apiServer = $.jd._settings.apiServer + "/";
			return this;
		},
		/**
		 * ##getOptions()
		 * Get a clone of the current options
		 * 
		 * @return {Object.<string, *>}
		 */
		getOptions : function getOptions() {
			return $.extend(true, {}, $.jd._settings);
		},
		/**
		 * ##startSession()
		 * Start a session. Please note that this function is async and will
		 * return immediately. Use the callback function for functions requiring
		 * a session. For anonymous sessions, omit login parameters. If user and
		 * pass are set as options, you can omit these parameters.
		 * 
		 * @param {string=} user (optional)
		 * @param {string=} pass (optional)
		 * @param {function(Object.<string, *>,?string=)=} callback
		 * @suppress {checkTypes}
		 */
		startSession : function startSession(user, pass, callback) {
			if ($.jd._ajax.sessionStatus !== $.jd.e.sessionStatus.NO_SESSION) {
				if($.jd._ajax.sessionStatus === $.jd.e.sessionStatus.REGISTERED){
					callback({
						"status" : $.jd._ajax.sessionStatus,
						"data" : "already logged in"
					});
					return this;
				}
				$.jd.stopSession(function() {
					$.jd.startSession(user, pass, callback);
				}); // disconnect first then
				return this;
			}
			
			//! Shift parameters if user and pass are omitted
			if ($.isFunction(user)) { 
				callback = user;
				user = pass = undefined;
			}
			$.jd._settings.user = (user) ? user : $.jd._settings.user;
			$.jd._settings.pass = (pass) ? pass : $.jd._settings.pass;

			var callbackIsFunction = $.isFunction(callback);
			
			$.jd.send("session/handshake", ($.jd._settings.user) ? [ $.jd._settings.user, $.jd._settings.pass ]
					: /* anonymous */[ "", "" ], function handshakeResponse(response) {
				$.jd._ajax.debug( "Handshake response:", response );
				//! Check if server is returning a session id. If so, our
				//! handshake (either authenticated or anonymous) succeeded.
				if (response && typeof (response) === "string" && response !== ""
						&& response !== $.jd.e.sessionStatus.ERROR) {
					var status = ($.jd._settings.user) ? $.jd.e.sessionStatus.REGISTERED
							: $.jd.e.sessionStatus.ANONYMOUS;
					$.jd._ajax.token = response;
					$.jd._settings.user = user;
					$.jd._settings.pass = pass;
					$.jd._ajax.sessionStatus = status;
					if (callbackIsFunction) {
						callback({
							"status" : $.jd._ajax.sessionStatus,
							"data" : response
						});
					}
				} else if (callbackIsFunction) {
					callback({
						"status" : $.jd.e.sessionStatus.ERROR,
						"data" : response
					});
				}
			}, function handshakeError(response) {
				if (callbackIsFunction) {
					callback({
						"status" : $.jd.e.sessionStatus.ERROR,
						"data" : response
					});
				}
			});
			return this;
		},
		/**
		 * ##stopSession()
		 * Stops a session.
		 * 
		 * @param {function(Object.<string, *>,?string=)=} callback
		 */
		stopSession : function stopSession(callback) {
			if($.jd._ajax.sessionStatus === $.jd.e.sessionStatus.NO_SESSION)
				return this;
			$.jd.stopPolling();
			$.jd._ajax.token = undefined;
			$.jd._ajax.subscriptions = {};
			//! No matter whether the server invalidates the session, we do!
			$.jd._ajax.sessionStatus = $.jd.e.sessionStatus.NO_SESSION;
			$.jd.send("session/disconnect", callback, callback);
			return this;
		},
		/**
		 * ##getSessionStatus()
		 * Get the current session status. See sessionStatus enum.
		 * 
		 * @return {string|undefined}
		 */
		getSessionStatus : function getSessionStatus() {
			return $.jd._ajax.sessionStatus;
		},
		/**
		 * ##startPolling()
		 * Starts polling from the server continuously
		 * 
		 * @return {jQuery}
		 */
		startPolling : function startPolling() {
			if ($.jd.isPollingContinuously())
				return this;
			$.jd._ajax.active = true;
			$.jd._ajax.lastEventId = undefined;
			$.jd.pollOnce();
			return this;
		},
		
		/**
		 * ##subscribe()
		 * Subscribe to events of a certain namespace. If you want to subscribe to all namespaces, use \* as namespace parameter. 
		 * In this case, the callback parameter will replace the onmessage setting.
		 * 
		 * @param {string} namespace to subscribe to
		 * @param {function(Object.<string, *>,?string=)=} callback function to be executed every time an event happens
		 * @param {function(Object.<string, *>)=} onSubscribed callback function to be executed as soon as you are subscribed.
		 * @return {jQuery}
		 */
		subscribe : function subscribe(namespace, callback, onSubscribed) {
			
			namespace = $.jd._ajax.cleanNamespace(namespace);
			
			if(namespace in $.jd._ajax.subscriptions && $.isFunction(callback))
			{
				//Check if callback is already registered for this namespace. Skip, if so.
				var subscr = $.jd._ajax.subscriptions[namespace];
				for(var i=0;i<subscr.length;i++)
				{
					if(subscr[i] === callback)
					{
						if($.isFunction(onSubscribed))
							onSubscribed({"status":"already subscribed with that callback"});
						return this;

					}
				}
				$.jd._ajax.subscriptions[namespace].push(callback);
				if($.isFunction(onSubscribed))
					onSubscribed({"status":"callback added"});
			} else {
				if($.isFunction(callback))
					$.jd._ajax.subscriptions[namespace] = [callback];
				
				if(namespace === "*")
				{
					if($.isFunction(callback))
						$.jd._settings.onmessage = callback;
					//TODO: Subscribe all
				}
					
				$.jd.send(namespace + "/subscribe",onSubscribed,onSubscribed);
				
			}
			return this;
		},
		/**
		 * ##unsubscribe()
		 * Unsubscribe from events of a certain namespace. 
		 * If you want to unsubscribe from all namespaces, use \* as namespace parameter.
		 * 
		 * @param {string} namespace to unsubscribe from
		 * @param {function(Object.<string, *>)=}  onUnsubscribed callback which function to be executed after subscription has been canceled.
		 * @return {jQuery}
		 */
		unsubscribe: function unsubscribe(namespace, onUnsubscribed){
			namespace = $.jd._ajax.cleanNamespace(namespace);
			var namespaces = {};
			if(namespace === "*")
				namespaces = $.jd._ajax.subscriptions;
			else if(namespace in $.jd._ajax.subscriptions)
				namespaces[namespace] = $.jd._ajax.subscriptions[namespace];
			else
			{
				if($.isFunction(onUnsubscribed))
					onUnsubscribed({"status":"already unsubscribed"});
				return this;
			}
			
			$.each(namespaces,function(namespace,callbacks){
				
				$.jd.send(namespace + "/unsubscribe");
				delete $.jd._ajax.subscriptions[namespace];				
				
			});
			return this;
		},

		/**
		 * ##stopPolling()
		 * Stops polling from the server continuously
		 * 
		 * @return {jQuery}
		 */
		stopPolling : function stopPolling() {
			$.jd._ajax.active = false;
			if ($.jd._ajax.jqXHR && $.jd._ajax.jqXHR.abort)
				$.jd._ajax.jqXHR.abort();
			$.jd._ajax.lastEventId = undefined;
			return this;
		},

		/**
		 * ##isPollingContinuously()
		 * Returns `true` if jquery is polling continuously
		 * 
		 * @return {boolean}
		 */
		isPollingContinuously : function isPollingContinuously() {
			return $.jd._ajax.active;
		},

		/**
		 * ##pollOnce()
		 * Poll events from the server **once**
		 * 
		 * @return {jQuery}
		 */
		pollOnce : function pollOnce() {
			if ($.jd._ajax.sessionStatus === $.jd.e.sessionStatus.NO_SESSION) {
				$.jd.stopPolling();
				$.jd._ajax.handlePoll({
					"type" : $.jd.e.messageType.SYSTEM,
					"message" : $.jd.e.sysMessage.ERROR,
					"data" : {
						"status" : "No active session. Did you call startSession()?"
					}
				});
			} else {
				if ($.jd._ajax.jqXHR !== null) // abort running requests
					$.jd._ajax.jqXHR.abort();
				$.jd._ajax.jqXHR = $.ajax({
					dataType : ($.jd._settings.sameDomain ? "json" : "jsonp"),
					type : "GET",
					url : $.jd.getURL('events/listen'),
					data : {
						token : $.jd._ajax.token,
						lastEventId : $.jd._ajax.lastEventId
					},
					async : true,
					cache : false,
					timeout : $.jd._settings.apiTimeout,
					success : $.jd._ajax.handlePoll,
					complete : $.jd._ajax.pollComplete,
					error : $.jd._ajax.pollError
				});
				$.jd._ajax.debug( "pollOnce", $.jd._ajax.lastEventId, $.jd._ajax.jqXHR );
			}
			return this;
		},
		/**
		 * ##getURL()
		 * Builds a query string for the API
		 * 
		 * @param {string} action to perform
		 * @param {function(Object.<string, *>,?string=)|Array.<string>=}
		 * params (optional) parameters for this action as a JSON array (e.g.
		 * ["param1",1,1.2,"param4"]) <br/>
		 * @return {string} url
		 */
		getURL: function getURL(action,params){
			
			action = $.jd._ajax.cleanNamespace(action);
			
			var query = "";
			if(params)
			{
				query = "?"+$.param({
					"token" : $.jd._ajax.token,
					"p" : params
				});
			}
			return $.jd._settings.apiServer + action + query;
		},

		/**
		 * ##send()
		 * Perform the action on the server
		 * 
		 * @param {string} action to perform.
		 * @param {function(Object.<string, *>,?string=)|Array.<string>=}
		 * params (optional) parameters for this action as a JSON array (e.g.
		 * ["param1",1,1.2,"param4"]) <br/>
		 * @param {function(Object.<string, *>,?string=)=} onSuccess (optional)
		 * function to execute. Response and PID are supplied as function
		 * parameters <br/>
		 * @param {function(Object.<string, *>)=} onError (optional) function
		 * to execute if an error occurs. Response is supplied as function
		 * parameter. <br/>
		 * @param {function(Object.<string, *>,?string=)=} onEvent (optional)
		 * function to execute each time an event is streamed that refers to this
		 * request. If this parameter is either omitted or this functions does
		 * *not* return false, the general poll callback is invoked, too. <br/>
		 * @return {jQuery}
		 * @suppress {checkTypes}
		 */
		send : function send(action, params, onSuccess, onError, onEvent) {
			//! shift callback if params are undefined
			if ($.isFunction(params)) {
				onEvent = onError;
				onError = onSuccess;
				onSuccess = params;
				params = undefined;
			}

			//! Check if callbacks are real functions
			if (!$.isFunction(onSuccess))
				onSuccess = undefined;
			if (!$.isFunction(onEvent))
				onEvent = undefined;
			if (!$.isFunction(onError))
				onError = undefined;

			//! Try to interpolate if we didn't get an array for params
			if ((!$.isArray(params)) && (params !== undefined)) {
				if ($.isPlainObject(params)) {
					var tmp = [];
					$.each(params, function(k, v) {
						tmp.push(v);
					});
					params = tmp;
				} else
					params = $.makeArray(params);
			}
			$.jd._ajax.debug( $.jd.getURL(action), params, onSuccess, onError, onEvent );
			$.ajax({
				dataType : ($.jd._settings.sameDomain ? "json" : "jsonp"),
				type : "GET",
				url : $.jd.getURL(action),
				data : {
					"token" : $.jd._ajax.token,
					"p" : params
				},
				async : true,
				cache : false,
				timeout : $.jd._settings.apiTimeout,
				success : function(event) {
					$.jd._ajax.sendSuccess(event, onSuccess, onError, onEvent);
				},
				error : function(a, b, c) {
					$.jd._ajax.sendError(a, b, c, onError);
				}
			});
			return this;
		},

		/**
		 * ##Settings
		 * Set by using setOptions. **Don't access this property directly.**
		 */
		_settings : {
			/**
			 * ###user
			 * Username for API auth
			 * 
			 * @type {(string|undefined)}
			 */
			user : undefined,
			/**
			 * ###pass
			 * pass for API auth
			 * 
			 * @type {(string|undefined)}
			 */
			pass : undefined,
			/**
			 * ###apiServer
			 * API server root url
			 * 
			 * @type {string}
			 */
			apiServer : 'http://127.0.0.1:3128/',
			/**
			 * ###onmessage
			 * Callback function to be called if an event is recieved. If the
			 * event comes with a PID, The PIDs custom function will be executed at first.
			 * If it returns `false`, this function won't be executed.
			 * 
			 * @type {function(Object,number=)|undefined}
			 */
			onmessage : undefined,
			/**
			 * ###onerror
			 * Callback function to be called if an internal error occured.
			 * (Disconnect, out of sync, unknown error)
			 * 
			 * @type {function(Object)|undefined}
			 */
			onerror : undefined,
			/**
			 * ###debug
			 * Debug Mode. Watch your Firebug / Chrome Dev tools console.
			 * 
			 * @type {boolean}
			 */
			debug : false,
			/**
			 * ###apiTimeout
			 * Timeout until API calls time out. Applies for both polling (once)
			 * and sending.
			 * 
			 * @type {number}
			 */
			apiTimeout : 31000,
			/**
			 * ###sameDomain
			 * Use JSON instead of JSONP requests. (Force same domain origin)
			 * 
			 * @type {boolean}
			 */
			sameDomain : false
		},
		//##Enums
		e : {
			/**
			 * ###login status
			 * 
			 * @const {string|undefined}
			 */
			sessionStatus : {
				NO_SESSION : undefined,
				ERROR : "error",
				ANONYMOUS : "anonymous",
				REGISTERED : "registered"
			},
			/**
			 * ###polling message type
			 * 
			 * @const {string}
			 */
			messageType : {
				SYSTEM : "system"
			},
			/**
			 * ###polling system message type
			 * 
			 * @const {string}
			 */
			sysMessage : {
				ERROR : "error",
				DISCONNECT : "disconnect",
				HEARTBEAT : "heartbeat",
				OUT_OF_SYNC : "outofsync"
			}
		},
		//- - -
		//## Internal Functions 
		//## <font color="red">Stop here if you are only using this API.</font>
		//- - -
		_ajax : {
			/**
			 * ###lastEventId
			 * Contains the last received message id. If connection fails due to
			 * timeouts, messages will get fetched again.
			 */
			lastEventId : undefined,
			/**
			 * ###active
			 * Status of continuous polling.
			 */
			active : false,
			/**
			 * ###jqXHR
			 * Stores the active jqXHR object for polling. Needed for aborting
			 * requests
			 */
			jqXHR : null,
			/**
			 * ###callbackMap
			 * Callback map storing callback functions for async requests. {pid :
			 * callback}
			 */
			callbackMap : {},
			/**
			 * ###subscriptions
			 * Callback map storing callback functions for events belonging to subscribed modules. {namespace : [callback]
			 */
			subscriptions: {},
			/**
			 * ###token
			 * Current security token
			 * 
			 * @type {string|undefined}
			 */
			token : undefined,
			/**
			 * ###sessionStatus
			 * Current session status
			 * 
			 * @type {string|undefined}
			 */
			sessionStatus : undefined,
			/**
			 * ##sendSuccess()
			 * Trigger/Register callbacks associated with a certain request
			 * (fixed process id)
			 * 
			 * @param event direct return value of the request
			 * @param {function(Object.<string, *>,?string=)=} onSuccess to be
			 * called after direct response has been recieved.
			 * @param {function(Object.<string, *>)=} onError to be called if
			 * an error occurs
			 * @param {function(Object.<string, *>,?string=)=} onEvent to be
			 * called if further events associated with this pid are streamed.
			 */
			sendSuccess : function sendSuccess(event, onSuccess, onError, onEvent) {
				
				$.jd._ajax.debug("sendSuccess:", event);
				
				//! Check for errors
				if (event.type && event.type === $.jd.e.messageType.SYSTEM && event.message
						&& event.message === $.jd.e.sysMessage.ERROR) {
					if($.isFunction(onError))
						onError(event.data);
					return;
				}

				//! register processCallback
				if (event.pid && $.isFunction(onEvent)) {
					$.jd._ajax.callbackMap[event.pid] = onEvent;
					$.jd._ajax.debug("Register PID...", event);
				}
				//! run normal callback
				if (jQuery.isFunction(onSuccess))
					onSuccess(event.data, event.pid);
			},
			/**
			 * ##handlePoll()
			 * Handles completed poll from the server
			 * 
			 * @param {Object.<string,*>} event data received from the server
			 */
			handlePoll : function handlePoll(event) {
				$.jd._ajax.debug("handlePoll: ",event,event.id,$.jd._ajax.lastEventId);
				// accept message if lastEventId is valid. System events come out of scope
				if (($.jd._ajax.lastEventId === undefined || (event.data && ($.jd._ajax.lastEventId === (event.id - event.data.length))))
						|| (event.type && event.type === $.jd.e.messageType.SYSTEM)) {
					if (event.data && !(event.type && event.type == $.jd.e.messageType.SYSTEM))
						$.jd._ajax.lastEventId = event.id;

					
					// System events will be handeled internally and might be
					// passed to the onerror function
					if (event.type && event.type === $.jd.e.messageType.SYSTEM) {
						switch (event.message) {
						case $.jd.e.sysMessage.DISCONNECT:
							$.jd.stopPolling();
							if ($.isFunction($.jd._settings.onerror))
								$.jd._settings.onerror({
									"status" : $.jd.e.sysMessage.DISCONNECT
								});
						break;
						case $.jd.e.sysMessage.HEARTBEAT:
						break;
						case $.jd.e.sysMessage.OUT_OF_SYNC:
							$.jd.stopPolling();
							if ($.isFunction($.jd._settings.onerror))
								$.jd._settings.onerror({
									"status" : $.jd.e.sysMessage.OUT_OF_SYNC
								});
						break;
						default:
							if ($.isFunction($.jd._settings.onerror))
								$.jd._settings.onerror(event.data);
						break;
						}
					} else // We recieved a regular event.
					{
						$.jd._ajax.debug(event.data.length + " events in this message.");

						$.each(event.data, function(i, e) {
							$.jd._ajax.handleEvent(e);
						});
					}
				}
				else
				{
					$.jd._ajax.debug("Invalid Message id");
				}
			},
			/**
			 * ##handleEvent()
			 * Trigger all callbacks belonging to the given event
			 * 
			 * @param {Object.<string, *>} event the event
			 * @suppress {checkTypes}
			 */
			handleEvent : function handleEvent(event) {
				if (event.pid && (event.pid in $.jd._ajax.callbackMap)) {
					//! If specific callback returns false,
					//! don't trigger general callback
					if ($.jd._ajax.callbackMap[event.pid](event.data, event.namespace, event.pid) === false)
						return;

				}
				
				if(event.namespace && event.namespace in $.jd._ajax.subscriptions)
				{
					$.each($.jd._ajax.subscriptions[event.namespace], function(i,callback){
						callback(event.data,event.namespace,event.pid);
					});
				}
				
				if ($.isFunction($.jd._settings.onmessage))
					$.jd._settings.onmessage(event.data, event.namespace, event.pid);
			},
			/**
			 * ##pollComplete()
			 * Checks if continuous polling is active and polls again if
			 * necessary
			 * 
			 * @param jqXHR see jQuery.ajax doc
			 * @param textStatus see jQuery.ajax doc
			 */
			pollComplete : function pollComplete(jqXHR, textStatus) {
				$.jd._ajax.debug("pollComplete",$.jd._ajax.lastEventId);
				$.jd._ajax.jqXHR = null;
				if ($.jd._ajax.active && $.jd._ajax.active === true) {
					if ($.jd._settings.debug === true) {
						setTimeout($.jd.pollOnce, 2000);
					} else {
						$.jd.pollOnce();
					}
				}
			},
			/**
			 * ##pollError()
			 * Handle ajax poll error.
			 * 
			 * @param jqXHR see jQuery.ajax doc
			 * @param textStatus see jQuery.ajax doc
			 * @param errorThrown see jQuery.ajax doc
			 */
			pollError : function pollError(jqXHR, textStatus, errorThrown) {
				$.jd._ajax.handlePoll({
					"type" : $.jd.e.messageType.SYSTEM,
					"message" : $.jd.e.sysMessage.ERROR,
					"data" : {
						"jqXHR" : jqXHR,
						"status" : textStatus,
						"errorThrown" : errorThrown
					}
				});
			},
			/**
			 * ##sendError()
			 * Handle ajax errors for sending.
			 * 
			 * @param jqXHR see jQuery.ajax doc
			 * @param textStatus see jQuery.ajax doc textStatus
			 * @param errorThrown see jQuery.ajax doc
			 * @param {function(Object.<string, *>,?string=)=} onError
			 * additional callback to be executed
			 */
			sendError : function sendError(jqXHR, textStatus, errorThrown, onError) {
				if ($.isFunction(onError))
					onError({
						"jqXHR" : jqXHR,
						"status" : textStatus,
						"errorThrown" : errorThrown
					});
			},
			
			/**
			 * ##debug()
			 * @param {...*} output
			 * Debug if debug flag is active.
			 */
			debug: function debug(output){
				  if($.jd._settings.debug === true && window.console){
					  console.log( Array.prototype.slice.call(arguments) );
				}
			},
			
			/**
			 * ##cleanNamespace()
			 * Removes leading & trailing slash from a string
			 * 
			 * @param {string} namespace
			 * @return {string} Cleaned namespace
			 */
			cleanNamespace: function cleanNamespace(namespace){
				if (namespace[namespace.length-1] === "/")
					namespace = namespace.slice(0,-1);
				if (namespace[0] === "/")
					namespace = namespace.substr(1);
				return namespace;
			}

		}
	};

})(jQuery);