export class RestFetch {
    private baseUrl: string = null
    private myHeaders = new Headers();

    constructor(baseUrl: string) {
        this.baseUrl = baseUrl
        this.myHeaders.append("Content-Type", "application/json");
    }

    private checkEndpoint(endpoint: string) {
        if (!endpoint.startsWith("/")) {
            throw Error("Endpoint should start with forward slash '/'")
        }
    }

    async get(endpoint: string): Promise<Response> {
        this.checkEndpoint(endpoint)

        const requestOptions: RequestInit = {
            method: "GET",
            headers: this.myHeaders,
            redirect: "follow"
        };

        return await fetch(`${this.baseUrl}${endpoint}`, requestOptions)
    }

    async post(endpoint: string, body?: any): Promise<Response> {
        this.checkEndpoint(endpoint)

        const requestOptions: RequestInit = {
            method: "POST",
            headers: this.myHeaders,
            body: JSON.stringify(body),
            redirect: "follow"
        };

        return await fetch(`${this.baseUrl}${endpoint}`, requestOptions)
    }
}