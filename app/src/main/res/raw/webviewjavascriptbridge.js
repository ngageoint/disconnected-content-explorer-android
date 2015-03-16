;(function() {
	if (window.WebViewJavascriptBridge) { return }

	var messageHandlers = {}
	var responseCallbacks = {}
	var uniqueId = 1

	function init(messageHandler) {
		if (WebViewJavascriptBridge._messageHandler) { throw new Error("WebViewJavascriptBridge.init called twice") }
		WebViewJavascriptBridge._messageHandler = messageHandler
	}

	function send(data, responseCallback) {
		_doSend({ data:data }, responseCallback)
	}
	
	function registerHandler(handlerName, handler) {
		messageHandlers[handlerName] = handler
	}
	
	function callHandler(handlerName, data, responseCallback) {
		_doSend({ handlerName:handlerName, data:data }, responseCallback)
	}
	
	function _doSend(message, responseCallback) {
	console.log("responseCallback:"+responseCallback);
		if (responseCallback) {
			var callbackId = "cb_"+(uniqueId++)+"_"+new Date().getTime()
			responseCallbacks[callbackId] = responseCallback
			message["callbackId"] = callbackId
			}
				console.log("sending:"+JSON.stringify(message));
				console.log("in _doSend with data: " + message.data + " and handler name " + message.handlerName);
		_WebViewJavascriptBridge._handleMessageFromJs(JSON.stringify(message.data)||null,message.responseId||null,
		    message.responseData||null,message.callbackId||null,message.handlerName||null);

	}

	function _dispatchMessageFromJava(messageJSON) {
			console.log("dispatching message from java");
			var message = JSON.parse(messageJSON)
			var messageHandler
			
			if (message.responseId) {
				var responseCallback = responseCallbacks[message.responseId]
				if (!responseCallback) { return; }
				console.log("if (message.responseId): Type of message.responseData: " + typeof message.responseData);
				responseCallback(JSON.parse(message.responseData))
				delete responseCallbacks[message.responseId]
			} else {
				var responseCallback
				if (message.callbackId) {
					var callbackResponseId = message.callbackId
					console.log("else: Type of responseData: " + responseData);
					responseCallback = function(responseData) {
						_doSend({ responseId:callbackResponseId, responseData:responseData })
					}
				}
				
				var handler = WebViewJavascriptBridge._messageHandler
				if (message.handlerName) {
					handler = messageHandlers[message.handlerName]
				}
				try {
				    console.log("try: Type of message.data: " + message.data);
					handler(message.data, responseCallback)
				} catch(exception) {
					if (typeof console != "undefined") {
						console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception)
					}
				}
			}
	}
			

	function _handleMessageFromJava(messageJSON) {
		_dispatchMessageFromJava(messageJSON)
	}

	//export
	window.WebViewJavascriptBridge = {
		init: init,
		send: send,
		registerHandler: registerHandler,
		callHandler: callHandler,
		_handleMessageFromJava: _handleMessageFromJava
	}

    console.log("initialized js bridge");

	//dispatch event
	var doc = document;
    var readyEvent = doc.createEvent("Events");
    readyEvent.initEvent("WebViewJavascriptBridgeReady");
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();
