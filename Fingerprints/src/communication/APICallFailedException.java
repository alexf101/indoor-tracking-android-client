package communication;

public class APICallFailedException extends Exception {
    public int failureCode;

    public APICallFailedException(String reasonForFailure, int failureCode) {
        super(reasonForFailure);
        this.failureCode = failureCode;
    }

    public APICallFailedException(String s) {
        super(s);
    }

    public APICallFailedException(Throwable throwable) {
        super(throwable);
    }
}
