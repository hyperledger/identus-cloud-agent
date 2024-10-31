import { CreatePeerDID, DIDCommContext, Domain, HandshakeRequest, OutOfBandInvitation, Task } from "@hyperledger/identus-edge-agent-sdk"

interface Args {
    invitation: OutOfBandInvitation;
    alias?: string;
    callback: (callback: Domain.Message, ctx: DIDCommContext) => Promise<void>
}

class HandleOOBInvitation extends Task<void, Args> {
    private ctx: DIDCommContext
    response: any = null

    async run(ctx: DIDCommContext) {
        this.ctx = ctx
        const peerDID = await ctx.run(new CreatePeerDID({ services: [], updateMediator: true }));
        const request = HandshakeRequest.fromOutOfBand(this.args.invitation, peerDID);
        await this.modifiedSendMessage(request.makeMessage(), this.args.callback);
        const pair = new Domain.DIDPair(peerDID, request.to, "test");
        await ctx.ConnectionManager.addConnection(pair);
        await ctx.ConnectionManager.cancellables[0].then()
    }

    private async modifiedSendMessage(
        message: Domain.Message,
        callback: (callback: Domain.Message, ctx: DIDCommContext) => Promise<void>
    ): Promise<Domain.Message | undefined> {
        message.direction = Domain.MessageDirection.SENT;
        await this.ctx.Pluto.storeMessage(message);
        return this.modifiedSendMessageParseMessage(message, callback);
    }

    private async makeRequest<T>(service: Domain.Service | URL | undefined, message: string) {
        const headers = new Map();
        headers.set("Content-type", "application/didcomm-encrypted+json");
        const requestUrl = service instanceof URL ? service.toString() : service.serviceEndpoint.uri;
        const response = await this.ctx.Api.request<T>("POST", requestUrl, new Map(), headers, message);
        this.response = response
        return response.body;
    }

    private async modifiedSendMessageParseMessage(
        message: Domain.Message,
        callback: (callback: Domain.Message, ctx: DIDCommContext) => Promise<void>
    ): Promise<Domain.Message | undefined> {
        try {
            const responseBody = await this.modifiedMercurySendMessage(message, callback);
            const responseJSON = JSON.stringify(responseBody);
            return await this.ctx.Mercury.unpackMessage(responseJSON);
        } catch (err) {
            return undefined
        }
    }

    private async modifiedMercurySendMessage(
        message: Domain.Message,
        callback: (callback: Domain.Message, ctx: DIDCommContext) => Promise<void>
    ): Promise<Uint8Array> {
        const toDid = message.to;
        const document = await this.ctx.Castor.resolveDID(toDid.toString());
        // Didcomm message change
        await callback(message, this.ctx)
        const packedMessage = await this.ctx.Mercury.packMessage(message);
        const service = document.services.find((x) => x.isDIDCommMessaging);
        const response: Promise<Uint8Array> = this.makeRequest(service, packedMessage);
        return response
    }
}

export const modifiedConnection = {
    wrongService: async (oobInvitation: OutOfBandInvitation) => {
        return new HandleOOBInvitation({
            invitation: oobInvitation,
            callback: async (message, ctx) => {
                const service = await ctx.run(new CreatePeerDID({
                    services: [
                        new Domain.Service(
                            "#didcomm-1",
                            ["DIDCommMessaging"],
                            new Domain.ServiceEndpoint(
                                "http://localhost:1234",
                                ['didcomm/v2'],
                                null
                            )
                        )
                    ],
                    updateMediator: false
                }))

                const peerDID = await ctx.run(new CreatePeerDID({
                    services: [
                        new Domain.Service(
                            "#didcomm-1",
                            ["DIDCommMessaging"],
                            new Domain.ServiceEndpoint(service.toString())
                        )
                    ],
                    updateMediator: false
                }))

                //@ts-ignore
                message.from = peerDID
            }
        })
    }
}
