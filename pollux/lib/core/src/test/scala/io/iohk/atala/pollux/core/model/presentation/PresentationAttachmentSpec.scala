package io.iohk.atala.pollux.core.model.presentation

import io.circe.Json
import io.circe.parser.*
import io.circe.syntax.*
import munit.*

class PresentationAttachmentSpec extends ZSuite {

  test("Verifier Request Presentation Attachment") {
    val expectedConstraintJson = parse(s"""
        {
          "fields": [
            {
              "path": [
                "credentialSubject.dateOfBirth",
                "credentialSubject.dob",
                "vc.credentialSubject.dateOfBirth",
                "vc.credentialSubject.dob"
              ]
            }
          ]
        }
    """.stripMargin).getOrElse(Json.Null)
    val field = Field(
      None,
      path = Seq(
        "credentialSubject.dateOfBirth",
        "credentialSubject.dob",
        "vc.credentialSubject.dateOfBirth",
        "vc.credentialSubject.dob"
      )
    )
    val constraints = Constraints(fields = Some(Seq(field)))
    val result = constraints.asJson.deepDropNullValues
    assertEquals(result, expectedConstraintJson)

    val expectedInputDescriptorJson = parse(s"""
      {
        "id": "wa_driver_license",
        "name": "Washington State Business License",
        "purpose": "We can only allow licensed Washington State business representatives into the WA Business Conference",
        "constraints": {
          "fields": [
            {
              "path": [
                "credentialSubject.dateOfBirth",
                "credentialSubject.dob",
                "vc.credentialSubject.dateOfBirth",
                "vc.credentialSubject.dob"
              ]
            }
          ]
        }
      }
      """.stripMargin).getOrElse(Json.Null)

    val inputDescriptor = InputDescriptor(
      id = "wa_driver_license",
      name = Some("Washington State Business License"),
      purpose =
        Some("We can only allow licensed Washington State business representatives into the WA Business Conference"),
      constraints = constraints
    )
    val resultInputDescriptor = inputDescriptor.asJson.deepDropNullValues
    assertEquals(resultInputDescriptor, expectedInputDescriptorJson)

    val expectedPresentationDefinitionJson = parse(s"""
      {
        "id": "32f54163-7166-48f1-93d8-ff217bdb0653",
        "input_descriptors": [
          {
            "id": "wa_driver_license",
            "name": "Washington State Business License",
            "purpose": "We can only allow licensed Washington State business representatives into the WA Business Conference",
            "constraints": {
              "fields": [
                {
                  "path": [
                    "credentialSubject.dateOfBirth",
                    "credentialSubject.dob",
                    "vc.credentialSubject.dateOfBirth",
                    "vc.credentialSubject.dob"
                  ]
                }
              ]
            }
          }
        ]
      }
      """.stripMargin).getOrElse(Json.Null)

    val presentationDefinition =
      PresentationDefinition(id = "32f54163-7166-48f1-93d8-ff217bdb0653", input_descriptors = Seq(inputDescriptor))
    val resultPresentationDefinition = presentationDefinition.asJson.deepDropNullValues
    assertEquals(resultPresentationDefinition, expectedPresentationDefinitionJson)

    val expectedPresentationAttachmentJson = parse(s"""
      {
        "options": {
          "challenge": "23516943-1d79-4ebd-8981-623f036365ef",
          "domain": "us.gov/DriversLicense"
        },
        "presentation_definition": {
          "id": "32f54163-7166-48f1-93d8-ff217bdb0653",
          "input_descriptors": [
            {
              "id": "wa_driver_license",
              "name": "Washington State Business License",
              "purpose": "We can only allow licensed Washington State business representatives into the WA Business Conference",
              "constraints": {
                "fields": [
                  {
                    "path": [
                      "credentialSubject.dateOfBirth",
                      "credentialSubject.dob",
                      "vc.credentialSubject.dateOfBirth",
                      "vc.credentialSubject.dob"
                    ]
                  }
                ]
              }
            }
          ]
        }
      }
      """.stripMargin).getOrElse(Json.Null)
    val options = Options(challenge = "23516943-1d79-4ebd-8981-623f036365ef", domain = "us.gov/DriversLicense")
    val presentationAttachment =
      PresentationAttachment(presentation_definition = presentationDefinition, options = Some(options))
    val resultPresentationAttachment = presentationAttachment.asJson.deepDropNullValues
    println(resultPresentationAttachment)
    assertEquals(resultPresentationAttachment, expectedPresentationAttachmentJson)

  }
}
