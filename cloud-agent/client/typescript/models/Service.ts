/**
* A service expressed in the DID document. https://www.w3.org/TR/did-core/#services
*/
export class Service {
    /**
    * The id of the service. Requires a URI fragment when use in create / update DID. Returns the full ID (with DID prefix) when resolving DID
    */
    'id': string;
    'type': Array<string>;
    'serviceEndpoint': string | Array<string> | object;

    static readonly discriminator: string | undefined = undefined;

    static readonly attributeTypeMap: Array<{name: string, baseName: string, type: string, format: string}> = [
        {
            "name": "id",
            "baseName": "id",
            "type": "string",
            "format": ""
        },
        {
            "name": "type",
            "baseName": "type",
            "type": "ServiceType",
            "format": ""
        },
        {
            "name": "serviceEndpoint",
            "baseName": "serviceEndpoint",
            "type": "Json",
            "format": ""
        }    ];

    static getAttributeTypeMap() {
        return Service.attributeTypeMap;
    }
}

