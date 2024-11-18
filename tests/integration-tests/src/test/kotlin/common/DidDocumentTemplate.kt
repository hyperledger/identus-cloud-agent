package common

import org.hyperledger.identus.client.models.ManagedDIDKeyTemplate
import org.hyperledger.identus.client.models.Service

data class DidDocumentTemplate(
    val publicKeys: MutableList<ManagedDIDKeyTemplate>,
    val services: MutableList<Service>,
)
