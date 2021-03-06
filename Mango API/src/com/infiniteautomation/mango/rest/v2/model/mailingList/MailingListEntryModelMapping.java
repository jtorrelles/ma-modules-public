/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.model.mailingList;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.vo.mailingList.MailingListEntry;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
@Component
public class MailingListEntryModelMapping implements RestModelMapping<MailingListEntry, MailingListEntryModel> {

    @Override
    public Class<? extends MailingListEntry> fromClass() {
        return MailingListEntry.class;
    }

    @Override
    public Class<? extends MailingListEntryModel> toClass() {
        return MailingListEntryModel.class;
    }

    @Override
    public MailingListEntryModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        MailingListEntry vo = (MailingListEntry)from;
        return new MailingListEntryModel(vo);
    }
}
