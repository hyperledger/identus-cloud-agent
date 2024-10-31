
export async function waitFor(condition: () => Promise<boolean>): Promise<boolean> {
    let timeoutId: NodeJS.Timeout = null
    let pollingId: NodeJS.Timeout = null

    const clear = () => {
        if (timeoutId) clearTimeout(timeoutId)
        if (pollingId) clearInterval(pollingId)
    }

    const timeoutPromise = new Promise<boolean>((_, reject) => {
        timeoutId = setTimeout(() => {
            clear()
            reject(new Error('Polling timed out'))
        }, 30000);
    });

    const conditionPromise = new Promise<boolean>(async (resolve, reject) => {
        pollingId = setInterval(async () => {
            try {
                if (await condition()) {
                    clear()
                    resolve(true);
                }
            } catch (e) {
                clear()
                reject(e)
            }
        }, 1000);
    });

    return Promise.race([conditionPromise, timeoutPromise]);
}
