/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.rt.event.detectors.AlphanumericRegexStateDetectorRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class AlphanumericRegexStateEventDetectorRTModelMapping implements RestModelMapping<AlphanumericRegexStateDetectorRT, AlphanumericRegexStateEventDetectorRTModel> {

    @Override
    public Class<? extends AlphanumericRegexStateDetectorRT> fromClass() {
        return AlphanumericRegexStateDetectorRT.class;
    }

    @Override
    public Class<? extends AlphanumericRegexStateEventDetectorRTModel> toClass() {
        return AlphanumericRegexStateEventDetectorRTModel.class;
    }

    @Override
    public AlphanumericRegexStateEventDetectorRTModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new AlphanumericRegexStateEventDetectorRTModel((AlphanumericRegexStateDetectorRT)from);
    }

}
