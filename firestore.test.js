const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

// --- TESTS ---

test("Unauthenticated user: cannot read conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("conversations").get());
});

test("Authenticated user: Alice cannot read conversations she is not a participant of", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("bob_chat").set({
      conversationId: "bob_chat",
      type: "direct",
      title: "Bob & Charlie",
      participantUids: [BOB_UID, CHARLIE_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("conversations").doc("bob_chat").get());
});

test("Authenticated user: Alice can read conversation she is in", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("alice_bob_chat").set({
      conversationId: "alice_bob_chat",
      type: "direct",
      title: "Alice & Bob",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("conversations").doc("alice_bob_chat").get());
});

test("Query alignment: Alice can list conversations containing her UID", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("alice_chat_1").set({
      conversationId: "alice_chat_1",
      type: "group",
      title: "Family",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").where("participantUids", "array-contains", ALICE_UID).get()
  );
});

test("Create conversation: Alice can create conversation including herself", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_123").set({
      conversationId: "conv_123",
      type: "group",
      title: "Project Team",
      participantUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      isEncrypted: true,
      createdAt: new Date(),
    })
  );
});
