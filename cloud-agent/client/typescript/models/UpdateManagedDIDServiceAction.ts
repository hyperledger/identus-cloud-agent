/**
* A patch to existing Service. \'type\' and \'serviceEndpoint\' cannot both be empty.
*/
export class UpdateManagedDIDServiceAction {
    /**
    * The id of the service to update
    */
    'id': string;
    'type'?: Array<string>;
    'serviceEndpoint'?: string | Array<string> | object;

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
            "type": "UpdateManagedDIDServiceActionType",
            "format": ""
        },
        {
            "name": "serviceEndpoint",
            "baseName": "serviceEndpoint",
            "type": "Json",
            "format": ""
        }    ];

    static getAttributeTypeMap() {
        return UpdateManagedDIDServiceAction.attributeTypeMap;
    }
}

