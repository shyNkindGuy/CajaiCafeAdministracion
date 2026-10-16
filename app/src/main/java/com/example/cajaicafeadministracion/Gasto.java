package com.example.cajaicafeadministracion;

/**
 * Gasto que no es parte del costeo de un costal (empaque comprado por mayor, transporte,
 * publicidad, etc.). Se guarda en {@code gastos/{id}}; ver ValidacionGasto para las categorías.
 */
public class Gasto {
    public String id;
    public String fecha;      // yyyy-MM-dd HH:mm:ss, igual que las ventas
    public String categoria;  // empaque | transporte | publicidad | servicios | mantenimiento | otros
    public double monto;
    public String nota;       // opcional
    public String loteId;     // opcional: null = gasto general del negocio
    public String uid;        // cuenta que lo registró

    public Gasto() {}

    public Gasto(String id, String fecha, String categoria, double monto, String nota, String loteId, String uid) {
        this.id = id;
        this.fecha = fecha;
        this.categoria = categoria;
        this.monto = monto;
        this.nota = nota;
        this.loteId = loteId;
        this.uid = uid;
    }
}
