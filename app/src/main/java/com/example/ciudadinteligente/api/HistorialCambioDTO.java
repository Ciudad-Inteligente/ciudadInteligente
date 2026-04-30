package com.example.ciudadinteligente.api;

public class HistorialCambioDTO {
    public Long id;
    public String tipo;                    // "estado", "comentario_interno", "mensaje_ciudadano"
    public String descripcion;             // "Cambió estado a: en_proceso"
    public String comentario;              // Comentario adicional
    public Boolean visibleCiudadano;       // Si es visible para el ciudadano
    public String fechaCambio;             // "2024-04-29T21:00:00"
    public String uidUsuario;              // UID del funcionario que hizo el cambio
}
