package com.example.cajaicafeadministracion;

/**
 * Validación de los campos del login. No depende de Android para poder probarla
 * con tests unitarios en el CI.
 */
public final class ValidacionLogin {

    private ValidacionLogin() {}

    /** Devuelve el mensaje de error a mostrar, o null si los datos se pueden enviar. */
    public static String validar(String correo, String contrasena) {
        String c = correo == null ? "" : correo.trim();
        if (c.isEmpty()) return "Ingresa tu correo";

        int arroba = c.indexOf('@');
        if (arroba <= 0 || arroba == c.length() - 1 || c.indexOf('.', arroba) < 0) {
            return "Correo no válido";
        }

        if (contrasena == null || contrasena.isEmpty()) return "Ingresa tu contraseña";
        if (contrasena.length() < 6) return "La contraseña tiene al menos 6 caracteres";
        return null;
    }
}
