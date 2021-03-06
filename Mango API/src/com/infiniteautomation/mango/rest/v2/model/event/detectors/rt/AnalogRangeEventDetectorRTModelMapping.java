/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.rt.event.detectors.AnalogRangeDetectorRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class AnalogRangeEventDetectorRTModelMapping implements RestModelMapping<AnalogRangeDetectorRT, AnalogRangeEventDetectorRTModel> {

    @Override
    public Class<? extends AnalogRangeDetectorRT> fromClass() {
        return AnalogRangeDetectorRT.class;
    }

    @Override
    public Class<? extends AnalogRangeEventDetectorRTModel> toClass() {
        return AnalogRangeEventDetectorRTModel.class;
    }

    @Override
    public AnalogRangeEventDetectorRTModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new AnalogRangeEventDetectorRTModel((AnalogRangeDetectorRT)from);
    }

}
