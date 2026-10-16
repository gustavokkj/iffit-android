import { readFileSync } from "node:fs";
import { after, before, beforeEach, test } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { get, ref, set, update } from "firebase/database";

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-iffit",
    database: { rules: readFileSync(new URL("./database.rules.example.json", import.meta.url), "utf8") }
  });
});
beforeEach(async () => env.clearDatabase());
after(async () => env?.cleanup());

const activity = (uid = "alice") => ({
  id: "run-1", usuarioId: uid, modalidade: "Corrida",
  inicio: 1700000000000, duracaoSegundos: 120, distanciaMetros: 0
});
const db = (uid) => env.authenticatedContext(uid).database();

test("owner can save two minutes with zero distance and read the result", async () => {
  const target = ref(db("alice"), "atividades/alice/run-1");
  await assertSucceeds(set(target, activity()));
  await assertSucceeds(get(target));
});
test("another account cannot read or write the owner's activity", async () => {
  await assertSucceeds(set(ref(db("alice"), "atividades/alice/run-1"), activity()));
  const other = ref(db("bob"), "atividades/alice/run-1");
  await assertFails(get(other));
  await assertFails(set(other, activity()));
});
test("anonymous access is denied", async () => {
  const anonymous = ref(env.unauthenticatedContext().database(), "atividades/alice/run-1");
  await assertFails(set(anonymous, activity()));
  await assertFails(get(anonymous));
});
test("invalid activity values are denied", async () => {
  const target = ref(db("alice"), "atividades/alice/run-1");
  for (const invalid of [
    { ...activity(), usuarioId: "bob" }, { ...activity(), id: "other-id" },
    { ...activity(), distanciaMetros: -1 }, { ...activity(), duracaoSegundos: 0 },
    { ...activity(), modalidade: "Produto" }, { ...activity(), extra: true }
  ]) await assertFails(set(target, invalid));
});
test("profile updates preserve legacy fields and cannot cross accounts", async () => {
  const target = ref(db("alice"), "users/alice");
  await assertSucceeds(set(target, { nome: "Alice", endereco: "Campo antigo" }));
  await assertSucceeds(update(target, { cidade: "Curitiba", bio: "Corrida" }));
  const snapshot = await get(target);
  if (snapshot.child("endereco").val() !== "Campo antigo") throw new Error("Legacy data lost");
  await assertFails(get(ref(db("bob"), "users/alice")));
  await assertFails(set(ref(db("bob"), "users/alice"), { nome: "Bob" }));
});
