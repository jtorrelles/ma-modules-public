/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.rt.event.detectors.PositiveCusumDetectorRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class PositiveCusumEventDetectorRTModelMapping implements RestModelMapping<PositiveCusumDetectorRT, PositiveCusumEventDetectorRTModel> {

    @Override
    public Class<? extends PositiveCusumDetectorRT> fromClass() {
        return PositiveCusumDetectorRT.class;
    }

    @Override
    public Class<? extends PositiveCusumEventDetectorRTModel> toClass() {
        return PositiveCusumEventDetectorRTModel.class;
    }

    @Override
    public PositiveCusumEventDetectorRTModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new PositiveCusumEventDetectorRTModel((PositiveCusumDetectorRT)from);
    }

}
