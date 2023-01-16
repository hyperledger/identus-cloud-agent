# Atala v2 Documentation

This is the landing page for the Atala v2 technical documentation set.

## Architecture diagrams

The diagrams are based on the [C4 model](https://c4model.com) to describe the architecture at different levels of details:
1. ***Context***: provides a starting point, showing how the software system in scope fits into the world around it.
2. ***Containers***: zooms into the software system in scope, showing the high-level technical building blocks.
3. ***Components***: zooms into an individual container, showing the components inside it.
4. ***Code***: optional, can be used to zoom into an individual component, showing how that component is implemented.

We use the [Diagrams as code](https://diagrams-as-code.com) approach, coding using the [Structurizr DSL](https://structurizr.com).

### Source files

Source code for the different diagrams can be found in the DSL files located [here](./architecture/structurizr).

### Visualisation

A visual and interactive representation of the diagrams can be rendered using [Structurizr Lite](https://structurizr.com/help/lite).

The following [Dockerfile](./architecture/Dockerfile) is provided to create a custom Docker image of Structurizr Lite that embeds the Atala DSL files.
To build the custom image, execute the following command from the repo's root folder:
```bash
docker build -t atala-structurizr-lite docs/architecture
```

## OpenAPI Specifications

### Source files
- The OpenAPI specification for a **service** building block is written in YAML format.
- Each building block is responsible for making its YAML spec available to other parties via HTTP GET on one of its service endpoints.

### Visualisation

[Swagger UI](https://swagger.io/tools/swagger-ui/) is used and allows development teams and service consumers to visualize and interact with the API’s resources
without having any of the implementation logic in place. It is automatically generated from the OpenAPI specification.

Swagger UI is available as a [Docker image](https://hub.docker.com/r/swaggerapi/swagger-ui).

## Run locally with Docker Compose

The following [docker-compose.yml](./docker-compose.yml) file can be used to run both services in one shot by executing the following command from the repo's root folder:

***!!! Make sure to build the custom Atala Structurizr Lite Docker image first as described above.***
```bash
docker-compose -f docs/docker-compose.yml up
```
- Architecture diagrams doc is available on *localhost:8080*
- OpenAPI specifications doc is available *localhost:8081*
