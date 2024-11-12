package cl.duoc.app.recetas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Receta {

    private Long id;

    private String nombre;

    private String tipoCocina;

    private String ingredientes;

    private String paisOrigen;

    private String dificultad;

    private int popularidad;

    private String instruccionesPreparacion;

    private int tiempoCoccion;

    private int porciones;

    private String fotografiaUrl;

    private LocalDateTime fechaCreacion;

    private Long idUsuario;
}
