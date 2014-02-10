package communication;

import messages.FailureMsg;

public interface ResponseHandler<T> {

    public void onServerResponse(T msg);

    public void onServerDeniedRequest(FailureMsg failureMsg);
}
