package com.example.ciudadinteligente.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Mientras el microservicio corre en tu PC
    // Emulador → 10.0.2.2
    // Celular físico → la IP de tu PC en WiFi (ej: 192.168.1.X)
    private static final String BASE_URL = "http://192.168.101.3:8080/";

    private static Retrofit instance;

    public static ReporteApi getApi() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance.create(ReporteApi.class);
    }
}