/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infiniteautomation.mango.rest.v2.JsonEmportV2Controller.ImportStatusProvider;
import com.infiniteautomation.mango.rest.v2.genericcsv.CsvJacksonModule;
import com.infiniteautomation.mango.rest.v2.genericcsv.GenericCSVMessageConverter;
import com.infiniteautomation.mango.rest.v2.mapping.HtmlHttpMessageConverter;
import com.infiniteautomation.mango.rest.v2.mapping.JScienceModule;
import com.infiniteautomation.mango.rest.v2.mapping.JsonStreamMessageConverter;
import com.infiniteautomation.mango.rest.v2.mapping.MangoRestV2JacksonModule;
import com.infiniteautomation.mango.rest.v2.mapping.PointValueTimeStreamCsvMessageConverter;
import com.infiniteautomation.mango.rest.v2.mapping.SerotoninJsonMessageConverter;
import com.infiniteautomation.mango.rest.v2.mapping.SqlMessageConverter;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.resolver.PartialUpdateArgumentResolver;
import com.infiniteautomation.mango.rest.v2.util.MangoRestTemporaryResourceContainer;
import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.m2m2.web.MediaTypes;
import com.serotonin.m2m2.web.mvc.spring.security.MangoMethodSecurityConfiguration;

/**
 * @author Terry Packer
 *
 */
@Configuration("MangoV2RestDispatcherConfiguration")
@Import({MangoCommonConfiguration.class, MangoMethodSecurityConfiguration.class, MangoWebSocketConfiguration.class})
@EnableWebMvc
@ComponentScan(basePackages = { "com.infiniteautomation.mango.rest.v2" }, excludeFilters = {})
public class MangoRestDispatcherConfiguration implements WebMvcConfigurer {
    public static final String CONTEXT_ID = "restV2Context";
    public static final String DISPATCHER_NAME = "restV2DispatcherServlet";

    final ObjectMapper mapper;
    final PartialUpdateArgumentResolver resolver;
    final List<HttpMessageConverter<?>> converters;

    @Autowired
    public MangoRestDispatcherConfiguration(
            @Qualifier(MangoRuntimeContextConfiguration.REST_OBJECT_MAPPER_NAME)
            ObjectMapper mapper,
            PartialUpdateArgumentResolver resolver,
            RestModelMapper modelMapper) {
        this.mapper = mapper;
        this.resolver = resolver;
        this.converters = new ArrayList<>();

        mapper
        .registerModule(new MangoRestV2JacksonModule())
        .registerModule(new Jdk8Module())
        .registerModule(new JScienceModule());

        modelMapper.addMappings(mapper);
    }

    @PostConstruct
    public void init() {
        //Setup our message converter list
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new ResourceRegionHttpMessageConverter());
        converters.add(new JsonStreamMessageConverter(mapper));
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new HtmlHttpMessageConverter());
        converters.add(new SerotoninJsonMessageConverter());
        converters.add(new SqlMessageConverter());
        converters.add(new PointValueTimeStreamCsvMessageConverter(csvMapper()));
        converters.add(new GenericCSVMessageConverter(csvObjectMapper()));
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    /**
     * Create a Path helper that will not URL Decode
     * the context path and request URI but will
     * decode the path variables...
     *
     */
    public UrlPathHelper getUrlPathHelper() {
        UrlPathHelper helper = new UrlPathHelper();
        helper.setUrlDecode(false);
        return helper;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false).setUrlPathHelper(getUrlPathHelper());
    }

    /**
     * Setup Content Negotiation to map url extensions to returned data types
     *
     * @see http
     *      ://spring.io/blog/2013/05/11/content-negotiation-using-spring-mvc
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // dont set defaultContentType to text/html, we dont want this for REST
        // it causes Accept: */* headers to map to Accept: text/html
        // which causes hell for finding acceptable content types
        configurer
        .favorPathExtension(false)
        .ignoreAcceptHeader(false)
        .favorParameter(true)
        .useRegisteredExtensionsOnly(true)
        .mediaType("xml", MediaType.APPLICATION_XML)
        .mediaType("json", MediaType.APPLICATION_JSON_UTF8)
        .mediaType("sjson", MediaTypes.SEROTONIN_JSON)
        .mediaType("csv", MediaTypes.CSV_V2)
        .mediaType("csv2", MediaTypes.CSV_V2)
        .mediaType("txt", MediaType.TEXT_PLAIN);
    }

    @Bean("csvObjectMapper")
    public ObjectMapper csvObjectMapper() {
        return mapper.copy()
                .setDateFormat(GenericCSVMessageConverter.EXCEL_DATE_FORMAT)
                .registerModule(new CsvJacksonModule());
    }

    @Bean("csvMapper")
    public CsvMapper csvMapper() {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS, true);
        csvMapper.configure(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS, false);
        csvMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        csvMapper.registerModule(new JavaTimeModule());
        csvMapper.registerModule(new CsvJacksonModule());
        csvMapper.setTimeZone(TimeZone.getDefault()); //Set to system tz
        return csvMapper;
    }

    /**
     * Configure the Message Converters for the API, this will override any defaults
     *  added by Spring.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addAll(this.converters);
    }

    /**
     * To inject a singleton into the JSON Import rest controller
     * @return
     */
    @Bean()
    public MangoRestTemporaryResourceContainer<ImportStatusProvider> importStatusResources() {
        return new MangoRestTemporaryResourceContainer<ImportStatusProvider>("IMPORT_");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
