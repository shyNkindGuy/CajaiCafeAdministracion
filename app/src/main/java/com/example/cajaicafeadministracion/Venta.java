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
}
