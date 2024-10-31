import { RestFetch } from "../common/fetch";
import { Initializable } from "./initializable";
import { WebhookServer } from "./webhook-server";

export class CloudAgent implements Initializable {
    private static CLOUD_AGENT_URL = process.env.CLOUD_AGENT_URL || "http://localhost:8090"
    private restFetch = new RestFetch(CloudAgent.CLOUD_AGENT_URL)

    async init(): Promise<void> {
    }

    async createConnection() {
        const payload = {
            label: "alice",
            goalCode: "automation",
            goal: "automation required"
        }
        return await this.restFetch.post('/connections', payload)
    }

    async registerWebhook() {
        const payload = {
            "url": WebhookServer.url
        }
        this.restFetch.post('/events/webhooks', payload)
    }
}
