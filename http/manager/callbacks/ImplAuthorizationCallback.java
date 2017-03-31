package com.ggec.voice.assistservice.http.manager.callbacks;

public abstract class ImplAuthorizationCallback<E> implements AuthorizationCallback{

        AsyncCallback<E, Exception> callback;

        public ImplAuthorizationCallback(AsyncCallback<E, Exception> callback){
            this.callback = callback;
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onError(Exception error) {
            if (callback != null) {
                //bubble up the error
                callback.failure(error);
            }
        }
    }