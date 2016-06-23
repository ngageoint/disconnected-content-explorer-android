package com.fangjian;

import android.app.Activity;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * implements Serializable in case of javascript interface will be removed in obfuscated code.
 *
 * User: jack_fang
 * Date: 13-8-15
 * Time: 下午6:08
 */
public class WebViewJavascriptBridge implements Serializable {
    WebView mWebView;
    Activity mContext;
    WVJBHandler _messageHandler;
    Map<String,WVJBHandler> _messageHandlers;
    Map<String,WVJBResponseCallback> _responseCallbacks;
    long _uniqueId;

    public WebViewJavascriptBridge(Activity context,WebView webview,WVJBHandler handler, WebViewClient webViewClient) {
        this.mContext=context;
        this.mWebView=webview;
        this._messageHandler=handler;
        _messageHandlers=new HashMap<String,WVJBHandler>();
        _responseCallbacks=new HashMap<String, WVJBResponseCallback>();
        _uniqueId=0;
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setWebContentsDebuggingEnabled(true);
        }*/
        mWebView.addJavascriptInterface(this, "_WebViewJavascriptBridge");
        mWebView.setWebViewClient(webViewClient);
        mWebView.setWebChromeClient(new MyWebChromeClient());     //optional, for show console and alert
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage cm) {
            Log.d("JSBridge", cm.message() + " line: " + cm.lineNumber());
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            // if don't cancel the alert, webview after onJsAlert not responding taps
            // you can check this :
            // http://stackoverflow.com/questions/15892644/android-webview-after-onjsalert-not-responding-taps
            result.cancel();
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public interface WVJBHandler{
        public void handle(String data,WVJBResponseCallback jsCallback);
    }

    public interface WVJBResponseCallback{
        public void callback(String data);
    }

    public void registerHandler(String handlerName,WVJBHandler handler) {
        _messageHandlers.put(handlerName,handler);
    }

    private class CallbackJs implements WVJBResponseCallback{
        private final String callbackIdJs;

        public  CallbackJs(String callbackIdJs){
            this.callbackIdJs=callbackIdJs;
        }
        @Override
        public void callback(String data) {
            _callbackJs(callbackIdJs,data);
        }
    }


    private void _callbackJs(String callbackIdJs,String data) {
        //TODO: CALL js to call back;
        Map<String,String> message=new HashMap<String, String>();
        message.put("responseId",callbackIdJs);
        message.put("responseData",data);
        _dispatchMessage(message);
    }

    @JavascriptInterface
    public void _handleMessageFromJs(String data,String responseId,
                                     String responseData,String callbackId,String handlerName){
        if (null!=responseId) {
            WVJBResponseCallback responseCallback = _responseCallbacks.get(responseId);
            responseCallback.callback(responseData);
            _responseCallbacks.remove(responseId);
        } else {
            WVJBResponseCallback responseCallback = null;
            if (null!=callbackId) {
                responseCallback=new CallbackJs(callbackId);
            }
            WVJBHandler handler;
            if (null!=handlerName) {
                handler = _messageHandlers.get(handlerName);
                if (null==handler) {
                    Log.e("JSBridge","WVJB Warning: No handler for "+handlerName);
                    return ;
                }
            } else {
                handler = _messageHandler;
            }
            try {
                final String tmpData = data;
                final WVJBResponseCallback tmpResponseCallback = responseCallback;
                final WVJBHandler tmpHandler = handler;

                mContext.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        tmpHandler.handle(tmpData, tmpResponseCallback);
                    }
                });
            }catch (Exception exception) {
                Log.e("JSBridge","WebViewJavascriptBridge: WARNING: java handler threw. "+exception.getMessage());
            }
        }
    }

    public void send(String data) {
        send(data,null);
    }

    public void send(String data ,WVJBResponseCallback responseCallback) {
        _sendData(data,responseCallback,null);
    }

    private void _sendData(String data,WVJBResponseCallback responseCallback,String  handlerName){
        Map <String, String> message=new HashMap<String,String>();
        message.put("data",data);
        if (null!=responseCallback) {
            String callbackId = "java_cb_"+ (++_uniqueId);
            _responseCallbacks.put(callbackId,responseCallback);
            message.put("callbackId",callbackId);
        }
        if (null!=handlerName) {
            message.put("handlerName", handlerName);
        }
        _dispatchMessage(message);
    }

    private void _dispatchMessage(Map <String, String> message){
        String messageJSON = new JSONObject(message).toString();
        Log.d("JSBridge","sending:"+messageJSON);
        final  String javascriptCommand =
                String.format("javascript:WebViewJavascriptBridge._handleMessageFromJava('%s');",doubleEscapeString(messageJSON));
        mContext.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                mWebView.loadUrl(javascriptCommand);
            }
        });
    }


    public  void callHandler(String handlerName) {
        callHandler(handlerName, null, null);
    }

    public void callHandler(String handlerName,String data) {
        callHandler(handlerName, data,null);
    }

    public void callHandler(String handlerName,String data,WVJBResponseCallback responseCallback){
        _sendData(data, responseCallback,handlerName);
    }

    /*
      * you must escape the char \ and  char ", or you will not recevie a correct json object in 
      * your javascript which will cause a exception in chrome.
      *
      * please check this and you will know why.
      * http://stackoverflow.com/questions/5569794/escape-nsstring-for-javascript-input
      * http://www.json.org/
    */
    private String doubleEscapeString(String javascript) {
        String result;
        result = javascript.replace("\\", "\\\\");
        result = result.replace("\"", "\\\"");
        result = result.replace("\'", "\\\'");
        result = result.replace("\n", "\\n");
        result = result.replace("\r", "\\r");
        result = result.replace("\f", "\\f");
        return result;
    }

}
