import { test, after, describe, before } from "node:test";
import { Workflow } from "../src/workflow";

describe("Receive error notification through webhook", async () => {
    const workflow = new Workflow()
    
    before(async () => {
        await workflow.setup({
            cloudAgent: true,
            wallet: true,
            server: true
        })
    })

    describe("Cloud agent should send an error notification when wrong service url on connection", async () => {
        test("Register cloud-agent webhook", async () => {
            await workflow.registerWebhook()
        })

        test("Create a connection invitation", async () => {
            await workflow.createConnectionInvite()
        })

        test("Send a message with malformed peer did service", async () => {
            await workflow.sendWrongDidServiceOnConnection()
        })

        test("Wait for webhook error notification", async () => {
            await workflow.waitForWebhookErrorNotification(async (messages: any[]) => {
                return messages.some((v) => {
                    return v.data?.kind == 'Connection'
                     && v.data?.metaLastFailure?.status == 400
                     && v.data?.metaLastFailure.detail?.includes('Connection refused')
                })
            })
        })
    })
  
    after(async () => {
        await workflow.close()
    })
})
