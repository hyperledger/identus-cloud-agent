# Quick start

If you don't have [sbt](https://www.scala-sbt.org) installed already, you can use the provided wrapper script:

```shell
./sbtx -h # shows an usage of a wrapper script
./sbtx compile # build the project
./sbtx test # run the tests
./sbtx agents/run # run example of encrypt / decrypt messages
```

For more details check the [sbtx usage](https://github.com/dwijnand/sbt-extras#sbt--h) page.

Otherwise, if sbt is already installed, you can use the standard commands:

```shell
sbt compile # build the project
sbt test # run the tests
sbt mediator/run # run the application (Main)
```

## open api docs explorer

```shell
 http://localhost:8080/docs 
 currently the above url defaults to swagger petstore was not able to fix in mean while follow bellow step.
 once you open above docs url in browser insert in the explorer input box `/docs/docs.yaml`
```

## Links

- [tapir documentation](https://tapir.softwaremill.com/en/latest/)
- [tapir github](https://github.com/softwaremill/tapir)
- [bootzooka: template microservice using tapir](https://softwaremill.github.io/bootzooka/)
- [sbtx wrapper](https://github.com/dwijnand/sbt-extras#installation)
