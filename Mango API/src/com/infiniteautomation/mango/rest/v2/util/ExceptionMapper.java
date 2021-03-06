/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.util;

/**
 * @author Jared Wiltshire
 */
public interface ExceptionMapper<E> {
    public E mapException(Throwable e);
}
