/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.rest.v2.websocket.pointValue;

/**
 * @author Terry Packer
 *
 */
public enum PointValueEventType {

    INITIALIZE,
    UPDATE, //Value was updated to possibly the same value
    CHANGE, //Value has changed
    SET,
    BACKDATE,
    TERMINATE,
    REGISTERED, //We registered and this is our first response
    ATTRIBUTE_CHANGE,
    LOGGED

}
