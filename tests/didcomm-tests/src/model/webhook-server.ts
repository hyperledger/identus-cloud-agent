import Fastify, { FastifyInstance } from 'fastify'
import { EventEmitter } from 'stream'
import { Initializable } from './initializable'

export class WebhookServer implements Initializable {
    static port: number = Number(process.env.WEBHOOK_PORT) || 3000
    static url: string = process.env.WEBHOOK_URL || `http://host.docker.internal:${WebhookServer.port}`

    private fastify: FastifyInstance = null
    private eventEmitter: EventEmitter = new EventEmitter()

    constructor() {
        this.fastify = Fastify()
        this.setupEndpoints()
    }

    private setupEndpoints() {
        this.fastify.post("/", async (req, res) => {
            this.eventEmitter.emit("message", req.body)
            res.code(202).send()
        })
    }

    async init() {
        try {
            if (!this.fastify.server.listening) {
                await this.fastify.listen({ port: WebhookServer.port })
            }
        } catch (err) {
            console.error(err)
            this.fastify.log.error(err)
            process.exit(1)
        }
    }

    async close() {
        this.eventEmitter.removeAllListeners()
        await this.fastify.close()
    }

    listen(callback: (message: any) => void) {
        this.eventEmitter.on("message", async (event) => {
            callback(event)
        })
    }
}
