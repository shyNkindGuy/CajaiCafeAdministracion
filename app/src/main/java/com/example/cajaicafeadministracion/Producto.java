package com.example.cajaicafeadministracion;

public class Producto {
    public String id;
    public String nombre;
    public double precio;
    public long stock;

    public Producto(){}

    public Producto(String id, String nombre, double precio, long stock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }
}
