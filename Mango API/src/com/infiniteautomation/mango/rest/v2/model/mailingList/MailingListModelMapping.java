/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.mailingList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
@Component
public class MailingListModelMapping implements RestModelMapping<MailingList, MailingListModel> {

    private final RoleService roleService;
    private final MailingListService mailingListService;

    @Autowired
    public MailingListModelMapping(MailingListService mailingListService, RoleService service) {
        this.mailingListService = mailingListService;
        this.roleService = service;
    }

    @Override
    public Class<? extends MailingList> fromClass() {
        return MailingList.class;
    }

    @Override
    public Class<? extends MailingListModel> toClass() {
        return MailingListModel.class;
    }

    @Override
    public boolean supports(Class<?> from, Class<?> toClass) {
        return this.fromClass().isAssignableFrom(from) &&
                (toClass.isAssignableFrom(this.toClass())
                        || toClass.isAssignableFrom(MailingListWithRecipientsModel.class));
    }

    @Override
    public MailingListModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        MailingList vo = (MailingList)from;
        MailingListModel model;
        if(mailingListService.hasRecipientViewPermission(user, vo)) {
            model = new MailingListWithRecipientsModel(vo);

            if(vo.getEntries() != null && vo.getEntries().size() > 0) {
                List<EmailRecipientModel> recipients = new ArrayList<>();
                ((MailingListWithRecipientsModel)model).setRecipients(recipients);

                for(MailingListRecipient entry : vo.getEntries()) {
                    recipients.add(mapper.map(entry, EmailRecipientModel.class, user));
                }
            }
        } else {
            model = new MailingListModel(vo);
        }
        Set<String> readPermissions = new HashSet<>();
        model.setReadPermissions(readPermissions);
        for(Role role : vo.getReadRoles()) {
            readPermissions.add(role.getXid());
        }
        Set<String> editPermissions = new HashSet<>();
        model.setEditPermissions(editPermissions);
        for(Role role : vo.getEditRoles()) {
            editPermissions.add(role.getXid());
        }
        return model;
    }

    @Override
    public MailingList unmap(Object from, PermissionHolder user, RestModelMapper mapper) throws ValidationException {
        MailingListModel model = (MailingListModel)from;
        MailingList vo = model.toVO();
        ProcessResult result = new ProcessResult();
        if(model.getReadPermissions() != null) {
            Set<Role> roles = new HashSet<>();
            vo.setReadRoles(roles);
            for(String role : model.getReadPermissions()) {
                try {
                    roles.add(roleService.get(role).getRole());
                }catch(NotFoundException e) {
                    result.addContextualMessage("readPermissions", "roles.roleNotFound", role);
                }
            }
        }

        if(model.getEditPermissions() != null) {
            Set<Role> roles = new HashSet<>();
            vo.setEditRoles(roles);
            for(String role : model.getEditPermissions()) {
                try {
                    roles.add(roleService.get(role).getRole());
                }catch(NotFoundException e) {
                    result.addContextualMessage("editPermissions", "roles.roleNotFound", role);
                }
            }
        }
        if(!result.isValid()) {
            throw new ValidationException(result);
        }
        return vo;
    }

}
