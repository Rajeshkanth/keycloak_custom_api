# keycloak_custom_api

It contain custom endpoints for creating new users `/addUser`, `/getUsers`, it extends `RealmResourceProvider` for custom endpoint creation.

Also, these endpoints are protected with `UMA` for protecting the resources and restricting user's access over resource. 

By validating the token scopes and providing the `RPT` for restricted access.
