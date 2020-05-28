package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapRoleResolveSettings;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This LdapAuthoritiesPopulator is used to populate user object with roles upon basic authentication when they are
 * authenticated by a ldap server.
 */
public class CustomUserAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private InspectitServerSettings settings;

    public CustomUserAuthoritiesMapper(InspectitServerSettings settings) {
        this.settings = settings;
    }

    /**
     * Maps the in the ldap section of the server config defined ldap roles to the role set defined in
     * {@link UserRoleConfiguration}. Always returns the role-set with highest access level if user contains multiple
     * matching authorities.
     *
     * @param authorities A List of GrantedAuthority-Objects that should be mapped.
     *
     * @return The highest level of access role the user's authorities could be resolved to.
     */
    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        LdapRoleResolveSettings role_settings = settings.getSecurity().getLdap().getRoles();
        if (containsAuthority(authorities, role_settings.getAdmin()) || hasAdminGroup(authorities)) {
            return UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getCommit())) {
            return UserRoleConfiguration.COMMIT_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getWrite())) {
            return UserRoleConfiguration.WRITE_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getRead())) {
            return UserRoleConfiguration.READ_ROLE_PERMISSION_SET;
        }
        return Collections.emptyList();

    }

    /**
     * Checks if at least one entry of a Collection of authorities is contained in a List of Strings.
     *
     * @param authorities A Collection containing GrantedAuthority objects.
     * @param roleList    The List of Strings the authorities are checked with.
     *
     * @return Returns true if at least one element of authorities is contained in roleList or vice versa.
     */
    private boolean containsAuthority(Collection<? extends GrantedAuthority> authorities, List<String> roleList) {
        Set<String> rolesLowerCase = roleList.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .anyMatch(authority -> rolesLowerCase.contains(authority.substring("ROLE_".length()).toLowerCase()));
    }

    /**
     * Checks if a collection of GrantedAuthorities contains the admin group defined in application.yml. This method
     * ensures backwards compatibility for the old configuration standard.
     *
     * @param authorities A collection of authorities the admin group should be contained in.
     *
     * @return True if the given admin group is contained in the authorities. Otherwise false.
     */
    @SuppressWarnings("deprecation")
    private boolean hasAdminGroup(Collection<? extends GrantedAuthority> authorities) {
        String ldapAdminGroup = settings.getSecurity().getLdap().getAdminGroup();
        return authorities.stream()
                .anyMatch(
                        authority -> authority.getAuthority()
                                .substring("ROLE_".length())
                                .equalsIgnoreCase(ldapAdminGroup)
                );
    }
}