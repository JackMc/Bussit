package me.jackmccracken.bussit.utils;

/**
 * Defines a task which will be executed <b>on the main thread</b> after the execution of a
 * Reddit API call.
 * @param <T> The data to be returned from the server.
 */
public interface AfterCallTask<T> {
    /**
     * Called after a success.
     * @param param A function-defined parameter.
     */
    public void run(T param);

    /**
     * Called if the operation fails.
     *
     * @param message A user-readable message to be displayed if it is appropriate.
     */
    public void fail(String message);
}