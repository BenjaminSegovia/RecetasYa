/**
 * Pantalla de inventario: carga de stock (POST hace upsert por
 * medicamento + sucursal), edición y eliminación.
 * Cualquier usuario autenticado puede verla, pero el endpoint de
 * dispensación descuenta el stock solo con el rol FARMACEUTICO.
 */

const usuario = Sesion.exigir();
if (usuario) {
  montarHeader("inventario.html");
  iniciar();
}

function iniciar() {
  const form = document.getElementById("form-stock");
  const btnGuardar = document.getElementById("btn-guardar");
  const btnCancelar = document.getElementById("btn-cancelar");
  const tituloForm = document.getElementById("titulo-form");
  const tbody = document.getElementById("tabla-stock");
  let editandoId = null;

  /* ------------------------------ Formulario ------------------------------ */

  function modoEdicion(stock) {
    editandoId = stock.id;
    tituloForm.textContent = `Editar stock #${stock.id}`;
    btnGuardar.textContent = "Guardar cambios";
    btnCancelar.classList.remove("oculto");
    document.getElementById("medicamento").value = stock.nombreMedicamento;
    document.getElementById("sucursal").value = stock.sucursal;
    document.getElementById("cantidad").value = stock.cantidadDisponible;
    // El medicamento y la sucursal son la clave: no se pueden cambiar al editar.
    document.getElementById("medicamento").disabled = true;
    document.getElementById("sucursal").disabled = true;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function modoCrear() {
    editandoId = null;
    tituloForm.textContent = "Cargar stock";
    btnGuardar.textContent = "Guardar stock";
    btnCancelar.classList.add("oculto");
    form.reset();
    document.getElementById("medicamento").disabled = false;
    document.getElementById("sucursal").disabled = false;
    document.getElementById("cantidad").value = 10;
    limpiarMensaje("mensaje");
  }

  btnCancelar.addEventListener("click", modoCrear);

  form.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const cuerpo = {
      nombreMedicamento: document.getElementById("medicamento").value.trim(),
      sucursal: document.getElementById("sucursal").value.trim(),
      cantidadDisponible: Number(document.getElementById("cantidad").value)
    };

    if (!cuerpo.nombreMedicamento || !cuerpo.sucursal) {
      mostrarMensaje("mensaje", "Medicamento y sucursal son obligatorios.", "error");
      return;
    }
    if (!(cuerpo.cantidadDisponible >= 0)) {
      mostrarMensaje("mensaje", "La cantidad debe ser 0 o más.", "error");
      return;
    }

    // En edición no se puede cambiar la clave: reactivamos los campos solo
    // para leer el valor (están deshabilitados y no se envían en formularios,
    // pero aquí los leemos a mano, así que no afecta).
    btnGuardar.disabled = true;
    try {
      // POST hace upsert: si ya existe medicamento + sucursal, actualiza.
      await api("/inventario", { method: "POST", body: cuerpo });
      mostrarMensaje(
        "mensaje",
        editandoId === null
          ? `Stock de ${cuerpo.nombreMedicamento} en ${cuerpo.sucursal} guardado.`
          : `Stock #${editandoId} actualizado.`,
        "ok"
      );
      modoCrear();
      await cargarStock();
    } catch (error) {
      mostrarMensaje("mensaje", error.message, "error");
    } finally {
      btnGuardar.disabled = false;
    }
  });

  /* ------------------------------- Lista ---------------------------------- */

  async function cargarStock() {
    tbody.innerHTML = `<tr><td colspan="5" class="vacio">Cargando…</td></tr>`;

    try {
      const stocks = await api("/inventario");

      if (stocks.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="vacio">No hay stock cargado. Usa el formulario de la izquierda.</td></tr>`;
        return;
      }

      tbody.innerHTML = stocks
        .map(
          (s) => `
          <tr>
            <td>${s.id}</td>
            <td>${escapar(s.nombreMedicamento)}</td>
            <td>${escapar(s.sucursal)}</td>
            <td><strong>${s.cantidadDisponible}</strong></td>
            <td class="acciones-tabla">
              <button type="button" class="btn btn-secundario btn-chico" data-editar="${s.id}">Editar</button>
              <button type="button" class="btn btn-peligro btn-chico" data-eliminar="${s.id}">Eliminar</button>
            </td>
          </tr>`
        )
        .join("");

      tbody.querySelectorAll("[data-editar]").forEach((boton) =>
        boton.addEventListener("click", () => {
          const stock = stocks.find((s) => s.id === Number(boton.dataset.editar));
          if (stock) modoEdicion(stock);
        })
      );

      tbody.querySelectorAll("[data-eliminar]").forEach((boton) =>
        boton.addEventListener("click", async () => {
          const id = boton.dataset.eliminar;
          if (!confirm(`¿Eliminar el registro de stock #${id}?`)) return;
          try {
            await api(`/inventario/${id}`, { method: "DELETE" });
            mostrarMensaje("mensaje", `Stock #${id} eliminado.`, "ok");
            await cargarStock();
          } catch (error) {
            mostrarMensaje("mensaje", error.message, "error");
          }
        })
      );
    } catch (error) {
      tbody.innerHTML = `<tr><td colspan="5" class="vacio">No se pudo cargar: ${escapar(error.message)}</td></tr>`;
    }
  }

  document.getElementById("btn-recargar").addEventListener("click", cargarStock);

  cargarStock();
}
