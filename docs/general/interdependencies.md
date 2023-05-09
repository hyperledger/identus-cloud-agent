# Interdependencies

This document describes the interdependencies between the different Building Blocks.



```mermaid
flowchart
  PLUTO-didcomm -.WIP.-> OR1{OR} 
  didcommx --external dependency--> OR1{OR} 

  subgraph PRISM
    prism-cryto --> prism-node-1.4
    prism-cryto --> OR2{OR}
    prism-node-1.4 -.copy proto files.-> prism-node

    subgraph BB[Building Blocks Interdependencies]


      pluto -.WIP.-> OR2{OR}
      pluto --> PLUTO-didcomm

      OR1 --> mercury

      OR2 ---> iris
      OR2 --> castor

      subgraph REPO [BB Repository]
        prism-node -.???\nMAYBE in the future.-> pollux
        prism-node --> castor

        iris -.IRIS client\nMAYBE in the future.-> castor
        iris -.IRIS client\nMAYBE in the future.-> prism-agent
        iris --> iris-server

        shared --> prism-agent
        shared --> pollux
        shared --> castor
        shared --> connect
        shared --> iris
        
        mercury --> pollux
        mercury --> connect
        mercury --> prism-agent
        mercury --> mediator

        connect --> prism-agent

        castor --> pollux
        castor --> prism-agent
        %%castor --> iris

        pollux --> prism-agent

        infrastructure
        test
      end
    end
  end

  

%% All 
castor[Castor]
connect
infrastructure
iris[IRIS]
iris-server((IRIS-server))
mercury[MERCURY]
mediator((Mercury\nMediator))
pluto[PLUTO extrenal repo]
pollux[POLLUX]
prism-agent((Prism Agent))
prism-node[prism-node-client]
shared
test
```

**Note:**
- The **Mercury Libraries** collect all dependencies with the single cross version.
We will represent it as a single box here. But the actual dependencies can be of a subset of the Mercury Libraries.
For more information about the Mercury Modules Interdependencies, see [HERE](./mercury/mercury-library/README.md).
- The **Pollux Libraries** collect all dependencies with the single cross version.