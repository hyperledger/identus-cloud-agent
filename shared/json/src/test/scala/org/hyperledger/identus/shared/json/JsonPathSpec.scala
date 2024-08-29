package org.hyperledger.identus.shared.json

import org.hyperledger.identus.shared.json.JsonPathError.{InvalidPathInput, PathNotFound}
import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.test.*
import zio.test.Assertion.*

object JsonPathSpec extends ZIOSpecDefault {

  override def spec = suite("JsonPathSpec")(
    test("sucessfully compile a valid json path") {
      val paths = Seq(
        "$.store.book[*].author",
        "$..author",
        "$.store.*",
        "$.store..price",
        "$..book[2]",
        "$..book[-1:]",
        "$..book[0,1]",
        "$..book[:2]",
        "$..book[?(@.isbn)]",
        "$..book[?(@.price<10)]",
        "$..book[?(@.price==8.95)]",
        "$..book[?(@.price<30 && @.category==\"fiction\")]",
        "$..*",
        "$['foo']['bar']"
      )
      ZIO
        .foreach(paths)(JsonPath.compile)
        .as(assertCompletes)
    },
    test("do not accept invalid json path") {
      val paths = Seq(
        "",
        " ",
        " $ ",
        "$$",
        "hello world",
      )
      ZIO
        .foreach(paths)(p => JsonPath.compile(p).flip)
        .map { errors =>
          assert(errors)(forall(isSubtype[InvalidPathInput](anything)))
        }
    },
    test("query valid path inside json structure") {
      val jsonStr =
        """{
          |  "vc": {
          |    "name": "alice",
          |    "age": 42,
          |    "degree": null,
          |    "pets": ["dog", "cat"],
          |    "isEmployed": false,
          |    "languages": {"english": "native", "chinese": "fluent"}
          |  }
          |}
        """.stripMargin
      for {
        json <- ZIO.fromEither(jsonStr.fromJson[Json])
        namePath <- JsonPath.compile("$.vc.name")
        agePath <- JsonPath.compile("$.vc.age")
        degreePath <- JsonPath.compile("$.vc.degree")
        petPath <- JsonPath.compile("$.vc.pets")
        firstPetPath <- JsonPath.compile("$.vc.pets[0]")
        isEmployedPath <- JsonPath.compile("$.vc.isEmployed")
        languagesPath <- JsonPath.compile("$.vc.languages")
        name <- namePath.read(json)
        age <- agePath.read(json)
        degree <- degreePath.read(json)
        pet <- petPath.read(json)
        firstPet <- firstPetPath.read(json)
        isEmployed <- isEmployedPath.read(json)
        languages <- languagesPath.read(json)
      } yield assert(name.asString)(isSome(equalTo("alice")))
        && assert(age.asNumber)(isSome(equalTo(Json.Num(42))))
        && assert(degree.asNull)(isSome(anything))
        && assert(pet.asArray)(isSome(hasSize((equalTo(2)))))
        && assert(firstPet.asString)(isSome(equalTo("dog")))
        && assert(isEmployed.asBoolean)(isSome(isFalse))
        && assert(languages.asObject)(isSome(anything))
    },
    test("query invalid path inside json structure") {
      val jsonStr =
        """{
          |  "vc": {
          |    "name": "alice",
          |    "pets": ["dog", "cat"]
          |  }
          |}
        """.stripMargin
      for {
        json <- ZIO.fromEither(jsonStr.fromJson[Json])
        nonExistingPath <- JsonPath.compile("$.vc2.name")
        invalidTypeArrayPath <- JsonPath.compile("$.vc.name[0]")
        outOfBoundArrayPath <- JsonPath.compile("$.vc.name[5]")
        outOfBoundSlicePath <- JsonPath.compile("$.vc.name[1:4]")
        exit1 <- nonExistingPath.read(json).exit
        exit2 <- invalidTypeArrayPath.read(json).exit
        exit3 <- outOfBoundArrayPath.read(json).exit
        exit4 <- outOfBoundSlicePath.read(json).exit
      } yield assert(exit1)(failsWithA[PathNotFound])
        && assert(exit2)(failsWithA[PathNotFound])
        && assert(exit3)(failsWithA[PathNotFound])
        && assert(exit4)(failsWithA[PathNotFound])
    }
  )

}
