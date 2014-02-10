package communication;

/**
 * Represents a task to run on receipt of a server reply that is expected to have no body, i.e. a response to a Delete
 * request.
 *
 */
public interface AcknowledgedTask {
    public void onServerAcknowledge();
}
