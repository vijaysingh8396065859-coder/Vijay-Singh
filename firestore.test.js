const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_dairy_owner_1";
const BOB_UID = "bob_dairy_owner_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCustomerPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "रमेश शर्मा",
    phone: "9876543210",
    address: "शिवाजी नगर, गली 4",
    milkType: "BUFFALO",
    defaultShift: "MORNING",
    defaultQtyLiters: 2.0,
    ratePerLiter: 70.0,
    openingBalance: 150.0,
    active: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validMilkEntryPayload(uid, customerId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    customerId,
    customerName: "रमेश शर्मा",
    dateKey: "2026-11-24",
    shift: "MORNING",
    milkType: "BUFFALO",
    quantityLiters: 2.0,
    ratePerLiter: 70.0,
    totalAmount: 140.0,
    absent: false,
    note: "सुबह का ताज़ा दूध",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validPaymentPayload(uid, customerId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    customerId,
    customerName: "रमेश शर्मा",
    dateKey: "2026-11-24",
    amount: 500.0,
    paymentMode: "UPI",
    note: "नवंबर का भुगतान",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/customers`)
      .doc("cust_1")
      .set(validCustomerPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/milk_entries`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/payments`).get());
});

test("Authenticated owner: valid customer create, list, partial update, toggle active, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const customersCol = aliceDb.collection(`users/${ALICE_UID}/customers`);

  // Empty list query succeeds (no existing() access in allow list)
  await assertSucceeds(customersCol.get());

  // Create customer
  await assertSucceeds(customersCol.doc("cust_1").set(validCustomerPayload(ALICE_UID)));

  // Get & list customer
  await assertSucceeds(customersCol.doc("cust_1").get());
  await assertSucceeds(customersCol.get());

  // Partial update: edit profile & defaults
  await assertSucceeds(
    customersCol.doc("cust_1").update({
      name: "रमेश कुमार शर्मा",
      ratePerLiter: 72.0,
      defaultQtyLiters: 2.5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Partial update: toggle active
  await assertSucceeds(
    customersCol.doc("cust_1").update({
      active: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete customer
  await assertSucceeds(customersCol.doc("cust_1").delete());
});

test("Customer rules reject ghost fields, @DocumentId id key, future timestamps, immutable field mutations, and invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const customersCol = aliceDb.collection(`users/${ALICE_UID}/customers`);

  // Ghost field rejected (Shadow Update / Create test)
  await assertFails(
    customersCol.doc("cust_ghost").set(validCustomerPayload(ALICE_UID, { isVerified: true }))
  );

  // Redundant @DocumentId `id` field rejected
  await assertFails(
    customersCol.doc("cust_id").set(validCustomerPayload(ALICE_UID, { id: "cust_id" }))
  );

  // Future timestamp rejected
  await assertFails(
    customersCol
      .doc("cust_future")
      .set(validCustomerPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Invalid milkType enum rejected
  await assertFails(
    customersCol.doc("cust_enum").set(validCustomerPayload(ALICE_UID, { milkType: "GOAT" }))
  );

  // Zero ratePerLiter rejected
  await assertFails(
    customersCol.doc("cust_rate").set(validCustomerPayload(ALICE_UID, { ratePerLiter: 0 }))
  );

  // Spoofed userId rejected
  await assertFails(
    customersCol.doc("cust_spoof").set(validCustomerPayload(BOB_UID))
  );

  // Valid customer then test immutable field tamper
  await assertSucceeds(customersCol.doc("cust_1").set(validCustomerPayload(ALICE_UID)));
  await assertFails(
    customersCol.doc("cust_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    customersCol.doc("cust_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    customersCol.doc("cust_1").update({
      ratePerLiter: "invalid_type",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("MilkEntry and Payment require existing customer and enforce immutability & validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const customersCol = aliceDb.collection(`users/${ALICE_UID}/customers`);
  const entriesCol = aliceDb.collection(`users/${ALICE_UID}/milk_entries`);
  const paymentsCol = aliceDb.collection(`users/${ALICE_UID}/payments`);

  // Orphan milk entry & payment before customer exists must fail
  await assertFails(
    entriesCol.doc("entry_orphan").set(validMilkEntryPayload(ALICE_UID, "missing_cust"))
  );
  await assertFails(
    paymentsCol.doc("pay_orphan").set(validPaymentPayload(ALICE_UID, "missing_cust"))
  );

  // Create customer first
  await assertSucceeds(customersCol.doc("cust_1").set(validCustomerPayload(ALICE_UID)));

  // Create valid milk entry
  await assertSucceeds(
    entriesCol.doc("entry_1").set(validMilkEntryPayload(ALICE_UID, "cust_1"))
  );
  await assertSucceeds(entriesCol.get());

  // Partial update on milk entry (mark absent)
  await assertSucceeds(
    entriesCol.doc("entry_1").update({
      quantityLiters: 0.0,
      totalAmount: 0.0,
      absent: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject mutating customerId on milk entry
  await assertFails(
    entriesCol.doc("entry_1").update({
      customerId: "cust_2",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject invalid dateKey format
  await assertFails(
    entriesCol
      .doc("entry_bad_date")
      .set(validMilkEntryPayload(ALICE_UID, "cust_1", { dateKey: "24-11-2026" }))
  );

  // Create valid payment
  await assertSucceeds(
    paymentsCol.doc("pay_1").set(validPaymentPayload(ALICE_UID, "cust_1"))
  );
  await assertSucceeds(paymentsCol.get());

  // Partial update on payment
  await assertSucceeds(
    paymentsCol.doc("pay_1").update({
      amount: 750.0,
      paymentMode: "CASH",
      note: "नकद प्राप्त",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Zero or negative payment amount rejected
  await assertFails(
    paymentsCol.doc("pay_zero").set(validPaymentPayload(ALICE_UID, "cust_1", { amount: 0 }))
  );
  await assertFails(
    paymentsCol.doc("pay_neg").set(validPaymentPayload(ALICE_UID, "cust_1", { amount: -100 }))
  );
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection(`users/${ALICE_UID}/customers`)
      .doc("cust_1")
      .set(validCustomerPayload(ALICE_UID));
    await db
      .collection(`users/${ALICE_UID}/milk_entries`)
      .doc("entry_1")
      .set(validMilkEntryPayload(ALICE_UID, "cust_1"));
    await db
      .collection(`users/${ALICE_UID}/payments`)
      .doc("pay_1")
      .set(validPaymentPayload(ALICE_UID, "cust_1"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").get());
  await assertFails(
    bobDb
      .collection(`users/${ALICE_UID}/customers`)
      .doc("cust_1")
      .update({ name: "Hijacked", updatedAt: new Date(Date.now() - 200) })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").delete());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/milk_entries`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(
    bobDb
      .collection(`users/${ALICE_UID}/payments`)
      .doc("pay_hack")
      .set(validPaymentPayload(BOB_UID, "cust_1"))
  );
});
