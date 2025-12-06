package com.example.cajaicafeadministracion;

/**
 * Representa un costal/lote de café con sus costos y su propio stock de bolsas.
 * Nota: el costo de empaque (bolsas+stickers) por ahora es un campo manual (empaqueTotal),
 * porque en la práctica no se compra pensando en un solo lote (se compra por mayor y se
 * reutiliza entre costales). Cuando construyamos el inventario compartido de empaque,
 * este campo se calculará solo en vez de escribirse a mano.
 */
public class Lote {
    public String id;
    public String codigo;       // ej. "LOTE1-NOV2025"
    public String fechaInicio;  // yyyy-MM-dd
    public String estado;       // "abierto" | "cerrado"

    // --- A. Café ---
    public double pergaminoKg;
    public double pergaminoPrecioKg;
    public double pergaminoTotal;

    public double piladoKg;
    public double piladoPrecioKg;
    public double piladoTotal;

    public double tostadoKg;
    public double tostadoPrecioKg;
    public double tostadoTotal;

    public double flete;

    // --- B. Electricidad (selladora + moledora) ---
    public double potSelladoraKw;
    public double horasSelladora;
    public double potMoledoraKw;
    public double horasMoledora;
    public double precioKwh;
    public double electricidadTotal;

    // --- C. Empaque (temporal, manual — ver nota arriba) ---
    public double empaqueTotal;

    // --- Resumen ---
    public double totalGastos;

    // --- Stock propio del lote ---
    public int stockBolsas12;
    public int stockBolsas14;

    public Lote() {}

    public Lote(String id, String codigo, String fechaInicio) {
        this.id = id;
        this.codigo = codigo;
        this.fechaInicio = fechaInicio;
        this.estado = "abierto";
    }
}
