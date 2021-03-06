/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.pointValue.query;

import java.time.ZonedDateTime;

import com.infiniteautomation.mango.rest.v2.model.pointValue.PointValueField;

/**
 *
 * @author Terry Packer
 */
public class XidStatisticsQueryInfoModel extends XidTimeRangeQueryModel {

    public XidStatisticsQueryInfoModel() {
        
    }
    
    public XidStatisticsQueryInfoModel(String[] xids, String dateTimeFormat,
            String timezone, ZonedDateTime from, ZonedDateTime to, Integer limit,
            boolean bookend, PointValueTimeCacheControl useCache, Double simplifyTolerance, 
            Integer simplifyTarget, PointValueField[] fields) {
        super(xids, dateTimeFormat, timezone, from, to, null, true,
                useCache, null, null, fields);
    }
    
    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.rest.v2.model.pointValue.query.XidTimeRangeQueryModel#isBookend()
     */
    @Override
    public boolean isBookend() {
        return true;
    }
    
}
