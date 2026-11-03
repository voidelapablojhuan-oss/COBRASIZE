// Uso: node scripts/create-license.js CODIGO-VIP-LONGO [2027-12-31T23:59:59Z|never]
// Execute dentro da pasta functions após configurar Firebase CLI e autenticar.
// O código puro não é gravado no Firestore; somente seu hash SHA-256.
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { createHash } = require("node:crypto");

const code = String(process.argv[2] || "").trim().toUpperCase().replace(/\\s+/g, "");
const expiryArg = process.argv[3] || "never";

if (code.length < 10 || code.length > 80) {
  console.error("Informe um código entre 10 e 80 caracteres.");
  process.exit(1);
}

let expiresAt = null;
if (expiryArg !== "never") {
  const date = new Date(expiryArg);
  if (Number.isNaN(date.getTime()) || date.getTime() <= Date.now()) {
    console.error("A validade precisa ser uma data futura em ISO 8601.");
    process.exit(1);
  }
  expiresAt = Timestamp.fromDate(date);
}

initializeApp();
const db = getFirestore();
const id = createHash("sha256").update(code, "utf8").digest("hex");

(async () => {
  const ref = db.collection("licenses").doc(id);
  const existing = await ref.get();
  if (existing.exists) {
    console.error("Já existe uma licença com esse código.");
    process.exitCode = 1;
    return;
  }
  await ref.create({
    active: true,
    uid: null,
    expiresAt,
    createdAt: Timestamp.now()
  });
  console.log("Licença criada. Entregue o código ao usuário por um canal privado.");
})().catch((error) => {
  console.error("Falha ao criar licença:", error.message);
  process.exitCode = 1;
});
