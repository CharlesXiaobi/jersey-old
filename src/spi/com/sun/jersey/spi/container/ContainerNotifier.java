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

package com.sun.jersey.spi.container;

/**
 * A container notifier that is used to register container listeners.
 * <p>
 * An implementation may be registered in a {@link ResourceConfig} using the
 * property (@link ResourceConfig#PROPERTY_CONTAINER_NOTIFIER}.
 * 
 * @author Paul.Sandoz@Sun.Com
 */
public interface ContainerNotifier {
    /**
     * Add a container listener to be notified when the container
     * events occur.
     * 
     * @param l the listener.
     */
    void addListener(ContainerListener l);
}