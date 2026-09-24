package br.com.squadcore.comparaprecos.dto;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioResponse usuario) {

    public static LoginResponse bearer(String token, Instant expiraEm, UsuarioResponse usuario) {
        return new LoginResponse(token, "Bearer", expiraEm, usuario);
    }
}
