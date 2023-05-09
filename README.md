<img src="docs/logo.png" alt="Adapquest" width="480"/>

**ADAP**tive **QUEST**ionnarie is a web-based microservice tool that permit the creation of adaptive tests, surveys,
and questionnaires based on a pool of questions and a graphical model.

The idea behind this project, is to have a simple way to assess the _skill level_ of a person based on a series of
questions. A skill is considered a particular detail of a person that can be identified with the right questions. The
adaptive engine aims to find the optimal question for a user in order to maximize the score of the skills.

This tool is based on the [CreMA](https://github.com/IDSIA/crema) library, also developed
by [IDSIA](https://github.com/IDSIA).

# Content

This repository is composed by three different modules:

* the `Backend` that contains the adaptive engine;

* the `Exchange` library, a _Java_ library that can be used to build remote clients for the adaptive engine; and

* the `Experiments` module: a module that uses the `Backend` and the `Exchange` library to perform academic experiments
  with adaptive surveys.

In addition, the `Backend` module is also available as a **Docker** image and in a ready to use `docker-compose` format.

# Characteristics

This tool allows the user to build a survey with an _adaptive_ choice of the questions. In other words, the next
questions depends on the model and the previous answers of a person.

For each survey/questionnaire, a unique `access code` allows multiple person to complete the questions in an anonymous
way. Each survey has its own `model` that can be crafted to the specific scenarios and questions available.

Some configurations are available, such as:

- limiting the adaptivity using minimum and maximum number of question, and minimum and maximum scoring parameters;
- limit the order of the questions can be posed;
- group the questions by skill, depending on the structure of the used model.

Each answer given by a person update its internal `state`. The history of all the answers given and the states produced
can be used to follow the evolution of the distributions of probability associated with the questionnaire.

For details on the model creation, check the documentation for the [CreMA](https://github.com/IDSIA/crema) library. For
an in depth analysis of the available options, check the [Wiki](https://github.com/IDSIA/adapquest/wiki).

# Usage

## Execution

Once compiled, the simplest way to run the tool and access to the demo page is to first set these two environment
variables:

```bash
DB_DBMS=memory
MAGIC_API_KEY=<put there a random string>
```

These two settings allows the application to run with an *in-memory* database and init the remote api key.

Finally, run the build jar as following:

```bash
java -jar adapquest-backend.jar
```

The demo page will be accessible at the url http://localhost:8080/demo/.

This configuration is **not** suitable for a production environment and should be used only to check the demo or be used
for experiments. For something production-ready-more-or-less rad the next section.

## Docker image

The easiest way to run the tool, is to use the **Docker** image. Using the provided `docker-compose.yaml` it is possible
to have a running application in few minutes reachable on port `:8080`.

The stack is composed by a database for storing the questions and the session and the backend engine. We
chose `Postgres 13.1` but any other SQL-based engine supported by *Hibernate* should be compatible.

Refer to the [Wiki](https://github.com/IDSIA/adapquest/wiki) for more details on the `docker-compose` configuration.

> **Note:** to change the context path of the application it is possible to use the environment variable `SERVER_SERVLET_CONTEXT-PATH`.

## Personalization

It is possible to change the title of the page using the following environment variable:

```
ADAPQUEST_PAGE_TITLE: "AdapQuest"
```

If it is required to have an _exit button_, a button that can bring the survey token outside the AdapQuest platform, it is possible to use the following two environment variables.

```
ADAPQUEST_EXIT_URL: "<some valid url>"
ADAPQUEST_EXIT_TEXT: "<the text to show>"
```

The required token will be available in the `sid` field.


## Keycloak integration

It is possible to use Keycloak as identity provider instead of the simple internal mechanism based on APIkey.

### Keycloak client entry

In order to do so, first create a new `client` on Keycloak by assigning a new `client-id`.
This client need to have `openid-connect` as _Client Protocol_ and `confidential` as _Access Type_.
Fields `Valid Redirect URIs` and `Web Origins` can be set to the deployment location or to `*`.

In the page _Credentials_ generate and copy the `Secret` field.

### Environment variables

Then on the deployment environment variables, set them as following:

* `KEYCLOAK_ENABLED` set to `true` to enable the Keycloak integration;
* `KEYCLOAK_REALM` set to the value of the application realm;
* `KEYCLOAK_AUTH_SERVER_URL` set to the authentication server url (ex. `http://keycloak.example.com/auth`);
* `KEYCLOAK_RESOURCE` set as the `client-id` assigned in Keycloak for this application;
* `KEYCLOAK_CREDENTIALS_SECRET` set as the generated `Secret` as above;
* `ADAPQUEST_KEYCLOAK_FIELD`: set the filed to store in the database, if missing or set as empty (`""`) nothing is stored.

Default fields are: _email, username, birthdate, name, family_name, nickname, given_name, middle_name, phone_number, website_. For custom fields, assign new _scopes_ to the client. 

### Application roles

The application can have two types of roles: a generic role, and an administrative role.

The generic role (`ADAPQUEST_KEYCLOAK_ROLE`) is to allow only users that have the specified role to be able to perform the survey.
If this variable is empty (`""`) then all users can access to the surveys.

The administrative role (`ADAPQUEST_KEYCLOAK_ADMIN`) is only used to update and manage surveys through the admin console.

### Docker-compose

All these variables can be used in a `docker-compose` file.

# Citation

If you write a scientific paper describing research that made use of the `AdapQuest` tool, please see the [CreMA](https://github.com/IDSIA/crema#citation) library citation note.
