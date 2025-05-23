
# manage-transit-movements-arrival-cache

## Endpoints

---

## `GET /user-answers?mrn=[Option[String]]&limit=[Option[Int]]&skip=[Option[Int]]&sortBy=[Option[String]]`

#### Params

* Optional param list for user answers endpoint
    * mrn - Filters documents for partial or matching Movement Reference Number of all user answers for given eori number
    * limit - Sets the maximum number of returned documents
    * skip - Sets the increment of skipped documents for pagination purposes. The number of documents skipped is worked out as 'skip * limit'
    * sortBy - Sorts the documents being returned. These can be sorted in ascending or descending order using the MRN or date created

### Successful response

#### 200 OK

* A call is made to the `GET` endpoint with:
    * a valid bearer token
    * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
* A list of documents are found in the `user-answers` collection for the given EORI number from enrolment
* The response JSON will have a summary list of each user answer with the following fields:
    * `mrn` - The movement reference number associated with the arrival application
    * `_links` - Links used to retrieve the full data for a specific mrn
    * `createdAt` - The date and time that the application was started. The user has 30 days from this point to complete and submit the application
    * `lastUpdated` - The date and time that the application was last updated
    * `id` - a UUID

#### Success response

```
{
  "eoriNumber": "1234567",
  "totalMovements": 2,
  "totalMatchingMovements": 2, 
  "userAnswers": [
      {
        "mrn": "AB123",
        "_links": {
          "self": {
            "href": "/manage-transit-movements-arrival-cache/user-answers/AB123"
        }
      },
      "createdAt": "2023-01-26T10:32:15.648",
      "lastUpdated": "2023-01-27T08:43:17.064",
      "expiresInDays": 29,
      "_id": "27e687a9-4544-4e22-937e-74e699d855f8"
      },
      {
        "mrn": "CD123",
        "_links": {
        "self": {
          "href": "/manage-transit-movements-arrival-cache/user-answers/CD123"
      }
    },
    "createdAt": "2023-01-26T10:32:36.96",
    "lastUpdated": "2023-01-26T10:32:41.377",
    "expiresInDays": 29,
    "_id": "750f1f92-6c61-4a3b-ad3e-95d8c7418eb4"
    }
  ]
}
```

### Unsuccessful responses (with possible causes)

#### 401 UNAUTHORIZED
* A generic authorization error occurred. The likely cause of this is an invalid or missing bearer token.

#### 403 FORBIDDEN
* User has insufficient enrolments

#### 404 NOT_FOUND
* No documents were found for EORI number

#### 500 INTERNAL_SERVER_ERROR
* An error occurred in the mongo client

---

## `GET /user-answers/:mrn`

### Successful response

#### 200 OK

* A call is made to the `GET` endpoint with:
    * a valid bearer token
    * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
* A document is found in the `user-answers` collection for the given MRN (the EORI number is extracted from the enrolment)
* The response JSON has the following fields:
    * `mrn` - The movement reference number associated with the departure application
    * `eoriNumber` - The EORI number linked to the user's enrolment
    * `data` - The user's answers
    * `createdAt` - The date and time that the application was started. The user has 30 days from this point to complete and submit the application
    * `lastUpdated` - The date and time that the application was last updated
    * `id` - a UUID

### Unsuccessful responses (with possible causes)

#### 401 UNAUTHORIZED
* A generic authorization error occurred. The likely cause of this is an invalid or missing bearer token.

#### 403 FORBIDDEN
* User has insufficient enrolments

#### 404 NOT_FOUND
* No document was found for the given MRN

#### 500 INTERNAL_SERVER_ERROR
* An error occurred in the mongo client

---

##  `POST /user-answers/:mrn`

### Successful response

#### 200 OK

* A call is made to the `POST` endpoint with:
    * a valid bearer token
    * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
    * a valid `UserAnswers` request body
    * the same EORI number in the enrolment and request body
* Then, for the given MRN and EORI number in the request body, either:
    * A document is found in the `user-answers` collection and the document gets updated with the user answers from the request body
    * A document is not found in the `user-answers` collection and a new document gets created with the user answers from the request body

### Unsuccessful responses (with possible causes)

#### 400 BAD_REQUEST
* Request body could not be validated as a `UserAnswers'

#### 401 UNAUTHORIZED
* A generic authorization error occurred. The likely cause of this is an invalid or missing bearer token.

#### 403 FORBIDDEN
* User has insufficient enrolments
* EORI number in request body does not match the EORI number in the user's enrolment

#### 500 INTERNAL_SERVER_ERROR
* An error occurred in the mongo client

---

##  `PUT /user-answers`

### Successful response

#### 200 OK

* A call is made to the `PUT` endpoint with:
    * a valid bearer token
    * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
    * a valid `String` request body representing the MRN
* Then, for the given MRN in the request body and EORI number in the enrolment, a new document gets created with an empty user answers

### Unsuccessful responses (with possible causes)

#### 400 BAD_REQUEST
* Request body could not be validated as a `String'

#### 401 UNAUTHORIZED
* A generic authorization error occurred. The likely cause of this is an invalid or missing bearer token.

#### 403 FORBIDDEN
* User has insufficient enrolments

#### 500 INTERNAL_SERVER_ERROR
* An error occurred in the mongo client

---

##  `DELETE /user-answers/:mrn`

### Successful response

#### 200 OK

* A call is made to the `DELETE` endpoint with:
    * a valid bearer token
    * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
* A document is deleted from the `user-answers` collection for the given MRN (the EORI number is extracted from the enrolment)

### Unsuccessful responses (with possible causes)

#### 500 INTERNAL_SERVER_ERROR
* An error occurred in the mongo client

---

## `POST /declaration/submit`

### Successful response

#### 200 OK

* A call is made to the `POST` endpoint with:
  * a valid bearer token
  * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
  * a valid `String` request body representing the MRN
* Then, an IE007 gets successfully submitted to the API

### Unsuccessful responses (with possible causes)

#### 400 BAD_REQUEST
* Request body could not be validated as a `String`

#### 401 UNAUTHORIZED
* A generic authorization error occurred. The likely cause of this is an invalid or missing bearer token.

#### 403 FORBIDDEN
* User has insufficient enrolments

#### 500 INTERNAL_SERVER_ERROR
* An error occurred

---

## `GET /messages/:mrn`

### Successful response

#### 200 OK

* A call is made to the `GET` endpoint with:
  * a valid bearer token
  * a valid `HMRC-CTC-ORG` enrolment with `EoriNumber` identifier
* An arrival is found in the API for the given MRN and EORI number (extracted from the enrolment)
* Then, the messages corresponding to this arrival ID are retrieved

### Unsuccessful responses (with possible causes)

#### 204 NO_CONTENT
* The arrival was found, but it contained no messages

#### 401 UNAUTHORIZED
* A generic authorization error occurred. The likely cause of this is an invalid or missing bearer token.

#### 403 FORBIDDEN
* User has insufficient enrolments

#### 404 NOT_FOUND
* The arrival was not found

#### 500 INTERNAL_SERVER_ERROR
* An error occurred

---

# Running the Service
* Start the service locally with `sbt run` in the root directory
* The service can be started via service manager, depending on the version of service manager you are using, with either:
    * `sm --start MANAGE_TRANSIT_MOVEMENTS_ARRIVAL_CACHE`
    * `sm2 --start MANAGE_TRANSIT_MOVEMENTS_ARRIVAL_CACHE`

# Testing the Service
* Run the unit tests by running `sbt test` in the root directory
* Run the integration tests (requires `MongoDB`) by running `sbt it/test` in the root directory

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
