/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import com.infiniteautomation.mango.rest.v2.exception.GenericRestException;
import com.infiniteautomation.mango.rest.v2.exception.NotFoundRestException;
import com.infiniteautomation.mango.rest.v2.exception.ResourceNotFoundException;
import com.infiniteautomation.mango.rest.v2.model.filestore.FileModel;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Manage files from stores defined by FileStoreDefinition(s)
 * 
 * @author Terry Packer
 */
@Api(value="File Store", description="Allow read/write access to file storage areas")
@RestController()
@RequestMapping("/v2/file-stores")
public class FileStoreRestV2Controller extends AbstractMangoRestV2Controller{

	@ApiOperation(
			value = "List all file store names",
			notes = "Must have read access to see the store"
			)
	@RequestMapping(method = RequestMethod.GET, produces={"application/json"})
    public ResponseEntity<List<String>> list(
    		@AuthenticationPrincipal User user,
    		HttpServletRequest request) {
		
		Map<String, FileStoreDefinition> defs = ModuleRegistry.getFileStoreDefinitions();
		List<String> accessible = new ArrayList<String>(defs.size());
		if(user.isAdmin()){
			//admin users don't need to filter the results
			for(FileStoreDefinition def : defs.values()){
				def.ensureStoreReadPermission(user);
				accessible.add(def.getStoreName());
			}
		}else{

			for(FileStoreDefinition def : defs.values()){
				try{
					def.ensureStoreReadPermission(user);
					accessible.add(def.getStoreName());
				}catch(AccessDeniedException e){ }
			}
		}
		return new ResponseEntity<>(accessible, HttpStatus.OK);
	}

	@ApiOperation(
			value = "Upload a file to a store with a path",
			notes = "Must have write access to the store, will overwrite existing files"
			)
	@RequestMapping(method = RequestMethod.POST, produces={"application/json"}, value="/{name}/**")
    public ResponseEntity<List<FileModel>> uploadWithPath(
       		@ApiParam(value = "Valid File Store name", required = true, allowMultiple = false)
       	 	@PathVariable("name") String name,
    		@AuthenticationPrincipal User user,
    		MultipartHttpServletRequest multipartRequest,
    		HttpServletRequest request) throws IOException {
		
		FileStoreDefinition def = ModuleRegistry.getFileStoreDefinition(name);
		if(def == null)
			throw new NotFoundRestException();

		//Check Permissions
		def.ensureStoreWritePermission(user);

		String pathInStore = parsePath(request);
		
		File root = def.getRoot();
		File toSave = new File(root, pathInStore);
		
		if (toSave.exists() && !toSave.isDirectory()) {
		    throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR, new TranslatableMessage("filestore.cannotCreateDir", removeToRoot(root, toSave), name));
		}

		if(!toSave.exists()){
			if(!toSave.mkdirs())
				throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR, new TranslatableMessage("filestore.cannotCreateDir", removeToRoot(root, toSave), name));
		}
		
		//Put the file where it belongs
		List<FileModel> fileModels = new ArrayList<>();
		Iterator<String> itr =  multipartRequest.getFileNames();
		while(itr.hasNext()){
            MultipartFile file = multipartRequest.getFile(itr.next());
            String filename = file.getOriginalFilename();
            if (filename == null) {
                filename = file.getName();
            }
			File newFile = new File(toSave, filename);
			fileModels.add(fileToModel(newFile, root, request.getServletContext()));
        	byte[] bytes = file.getBytes();
            try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(newFile, false))) {
                stream.write(bytes);
            }
		}
		return new ResponseEntity<>(fileModels, HttpStatus.OK);
	}
	
	@ApiOperation(value = "List a directory or download a file from a store")
	@RequestMapping(method = RequestMethod.GET, produces={"*/*"}, value="/{name}/**")
    public ResponseEntity<?> download(
       		@ApiParam(value = "Valid File Store name", required = true, allowMultiple = false)
       	 	@PathVariable("name") String name,
       	 	@ApiParam(value = "Set content disposition to attachment", required = false, defaultValue="false", allowMultiple = false)
            @RequestParam(required=false, defaultValue="true") boolean download,
    		@AuthenticationPrincipal User user,
    		HttpServletRequest request) throws IOException, HttpMediaTypeNotAcceptableException {
    	
		FileStoreDefinition def = ModuleRegistry.getFileStoreDefinition(name);
		if (def == null)
			throw new ResourceNotFoundException("File store: " + name);
		
		//Check permissions
		def.ensureStoreReadPermission(user);
		
		String path = parsePath(request);
		File file = new File(def.getRoot(), path);
		if(!file.exists())
			throw new ResourceNotFoundException("filestore/" + name + "/" + path);

        // TODO Allow downloading directory as a zip
		if (file.isFile()) {
		    return getFile(file, download, request);
		} else {
		    return listStoreContents(file, request);
		}
	}

	protected ResponseEntity<List<FileModel>> listStoreContents(File directory, HttpServletRequest request) throws IOException {
        Collection<File> files = Arrays.asList(directory.listFiles());
        List<FileModel> found = new ArrayList<>(files.size());
        
        for (File file : files)
            found.add(fileToModel(file, directory, request.getServletContext()));
        
        @SuppressWarnings("unchecked")
        Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        
        return new ResponseEntity<>(found, responseHeaders, HttpStatus.OK);
    }
	
	protected ResponseEntity<FileSystemResource> getFile(File file, boolean download, HttpServletRequest request) throws HttpMediaTypeNotAcceptableException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, download ? "attachment" : "inline");

        // We need to do our own custom MIME handling as the Spring implementation doesnt handle this correctly:
        // - Wildcards like image/* dont work
        // - Does not trigger HTTP 406 Not Acceptable if file MIME does not match the Accept header
        
        MediaType mediaType = null;
        
        @SuppressWarnings("unchecked")
        Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        mediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        
        // ResourceHttpMessageConverter uses ActivationMediaTypeFactory.getMediaType(resource) but this is not visible
        String mimeType = request.getServletContext().getMimeType(file.getName());
        if (StringUtils.hasText(mimeType)) {
            try {
                mediaType = MediaType.parseMediaType(mimeType);
                MediaType compatibleType = null;

                HeaderContentNegotiationStrategy strategy = new HeaderContentNegotiationStrategy();
                List<MediaType> requestedMediaTypes = strategy.resolveMediaTypes(new ServletWebRequest(request));

                if (requestedMediaTypes.isEmpty()) {
                    compatibleType = mediaType;
                } else {
                    for (MediaType requestedType : requestedMediaTypes) {
                        if (requestedType.isCompatibleWith(mediaType) || requestedType.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM)) {
                            compatibleType = mediaType;
                            break;
                        }
                    }
                }

                if (compatibleType != null) {
                    // setting this request attribute essentially modifies the @RequestMapping(produces=xxx) property
                    // we need to do this because otherwise AbstractMessageConverterMethodProcessor fails on partial wildcard Accept headers like image/*
                    // and causes HTTP 406 Not Acceptable errors
                    mediaTypes.add(mediaType);
                } else {
                    throw new HttpMediaTypeNotAcceptableException(Arrays.asList(mediaType, MediaType.APPLICATION_OCTET_STREAM));
                }
            } catch (InvalidMediaTypeException e) {
                // Shouldn't happen - ServletContext.getMimeType() should return valid mime types
            }
        }

        // always set the content type header or AbstractHttpMessageConverter.addDefaultHeaders() will set the Content-Type
        // to whatever the Accept header was
        responseHeaders.setContentType(mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM);
        
        return new ResponseEntity<>(new FileSystemResource(file), responseHeaders, HttpStatus.OK);
	}
	
    /**
     * Get the path within the store off the URL
     * @param request
     * @return
     * @throws UnsupportedEncodingException 
     */
    protected String parsePath(HttpServletRequest request) throws UnsupportedEncodingException{
	    String path = (String) request.getAttribute(
	            HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
	    String bestMatchPattern = (String ) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

	    AntPathMatcher apm = new AntPathMatcher();
	    return URLDecoder.decode(apm.extractPathWithinPattern(bestMatchPattern, path), StandardCharsets.UTF_8.name());
    }

	/**
	 * Remove the path up to the root folder
	 * @param root
	 * @param file
	 * @return
	 */
    public static String removeToRoot(File root, File file){
	    String name = root.toURI().relativize(file.toURI()).toString();
	    if (file.isDirectory() && name.endsWith("/")) {
	        name = name.substring(0, name.length() - 1);
	    }
	    return name;
	}
	
	public static FileModel fileToModel(File file, File relativeTo, ServletContext context) {
	    FileModel model = new FileModel();
	    model.setFilename(removeToRoot(relativeTo, file));
	    model.setDirectory(file.isDirectory());
        model.setLastModified(new Date(file.lastModified()));
	    model.setMimeType(context.getMimeType(file.getName()));
	    if (!file.isDirectory())
	        model.setSize(file.length());
	    return model;
	}
}
