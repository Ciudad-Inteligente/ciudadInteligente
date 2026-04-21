package com.example.ciudadinteligente.api;

public class ReporteDTO {
    public Long id;
    public String uidCiudadano;
    public String asunto;
    public String descripcion;
    public String estado;
    public Double latitud;
    public Double longitud;
    public String direccion;
    public String zona;
    public String prioridad;
    public String uidFuncionario;
    public Boolean activo;
    public String fechaReporte;
    public TipoReporteDTO tipoReporte;

    public static class TipoReporteDTO {
        public Long id;
        public String nombre;
        public String icono;
        public AreaDTO area;
    }

    public static class AreaDTO {
        public Long id;
        public String nombre;
    }
}