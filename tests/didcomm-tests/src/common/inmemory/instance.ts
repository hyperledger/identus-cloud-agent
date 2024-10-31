import type {
  RxStorageInstance,
  RxStorageDefaultCheckpoint,
  StringKeys,
  RxDocumentData,
  EventBulk,
  RxStorageChangeEvent,
  RxJsonSchema,
  BulkWriteRow,
  RxStorageBulkWriteResponse,
  RxDocumentDataById,
  RxStorageQueryResult,
  RxStorageCountResult,
  RxConflictResultionTask,
} from "rxdb"

import type {
  QueryMatcher
} from "rxdb/dist/types/types"

import type {
  Observable
} from "rxjs"

import type {
  InMemoryStorageInternals,
  InMemorySettings,
  RxStorageInMemoryType,
  InMemoryPreparedQuery
} from "./types"


import {
  categorizeBulkWriteRows,
  ensureNotFalsy,
  now,
  getPrimaryFieldOfPrimaryKey,
  getQueryMatcher,
  getSortComparator
} from "rxdb"

import {
  Subject
} from "rxjs"

function fixTxPipe(str: string): string {
  const split = str.split(".")
  if (split.length > 1) {
    return split.map(part => fixTxPipe(part)).join(".")
  }

  return str
}

export class RxStorageIntanceInMemory<RxDocType> implements RxStorageInstance<
  RxDocType,
  InMemoryStorageInternals<RxDocType>,
  InMemorySettings,
  RxStorageDefaultCheckpoint> {
  public readonly primaryPath: StringKeys<RxDocumentData<RxDocType>>
  public conflictResultionTasks$ = new Subject<RxConflictResultionTask<RxDocType>>()
  public changes$ = new Subject<EventBulk<RxStorageChangeEvent<RxDocumentData<RxDocType>>, RxStorageDefaultCheckpoint>>()
  public closed: boolean = false

  constructor(
    public readonly storage: RxStorageInMemoryType<RxDocType>,
    public readonly databaseName: string,
    public readonly collectionName: string,
    public readonly schema: Readonly<RxJsonSchema<RxDocumentData<RxDocType>>>,
    public readonly internals: InMemoryStorageInternals<RxDocType>,
    public readonly options: Readonly<InMemorySettings>
  ) {
    this.primaryPath = getPrimaryFieldOfPrimaryKey(this.schema.primaryKey)
  }

  async bulkWrite(
    documentWrites: Array<BulkWriteRow<RxDocType>>,
    context: string): Promise<RxStorageBulkWriteResponse<RxDocType>> {
    const primaryPath = this.primaryPath
    const ret: RxStorageBulkWriteResponse<RxDocType> = {
      success: {},
      error: {}
    }

    const documents = this.internals.documents
    const fixed = documentWrites.reduce<Array<BulkWriteRow<RxDocType>>>((fixedDocs, currentWriteDoc) => {
      const currentId = currentWriteDoc.document[this.primaryPath] as any
      const previousDocument = currentWriteDoc.previous ?? this.internals.documents.get(currentId)
      if (context === "data-migrator-delete") {
        if (previousDocument) {
          currentWriteDoc.document = {
            ...previousDocument,
            _deleted: true
          }
          currentWriteDoc.previous = {
            ...previousDocument,
            _deleted: false
          }
          fixedDocs.push(currentWriteDoc)
        }
      } else {
        if (previousDocument && previousDocument._rev !== currentWriteDoc.document._rev) {
          currentWriteDoc.previous = previousDocument
        } else {
          currentWriteDoc.previous = undefined
        }
        fixedDocs.push(currentWriteDoc)
      }
      return fixedDocs
    }, [])

    const categorized = categorizeBulkWriteRows<RxDocType>(
      this,
      primaryPath as any,
      documents as any,
      fixed,
      context
    )
    ret.error = categorized.errors

    /**
        * Do inserts/updates
        */
    const bulkInsertDocs = categorized.bulkInsertDocs
    for (let i = 0; i < bulkInsertDocs.length; ++i) {
      const writeRow = bulkInsertDocs[i]!
      const docId = writeRow.document[primaryPath]
      await this.internals.bulkPut([writeRow.document], this.collectionName, this.schema)
      ret.success[docId as any] = writeRow.document
    }

    const bulkUpdateDocs = categorized.bulkUpdateDocs
    for (let i = 0; i < bulkUpdateDocs.length; ++i) {
      const writeRow = bulkUpdateDocs[i]!
      const docId = writeRow.document[primaryPath]
      await this.internals.bulkPut([writeRow.document], this.collectionName, this.schema)
      ret.success[docId as any] = writeRow.document
    }

    if (categorized.eventBulk.events.length > 0) {
      const lastState = ensureNotFalsy(categorized.newestRow).document
      categorized.eventBulk.checkpoint = {
        id: lastState[primaryPath],
        lwt: lastState._meta.lwt
      }
      const endTime = now()
      categorized.eventBulk.events.forEach(event => {
        (event as any).endTime = endTime
      })
      this.changes$.next(categorized.eventBulk)
    }

    return await Promise.resolve(ret)
  }

  async findDocumentsById(ids: string[], withDeleted: boolean): Promise<RxDocumentDataById<RxDocType>> {
    return this.internals.bulkGet(ids, withDeleted)
  }

  async query(preparedQuery: InMemoryPreparedQuery<RxDocType>): Promise<RxStorageQueryResult<RxDocType>> {
    const { queryPlan, query } = preparedQuery
    const selector = query.selector
    const selectorKeys = Object.keys(selector)
    const skip = query.skip ? query.skip : 0
    const limit = query.limit ? query.limit : Infinity
    const skipPlusLimit = skip + limit
    const queryMatcher: QueryMatcher<RxDocumentData<RxDocType>> = getQueryMatcher(
      this.schema,
      query
    )

    const queryPlanFields: string[] = queryPlan.index
    const indexes: string[] = []
    if (queryPlanFields.length === 1) {
      indexes.push(fixTxPipe(queryPlanFields[0]!))
    } else {
      indexes.push(...queryPlanFields.map(field => fixTxPipe(field)))
    }

    const shouldAddCompoundIndexes = this.schema.indexes?.find((index) => {
      if (typeof index === "string") {
        return indexes.find((index2) => index2 === index)
      } else {
        return index.find((subIndex) => {
          return subIndex === index.find((indexValue) => indexValue === subIndex)
        })
      }
    })

    if (shouldAddCompoundIndexes) {
      indexes.splice(0, indexes.length)
      indexes.push(this.collectionName)
      if (typeof shouldAddCompoundIndexes === "string") {
        indexes.push(shouldAddCompoundIndexes)
      } else {
        indexes.push(...shouldAddCompoundIndexes)
      }
    } else {
      indexes.unshift(this.collectionName)
    }

    const indexName: string = `[${indexes.join("+")}]`
    const documentIds = this.internals.index.get(indexName)

    if (!documentIds) {
      return { documents: [] }
    }

    let documents = documentIds.reduce<Array<RxDocumentData<RxDocType>>>((allDocuments, id) => {
      const document = this.internals.data.get(id)
      if (document) {
        if (selectorKeys.length <= 0) {
          return [...allDocuments, document]
        }
        const matches = queryMatcher(document)
        if (matches) {
          return [...allDocuments, document]
        }
      }
      return allDocuments
    }, [])

    const sortComparator = getSortComparator(this.schema, preparedQuery.query)
    documents = documents.sort(sortComparator)
    documents = documents.slice(skip, skipPlusLimit)
    return {
      documents
    }
  }

  async count(preparedQuery: any): Promise<RxStorageCountResult> {
    const result = await this.query(preparedQuery)
    return {
      count: result.documents.length,
      mode: "fast"
    }
  }

  /* istanbul ignore next */
  async getAttachmentData(): Promise<string> {
    throw new Error("Method not implemented.")
  }

  /* istanbul ignore next */
  async getChangedDocumentsSince(): Promise<{ documents: Array<RxDocumentData<RxDocType>>, checkpoint: RxStorageDefaultCheckpoint }> {
    throw new Error("Method not implemented.")
  }

  /* istanbul ignore next */
  changeStream(): Observable<EventBulk<RxStorageChangeEvent<RxDocType>, RxStorageDefaultCheckpoint>> {
    return this.changes$.asObservable()
  }

  async cleanup(): Promise<boolean> {
    this.internals.clear()

    return true
  }

  /* istanbul ignore next */
  async close(): Promise<void> {
    if (this.closed) {
      await Promise.reject(new Error("already closed")); return
    }
    this.closed = true

    this.internals.refCount = this.internals.refCount - 1
  }

  /* istanbul ignore next */
  async remove(): Promise<void> {
    await Promise.resolve()
  }

  conflictResultionTasks(): Observable<RxConflictResultionTask<RxDocType>> {
    return this.conflictResultionTasks$.asObservable()
  }

  /* istanbul ignore next */
  async resolveConflictResultionTask(): Promise<void> {
    await Promise.resolve()
  }
}
