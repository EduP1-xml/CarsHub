package carshub.com.br.backend.models.dtos;


import carshub.com.br.backend.models.entities.Usuario;

public record UserPerfilDTO(String nome , String telefone , String login, String urlFotoPerfil) {

    public UserPerfilDTO(Usuario user) {
        this( user.getNome(), user.getTelefone(), user.getLogin(), user.getUrlFotoPerfil());
    }
}
