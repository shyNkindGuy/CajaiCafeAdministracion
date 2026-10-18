package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CuentasPorCobrarTest {

    private static final double DELTA = 0.0001;

    private static Venta venta(String id, String fecha, String cliente, int cantidad, double total,
                               String estado, double aCuenta) {
        Venta v = new Venta(id, fecha, "Bolsa 1/2 kg", cliente, 25, cantidad, estado, aCuenta, total);
        v.productoId = "medio_kilo";
        return v;
    }

    @Test
    public void saldo_segunEstado() {
        assertEquals(0, CuentasPorCobrar.saldo(venta("v1", "2026-10-03 10:00:00", "Ana", 2, 45, "pagado", 0)), DELTA);
        assertEquals(20, CuentasPorCobrar.saldo(venta("v2", "2026-10-03 10:00:00", "Ana", 2, 45, "parcial", 25)), DELTA);
        assertEquals(45, CuentasPorCobrar.saldo(venta("v3", "2026-10-03 10:00:00", "Ana", 2, 45, "pendiente", 0)), DELTA);
        // Sin estado (ventas muy antiguas): como en Inicio, cuenta como no cobrada.
        assertEquals(45, CuentasPorCobrar.saldo(venta("v4", "2026-10-03 10:00:00", "Ana", 2, 45, null, 0)), DELTA);
        // A cuenta mayor al total (dato mal escrito): no hay saldo negativo.
        assertEquals(0, CuentasPorCobrar.saldo(venta("v5", "2026-10-03 10:00:00", "Ana", 2, 45, "parcial", 60)), DELTA);
        // Céntimos: 15 − 12.30 = 2.70 sin errores de redondeo.
        assertEquals(2.70, CuentasPorCobrar.saldo(venta("v6", "2026-10-03 10:00:00", "Ana", 1, 15, "parcial", 12.30)), DELTA);
    }

    @Test
    public void pendientes_soloConSaldoYDeLaMasAntigua() {
        List<Venta> ventas = Arrays.asList(
                venta("v1", "2026-10-10 10:00:00", "Ana", 1, 25, "pendiente", 0),
                venta("v2", "2026-09-01 10:00:00", "Beto", 2, 45, "pagado", 0),
                venta("v3", "2026-09-15 10:00:00", "Carla", 2, 45, "parcial", 20),
                venta("v4", "2026-10-10 10:00:00", "Dani", 1, 15, "pendiente", 0),
                null);
        List<Venta> p = CuentasPorCobrar.pendientes(ventas);
        assertEquals(3, p.size());
        assertEquals("v3", p.get(0).id);
        assertEquals("v1", p.get(1).id); // misma fecha que v4: desempata el id
        assertEquals("v4", p.get(2).id);
        assertEquals(65.00, CuentasPorCobrar.total(p), DELTA); // 25 + 25 + 15
        assertEquals(0, CuentasPorCobrar.total(Collections.<Venta>emptyList()), DELTA);
    }

    @Test
    public void nombreCliente_sinNombreODefecto() {
        assertEquals("Juan", CuentasPorCobrar.nombreCliente(venta("v", "", "  Juan ", 1, 25, "pendiente", 0)));
        assertNull(CuentasPorCobrar.nombreCliente(venta("v", "", "Cliente", 1, 25, "pendiente", 0)));
        assertNull(CuentasPorCobrar.nombreCliente(venta("v", "", "cliente", 1, 25, "pendiente", 0)));
        assertNull(CuentasPorCobrar.nombreCliente(venta("v", "", "", 1, 25, "pendiente", 0)));
        assertNull(CuentasPorCobrar.nombreCliente(venta("v", "", null, 1, 25, "pendiente", 0)));
    }

    @Test
    public void fechaCorta() {
        assertEquals("03/10/2026", CuentasPorCobrar.fechaCorta("2026-10-03 10:00:00"));
        assertEquals("03/10/2026", CuentasPorCobrar.fechaCorta("2026-10-03"));
        assertNull(CuentasPorCobrar.fechaCorta("3/10/2026"));
        assertNull(CuentasPorCobrar.fechaCorta(null));
    }

    @Test
    public void detalle() {
        assertEquals("2 × Bolsa 1/2 kg · 03/10/2026 · total S/ 45.00, pagó S/ 25.00",
                CuentasPorCobrar.detalle(venta("v", "2026-10-03 10:00:00", "Ana", 2, 45, "parcial", 25)));
        assertEquals("1 × Bolsa 1/2 kg · total S/ 25.00",
                CuentasPorCobrar.detalle(venta("v", null, "Ana", 1, 25, "pendiente", 0)));
    }

    @Test
    public void mensaje_parcialConNombre() {
        assertEquals("Hola Juan, te saludamos de Cajai Café. Te recordamos que tienes un saldo pendiente de "
                        + "S/ 20.00 por 2 × Bolsa 1/2 kg (compra del 03/10/2026; ya abonaste S/ 25.00). ¡Muchas gracias!",
                CuentasPorCobrar.mensaje(venta("v", "2026-10-03 10:00:00", "Juan", 2, 45, "parcial", 25)));
    }

    @Test
    public void mensaje_pendienteSinNombreNiFecha() {
        assertEquals("Hola, te saludamos de Cajai Café. Te recordamos que tienes un saldo pendiente de "
                        + "S/ 25.00 por 1 × Bolsa 1/2 kg. ¡Muchas gracias!",
                CuentasPorCobrar.mensaje(venta("v", null, "Cliente", 1, 25, "pendiente", 0)));
        assertEquals("Hola, te saludamos de Cajai Café. Te recordamos que tienes un saldo pendiente de "
                        + "S/ 25.00 por 1 × Bolsa 1/2 kg (compra del 01/10/2026). ¡Muchas gracias!",
                CuentasPorCobrar.mensaje(venta("v", "2026-10-01 08:00:00", "", 1, 25, "pendiente", 0)));
    }
}
