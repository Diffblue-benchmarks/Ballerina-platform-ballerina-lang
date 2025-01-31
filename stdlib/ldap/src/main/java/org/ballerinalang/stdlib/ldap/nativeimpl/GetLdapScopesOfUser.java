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
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.ldap.CommonLdapConfiguration;
import org.ballerinalang.stdlib.ldap.LdapConstants;
import org.ballerinalang.stdlib.ldap.UserStoreException;
import org.ballerinalang.stdlib.ldap.util.LdapUtils;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * Provides the scopes of a given user.
 *
 * @since 0.983.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "ldap",
        functionName = "getLdapScopes",
        args = {@Argument(name = "username", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.ARRAY, elementType = TypeKind.STRING)},
        isPublic = true)
public class GetLdapScopesOfUser extends BlockingNativeCallableUnit {

    private static final Log LOG = LogFactory.getLog(GetLdapScopesOfUser.class);

    @Override
    public void execute(Context context) {
        try {
            BMap<String, BValue> authStore = ((BMap<String, BValue>) context.getRefArgument(0));
            LdapUtils.setServiceName((String) authStore.getNativeData(LdapConstants.ENDPOINT_INSTANCE_ID));
            DirContext ldapConnectionContext = (DirContext) authStore.getNativeData(
                    LdapConstants.LDAP_CONNECTION_CONTEXT);
            CommonLdapConfiguration ldapConfiguration = (CommonLdapConfiguration) authStore.getNativeData(
                    LdapConstants.LDAP_CONFIGURATION);
            String userName = context.getStringArgument(0);
            String[] externalRoles = doGetGroupsListOfUser(userName, ldapConfiguration, ldapConnectionContext);
            context.setReturnValues(new BValueArray(externalRoles));
        } catch (UserStoreException | NamingException e) {
            context.setReturnValues(new BValueArray(BTypes.typeString));
        } finally {
            LdapUtils.removeServiceName();
        }
    }

    public static ArrayValue getScopes(Strand strand, ObjectValue authStore, String userName) {
        try {
            LdapUtils.setServiceName((String) authStore.getNativeData(LdapConstants.ENDPOINT_INSTANCE_ID));
            DirContext ldapConnectionContext = (DirContext) authStore.getNativeData(
                    LdapConstants.LDAP_CONNECTION_CONTEXT);
            CommonLdapConfiguration ldapConfiguration = (CommonLdapConfiguration) authStore.getNativeData(
                    LdapConstants.LDAP_CONFIGURATION);
            String[] externalRoles = doGetGroupsListOfUser(userName, ldapConfiguration, ldapConnectionContext);
            return new ArrayValue(externalRoles);
        } catch (UserStoreException | NamingException e) {
            return new ArrayValue(org.ballerinalang.jvm.types.BTypes.typeString);
        } finally {
            LdapUtils.removeServiceName();
        }
    }

    private static String[] doGetGroupsListOfUser(String userName, CommonLdapConfiguration ldapAuthConfig,
                                                  DirContext ldapConnectionContext)
            throws UserStoreException, NamingException {
        // Get the effective search base
        List<String> searchBase = ldapAuthConfig.getGroupSearchBase();
        return getLDAPGroupsListOfUser(userName, searchBase, ldapAuthConfig, ldapConnectionContext);
    }

    private static String[] getLDAPGroupsListOfUser(String userName, List<String> searchBase,
                                                    CommonLdapConfiguration ldapAuthConfig,
                                                    DirContext ldapConnectionContext)
                                             throws UserStoreException, NamingException {
        if (userName == null) {
            throw new BallerinaException("userName value is null.");
        }

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        // Load normal roles with the user
        String searchFilter = ldapAuthConfig.getGroupNameListFilter();
        String roleNameProperty = ldapAuthConfig.getGroupNameAttribute();
        String membershipProperty = ldapAuthConfig.getMembershipAttribute();
        String nameInSpace = getNameInSpaceForUserName(userName, ldapAuthConfig, ldapConnectionContext);

        if (membershipProperty == null || membershipProperty.length() < 1) {
            throw new BallerinaException("membershipAttribute not set in configuration");
        }

        String membershipValue;
        if (nameInSpace != null) {
            LdapName ldn = new LdapName(nameInSpace);
            if (LdapConstants.MEMBER_UID.equals(ldapAuthConfig.getMembershipAttribute())) {
                // membership value of posixGroup is not DN of the user
                List rdns = ldn.getRdns();
                membershipValue = ((Rdn) rdns.get(rdns.size() - 1)).getValue().toString();
            } else {
                membershipValue = escapeLdapNameForFilter(ldn);
            }
        } else {
            return new String[0];
        }

        searchFilter = "(&" + searchFilter + "(" + membershipProperty + "=" + membershipValue + "))";
        String returnedAtts[] = {roleNameProperty};
        searchCtls.setReturningAttributes(returnedAtts);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading roles with the membershipProperty Property: " + membershipProperty);
        }

        List<String> list = getListOfNames(searchBase, searchFilter, searchCtls, roleNameProperty,
                                           ldapConnectionContext);
        return list.toArray(new String[list.size()]);
    }

    private static List<String> getListOfNames(List<String> searchBases, String searchFilter, SearchControls searchCtls,
                                               String property, DirContext ldapConnectionContext)
            throws NamingException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Result for searchBase: " + searchBases + " searchFilter: " + searchFilter +
                    " property:" + property + " appendDN: false");
        }

        List<String> names = new ArrayList<>();
        NamingEnumeration<SearchResult> answer = null;
        try {
            // handle multiple search bases
            for (String searchBase : searchBases) {
                answer = ldapConnectionContext.search(LdapUtils.escapeDNForSearch(searchBase),
                        searchFilter, searchCtls);
                while (answer.hasMoreElements()) {
                    SearchResult searchResult = answer.next();
                    if (searchResult.getAttributes() == null) {
                        continue;
                    }
                    Attribute attr = searchResult.getAttributes().get(property);
                    if (attr == null) {
                        continue;
                    }
                    for (Enumeration vals = attr.getAll(); vals.hasMoreElements(); ) {
                        String name = (String) vals.nextElement();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found user: " + name);
                        }
                        names.add(name);
                    }
                }

                if (LOG.isDebugEnabled()) {
                    for (String name : names) {
                        LOG.debug("Result  :  " + name);
                    }
                }
            }
        } finally {
            LdapUtils.closeNamingEnumeration(answer);
        }
        return names;
    }

    /**
     * Takes the corresponding name for a given username from LDAP.
     *
     * @param userName Given username
     * @param ldapConfiguration LDAP user store configurations
     * @param ldapConnectionContext connection context
     * @return Associated name for the given username
     * @throws UserStoreException if there is any exception occurs during the process
     */
    private static String getNameInSpaceForUserName(String userName, CommonLdapConfiguration ldapConfiguration,
                                                    DirContext ldapConnectionContext)
            throws UserStoreException, NamingException {
        return LdapUtils.getNameInSpaceForUsernameFromLDAP(userName, ldapConfiguration, ldapConnectionContext);
    }

    /**
     * This method escapes the special characters in a LdapName according to the ldap filter escaping standards.
     *
     * @param ldn LDAP name
     * @return A String which special characters are escaped
     */
    private static String escapeLdapNameForFilter(LdapName ldn) {
        if (ldn == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received null value to escape special characters. Returning null");
            }
            return null;
        }

        StringBuilder escapedDN = new StringBuilder();
        for (int i = ldn.size() - 1; i > -1; i--) { //escaping the rdns separately and re-constructing the DN
            escapedDN = escapedDN.append(escapeSpecialCharactersForFilterWithStarAsRegex(ldn.get(i)));
            if (i != 0) {
                escapedDN = escapedDN.append(",");
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Escaped DN value for filter : " + escapedDN.toString());
        }
        return escapedDN.toString();
    }

    /**
     * Escaping ldap search filter special characters in a string.
     *
     * @param filter LDAP search filter
     * @return A String which special characters are escaped
     */
    private static String escapeSpecialCharactersForFilterWithStarAsRegex(String filter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filter.length(); i++) {
            char currentChar = filter.charAt(i);
            switch (currentChar) {
                case '\\':
                    if (filter.charAt(i + 1) == '*') {
                        sb.append("\\2a");
                        i++;
                        break;
                    }
                    sb.append("\\5c");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\u0000':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(currentChar);
            }
        }
        return sb.toString();
    }
}
