# Custom Types

Right now, the `openapi-generator` tool being used [doesn't cover](https://openapi-generator.tech/docs/generators/kotlin#schema-support-feature)
all the `OAS 3+` features. The goal of this document is to define some processes when we have to manually write the models for it.

- OpenApi Generator for Kotlin: https://openapi-generator.tech/docs/generators/kotlin

## OneOf

- Specification: https://swagger.io/specification/#discriminator-object

1. Generate the models
2. Find the model you want to override
3. Add the file to `.openapi-generator-ignore`
4. Implement the files
   1. Model class
   2. Interfaces
   3. Adapters
5. Add to git
6. Push

### OneOf - File Implementation

The strategy found to implement the `OneOf` was through interfaces.

E.g.
```yml
AnimalResponse:
  oneOf:
  - $ref: '#/components/schemas/Cat'
  - $ref: '#/components/schemas/Dog'
  - $ref: '#/components/schemas/Lizard'
```

Generating the models will create the `AnimalResponse` model class file. 
In this case `AnimalResponse` can be one of these 3 types

- Cat
- Dog
- Lizard

The next step is to create a new base types (interfaces) in `org.hyperledger.identus.client.custom.types.base` package.

Since we are not going to add a new `model` we just have to create the interfaces in that package.

It's `mandatory` that each interface implements the `BaseType` interface.

#### Interfaces

```kotlin
interface CatType : BaseType {
    fun meow(): String {
        return convert()
    }
}
```

```kotlin
interface DogType : BaseType {
    fun bark(): String {
        return convert()
    }
}
```

```kotlin
interface LizardType : BaseType {
    fun chirp(): String {
        return convert()
    }
}
```

#### Model class

```kotlin
@JsonAdapter(AnimalResponseAdapter::class)
class AnimalResponse(override var value: Any?) : CatType, DogType, LizardType
```

#### Adapter

If there's no `discriminator` you'll have to try to parse each type to retrieve the typed object.

```kotlin
class ServiceTypeAdapter : TypeAdapter<AnimalResponse>() {
    override fun write(out: JsonWriter, value: AnimalResponse) = // ...
    override fun read(input: JsonReader): AnimalResponse {
        // use the generic Extension.read(input) to retrieve the object as `Hashmap`
        // and add the cases to deserialize the object.
        // this method sets the `value` of the base type
        return AnimalResponse
    }
}
```

Or add the `GenericObjectType` interface to the type and retrieve using the `.asObject(MyClass::class)` method.
