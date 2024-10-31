import { OutOfBandInvitation } from "@hyperledger/identus-edge-agent-sdk"
import { modifiedConnection } from "./didcomm/handle-oob-invitation"
import { CloudAgent } from "./model/cloud-agent"
import { WebhookServer } from "./model/webhook-server"
import { Wallet } from "./model/wallet"
import { waitFor } from "./util"

type Context = {
    wallet: Wallet,
    cloudAgent: CloudAgent,
    webhookServer: WebhookServer
    connectionEstabilished: boolean
    testData: Map<string, any>
}

export class Workflow {
    private readonly ctx: Context = {
        wallet: null,
        cloudAgent: null,
        webhookServer: null,
        connectionEstabilished: false,
        testData: new Map<string, any>()
    }

    async setup(config: { wallet?: boolean, cloudAgent?: boolean, server?: boolean}) {
        const setupList = []
        
        if (config.wallet) {
            this.ctx.wallet = new Wallet()
            setupList.push(this.ctx.wallet.init())
        }
        
        if (config.cloudAgent) {
            this.ctx.cloudAgent = new CloudAgent()
            setupList.push(this.ctx.cloudAgent.init())
        }

        if (config.server) {
            this.ctx.webhookServer = new WebhookServer()
            setupList.push(this.ctx.webhookServer.init())
        }

        await Promise.all(setupList)
    }

    async close() {
        const close = [
            this.ctx.wallet.close(),
            this.ctx.webhookServer.close()
        ]
        await Promise.all(close)
    }

    async createConnectionInvite() {
        const connectionResponse: any = await (await this.ctx.cloudAgent.createConnection()).json()
        const invitationUrl = connectionResponse.invitation.invitationUrl
        await this.ctx.wallet.usingSdk(async (sdk) => {
            const invitation = await sdk.parseOOBInvitation(new URL(invitationUrl))
            this.ctx.testData.set('invitation', invitation)
        })
    }

    async registerWebhook() {
        await this.ctx.cloudAgent.registerWebhook()
        this.ctx.testData.set('webhookMessages', [])
        this.ctx.webhookServer.listen((message) => {
            this.ctx.testData.get('webhookMessages').push(message)
        })
    }

    async sendWrongDidServiceOnConnection() {
        const invitation: OutOfBandInvitation = this.ctx.testData.get('invitation')
        const task = await modifiedConnection.wrongService(invitation)
        await this.ctx.wallet.runTask(task)
    }

    async waitForWebhookErrorNotification(callback: (messages: any[]) => Promise<boolean>) {
        await waitFor(async () => {
            return await callback(this.ctx.testData.get('webhookMessages'))
        })
    }
}
