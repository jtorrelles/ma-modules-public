/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.websockets;

/**
 * @author Jared Wiltshire
 */
public class WebSocketSendException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WebSocketSendException() {
        super();
    }

    public WebSocketSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketSendException(String message) {
        super(message);
    }

    public WebSocketSendException(Throwable cause) {
        super(cause);
    }
}
