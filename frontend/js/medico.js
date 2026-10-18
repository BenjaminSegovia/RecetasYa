/**
 * Pantalla del médico: crear recetas, listarlas (con filtro por estado),
 * editarlas y eliminarlas.
 *
 * El backend solo permite editar/eliminar recetas que aún no estén
 * reservadas ni dispensadas (ACEPTADA_PENDIENTE_RESERVA o SIN_STOCK);
 * para el resto ni siquiera mostramos los botones.
 */

const usuario = Sesion.exigir("MEDICO");
if (usuario) {
  montarHeader("medico.html");
  iniciar();
}

function iniciar() {
  /* --------------------- Medicamentos (filas dinámicas) -------------------- */

  function agregarFila(nombre = "", cantidad = 1) {
    const fila = document.createElement("tr");
    fila.className = "fila-medicamento";
    fila.innerHTML = `
      <td><input class="med-nombre" placeholder="Ibuprofeno 400mg" required /></td>
      <td><input class="med-cantidad" type="number" min="1" value="${cantidad}" /></td>
      <td><button type="button" class="btn btn-peligro btn-chico" data-quitar>✕</button></td>`;
    fila.querySelector(".med-nombre").value = nombre;
    fila.querySelector("[data-quitar]").addEventListener("click", () => {
      const filas = document.querySelectorAll("#tabla-medicamentos .fila-medicamento");
      if (filas.length === 1) {
        mostrarMensaje("mensaje", "La receta necesita al menos un medicamento.", "error");
        return;
      }
      fila.remove();
    });
    document.getElementById("tabla-medicamentos").appendChild(fila);
  }

  document.getElementById("btn-agregar-med").addEventListener("click", () => agregarFila());

  function leerMedicamentos() {
    return [...document.querySelectorAll("#tabla-medicamentos .fila-medicamento")]
      .map((fila) => ({
        nombreMedicamento: fila.querySelector(".med-nombre").value.trim(),
        cantidad: Number(fila.querySelector(".med-cantidad").value) || 0
      }))
      .filter((m) => m.nombreMedicamento.length > 0);
  }

  /* ------------------------- Formulario crear/editar ----------------------- */

  const form = document.getElementById("form-receta");
  const btnGuardar = document.getElementById("btn-guardar");
  const btnCancelar = document.getElementById("btn-cancelar");
  const tituloForm = document.getElementById("titulo-form");
  let editandoId = null;

  function modoEdicion(receta) {
    editandoId = receta.id;
    tituloForm.textContent = `Editar receta #${receta.id}`;
    btnGuardar.textContent = "Guardar cambios";
    btnCancelar.classList.remove("oculto");
    document.getElementById("paciente").value = receta.pacienteNombre;
    document.getElementById("sucursal").value = receta.sucursal;
    document.getElementById("tabla-medicamentos").innerHTML = "";
    (receta.medicamentos || []).forEach((m) => agregarFila(m.nombreMedicamento, m.cantidad));
    if ((receta.medicamentos || []).length === 0) agregarFila();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function modoCrear() {
    editandoId = null;
    tituloForm.textContent = "Nueva receta";
    btnGuardar.textContent = "Crear receta";
    btnCancelar.classList.add("oculto");
    form.reset();
    document.getElementById("tabla-medicamentos").innerHTML = "";
    agregarFila();
    limpiarMensaje("mensaje");
  }

  btnCancelar.addEventListener("click", modoCrear);

  form.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const cuerpo = {
      pacienteNombre: document.getElementById("paciente").value.trim(),
      sucursal: document.getElementById("sucursal").value.trim(),
      medicamentos: leerMedicamentos()
    };

    if (!cuerpo.pacienteNombre || !cuerpo.sucursal) {
      mostrarMensaje("mensaje", "Paciente y sucursal son obligatorios.", "error");
      return;
    }
    if (cuerpo.medicamentos.length === 0) {
      mostrarMensaje("mensaje", "Agrega al menos un medicamento.", "error");
      return;
    }

    btnGuardar.disabled = true;
    try {
      if (editandoId === null) {
        const creada = await api("/recetas", { method: "POST", body: cuerpo });
        mostrarMensaje(
          "mensaje",
          `Receta #${creada.id} creada para ${cuerpo.pacienteNombre}: ${creada.estado.replaceAll("_", " ").toLowerCase()}.`,
          "ok"
        );
        modoCrear();
      } else {
        await api(`/recetas/${editandoId}`, { method: "PUT", body: cuerpo });
        mostrarMensaje("mensaje", `Receta #${editandoId} actualizada.`, "ok");
        modoCrear();
      }
      await cargarRecetas();
    } catch (error) {
      mostrarMensaje("mensaje", error.message, "error");
    } finally {
      btnGuardar.disabled = false;
    }
  });

  /* ------------------------------ Lista de recetas -------------------------- */

  const tbody = document.getElementById("tabla-recetas");

  async function cargarRecetas() {
    const estado = document.getElementById("filtro-estado").value;
    const soloMias = document.getElementById("solo-mias").checked;

    tbody.innerHTML = `<tr><td colspan="7" class="vacio">Cargando…</td></tr>`;

    try {
      const ruta = estado === "TODOS" ? "/recetas" : `/recetas?estado=${estado}`;
      let recetas = await api(ruta);

      if (soloMias) {
        recetas = recetas.filter((r) => r.medicoUsername === usuario.username);
      }

      if (recetas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="vacio">No hay recetas con este filtro.</td></tr>`;
        return;
      }

      tbody.innerHTML = recetas
        .map((r) => {
          const modificable =
            r.estado === "ACEPTADA_PENDIENTE_RESERVA" || r.estado === "SIN_STOCK";
          return `
          <tr>
            <td>${r.id}</td>
            <td>${escapar(r.pacienteNombre)}<br><small>${escapar(r.medicoUsername)}</small></td>
            <td>${escapar(r.sucursal)}</td>
            <td>${medicamentosResumen(r.medicamentos)}</td>
            <td>${badgeEstado(r.estado)}</td>
            <td>${formatearFecha(r.fechaCreacion)}</td>
            <td class="acciones-tabla">
              ${
                modificable
                  ? `<button type="button" class="btn btn-secundario btn-chico" data-editar="${r.id}">Editar</button>
                     <button type="button" class="btn btn-peligro btn-chico" data-eliminar="${r.id}">Eliminar</button>`
                  : `<small>Solo lectura</small>`
              }
            </td>
          </tr>`;
        })
        .join("");

      tbody.querySelectorAll("[data-editar]").forEach((boton) =>
        boton.addEventListener("click", () => {
          const receta = recetas.find((r) => r.id === Number(boton.dataset.editar));
          if (receta) modoEdicion(receta);
        })
      );

      tbody.querySelectorAll("[data-eliminar]").forEach((boton) =>
        boton.addEventListener("click", async () => {
          const id = boton.dataset.eliminar;
          if (!confirm(`¿Eliminar la receta #${id}?`)) return;
          try {
            await api(`/recetas/${id}`, { method: "DELETE" });
            mostrarMensaje("mensaje", `Receta #${id} eliminada.`, "ok");
            await cargarRecetas();
          } catch (error) {
            mostrarMensaje("mensaje", error.message, "error");
          }
        })
      );
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="7" class="vacio">No se pudo cargar: ${escapar(error.message)}</td></tr>`;
    }
  }

  document.getElementById("btn-recargar").addEventListener("click", cargarRecetas);
  document.getElementById("filtro-estado").addEventListener("change", cargarRecetas);
  document.getElementById("solo-mias").addEventListener("change", cargarRecetas);

  cargarRecetas();
}
