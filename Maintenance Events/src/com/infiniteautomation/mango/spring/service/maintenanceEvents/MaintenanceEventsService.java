/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service.maintenanceEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.rest.v2.model.StreamedArrayWithTotal;
import com.infiniteautomation.mango.rest.v2.model.StreamedVORqlQueryWithTotal;
import com.infiniteautomation.mango.spring.service.AbstractVOService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalStateException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventDao;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventRT;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventVO;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventsTableDefinition;
import com.serotonin.m2m2.maintenanceEvents.RTMDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.timer.CronTimerTrigger;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
@Service
public class MaintenanceEventsService extends AbstractVOService<MaintenanceEventVO, MaintenanceEventsTableDefinition, MaintenanceEventDao>{

    private final DataSourceDao dataSourceDao;

    @Autowired
    public MaintenanceEventsService(MaintenanceEventDao dao, DataSourceDao dataSourceDao, PermissionService permissionService) {
        super(dao, permissionService);
        this.dataSourceDao = dataSourceDao;
    }

    @Override
    public MaintenanceEventVO insert(MaintenanceEventVO vo)
            throws PermissionException, ValidationException {

        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, user);
        RTMDefinition.instance.saveMaintenanceEvent(vo);
        return vo;
    }

    @Override
    public MaintenanceEventVO update(MaintenanceEventVO existing, MaintenanceEventVO vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(vo, user);
        RTMDefinition.instance.saveMaintenanceEvent(vo);
        return vo;
    }

    @Override
    public MaintenanceEventVO update(String existingXid, MaintenanceEventVO vo)
            throws PermissionException, ValidationException {
        return update(get(existingXid), vo);
    }

    /**
     * Delete an event
     * @param event
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    @Override
    public MaintenanceEventVO delete(MaintenanceEventVO vo)
            throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureEditPermission(user, vo);
        RTMDefinition.instance.deleteMaintenanceEvent(vo.getId());
        return vo;
    }

    /**
     * Toggle a running maintenance event
     * @param xid
     * @return - state of event after toggle
     * @throws NotFoundException - if DNE
     * @throws PermissionException - if no toggle permission
     * @throws TranslatableIllegalStateException - if disabled
     */
    public boolean toggle(String xid) throws NotFoundException, PermissionException, TranslatableIllegalStateException {
        MaintenanceEventRT rt = getEventRT(xid);
        return rt.toggle();
    }

    /**
     * Check if a maintenance event is active
     * @param xid
     * @return - state of event
     * @throws NotFoundException - if DNE
     * @throws PermissionException - if no toggle permission
     * @throws TranslatableIllegalStateException - if disabled
     */
    public boolean isEventActive(String xid) throws NotFoundException, PermissionException, TranslatableIllegalStateException {
        MaintenanceEventRT rt = getEventRT(xid);
        return rt.isEventActive();
    }

    /**
     * Set the state of a Maintenance event, if state does not change do nothing.
     * @param xid
     * @param active
     * @return - state of event, should match active unless event is disabled
     * @throws NotFoundException - if DNE
     * @throws PermissionException - if no toggle permission
     * @throws TranslatableIllegalStateException - if disabled
     */
    public boolean setState(String xid, boolean active) throws NotFoundException, PermissionException, TranslatableIllegalStateException {
        MaintenanceEventRT rt = getEventRT(xid);
        if(active) {
            //Ensure active
            if(!rt.isEventActive())
                rt.toggle();
            return true;
        }else {
            if(rt.isEventActive())
                rt.toggle();
            return false;
        }
    }

    public MaintenanceEventRT getEventRT(String xid) throws NotFoundException, PermissionException, TranslatableIllegalStateException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        MaintenanceEventVO existing = dao.getByXid(xid);
        if(existing == null)
            throw new NotFoundException();
        ensureTogglePermission(existing, user);
        MaintenanceEventRT rt = RTMDefinition.instance.getRunningMaintenanceEvent(existing.getId());
        if (rt == null)
            throw new TranslatableIllegalStateException(new TranslatableMessage("maintenanceEvents.toggle.disabled"));
        return rt;
    }

    //TODO Mango 4.0 move to REST api
    public StreamedArrayWithTotal doQuery(ASTNode rql, PermissionHolder user, Function<MaintenanceEventVO, Object> transformVO) {

        //If we are admin or have overall data source permission we can view all
        if (user.hasAdminRole() || permissionService.hasDataSourcePermission(user)) {
            return new StreamedVORqlQueryWithTotal<>(this, rql, null, null, transformVO);
        } else {
            return new StreamedVORqlQueryWithTotal<>(this, rql, null, null, item -> {
                if(item.getDataPoints().size() > 0) {
                    DataPointPermissionsCheckCallback callback = new DataPointPermissionsCheckCallback(user, true);
                    dao.getPoints(item.getId(), callback);
                    if(!callback.hasPermission.booleanValue())
                        return false;
                }
                if(item.getDataSources().size() > 0) {
                    DataSourcePermissionsCheckCallback callback = new DataSourcePermissionsCheckCallback(user);
                    dao.getDataSources(item.getId(), callback);
                    if(!callback.hasPermission.booleanValue())
                        return false;
                }
                return true;
            },  transformVO);
        }
    }

    /**
     * Ensure the user has permission to toggle this event
     * @param user
     * @param vo
     */
    public void ensureTogglePermission(MaintenanceEventVO vo, PermissionHolder user) {
        if(user.hasAdminRole())
            return;
        else if(permissionService.hasDataSourcePermission(user))
            //TODO Review how this permission works
            return;
        else if(!permissionService.hasAnyRole(user, vo.getToggleRoles()));
        throw new PermissionException(new TranslatableMessage("maintenanceEvents.permission.unableToToggleEvent"), user);
    }


    /**
     * Check the permission on the data point and if the user does not have it
     * then cache and check the permission on the data source
     *
     * @author Terry Packer
     */
    class DataPointPermissionsCheckCallback implements MappedRowCallback<DataPointVO> {

        Map<Integer, DataSourceVO> sources = new HashMap<>();
        MutableBoolean hasPermission = new MutableBoolean(true);
        boolean read;
        PermissionHolder user;

        /**
         *
         * @param read = true to check read permission, false = check edit permission
         */
        public DataPointPermissionsCheckCallback(PermissionHolder user, boolean read) {
            this.user = user;
            this.read = read;
        }

        @Override
        public void row(DataPointVO point, int index) {

            if(!hasPermission.getValue()) {
                //short circuit the logic if we already failed
                return;
            }else {
                if(read) {
                    if(!permissionService.hasDataPointReadPermission(user, point)) {
                        DataSourceVO source = sources.computeIfAbsent(point.getDataSourceId(), k -> {
                            DataSourceVO newDs = dataSourceDao.get(k);
                            return newDs;
                        });
                        if(!permissionService.hasDataSourceEditPermission(user, source))
                            hasPermission.setFalse();
                    }
                }else {
                    DataSourceVO source = sources.computeIfAbsent(point.getDataSourceId(), k -> {
                        DataSourceVO newDs = dataSourceDao.get(k);
                        return newDs;
                    });
                    if(!permissionService.hasDataSourceEditPermission(user, source))
                        hasPermission.setFalse();
                }
            }
        }
    }

    /**
     * Does the user have edit permission for all data sources
     *
     * @author Terry Packer
     */
    class DataSourcePermissionsCheckCallback implements MappedRowCallback<DataSourceVO> {

        MutableBoolean hasPermission = new MutableBoolean(true);
        PermissionHolder user;

        /**
         *
         * @param read = true to check read permission, false = check edit permission
         */
        public DataSourcePermissionsCheckCallback(PermissionHolder user) {
            this.user = user;
        }

        @Override
        public void row(DataSourceVO source, int index) {

            if(!hasPermission.getValue()) {
                //short circuit the logic if we already failed
                return;
            }else {
                if(!permissionService.hasDataSourceEditPermission(user, source))
                    hasPermission.setFalse();
            }
        }
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, MaintenanceEventVO vo) {
        return permissionService.hasDataSourcePermission(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, MaintenanceEventVO vo) {
        if(user.hasAdminRole())
            return true;
        else if(permissionService.hasDataSourcePermission(user))
            //TODO Review how this permission works
            return true;
        else {
            if(vo.getDataPoints().size() > 0) {
                DataPointPermissionsCheckCallback callback = new DataPointPermissionsCheckCallback(user, false);
                dao.getPoints(vo.getId(), callback);
                if(!callback.hasPermission.booleanValue())
                    return false;
            }

            if(vo.getDataSources().size() > 0) {
                DataSourcePermissionsCheckCallback callback = new DataSourcePermissionsCheckCallback(user);
                dao.getDataSources(vo.getId(), callback);
                if(!callback.hasPermission.booleanValue())
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, MaintenanceEventVO vo) {
        if(user.hasAdminRole())
            return true;
        else if(permissionService.hasDataSourcePermission(user))
            //TODO Review how this permission works
            return true;
        else {
            if(vo.getDataPoints().size() > 0) {
                DataPointPermissionsCheckCallback callback = new DataPointPermissionsCheckCallback(user, true);
                dao.getPoints(vo.getId(), callback);
                if(!callback.hasPermission.booleanValue())
                    return false;
            }

            if(vo.getDataSources().size() > 0) {
                DataSourcePermissionsCheckCallback callback = new DataSourcePermissionsCheckCallback(user);
                dao.getDataSources(vo.getId(), callback);
                if(!callback.hasPermission.booleanValue())
                    return false;
            }
        }
        return true;
    }

    @Override
    public ProcessResult validate(MaintenanceEventVO vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        permissionService.validateVoRoles(response, "toggleRoles", user, false, null, vo.getToggleRoles());
        return response;
    }

    @Override
    public ProcessResult validate(MaintenanceEventVO existing, MaintenanceEventVO vo,
            PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "toggleRoles", user, false, existing.getToggleRoles(), vo.getToggleRoles());
        return result;
    }

    public ProcessResult commonValidation(MaintenanceEventVO vo, PermissionHolder user) {
        ProcessResult response = super.validate(vo, user);

        if((vo.getDataSources().size() < 1) &&(vo.getDataPoints().size() < 1)) {
            response.addContextualMessage("dataSources", "validate.invalidValue");
            response.addContextualMessage("dataPoints", "validate.invalidValue");
        }

        //Validate that the ids are legit
        for(int i=0; i<vo.getDataSources().size(); i++) {
            DataSourceVO ds = DataSourceDao.getInstance().get(vo.getDataSources().get(i));
            if(ds == null) {
                response.addContextualMessage("dataSources[" + i + "]", "validate.invalidValue");
            }
        }

        for(int i=0; i<vo.getDataPoints().size(); i++) {
            DataPointVO dp = DataPointDao.getInstance().get(vo.getDataPoints().get(i));
            if(dp == null) {
                response.addContextualMessage("dataPoints[" + i + "]", "validate.invalidValue");
            }
        }

        // Check that cron patterns are ok.
        if (vo.getScheduleType() == MaintenanceEventVO.TYPE_CRON) {
            try {
                new CronTimerTrigger(vo.getActiveCron());
            }
            catch (Exception e) {
                response.addContextualMessage("activeCron", "maintenanceEvents.validate.activeCron", e.getMessage());
            }

            try {
                new CronTimerTrigger(vo.getInactiveCron());
            }
            catch (Exception e) {
                response.addContextualMessage("inactiveCron", "maintenanceEvents.validate.inactiveCron", e.getMessage());
            }
        }

        // Test that the triggers can be created.
        MaintenanceEventRT rt = new MaintenanceEventRT(vo);
        try {
            rt.createTrigger(true);
        }
        catch (RuntimeException e) {
            response.addContextualMessage("activeCron", "maintenanceEvents.validate.activeTrigger", e.getMessage());
        }

        try {
            rt.createTrigger(false);
        }
        catch (RuntimeException e) {
            response.addContextualMessage("inactiveCron", "maintenanceEvents.validate.inactiveTrigger", e.getMessage());
        }

        // If the event is once, make sure the active time is earlier than the inactive time.
        if (vo.getScheduleType() == MaintenanceEventVO.TYPE_ONCE) {
            DateTime adt = new DateTime(
                    vo.getActiveYear(),
                    vo.getActiveMonth(),
                    vo.getActiveDay(),
                    vo.getActiveHour(),
                    vo.getActiveMinute(),
                    vo.getActiveSecond(), 0);
            DateTime idt = new DateTime(
                    vo.getInactiveYear(),
                    vo.getInactiveMonth(),
                    vo.getInactiveDay(),
                    vo.getInactiveHour(),
                    vo.getInactiveMinute(),
                    vo.getInactiveSecond(), 0);
            if (idt.getMillis() <= adt.getMillis())
                response.addContextualMessage("scheduleType", "maintenanceEvents.validate.invalidRtn");
            if(vo.getTimeoutPeriods() > 0) {
                if (!Common.TIME_PERIOD_CODES.isValidId(vo.getTimeoutPeriods()))
                    response.addContextualMessage("updatePeriodType", "validate.invalidValue");
            }
        }
        return response;
    }
}
