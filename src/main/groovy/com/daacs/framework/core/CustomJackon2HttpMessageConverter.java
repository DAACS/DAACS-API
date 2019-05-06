package com.daacs.framework.core;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.jetty.server.HttpOutput;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by chostetter on 7/28/16.
 * Most of this is taken from MappingJackson2HttpMessageConverter.
 */
@Component
public class CustomJackon2HttpMessageConverter extends MappingJackson2HttpMessageConverter {

    private String[] unwrappableRequestUris = { "swagger-resources", "api-docs" };
    private String[] unwrappableParentClasses = { "com.daacs.model.dto.UnwrappableResponse" };
    private String[] unwrappableClasses = { "ErrorResponse", "MetaData", "springfox" };

    // Check for Jackson 2.6+ for support of generic type aware serialization of polymorphic collections
    private static final boolean jackson26Available = ClassUtils.hasMethod(ObjectMapper.class,
            "setDefaultPrettyPrinter", PrettyPrinter.class);

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
        JsonGenerator generator = this.objectMapper.getFactory().createGenerator(outputMessage.getBody(), encoding);
        try {
            writePrefix(generator, object);

            Class<?> serializationView = null;
            FilterProvider filters = null;
            Object value = object;
            JavaType javaType = null;

            if (object instanceof MappingJacksonValue) {
                MappingJacksonValue container = (MappingJacksonValue) object;
                value = container.getValue();
                serializationView = container.getSerializationView();
                filters = container.getFilters();
            }

            if (jackson26Available && type != null && value != null && TypeUtils.isAssignable(type, value.getClass())) {
                javaType = getJavaType(type, null);
            }

            ObjectWriter objectWriter;
            if (serializationView != null) {
                objectWriter = this.objectMapper.writerWithView(serializationView);
            }
            else if (filters != null) {
                objectWriter = this.objectMapper.writer(filters);
            }
            else {
                objectWriter = this.objectMapper.writer();
            }

            if (javaType != null && javaType.isContainerType()) {
                objectWriter = objectWriter.withType(javaType);
            }

            HttpOutput httpOutput = null;
            if(outputMessage.getBody() instanceof HttpOutput){
                httpOutput = (HttpOutput) outputMessage.getBody();
            }
            else{
                try {
                    //basic auth requests+responses get wrapped at some layer of Spring Security
                    //in which we can't access HttpOutput directly, so this is a hack to extract it from the wrapper.
                    Object body = FieldUtils.readField(outputMessage.getBody(), "delegate", true);
                    if (body instanceof HttpOutput){
                        httpOutput = (HttpOutput) body;
                    } else {
                        httpOutput = (HttpOutput) FieldUtils.readField(body, "delegate", true);
                    }
                }
                catch(Exception e){
                    logger.warn("Unable to extract httpOutput", e);
                }
            }

            if (httpOutput != null && shouldWrapObject(object) && shouldWrapResponse(httpOutput)){
                objectWriter = objectWriter.withRootName("data");
            }

            objectWriter.writeValue(generator, value);

            writeSuffix(generator, object);
            generator.flush();

        }
        catch (JsonProcessingException ex) {
            throw new HttpMessageNotWritableException("Could not write content: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        JavaType javaType = getJavaType(type, contextClass);
        return readJavaType(javaType, contextClass, inputMessage);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        JavaType javaType = getJavaType(clazz, null);
        return readJavaType(javaType, clazz, inputMessage);
    }

    @SuppressWarnings("deprecation")
    private Object readJavaType(JavaType javaType, Class<?> clazz, HttpInputMessage inputMessage) {

        ObjectReader objectReader = null;
        try {
            if (inputMessage instanceof MappingJacksonInputMessage) {
                Class<?> deserializationView = ((MappingJacksonInputMessage) inputMessage).getDeserializationView();
                if (deserializationView != null) {
                    objectReader = objectMapper.readerWithView(deserializationView).withType(javaType);
                }
            }

            if(objectReader == null){
                objectReader = objectMapper.reader(javaType);
            }

            if (shouldWrapObject(clazz.getName())){
                objectReader = objectReader.withRootName("data");
            }

            return objectReader.readValue(inputMessage.getBody());
        }
        catch (IOException ex) {
            throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex);
        }
    }

    private boolean shouldWrapObject(Object object) {

        String className = object.getClass().getName();
        for(String unwrappableClass : unwrappableClasses){
            if(className.contains(unwrappableClass)) return false;
        }

        for(String unwrappableParentClass : unwrappableParentClasses){
            try {
                if (Class.forName(unwrappableParentClass).isInstance(object))return false;
            }catch(Exception ex){
                logger.warn("Unable to check for unwrappable parent classes: " + ex.getMessage(), ex);
            }
        }

        return true;
    }


    private boolean shouldWrapResponse(HttpOutput httpOutputOptional) throws IOException{
        String requestUri = httpOutputOptional.getHttpChannel().getRequest().getRequestURI();

        for(String unwrappableRequestUri : unwrappableRequestUris){
            if(requestUri.contains(unwrappableRequestUri)) return false;
        }

        return true;
    }

}
