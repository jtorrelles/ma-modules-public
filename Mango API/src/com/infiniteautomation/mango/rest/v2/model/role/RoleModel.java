/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.role;

import com.infiniteautomation.mango.rest.v2.model.AbstractVoModel;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
public class RoleModel extends AbstractVoModel<RoleVO> {
    
    public RoleModel(RoleVO vo) {
        fromVO(vo);
    }
    
    public RoleModel() {
        
    }
    
    /**
     * Create a vo from our fields
     * @return
     */
    @Override
    public RoleVO toVO() throws ValidationException {
        return new RoleVO(id, xid, name);
    }
    
    @Override
    protected RoleVO newVO() {
        throw new UnsupportedOperationException("not implemented");
    }

}
