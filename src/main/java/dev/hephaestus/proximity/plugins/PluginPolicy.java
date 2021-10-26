package dev.hephaestus.proximity.plugins;

import java.security.*;

public final class PluginPolicy extends Policy {
    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        Permissions permissions = new Permissions();

        if (!(domain.getClassLoader() instanceof PluginClassLoader)) {
            permissions.add(new AllPermission());
        }

        return permissions;
    }
}
