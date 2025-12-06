package com.example.cajaicafeadministracion;

public class Venta {
    public String id;
    public String fecha;

    public double precioUnitario;
    public int cantidad;
    public String estadoPago;
    public double montoParcial;
    public double total;
    public String cliente;
    public String producto;

    // NUEVO: a qué lote pertenece esta venta. Todavía no se llena desde VentaFragment
    // (eso es el siguiente paso) — hasta entonces queda null y el resumen del lote mostrará S/0.
    public String loteId;

    public Venta() {}
    public Venta(String id, String tipo, int i, double v, String pagado, String fecha){}

    public Venta(String id, String fecha, String producto, String cliente, double precioUnitario, int cantidad, String estadoPago, double montoParcial, double total) {
        this.id = id;
        this.fecha = fecha;
        this.producto = producto;
        this.cliente = cliente;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.estadoPago = estadoPago;
        this.montoParcial = montoParcial;
        this.total = total;
    }

    public Venta(String id, String fecha, String producto, String cliente, double precioUnitario, int cantidad, String estadoPago, double montoParcial, double total, String loteId) {
        this(id, fecha, producto, cliente, precioUnitario, cantidad, estadoPago, montoParcial, total);
        this.loteId = loteId;
    }
}
