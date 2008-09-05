/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
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

package com.sun.jersey.impl.modelapi.validation;

import com.sun.jersey.impl.modelapi.validation.BasicValidator;
import com.sun.jersey.api.model.AbstractResource;
import com.sun.jersey.api.model.ResourceModelIssue;
import com.sun.jersey.impl.modelapi.annotation.IntrospectionModeller;
import com.sun.jersey.spi.resource.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import junit.framework.TestCase;

/**
 *
 * @author japod
 */
public class BasicValidatorTest extends TestCase {
    
    @Path("rootNoCtor")
    public static class TestRootResourceWithoutPublicConstructor {
        
        private TestRootResourceWithoutPublicConstructor() {};

        @GET
        public String getIt() {
            return "it";
        }
    }

    public void testRootResourceWithoutPublicConstructor() throws Exception {
        System.out.println("---\nAn issue should be reported if a public ctor is missing at a root resource:");
        AbstractResource ar = IntrospectionModeller.createResource(TestRootResourceWithoutPublicConstructor.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }

    @Path("rootAmbigCtors")
    public static class TestRootResourceAmbigCtors {
        
        public TestRootResourceAmbigCtors(@QueryParam("s") String p) {};

        public TestRootResourceAmbigCtors(@QueryParam("s") int p) {};

        @GET
        public String getIt() {
            return "it";
        }
    }

    public void testRootResourceAmbigConstructors() throws Exception {
        System.out.println("---\nAn issue should be reported if more public ctors exists with the same number of params at a root resource:");
        AbstractResource ar = IntrospectionModeller.createResource(TestRootResourceAmbigCtors.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(!validator.getIssueList().get(0).isFatal());
    }

    @Path("rootNonAmbigCtors")
    public static class TestRootResourceNonAmbigCtors {
        
        // TODO: hmmm, even if this is not ambiguous, it is strange; shall we warn, the 1st and the 2nd ctor won't be used?
        public TestRootResourceNonAmbigCtors(@QueryParam("s") String s) {};

        public TestRootResourceNonAmbigCtors(@QueryParam("n") int n) {};

        public TestRootResourceNonAmbigCtors(@QueryParam("s") String s, @QueryParam("n") int n) {};

        @GET
        public String getIt() {
            return "it";
        }
    }

    public void testRootResourceNonAmbigConstructors() throws Exception {
        System.out.println(
                "---\nNo issue should be reported if more public ctors exists with the same number of params, " +
                "but another just one is presented with more params at a root resource:");
        AbstractResource ar = IntrospectionModeller.createResource(TestRootResourceNonAmbigCtors.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(validator.getIssueList().isEmpty());
    }
    
    @Singleton
    @Path("rootSingleton/{p}")
    public static class TestCantInjectFieldsForSingleton {
        @MatrixParam("m") String matrixParam;
        @QueryParam("q") String queryParam;
        @PathParam("p") String pParam;
        @CookieParam("c") String cParam;
        @HeaderParam("h") String hParam;
        
        @GET
        public String getIt() {
            return "it";
        }
    }
    
    // this should be sorted out at runtime rather than during validation
    public void suspendedTestSingletonFieldsInjection() throws Exception {
        System.out.println("---\nAn issue should be reported if injection is required for a singleton life-cycle:");
        AbstractResource ar = IntrospectionModeller.createResource(TestCantInjectFieldsForSingleton.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertEquals(5, validator.getIssueList().size());
    }

    public static class TestNonPublicRM {
        @GET
        private String getIt() {
            return "this";
        }
    }
    
    public void testNonPublicRM() throws Exception {
        System.out.println("---\nAn issue should be reported if a resource method is not public:");
        AbstractResource ar = IntrospectionModeller.createResource(TestNonPublicRM.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        // TODO: there might still be an implicit viewable associated with it
        // assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    public static class TestMoreThanOneEntity {
        @PUT
        public void put(String one, String two) {
        }
    }
    
    // should be probably validated at runtime rather then at the validation phase
    public void suspendedTestMoreThanOneEntity() throws Exception {
        System.out.println("---\nAn issue should be reported if a resource method takes more than one entity params:");
        AbstractResource ar = IntrospectionModeller.createResource(TestMoreThanOneEntity.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
    }
    
    
    public static class TestGetRMReturningVoid {
        @GET
        public void getMethod() {
        }
    }
    
    public void testGetRMReturningVoid() throws Exception {
        System.out.println("---\nAn issue should be reported if a get method returns void:");
        AbstractResource ar = IntrospectionModeller.createResource(TestGetRMReturningVoid.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    public static class TestSRLReturningVoid {
        @Path("srl")
        public void srLocator() {
        }
    }
    
    public void testSRLReturningVoid() throws Exception {
        System.out.println("---\nAn issue should be reported if a sub-resource locator returns void:");
        AbstractResource ar = IntrospectionModeller.createResource(TestSRLReturningVoid.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
   
    public static class TestGetSRMReturningVoid {
        @GET @Path("srm")
        public void getSRMethod() {
        }
    }
    
    public void testGetSRMReturningVoid() throws Exception {
        System.out.println("---\nAn issue should be reported if a get sub-resource method returns void:");
        AbstractResource ar = IntrospectionModeller.createResource(TestGetSRMReturningVoid.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }

    @Path("emptyResource")
    public static class TestEmptyResource {
        public void getSRMethod() {
        }
    }
    
    public void testEmptyResource() throws Exception {
        System.out.println("---\nAn issue should be reported if a resource does not contain any method neither any locator:");
        AbstractResource ar = IntrospectionModeller.createResource(TestEmptyResource.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(!validator.getIssueList().get(0).isFatal());
    }
    
    @Path("rootAmbigResourceMethodsGET")
    public static class TestAmbigResourceMethodsGET {
        
        @GET @Produces("application/xml")
        public String getJson() {
            return "{}";
        }
        
        @GET @Produces("text/plain")
        public String getIt() {
            return "it";
        }

        @GET @Produces("text/plain")
        public String getItWithParam(@QueryParam("q") String q) {
            return String.format("it, q=%s", q);
        }
    }

    public void testAmbigResourceMethodsGET() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one GET method with same output mime-types exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigResourceMethodsGET.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }


    @Path("rootAmbigResourceMethodsPUT")
    public static class TestAmbigResourceMethodsPUT {
        
        @PUT @Consumes("application/xml")
        public void putJson(Object o) {
        }
        
        @PUT @Consumes({"text/plain", "text/xml"})
        public void putIt(Object o) {
        }

        @PUT @Consumes("text/plain")
        public void getItWithParam(@QueryParam("q") String q, Object o) {
        }
    }

    public void testAmbigResourceMethodsPUT() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one PUT method with same input mime-types exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigResourceMethodsPUT.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    
   @Target({ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   @HttpMethod("CUSTOM_HTTP_METHOD")
   public @interface CUSTOM_HTTP_METHOD {
   }
    
    @Path("rootAmbigResourceMethodsCUSTOM")
    public static class TestAmbigResourceMethodsCUSTOM {
        
        @CUSTOM_HTTP_METHOD @Consumes("application/xml")
        public void putJson(Object o) {
        }
        
        @CUSTOM_HTTP_METHOD @Consumes({"text/plain", "text/xml"})
        public void putIt(Object o) {
        }

        @CUSTOM_HTTP_METHOD @Consumes("text/plain")
        public void getItWithParam(@QueryParam("q") String q, Object o) {
        }
    }

    public void testAmbigResourceMethodsCUSTOM() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one CUSTOM_HTTP_METHOD method with same input mime-types exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigResourceMethodsCUSTOM.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    @Path("rootAmbigSubResourceMethodsGET")
    public static class TestAmbigSubResourceMethodsGET {
        
        @Path("{one}") @GET @Produces("application/xml")
        public String getJson() {
            return "{}";
        }
        
        @Path("{seven}") @GET @Produces("text/plain")
        public String getIt() {
            return "it";
        }

        @Path("{million}") @GET @Produces("text/plain")
        public String getItWithParam(@QueryParam("q") String q) {
            return String.format("it, q=%s", q);
        }
    }

    public void ignoredTestAmbigSubResourceMethodsGET() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one GET method with same output mime-types exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigSubResourceMethodsGET.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }


    @Path("rootAmbigSubResourceMethodsPUT")
    public static class TestAmbigSubResourceMethodsPUT {
        
        @Path("sub/{one}") @PUT @Consumes("application/xml")
        public void putJson(Object o) {
        }
        
        @Path("sub/{slash}/") @PUT @Consumes({"text/plain", "text/xml"})
        public void putIt(Object o) {
        }

        @Path("sub/{two}") @PUT @Consumes("text/plain")
        public void getItWithParam(@QueryParam("q") String q, Object o) {
        }
    }

    public void ignoredTestAmbigSubResourceMethodsPUT() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one PUT method with same input mime-types exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigSubResourceMethodsPUT.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    
    @Path("rootAmbigSubResourceMethodsCUSTOM")
    public static class TestAmbigSubResourceMethodsCUSTOM {
        
        @Path("sub/{a}") @CUSTOM_HTTP_METHOD @Consumes("application/xml")
        public void putJson(Object o) {
        }
        
        @Path("sub/{b}") @CUSTOM_HTTP_METHOD @Consumes({"text/plain", "text/xml"})
        public void putIt(Object o) {
        }

        @Path("sub/{c}") @CUSTOM_HTTP_METHOD @Consumes("text/plain")
        public void getItWithParam(@QueryParam("q") String q, Object o) {
        }
    }

    public void ignoredTestAmbigSubResourceMethodsCUSTOM() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one CUSTOM_HTTP_METHOD method with same input mime-types exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigSubResourceMethodsCUSTOM.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    @Path("rootAmbigSRLocators")
    public static class TestAmbigSRLocators {
        
        @Path("{one}")
        public String locatorOne(@PathParam("one") String one) {
            return "it";
        }

        @Path("{two}")
        public String locatorTwo(@PathParam("two") String two, @QueryParam("q") String q) {
            return String.format("it, q=%s", q);
        }
    }
    
    public void testAmbigSRLocators() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one sub-resource locator with the same path exists:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigSRLocators.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    @Path("rootAmbigSRLocatorsWithSlash")
    public static class TestAmbigSRLocatorsWithSlash {
        
        @Path("{one}")
        public String locatorOne(@PathParam("one") String one) {
            return "it";
        }

        @Path("{two}/")
        public String locatorTwo(@PathParam("two") String two, @QueryParam("q") String q) {
            return String.format("it, q=%s", q);
        }
    }
    
    public void testAmbigSRLocatorsWithSlash() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one sub-resource locator with paths differing only in ending slash exist:");
        AbstractResource ar = IntrospectionModeller.createResource(TestAmbigSRLocatorsWithSlash.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }
    
    @Path("rootMultipleHttpMethodDesignatorsRM")
    public static class TestMultipleHttpMethodDesignatorsRM {
        
        @GET @PUT
        public String getIt() {
            return "it";
        }
    }
    
    public void testMultipleHttpMethodDesignatorsRM() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one HTTP method designator exist on a resource method:");
        AbstractResource ar = IntrospectionModeller.createResource(TestMultipleHttpMethodDesignatorsRM.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }

    @Path("rootMultipleHttpMethodDesignatorsSRM")
    public static class TestMultipleHttpMethodDesignatorsSRM {
        
        @Path("srm") @POST @PUT
        public String doIt() {
            return "it";
        }
    }
    
    public void testMultipleHttpMethodDesignatorsSRM() throws Exception {
        System.out.println("---\nAn issue should be reported if more than one HTTP method designator exist on a sub-resource method:");
        AbstractResource ar = IntrospectionModeller.createResource(TestMultipleHttpMethodDesignatorsSRM.class);
        BasicValidator validator = new BasicValidator();
        validator.validate(ar);
        printIssueList(validator);
        assertTrue(!validator.getIssueList().isEmpty());
        assertTrue(validator.getIssueList().get(0).isFatal());
    }

    // TODO: test multiple root resources with the same uriTempl (in WebApplicationImpl.processRootResources ?)

    private static void printIssueList(BasicValidator validator) {
        for (ResourceModelIssue issue : validator.getIssueList()) {
            System.out.println((issue.isFatal() ? "ERROR: " : "WARNING: ") + issue.getMessage());
        }
    }
}
