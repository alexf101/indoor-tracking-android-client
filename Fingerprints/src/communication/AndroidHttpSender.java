package communication;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import messages.FailureMsg;
import org.apache.http.Header;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import util.Dbg;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class AndroidHttpSender implements Sender {

    protected final String serverRoot;
    protected AsyncHttpClient httpClient;
    protected static AndroidHttpClient syncHttpClient;
    protected Gson gson = new Gson();
    private boolean authenticated = false;
    private String username;
    private String password;

    public AndroidHttpSender(String serverRoot) {
        this.serverRoot = serverRoot;
        httpClient = new AsyncHttpClient();
        httpClient.setUserAgent("android");
        if (syncHttpClient == null) {
            try {
                syncHttpClient = AndroidHttpClient.newInstance("android");
            } catch (IllegalStateException e) {
                Log.e(AndroidHttpSender.class.getName(), "AndroidHttpClient was not closed", e);
            }
        }
        /*
        syncHttpClient = new SyncHttpClient() {
            @Override
            public String onRequestFailed(Throwable throwable, String s) {
                Dbg.loge(this.getClass().getName(), "Request failed...", throwable);
                return "Request failed: " + s;
            }
        };
        */
    }

    @Override
    public void finalize() throws Throwable {
        syncHttpClient.close();
        super.finalize();
    }

    @Override
    public void setUser(String username, String password) {
        this.username = username;
        this.password = password;
        if (username != null && password != null) {
            httpClient.setBasicAuth(username, password);
            authenticated = true;
            Dbg.logi(this.getClass().getName(), "Using " + username + " for authentication");
        } else {
            Dbg.logw(this.getClass().getName(), "Could not set user, username or password were null");
        }
    }

    @Override
    public void async_send_no_reply(HttpMethod type, String url, final AcknowledgedTask acknowledgedTask) {
        Dbg.logd(this.getClass().getName(), "Sending message to " + url);
        AsyncHttpResponseHandler asyncHttpResponseHandler = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String content) {
                if (content != null && content.length() > 0) {
                    Dbg.logw(this.getClass().getName(), "Called no_reply, but there was one: " + content);
                }
                Dbg.logd(this.getClass().getName(), "Calling on server acknowledge");
                acknowledgedTask.onServerAcknowledge();
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Dbg.loge(this.getClass().getName(), "Failure in HTTP layer: " + response, e);
            }

        };

        async_send(type, url, null, asyncHttpResponseHandler);
    }

//
//    /**
//     *
//     *
//     * @param url complete url, e.g. http://localhost:8080/buildings
//     * @param type GET, POST, PUT or DELETE
//     * @param json_content Optional. Not used with GET. A msg for the server.
//     * @param resultMsgType What class of msg is expected as the response?
//     * @return
//     */
//    @Override
//    public <T> T sync_send(HttpMethod type, String url, Object json_content, Class<T> resultMsgType) throws InterruptedException, ExecutionException, TimeoutException, APICallFailedException {
//
//        Request request = httpClient
//                .newRequest(serverRoot + url)
//                .method(type)
//                .timeout(3, TimeUnit.SECONDS);
//        if (type != HttpMethod.GET){
//            String json_data = gson.toJson(json_content);
//            request
//                    .param("data", json_data)
//                    .header(HttpHeader.CONTENT_TYPE, "application/json");
//        }
//        ContentResponse response = request.send();
//        return parseResponse(response.getContentAsString(), resultMsgType);
//    }

    public <T> void async_send(HttpMethod type, final String url, Object json_content, final Class<T> resultMsgType, final ResponseHandler<T> responseHandler) throws InterruptedException, ExecutionException, TimeoutException {
        Dbg.logd(this.getClass().getName(), "Sending message to " + url);
        AsyncHttpResponseHandler asyncHttpResponseHandler = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String content) {
                try {
                    Dbg.logd(this.getClass().getName(), "Received server reply to "+url+" request");
                    responseHandler.onServerResponse(parseResponse(content, resultMsgType));
                } catch (APICallFailedException e) {
                    Dbg.loge(this.getClass().getName(), "Could not recognise server response: " + content + " as an object of type " + resultMsgType, e);
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Dbg.loge(this.getClass().getName(), "Failure in HTTP layer: " + response, e);
                try {
                    responseHandler.onServerDeniedRequest(gson.fromJson(response, FailureMsg.class));
                } catch (JsonSyntaxException j) {
                    responseHandler.onServerDeniedRequest(new FailureMsg(response));
                }
            }

        };

        async_send(type, url, json_content, asyncHttpResponseHandler);
    }

    @Override
    public <T> T sync_send(HttpMethod type, String url, Object json_content, Class<T> resultType) {
        Dbg.logd(this.getClass().getName(), "Sending message to " + url);

        // params is a JSONObject
        StringEntity se = null;
        if (json_content != null) {
            try {
                String json = gson.toJson(json_content);
                se = new StringEntity(json);
                Dbg.logd(this.getClass().getName(), "Sending json payload: " + json);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //noinspection ConstantConditions
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        }

        if (type != HttpMethod.GET && !authenticated) {
            Dbg.logw(this.getClass().getName(), "No authentication details provided and about to execute a " +
                    "POST, DELETE or PUT request - this will probably fail!");
        }
        Header[] headers;
        if (authenticated) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
            headers = new Header[]{
                    BasicScheme.authenticate(credentials, "UTF-8", false),
                    new BasicHeader("Content-Type", "application/json")
            };
        } else {
            headers = new Header[]{new BasicHeader("Content-Type", "application/json")};
        }
        if (!url.startsWith("http://")) {
            url = serverRoot + url;
        }
        HttpUriRequest request = httpRequestForMethod(type, url, se);
        request.setHeaders(headers);
        try {
            String response = syncHttpClient.execute(request, new BasicResponseHandler());
            return parseResponse(response, resultType);
        } catch (APICallFailedException e) {
            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
            return null;
        } catch (IOException e) {
            Dbg.loge(this.getClass().getName(), "An unexpected error occurred", e);
            return null;
        }
    }

    @Override
    public void close() {
        syncHttpClient.close();
    }

    private HttpUriRequest httpRequestForMethod(HttpMethod type, String url, StringEntity se) {
        switch (type) {
            case GET:
                return new HttpGet(url);
            case POST:
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(se);
                return httpPost;
            case DELETE:
                return new HttpDelete(url);
            case PUT:
                HttpPut httpPut = new HttpPut(url);
                httpPut.setEntity(se);
                return httpPut;
            default:
                throw new Error("Unknown request type: " + type);
        }
    }

    private <T> void async_send(HttpMethod type, String url, Object json_content, AsyncHttpResponseHandler asyncHttpResponseHandler) {

        // params is a JSONObject
        StringEntity se = null;
        if (json_content != null) {
            try {
                String json = gson.toJson(json_content);
                se = new StringEntity(json);
                Dbg.logd(this.getClass().getName(), "Sending json payload: " + json);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //noinspection ConstantConditions
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        }

        if (type != HttpMethod.GET && !authenticated) {
            Dbg.logw(this.getClass().getName(), "No authentication details provided and about to execute a " +
                    "POST, DELETE or PUT request - this will probably fail!");
        }
        Header[] headers = new Header[0];
        if (authenticated) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
            headers = new Header[]{BasicScheme.authenticate(credentials, "UTF-8", false)};
        }

        if (!url.startsWith("http://")) {
            url = serverRoot + url;
        }
        switch (type) {
            case GET:
                httpClient.get(null, url, headers, null, asyncHttpResponseHandler);
                break;
            case POST:
                httpClient.post(null, url, headers, se, "application/json", asyncHttpResponseHandler);
                break;
            case DELETE:
                httpClient.delete(null, url, headers, asyncHttpResponseHandler);
                break;
            case PUT:
                httpClient.put(null, url, headers, se, "application/json", asyncHttpResponseHandler);
                break;
        }
    }

    protected <T> T parseResponse(String msgContent, Class<T> resultMsgType) throws APICallFailedException {
        Dbg.logd(this.getClass().getName(), msgContent);
        T resultMsg;
        try {
            resultMsg = gson.fromJson(msgContent, resultMsgType);
        } catch (JsonSyntaxException e) {
            Dbg.loge(this.getClass().getName(), "Could not parse json from msgContent: \n" + msgContent);
            resultMsg = null;
        }
        if (resultMsg == null) {
            throw new APICallFailedException("No decipherable message returned");
        }
        return resultMsg;
    }

}
