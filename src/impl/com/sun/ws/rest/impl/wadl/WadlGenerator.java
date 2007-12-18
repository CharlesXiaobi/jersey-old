/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.php
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * WadlGenerator.java
 *
 * Created on November 30, 2007, 2:09 PM
 *
 */

package com.sun.ws.rest.impl.wadl;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.ParamStyle;
import com.sun.research.ws.wadl.RepresentationType;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;
import com.sun.ws.rest.api.model.AbstractResource;
import com.sun.ws.rest.api.model.AbstractResourceMethod;
import com.sun.ws.rest.api.model.AbstractSubResourceLocator;
import com.sun.ws.rest.api.model.AbstractSubResourceMethod;
import com.sun.ws.rest.api.model.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 *
 * @author mh124079
 */
public class WadlGenerator {
    
    /**
     * Generate WADL for a set of resources.
     * @param resources the set of resources
     * @return the JAXB WADL application bean
     */
    public static Application generate(Set<AbstractResource> resources) {
        Application wadlApplication = new Application();
        Resources wadlResources = new Resources();
        
        // for each resource
        for (AbstractResource r: resources) {
            Resource wadlResource = generateResource(r);
            wadlResources.getResource().add(wadlResource);
        }
        wadlApplication.setResources(wadlResources);
        return wadlApplication;
    }
    
    /**
     * Generate WADL for a resource.
     * @param resource the resource
     * @return the JAXB WADL application bean
     */
    public static Application generate(AbstractResource resource) {
        Application wadlApplication = new Application();
        Resources wadlResources = new Resources();
        Resource wadlResource = generateResource(resource);
        wadlResources.getResource().add(wadlResource);
        wadlApplication.setResources(wadlResources);
        return wadlApplication;
    }

    /**
     * Generate WADL for a virtual subresource resulting from sub resource
     * methods.
     * @param resource the parent resource
     * @param path the value of the methods path annotations
     * @return the JAXB WADL application bean
     */
    public static Application generate(AbstractResource resource, String path) {
        Application wadlApplication = new Application();
        Resources wadlResources = new Resources();
        Resource wadlResource = generateSubResource(resource, path);
        wadlResources.getResource().add(wadlResource);
        wadlApplication.setResources(wadlResources);
        return wadlApplication;
    }

    private static com.sun.research.ws.wadl.Method generateMethod(final Map<String, Param> wadlResourceParams, final AbstractResourceMethod m) {
        com.sun.research.ws.wadl.Method wadlMethod = 
                new com.sun.research.ws.wadl.Method();
        wadlMethod.setName(m.getHttpMethod());
        // generate the request part
        Request wadlRequest = generateRequest(m, wadlResourceParams);
        if (wadlRequest != null)
            wadlMethod.setRequest(wadlRequest);
        // generate the response part
        Response wadlResponse = generateResponse(m);
        if (wadlResponse != null)
            wadlMethod.setResponse(wadlResponse);
        return wadlMethod;
    }

    private static Request generateRequest(final AbstractResourceMethod m, 
            Map<String,Param> wadlResourceParams) {
        if (m.getParameters().size()==0)
            return null;
        
        Request wadlRequest = new Request();

        for (Parameter p: m.getParameters()) {
            if (p.getSource()==Parameter.Source.ENTITY) {
                for (MediaType mediaType: m.getSupportedInputTypes()) {
                    RepresentationType wadlRepresentation = new RepresentationType();
                    wadlRepresentation.setMediaType(mediaType.toString());
                    wadlRequest.getRepresentation().add(wadlRepresentation);
                }
            } else {
                Param wadlParam = generateParam(p);
                if (wadlParam == null)
                    continue;
                if (wadlParam.getStyle()==ParamStyle.TEMPLATE)
                    wadlResourceParams.put(wadlParam.getName(),wadlParam);
                else
                    wadlRequest.getParam().add(wadlParam);
            }
        }
        if (wadlRequest.getRepresentation().size()+wadlRequest.getParam().size() == 0)
            return null;
        else
            return wadlRequest;
    }

    private static Param generateParam(final Parameter p) {
        if (p.getSource()==Parameter.Source.ENTITY || p.getSource()==Parameter.Source.CONTEXT)
            return null;
        Param wadlParam = new Param();
        wadlParam.setName(p.getSourceName());
        switch (p.getSource()) {
            case QUERY: 
                wadlParam.setStyle(ParamStyle.QUERY);
                break;
            case MATRIX:
                wadlParam.setStyle(ParamStyle.MATRIX);
                break;
            case URI:
                wadlParam.setStyle(ParamStyle.TEMPLATE);
                break;
            case HEADER:
                wadlParam.setStyle(ParamStyle.HEADER);
                break;
            default:
                break;
        }
        if (p.hasDefaultValue())
            wadlParam.setDefault(p.getDefaultValue());
        Class pClass = p.getParameterClass();
        if (pClass.isArray()) {
            wadlParam.setRepeating(true);
            pClass = pClass.getComponentType();
        }
        if (pClass.equals(int.class) || pClass.equals(Integer.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "int", "xs"));
        else if (pClass.equals(boolean.class) || pClass.equals(Boolean.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "boolean", "xs"));
        else if (pClass.equals(long.class) || pClass.equals(Long.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "long", "xs"));
        else if (pClass.equals(short.class) || pClass.equals(Short.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "short", "xs"));
        else if (pClass.equals(byte.class) || pClass.equals(Byte.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "byte", "xs"));
        else if (pClass.equals(float.class) || pClass.equals(Float.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "float", "xs"));
        else if (pClass.equals(double.class) || pClass.equals(Double.class))
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "double", "xs"));
        else
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "string", "xs"));
        return wadlParam;
    }

    private static Resource generateResource(AbstractResource r) {
        Resource wadlResource = new Resource();
        wadlResource.setPath(r.getUriTemplate().getValue());
        // for each resource method
        Map<String, Param> wadlResourceParams = new HashMap<String, Param>();
        for (AbstractResourceMethod m : r.getResourceMethods()) {
            com.sun.research.ws.wadl.Method wadlMethod = generateMethod(wadlResourceParams, m);
            wadlResource.getMethodOrResource().add(wadlMethod);
        }
        // add parameters that are associated with the resource URI template
        for (Param wadlParam : wadlResourceParams.values()) {
            wadlResource.getParam().add(wadlParam);
        }

        // for each sub-resource method
        Map<String, Resource> wadlSubResources = new HashMap<String, Resource>();
        Map<String, Map<String, Param>> wadlSubResourcesParams = 
                new HashMap<String, Map<String, Param>>();
        for (AbstractSubResourceMethod m : r.getSubResourceMethods()) {
            // find or create sub resource for uri template
            String template = m.getUriTemplate().getValue();
            Resource wadlSubResource = wadlSubResources.get(template);
            Map<String, Param> wadlSubResourceParams = wadlSubResourcesParams.get(template);
            if (wadlSubResource == null) {
                wadlSubResource = new Resource();
                wadlSubResource.setPath(template);
                wadlSubResources.put(template, wadlSubResource);
                wadlSubResourceParams = new HashMap<String, Param>();
                wadlSubResourcesParams.put(template, wadlSubResourceParams);
                wadlResource.getMethodOrResource().add(wadlSubResource);
            }
            com.sun.research.ws.wadl.Method wadlMethod = generateMethod(wadlSubResourceParams, m);
            wadlSubResource.getMethodOrResource().add(wadlMethod);
        }
        // add parameters that are associated with each sub-resource method URI template
        for (String template : wadlSubResources.keySet()) {
            Resource wadlSubResource = wadlSubResources.get(template);
            Map<String, Param> wadlSubResourceParams = wadlSubResourcesParams.get(template);
            for (Param wadlParam : wadlSubResourceParams.values()) {
                wadlSubResource.getParam().add(wadlParam);
            }
        }

        // for each sub resource locator
        for (AbstractSubResourceLocator l : r.getSubResourceLocators()) {
            Resource wadlSubResource = new Resource();
            wadlSubResource.setPath(l.getUriTemplate().getValue());
            for (Parameter p : l.getParameters()) {
                Param wadlParam = generateParam(p);
                wadlSubResource.getParam().add(wadlParam);
            }
            wadlResource.getMethodOrResource().add(wadlSubResource);
        }
        return wadlResource;
    }

    private static Resource generateSubResource(AbstractResource r, String path) {
        Resource wadlResource = new Resource();
        StringBuilder b = new StringBuilder(r.getUriTemplate().getValue());
        if (!(r.getUriTemplate().getValue().endsWith("/") || path.startsWith("/")))
            b.append("/");
        b.append(path);
        wadlResource.setPath(b.toString());

        // for each sub-resource method
        Map<String, Param> wadlSubResourceParams = new HashMap<String, Param>();
        for (AbstractSubResourceMethod m : r.getSubResourceMethods()) {
            // find or create sub resource for uri template
            String template = m.getUriTemplate().getValue();
            if (!template.equals(path))
                continue;
            com.sun.research.ws.wadl.Method wadlMethod = generateMethod(wadlSubResourceParams, m);
            wadlResource.getMethodOrResource().add(wadlMethod);
        }
        // add parameters that are associated with each sub-resource method URI template
        for (Param wadlParam : wadlSubResourceParams.values()) {
            wadlResource.getParam().add(wadlParam);
        }

        return wadlResource;
    }

    private static Response generateResponse(final AbstractResourceMethod m) {
        if (m.getMethod().getReturnType() == void.class)
            return null;
        Response wadlResponse = new Response();
        for (MediaType mediaType: m.getSupportedOutputTypes()) {
            RepresentationType wadlRepresentation = new RepresentationType();
            wadlRepresentation.setMediaType(mediaType.toString());
            JAXBElement<RepresentationType> element = new JAXBElement<RepresentationType>(
                    new QName("http://research.sun.com/wadl/2006/10","representation"),
                    RepresentationType.class,
                    wadlRepresentation);
            wadlResponse.getRepresentationOrFault().add(element);
        }
        return wadlResponse;
    }
    
    
}
