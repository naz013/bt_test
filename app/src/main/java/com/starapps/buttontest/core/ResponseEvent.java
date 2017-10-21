package com.starapps.buttontest.core;

import android.os.Message;

public class ResponseEvent {
    private Message msg;

    public ResponseEvent(Message msg) {
        this.msg = msg;
    }

    public Message getMsg() {
        return msg;
    }
}
