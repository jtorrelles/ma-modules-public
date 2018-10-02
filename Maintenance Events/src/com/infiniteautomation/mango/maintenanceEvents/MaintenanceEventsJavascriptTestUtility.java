/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.maintenanceEvents;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalStateException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventDao;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventRT;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventVO;
import com.serotonin.m2m2.maintenanceEvents.RTMDefinition;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Terry Packer
 *
 */
public class MaintenanceEventsJavascriptTestUtility extends MaintenanceEventsJavascriptUtility{

    
    @Override
    public boolean toggle(String xid)
            throws NotFoundException, PermissionException, TranslatableIllegalStateException {
        MaintenanceEventVO existing = MaintenanceEventDao.getInstance().getByXid(xid);
        if(existing == null)
            throw new NotFoundException();
        service.ensureTogglePermission(existing, permissions);
        MaintenanceEventRT rt = RTMDefinition.instance.getRunningMaintenanceEvent(existing.getId());
        if (rt == null)
            throw new TranslatableIllegalStateException(new TranslatableMessage("maintenanceEvents.toggle.disabled"));
        else {
            return !rt.isEventActive();
        }
    }
    
    @Override
    public MaintenanceEventVO insert(MaintenanceEventVO vo)
            throws NotFoundException, PermissionException, ValidationException {
        //Ensure they can create an event
        Permissions.ensureDataSourcePermission(permissions);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(MaintenanceEventDao.getInstance().generateUniqueXid());
        
        vo.ensureValid();
        return vo;
    }
    
    @Override
    public MaintenanceEventVO update(MaintenanceEventVO existing, MaintenanceEventVO vo)
            throws NotFoundException, PermissionException, ValidationException {
        service.ensureEditPermission(existing, permissions);
        //Don't change ID ever
        vo.setId(existing.getId());
        vo.ensureValid();
        return vo;
    }
    
    @Override
    public MaintenanceEventVO update(String existingXid, MaintenanceEventVO vo)
            throws NotFoundException, PermissionException, ValidationException {
        MaintenanceEventVO existing = service.getFullByXid(existingXid, permissions);
        service.ensureEditPermission(existing, permissions);
        //Don't change ID ever
        vo.setId(existing.getId());
        vo.ensureValid();
        return vo;
    }
    
    @Override
    public MaintenanceEventVO delete(String xid) throws NotFoundException, PermissionException {
        MaintenanceEventVO vo = service.getFullByXid(xid, permissions);
        service.ensureEditPermission(vo, permissions);
        return vo;
    }
}