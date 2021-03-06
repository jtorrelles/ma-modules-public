/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.rt.event.detectors.NegativeCusumDetectorRT;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class NegativeCusumEventDetectorRTModelMapping implements RestModelMapping<NegativeCusumDetectorRT, NegativeCusumEventDetectorRTModel> {

    @Override
    public Class<? extends NegativeCusumDetectorRT> fromClass() {
        return NegativeCusumDetectorRT.class;
    }

    @Override
    public Class<? extends NegativeCusumEventDetectorRTModel> toClass() {
        return NegativeCusumEventDetectorRTModel.class;
    }

    @Override
    public NegativeCusumEventDetectorRTModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new NegativeCusumEventDetectorRTModel((NegativeCusumDetectorRT)from);
    }

}
