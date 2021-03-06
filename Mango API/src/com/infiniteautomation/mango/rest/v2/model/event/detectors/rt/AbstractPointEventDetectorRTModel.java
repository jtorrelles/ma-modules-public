/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 *
 * @author Terry Packer
 */
public abstract class AbstractPointEventDetectorRTModel<T extends AbstractPointEventDetectorVO> extends AbstractEventDetectorRTModel<T> {

    public AbstractPointEventDetectorRTModel(PointEventDetectorRT<T> rt) {
        super(rt);
    }

}
