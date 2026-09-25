package carshub.com.br.backend.models.dtos;


import carshub.com.br.backend.models.entities.Usuario;

public record PerfilUsuarioDTO(String nome , String telefone , String login, String urlFotoPerfil) {

    public PerfilUsuarioDTO(Usuario user) {
        this( user.getNome(), user.getTelefone(), user.getLogin(), user.getUrlFotoPerfil());
    }
}
