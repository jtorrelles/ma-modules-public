/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.model.dataPoint;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;

/**
 *
 * @author Terry Packer
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property=AbstractPointLocatorModel.MODEL_TYPE)
public abstract class AbstractPointLocatorModel <T extends PointLocatorVO<T>> {

    public static final String MODEL_TYPE = "modelType";

    protected String dataType;
    protected boolean settable;
    protected Boolean relinquishable;
    protected TranslatableMessage configurationDescription;

    public AbstractPointLocatorModel() { }
    public AbstractPointLocatorModel(T vo) {
        fromVO(vo);
    }

    /**
     * Convert from a point locator to this model
     * @param locator
     */
    public void fromVO(T vo) {
        this.dataType = DataTypes.CODES.getCode(vo.getDataTypeId());
        this.settable = vo.isSettable();
        this.relinquishable = vo.isRelinquishable();
        this.configurationDescription = vo.getConfigurationDescription();
    }

    /**
     * Convert to a point locator VO
     * @return
     */
    abstract public T toVO();

    /**
     * Return the TYPE_NAME from the Data Source definition
     * @return
     */
    abstract public String getModelType();

    public TranslatableMessage getConfigurationDescription() {
        return configurationDescription;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isSettable() {
        return settable;
    }

    public void setSettable(Boolean settable) {
        this.settable = settable;
    }

    public Boolean isRelinquishable() {
        return relinquishable;
    }

}
