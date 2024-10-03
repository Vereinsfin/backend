package app.hopps.org.delegates;

import app.hopps.org.jpa.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

@ApplicationScoped
public class CreateUserInKeycloak {

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "app.hopps.vereine.auth.realm-name")
    String realmName;

    @ConfigProperty(name = "app.hopps.vereine.auth.default-role")
    String ownerRoleName;

    /**
     * If necessary, creates the owner role, otherwise just returns it.
     */
    private static RoleRepresentation createOwnerRole(RealmResource realmResource, String ownerRoleName) {
        RoleRepresentation ownerRole;
        try {
            ownerRole = realmResource.roles().get(ownerRoleName).toRepresentation();
        } catch (Exception e) {
            ownerRole = new RoleRepresentation();
            ownerRole.setName(ownerRoleName);
            try {
                realmResource.roles().create(ownerRole);
            } catch (Exception ignored) { }

            ownerRole = realmResource.roles().get(ownerRoleName).toRepresentation();
        }
        return ownerRole;
    }

    public void createUserInKeycloak(Member user) {

        RealmResource realmResource = keycloak.realm(realmName);
        UsersResource usersResource = realmResource.users();
        RoleRepresentation ownerRole = createOwnerRole(realmResource, ownerRoleName);
        
        UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEnabled(true);
        userRepresentation.setFirstName(user.getFirstName());
        userRepresentation.setLastName(user.getLastName());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setUsername(user.getEmail());

        Response response = usersResource.create(userRepresentation);

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String body = response.readEntity(String.class);
            throw new WebApplicationException("Could not create user, body: " + body, response);
        }

        response.close();

        // Assign user to the owner role
        UserRepresentation createdUser = usersResource.search(userRepresentation.getUsername())
                .getFirst();

        usersResource.get(createdUser.getId())
                .roles()
                .realmLevel()
                .add(List.of(ownerRole));
    }
}
