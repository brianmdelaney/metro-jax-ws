/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package epr.epr_webparam_header.server;

import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.jws.WebService;

/**
 * @author Rama Pulavarthi
 */
@WebService(endpointInterface = "epr.epr_webparam_header.server.Test")
public class TestImpl {
    public W3CEndpointReference test( W3CEndpointReference parameters){
        return parameters;
    }
}
