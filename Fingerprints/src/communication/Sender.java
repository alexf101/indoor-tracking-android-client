package communication;

import datatypes.Building;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Sender {

    public void setUser(String username, String password);

    public void async_send_no_reply(HttpMethod type, String url, final AcknowledgedTask acknowledgedTask);

    public <T> void async_send(HttpMethod type, String url, Object json_content, final Class<T> resultMsgType, final ResponseHandler<T> asyncHttpResponseHandler) throws InterruptedException, ExecutionException, TimeoutException;

    public <T> T sync_send(HttpMethod type, String url, Object json_content, Class<T> resultType);

    public void close();
}
