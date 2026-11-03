const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { createHash } = require("node:crypto");

initializeApp();
const db = getFirestore();

function normalizeCode(value) {
  return String(value || "").trim().toUpperCase().replace(/\s+/g, "");
}

function hashCode(code) {
  return createHash("sha256").update(code, "utf8").digest("hex");
}

function isExpired(expiresAt) {
  return !!expiresAt && typeof expiresAt.toMillis === "function" &&
    expiresAt.toMillis() <= Date.now();
}

exports.validateVipCode = onCall({ enforceAppCheck: false }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Faça login para validar a licença.");
  }

  const code = normalizeCode(request.data && request.data.code);
  if (code.length < 10 || code.length > 80) {
    throw new HttpsError("invalid-argument", "Código inválido.");
  }

  const licenseId = hashCode(code);
  const licenseRef = db.collection("licenses").doc(licenseId);
  const entitlementRef = db.collection("vipEntitlements").doc(request.auth.uid);

  await db.runTransaction(async (tx) => {
    const licenseSnap = await tx.get(licenseRef);
    if (!licenseSnap.exists) {
      throw new HttpsError("not-found", "Licença não encontrada.");
    }

    const license = licenseSnap.data();
    if (license.active !== true || isExpired(license.expiresAt)) {
      throw new HttpsError("failed-precondition", "Licença expirada ou desativada.");
    }
    if (license.uid && license.uid !== request.auth.uid) {
      throw new HttpsError("failed-precondition", "Esta licença já está vinculada a outra conta.");
    }

    tx.set(licenseRef, {
      uid: request.auth.uid,
      redeemedAt: license.redeemedAt || Timestamp.now()
    }, { merge: true });

    tx.set(entitlementRef, {
      active: true,
      licenseId,
      expiresAt: license.expiresAt || null,
      updatedAt: Timestamp.now()
    });
  });

  return { valid: true };
});

exports.getVipStatus = onCall({ enforceAppCheck: false }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Faça login para consultar o VIP.");
  }

  const entitlementRef = db.collection("vipEntitlements").doc(request.auth.uid);
  const entitlementSnap = await entitlementRef.get();
  if (!entitlementSnap.exists) return { active: false };

  const entitlement = entitlementSnap.data();
  if (entitlement.active !== true || isExpired(entitlement.expiresAt)) {
    return { active: false };
  }

  const licenseSnap = await db.collection("licenses").doc(entitlement.licenseId).get();
  if (!licenseSnap.exists) return { active: false };
  const license = licenseSnap.data();

  const active = license.active === true &&
    license.uid === request.auth.uid &&
    !isExpired(license.expiresAt);

  return { active };
});
