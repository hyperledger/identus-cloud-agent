import { type RxStorage, RxStorageDefaultStatics, type RxStorageInstance, type RxStorageInstanceCreationParams, newRxError } from "rxdb"
import { type InMemorySettings, type InMemoryStorageInternals, type RxStorageInMemoryType } from "./types"
import { RxStorageIntanceInMemory } from "./instance"
import { InMemoryInternal } from "./internal"

const internalInstance = new Map<string, InMemoryInternal<any>>()

function getRxStorageMemory<RxDocType>(settings: InMemorySettings = {}): RxStorageInMemoryType<RxDocType> {
  const inMemoryInstance: RxStorageInMemoryType<any> = {
    name: "in-memory",
    statics: RxStorageDefaultStatics,
    async createStorageInstance<RxDocType>(
      params: RxStorageInstanceCreationParams<RxDocType, InMemorySettings>
    ): Promise<RxStorageInstance<RxDocType, InMemoryStorageInternals<RxDocType>, InMemorySettings, any>> {
      if (params.schema.keyCompression) {
        throw newRxError("UT5", { args: { databaseName: params.databaseName, collectionName: params.collectionName } })
      }
      const existingInstance = internalInstance.get(params.databaseName)
      if (!existingInstance) {
        internalInstance.set(params.databaseName, new InMemoryInternal<RxDocType>(0))
      } else {
        existingInstance.refCount++
        internalInstance.set(params.databaseName, existingInstance)
      }
      return new RxStorageIntanceInMemory(
        this,
        params.databaseName,
        params.collectionName,
        params.schema,
        internalInstance.get(params.databaseName)!,
        settings
      )
    }
  }
  return inMemoryInstance
}

/**
 * InMemory storage
 * @description Use this as storage in our RXDB database. For now there is no initialisation settings, so you can use it out of the box.
 */
const storage: RxStorage<any, any> = getRxStorageMemory()

export default storage
