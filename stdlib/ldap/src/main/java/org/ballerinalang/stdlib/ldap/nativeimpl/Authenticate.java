/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.ldap.nativeimpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.ldap.CommonLdapConfiguration;
import org.ballerinalang.stdlib.ldap.LdapConnectionContext;
import org.ballerinalang.stdlib.ldap.LdapConstants;
import org.ballerinalang.stdlib.ldap.UserStoreException;
import org.ballerinalang.stdlib.ldap.util.LdapUtils;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.nio.charset.Charset;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

/**
 * Ballerina function to authenticate users with LDAP user store.
 *
 * @since 0.983.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "ldap",
        functionName = "doAuthenticate",
        args = {@Argument(name = "username", type = TypeKind.STRING),
                @Argument(name = "password", type = TypeKind.STRING)
        },
        returnType = {@ReturnType(type = TypeKind.BOOLEAN)})
public class Authenticate extends BlockingNativeCallableUnit {

    private static final Log LOG = LogFactory.getLog(Authenticate.class);
    private LdapConnectionContext connectionSource;
    private CommonLdapConfiguration ldapConfiguration;
    private DirContext ldapConnectionContext;

    @Override
    public void execute(Context context) {
        BMap<String, BValue> authStore = ((BMap<String, BValue>) context.getRefArgument(0));
        String userName = context.getStringArgument(0);
        byte[] credential = context.getStringArgument(1).getBytes(Charset.forName(LdapConstants.UTF_8_CHARSET));
        connectionSource = (LdapConnectionContext) authStore.getNativeData(LdapConstants.LDAP_CONNECTION_SOURCE);
        ldapConnectionContext = (DirContext) authStore.getNativeData(LdapConstants.LDAP_CONNECTION_CONTEXT);
        ldapConfiguration = (CommonLdapConfiguration) authStore.getNativeData(LdapConstants.LDAP_CONFIGURATION);
        LdapUtils.setServiceName((String) authStore.getNativeData(LdapConstants.ENDPOINT_INSTANCE_ID));

        if (LdapUtils.isNullOrEmptyAfterTrim(userName)) {
            context.setReturnValues(new BBoolean(false));
            throw new BallerinaException("username or credential value is empty or null.");
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticating user " + userName);
            }
            String name = LdapUtils.getNameInSpaceForUsernameFromLDAP(userName.trim(), ldapConfiguration, this
                    .ldapConnectionContext);
            if (name != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authenticating with " + name);
                }
                boolean bValue = this.bindAsUser(name, credential);
                context.setReturnValues(new BBoolean(bValue));
            } else {
                context.setReturnValues(new BBoolean(false));
            }
        } catch (NamingException e) {
            LOG.error("Cannot bind user : " + userName, e);
            context.setReturnValues(new BBoolean(false));
        } catch (UserStoreException e) {
            LOG.error(e.getMessage(), e);
            context.setReturnValues(new BBoolean(false));
        } finally {
            LdapUtils.removeServiceName();
        }
    }

    private boolean bindAsUser(String dn, byte[] credentials) throws NamingException {
        boolean isAuthenticated;
        LdapContext cxt = null;
        try {
            cxt = this.connectionSource.getContextWithCredentials(dn, credentials);
            isAuthenticated = true;
        } finally {
            LdapUtils.closeContext(cxt);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("User: " + dn + " is authenticated: " + isAuthenticated);
        }
        return isAuthenticated;
    }

    public static boolean doAuthenticate(Strand strand, ObjectValue authStore, String userName, String password) {
        byte[] credential = password.getBytes(Charset.forName(LdapConstants.UTF_8_CHARSET));
        LdapConnectionContext connectionSource = (LdapConnectionContext) authStore.getNativeData(
                LdapConstants.LDAP_CONNECTION_SOURCE);
        DirContext ldapConnectionContext = (DirContext) authStore.getNativeData(LdapConstants.LDAP_CONNECTION_CONTEXT);
        CommonLdapConfiguration ldapConfiguration = (CommonLdapConfiguration) authStore.getNativeData(
                LdapConstants.LDAP_CONFIGURATION);
        LdapUtils.setServiceName((String) authStore.getNativeData(LdapConstants.ENDPOINT_INSTANCE_ID));

        if (LdapUtils.isNullOrEmptyAfterTrim(userName)) {
            //TODO verify why both return and throw are needed
//            context.setReturnValues(new BBoolean(false));
            throw new BallerinaException("username or credential value is empty or null.");
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticating user " + userName);
            }
            String name = LdapUtils.getNameInSpaceForUsernameFromLDAP(userName.trim(), ldapConfiguration,
                                                                      ldapConnectionContext);
            if (name != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authenticating with " + name);
                }
                return bindAsUser(name, credential, connectionSource);
            }
            return false;
        } catch (NamingException e) {
            LOG.error("Cannot bind user : " + userName, e);
            return false;
        } catch (UserStoreException e) {
            LOG.error(e.getMessage(), e);
            return false;
        } finally {
            LdapUtils.removeServiceName();
        }
    }

    private static boolean bindAsUser(String userDN, byte[] credentials, LdapConnectionContext connectionSource)
            throws NamingException {
        LdapContext cxt = null;
        try {
            cxt = connectionSource.getContextWithCredentials(userDN, credentials);
        } finally {
            LdapUtils.closeContext(cxt);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("User: " + userDN + " is authenticated: true");
        }
        return true;
    }
}
