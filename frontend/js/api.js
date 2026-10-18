/**
 * Utilidades compartidas por todas las páginas del frontend:
 * configuración, sesión (token + rol), el fetch con Authorization y los
 * helpers de UI (mensajes, badges, header).
 *
 * El frontend corre en Live Server (otro origen que la API), por eso la API
 * está en URL absoluta y el backend tiene CORS habilitado para ese origen.
 */

const API_BASE = "http://localhost:8080/api";

const CLAVE_TOKEN = "recetaya_token";
const CLAVE_USUARIO = "recetaya_usuario";

/** Página principal de cada rol. */
function homeDe(role) {
  return role === "MEDICO" ? "medico.html" : "farmaceutico.html";
}

const Sesion = {
  guardar(token, username, role) {
    localStorage.setItem(CLAVE_TOKEN, token);
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify({ username, role }));
  },

  get token() {
    return localStorage.getItem(CLAVE_TOKEN);
  },

  get usuario() {
    try {
      return JSON.parse(localStorage.getItem(CLAVE_USUARIO));
    } catch {
      return null;
    }
  },

  cerrar() {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_USUARIO);
  },

  /**
   * Devuelve el usuario o redirige al login si no hay sesión.
   * Si se indicó un rol y no coincide, manda al home de su propio rol
   * (un farmacéutico no debería ver la pantalla del médico, y viceversa).
   */
  exigir(rol) {
    const usuario = this.usuario;
    if (!this.token || !usuario) {
      window.location.href = "index.html";
      return null;
    }
    if (rol && usuario.role !== rol) {
      window.location.href = homeDe(usuario.role);
      return null;
    }
    return usuario;
  }
};

/**
 * Llamada a la API.
 *  - Agrega el JWT en Authorization.
 *  - Convierte los errores del backend (ErrorResponse: {message, fieldErrors})
 *    en excepciones con mensaje legible para mostrar en pantalla.
 *  - Si responde 401, cierra la sesión y vuelve al login.
 */
async function api(ruta, { method = "GET", body } = {}) {
  const cabeceras = { Accept: "application/json" };
  if (Sesion.token) cabeceras.Authorization = `Bearer ${Sesion.token}`;
  if (body !== undefined) cabeceras["Content-Type"] = "application/json";

  let respuesta;
  try {
    respuesta = await fetch(API_BASE + ruta, {
      method,
      headers: cabeceras,
      body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch {
    throw new Error("No se pudo contactar con la API. ¿Está `docker compose up` levantado?");
  }

  if (respuesta.status === 401) {
    Sesion.cerrar();
    window.location.href = "index.html";
    throw new Error("Sesión expirada o inválida: vuelve a iniciar sesión.");
  }

  if (respuesta.status === 204) return null;

  const datos = await respuesta.json().catch(() => null);

  if (!respuesta.ok) {
    const partes = [datos && datos.message ? datos.message : `Error ${respuesta.status}`];
    if (datos && Array.isArray(datos.fieldErrors)) {
      partes.push(datos.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(", "));
    }
    const error = new Error(partes.join(" — "));
    error.status = respuesta.status;
    throw error;
  }

  return datos;
}

/* ------------------------------ Helpers de UI ------------------------------ */

function elemento(destino) {
  return typeof destino === "string" ? document.getElementById(destino) : destino;
}

/** Muestra un mensaje (éxito en verde, error en rojo). */
function mostrarMensaje(destino, texto, tipo = "ok") {
  const caja = elemento(destino);
  if (!caja) return;
  caja.textContent = texto;
  caja.className = `alerta alerta-${tipo}`;
  caja.hidden = false;
  if (tipo === "ok") {
    setTimeout(() => {
      caja.hidden = true;
    }, 5000);
  }
}

function limpiarMensaje(destino) {
  const caja = elemento(destino);
  if (!caja) return;
  caja.hidden = true;
  caja.textContent = "";
}

/** Escapa texto antes de pintarlo en el HTML (los datos vienen de la API). */
function escapar(texto) {
  return String(texto == null ? "" : texto)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

/** Badge con color según el estado de la receta. */
function badgeEstado(estado) {
  const conf = {
    ACEPTADA_PENDIENTE_RESERVA: ["badge-pendiente", "Pendiente de reserva"],
    RESERVADA: ["badge-reservada", "Reservada"],
    SIN_STOCK: ["badge-sinstock", "Sin stock"],
    DISPENSADA: ["badge-dispensada", "Dispensada"]
  };
  const [clase, texto] = conf[estado] || ["badge-otro", estado];
  return `<span class="badge ${clase}">${escapar(texto)}</span>`;
}

function formatearFecha(iso) {
  if (!iso) return "—";
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return escapar(iso);
  return fecha.toLocaleString("es-CL", { dateStyle: "short", timeStyle: "short" });
}

/** "Ibuprofeno 400mg ×2" (en HTML, escapado). */
function medicamentosResumen(medicamentos) {
  const lista = medicamentos || [];
  if (lista.length === 0) return "—";
  return lista
    .map((m) => `${escapar(m.nombreMedicamento)} ×${m.cantidad}`)
    .join("<br>");
}

/** Pinta el header con los enlaces que corresponden al rol. */
function montarHeader(paginaActual) {
  const cont = document.getElementById("topbar");
  const usuario = Sesion.usuario;
  if (!cont || !usuario) return;

  const enlaces = [];
  if (usuario.role === "MEDICO") {
    enlaces.push({ href: "medico.html", texto: "Mis recetas" });
  }
  if (usuario.role === "FARMACEUTICO") {
    enlaces.push({ href: "farmaceutico.html", texto: "Dispensación" });
  }
  enlaces.push({ href: "inventario.html", texto: "Inventario" });

  cont.innerHTML = `
    <div class="topbar-inner">
      <a class="marca" href="${homeDe(usuario.role)}">Receta<span>YA</span></a>
      <nav>
        ${enlaces
          .map(
            (e) =>
              `<a href="${e.href}" class="${e.href === paginaActual ? "activo" : ""}">${e.texto}</a>`
          )
          .join("")}
      </nav>
      <div class="sesion">
        <span class="usuario">${escapar(usuario.username)} <small>${escapar(usuario.role)}</small></span>
        <button type="button" class="btn btn-secundario" data-salir>Salir</button>
      </div>
    </div>`;

  cont.querySelector("[data-salir]").addEventListener("click", () => {
    Sesion.cerrar();
    window.location.href = "index.html";
  });
}
