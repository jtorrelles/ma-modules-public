/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.maintenanceEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class MaintenanceEventTypeDefinition extends EventTypeDefinition {
    @Override
    public String getTypeName() {
        return MaintenanceEventType.TYPE_NAME;
    }

    @Override
    public Class<? extends EventType> getEventTypeClass() {
        return MaintenanceEventType.class;
    }

    @Override
    public EventType createEventType(String subtype, int ref1, int ref2) {
        return new MaintenanceEventType(ref1);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user) {
        return user.hasAdminRole();
    }

    @Override
    public List<EventTypeVO> getEventTypeVOs(PermissionHolder user, PermissionService service) {
        List<EventTypeVO> vos = new ArrayList<EventTypeVO>();
        for (MaintenanceEventVO me : MaintenanceEventDao.getInstance().getAll())
            if(service.hasEventTypePermission(user, me.getEventType().getEventType()))
                vos.add(me.getEventType());

        return vos;
    }

    @Override
    public String getDescriptionKey() {
        return "maintenanceEvents.mes";
    }

    @Override
    public TranslatableMessage getSourceDisabledMessage() {
        return new TranslatableMessage("event.rtn.maintDisabled");
    }

    @Override
    public List<String> getEventSubTypes(PermissionHolder user) {
        return Collections.emptyList();
    }

    @Override
    public boolean supportsReferenceId1() {
        return true;
    }

    @Override
    public boolean supportsReferenceId2() {
        return false;
    }

    @Override
    public boolean supportsSubType() {
        return false;
    }

    @Override
    public List<EventTypeVO> generatePossibleEventTypesWithReferenceId1(PermissionHolder user,
            String subtype, PermissionService service) {
        List<EventTypeVO> vos = new ArrayList<EventTypeVO>();
        for (MaintenanceEventVO me : MaintenanceEventDao.getInstance().getAll())
            if(service.hasEventTypePermission(user, me.getEventType().getEventType()) && StringUtils.equals(me.getEventType().getEventType().getEventSubtype(), subtype))
                vos.add(me.getEventType());
        return vos;
    }
}
