package com.example.cajaicafeadministracion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ValidacionLoginTest {

    @Test
    public void datosCorrectos_noHayError() {
        assertNull(ValidacionLogin.validar("papa@cajaicafe.me", "secreto1"));
    }

    @Test
    public void correoConEspacios_seAceptaRecortado() {
        assertNull(ValidacionLogin.validar("  papa@cajaicafe.me ", "secreto1"));
    }

    @Test
    public void correoVacioONulo() {
        assertEquals("Ingresa tu correo", ValidacionLogin.validar("", "secreto1"));
        assertEquals("Ingresa tu correo", ValidacionLogin.validar("   ", "secreto1"));
        assertEquals("Ingresa tu correo", ValidacionLogin.validar(null, "secreto1"));
    }

    @Test
    public void correoMalFormado() {
        assertEquals("Correo no válido", ValidacionLogin.validar("papa", "secreto1"));
        assertEquals("Correo no válido", ValidacionLogin.validar("@cajaicafe.me", "secreto1"));
        assertEquals("Correo no válido", ValidacionLogin.validar("papa@", "secreto1"));
        assertEquals("Correo no válido", ValidacionLogin.validar("papa@cajaicafe", "secreto1"));
    }

    @Test
    public void contrasenaVaciaOCorta() {
        assertEquals("Ingresa tu contraseña", ValidacionLogin.validar("papa@cajaicafe.me", ""));
        assertEquals("Ingresa tu contraseña", ValidacionLogin.validar("papa@cajaicafe.me", null));
        assertEquals("La contraseña tiene al menos 6 caracteres",
                ValidacionLogin.validar("papa@cajaicafe.me", "12345"));
    }
}
