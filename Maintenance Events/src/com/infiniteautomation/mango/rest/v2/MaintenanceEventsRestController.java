/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.rest.v2.EventsRestController.EventTableRqlMappings;
import com.infiniteautomation.mango.rest.v2.model.EventQueryByMaintenanceCriteria;
import com.infiniteautomation.mango.rest.v2.model.EventQueryByMaintenanceEventRql;
import com.infiniteautomation.mango.rest.v2.model.MaintenanceEventModel;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.StreamedArray;
import com.infiniteautomation.mango.rest.v2.model.StreamedArrayWithTotal;
import com.infiniteautomation.mango.rest.v2.model.StreamedVORqlQueryWithTotal;
import com.infiniteautomation.mango.rest.v2.model.event.EventInstanceModel;
import com.infiniteautomation.mango.rest.v2.patch.PatchVORequestBody;
import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.infiniteautomation.mango.spring.service.EventInstanceService;
import com.infiniteautomation.mango.spring.service.maintenanceEvents.MaintenanceEventsService;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventDao;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventType;
import com.serotonin.m2m2.maintenanceEvents.MaintenanceEventVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventInstanceVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
@Api(value="Maintenance Events API")
@RestController()
@RequestMapping("/maintenance-events")
public class MaintenanceEventsRestController {

    private final EventInstanceService eventService;
    private final Map<String, Function<Object, Object>> eventTableValueConverters;
    private final Map<String, Field<?>> eventTableFieldMap;
    private final BiFunction<EventInstanceVO, User, EventInstanceModel> eventMap;

    private final MaintenanceEventDao dao;
    private final MaintenanceEventsService service;

    @Autowired
    public MaintenanceEventsRestController(MaintenanceEventsService service,
            MaintenanceEventDao dao,
            RestModelMapper modelMapper, EventInstanceService eventService,
            EventInstanceTableDefinition eventTable) {
        this.service = service;
        this.dao = dao;
        this.eventService = eventService;
        this.eventTableValueConverters = new HashMap<>();
        this.eventTableFieldMap = new EventTableRqlMappings(eventTable);

        this.eventMap = (vo, user) -> {
            return modelMapper.map(vo, EventInstanceModel.class, user);
        };
    }

    @ApiOperation(
            value = "Get maintenance event by XID",
            notes = "Only events that user has permission to are returned"
            )
    @RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public MaintenanceEventModel get(
            @ApiParam(value = "Valid XID", required = true, allowMultiple = false)
            @PathVariable String xid,
            @AuthenticationPrincipal User user) {
        return new MaintenanceEventModel(service.get(xid));
    }

    @ApiOperation(value = "Partially update an existing maintenance event")
    @RequestMapping(method = RequestMethod.PATCH, value = "/{xid}")
    public ResponseEntity<MaintenanceEventModel> partialUpdate(
            @PathVariable String xid,

            @ApiParam(value = "Updated maintenance event", required = true)
            @PatchVORequestBody(
                    service=MaintenanceEventsService.class,
                    modelClass=MaintenanceEventModel.class)
            MaintenanceEventModel model,

            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {

        MaintenanceEventVO vo = service.update(xid, model.toVO());

        URI location = builder.path("/maintenance-events/{xid}").buildAndExpand(vo.getXid()).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(new MaintenanceEventModel(vo), headers, HttpStatus.OK);
    }

    @ApiOperation(value = "Update an existing maintenance event")
    @RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<MaintenanceEventModel> update(
            @PathVariable String xid,

            @ApiParam(value = "Updated maintenance event", required = true)
            @RequestBody(required=true) MaintenanceEventModel model,

            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {
        MaintenanceEventVO vo = service.update(xid, model.toVO());
        URI location = builder.path("/maintenance-events/{xid}").buildAndExpand(vo.getXid()).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(new MaintenanceEventModel(vo), headers, HttpStatus.OK);
    }

    @ApiOperation(value = "Create new maintenance event", notes="User must have global data source permission")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<MaintenanceEventModel> create(
            @ApiParam(value = "Updated maintenance event", required = true)
            @RequestBody(required=true) MaintenanceEventModel model,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {

        MaintenanceEventVO vo = service.insert(model.toVO());

        URI location = builder.path("/maintenance-events/{xid}").buildAndExpand(vo.getXid()).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(new MaintenanceEventModel(vo), headers, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete a maintenance event")
    @RequestMapping(method = RequestMethod.DELETE, value = "/{xid}")
    public MaintenanceEventModel delete(
            @ApiParam(value = "Valid maintenance event XID", required = true, allowMultiple = false)
            @PathVariable String xid,
            @AuthenticationPrincipal User user) {
        return new MaintenanceEventModel(service.delete(xid));
    }

    @ApiOperation(value = "Toggle the state of a maintenance event", notes="must have toggle permission, returns boolean state of event")
    @RequestMapping(method = RequestMethod.PUT, value = "/toggle/{xid}")
    public ResponseEntity<Boolean> toggle(
            @PathVariable String xid,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {
        boolean activated = service.toggle(xid);
        URI location = builder.path("/maintenance-events/{xid}").buildAndExpand(xid).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(activated, headers, HttpStatus.OK);
    }

    @ApiOperation(value = "Get the current active state of a maintenance event", notes="must have toggle permission, returns new boolean state of event")
    @RequestMapping(method = RequestMethod.GET, value = "/active/{xid}")
    public ResponseEntity<Boolean> getState(@PathVariable String xid, @AuthenticationPrincipal User user) {
        return new ResponseEntity<>(service.isEventActive(xid), HttpStatus.OK);
    }

    @ApiOperation(value = "Set the state of a maintenance event, only change state if necessary ignore if no change and just return current state", notes="must have toggle permission, returns new boolean state of event")
    @RequestMapping(method = RequestMethod.PUT, value = "/active/{xid}")
    public ResponseEntity<Boolean> setState(
            @PathVariable String xid,
            @ApiParam(value = "State to set event to", required=true)
            @RequestParam(value="active", required=true, defaultValue="false") boolean active,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {

        boolean activated = service.setState(xid, active);
        URI location = builder.path("/maintenance-events/{xid}").buildAndExpand(xid).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(activated, headers, HttpStatus.OK);
    }


    @ApiOperation(
            value = "Query Maintenance Events",
            notes = "Use RQL formatted query",
            response=MaintenanceEventModel.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.GET)
    public StreamedArrayWithTotal queryRQL(
            HttpServletRequest request,
            @AuthenticationPrincipal User user) {

        ASTNode rql = RQLUtils.parseRQLtoAST(request.getQueryString());
        return service.doQuery(rql, user, transformVisit);
    }

    @ApiOperation(
            value = "Find Maintenance Events linked to data points by point IDs",
            notes = "Returns a map of point ids to a list of events that have this data point in their list OR the its data source in the list",
            response=Map.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.GET, value="/query/get-for-points-by-ids/{pointIds}")
    public Map<Integer, List<MaintenanceEventModel>> getForPointsByIds(
            @PathVariable(required = true) List<Integer> pointIds,
            HttpServletRequest request,
            @AuthenticationPrincipal User user) {

        Map<Integer, List<MaintenanceEventModel>> map = new HashMap<>();
        for(Integer id: pointIds) {
            List<MaintenanceEventModel> models = new ArrayList<>();
            map.put(id, models);
            dao.getForDataPoint(id, new MappedRowCallback<MaintenanceEventVO>() {

                @Override
                public void row(MaintenanceEventVO vo, int index) {
                    MaintenanceEventModel model = new MaintenanceEventModel(vo);
                    fillDataPoints(model);
                    fillDataSources(model);
                    models.add(model);
                }

            });
        }
        return map;
    }

    @ApiOperation(
            value = "Find Maintenance Events linked to data points by point XIDs",
            notes = "Returns a map of point xids to a list of events that have this data point in their list OR the its data source in the list",
            response=Map.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.GET, value="/query/get-for-points-by-xids/{pointXids}")
    public Map<String, List<MaintenanceEventModel>> getForPointsByXid(
            @PathVariable(required = true) List<String> pointXids,
            HttpServletRequest request,
            @AuthenticationPrincipal User user) {

        Map<String, List<MaintenanceEventModel>> map = new HashMap<>();
        for(String xid: pointXids) {
            List<MaintenanceEventModel> models = new ArrayList<>();
            map.put(xid, models);
            dao.getForDataPoint(xid, new MappedRowCallback<MaintenanceEventVO>() {

                @Override
                public void row(MaintenanceEventVO vo, int index) {
                    MaintenanceEventModel model = new MaintenanceEventModel(vo);
                    fillDataPoints(model);
                    fillDataSources(model);
                    models.add(model);
                }

            });
        }
        return map;
    }

    @ApiOperation(
            value = "Find Maintenance Events linked to data sources by source IDs",
            notes = "Returns a map of source ids to a list of events that have this data source in thier list",
            response=Map.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.GET, value="/query/get-for-sources-by-ids/{sourceIds}")
    public Map<Integer, List<MaintenanceEventModel>> getForSourcesByIds(
            @PathVariable(required = true) List<Integer> sourceIds,
            HttpServletRequest request,
            @AuthenticationPrincipal User user) {

        Map<Integer, List<MaintenanceEventModel>> map = new HashMap<>();
        for(Integer id: sourceIds) {
            List<MaintenanceEventModel> models = new ArrayList<>();
            map.put(id, models);
            dao.getForDataSource(id, new MappedRowCallback<MaintenanceEventVO>() {

                @Override
                public void row(MaintenanceEventVO vo, int index) {
                    MaintenanceEventModel model = new MaintenanceEventModel(vo);
                    fillDataPoints(model);
                    fillDataSources(model);
                    models.add(model);
                }

            });
        }
        return map;
    }

    @ApiOperation(
            value = "Find Maintenance Events linked to data sources by source XIDs",
            notes = "Returns a map of source xids to a list of events that have this data source in thier list",
            response=Map.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.GET, value="/query/get-for-sources-by-xids/{sourceXids}")
    public Map<String, List<MaintenanceEventModel>> getForSourcesByXid(
            @PathVariable(required = true) List<String> sourceXids,
            HttpServletRequest request,
            @AuthenticationPrincipal User user) {

        Map<String, List<MaintenanceEventModel>> map = new HashMap<>();
        for(String xid: sourceXids) {
            List<MaintenanceEventModel> models = new ArrayList<>();
            map.put(xid, models);
            if(xid != null) {
                dao.getForDataSource(xid, new MappedRowCallback<MaintenanceEventVO>() {

                    @Override
                    public void row(MaintenanceEventVO vo, int index) {
                        MaintenanceEventModel model = new MaintenanceEventModel(vo);
                        fillDataPoints(model);
                        fillDataSources(model);
                        models.add(model);
                    }

                });
            }
        }
        return map;
    }

    @ApiOperation(
            value = "Find Events for a set of Maintenance events created by the supplied rql query",
            notes = "Returns Events for any Maintenance event that",
            response=EventInstanceModel.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.POST, value="/query/get-events-by-rql")
    public StreamedArrayWithTotal getEvents(
            @RequestBody
            EventQueryByMaintenanceEventRql body,
            @AuthenticationPrincipal User user) {
        ASTNode rql = RQLUtils.parseRQLtoAST(body.getMaintenanceEventsRql());

        //First do the RQL on maintenance events
        List<Object> args = new ArrayList<>();
        args.add("typeRef1");
        service.customizedQuery(rql, new MappedRowCallback<MaintenanceEventVO>() {
            @Override
            public void row(MaintenanceEventVO vo, int index) {
                args.add(Integer.toString(vo.getId()));
            }
        });
        //Second query the events
        if(args.size() > 1) {
            ASTNode query = new ASTNode("in", args);
            query = addAndRestriction(query, new ASTNode("eq", "typeName", MaintenanceEventType.TYPE_NAME));

            if(body.getActive() != null) {
                query = addAndRestriction(query, new ASTNode("eq", "active", body.getActive()));
            }
            if(body.getOrder() != null) {
                String order = body.getOrder();
                if("asc".equals(order))
                    query = addAndRestriction(query, new ASTNode("sort","+activeTs"));
                else
                    query = addAndRestriction(query, new ASTNode("sort","-activeTs"));
            }
            if (body.getLimit() != null)
                query = addAndRestriction(query, new ASTNode("limit", body.getLimit()));

            return doEventQuery(query, user);
        }else {
            return new StreamedArrayWithTotal() {
                @Override
                public StreamedArray getItems() {
                    return null;
                }
                @Override
                public int getTotal() {
                    return 0;
                }
            };
        }
    }

    @ApiOperation(
            value = "Find Events for a set of Maintenance events created by the supplied criteria",
            notes = "Returns Events for any Maintenance event that has any of the supplied data points OR data sources as its source",
            response=EventInstanceModel.class,
            responseContainer="List"
            )
    @RequestMapping(method = RequestMethod.POST, value="/query/get-events-by-points-and-or-sources")
    public StreamedArrayWithTotal getEventsByCriteria(
            @RequestBody
            EventQueryByMaintenanceCriteria body,
            @AuthenticationPrincipal User user) {

        List<String> dataSourceXids = new ArrayList<>();
        if(body.getDataSourceXids() != null) {
            for(String xid : body.getDataSourceXids()) {
                dataSourceXids.add(xid);
            }
        }
        List<String> dataPointXids = new ArrayList<>();
        if(body.getDataPointXids() != null) {
            for(String xid : body.getDataPointXids()) {
                dataPointXids.add(xid);
            }
        }
        //Find all matching Maintenance Events
        Set<Integer> ids = new HashSet<>();
        MappedRowCallback<MaintenanceEventVO> callback = new MappedRowCallback<MaintenanceEventVO>() {
            @Override
            public void row(MaintenanceEventVO vo, int index) {
                ids.add(vo.getId());
            }
        };
        for(String xid : dataPointXids) {
            MaintenanceEventDao.getInstance().getForDataPoint(xid, callback);
        }

        for(String xid : dataSourceXids) {
            MaintenanceEventDao.getInstance().getForDataSource(xid, callback);
        }

        List<Object> args = new ArrayList<>();
        args.add("typeRef1");
        for(Integer id : ids)
            args.add(Integer.toString(id));

        //Second query the events
        if(args.size() > 1) {
            ASTNode query = new ASTNode("in", args);
            query = addAndRestriction(query, new ASTNode("eq", "typeName", MaintenanceEventType.TYPE_NAME));

            if(body.getActive() != null) {
                query = addAndRestriction(query, new ASTNode("eq", "active", body.getActive()));
            }
            if(body.getOrder() != null) {
                String order = body.getOrder();
                if("asc".equals(order))
                    query = addAndRestriction(query, new ASTNode("sort","+activeTs"));
                else
                    query = addAndRestriction(query, new ASTNode("sort","-activeTs"));
            }
            if (body.getLimit() != null)
                query = addAndRestriction(query, new ASTNode("limit", body.getLimit()));

            return doEventQuery(query, user);
        }else {
            return new StreamedArrayWithTotal() {
                @Override
                public StreamedArray getItems() {
                    return null;
                }
                @Override
                public int getTotal() {
                    return 0;
                }
            };
        }
    }

    //Helpers for Queries
    private StreamedArrayWithTotal doEventQuery(ASTNode rql, User user) {
        if (user.hasAdminRole()) {
            return new StreamedVORqlQueryWithTotal<>(eventService, rql, eventTableFieldMap, eventTableValueConverters, item -> true, vo -> eventMap.apply(vo, user));
        } else {
            return new StreamedVORqlQueryWithTotal<>(eventService, rql, eventTableFieldMap, eventTableValueConverters, item -> eventService.hasReadPermission(user, item), vo -> eventMap.apply(vo, user));
        }
    }

    /**
     * Append an AND Restriction to a query
     * @param query - can be null
     * @param restriction
     * @return
     */
    protected static ASTNode addAndRestriction(ASTNode query, ASTNode restriction){
        //Root query node
        ASTNode root = null;

        if(query == null){
            root = restriction;
        }else if(query.getName().equalsIgnoreCase("and")){
            root = query.addArgument(restriction);
        }else{
            root = new ASTNode("and", restriction, query);
        }
        return root;
    }

    /**
     * Append an OR restriction to the query
     * @param query - can be null
     * @param restriction
     * @return
     */
    protected static ASTNode addOrRestriction(ASTNode query, ASTNode restriction){
        //Root query node
        ASTNode root = null;

        if(query == null){
            root = restriction;
        }else if(query.getName().equalsIgnoreCase("or")){
            root = query.addArgument(restriction);
        }else{
            root = new ASTNode("or", restriction, query);
        }
        return root;
    }

    final Function<MaintenanceEventVO, Object> transformVisit = item -> {
        MaintenanceEventModel model = new MaintenanceEventModel(item);
        fillDataPoints(model);
        fillDataSources(model);
        return model;
    };

    /**
     * Set the data point XIDs if there are any, id must be set in model
     * @param model
     */
    private void fillDataPoints(MaintenanceEventModel model) {
        List<String> xids = new ArrayList<String>();
        dao.getPointXids(model.getId(), new MappedRowCallback<String>() {
            @Override
            public void row(String item, int index) {
                xids.add(item);
            }
        });
        model.setDataPoints(xids.size() == 0 ? null : xids);
    }

    /**
     * Set the data source XIDs if there are any, id must be set in model
     * @param model
     */
    private void fillDataSources(MaintenanceEventModel model) {
        List<String> dsXids = new ArrayList<String>();
        dao.getSourceXids(model.getId(), new MappedRowCallback<String>() {
            @Override
            public void row(String item, int index) {
                dsXids.add(item);
            }
        });
        model.setDataSources(dsXids.size() == 0 ? null : dsXids);
    }
}
