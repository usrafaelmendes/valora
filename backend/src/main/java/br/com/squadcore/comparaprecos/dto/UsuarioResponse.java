package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;

/** Dados públicos do usuário: o hash da senha nunca sai da API. */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.isAtivo());
    }
}
