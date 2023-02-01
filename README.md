# webapp
> aim to build a cloud native web application

## Technology Stack
A cloud-native application is an application that is specifically designed for cloud computing architecture. It takes advantage of cloud computing frameworks, which are composed of loosely coupled cloud services.

**Programming Language:** Java 17

**Relational Database:** MySQL

**Object Storage:** N/A

**Backend Framework:** Spring Boot 3.0

**UI Framework:** N/A

**CSS:** N/A

## User Story

1. All API request/response payloads should be in JSON.
2. No UI should be implemented for the application.
3. As a user, I expect all API calls to return with a proper [HTTP status code (Links to an external site.)](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes).
4. As a user, I expect the code quality of the application to be maintained to the highest standards using the unit and/or integration tests.
5. Your web application must only support [Token-Based authentication and not Session Authentication Links to an external site.](https://security.stackexchange.com/questions/81756/session-authentication-vs-token-authentication).
6. As a user, I must provide a [basic Links to an external site.](https://en.wikipedia.org/wiki/Basic_access_authentication#Client_side) [authentication Links to an external site.](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication) token when making an API call to the `authenticated` endpoint.
7. Create a new user
   1. As a user, I want to create an account by providing the following information.
      1. Email Address
      2. Password
      3. First Name
      4. Last Name
   2. `account_created` field for the user should be set to the current time when user creation is successful.
   3. Users should not be able to set values for `account_created` and `account_updated`. Any value provided for these fields must be ignored.
   4. `Password` should never be returned in the response payload.
   5. As a user, I expect to use my *email address* as my *username*.
   6. Application must return `400 Bad Request` HTTP response code when a user account with the email address already exists.
   7. As a user, I expect my password to be stored securely using the [BCrypt password hashing scheme Links to an external site.](https://docs.spring.io/spring-security/site/docs/current/apidocs/org/springframework/security/crypto/bcrypt/BCrypt.html) with [salt Links to an external site.](https://en.wikipedia.org/wiki/Salt_(cryptography)).
8. Update user information
   1. As a user, I want to update my account information. I should only be allowed to update the following fields.
      1. First Name
      2. Last Name
      3. Password
   2. Attempt to update any other field should return `400 Bad Request` HTTP response code.
   3. `account_updated` field for the user should be updated when the user update is successful.
   4. A user can only update their own account information.
9. Get user information
   1. As a user, I want to get my account information. Response payload should return all fields for the user except for `password`.
