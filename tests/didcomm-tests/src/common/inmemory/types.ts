import type {
  DefaultPreparedQuery,
  RxDocumentData,
  RxDocumentDataById,
  RxJsonSchema,
  RxStorage
} from "rxdb"

/**
 * Index of a table can be a string or a number
 */
export type IndexType = string | number
/**
 * InMemory internal data structure is a Map with an index
 * and RxDocumentData from RXDB
 */
export type InMemoryDataStructure<RxDocType> = Map<IndexType, RxDocumentData<RxDocType>>
/**
 * Data type for index keystorage
 * I used this to get faster searches based on what RXDB indexes we were
 * informed
 */
export type InMemoryDataIndex = Map<IndexType, IndexType[]>
/**
 * Query type for InMemory
 */
export type InMemoryPreparedQuery<DocType> = DefaultPreparedQuery<DocType>
/**
 * Main storage interface for InMemoryStorage
 */
export interface InMemoryStorageInternals<RxDocType> {
  data: InMemoryDataStructure<RxDocType>
  index: InMemoryDataIndex
  documents: InMemoryDataStructure<RxDocType>
  removed: boolean
  refCount: number
  addIndex: (indexName: string, docId: IndexType) => any
  removeFromIndex: (indexName: string, id: string) => void
  bulkPut: (
    items: any,
    collectionName: string,
    schema: Readonly<RxJsonSchema<RxDocumentData<RxDocType>>>) => any
  bulkGet: (docIds: string[], withDeleted: boolean) => RxDocumentDataById<RxDocType>
  clear: () => void
}

export type RxStorageInMemoryType<RxDocType> = RxStorage<RxDocType, InMemorySettings>

export interface InMemorySettings { }
