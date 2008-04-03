/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License. 
 * 
 * You can obtain a copy of the License at:
 *     https://jersey.dev.java.net/license.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at:
 *     https://jersey.dev.java.net/license.txt
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyrighted [year] [name of copyright owner]"
 */

package com.sun.ws.rest.impl.modelapi.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

/**
 * 
 * @author Paul.Sandoz@Sun.Com
 */
public final class AnnotatedMethod implements AnnotatedElement {
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> 
            METHOD_META_ANNOTATIONS = getSet(
                HttpMethod.class);
    
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> 
            METHOD_ANNOTATIONS = getSet(
                Path.class, 
                ProduceMime.class, 
                ConsumeMime.class);
        
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> 
            PARAMETER_ANNOTATIONS = getSet( 
        Context.class, 
        Encoded.class, 
        DefaultValue.class, 
        QueryParam.class,
        CookieParam.class, 
        HeaderParam.class, 
        PathParam.class);
    
    private static Set<Class<? extends Annotation>> getSet(Class<? extends Annotation>... cs) {
        Set<Class<? extends Annotation>> s = new HashSet<Class<? extends Annotation>>();
        for (Class<? extends Annotation> c : cs) s.add(c);
        return s;
    }
        
    private final Method m;
    
    private final Method am;

    private final Annotation[] methodAnnotations;
    
    private final Annotation[][] parameterAnnotations;
    
    public AnnotatedMethod(Method m) {
        this.m = m;
        this.am = findAnnotatedMethod(m);
        
        if (m.equals(am)) {
            methodAnnotations = m.getAnnotations();
            parameterAnnotations = m.getParameterAnnotations();
        } else {
            methodAnnotations = mergeMethodAnnotations(m, am);
            parameterAnnotations = mergeParameterAnnotations(m, am);
        }
    }

    public Method getMethod() {
        return m;
    }

    public Method getAnnotatedMethod() {
        return am;
    }
    
    public Annotation[][] getParameterAnnotations() {
        return parameterAnnotations.clone();
    }
    
    public Class<?>[] getParameterTypes() {
        return m.getParameterTypes();
    }
    
    public TypeVariable<Method>[] getTypeParameters() {
        return m.getTypeParameters();
    } 
    
    public Type[] getGenericParameterTypes() {
        return m.getGenericParameterTypes();
    }
                            
    public <T extends Annotation> List<T> getMetaMethodAnnotations(
            Class<T> annotation) {
        List <T> ma = new ArrayList<T>();
        for (Annotation a : methodAnnotations) {
            if (a.annotationType().getAnnotation(annotation) != null) {
                ma.add(a.annotationType().getAnnotation(annotation));
            }
        }
        
        return ma;
    }
    
    @Override
    public String toString() {
        return m.toString();
    }
    
    // AnnotatedElement
    
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        for (Annotation ma : methodAnnotations) {
            if (ma.annotationType() == annotationType)
                return true;
        }
        return false;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation ma : methodAnnotations) {
            if (ma.annotationType() == annotationType)
                return annotationType.cast(ma);
        }
        return am.getAnnotation(annotationType);
    }

    public Annotation[] getAnnotations() {
        return methodAnnotations.clone();
    }

    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    
    // 
    
    private static Annotation[] mergeMethodAnnotations(Method m, Method am) {
        List<Annotation> al = asList(m.getAnnotations());
        for (Annotation a : am.getAnnotations()) {
            if (isMethodAnnotation(a))
                al.add(a);
        }
        
        return al.toArray(new Annotation[0]);
    }
    
    private static Annotation[][] mergeParameterAnnotations(Method m, Method am) {        
        Annotation[][] mp = m.getParameterAnnotations();
        Annotation[][] amp = am.getParameterAnnotations();
        
        List<List<Annotation>> ala = new ArrayList<List<Annotation>>();        
        for (int i = 0; i < mp.length; i++) {
            List<Annotation> al = asList(mp[i]);
            for (Annotation a : amp[i])
                if (isParameterAnnotation(a))
                    al.add(a);
            ala.add(al);            
        }
        
        Annotation[][] paa = new Annotation[mp.length][];
        for (int i = 0; i < mp.length; i++) {
            paa[i] = ala.get(i).toArray(new Annotation[0]);
        }
        
        return paa;
    }
    
    private static Method findAnnotatedMethod(Method m) {   
        Method am = findAnnotatedMethod(m.getDeclaringClass(), m);
        return (am != null) ? am : m;
    }
    
    private static Method findAnnotatedMethod(Class<?> c, Method m) {
        try {
            m = c.getMethod(m.getName(), m.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            return null;
        }

        if (hasAnnotations(m)) return m;
        
        for (Class<?> ic : c.getInterfaces()) {
            Method im = findAnnotatedMethod(ic, m);
            if (im != null) return im;
        }

        Class<?> sc = c.getSuperclass();
        if (sc != null) {
            Method sm = findAnnotatedMethod(sc, m);
            if (sm != null) return sm;
        }

        return null;
    }    
    
    private static boolean isMethodAnnotation(Annotation a) {
        for (Class<? extends Annotation> ac : METHOD_META_ANNOTATIONS) {
            if (a.annotationType().getAnnotation(ac) != null) return true;            
        }

        return METHOD_ANNOTATIONS.contains(a.annotationType());
    }
    
    private static boolean isParameterAnnotation(Annotation a) {
        return PARAMETER_ANNOTATIONS.contains(a.annotationType());
    }
    
    private static boolean hasAnnotations(Method m) {
        return hasMetaMethodAnnotations(m) || 
                hasMethodAnnotations(m) || 
                hasParameterAnnotations(m);
    }
    
    private static boolean hasMetaMethodAnnotations(Method m) {
        for (Class<? extends Annotation> ac : METHOD_META_ANNOTATIONS)
            for (Annotation a : m.getAnnotations())
                if (a.annotationType().getAnnotation(ac) != null) return true;
        
        return false;
    }
    
    private static boolean hasMethodAnnotations(Method m) {
        for (Class<? extends Annotation> ac : METHOD_ANNOTATIONS)
            if (m.isAnnotationPresent(ac)) return true;
        
        return false;
    }
    
    private static boolean hasParameterAnnotations(Method m) {
        for (Annotation[] as : m.getParameterAnnotations()) 
            for (Annotation a : as)
                if (PARAMETER_ANNOTATIONS.contains(a)) return true;
                
        return false;
    }
    
    private static <T> List<T> asList(T... ts) {
        List<T> l = new ArrayList<T>();
        for (T t : ts) l.add(t);
        return l;
    }
}