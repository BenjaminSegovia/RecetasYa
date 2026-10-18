/**
 * Pantalla de acceso: login y registro.
 * Al autenticarse guarda el token + rol en localStorage y manda al usuario
 * a la pantalla de su rol.
 */

const $ = (id) => document.getElementById(id);

// Si ya hay sesión, ni siquiera mostramos el login.
if (Sesion.token && Sesion.usuario) {
  window.location.href = homeDe(Sesion.usuario.role);
}

/* ------------------------------- Pestañas -------------------------------- */

function activarPestana(cual) {
  const esLogin = cual === "login";
  $("tab-login").classList.toggle("activa", esLogin);
  $("tab-registro").classList.toggle("activa", !esLogin);
  $("form-login").classList.toggle("oculto", !esLogin);
  $("form-registro").classList.toggle("oculto", esLogin);
  limpiarMensaje("mensaje");
}

$("tab-login").addEventListener("click", () => activarPestana("login"));
$("tab-registro").addEventListener("click", () => activarPestana("registro"));

/* -------------------------------- Login ---------------------------------- */

$("form-login").addEventListener("submit", async (evento) => {
  evento.preventDefault();
  limpiarMensaje("mensaje");

  const username = $("login-username").value.trim();
  const password = $("login-password").value;

  if (!username || !password) {
    mostrarMensaje("mensaje", "Completa usuario y contraseña.", "error");
    return;
  }

  try {
    const respuesta = await api("/auth/login", {
      method: "POST",
      body: { username, password }
    });
    Sesion.guardar(respuesta.token, respuesta.username, respuesta.role);
    window.location.href = homeDe(respuesta.role);
  } catch (error) {
    mostrarMensaje("mensaje", error.message, "error");
  }
});

/* ------------------------------- Registro -------------------------------- */

$("form-registro").addEventListener("submit", async (evento) => {
  evento.preventDefault();
  limpiarMensaje("mensaje");

  const username = $("reg-username").value.trim();
  const nombreCompleto = $("reg-nombre").value.trim();
  const email = $("reg-email").value.trim();
  const password = $("reg-password").value;
  const role = $("reg-rol").value;

  if (!username || !nombreCompleto || !password) {
    mostrarMensaje("mensaje", "Usuario, nombre y contraseña son obligatorios.", "error");
    return;
  }
  if (role === "MEDICO" && !email) {
    mostrarMensaje(
      "mensaje",
      "Un médico necesita email: es por ahí donde llega la notificación de sus recetas.",
      "error"
    );
    return;
  }

  const cuerpo = { username, password, nombreCompleto, role };
  if (email) cuerpo.email = email;

  try {
    const respuesta = await api("/auth/register", { method: "POST", body: cuerpo });
    Sesion.guardar(respuesta.token, respuesta.username, respuesta.role);
    window.location.href = homeDe(respuesta.role);
  } catch (error) {
    mostrarMensaje("mensaje", error.message, "error");
  }
});
