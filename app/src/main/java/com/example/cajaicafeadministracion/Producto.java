package com.example.cajaicafeadministracion;

public class Producto {
    public String id;
    public String nombre;
    public double precio;
    public long stock;

    // Oferta por cantidad (opcional, se configura en Firebase): cada grupo de ofertaCantidad
    // unidades cuesta ofertaPrecio. Ej. bolsa 1/2 kg: 2 x S/45 (el kilo). 0 = sin oferta.
    public int ofertaCantidad;
    public double ofertaPrecio;

    public Producto(){}

    public Producto(String id, String nombre, double precio, long stock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }
}
