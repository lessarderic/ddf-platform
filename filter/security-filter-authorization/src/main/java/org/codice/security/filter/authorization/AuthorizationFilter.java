/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.security.filter.authorization;

import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import org.apache.shiro.authz.Authorizer;
import org.codice.security.policy.context.ContextPolicy;
import org.codice.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handler that implements authorization checking for contexts.
 */
public class AuthorizationFilter implements Filter {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(AuthorizationFilter.class);

    private final ContextPolicyManager contextPolicyManager;

    private List<String> roles = new ArrayList<String>();

    private final Authorizer authorizer;

    public AuthorizationFilter(Authorizer authorizer, ContextPolicyManager contextPolicyManager) {
        super();
        this.authorizer = authorizer;
        this.contextPolicyManager = contextPolicyManager;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Starting AuthZ filter.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Subject subject = (Subject) httpRequest.getAttribute("ddf.security.subject");

        ContextPolicy policy = contextPolicyManager.getContextPolicy(httpRequest.getContextPath());

        Collection<CollectionPermission> permissions = policy.getAllowedAttributePermissions();

        boolean permitted = true;
        for(CollectionPermission permission : permissions) {
            if(!authorizer.isPermittedAll(subject.getPrincipals(), permission.getPermissionList())) {
                permitted = false;
            }
        }

        if (!permitted) {
            LOGGER.debug("Subject not authorized.");
            returnNotAuthorized(httpResponse);
        } else {
            LOGGER.debug("Subject is authorized!");
            chain.doFilter(request, response);
        }
    }

    private void returnNotAuthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response: {}", ioe);
        }

    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying AuthZ filter.");
    }

}
