<img src="docs/logo.png" alt="Adapquest" width="480"/>

**ADAP**tive **QUEST**ionnarie is a web-based micro-service tool that permit the creation of adaptive tests, surveys,
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

For each survey/questionnaire, an unique `access code` allows multiple person to complete the questions in an anonymous
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
chosed `Postgres 13.1` but any other SQL-based engine supported by *Hibernate* should be compatible.

Refer to the [Wiki](https://github.com/IDSIA/adapquest/wiki) for more details on the `docker-compose` configuration.

# Citation

If you write a scientific paper describing research that made use of the `AdapQuest` tool, please see
the [CreMA](https://github.com/IDSIA/crema#citation) library citation note.
