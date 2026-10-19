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

    // Costo de empaque por unidad (opcional, se configura en Firebase) para el costo por bolsa del
    // lote (CosteoLote). Solo lo usan las bolsas que salen de un lote: 1/2 kg y 1/4 kg.
    public double costoBolsa;
    public double costoSticker;

    // Aviso de stock bajo por lote (AlertaStock): avisar cuando queden stockMinimo bolsas o menos.
    // Integer y no int para distinguir "sin configurar" (null → 5) de "apagado" (0).
    public Integer stockMinimo;

    public Producto(){}

    public Producto(String id, String nombre, double precio, long stock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }
}
