/**
 * Pantalla del farmacéutico: ve las recetas con stock reservado y las
 * dispensa (POST /dispensaciones), y consulta todas las recetas con filtro.
 */

const usuario = Sesion.exigir("FARMACEUTICO");
if (usuario) {
  montarHeader("farmaceutico.html");
  iniciar();
}

function iniciar() {
  const tbodyReservadas = document.getElementById("tabla-reservadas");
  const tbodyTodas = document.getElementById("tabla-todas");

  /* ------------------------- Recetas reservadas --------------------------- */

  async function cargarReservadas() {
    tbodyReservadas.innerHTML = `<tr><td colspan="7" class="vacio">Cargando…</td></tr>`;

    try {
      const reservadas = await api("/recetas/reservadas");

      if (reservadas.length === 0) {
        tbodyReservadas.innerHTML = `<tr><td colspan="7" class="vacio">No hay recetas reservadas pendientes de dispensar.</td></tr>`;
        return;
      }

      tbodyReservadas.innerHTML = reservadas
        .map(
          (r) => `
          <tr>
            <td>${r.id}</td>
            <td>${escapar(r.pacienteNombre)}<br><small>${escapar(r.medicoUsername)}</small></td>
            <td>${escapar(r.sucursal)}</td>
            <td>${medicamentosResumen(r.medicamentos)}</td>
            <td>${badgeEstado(r.estado)}</td>
            <td>${formatearFecha(r.fechaCreacion)}</td>
            <td>
              <button type="button" class="btn btn-chico" data-dispensar="${r.id}">Dispensar</button>
            </td>
          </tr>`
        )
        .join("");

      tbodyReservadas.querySelectorAll("[data-dispensar]").forEach((boton) =>
        boton.addEventListener("click", async () => {
          const id = boton.dataset.dispensar;
          boton.disabled = true;
          boton.textContent = "Dispensando…";
          try {
            await api("/dispensaciones", { method: "POST", body: { recetaId: Number(id) } });
            mostrarMensaje("mensaje", `Receta #${id} dispensada: stock descontado y médico notificado.`, "ok");
            await Promise.all([cargarReservadas(), cargarTodas()]);
          } catch (error) {
            mostrarMensaje("mensaje", error.message, "error");
            boton.disabled = false;
            boton.textContent = "Dispensar";
          }
        })
      );
    } catch (error) {
      tbodyReservadas.innerHTML = `<tr><td colspan="7" class="vacio">No se pudo cargar: ${escapar(error.message)}</td></tr>`;
    }
  }

  /* ---------------------------- Todas las recetas -------------------------- */

  async function cargarTodas() {
    const estado = document.getElementById("filtro-estado").value;
    tbodyTodas.innerHTML = `<tr><td colspan="6" class="vacio">Cargando…</td></tr>`;

    try {
      const ruta = estado === "TODOS" ? "/recetas" : `/recetas?estado=${estado}`;
      const recetas = await api(ruta);

      if (recetas.length === 0) {
        tbodyTodas.innerHTML = `<tr><td colspan="6" class="vacio">No hay recetas con este filtro.</td></tr>`;
        return;
      }

      tbodyTodas.innerHTML = recetas
        .map(
          (r) => `
          <tr>
            <td>${r.id}</td>
            <td>${escapar(r.pacienteNombre)}<br><small>${escapar(r.medicoUsername)}</small></td>
            <td>${escapar(r.sucursal)}</td>
            <td>${medicamentosResumen(r.medicamentos)}</td>
            <td>${badgeEstado(r.estado)}</td>
            <td>${formatearFecha(r.fechaCreacion)}</td>
          </tr>`
        )
        .join("");
    } catch (error) {
      tbodyTodas.innerHTML = `<tr><td colspan="6" class="vacio">No se pudo cargar: ${escapar(error.message)}</td></tr>`;
    }
  }

  document.getElementById("btn-recargar").addEventListener("click", cargarReservadas);
  document.getElementById("btn-recargar-todas").addEventListener("click", cargarTodas);
  document.getElementById("filtro-estado").addEventListener("change", cargarTodas);

  cargarReservadas();
  cargarTodas();

  // Las recetas reservadas llegan cuando el proceso SQS termina la reserva:
  // refrescamos solas cada 15 segundos para no perderla.
  setInterval(cargarReservadas, 15000);
}
