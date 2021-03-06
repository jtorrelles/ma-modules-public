/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.dataPoint.textRenderer;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelJacksonMapping;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.serotonin.m2m2.view.text.RangeRenderer;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class RangeTextRendererModelMapping implements RestModelJacksonMapping<RangeRenderer, RangeTextRendererModel> {

    @Override
    public Class<? extends RangeRenderer> fromClass() {
        return RangeRenderer.class;
    }

    @Override
    public Class<? extends RangeTextRendererModel> toClass() {
        return RangeTextRendererModel.class;
    }

    @Override
    public RangeTextRendererModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new RangeTextRendererModel((RangeRenderer)from);
    }

    @Override
    public String getTypeName() {
        return RangeRenderer.getDefinition().getName();
    }

}
