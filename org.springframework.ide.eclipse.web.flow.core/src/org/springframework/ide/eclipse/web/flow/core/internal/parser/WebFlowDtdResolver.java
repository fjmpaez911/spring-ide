/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ide.eclipse.web.flow.core.internal.parser;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class WebFlowDtdResolver implements EntityResolver {

    private static final String DTD_NAME = "spring-webflow";

    private static final String SEARCH_PACKAGE = "/org/springframework/web/flow/config/";

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXParseException {
        if (systemId != null
                && systemId.indexOf(DTD_NAME) > systemId.lastIndexOf("/")) {
            String dtdFile = systemId.substring(systemId.indexOf(DTD_NAME));
            try {
                Resource resource = new ClassPathResource(SEARCH_PACKAGE
                        + dtdFile, getClass());
                InputSource source = new InputSource(resource.getInputStream());
                source.setPublicId(publicId);
                source.setSystemId(systemId);
                return source;
            }
            catch (IOException ex) {
                throw new SAXParseException("Spring WebFlow Dtd not found", publicId, systemId, 1, 1);
            }
        }
        throw new SAXParseException("Spring WebFlow Dtd not declared", publicId, systemId, 1, 1);
    }

}