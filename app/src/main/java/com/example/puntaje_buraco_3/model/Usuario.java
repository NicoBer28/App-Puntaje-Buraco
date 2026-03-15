package com.example.puntaje_buraco_3.model;

import java.util.List;

public class Usuario {
    private String nombreUsuario;
    private String nombreUsuarioMinus;

    private int partidasJugadas;
    private int partidasGanadas;
    private List<String> listaAmigos;


    public Usuario() {
    }

    public Usuario(String nombreUsuario, int partidasJugadas, int partidasGanadas, List<String> listaAmigos) {
        this.nombreUsuario = nombreUsuario;
        this.nombreUsuarioMinus = nombreUsuario.toLowerCase();
        this.partidasJugadas = partidasJugadas;
        this.partidasGanadas = partidasGanadas;
        this.listaAmigos = listaAmigos;
    }

    public int getPartidasJugadas() {
        return partidasJugadas;
    }

    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    public List<String> getListaAmigos() {
        return listaAmigos;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreUsuarioMinus() {
        return nombreUsuarioMinus;
    }


}