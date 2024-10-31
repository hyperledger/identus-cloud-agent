export class Mediator {
    private static MEDIATOR_OOB_URL = process.env.MEDIATOR_OOB_URL || "http://localhost:8080/invitationOOB"

    async getMediatorDidThroughOob() {
        const myHeaders = new Headers();
        myHeaders.append("Content-Type", "application/json");

        const requestOptions: RequestInit = {
            method: "GET",
            headers: myHeaders,
            redirect: "follow"
        };

        const response = await (await fetch(Mediator.MEDIATOR_OOB_URL, requestOptions)).text()
        const encodedData = response.split("?_oob=")[1]
        const oobData = JSON.parse(Buffer.from(encodedData, "base64").toString())
        return oobData.from
    }
}
