/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.maintenanceEvents;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.maintenanceEvents.MaintenanceEventsService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalStateException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventDao;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventRT;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventVO;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Terry Packer
 *
 */
public class MaintenanceEventsJavascriptTestUtility extends MaintenanceEventsJavascriptUtility{

    @Autowired
    public MaintenanceEventsJavascriptTestUtility(MangoJavaScriptService service, PermissionService permissionService, MaintenanceEventsService meService) {
        super(service, permissionService, meService);
    }

    @Override
    public boolean toggle(String xid)
            throws NotFoundException, PermissionException, TranslatableIllegalStateException {
        return this.permissionService.runAs(permissions, () -> {
            MaintenanceEventRT rt = meService.getEventRT(xid);
            return !rt.isEventActive();
        });
    }

    @Override
    public boolean setState(String xid, boolean state) {

        return this.permissionService.runAs(permissions, () -> {
            meService.getEventRT(xid); //check that it's enabled, we have toggle permissions
            return state;
        });
    }

    @Override
    public MaintenanceEventVO insert(MaintenanceEventVO vo)
            throws NotFoundException, PermissionException, ValidationException {
        //Ensure they can create an event
        permissionService.ensureDataSourcePermission(permissions);

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(MaintenanceEventDao.getInstance().generateUniqueXid());

        meService.ensureValid(vo, permissions);
        return vo;
    }

    @Override
    public MaintenanceEventVO update(MaintenanceEventVO existing, MaintenanceEventVO vo)
            throws NotFoundException, PermissionException, ValidationException {
        meService.ensureEditPermission(permissions, existing);
        //Don't change ID ever
        vo.setId(existing.getId());
        meService.ensureValid(vo, permissions);
        return vo;
    }

    @Override
    public MaintenanceEventVO update(String existingXid, MaintenanceEventVO vo)
            throws NotFoundException, PermissionException, ValidationException {
        return this.permissionService.runAs(permissions, () -> {
            MaintenanceEventVO existing = meService.get(existingXid);
            meService.ensureEditPermission(permissions, existing);
            //Don't change ID ever
            vo.setId(existing.getId());
            meService.ensureValid(vo, permissions);
            return vo;
        });
    }

    @Override
    public MaintenanceEventVO delete(String xid) throws NotFoundException, PermissionException {
        return this.permissionService.runAs(permissions, () -> {
            MaintenanceEventVO vo = meService.get(xid);
            meService.ensureEditPermission(permissions, vo);
            return vo;
        });
    }
}
