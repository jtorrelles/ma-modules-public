/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.rt.event.detectors.AnalogChangeDetectorRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class AnalogChangeEventDetectorRTModelMapping implements RestModelMapping<AnalogChangeDetectorRT, AnalogChangeEventDetectorRTModel> {

    @Override
    public Class<? extends AnalogChangeDetectorRT> fromClass() {
        return AnalogChangeDetectorRT.class;
    }

    @Override
    public Class<? extends AnalogChangeEventDetectorRTModel> toClass() {
        return AnalogChangeEventDetectorRTModel.class;
    }

    @Override
    public AnalogChangeEventDetectorRTModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new AnalogChangeEventDetectorRTModel((AnalogChangeDetectorRT)from);
    }

}
