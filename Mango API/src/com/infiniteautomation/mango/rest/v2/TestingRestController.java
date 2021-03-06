/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.management.MBeanServer;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.rest.v2.exception.GenericRestException;
import com.infiniteautomation.mango.rest.v2.model.event.RaiseEventModel;
import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Jared Wiltshire
 */
@RestController
@ConditionalOnProperty(value = {"${testing.enabled:false}", "${testing.restApi.enabled:false}"})
@PreAuthorize("isAdmin()")
@RequestMapping("/testing")
public class TestingRestController {

    private final Logger log = LoggerFactory.getLogger(TestingRestController.class);

    private final Set<PosixFilePermission> readablePerms =
            Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ)
            .stream().collect(Collectors.toSet());

    private final MangoSessionRegistry sessionRegistry;;
    @Autowired
    public TestingRestController(MangoSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @RequestMapping(method = {RequestMethod.GET}, value = "/location")
    public ResponseEntity<Void> testLocation(UriComponentsBuilder builder) {

        HttpHeaders headers = new HttpHeaders();
        URI location = builder.path("/{id}").buildAndExpand("over-here").toUri();
        headers.setLocation(location);

        return new ResponseEntity<>(null, headers, HttpStatus.CREATED);
    }

    @RequestMapping(method = {RequestMethod.GET}, value = "/remote-addr")
    public String testLocation(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @RequestMapping(method = {RequestMethod.POST}, value = "/heap-dump")
    public String heapDump(@RequestParam String filename,
            @RequestParam(required=false, defaultValue="false") boolean overwrite,
            @RequestParam(required=false, defaultValue="false") boolean readable) throws Exception {
        boolean ibm = true;
        try {
            Class.forName("com.ibm.jvm.Dump");
        } catch (ClassNotFoundException e) {
            ibm = false;
        }

        Path filePath;

        if (ibm) {
            filePath = Common.MA_HOME_PATH.resolve(filename + ".phd").toAbsolutePath();
        } else {
            filePath = Common.MA_HOME_PATH.resolve(filename + ".hprof").toAbsolutePath();
        }

        log.info("Dumping heap to {}", filePath);

        if (overwrite) {
            Files.deleteIfExists(filePath);
        }

        if (ibm) {
            String newPath = (String) Class.forName("com.ibm.jvm.Dump").getMethod("heapDumpToFile", String.class).invoke(null, filePath.toString());
            filePath = Paths.get(newPath).toAbsolutePath();
        } else {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            Object bean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", clazz);
            clazz.getMethod("dumpHeap", String.class, boolean.class).invoke(bean, filePath.toString(), true);
        }

        // the dumps are written with only user read perms, enable relaxing the permissions
        if (readable) {
            try {
                Files.setPosixFilePermissions(filePath, readablePerms);
            } catch (UnsupportedOperationException e) {}
        }

        return filePath.toString();
    }

    @RequestMapping(method = {RequestMethod.GET}, value = "/jvm-info")
    public JVMInfo jvmInfo() {
        return new JVMInfo();
    }

    public static class JVMInfo {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        public String getName() {
            return runtime.getName();
        }
        public long getUptime() {
            return runtime.getUptime();
        }
        public long getStartTime() {
            return runtime.getStartTime();
        }
        public String getVmName() {
            return runtime.getVmName();
        }
        public String getVmVendor() {
            return runtime.getVmVendor();
        }
        public String getVmVersion() {
            return runtime.getVmVersion();
        }
        public String getSpecName() {
            return runtime.getSpecName();
        }
        public String getSpecVendor() {
            return runtime.getSpecVendor();
        }
        public String getSpecVersion() {
            return runtime.getSpecVersion();
        }
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Example User Credentials test", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/admin-get/{resourceId}"} )
    public ResponseEntity<Object> exampleGet(@AuthenticationPrincipal User user,
            @ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
        return new ResponseEntity<Object>(HttpStatus.OK);
    }


    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example User Credentials test", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/user-get/{resourceId}"} )
    public ResponseEntity<Object> userGet(@AuthenticationPrincipal User user,
            @ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
        return new ResponseEntity<Object>(HttpStatus.OK);
    }

    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example Permission Exception Response", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/permissions-exception"} )
    public ResponseEntity<Object> alwaysFails(@AuthenticationPrincipal User user) {
        throw new PermissionException(new TranslatableMessage("common.default", "I always fail."), user);
    }

    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example Access Denied Exception Response", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/access-denied-exception"} )
    public ResponseEntity<Object> accessDenied(@AuthenticationPrincipal User user) {
        throw new AccessDeniedException("I don't have access.");
    }

    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example Generic Rest Exception Response", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/generic-exception"} )
    public ResponseEntity<Object> genericFailure(@AuthenticationPrincipal User user) {
        throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example Runtime Exception Response", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/runtime-exception"} )
    public ResponseEntity<Object> runtimeFailure(@AuthenticationPrincipal User user) {
        throw new RuntimeException("I'm a runtime Exception");
    }

    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example IOException Response", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/io-exception"} )
    public ResponseEntity<Object> ioFailure(@AuthenticationPrincipal User user) throws IOException{
        throw new IOException("I'm an Exception");
    }

    @PreAuthorize("hasAllPermissions('user')")
    @ApiOperation(value = "Example LicenseViolationException Response", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/license-violation"} )
    public ResponseEntity<Object> licenseViolation(@AuthenticationPrincipal User user) throws IOException{
        throw new LicenseViolatedException(new TranslatableMessage("common.default", "Test Violiation"));
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Expire the session of the current user", notes = "must be admin")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/expire-session"} )
    public ResponseEntity<Object> expireSessions(@AuthenticationPrincipal User user){
        List<SessionInformation> infos = sessionRegistry.getAllSessions(user, false);
        for(SessionInformation info : infos)
            info.expireNow();
        return new ResponseEntity<Object>(HttpStatus.OK);
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Example Path matching", notes = "")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.GET}, value = {"/{resourceId}/**"} )
    public ResponseEntity<String> matchPath(@AuthenticationPrincipal User user,
            @ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId,
            HttpServletRequest request) {

        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String ) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

        return new ResponseEntity<String>(finalPath, HttpStatus.OK);
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Raise an event", notes = "must be admin")
    @ApiResponses({
        @ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
    })
    @RequestMapping( method = {RequestMethod.POST}, value = {"/raise-event"} )
    public ResponseEntity<Object> raiseExampleEvent(@AuthenticationPrincipal User user,
            @RequestBody(required=true) RaiseEventModel model){
        if(model == null)
            throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR);
        Common.eventManager.raiseEvent(model.getEvent().toVO(), Common.timer.currentTimeMillis(), true, model.getLevel(), new TranslatableMessage("common.default", model.getMessage()), model.getContext());
        return new ResponseEntity<Object>(HttpStatus.OK);
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Identity function", notes = "Returns whatever is sent in the request body. Useful for testing message converters. Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/identity"})
    public Object identityFunction(
            @RequestBody Object node) {

        return node;
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Log ERROR Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-error-message"})
    public void logErorMessage(
            @RequestBody String message) {
        log.error(message);
    }
    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Log WARN Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-warn-message"})
    public void logWarnMessage(
            @RequestBody String message) {
        log.warn(message);
    }
    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Log INFO Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-info-message"})
    public void logInfoMessage(
            @RequestBody String message) {
        log.info(message);
    }
    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Log DEBUG Level Message", notes = "Must be admin")
    @RequestMapping(method = RequestMethod.POST, value = {"/log-debug-message"})
    public void logDebugMessage(
            @RequestBody String message) {
        log.debug(message);
    }

    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Get upload limit")
    @RequestMapping(method = RequestMethod.GET, value = {"/upload-limit"})
    public long getUploadLimit() {
        return Common.envProps.getLong("web.fileUpload.maxSize", 50000000);
    }

    @PreAuthorize("hasRole('ROLE_TEST SPACE')")
    @RolesAllowed("ROLE_TEST SPACE")
    @Secured("ROLE_TEST SPACE")
    @ApiOperation(value = "User must have a permission named 'TEST SPACE'")
    @RequestMapping(method = RequestMethod.GET, value = {"/role-with-space"})
    public String canGetWithSpaceInPermission() {
        return "OK";
    }

    @Async
    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Execute a long running request that eventually returns OK")
    @RequestMapping(method = RequestMethod.GET, value = {"/delay-response/{delayMs}"})
    public CompletableFuture<String> delayedResponse(
            @ApiParam(value="Delay ms", required=true, allowMultiple=false) @PathVariable int delayMs,
            HttpServletRequest request) throws InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
                return "OK";
            }catch(InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Async
    @PreAuthorize("isAdmin()")
    @ApiOperation(value = "Execute a long running request that eventually fails on a runtime exception")
    @RequestMapping(method = RequestMethod.GET, value = {"/async-failure/{delayMs}"})
    public CompletableFuture<String> asyncFailure(
            @ApiParam(value="Delay ms", required=true, allowMultiple=false) @PathVariable int delayMs,
            HttpServletRequest request) throws InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
                throw new CompletionException(new RuntimeException("I Should Fail"));
            }catch(InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }
}
