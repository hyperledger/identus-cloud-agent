import { describe } from "node:test";

describe.skip("Error Handling Report Problem for Agent - Connection", async () => {
  describe("C1 - OOB Invitation has expired", async () => {
    //   Send a problem report (Invitation expired) e.p.msg.invitation-expired
  })

  describe("C2 - OOB is tampered / decoding error", async () => {
    //   Send a problem report (Invitation parsing decoding) e.p.msg.malformed-invitation
  })

  describe("C3 - Database connection or related issue", async () => {
    //   Send a problem report (DB connection issues) e.p.me.res.storage
  })
  describe("C4 - Max retries (Cascading Problems): Connection state cannot be moved after max retries", async () => {
    //   Send a problem report (After max retries) e.p.req.max-retries-exceeded
  })

  describe("C5 (G3) Send a problem report Any other error e.p.error", async () => {
    //   See G3
  })
})
