package com.willblaschko.android.alexa.data;

class Header {
    protected String namespace;
    protected String name;
    protected String messageId;
    protected String dialogRequestId;

    public String getNamespace() {
        return namespace;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getDialogRequestId() {
        return dialogRequestId;
    }

    public void setDialogRequestId(String dialogRequestId) {
        this.dialogRequestId = dialogRequestId;
    }
}