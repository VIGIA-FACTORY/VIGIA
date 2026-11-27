import { type FormEvent, useCallback, useEffect, useState } from "react";
import type { Api, Person, Vehicle } from "./api";

export function VehiclesPage({ api, canWrite }: { api: Api; canWrite: boolean }) {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const load = useCallback(() => {
    Promise.all([api.vehicles(), api.people()])
      .then(([loadedVehicles, loadedPeople]) => {
        setVehicles(loadedVehicles);
        setPeople(loadedPeople);
      })
      .catch((failure: Error) => setMessage(failure.message));
  }, [api]);
  useEffect(load, [load]);

  const ownerName = (id: string) => {
    const owner = people.find((person) => person.id === id);
    return owner ? `${owner.firstName} ${owner.lastName}` : id;
  };

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? "").trim();
    try {
      await api.registerVehicle({
        plate: text("plate"),
        ownerId: text("ownerId"),
        brand: text("brand") || null,
        model: text("model") || null,
        color: text("color") || null,
      });
      event.currentTarget.reset();
      setMessage("Vehículo registrado");
      load();
    } catch (failure) {
      setMessage((failure as Error).message);
    }
  }

  return (
    <section>
      <h2>Vehículos</h2>
      {canWrite && (
        <form onSubmit={(event) => void submit(event)}>
          <input name="plate" placeholder="Placa (PBA-1234)" required />
          <select name="ownerId" required defaultValue="">
            <option value="" disabled>
              Propietario
            </option>
            {people.map((person) => (
              <option key={person.id} value={person.id}>
                {person.firstName} {person.lastName}
              </option>
            ))}
          </select>
          <input name="brand" placeholder="Marca" />
          <input name="model" placeholder="Modelo" />
          <input name="color" placeholder="Color" />
          <button type="submit">Registrar</button>
        </form>
      )}
      {message && <p className="notice">{message}</p>}
      <table>
        <thead>
          <tr>
            <th>Placa</th>
            <th>Propietario</th>
            <th>Marca y modelo</th>
            <th>Color</th>
          </tr>
        </thead>
        <tbody>
          {vehicles.map((vehicle) => (
            <tr key={vehicle.id}>
              <td>{vehicle.plate}</td>
              <td>{ownerName(vehicle.ownerId)}</td>
              <td>{[vehicle.brand, vehicle.model].filter(Boolean).join(" ") || "—"}</td>
              <td>{vehicle.color ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
