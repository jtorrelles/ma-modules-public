/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.reports;

import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.web.taglib.Functions;

/**
 * @author Terry Packer
 *
 */
public class ReportAuditEvent extends AuditEventTypeDefinition{

	public static final String TYPE_NAME = "REPORT";

	@Override
	public String getTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return "event.audit.report";
	}

	@Override
	public String getEventListLink(int ref1, int ref2, Translations translations) {
        String alt = Functions.quotEncode(translations.translate("event.audit.report"));
        StringBuilder sb = new StringBuilder();
        sb.append("<a href='/reports.shtm?reportId=");
        sb.append(ref1);
        sb.append("'><img src='");
        sb.append("/" + Constants.DIR_MODULES + "/" + getModule().getName()).append("/web/images/report.png");
        sb.append("' alt='").append(alt);
        sb.append("' title='").append(alt);
        sb.append("'/></a>");
        return sb.toString();
	}

}