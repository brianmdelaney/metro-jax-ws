/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.client;

import java.util.concurrent.ExecutionException;

import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.Component;
import com.sun.xml.ws.api.ComponentFeature;
import com.sun.xml.ws.api.ComponentFeature.Target;
import com.sun.xml.ws.api.client.ThrowableInPacketCompletionFeature;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.ClientTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.ServerTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.ThrowableContainerPropertySet;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.TubelineAssembler;
import com.sun.xml.ws.api.pipe.TubelineAssemblerFactory;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;

import junit.framework.TestCase;

public class ThrowableInPacketCompletionTest extends TestCase {
    private static final QName SERVICE_NAME = new QName("http://test.oracle.com", "TestService");
    private static final QName PORT_NAME = new QName("http://test.oracle.com", "TestPort");

    public void testThrowableInPacket() {
        Service service = Service.create(SERVICE_NAME, serviceFeatures());
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:7001/TestService/TestPort");
        Dispatch<Packet> dispatch = service.createDispatch(PORT_NAME, Packet.class, Mode.MESSAGE, portFeatures(true));
        
        Packet request = new Packet();
        try {
            Packet response = dispatch.invoke(request);
            assertNotNull(response);
            
            ThrowableContainerPropertySet ps = response.getSatellite(ThrowableContainerPropertySet.class);
            assertNotNull(ps);
            assertTrue(ps.getThrowable() instanceof TestException);
            
        } catch (Throwable t) {
            fail("Exception not expected, but got: " + t.getMessage());
        }
    }
    
    public void testThrowable() {
        Service service = Service.create(SERVICE_NAME, serviceFeatures());
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:7001/TestService/TestPort");
        Dispatch<Packet> dispatch = service.createDispatch(PORT_NAME, Packet.class, Mode.MESSAGE, portFeatures(false));
        
        Packet request = new Packet();
        try {
            Packet response = dispatch.invoke(request);
            assertNotNull(response);
            
            ThrowableContainerPropertySet ps = response.getSatellite(ThrowableContainerPropertySet.class);
            fail("Exception expected, but got none");
            
        } catch (Throwable t) {
            assertTrue(t instanceof WebServiceException);
            assertTrue(t.getCause() instanceof TestException);
        }
    }
    
    public void testThrowableInPacketAsync() {
        Service service = Service.create(SERVICE_NAME, serviceFeatures());
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:7001/TestService/TestPort");
        Dispatch<Packet> dispatch = service.createDispatch(PORT_NAME, Packet.class, Mode.MESSAGE, portFeatures(true));
        
        Packet request = new Packet();
        try {
            Response<Packet> response = dispatch.invokeAsync(request);
            assertNotNull(response);
            
            Packet responsePacket = response.get();
            assertNotNull(responsePacket);
            
            ThrowableContainerPropertySet ps = responsePacket.getSatellite(ThrowableContainerPropertySet.class);
            assertNotNull(ps);
            assertTrue(ps.getThrowable() instanceof TestException);
            
        } catch (Throwable t) {
            fail("Exception not expected, but got: " + t.getMessage());
        }
    }
    
    public void testThrowableAsync() {
        Service service = Service.create(SERVICE_NAME, serviceFeatures());
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:7001/TestService/TestPort");
        Dispatch<Packet> dispatch = service.createDispatch(PORT_NAME, Packet.class, Mode.MESSAGE, portFeatures(false));
        
        Packet request = new Packet();
        try {
            Response<Packet> response = dispatch.invokeAsync(request);
            assertNotNull(response);
            
            Packet responsePacket = response.get();
            assertNotNull(responsePacket);
            
            ThrowableContainerPropertySet ps = responsePacket.getSatellite(ThrowableContainerPropertySet.class);
            fail("Exception expected, but got none");
            
        } catch (Throwable t) {
            assertTrue(t instanceof ExecutionException);
            assertTrue(t.getCause() instanceof WebServiceException);
            assertTrue(t.getCause().getCause() instanceof TestException);
        }
    }
    
    private static WebServiceFeature[] portFeatures(boolean isAddThrowableInPacketCompletionFeature) {
        return isAddThrowableInPacketCompletionFeature ?
                new WebServiceFeature[] { new ThrowableInPacketCompletionFeature() } :
                new WebServiceFeature[] { };
    }
    
    private static WebServiceFeature[] serviceFeatures() {
        ComponentFeature cf = new ComponentFeature(new Component() {

            @Override
            @Nullable
            public <S> S getSPI(@NotNull Class<S> spiType) {
                if (spiType.equals(TubelineAssemblerFactory.class)) {
                    return spiType.cast(new TubelineAssemblerFactory() {

                        @Override
                        public TubelineAssembler doCreate(BindingID bindingId) {
                            return new TubelineAssemblerImpl();
                        }
                        
                    });
                }
                return null;
            }
            
        }, Target.CONTAINER);
        
        return new WebServiceFeature[] { cf };
    }
    
    private static class TubelineAssemblerImpl implements TubelineAssembler {

        @Override
        @NotNull
        public Tube createClient(@NotNull ClientTubeAssemblerContext context) {
            return new TubeImpl();
        }

        @Override
        @NotNull
        public Tube createServer(@NotNull ServerTubeAssemblerContext context) {
            return new TubeImpl();
        }
        
    }
    
    private static class TubeImpl extends AbstractTubeImpl {
        public TubeImpl() {}
        
        public TubeImpl(TubeImpl that, TubeCloner cloner) {
            super(that, cloner);
        }
        
        @Override
        @NotNull
        public NextAction processRequest(@NotNull Packet request) {
            throw new TestException("Exception intentionally thrown for test");
        }

        @Override
        @NotNull
        public NextAction processResponse(@NotNull Packet response) {
            throw new IllegalStateException();
        }

        @Override
        @NotNull
        public NextAction processException(@NotNull Throwable t) {
            return doThrow(t);
        }

        @Override
        public void preDestroy() {
        }

        @Override
        public AbstractTubeImpl copy(TubeCloner cloner) {
            return new TubeImpl(this, cloner);
        }
        
    }
    
    private static class TestException extends RuntimeException {
        public TestException(String msg) {
            super(msg);
        }
    }
}
