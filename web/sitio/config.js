/**
 * Configuración de la tienda: lo único que se edita a mano para cambiar la web.
 *
 * Los PRECIOS no van aquí: se leen de Firebase (productos/{id}/precio, ofertaCantidad y ofertaPrecio),
 * los mismos que usa la app. Si cambias un precio en Firebase, la web lo muestra al recargar.
 */
export const CONFIG = {
  // WhatsApp oficial de Cajai Café, con el código de Perú (51) delante.
  whatsapp: "51991538843",

  // Realtime Database del proyecto cajaicafecontrol (la misma de la app). La URL exacta está en la
  // consola de Firebase → Realtime Database; si fuera otra, cámbiala aquí (_headers ya permite
  // *.firebaseio.com y *.firebasedatabase.app).
  baseDatos: "https://cajaicafecontrol-default-rtdb.firebaseio.com",

  // Bolsas que se muestran, en este orden. `id` es el de productos/{id} en Firebase; nombre y peso
  // salen también en el mensaje de WhatsApp.
  productos: [
    {
      id: "medio_kilo",
      nombre: "Bolsa 1/2 kg",
      peso: "500 g",
      descripcion: "Para la casa que toma café todos los días.",
    },
    {
      id: "cuarto_kilo",
      nombre: "Bolsa 1/4 kg",
      peso: "250 g",
      descripcion: "Para probarlo por primera vez o para regalar.",
    },
  ],
};
