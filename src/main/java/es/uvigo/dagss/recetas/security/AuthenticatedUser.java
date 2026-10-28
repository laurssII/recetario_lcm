package es.uvigo.dagss.recetas.security;

import es.uvigo.dagss.recetas.entities.UserType;

public record AuthenticatedUser(Long id, UserType role) {

    public boolean is(UserType expectedRole) {
        return role == expectedRole;
    }

    public boolean isSelf(UserType expectedRole, Long userId) {
        return role == expectedRole && id != null && id.equals(userId);
    }
}
