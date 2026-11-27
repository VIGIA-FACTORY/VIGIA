import { type FormEvent, useCallback, useEffect, useState } from "react";
import type { Api, MemberType, Person } from "./api";

const TYPES: Record<MemberType, string> = { STUDENT: "Estudiante", PROFESSOR: "Docente", STAFF: "Personal" };

export function PeoplePage({ api, canWrite }: { api: Api; canWrite: boolean }) {
  const [people, setPeople] = useState<Person[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const load = useCallback(() => {
    api.people().then(setPeople).catch((failure: Error) => setMessage(failure.message));
  }, [api]);
  useEffect(load, [load]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? "").trim();
    try {
      await api.registerPerson({
        documentNumber: text("documentNumber"),
        firstName: text("firstName"),
        lastName: text("lastName"),
        email: text("email") || null,
        memberType: text("memberType") as MemberType,
      });
      event.currentTarget.reset();
      setMessage("Persona registrada");
      load();
    } catch (failure) {
      setMessage((failure as Error).message);
    }
  }

  return (
    <section>
      <h2>Personas</h2>
      {canWrite && (
        <form onSubmit={(event) => void submit(event)}>
          <input name="documentNumber" placeholder="Cédula o pasaporte" required />
          <input name="firstName" placeholder="Nombres" required />
          <input name="lastName" placeholder="Apellidos" required />
          <input name="email" type="email" placeholder="Correo (opcional)" />
          <select name="memberType" defaultValue="STUDENT">
            {Object.entries(TYPES).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <button type="submit">Registrar</button>
        </form>
      )}
      {message && <p className="notice">{message}</p>}
      <table>
        <thead>
          <tr>
            <th>Documento</th>
            <th>Nombre</th>
            <th>Tipo</th>
            <th>Correo</th>
          </tr>
        </thead>
        <tbody>
          {people.map((person) => (
            <tr key={person.id}>
              <td>{person.documentNumber}</td>
              <td>
                {person.firstName} {person.lastName}
              </td>
              <td>{TYPES[person.memberType]}</td>
              <td>{person.email ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
