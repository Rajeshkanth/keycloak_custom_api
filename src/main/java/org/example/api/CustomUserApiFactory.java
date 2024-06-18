package org.example.api;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class CustomUserApiFactory implements RealmResourceProviderFactory {

    private static final String PROVIDER_ID = "custom";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new CustomUserCreateApi(session);
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization required
    }

    @Override
    public void close() {
        // No cleanup required
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
