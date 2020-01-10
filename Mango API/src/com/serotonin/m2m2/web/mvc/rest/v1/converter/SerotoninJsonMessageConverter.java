/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.MediaTypes;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;

/**
 * Convert Outgoing Objects to Serotonin JSON
 *
 * Convert Incoming Serotonin JSON to Mango VOs (experimental)
 *
 *
 * @author Terry Packer
 */
public class SerotoninJsonMessageConverter extends AbstractHttpMessageConverter<Object>{

    public SerotoninJsonMessageConverter(){
        super(MediaTypes.SEROTONIN_JSON);
    }

    /* (non-Javadoc)
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#supports(java.lang.Class)
     */
    @Override
    protected boolean supports(Class<?> clazz) {
        //TODO Maybe restrict to Lists,Objects that are RestModels etc, but Sero JSON should support most objects
        return true;
    }

    /* (non-Javadoc)
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
     */
    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        InputStreamReader isReader = new InputStreamReader(inputMessage.getBody());
        JsonTypeReader typeReader = new JsonTypeReader(isReader);
        try {
            JsonValue value = typeReader.read();

            if(clazz.equals(JsonValue.class))
                return value;
            else
                return root.toNative();
        }catch(JsonException e){
            throw new IOException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
     */
    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        OutputStreamWriter osWriter = new OutputStreamWriter(outputMessage.getBody());
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, osWriter);
        writer.setPrettyOutput(true);
        writer.setPrettyIndent(2);
        try {
            writer.writeObject(t);
            writer.flush();
        }
        catch (JsonException e) {
            throw new IOException(e);
        }
    }

    protected ModelDefinition findModelDefinition(Class<?> clazz) throws ModelNotFoundException{
        List<ModelDefinition> definitions = ModuleRegistry.getModelDefinitions();
        for(ModelDefinition definition : definitions){
            if(definition.supportsClass(clazz))
                return definition;
        }
        throw new ModelNotFoundException(clazz.getName());
    }

}
