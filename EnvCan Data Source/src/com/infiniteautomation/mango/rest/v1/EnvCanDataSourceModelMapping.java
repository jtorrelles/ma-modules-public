/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v1;

import org.springframework.stereotype.Component;

import com.serotonin.m2m2.envcan.EnvCanDataSourceModel;
import com.serotonin.m2m2.envcan.EnvCanDataSourceVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.model.RestModelMapper;
import com.serotonin.m2m2.web.mvc.rest.v1.model.RestModelMapping;

/**
 * @author Terry Packer
 *
 */
@Component
public class EnvCanDataSourceModelMapping implements RestModelMapping<EnvCanDataSourceVO, EnvCanDataSourceModel> {

    @Override
    public Class<? extends EnvCanDataSourceVO> fromClass() {
        return EnvCanDataSourceVO.class;
    }

    @Override
    public Class<? extends EnvCanDataSourceModel> toClass() {
        return EnvCanDataSourceModel.class;
    }

    @Override
    public EnvCanDataSourceModel map(Object from, User user, RestModelMapper mapper) {
        return new EnvCanDataSourceModel((EnvCanDataSourceVO)from);
    }

}
