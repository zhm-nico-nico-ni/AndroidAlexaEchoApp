package com.willblaschko.android.alexa.callbacks;

/**
 * A generic callback to handle four states of asynchronous operations
 */
public interface AsyncCallback<D, E>{
    void start();
    void handle(D result);
    void success(D result);
    void failure(E error);
    void complete();
}
