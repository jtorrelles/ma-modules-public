/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.infiniteautomation.mango.spring.service.maintenanceEvents.MaintenanceEventsService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventDao;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventVO;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventsTableDefinition;
import com.serotonin.m2m2.maintenanceEvents.RTMDefinition;
import com.serotonin.m2m2.maintenanceEvents.SchemaDefinition;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class MaintenanceEventsServiceTest extends AbstractVOServiceWithPermissionsTest<MaintenanceEventVO, MaintenanceEventsTableDefinition, MaintenanceEventDao, MaintenanceEventsService> {

    @BeforeClass
    public static void setup() {
        List<ModuleElementDefinition> definitions = new ArrayList<>();
        definitions.add(new SchemaDefinition());
        definitions.add(new RTMDefinition());
        addModule("maintenanceEvents", definitions);
    }

    @Override
    String getCreatePermissionType() {
        return DataSourcePermissionDefinition.PERMISSION;
    }

    @Override
    void setReadRoles(Set<Role> roles, MaintenanceEventVO vo) {
        //A user with read permission for all data points (and sources) in this event has read permission
        if(roles != null) {
            getService().permissionService.runAsSystemAdmin(() -> {
                //Get the data points and add our roles to the read roles
                for(int dpId : vo.getDataPoints()) {
                    DataPointVO dp = DataPointDao.getInstance().get(dpId);
                    dp.setReadRoles(roles);
                    DataPointDao.getInstance().update(dp.getId(), dp);
                }
            });
        }
        vo.setToggleRoles(roles);
    }

    @Override
    void addReadRoleToFail(Role role, MaintenanceEventVO vo) {
        vo.getToggleRoles().add(role);
    }

    @Override
    void setEditRoles(Set<Role> roles, MaintenanceEventVO vo) {
        //A user with edit permission for the sources of all points (and all data sources sources) in this event has edit permission
        if(roles != null) {
            getService().permissionService.runAsSystemAdmin(() -> {
                //Get the data points and add our roles to the read roles
                for(int dpId : vo.getDataPoints()) {
                    DataPointVO dp = DataPointDao.getInstance().get(dpId);
                    DataSourceVO ds = DataSourceDao.getInstance().get(dp.getDataSourceId());
                    ds.setEditRoles(roles);
                    DataSourceDao.getInstance().update(ds.getId(), ds);
                }
            });
        }
        vo.setToggleRoles(roles);
    }

    @Override
    void addEditRoleToFail(Role role, MaintenanceEventVO vo) {
        vo.getToggleRoles().add(role);
    }

    @Override
    MaintenanceEventsService getService() {
        return Common.getBean(MaintenanceEventsService.class);
    }

    @Override
    MaintenanceEventDao getDao() {
        return MaintenanceEventDao.getInstance();
    }

    @Override
    void assertVoEqual(MaintenanceEventVO expected, MaintenanceEventVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
    }

    @Override
    MaintenanceEventVO newVO(User owner) {
        MaintenanceEventVO vo = new MaintenanceEventVO();
        vo.setName("testing name");
        for(IDataPoint point : createMockDataPoints(5)) {
            vo.getDataPoints().add(point.getId());
        }
        return vo;
    }

    @Override
    MaintenanceEventVO updateVO(MaintenanceEventVO existing) {
        MaintenanceEventVO vo = (MaintenanceEventVO)existing.copy();
        vo.setName("testing");
        return vo;
    }

    @Override
    @Test()
    public void testCannotRemoveEditAccess() {
        //This test does not apply
    }

    @Override
    @Test()
    public void testAddEditRoleUserDoesNotHave() {
        //This test does not apply
    }

    //TODO Test Add/Remove/Use Toggle Permission

    @Override
    String getReadRolesContextKey() {
        return "toggleRoles";
    }

    @Override
    String getEditRolesContextKey() {
        return "toggleRoles";
    }

    @Test
    @Override
    public void testAddReadRoleUserDoesNotHave() {
        runTest(() -> {
            MaintenanceEventVO vo = newVO(readUser);
            setReadRoles(Collections.singleton(roleService.getUserRole()), vo);
            setEditRoles(Collections.singleton(roleService.getUserRole()), vo);
            getService().permissionService.runAsSystemAdmin(() -> {
                service.insert(vo);
            });
            getService().permissionService.runAs(readUser, () -> {
                MaintenanceEventVO fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                vo.setToggleRoles(Collections.singleton(roleService.getSuperadminRole()));
                service.update(fromDb.getId(), fromDb);
            });

        }, getReadRolesContextKey(), getReadRolesContextKey());
    }

}
