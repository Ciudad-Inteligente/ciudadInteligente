package com.example.ciudadinteligente.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;
import com.example.ciudadinteligente.api.ReporteDTO;

public interface ReporteApi {

    @POST("reportes")
    Call<ReporteDTO> crearReporte(@Body ReporteDTO reporte);

    @GET("reportes/ciudadano/{uid}")
    Call<List<ReporteDTO>> misReportes(@Path("uid") String uid);

    @GET("reportes/{id}")
    Call<ReporteDTO> detalleReporte(@Path("id") long id);

    @PUT("reportes/{id}")
    Call<ReporteDTO> editarReporte(@Path("id") long id, @Body ReporteDTO reporte);

    @DELETE("reportes/{id}")
    Call<Void> eliminarReporte(@Path("id") long id);

    @GET("areas")
    Call<List<ReporteDTO.AreaDTO>> listarAreas();

    @GET("tipos-reporte/area/{idArea}")
    Call<List<ReporteDTO.TipoReporteDTO>> listarTiposPorArea(@Path("idArea") Long idArea);
}