package cl.duoc.app.recetas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comentario {

    private Long id;

    private Long idReceta;

    private Long idUsuario;

    private String contenido;

    private LocalDateTime fechaCreacion;

    private boolean visible;
}
