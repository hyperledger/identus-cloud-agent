import { EventEmitter } from "stream"
import InMemoryStore from "../common/inmemory"
import { Agent, Apollo, DIDCommContext, Domain, ListenerKey, Pluto, ProtocolType, Store, Task } from "@hyperledger/identus-edge-agent-sdk"
import { Initializable } from "./initializable"
import { Mediator } from "./mediator"

export class Wallet implements Initializable {

    private sdk: Agent = null
    private ctx: DIDCommContext = null
    private message = new EventEmitter()

    async init(): Promise<void> {
        this.sdk = await this.createSdk()
        await this.sdk.start()
    }

    async close() {
        this.sdk.stopFetchingMessages()
        await this.sdk.stop()
    }

    async runTask(task: Task<any, any>) {
        await this.ctx.run(task)
    }

    async usingSdk<T>(callback: (sdk: Agent) => Promise<T>) {
        return await callback(this.sdk)
    }

    private async createSdk(seed = undefined): Promise<Agent> {
        const apollo = new Apollo()
        const store = new Store({
            name: [...Array(30)].map(() => Math.random().toString(36)[2]).join(""),
            storage: InMemoryStore,
            password: "random12434",
            ignoreDuplicate: true
        })
        const pluto = new Pluto(store, apollo)
        const mediator = new Mediator()
        const mediatorDID = Domain.DID.fromString(await mediator.getMediatorDidThroughOob())
        const sdk = Agent.initialize({ seed, apollo, pluto, mediatorDID })

        this.ctx = new DIDCommContext({
            ConnectionManager: sdk.connectionManager,
            MediationHandler: sdk.mediationHandler,
            Mercury: sdk.mercury,
            Api: sdk.api,
            Apollo: sdk.apollo,
            Castor: sdk.castor,
            Pluto: sdk.pluto,
            Pollux: sdk.pollux,
            Seed: sdk.seed,
        });

        sdk.addListener(ListenerKey.MESSAGE, (messages: Domain.Message[]) => {
            for (const message of messages) {
                this.message.emit(ProtocolType.ProblemReporting, message)
            }
        })


        return sdk
    }

    async waitForProblemReportMessage(): Promise<Domain.Message> {
        return new Promise((resolve) => {
            this.message.once(ProtocolType.ProblemReporting, resolve);
        })
    }
}

