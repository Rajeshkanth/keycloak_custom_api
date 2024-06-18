package org.example.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.api.utils.UserRepresentation;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.common.VerificationException;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.services.resource.RealmResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.api.utils.Constants.*;

public class CustomUserCreateApi implements RealmResourceProvider {

    private final RealmModel realm;
    private final UserProvider userProvider;
    private final AuthzClient authzClient = AuthzClient.create();
    private static final Logger logger = LoggerFactory.getLogger(CustomUserCreateApi.class);


    public CustomUserCreateApi(KeycloakSession session) {
        this.realm = session.getContext().getRealm();
        this.userProvider = session.users();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @POST
    @Path("addUser")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(UserRepresentation userRep, @Context HttpHeaders httpHeaders) {
        String username = userRep.getUsername();
        String password = userRep.getPassword();
        String email = userRep.getEmail();
        String token = extractTokenFromHeaders(httpHeaders);

        if (!authorizeRequest("add-user", "addUser", token)) {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(UNAUTHORIZED)
                    .build();
        }

        if (username == null || password == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(USERNAME_PSWD_REQUIRED)
                    .build();
        }

        if (isUserExists(username, email)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(USER_ALREADY)
                    .build();
        }

        UserModel user = userProvider.addUser(realm, username);
        user.setEnabled(userRep.isEnabled());
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
        user.setEmail(email);
        user.setUsername(username);
        user.credentialManager().updateCredential(UserCredentialModel.password(password));

        return Response.status(Response.Status.CREATED).entity(userRep).build();
    }

    private String extractTokenFromHeaders(HttpHeaders httpHeaders) {
        List<String> authHeaders = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.getFirst();
            if (authHeader.startsWith("Bearer")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }

    private boolean authorizeRequest(String scope, String resourceId, String token) {
        try {
            if (token == null) {
                logger.error("Token is missing");
                return false;
            }

            AccessToken accessToken = TokenVerifier.create(token, AccessToken.class).getToken();

            if (!accessToken.getScope().contains(scope)) {
                logger.error("User don't have the access to create user!");
                return false;
            }

            PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.setResourceId(resourceId);
            Set<String> scopes = new HashSet<>();
            scopes.add(scope);
            permissionRequest.setScopes(scopes);

            PermissionResponse response = authzClient.protection().permission().create(permissionRequest);
            logger.info("Request Partying Token generated!");
            return response.getTicket() != null && !response.getTicket().isEmpty();
        } catch (VerificationException e) {
            logger.error("Token verified failed!", e);
            return false;
        } catch (Exception e) {
            logger.error("Cannot authorize the request", e);
            return false;
        }
    }

    @GET
    @Path("getUsers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers(@QueryParam("userName") String userName, @Context HttpHeaders httpHeaders) {
        Map<String, String> params = new HashMap<>();
        String token = extractTokenFromHeaders(httpHeaders);

        if (!authorizeRequest("get-users", "getUsers", token)) {
            return Response.status(Response.Status.FORBIDDEN).entity(UNAUTHORIZED).build();
        }

        if (userName != null) {
            params.put(UserModel.USERNAME, userName);
        }

        List<UserRepresentation> users = userProvider.searchForUserStream(realm, params)
                .map(this::toRepresentation)
                .collect(Collectors.toList());

        if (users.isEmpty()) {
            logger.info("Users List is empty, {}", users);
            return Response.noContent().entity(USER_NOT_FOUND).build();
        }

        return Response.ok(users).build();
    }

    private boolean isUserExists(String username, String email) {
        return userProvider.getUserByUsername(realm, username) != null
                || (email != null && userProvider.getUserByEmail(realm, email) != null);
    }

    private UserRepresentation toRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(user.getUsername());
        rep.setEnabled(user.isEnabled());
        rep.setEmail(user.getEmail());
        rep.setFirstName(user.getFirstName());
        rep.setLastName(user.getLastName());
        return rep;
    }

    @Override
    public void close() {
        // No cleanup required
    }
}
