package cl.duoc.app.recetas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Banner {

    private Long id;

    private String empresaNombre;

    private String mensaje;

    private String urlImagen;

    private String linkWebsite;

    private Date fechaCreacion = new Date();
}
