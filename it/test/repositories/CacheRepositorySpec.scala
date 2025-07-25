/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import itbase.CacheRepositorySpecBase
import models.Sort.{SortByCreatedAtAsc, SortByCreatedAtDesc, SortByMRNAsc, SortByMRNDesc}
import models.{UserAnswers, UserAnswersSummary}
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.model.Filters
import play.api.libs.json.Json
import org.mongodb.scala.*

import java.time.Instant
import java.time.temporal.ChronoUnit.*

class CacheRepositorySpec extends CacheRepositorySpecBase {

  private lazy val userAnswers1 = emptyUserAnswers.copy(
    metadata = emptyMetadata.copy(mrn = "ABCD1111111111111", eoriNumber = "EoriNumber1")
  )

  private lazy val userAnswers2 = emptyUserAnswers.copy(
    metadata = emptyMetadata.copy(mrn = "ABCD2222222222222", eoriNumber = "EoriNumber2")
  )

  private lazy val userAnswers3 = emptyUserAnswers.copy(
    metadata = emptyMetadata.copy(mrn = "ABCD3333333333333", eoriNumber = "EoriNumber3")
  )

  private lazy val userAnswers4 = emptyUserAnswers.copy(
    metadata = emptyMetadata.copy(mrn = "ABCD1111111111111", eoriNumber = "EoriNumber4"),
    createdAt = Instant.now()
  )

  private lazy val userAnswers5 = emptyUserAnswers.copy(
    metadata = emptyMetadata.copy(mrn = "ABCD2222222222222", eoriNumber = "EoriNumber4"),
    createdAt = Instant.now().minus(1, HOURS)
  )

  private lazy val userAnswers6 = emptyUserAnswers.copy(
    metadata = emptyMetadata.copy(mrn = "EFGH3333333333333", eoriNumber = "EoriNumber4")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    insert(userAnswers1).futureValue
    insert(userAnswers2).futureValue
    insert(userAnswers4).futureValue
    insert(userAnswers5).futureValue
  }

  private def findOne(mrn: String, eoriNumber: String): Option[UserAnswers] =
    find(
      Filters.and(
        Filters.eq("mrn", mrn),
        Filters.eq("eoriNumber", eoriNumber)
      )
    ).futureValue.headOption

  "get" must {

    "return UserAnswers when given an LocalReferenceNumber and EoriNumber" in {

      val result = repository.get(userAnswers1.mrn, userAnswers1.eoriNumber).futureValue

      result.value.mrn shouldEqual userAnswers1.mrn
      result.value.eoriNumber shouldEqual userAnswers1.eoriNumber
      result.value.metadata shouldEqual userAnswers1.metadata
    }

    "return None when no UserAnswers match LocalReferenceNumber" in {

      val result = repository.get(userAnswers3.mrn, userAnswers1.eoriNumber).futureValue

      result should not be defined
    }

    "return None when no UserAnswers match EoriNumber" in {

      val result = repository.get(userAnswers1.mrn, userAnswers3.eoriNumber).futureValue

      result should not be defined
    }
  }

  "set" must {

    "create new document when given valid UserAnswers" in {

      findOne(userAnswers3.mrn, userAnswers3.eoriNumber) should not be defined

      val setResult = repository.set(userAnswers3.metadata).futureValue

      setResult shouldEqual true

      val getResult = findOne(userAnswers3.mrn, userAnswers3.eoriNumber).get

      getResult.mrn shouldEqual userAnswers3.mrn
      getResult.eoriNumber shouldEqual userAnswers3.eoriNumber
      getResult.metadata shouldEqual userAnswers3.metadata
    }

    "update document when it already exists" in {

      val firstGet = findOne(userAnswers1.mrn, userAnswers1.eoriNumber).get

      val metadata = userAnswers1.metadata.copy(
        data = Json.obj("foo" -> "bar")
      )
      val setResult = repository.set(metadata).futureValue

      setResult shouldEqual true

      val secondGet = findOne(userAnswers1.mrn, userAnswers1.eoriNumber).get

      firstGet.id shouldEqual secondGet.id
      firstGet.mrn shouldEqual secondGet.mrn
      firstGet.eoriNumber shouldEqual secondGet.eoriNumber
      firstGet.metadata shouldNot equal(secondGet.metadata)
      firstGet.createdAt shouldEqual secondGet.createdAt
      firstGet.lastUpdated `isBefore` secondGet.lastUpdated shouldEqual true
    }
  }

  "remove" must {

    "remove document when given a valid LocalReferenceNumber and EoriNumber" in {

      findOne(userAnswers1.mrn, userAnswers1.eoriNumber) shouldBe defined

      val removeResult = repository.remove(userAnswers1.mrn, userAnswers1.eoriNumber).futureValue

      removeResult shouldEqual true

      findOne(userAnswers1.mrn, userAnswers1.eoriNumber) should not be defined
    }

    "not fail if document does not exist" in {

      findOne(userAnswers3.mrn, userAnswers3.eoriNumber) should not be defined

      val removeResult = repository.remove(userAnswers3.mrn, userAnswers3.eoriNumber).futureValue

      removeResult shouldEqual true
    }
  }

  "getAll" must {

    "when given no params" should {

      "return UserAnswersSummary" in {

        val result = repository.getAll(userAnswers4.eoriNumber).futureValue

        result match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers4.eoriNumber
            totalMovements shouldEqual 2
            totalMatchingMovements shouldEqual 2
            userAnswers.length shouldEqual 2
            userAnswers.head.mrn shouldEqual userAnswers4.mrn
            userAnswers.head.eoriNumber shouldEqual userAnswers4.eoriNumber
            userAnswers(1).mrn shouldEqual userAnswers5.mrn
            userAnswers(1).eoriNumber shouldEqual userAnswers5.eoriNumber
        }
      }

      "return UserAnswersSummary with empty userAnswers when given an EoriNumber with no entries" in {

        val result = repository.getAll(userAnswers3.eoriNumber).futureValue

        result.userAnswers shouldEqual Seq.empty
      }
    }

    "when given an mrn param" should {

      "return UserAnswersSummary that match a full MRN" in {

        insert(userAnswers6).futureValue

        val result = repository.getAll(userAnswers4.eoriNumber, Some(userAnswers4.mrn)).futureValue

        result match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers4.eoriNumber
            totalMovements shouldEqual 3
            totalMatchingMovements shouldEqual 1
            userAnswers.length shouldEqual 1
            userAnswers.head.mrn shouldEqual userAnswers4.mrn
            userAnswers.head.eoriNumber shouldEqual userAnswers4.eoriNumber
        }
      }

      "return UserAnswersSummary that match a partial MRN" in {

        insert(userAnswers6).futureValue

        val result = repository.getAll(userAnswers4.eoriNumber, Some("ABCD")).futureValue

        result match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers4.eoriNumber
            totalMovements shouldEqual 3
            totalMatchingMovements shouldEqual 2
            userAnswers.length shouldEqual 2
            userAnswers.head.mrn shouldEqual userAnswers4.mrn
            userAnswers.head.eoriNumber shouldEqual userAnswers4.eoriNumber
            userAnswers(1).mrn shouldEqual userAnswers5.mrn
            userAnswers(1).eoriNumber shouldEqual userAnswers5.eoriNumber
        }

      }

      "return UserAnswersSummary with empty sequence of userAnswers when given an EoriNumber with no entries" in {

        val result = repository.getAll(userAnswers4.eoriNumber, Some("INVALID_SEARCH")).futureValue

        result.userAnswers shouldEqual Seq.empty
      }
    }

    "when given limit param" should {

      "return UserAnswersSummary to limit sorted by createdDate" in {

        val userAnswers1 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI1111111111111", eoriNumber = "AB123"),
          createdAt = Instant.now()
        )

        val userAnswers2 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "X22222222222222", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, HOURS)
        )

        val userAnswers3 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB13333333333333", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(2, DAYS)
        )

        val userAnswers4 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB24444444444444", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, DAYS)
        )

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers4).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, limit = Some(2)).futureValue

        result match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers1.eoriNumber
            totalMovements shouldEqual 4
            totalMatchingMovements shouldEqual 4
            userAnswers.length shouldEqual 2
            userAnswers.head.mrn shouldEqual userAnswers1.mrn
            userAnswers(1).mrn shouldEqual userAnswers2.mrn
        }

      }

      "return UserAnswersSummary to limit and to mrn param sorted by createdDate" in {

        val userAnswers1 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI1111111111111", eoriNumber = "AB123"),
          createdAt = Instant.now()
        )

        val userAnswers2 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI2222222222222", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, HOURS)
        )

        val userAnswers3 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI3333333333333", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(2, HOURS)
        )

        val userAnswers4 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB1111111111111", eoriNumber = "AB123"),
          createdAt = Instant.now()
        )

        val userAnswers5 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB2222222222222", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, DAYS)
        )

        val userAnswers6 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB3333333333333", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(2, DAYS)
        )

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers6).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, Some("GB"), limit = Some(2)).futureValue

        result match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers1.eoriNumber
            totalMovements shouldEqual 6
            totalMatchingMovements shouldEqual 3
            userAnswers.length shouldEqual 2
            userAnswers.head.mrn shouldEqual userAnswers4.mrn
            userAnswers(1).mrn shouldEqual userAnswers5.mrn
        }
      }
    }

    "when given skip param" should {

      "return UserAnswersSummary, skipping based on skip param and limit param" in {

        val userAnswers1 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB111", eoriNumber = "AB123"),
          createdAt = Instant.now()
        )

        val userAnswers2 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB222", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, HOURS)
        )

        val userAnswers3 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB333", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, DAYS)
        )

        val userAnswers4 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB444", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(2, DAYS)
        )

        val userAnswers5 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB555", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(3, DAYS)
        )

        val userAnswers6 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB666", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(4, DAYS)
        )

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers6).futureValue

        val result1 = repository.getAll(userAnswers1.eoriNumber, limit = Some(2), skip = Some(1)).futureValue
        val result2 = repository.getAll(userAnswers1.eoriNumber, limit = Some(2), skip = Some(2)).futureValue
        val result3 = repository.getAll(userAnswers1.eoriNumber, limit = Some(3), skip = Some(1)).futureValue

        result1 match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers1.eoriNumber
            totalMovements shouldEqual 6
            totalMatchingMovements shouldEqual 6
            userAnswers.length shouldEqual 2
            userAnswers.head.mrn shouldEqual userAnswers3.mrn
            userAnswers(1).mrn shouldEqual userAnswers4.mrn
        }

        result2 match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers1.eoriNumber
            totalMovements shouldEqual 6
            totalMatchingMovements shouldEqual 6
            userAnswers.length shouldEqual 2
            userAnswers.head.mrn shouldEqual userAnswers5.mrn
            userAnswers(1).mrn shouldEqual userAnswers6.mrn
        }

        result3 match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers1.eoriNumber
            totalMovements shouldEqual 6
            totalMatchingMovements shouldEqual 6
            userAnswers.length shouldEqual 3
            userAnswers.head.mrn shouldEqual userAnswers4.mrn
            userAnswers(1).mrn shouldEqual userAnswers5.mrn
            userAnswers(2).mrn shouldEqual userAnswers6.mrn
        }
      }

      "return UserAnswersSummary to limit, mrn and skip param sorted by createdDate" in {

        val userAnswers1 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI1111111111111", eoriNumber = "AB123"),
          createdAt = Instant.now()
        )

        val userAnswers2 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI2222222222222", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, HOURS)
        )

        val userAnswers3 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "XI3333333333333", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(2, HOURS)
        )

        val userAnswers4 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB1111111111111", eoriNumber = "AB123"),
          createdAt = Instant.now()
        )

        val userAnswers5 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB2222222222222", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(1, DAYS)
        )

        val userAnswers6 = emptyUserAnswers.copy(
          metadata = emptyMetadata.copy(mrn = "GB3333333333333", eoriNumber = "AB123"),
          createdAt = Instant.now().minus(2, DAYS)
        )

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers6).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, Some("GB"), limit = Some(2), skip = Some(1)).futureValue

        result match {
          case UserAnswersSummary(eoriNumber, userAnswers, totalMovements, totalMatchingMovements) =>
            eoriNumber shouldEqual userAnswers1.eoriNumber
            totalMovements shouldEqual 6
            totalMatchingMovements shouldEqual 3
            userAnswers.length shouldEqual 1
            userAnswers.head.mrn shouldEqual userAnswers6.mrn
        }
      }
    }

    "when given sortBy param" should {

      val userAnswers1 = emptyUserAnswers.copy(
        metadata = emptyMetadata.copy(mrn = "AA1111111111111", eoriNumber = "AB123"),
        createdAt = Instant.now().minus(3, DAYS)
      )

      val userAnswers2 = emptyUserAnswers.copy(
        metadata = emptyMetadata.copy(mrn = "BB2222222222222", eoriNumber = "AB123"),
        createdAt = Instant.now().minus(6, DAYS)
      )

      val userAnswers3 = emptyUserAnswers.copy(
        metadata = emptyMetadata.copy(mrn = "CC3333333333333", eoriNumber = "AB123"),
        createdAt = Instant.now().minus(5, DAYS)
      )

      val userAnswers4 = emptyUserAnswers.copy(
        metadata = emptyMetadata.copy(mrn = "DD1111111111111", eoriNumber = "AB123"),
        createdAt = Instant.now().minus(4, DAYS)
      )

      val userAnswers5 = emptyUserAnswers.copy(
        metadata = emptyMetadata.copy(mrn = "EE2222222222222", eoriNumber = "AB123"),
        createdAt = Instant.now().minus(1, DAYS)
      )

      val userAnswers6 = emptyUserAnswers.copy(
        metadata = emptyMetadata.copy(mrn = "FF3333333333333", eoriNumber = "AB123"),
        createdAt = Instant.now().minus(2, DAYS)
      )

      "return UserAnswersSummary, which is sorted by mrn in ascending order when sortBy is mrn.asc" in {

        insert(userAnswers6).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers1).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers2).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, sortBy = Some(SortByMRNAsc.convertParams)).futureValue

        result match {
          case UserAnswersSummary(_, userAnswers, _, _) =>
            userAnswers.head.mrn shouldEqual userAnswers1.mrn
            userAnswers(1).mrn shouldEqual userAnswers2.mrn
            userAnswers(2).mrn shouldEqual userAnswers3.mrn
            userAnswers(3).mrn shouldEqual userAnswers4.mrn
            userAnswers(4).mrn shouldEqual userAnswers5.mrn
            userAnswers(5).mrn shouldEqual userAnswers6.mrn
        }

      }
      "return UserAnswersSummary, which is sorted by mrn in descending order when sortBy is mrn.desc" in {

        insert(userAnswers6).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers1).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers2).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, sortBy = Some(SortByMRNDesc.convertParams)).futureValue

        result match {
          case UserAnswersSummary(_, userAnswers, _, _) =>
            userAnswers.head.mrn shouldEqual userAnswers6.mrn
            userAnswers(1).mrn shouldEqual userAnswers5.mrn
            userAnswers(2).mrn shouldEqual userAnswers4.mrn
            userAnswers(3).mrn shouldEqual userAnswers3.mrn
            userAnswers(4).mrn shouldEqual userAnswers2.mrn
            userAnswers(5).mrn shouldEqual userAnswers1.mrn
        }

      }
      "return UserAnswersSummary, which is sorted by createdAt in ascending order when sortBy is createdAt.asc" in {

        insert(userAnswers6).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers1).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers2).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, sortBy = Some(SortByCreatedAtAsc.convertParams)).futureValue

        result match {
          case UserAnswersSummary(_, userAnswers, _, _) =>
            userAnswers.head.mrn shouldEqual userAnswers2.mrn
            userAnswers(1).mrn shouldEqual userAnswers3.mrn
            userAnswers(2).mrn shouldEqual userAnswers4.mrn
            userAnswers(3).mrn shouldEqual userAnswers1.mrn
            userAnswers(4).mrn shouldEqual userAnswers6.mrn
            userAnswers(5).mrn shouldEqual userAnswers5.mrn
        }

      }
      "return UserAnswersSummary, which is sorted by createdAt in descending order when sortBy is createdAt.desc" in {

        insert(userAnswers6).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers1).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers2).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, sortBy = Some(SortByCreatedAtDesc.convertParams)).futureValue

        result match {
          case UserAnswersSummary(_, userAnswers, _, _) =>
            userAnswers.head.mrn shouldEqual userAnswers5.mrn
            userAnswers(1).mrn shouldEqual userAnswers6.mrn
            userAnswers(2).mrn shouldEqual userAnswers1.mrn
            userAnswers(3).mrn shouldEqual userAnswers4.mrn
            userAnswers(4).mrn shouldEqual userAnswers3.mrn
            userAnswers(5).mrn shouldEqual userAnswers2.mrn
        }

      }

      "return UserAnswersSummary, which is sorted by createdAt in descending order when sortBy is None" in {

        insert(userAnswers6).futureValue
        insert(userAnswers4).futureValue
        insert(userAnswers5).futureValue
        insert(userAnswers1).futureValue
        insert(userAnswers3).futureValue
        insert(userAnswers2).futureValue

        val result = repository.getAll(userAnswers1.eoriNumber, sortBy = None).futureValue

        result match {
          case UserAnswersSummary(_, userAnswers, _, _) =>
            userAnswers.head.mrn shouldEqual userAnswers5.mrn
            userAnswers(1).mrn shouldEqual userAnswers6.mrn
            userAnswers(2).mrn shouldEqual userAnswers1.mrn
            userAnswers(3).mrn shouldEqual userAnswers4.mrn
            userAnswers(4).mrn shouldEqual userAnswers3.mrn
            userAnswers(5).mrn shouldEqual userAnswers2.mrn
        }

      }
    }
  }

  "ensureIndexes" must {
    "ensure the correct indexes" in {
      val indexes = repository.collection.listIndexes().toFuture().futureValue
      indexes.length shouldEqual 3

      indexes.head.get("name").get shouldEqual BsonString("_id_")

      def findIndex(name: String): Document = indexes.find(_.get("name").get == BsonString(name)).get

      val createdAtIndex = findIndex("user-answers-created-at-index")
      createdAtIndex.get("key").get shouldEqual BsonDocument("createdAt" -> 1)
      createdAtIndex.get("expireAfterSeconds").get.asNumber().intValue() shouldEqual 2592000

      val eoriMrnIndex = findIndex("eoriNumber-mrn-index")
      eoriMrnIndex.get("key").get shouldEqual BsonDocument("eoriNumber" -> 1, "mrn" -> 1)
    }
  }
}
